/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nd.max.BuildConfig
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.core.hardware.HealthLedger
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.remote.InstallIdentities
import nd.max.core.remote.InstallIdentity
import nd.max.core.remote.SignalOutcome
import nd.max.core.remote.StatsDue
import nd.max.core.remote.StatsRules
import nd.max.core.remote.StatsState
import nd.max.core.remote.StatsStore
import nd.max.core.remote.TrackClient
import java.io.File
import java.util.Locale

/**
 * **قناة إحصاءات المجتمع — الطرف الذي كان غائبًا** (`MAX-CONTROL-01` §١٠ · §١١).
 *
 * الخادم كان جاهزًا منذ البداية (`track_install` و`admin_country_stats` في `supabase/schema.sql`،
 * ولوحة المالك تعرضهما)، ولم يكن في التطبيق طرفٌ ينادي — فبقي عدّاد «المستخدمون» صفرًا لا
 * لأنه معطوب بل لأنّ الحلقة مقطوعة. هذا الكائن يصلها: إشارة واحدة يوميًّا **بعد موافقة
 * صريحة**، أو لا شيء.
 *
 * ## الشرطان يُجمعان ولا يُبدَّلان
 *
 * الإرسال يقع إذا تحقّق: موافقة المستخدم على الجهاز (`settings` ← `stats_consent`،
 * الافتراضيّ **لا**) **و** `collect=true` في الإعداد الموقّع. فلا سلطة للخادم أن يجمع ممّن
 * لم يوافق، ولا للموافقة أن تُرسل إذا أوقف المركزُ الجمعَ (نصّ [RemoteStats] نفسه).
 *
 * ## ما لا يفعله هذا الكائن
 *
 * * لا يقرأ معرّفًا نظاميًّا ولا يشتقّ شيئًا من العتاد (§١٠.٢ — يمنعه اختبار العقد نصيًّا).
 * * لا يفتح خدمة خلفيّة ولا `WorkManager`: الإشارة تُركب الإقلاعَ القائم (§١٠.٤)، والقواعد
 *   النقيّة — الإيقاع والتباعد والسقوف — في [StatsRules] حيث تُختبر على JVM.
 * * لا يحجب خيطًا ولا يرمي: كلّه في نطاقه الخلفيّ، وكل عطبٍ يُسجَّل مصنَّفًا.
 */
object CommunityStats {

    private const val COMPONENT = "community_stats"

    /** ملفّ الإعدادات العامّ نفسه الذي تكتب فيه الشاشات (`UnavailableFeatures` و[ThemeSettings]) — لا ملفّ جديد. */
    private const val PREFS = "settings"

    /** مفتاح الموافقة: غيابه = لا (§١٠.٣: الافتراضيّ مطفأ، ولا إشارة قبل «نعم»). */
    private const val KEY_CONSENT = "stats_consent"

    private const val STATE_FILE = "stats.state"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var appContext: Context? = null
    @Volatile private var identity: InstallIdentity? = null
    @Volatile private var store: StatsStore? = null
    @Volatile private var running = false

    /** يُنادى مرّةً من [RemoteAccess.boot]: يحمّل الهويّة والذاكرة. بلا سياق لا قناة. */
    fun attach(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        identity = InstallIdentities.loadOrCreate(File(app.filesDir, InstallIdentities.FILE_NAME))
        store = StatsStore(File(app.filesDir, STATE_FILE))
        // عدّادات الصحّة (§١٠.١) تُركَّب هنا مع شقيقتها: مصدر واحد للإقلاع، كما في القواعد.
        HealthLedger.attach(app)
    }

    /** قراءة الموافقة الحاليّة — الافتراضيّ `false` ولو غاب المفتاح. */
    fun consent(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CONSENT, false)

    /**
     * قلب المفتاح — والاثنان فعلٌ واحد لا وعد:
     * * تشغيل ⇒ نداء إشارة الآن (تحترم قواعده: التباعد والسقوف).
     * * إطفاء ⇒ سحبٌ كامل ([withdraw]) — حذف سجلّنا المحلّيّ وطلب حذف واحد (§١٠.٣ البند ٣).
     */
    fun setConsent(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CONSENT, value)
            .apply()
        if (value) attempt("consent_on") else withdraw()
    }

    /**
     * **الرسالة كما ستُرسل حرفيًّا** — نفس بناء الإرسال (`StatsRules.trackPayload`) لا وصفًا
     * موازيًا؛ فما تراه في الشاشة هو البايتات نفسها (شرط §١٠.٣ البند ٢).
     */
    fun preview(): String {
        val app = appContext ?: return ""
        val id = identity?.id ?: return ""
        return payloadFor(id, appVersion(app), envNow(), HealthLedger.json())
    }

    /** نداء الإشارة — يُركب الإقلاع القائم، ومرّتان متزامنتان تُهمَل الثانية بهدوء. */
    fun attempt(reason: String) {
        val app = appContext ?: return
        if (running) return
        running = true
        scope.launch {
            try {
                val state = store?.read() ?: StatsState()
                val collect = RemoteAccess.state.value.config.stats?.collect == true
                val version = appVersion(app)
                val env = envNow()
                val health = HealthLedger.json()
                val now = System.currentTimeMillis()
                when (val due = StatsRules.due(state, consent(app), collect, version, env, now, health)) {
                    is StatsDue.Skip -> DiagnosticCenter.record(
                        component = COMPONENT,
                        message = "stats skipped ${due.reason.code} ($reason)",
                    )
                    StatsDue.Send -> send(app, state, version, env, health, now, reason)
                }
            } catch (error: Exception) {
                DiagnosticCenter.record(COMPONENT, "stats attempt error ($reason)", error)
            } finally {
                running = false
            }
        }
    }

    private fun send(
        app: Context,
        state: StatsState,
        version: String,
        env: String,
        health: String,
        now: Long,
        reason: String,
    ) {
        val id = identity?.id ?: run {
            DiagnosticCenter.record(COMPONENT, "stats skipped no_identity ($reason)")
            return
        }
        val client = client() ?: run {
            DiagnosticCenter.record(COMPONENT, "stats skipped not_configured ($reason)")
            return
        }
        when (val outcome = client.track(payloadFor(id, version, env, health))) {
            SignalOutcome.Sent -> {
                store?.write(
                    StatsState(
                        lastSentAt = now,
                        lastSentVersion = version,
                        lastSentEnv = env,
                        lastSentHealth = health,
                        attempts = 0,
                        lastAttemptAt = now,
                    ),
                )
                DiagnosticCenter.record(COMPONENT, "stats sent v$version env=$env ($reason)")
            }
            is SignalOutcome.Failed -> {
                store?.write(state.copy(attempts = state.attempts + 1, lastAttemptAt = now))
                DiagnosticCenter.record(COMPONENT, "stats send failed ${outcome.fault.code} ($reason)")
            }
        }
    }

    /**
     * السحب بضغطة واحدة: طلب حذف واحد بالمعرّف نفسه، فإذا وصل **حُذف الملفّ المحلّيّ** —
     * وتعود الدورة بهويّة جديدة إن عاد المستخدم فوافق (فلا يرتبط ما قبل السحب بما بعده).
     * وإن فشل الطلب (شبكة): يُمسح السجلّ المحلّيّ ويبقى المعرّف، فمحاولةٌ لاحقة قد تُتمّ الحذف.
     */
    private fun withdraw() {
        val app = appContext ?: return
        if (running) return
        running = true
        scope.launch {
            try {
                val id = identity?.id
                val client = client()
                if (id != null && client != null) {
                    when (val outcome = client.forget(StatsRules.forgetPayload(id))) {
                        SignalOutcome.Sent -> {
                            InstallIdentities.remove(File(app.filesDir, InstallIdentities.FILE_NAME))
                            identity = null
                            DiagnosticCenter.record(COMPONENT, "stats withdrawn delete_sent")
                        }
                        is SignalOutcome.Failed -> DiagnosticCenter.record(
                            component = COMPONENT,
                            message = "stats withdraw failed ${outcome.fault.code}",
                        )
                    }
                }
                store?.clear()
                // وعدّادات الصحّة تُمسح معها: لا يبقى لجهازٍ سحب موافقته حسابٌ محليّ (وإن عاد
                // فوافق، بدأ العدّ من الصفر — فلا يرتبط ما بعد السحب بما قبله).
                HealthLedger.clear()
                DiagnosticCenter.record(COMPONENT, "stats local record cleared")
            } catch (error: Exception) {
                DiagnosticCenter.record(COMPONENT, "stats withdraw error", error)
            } finally {
                running = false
            }
        }
    }

    private fun payloadFor(id: String, version: String, env: String, health: String): String =
        StatsRules.trackPayload(
            installId = id,
            country = Locale.getDefault().country,
            version = version,
            manufacturer = Build.MANUFACTURER ?: "",
            model = Build.MODEL ?: "",
            sdk = Build.VERSION.SDK_INT,
            release = Build.VERSION.RELEASE ?: "",
            env = env,
            health = health,
        )

    private fun envNow(): String = StatsRules.envOf(PrivilegeManager.snapshot.value.level.name)

    private fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString()
    }.getOrDefault("")

    private fun client(): TrackClient? {
        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isEmpty()) return null
        return TrackClient(baseUrl = url, apiKey = BuildConfig.SUPABASE_ANON_KEY.trim())
    }
}
