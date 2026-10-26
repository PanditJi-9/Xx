/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * هل هذا الإدخال في قائمة التطبيقات يطلب **شيئًا** فعلًا؟
 *
 * ### العطب المقيس الذي وُلد منه الملف (جولة الحرارة واللاغ)
 *
 * قائمة التطبيقات تحمل إدخالًا لكل تطبيق أضافه المستخدم، حتى لو كانت كل حقوله `default`.
 * وكان `AppMonitor` يعامل **مجرد وجود الاسم** على أنه ملكية عتاد: يلتقط خط أساس (عشرات
 * أوامر `shell`)، ويكتب سجل استعادة، ويعلن `perapp_active`، ويُنادي عند المغادرة
 * `restoreGlobalMaxManagerProfile()` — أي **عملية كاملة** تعيد كتابة ملف الأداء العام.
 *
 * والقياس من حزمة المستخدم (‏`MaxManager.log` · ٢٣–٢٥ أكتوبر): `com.termux` وحده أنتج
 * **٢٩٠ جلسة** و**٢٩٠ تراجعًا**، وفي كل جلسة `PERAPP_KNOB ... reason=profile-is-default`
 * — أي أن الإعداد لم يطلب شيئًا، والكتابة على العتاد وقعت مع ذلك. وهذا هو الحمل الذي
 * يُقاس حرارةً وبطاريةً في الاستخدام العادي (تبديل تطبيقات)، لا في اللعب.
 *
 * ### والقاعدة
 *
 * [hasExplicitOverride] تعني: **حقل واحد على الأقل يخرج عن `default`**. وما لا يخرج عنه
 * لا يملك مقبضًا، فلا جلسة ولا التقاط ولا تراجع. وهذا **تضييق لا توسيع**: كل ما كان يُطبَّق
 * فعلًا (حقل مُعدَّل) يبقى يُطبَّق بلا تغيير.
 *
 * **وحدّه المُعلن:** الحقول المعروفة وحدها تُقرأ (`KNOB_KEYS`)؛ مفتاح أجنبيّ أو أحدث لا
 * يُعدّ طلبًا — لأن كتابةً لم تُفهم لا يجوز أن تفتح جلسة عتاد. والدالّة لا تقرأ ملفًا ولا
 * تعرف أندرويد، فهي قابلة للقياس في JVM.
 */
object PerAppSessionRules {

    /**
     * الحقول التي يعني `default` فيها «لم أطلب شيئًا».
     *
     * مطابقة لمفاتيح [nd.max.ui.util.AppConfig] — ويُقاس التطابق في `PerAppSessionRulesTest`
     * حتى لا يُضاف حقل هناك ويُنسى هنا فتصير الجلسة صامتة عن طلب حقيقي.
     */
    val KNOB_KEYS: Set<String> = setOf(
        "perf_lite_mode",
        "app_priority",
        "game_preload",
        "cpu_boost",
        "cpu_policy_controls",
        "gpu_profile",
        "cpu_governor",
        "gpu_governor",
        "gpu_max_freq",
        "thermal_profile",
        "refresh_rate",
        "renderer",
        "resolution_downscale",
        "dnd_on_gaming",
        "bypass_charging",
        "touch_boost",
        "haptic_feedback",
        "kill_bg_apps",
        "force_hw_ui",
        "disable_notifs",
        "wifi_no_sleep",
    )

    /** ما لا يُعدّ طلبًا: القيمة الغائبة والفارغة والافتراضية. */
    private val NEUTRAL = setOf("", "default", "null")

    private val ENTRY = Regex("\"([A-Za-z0-9_]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\n]*))")

    /**
     * هل في كتلة إعداد تطبيق واحد حقلٌ صريح؟
     *
     * @param block محتوى كائن JSON للتطبيق (بلا الأقواس) كما يقرؤه `AppMonitor`.
     */
    fun hasExplicitOverride(block: String?): Boolean = explicitOverrideCount(block) > 0

    /** عدد الحقول الصريحة — للعرض والتشخيص، ويُشتقّ منه القرار نفسه. */
    fun explicitOverrideCount(block: String?): Int {
        if (block.isNullOrBlank()) return 0
        return ENTRY.findAll(block).count { match ->
            if (match.groupValues[1] !in KNOB_KEYS) return@count false
            // السلسلة المقتبسة إن وُجدت، وإلا القيمة العارية (`true` · `0` · رقم كسر).
            // و`cpu_policy_controls` الفارغة تُقرأ هنا كـ`default`: لا سياسة مطلوبة.
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }.trim()
            value !in NEUTRAL
        }
    }
}
