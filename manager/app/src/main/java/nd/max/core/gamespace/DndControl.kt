/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * «عدم الإزعاج» — الكتابة التي يحترمها النظام، لا التي يُلغيها.
 *
 * ### السبب الجذريّ (مقيس، لا مُرجَّح)
 *
 * كان المساران (`PanelToggles.setDnd` و`AppMonitor` عند «عدم الإزعاج أثناء اللعب») يكتبان
 * `settings put global zen_mode 1` ثم يقرآن القيمة فيجدانها… كما تُركِت. والسبب في AOSP نفسه:
 * `ZenModeHelper.SettingsObserver.update()` يعيد كتابة `zen_mode` من حالته الداخلية كلّما رآه
 * مختلفًا عنها (`"Fixing zen mode setting"`). فالكتابة المباشرة في الإعدادات **تُردّ** دائمًا،
 * و«النجاح» الذي كان يُعلن لم يكن إلا غياب استثناء من مزوّد الإعدادات.
 * وسجلّ جهاز المالك يحمل الأثر نفسه:
 * `EVENT=LOG_HEADER … code=zen-mode-write-not-honoured` مع `zen_mode 1` في `app_status`
 * و`saved_zen_mode=0` في `daemon_state`.
 *
 * ### والمسارات الحقيقية — اثنان، ولكلّ حدّه
 *
 * 1. **صلاحية سياسة الإشعارات** (`isNotificationPolicyAccessGranted`): تُكتب الحالة عبر
 *    `NotificationManager.setInterruptionFilter` — واجهة عامّة يحترمها النظام (API 21+).
 * 2. **`cmd notification set_dnd`** بجذر أو Shizuku (uid الـshell): دخل Android 11 (API 30)،
 *    وقبله لا يوجد مسار صدَفة. و`settings put global zen_mode` **لا** يُستعمل هنا أبدًا.
 *
 * وما لا مسار له يُعلَن `UNSUPPORTED` ولا يُقلَّد نجاحًا — ولهذا تُخفي اللوحة مفتاحها على
 * Android 10 بلا صلاحية سياسة بدل أن تعرض مفتاحًا لا يفعل شيئًا.
 *
 * ### ولا حكم بلا قراءة
 *
 * كل كتابة تُتبع بقراءة محدودة ([VERIFY_DELAYS_MS]) من [currentZen] — وهي تقرأ الفلتر الحيّ
 * إن مُنحت الصلاحية، وإلا مرآة `zen_mode` التي يكتبها النظام نفسه. ولا يُعلن [Verdict.APPLIED]
 * إلا إذا طابقت القراءة المطلوب؛ والغياب والفشل كلاهما ليس نجاحًا.
 */
object DndControl {
    /** أوّل إصدار يملك `cmd notification set_dnd` (Android 11). */
    const val SHELL_MIN_API = 30

    /** مهلات إعادة القراءة — مجموعها ١٫٢ ث، فلا تُعلَّق الواجهة على وعد كتابة. */
    val VERIFY_DELAYS_MS: LongArray = longArrayOf(80L, 160L, 320L, 640L)

    /** حكم الكتابة كما يراه المستدعي. */
    enum class Verdict {
        /** قرأنا المطلوب بعد الكتابة. */
        APPLIED,

        /** نُفِّذت المحاولة ولم تتغيّر الحالة — أو تغيّرت إلى غير المطلوب. */
        NOT_HONOURED,

        /** لا طبقة تنفيذ (وضع Basic/Shizuku غير جاهز) — لم يُنفَّذ شيء. */
        REFUSED,

        /** لا مسار على هذا الإصدار/الجهاز أصلًا. */
        UNSUPPORTED,
    }

    /** نتيجة أمر الصدفة — والفرق بين «رُفض» و«فشل» مقصود: لكلٍّ رسالته. */
    enum class ShellVerdict { RAN, FAILED, REFUSED }

    /** مدير الإشعارات **إن كانت صلاحية السياسة ممنوحة**؛ وغيره `null` (لا مسار مباشر). */
    fun notificationManager(ctx: Context): NotificationManager? =
        runCatching {
            ctx.applicationContext.getSystemService(NotificationManager::class.java)
                ?.takeIf { it.isNotificationPolicyAccessGranted }
        }.getOrNull()

    fun policyAccess(ctx: Context): Boolean = notificationManager(ctx) != null

    /** الإتاحة بلا سياق — نقيّة فتُختبر: صلاحية سياسة، أو إصدار يملك أمر الصدفة. */
    fun availableOn(sdkInt: Int, policyAccess: Boolean): Boolean =
        policyAccess || sdkInt >= SHELL_MIN_API

    fun available(ctx: Context): Boolean =
        availableOn(Build.VERSION.SDK_INT, policyAccess(ctx))

    /** مرآة `zen_mode` — يكتبها `ZenModeHelper` نفسه، وقراءتها مسموحة دائمًا. */
    fun zenModeOf(ctx: Context): Int =
        runCatching { Settings.Global.getInt(ctx.contentResolver, "zen_mode", 0) }.getOrDefault(0)

    /**
     * الحالة الحيّة: فلتر الانقطاع إن مُنحت السياسة (أدقّ وأسرع من المرآة)، وإلا `zen_mode`.
     * والقيمة المُعادة هي ترميز `zen_mode` (٠ .. ٣) لا ثنائيّة «مفعّل/متوقّف».
     */
    fun currentZen(ctx: Context): Int {
        val manager = notificationManager(ctx)
        if (manager != null) {
            val filter = runCatching { manager.currentInterruptionFilter }.getOrNull()
            zenForFilter(filter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN)?.let { return it }
        }
        return zenModeOf(ctx)
    }

    fun isOn(ctx: Context): Boolean = currentZen(ctx) != 0

    /** `zen_mode` ← فلتر الإطار (للمسار المباشر). */
    fun filterForZen(zen: Int): Int = when (zen) {
        1 -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        2 -> NotificationManager.INTERRUPTION_FILTER_NONE
        3 -> NotificationManager.INTERRUPTION_FILTER_ALARMS
        else -> NotificationManager.INTERRUPTION_FILTER_ALL
    }

    /** فلتر الإطار ← `zen_mode`؛ و`UNKNOWN` ليس حالة فلا يُترجم. */
    fun zenForFilter(filter: Int): Int? = when (filter) {
        NotificationManager.INTERRUPTION_FILTER_ALL -> 0
        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> 1
        NotificationManager.INTERRUPTION_FILTER_NONE -> 2
        NotificationManager.INTERRUPTION_FILTER_ALARMS -> 3
        else -> null
    }

    /** الكلمة التي يقبلها `cmd notification set_dnd`. */
    fun shellModeForZen(zen: Int): String = when (zen) {
        1 -> "priority"
        2 -> "none"
        3 -> "alarms"
        else -> "all"
    }

    /**
     * يكتب النمط [zen] (٠ = متوقّف · ١ = أولويات · ٢ = صمت · ٣ = منبّهات) ويعيد الحكم.
     *
     * [policy] غير الفارغ يعني أن الصلاحية ممنوحة (انظر [notificationManager])؛ و[shell] مسار
     * الصدفة (جذر/Shizuku) أو `null` عند من لا صدفة له. و[read] تقرأ الحالة الحيّة — تُمرَّر
     * من المستدعي ليبقى هذا الملفّ بلا اعتماد على سياق أو انعكاس. و[sdkInt]/[sleep] قابلان
     * للحقن في الاختبار (لا وقت ولا إصدار حقيقيَّين).
     */
    fun apply(
        zen: Int,
        policy: NotificationManager?,
        shell: ((String) -> ShellVerdict)?,
        read: () -> Int,
        sdkInt: Int = Build.VERSION.SDK_INT,
        sleep: (Long) -> Unit = { Thread.sleep(it) },
    ): Verdict {
        if (zen !in 0..3) return Verdict.UNSUPPORTED

        if (policy != null) {
            val accepted = runCatching { policy.setInterruptionFilter(filterForZen(zen)) }.isSuccess
            // سباق صلاحية (سُحبت بين الفحص والنداء) يُسقط إلى الصدفة، لا إلى «نجاح» وهميّ.
            if (accepted) return if (await(zen, read, sleep)) Verdict.APPLIED else Verdict.NOT_HONOURED
        }

        if (sdkInt < SHELL_MIN_API || shell == null) return Verdict.UNSUPPORTED

        val lane = runCatching { shell("cmd notification set_dnd ${shellModeForZen(zen)}") }
            .getOrDefault(ShellVerdict.FAILED)
        return when (lane) {
            ShellVerdict.REFUSED -> Verdict.REFUSED
            // أمر انتهى بخطأ لا يُقرأ نجاحًا، ولو كانت الحالة مصادفةً صحيحة.
            ShellVerdict.FAILED -> Verdict.NOT_HONOURED
            ShellVerdict.RAN -> if (await(zen, read, sleep)) Verdict.APPLIED else Verdict.NOT_HONOURED
        }
    }

    /** قراءة أولى فوريّة ثم إعادات محدودة — ولا يُتوقَّف على قراءة ترمي (انقطاع binder مثلًا). */
    private fun await(zen: Int, read: () -> Int, sleep: (Long) -> Unit): Boolean {
        fun observed(): Int = runCatching(read).getOrDefault(-1)
        if (observed() == zen) return true
        for (ms in VERIFY_DELAYS_MS) {
            sleep(ms)
            if (observed() == zen) return true
        }
        return false
    }
}
