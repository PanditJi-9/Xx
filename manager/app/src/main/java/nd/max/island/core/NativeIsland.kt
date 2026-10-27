/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الأصلية في النظام (HyperIsland / ColorOS Live Alerts) — **تشخيص لا تحكّم أعمى**.
 *
 * ### لماذا لا يوجد مفتاح «عطّل الجزيرة الأصلية» هنا
 *
 * لا يوجد في Android API عامّ يوقف الجزيرة الأصلية، ولا مفتاح `Settings` موثّق لها. والكتابة
 * في صفوف تخمينية، أو تعطيل مكوّن SystemUI، أو قتله — كلّها مخالفة صريحة للطلب (§Native
 * System Dynamic Island: «Never delete or modify system applications, disable essential
 * System UI components, kill System UI»). ولذلك لا يُنفّذ التطبيق إجراءً لم يُتحقّق منه على
 * جهاز: **القدرة تُقاس، والحدّ يُقال صراحةً**، ولا يُدّعى إيقاف لم يحدث.
 *
 * ### وما يُقدَّم بدلًا منه
 *
 * - كشف صادق: هل هذا الـROM معروف بأنه يشحن جزيرة أصلية؟
 * - لا مساس بأي مكوّن نظام.
 * - بديل متوافق: مفاتيح «ما تعرضه الجزيرة» لكل نشاط، فيُمنع التكرار من طرفنا بلا لمس النظام.
 *
 * والقواعد هنا نقيّة (بلا أندرويد) لتُختبر على JVM، والقراءة الفعلية في [NativeIslandProbe].
 */
package nd.max.island.core

/** حقائق ROM المقروءة من `Build` ومن وجود حزم النظام — لا من مفتاح مخزَّن. */
data class NativeIslandFacts(
    val manufacturer: String,
    val brand: String,
    val sdk: Int,
    val systemPackages: Set<String> = emptySet(),
)

/** سبب عدم القدرة على التحكّم. يُترجم في الواجهة، ولا يُدّعى غير ذلك. */
enum class NativeIslandReason {
    /** لا جزيرة أصلية معروفة على هذا الـROM. */
    NO_NATIVE_ISLAND,

    /** الـROM يشحن جزيرة، لكن لا واجهة موثّقة قابلة للرجوع. */
    NO_DOCUMENTED_SWITCH,

    /** لا شيء يُلمس في SystemUI؛ الحماية قرار لا عجز. */
    SYSTEM_UI_PROTECTED,
}

data class NativeIslandVerdict(
    val nativeLikely: Boolean,
    val controllable: Boolean,
    val reason: NativeIslandReason,
    /** اسم العائلة كما يُعرض للمستخدم، أو `null` إن لم تُعرف. */
    val vendor: String?,
)

object NativeIslandRules {
    private val xiaomi = setOf("xiaomi", "redmi", "poco")
    private val oppo = setOf("oppo", "oneplus", "realme")
    private val vivo = setOf("vivo", "iqoo")
    private val honor = setOf("honor", "huawei")
    private val samsung = setOf("samsung")

    private val knownPackages = mapOf(
        "Xiaomi / Redmi / POCO" to setOf("com.miui.systemui", "com.miui.home"),
        "OPPO / OnePlus / realme" to setOf("com.oplus.systemui", "com.coloros.systemui"),
        "vivo / iQOO" to setOf("com.vivo.systemui"),
        "HONOR / Huawei" to setOf("com.hihonor.systemui", "com.huawei.systemui"),
    )

    fun classify(facts: NativeIslandFacts): NativeIslandVerdict {
        val maker = facts.manufacturer.trim().lowercase()
        val brand = facts.brand.trim().lowercase()
        fun known(set: Set<String>) = maker in set || brand in set

        // أندرويد ١٠ وما قبله لا يشحن هذه الواجهات عمليًّا؛ ولا نبني حكمًا على إصدار أحدث.
        if (facts.sdk < 30) {
            return NativeIslandVerdict(false, false, NativeIslandReason.NO_NATIVE_ISLAND, null)
        }

        val vendor = when {
            known(xiaomi) -> "Xiaomi / Redmi / POCO"
            known(oppo) -> "OPPO / OnePlus / realme"
            known(vivo) -> "vivo / iQOO"
            known(honor) -> "HONOR / Huawei"
            known(samsung) -> "Samsung"
            else -> knownPackages.entries.firstOrNull { (_, pkgs) ->
                facts.systemPackages.any { it in pkgs }
            }?.key
        }

        if (vendor == null) {
            return NativeIslandVerdict(false, false, NativeIslandReason.NO_NATIVE_ISLAND, null)
        }
        return NativeIslandVerdict(
            nativeLikely = true,
            controllable = false,
            reason = NativeIslandReason.NO_DOCUMENTED_SWITCH,
            vendor = vendor,
        )
    }
}
