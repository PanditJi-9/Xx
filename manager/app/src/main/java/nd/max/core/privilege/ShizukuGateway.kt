/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuSystemProperties

/** حالة جسر Shizuku كما يراها التطبيق الآن. */
data class ShizukuState(
    val available: Boolean = false,
    val permissionGranted: Boolean = false,
    val version: Int = -1,
    val uid: Int = -1,
) {
    val ready: Boolean get() = available && permissionGranted
}

/**
 * جسر Shizuku (`AR-20`) — طبقة الامتياز الثانية بجانب الجذر، وتُنفِّذ **فعلًا** منذ هذه الجولة.
 *
 * **لماذا Shizuku تحديدًا:** جزء معتبر من أدواتنا (الكثافة · DNS · تجميد/تعطيل الحزم
 * عبر `pm` · `AppOps` · قراءة البطارية · السجلات) لا يحتاج جذرًا أصلًا، بل امتياز ADB.
 * فتح هذه الطبقة يضاعف من يستطيع استخدام التطبيق **ويضيّق** سطح الجذر معًا.
 *
 * **كيف تُنفَّذ الأوامر:** عبر خدمة مستخدم ([ShizukuShellService]) يعيش كائنها في عملية
 * يديرها خادم Shizuku، فيصل الأمر إلى ما لا تصل إليه عملية التطبيق (انظر [exec]). ولم تعد
 * `Shizuku.newProcess` (وهي خاصّة غير عامة في `api:13.1.5` — جرى التحقّق بـ`javap`) هي الطريق:
 * الـ`UserService` هو المسار العام والرسمي، وهو المُنفَّذ هنا.
 *
 * **قواعد السلامة:** كل استدعاء لـShizuku محاط بـ[runCatching] — فلا يسقط التطبيق إن
 * غاب الجسر أو تغيّر إصداره. ولا يُطلب امتياز إلا بفعل صريح من المستخدم. والخدمة تعمل
 * بصلاحية عملية Shizuku نفسها — لا ترقية سرّية، ولا كتابة sysfs من هذا الطريق.
 *
 * **حدّ يُعلَن ولا يُخفي:** التنفيذ الفعلي **لم يُقَس على جهاز** في هذه البيئة — المُتحقَّق هنا
 * هو العقد والترجمة والاختبارات؛ وأول تشغيل حقيقي على هاتف هو ما يحوّل «مبنيّ» إلى «يعمل».
 */
object ShizukuGateway {

    /** رمز طلب إذن Shizuku — ثابت داخل التطبيق، ولا يُشارَك مع أحد. */
    const val PERMISSION_REQUEST_CODE = 4213

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var listening = false

    /** سياق التطبيق — يُثبَّت مرّة في الإقلاع، ولا يُمسَك سياق نشاط (لا تسريب). */
    @Volatile
    private var appContext: Context? = null

    /** الخدمة المربوطة، أو `null` قبل الربط/بعده. قراءتها من أي خيط، والربط محروز واحدًا واحدًا. */
    @Volatile
    private var shell: IShizukuShell? = null

    /** من ينتظر وصول الـbinder الآن — يُكمَل من ردّ الربط أو يُبطَل بمهلة/موت الجسر. */
    @Volatile
    private var pendingConnect: CompletableDeferred<IShizukuShell?>? = null

    /** قُفل الربط: نداءان متوازيان لا يُطلقان عمليتين. */
    private val bindLock = Mutex()

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }

    private val binderDead = Shizuku.OnBinderDeadListener {
        // موت الجسر يُبطل كل ما بُني عليه: الخدمة ماتت معه، ومن ينتظر لا يُترك معلّقًا أبدًا.
        shell = null
        pendingConnect?.complete(null)
        refresh()
    }

    private val permissionResult =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == PERMISSION_REQUEST_CODE) refresh()
        }

    /**
     * ردّ الربط نفسه — على الخيط الرئيسي كما يشترط نظام الخدمات.
     * ووصول الخدمة مرّتين لا يضرّ: `CompletableDeferred.complete` الثانية لا تعمل شيئًا.
     */
    private val shellConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bound = runCatching { IShizukuShell.Stub.asInterface(service) }.getOrNull()
            shell = bound
            pendingConnect?.complete(bound)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shell = null
        }
    }

    /** يثبّت سياق التطبيق — يُنادى مرّة من الإقلاع ([`nd.max.MaxManagerApplication`]). */
    fun install(context: Context) {
        appContext = context.applicationContext
    }

    /** يُسجّل المستمعين مرة واحدة، ثم يحدّث الحالة. آمن للاستدعاء المتكرر. */
    fun start() {
        if (!listening) {
            listening = runCatching {
                Shizuku.addBinderReceivedListenerSticky(binderReceived)
                Shizuku.addBinderDeadListener(binderDead)
                Shizuku.addRequestPermissionResultListener(permissionResult)
                true
            }.getOrDefault(false)
        }
        refresh()
    }

    fun stop() {
        if (listening) {
            runCatching {
                Shizuku.removeBinderReceivedListener(binderReceived)
                Shizuku.removeBinderDeadListener(binderDead)
                Shizuku.removeRequestPermissionResultListener(permissionResult)
            }
            listening = false
        }
        // ومن ينتظر الربط لا يُترك معلّقًا حتى المهلة: يُبطل فورًا.
        pendingConnect?.complete(null)
        pendingConnect = null
        // والخدمة تُسقَط مع الربط (`remove = true`): لا تبقى عملية معلّقة سُربت من جلسة سابقة.
        val context = appContext
        if (shell != null && context != null) {
            runCatching { Shizuku.unbindUserService(userServiceArgs(context), shellConnection, true) }
        }
        shell = null
    }

    /** يقرأ الحالة الحالية ويحدّث [state]. يعود بالحالة المقروءة. */
    fun refresh(): ShizukuState {
        val current = runCatching { read() }.getOrElse { ShizukuState() }
        _state.value = current
        return current
    }

    private fun read(): ShizukuState {
        if (!Shizuku.pingBinder()) return ShizukuState()
        val version = runCatching { Shizuku.getVersion() }.getOrDefault(-1)
        val granted = if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        return ShizukuState(
            available = true,
            permissionGranted = granted,
            version = version,
            uid = uid,
        )
    }

    /** يطلب الإذن بفعل صريح من المستخدم فقط. لا يُنادى عند الإقلاع. */
    fun requestPermission() {
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
    }

    /**
     * ينفّذ أمرًا واحدًا بامتياز Shizuku ويعود بحكم معلن من ثلاثة ([ShizukuExec]).
     *
     * **العقد:** الرفض قبل المحاولة (`no-bridge`/`no-permission`/أمر غير صالح) لا يُنفَّذ شيء معه،
     * والفشل بعد المحاولة يُعلن بسببه، والنجاح برمز خروج — ولا نجاح بلا رمز.
     */
    suspend fun exec(command: String): ShizukuExec {
        val normalized = command.trim()
        val rejection = ShizukuExecRules.rejectionFor(normalized)
        if (rejection != null) return ShizukuExec.Refused(rejection)

        val current = state.value
        if (!current.available) return ShizukuExec.Refused("no-bridge")
        if (!current.permissionGranted) return ShizukuExec.Refused("no-permission")

        val service = connectedService() ?: return ShizukuExec.Failed("service-unavailable")
        return runCatching {
            withContext(Dispatchers.IO) { service.exec(normalized) }
        }.fold(
            onSuccess = { ShizukuExecRules.parseReply(it) },
            onFailure = { failure ->
                // سقط الخيط بين الندائين: لا يُترك binder ميتًا يُستعمل في النداء التالي.
                shell = null
                ShizukuExec.Failed(failure.message ?: failure.javaClass.simpleName)
            },
        )
    }

    /**
     * يرجع بالخدمة المربوطة أو يربطها مرّة واحدة. والربط نفسه يُجرى على الخيط الرئيسي
     * (الخدمات تسلّم ردّها هناك)، والانتظار بمهلة صريحة ([ShizukuExecRules.BIND_TIMEOUT_MS])
     * فلا يعلّق المستدعي أبدًا مهما كان حال الخادم.
     */
    private suspend fun connectedService(): IShizukuShell? {
        shell?.let { return it }
        return bindLock.withLock {
            shell?.let { return@withLock it }
            val context = appContext ?: return@withLock null
            val waiter = CompletableDeferred<IShizukuShell?>()
            pendingConnect?.complete(null)
            pendingConnect = waiter
            val bound = runCatching {
                withContext(Dispatchers.Main) {
                    Shizuku.bindUserService(userServiceArgs(context), shellConnection)
                }
            }.isSuccess
            if (!bound) {
                if (pendingConnect === waiter) pendingConnect = null
                return@withLock null
            }
            val service = withTimeoutOrNull(ShizukuExecRules.BIND_TIMEOUT_MS) { waiter.await() }
            if (pendingConnect === waiter) pendingConnect = null
            service
        }
    }

    /**
     * وصف الخدمة كما يجب أن يراها خادم Shizuku.
     *
     * و[Shizuku.UserServiceArgs.daemon] `false` مقصود: الخدمة تعيش مع التطبيق، لا بعده — فلا تبقى
     * عملية بصلاحية ADB معلّقة على الجهاز بعد أن يُغلق المستخدم التطبيق. والاسم النسبيّ `shell`
     * يفصلها عن أي خدمة أخرى لنا في المستقبل.
     */
    private fun userServiceArgs(context: Context): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuShellService::class.java.name))
            .daemon(false)
            .processNameSuffix(PROCESS_NAME_SUFFIX)
            .debuggable(false)
            .version(ShizukuExecRules.SERVICE_VERSION)

    /** لاحقة اسم العملية التي يشتقّها خادم Shizuku لعملية الخدمة. */
    private const val PROCESS_NAME_SUFFIX = "shell"

    /**
     * يقرأ خاصية نظام بامتياز Shizuku — عبر الواجهة **العامة** `ShizukuSystemProperties`.
     * يعود بـ`null` إن غاب الامتياز أو فشلت القراءة؛ والمستدعي يعلن الفشل.
     */
    fun getProp(key: String, def: String = ""): String? = runCatching {
        ShizukuSystemProperties.get(key, def)
    }.getOrNull()
}
