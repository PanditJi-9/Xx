/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nd.max.AppMonitorLogger
import nd.max.MaxManagerProps
import nd.max.core.privilege.PrivilegedShell
import nd.max.core.hardware.TouchControlBackend
import nd.max.core.platform.PropertyUtils
import java.util.Locale

/**
 * مقابض لوحة «المكعّب» التي تُكتب بامتياز فعلًا — عبر المسار الواحد: جذر، أو Shizuku، أو رفض معلن.
 * كل دالة `set*` تُستدعى من خيط IO فقط، وكل `*On` تقرأ **الحالة الحيّة** من خدمة النظام لا مفتاحًا
 * مخزَّنًا وحده؛ وكل كتابة تُتبع بقراءة تحقّق محدودة فلا تُعلن حالة لم تحدث. أمّا التحويل الجانبي:
 * لا أداة تُعرض هنا إن لم يكن لها أثر حقيقي على الجهاز.
 */
/** إخفاق كتابة مقبض: [key] مفتاح المقبض (`wifi` · `data` · `dnd`)، و[refused] = لم يُنفَّذ أصلًا. */
data class PanelWriteFailure(val key: String, val refused: Boolean)

object PanelToggles {
    /** نطاق للكتابة يعيش أطول من اللوحة: طيّ اللوحة بعد الضغط لا يلغي الكتابة. */
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** يستقبل الكتل المعلّقة أيضًا ([PrivilegedShell.exec]) فلا يحجب خيطًا بانتظار ربط أو مهلة. */
    fun async(block: suspend () -> Unit) {
        io.launch { runCatching { block() } }
    }

    private fun globalOrNull(ctx: Context, key: String): Int? =
        runCatching { Settings.Global.getInt(ctx.contentResolver, key) }.getOrNull()

    /**
     * الحالة الحيّة من `WifiManager`، أو `null` إن غابت الخدمة — والغائب يُقرأ من المرآة لا «مغلقًا».
     *
     * **السبب المقيس:** `wifi_on` مفتاح مُخزَّن يكتبه النظام عند تغيير الحالة، وليس الحالة نفسها.
     * فقراءته وحدها كانت تُظهر «مغلقًا» بينما Wi-Fi يعمل (أو العكس بعد تبديل خارج اللوحة)، ثم
     * تكتب اللوحة عكس ما يراه المستخدم. والقراءة الحيّة تحتاج `ACCESS_WIFI_STATE` (صلاحية عادية).
     */
    private fun wifiLive(ctx: Context): Boolean? = runCatching {
        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled
    }.getOrNull()

    fun wifiOn(ctx: Context): Boolean =
        PanelStateRules.resolve(wifiLive(ctx), globalOrNull(ctx, "wifi_on"), fallback = false)

    fun setWifi(ctx: Context, on: Boolean) =
        write("wifi", "svc wifi ${if (on) "enable" else "disable"}", on) { wifiOn(ctx) }

    fun hasCellular(ctx: Context): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    /**
     * الحالة الحيّة من `TelephonyManager.isDataEnabled()` (شريحة البيانات الافتراضية — **نفس هدف**
     * `svc data`)، أو `null` إن غابت الخدمة.
     *
     * **والفرق مقصود:** `Settings.Global.mobile_data` مفتاح قديم لا يتبع الشريحة الافتراضية على
     * الأجهزة متعددة الشرائح، وقد يغيب أصلًا. وقراءته وحدها كانت تعطي مفتاحًا لا يطابق ما تبدّله
     * `svc data`، فيقول المستخدم «التبديل لا يتزامن». والصلاحية المطلوبة `ACCESS_NETWORK_STATE` فقط.
     */
    private fun dataLive(ctx: Context): Boolean? = runCatching {
        (ctx.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.isDataEnabled
    }.getOrNull()

    fun dataOn(ctx: Context): Boolean =
        PanelStateRules.resolve(dataLive(ctx), globalOrNull(ctx, "mobile_data"), fallback = true)

    fun setData(ctx: Context, on: Boolean) =
        write("data", "svc data ${if (on) "enable" else "disable"}", on) { dataOn(ctx) }

    /**
     * هل لـ«عدم الإزعاج» مسار حقيقي على هذا الجهاز؟ تُخفي اللوحة مفتاحها حين لا يكون —
     * فلا يُعرض مفتاح لا يفعل شيئًا (Android 10 بلا صلاحية سياسة إشعارات).
     */
    fun dndAvailable(ctx: Context): Boolean = DndControl.available(ctx)

    fun dndOn(ctx: Context): Boolean = DndControl.isOn(ctx)

    /**
     * «عدم الإزعاج» عبر [DndControl] لا عبر `settings put global zen_mode`: تلك الكتابة يردّها
     * `ZenModeHelper` كما تُكتب، وقد ثبت ذلك في سجل جهاز المالك (`zen-mode-write-not-honoured`).
     */
    fun setDnd(ctx: Context, on: Boolean) = async {
        val verdict = DndControl.apply(
            zen = if (on) 1 else 0,
            policy = DndControl.notificationManager(ctx),
            shell = { command ->
                when (val outcome = PrivilegedShell.exec(command)) {
                    is PrivilegedShell.Outcome.Ran ->
                        if (outcome.ok) DndControl.ShellVerdict.RAN else DndControl.ShellVerdict.FAILED

                    is PrivilegedShell.Outcome.Refused -> DndControl.ShellVerdict.REFUSED
                    is PrivilegedShell.Outcome.Failed -> DndControl.ShellVerdict.FAILED
                }
            },
            read = { DndControl.currentZen(ctx) },
        )
        when (verdict) {
            DndControl.Verdict.APPLIED -> Unit
            DndControl.Verdict.REFUSED -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_REFUSED key=dnd reason=no-privilege")
                _failures.tryEmit(PanelWriteFailure("dnd", refused = true))
            }

            DndControl.Verdict.UNSUPPORTED -> {
                // لا مسار على هذا الإصدار/الجهاز: لا نجاح ولا «رفض امتياز» — الحقيقة أن الكتابة
                // غير متاحة، والبلاطة تُخفى أصلًا حينها. والرسالة تبقى «لم يتغيّر».
                AppMonitorLogger.w("EVENT=PANEL_WRITE_UNSUPPORTED key=dnd")
                _failures.tryEmit(PanelWriteFailure("dnd", refused = false))
            }

            DndControl.Verdict.NOT_HONOURED -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_NOT_HONOURED key=dnd desired=${if (on) 1 else 0}")
                _failures.tryEmit(PanelWriteFailure("dnd", refused = false))
            }
        }
    }

    /**
     * المسار الواحد للكتابة المميّزة: الجذر إن وُجد، وإلا Shizuku، وإلا رفض مُعلن.
     *
     * وقبل هذه الجولة كانت الثلاثة تُنفَّذ بـ`Shell.cmd` مباشرةً: بجذر تعمل، وبلا جذر تُنفَّذ
     * بصلاحية التطبيق فترجع «permission denied» بينما اللوحة تعيد القراءة كأنها كتبت. الآن
     * النتيجة تُسجَّل بمفتاحها ورمزها وطبقته — واللوحة تعيد القراءة فلا تعرض حالة لم تحدث.
     *
     * **والجديد في هذه الجولة:** رمز الخروج وحده لا يكفي. `svc wifi`/`svc data` قد يُقبل الأمر
     * ثم يعود النظام إلى الحالة السابقة (أو يبطئ تطبيقها)، فكانت البلاطة تعلن نجاحًا ثم يصحّحها
     * الاستطلاع بعد ٢٫٤ ث بلا سبب مفهوم. الآن تُقرأ الحالة بعد الكتابة ([confirm]) ولا تُترك
     * إلا إن طابقت المطلوب؛ وإلا وصل [PanelWriteFailure] فورًا وقالت اللوحة لماذا.
     */
    private fun write(key: String, command: String, desired: Boolean, read: () -> Boolean?) = async {
        when (val outcome = PrivilegedShell.exec(command)) {
            is PrivilegedShell.Outcome.Ran -> when {
                !outcome.ok -> {
                    AppMonitorLogger.w(
                        "EVENT=PANEL_WRITE_FAILED key=$key lane=${outcome.lane} code=${outcome.code}",
                    )
                    _failures.tryEmit(PanelWriteFailure(key, refused = false))
                }

                !confirm(desired, read) -> {
                    AppMonitorLogger.w(
                        "EVENT=PANEL_WRITE_NOT_HONOURED key=$key lane=${outcome.lane} desired=$desired",
                    )
                    _failures.tryEmit(PanelWriteFailure(key, refused = false))
                }
            }

            is PrivilegedShell.Outcome.Refused -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_REFUSED key=$key reason=${outcome.reason}")
                _failures.tryEmit(PanelWriteFailure(key, refused = true))
            }

            is PrivilegedShell.Outcome.Failed -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_FAILED key=$key reason=${outcome.reason}")
                _failures.tryEmit(PanelWriteFailure(key, refused = false))
            }
        }
    }

    /** قراءة أولى فوريّة ثم إعادات محدودة؛ وقراءة ترمي (خدمة غائبة) ليست نجاحًا. */
    private suspend fun confirm(desired: Boolean, read: () -> Boolean?): Boolean {
        fun observed(): Boolean? = runCatching(read).getOrNull()
        if (PanelWriteRules.honoured(desired, observed())) return true
        for (ms in PanelWriteRules.VERIFY_DELAYS_MS) {
            delay(ms)
            if (PanelWriteRules.honoured(desired, observed())) return true
        }
        return false
    }

    /**
     * إخفاق الكتابة يُقال للمستخدم لا للّوج وحده: كان الضغط على مقبض يفشل فيعود المقبض بعد إعادة القراءة
     * ولا يعرف السبب. [PanelWriteFailure.refused] = رُفض قبل التنفيذ (الوضع/الامتياز)، وغيره = نُفِّذ وفشل.
     * بلا تراكم: الأحداث القديمة تُسقَط، فلا تتأخّر رسالة عن ضغطتها.
     */
    private val _failures = MutableSharedFlow<PanelWriteFailure>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val failures: SharedFlow<PanelWriteFailure> = _failures.asSharedFlow()

    /**
     * لا تظهر الأداة إلا إن وُجد عقدة لمس **أو** محوّل مصنّع معلن على هذا الجهاز.
     *
     * وكانت القائمة هنا **نسخة ثانية** من `TouchBoostBackend.GAME_MODE_NODES`، والنسختان تفترقان
     * بأول تعديل — فيظهر المفتاح في اللوحة ويغيب عن شاشة اللمس (أو العكس) على الجهاز نفسه.
     * والاكتشاف الآن من المالك الواحد: [TouchControlBackend.discover]، وهو الذي يكتب أيضًا.
     */
    fun touchAvailable(): Boolean {
        if (!GameRootGate.allowed()) return false
        val found = TouchControlBackend.discover()
        return found.hasBoostNode || found.vendorTransportPresent
    }

    fun touchOn(): Boolean = PropertyUtils.get(MaxManagerProps.Touch.BOOST) == "1"

    /**
     * يطبّق عبر مالك اللمس الواحد، ولا يحفظ التفضيل إلا إن لم تُرفض الكتابة ولم يغب المزوّد.
     *
     * والحفظ مشروط بـ[TouchControlBackend.Attempt.persistable] — أي «نجحت» أو «لا شيء يُفعَل
     * الآن» (`DEFERRED` عند عقدة تحمل المطلوب أصلًا)، لا «رُفضت» ولا «لا مزوّد».
     */
    fun setTouch(on: Boolean) {
        if (!GameRootGate.allowed()) return
        val attempt = TouchControlBackend.reconcile(on)
        if (attempt.persistable) PropertyUtils.set(MaxManagerProps.Touch.BOOST, if (on) "1" else "0")
    }
}

/**
 * قاعدة قراءة المقبض: **الحيّة أولًا**، ثم مرآة الإعدادات، ثم الافتراضي — والغائب ليس «مغلقًا».
 * نقيّة: تحكمها اختبارات JVM، فلا يعود مصدر الحقيقة إلى مفتاح قديم بلا قياس.
 */
internal object PanelStateRules {
    fun resolve(live: Boolean?, mirror: Int?, fallback: Boolean): Boolean =
        live ?: (mirror?.let { it != 0 } ?: fallback)
}

/**
 * حكم الكتابة: لا نجاح بلا قراءة مطابقة، وبلا محاولات غير محدودة.
 * والمهلات تراكميّة (~١٫٨ ث) — كافية لتبديل يُطبَّق فورًا، وقصيرة عن أن تُعلّق تجربة المستخدم.
 */
internal object PanelWriteRules {
    val VERIFY_DELAYS_MS: LongArray = longArrayOf(120L, 240L, 480L, 960L)

    fun honoured(desired: Boolean, observed: Boolean?): Boolean = observed == desired
}

/** خطوات المنبّه بالدقائق، و`0` = متوقف. نقيّة فتُختبر بلا جهاز. */
val REMINDER_STEPS: List<Int> = listOf(30, 60, 90, 120)

fun nextReminderMinutes(current: Int): Int {
    if (current <= 0) return REMINDER_STEPS.first()
    val index = REMINDER_STEPS.indexOf(current)
    // قيمة غير معروفة (مثل 45) تبدأ من أول خطوة؛ والصفر حصرًا بعد آخر خطوة.
    if (index < 0) return REMINDER_STEPS.first()
    return if (index == REMINDER_STEPS.lastIndex) 0 else REMINDER_STEPS[index + 1]
}

/** منبّه وقت اللعب: مؤقّت في الذاكرة ينبّه بـToast + نغمة إشعار، ولا يحتاج إذنًا إضافيًّا. */
object PanelReminder {
    private val main = Handler(Looper.getMainLooper())
    private var task: Runnable? = null

    @Volatile private var minutes = 0
    @Volatile private var endsAt = 0L

    fun remainingMin(): Int =
        if (endsAt == 0L) 0 else ((endsAt - SystemClock.elapsedRealtime() + 59_999L) / 60_000L).toInt().coerceAtLeast(0)

    /** يدوّر الخطوة التالية ويعيدها (`0` = أُوقف). */
    fun cycle(ctx: Context, doneMessage: String): Int {
        cancel()
        val next = nextReminderMinutes(minutes)
        minutes = next
        if (next == 0) return 0
        val app = ctx.applicationContext
        val delayMs = next * 60_000L
        endsAt = SystemClock.elapsedRealtime() + delayMs
        val r = Runnable {
            endsAt = 0L
            task = null
            runCatching { Toast.makeText(app, doneMessage, Toast.LENGTH_LONG).show() }
            runCatching {
                RingtoneManager.getRingtone(app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play()
            }
        }
        task = r
        main.postDelayed(r, delayMs)
        return next
    }

    private fun cancel() {
        task?.let { main.removeCallbacks(it) }
        task = null
        endsAt = 0L
    }
}

/**
 * تسجيل الشاشة عبر `screenrecord` (مقاطع ≤ 3 دقائق متتابعة حتى الإيقاف) إلى `Movies/MaxManager`،
 * **بالمسار الموحَّد** ([PrivilegedShell]): جذر، أو Shizuku (uid الـshell يملك `screenrecord` والكتابة إلى
 * `/sdcard`)، أو رفض معلَن. العلم ملفّ في `/data/local/tmp` فيعيش خارج عمر اللوحة، ولو فشل
 * `screenrecord` توقّفت الحلقة بدل أن تدور بلا نهاية.
 *
 * **غير مقيس على جهاز:** استمرار الحلقة الخلفية (`&`) بعد عودة الأمر تحت خدمة Shizuku. والتحقّق من
 * الحالة يبقى على ملفّ العلم نفسه، فإن لم تستمرّ تُعلن اللوحة فشل البدء بدل «بدأ التسجيل».
 */
object PanelScreenRecorder {
    private const val DIR = "/sdcard/Movies/MaxManager"
    private const val FLAG = "/data/local/tmp/.max_screenrec"

    /** الحالة من ملفّ العلم نفسه: لو مات `screenrecord` وحذف العلم، تتصحّح اللوحة وحدها. */
    suspend fun isRecording(): Boolean =
        PanelShellOutput.lastLine(PrivilegedShell.exec("[ -f $FLAG ] && echo 1 || echo 0")) == "1"

    suspend fun start() {
        PrivilegedShell.exec(
            "mkdir -p $DIR && touch $FLAG && " +
                "((while [ -f $FLAG ]; do screenrecord --time-limit 180 $DIR/Max_\$(date +%Y%m%d_%H%M%S).mp4 " +
                "|| { rm -f $FLAG; break; }; done) >/dev/null 2>&1 &)"
        )
    }

    suspend fun stop() {
        PrivilegedShell.exec(
            "rm -f $FLAG; pkill -2 screenrecord || killall -2 screenrecord; sleep 1; " +
                "for f in $DIR/*.mp4; do am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE " +
                "-d file://\$f >/dev/null 2>&1; done"
        )
    }
}

/**
 * لقطة شاشة إلى `Pictures/MaxManager` — **والتحقق قبل الإعلان**، وبالمسار الموحَّد ([PrivilegedShell]).
 *
 * `screencap` قد يُنهي أمره رمزيًّا ويكتب ملفًا صفريًّا (محتوى محمي، أو تنفيذ بلا صلاحية
 * رسومية)، والملفّ الفارغ يُقرأ «نجاحًا» في الواجهة وهو لا شيء. فالقاعدة نفسها التي تحكم
 * القياس تحكم الالتقاط: **لا يُعلن ما لم يُقَس** — لا يُعاد مسار إلا بعد أن يقول الملفّ إنه
 * صورة (مقاسه > صفر)، وما دونه يُحذف فورًا فلا يبقى أثر يوهم بنجاح (§9.3 من الطلب).
 * ورفضُ المسار (لا امتياز في الوضع المختار) يُعامَل كأي فشل: الحجم صفر ⇒ `null`.
 */
object PanelScreenshot {
    private const val DIR = "/sdcard/Pictures/MaxManager"

    /** يلتقط ويعيد مسار الصورة، أو `null` إن فشل الالتقاط أو الكتابة. من أي سياق (يتحوّل إلى IO داخليًا). */
    suspend fun capture(): String? {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val path = "$DIR/Max_$stamp.png"
        PrivilegedShell.exec("mkdir -p $DIR && screencap -p $path")
        val size = PanelShellOutput.lastLine(PrivilegedShell.exec("stat -c %s $path 2>/dev/null || echo 0"))
            ?.toLongOrNull() ?: 0L
        if (size <= 0L) {
            PrivilegedShell.exec("rm -f $path")
            return null
        }
        PrivilegedShell.exec(
            "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$path >/dev/null 2>&1"
        )
        return path
    }
}

/** قراءة خَرْج أمر المسار الموحَّد: آخر سطر غير فارغ إن نُفِّذ فعلًا، و`null` إن رُفض أو فشل الطريق. */
internal object PanelShellOutput {
    fun lastLine(outcome: PrivilegedShell.Outcome): String? =
        (outcome as? PrivilegedShell.Outcome.Ran)?.output
            ?.lineSequence()
            ?.map { it.trim() }
            ?.lastOrNull { it.isNotEmpty() }
}

/** ساعة الجلسة: منذ متى اللعبة الحالية أمامية (تُصفَّر بتبدّل اللعبة). */
object PanelSessionClock {
    @Volatile private var pkg: String? = null
    @Volatile private var since = 0L

    fun touch(packageName: String) {
        if (packageName != pkg) {
            pkg = packageName
            since = SystemClock.elapsedRealtime()
        }
    }

    fun hoursText(): String {
        val hours = if (since == 0L) 0f else (SystemClock.elapsedRealtime() - since) / 3_600_000f
        return "%.1fh".format(Locale.US, hours)
    }
}

/** تطبيق مختصر يظهر أعلى اللوحة: يُعرض فقط إن كان مثبّتًا فعلًا وله شاشة تشغيل. */
class QuickApp(val pkg: String, val label: String, val icon: Bitmap)
