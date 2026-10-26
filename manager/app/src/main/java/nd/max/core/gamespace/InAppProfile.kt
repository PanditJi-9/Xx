/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.privilege.ExecutionMode
import nd.max.core.privilege.ExecutionModeRules
import nd.max.core.privilege.PrivilegedShellRules
import nd.max.ui.util.AppConfig

/**
 * بروفايل اللعبة **داخل التطبيق** (بلا رفيق الجذر): المجموعة الجزئية التي تُنفَّذ كلها بـ`settings put` عبر
 * المسار الموحَّد، فتعمل بجذر أو Shizuku. الدلالات منقولة من `AppMonitor` حرفيًّا (قراءة 2026-10-25):
 *
 * - `dnd_on_gaming == "true"` ⇒ `zen_mode=1` **فقط إن كان الصفر الآن** (لا يُخفَّض نمط أقوى اختاره المستخدم).
 * - `refresh_rate` عدد صحيح بالهرتز (غير `default`) ⇒ يُقرَّب إلى أقرب معدّل تدعمه الشاشة، ويُكتب في المفاتيح
 *   الأربعة التي يكتبها `PerAppRefreshRateController` (مفتاحا شاومي + `peak`/`min`).
 * - `haptic_feedback == "true"` ⇒ `haptic_feedback_enabled=0`.
 *
 * وكل ما عدا ذلك (حوكمة وحدات المعالج/الرسوميات، البايباس، `wifi_no_sleep`، كتم الإشعارات…) خارج هذه
 * المجموعة ويبقى لرفيق الجذر؛ ولا يُوعَد به في الوضعين غير الجذريين.
 */
enum class InAppKnob { DND, REFRESH_RATE, HAPTICS }

data class SettingRef(val namespace: String, val key: String)

data class SettingWrite(val ref: SettingRef, val value: String)

/** قيمة مفتاح قبل التعديل: [value] = `null` تعني «غير مضبوط» (يُعاد بـ`settings delete`). */
data class SavedSetting(val ref: SettingRef, val value: String?)

data class InAppWants(val dnd: Boolean, val refreshHz: Int?, val reduceHaptics: Boolean) {
    val any: Boolean get() = dnd || refreshHz != null || reduceHaptics
}

data class InAppPlan(
    val writes: List<SettingWrite>,
    val saved: List<SavedSetting>,
    val skipped: Map<InAppKnob, String>,
)

/** قواعد البروفايل الداخلي — **نقيّة**: لا صدفة ولا سياق، فتُقاس على JVM. */
object InAppProfileRules {
    val ZEN = SettingRef("global", "zen_mode")
    val HAPTIC = SettingRef("system", "haptic_feedback_enabled")
    val REFRESH_REFS = listOf(
        SettingRef("secure", "user_refresh_rate"),
        SettingRef("secure", "miui_refresh_rate"),
        SettingRef("system", "peak_refresh_rate"),
        SettingRef("system", "min_refresh_rate"),
    )

    private val SAFE_VALUE = Regex("^[A-Za-z0-9_.,:\\-]{0,64}$")
    private val SAFE_KEY = Regex("^[A-Za-z0-9_.]{1,64}$")
    private val NAMESPACES = setOf("global", "secure", "system")

    /**
     * هل هذا المطبِّق هو المسؤول؟ حين يكون مسار الأوامر في الوضع جذرًا فرفيق الجذر (`AppMonitor`) هو من يطبّق
     * ولا يُزاحَم؛ وغير ذلك (Shizuku · Basic · لا جذر) يطبّقه التطبيق نفسه.
     */
    fun isActive(mode: ExecutionMode, rootGranted: Boolean): Boolean =
        ExecutionModeRules.lane(mode, rootGranted, shizukuReady = false) != PrivilegedShellRules.Lane.ROOT

    fun wants(config: AppConfig, supportedRates: List<Int>): InAppWants = InAppWants(
        dnd = config.dnd_on_gaming == "true",
        refreshHz = config.refresh_rate
            .takeIf { it.isNotBlank() && it != "default" }
            ?.toFloatOrNull()?.toInt()
            ?.let { normalizeHz(it, supportedRates) },
        reduceHaptics = config.haptic_feedback == "true",
    )

    /** أقرب معدّل مدعوم، أو المطلوب نفسه إن لم تُعرف المعدّلات، أو `null` إن لم يصحّ الطلب. */
    fun normalizeHz(requested: Int, supported: List<Int>): Int? {
        if (requested <= 0) return null
        if (supported.isEmpty()) return requested
        return supported.minByOrNull { kotlin.math.abs(it - requested) }
    }

    /** المفاتيح التي يلزم أخذ قيمتها الحالية قبل أي كتابة. */
    fun refsFor(wants: InAppWants): List<SettingRef> = buildList {
        if (wants.dnd) add(ZEN)
        if (wants.refreshHz != null) addAll(REFRESH_REFS)
        if (wants.reduceHaptics) add(HAPTIC)
    }

    /**
     * الخطة من القيم الحالية [before] (غياب المفتاح من الخريطة = تعذّرت قراءته، و`null` = غير مضبوط).
     * **لا كتابة بلا لقطة قابلة للاسترجاع:** قيمة غير آمنة للاقتباس أو غير مقروءة ⇒ يُتخطّى المقبض كله ويُقال السبب.
     */
    fun plan(wants: InAppWants, before: Map<SettingRef, String?>): InAppPlan {
        val writes = ArrayList<SettingWrite>()
        val saved = ArrayList<SavedSetting>()
        val skipped = LinkedHashMap<InAppKnob, String>()

        fun restorable(ref: SettingRef): Boolean =
            ref in before && (before[ref]?.let(::isSafeValue) ?: true)

        if (wants.dnd) {
            // مفتاح غير مضبوط في `zen_mode` = الصفر (الافتراضي)، لا «مجهول».
            val zen = if (ZEN !in before) null else (before[ZEN]?.toIntOrNull() ?: if (before[ZEN] == null) 0 else null)
            when {
                zen == null -> skipped[InAppKnob.DND] = "zen-unreadable"
                zen != 0 -> skipped[InAppKnob.DND] = "zen-mode-already-on"
                !restorable(ZEN) -> skipped[InAppKnob.DND] = "snapshot-unsafe"
                else -> {
                    writes += SettingWrite(ZEN, "1")
                    saved += SavedSetting(ZEN, before[ZEN])
                }
            }
        }
        val hz = wants.refreshHz
        if (hz != null) {
            if (REFRESH_REFS.all(::restorable)) {
                for (ref in REFRESH_REFS) {
                    writes += SettingWrite(ref, hz.toString())
                    saved += SavedSetting(ref, before[ref])
                }
            } else {
                skipped[InAppKnob.REFRESH_RATE] = "snapshot-unreadable"
            }
        }
        if (wants.reduceHaptics) {
            when {
                !restorable(HAPTIC) -> skipped[InAppKnob.HAPTICS] = "snapshot-unreadable"
                before[HAPTIC] == "0" -> skipped[InAppKnob.HAPTICS] = "already-off"
                else -> {
                    writes += SettingWrite(HAPTIC, "0")
                    saved += SavedSetting(HAPTIC, before[HAPTIC])
                }
            }
        }
        return InAppPlan(writes, saved, skipped)
    }

    fun isSafeValue(value: String): Boolean = SAFE_VALUE.matches(value)

    fun getCommand(ref: SettingRef): String = "settings get ${ref.namespace} '${ref.key}'"

    fun putCommand(ref: SettingRef, value: String): String = "settings put ${ref.namespace} '${ref.key}' '$value'"

    fun deleteCommand(ref: SettingRef): String = "settings delete ${ref.namespace} '${ref.key}'"

    /** أوامر الاسترجاع بترتيب اللقطة: `null` ⇒ حذف المفتاح، وغيره ⇒ إعادة قيمته. */
    fun restoreCommands(saved: List<SavedSetting>): List<String> =
        saved.map { if (it.value == null) deleteCommand(it.ref) else putCommand(it.ref, it.value) }

    /** خَرْج `settings get`: الفارغ أو الكلمة `null` ⇒ غير مضبوط. */
    fun readValue(rawLine: String?): String? = rawLine?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    // ── اللقطة المحفوظة (تنجو من موت العملية: ما أُجبِر على قيمة يجب أن يُعاد ولو انهارت الخدمة) ──

    fun encodeSaved(pkg: String, saved: List<SavedSetting>): String = buildString {
        append("pkg\t").append(pkg).append('\n')
        for (item in saved) {
            append("s\t").append(item.ref.namespace).append('\t').append(item.ref.key).append('\t')
            append(if (item.value == null) "-" else "+" + item.value).append('\n')
        }
    }

    /** `null` إن فسد النص أو حمل ما لا يُقتبس بأمان: لا يُنفَّذ أمر مبنيّ على نصّ مشبوه. */
    fun decodeSaved(text: String): Pair<String, List<SavedSetting>>? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val header = lines.firstOrNull()?.split('\t') ?: return null
        if (header.size != 2 || header[0] != "pkg") return null
        val items = ArrayList<SavedSetting>()
        for (line in lines.drop(1)) {
            val parts = line.split('\t')
            if (parts.size != 4 || parts[0] != "s") return null
            val (namespace, key, encoded) = Triple(parts[1], parts[2], parts[3])
            if (namespace !in NAMESPACES || !SAFE_KEY.matches(key)) return null
            val value = when {
                encoded == "-" -> null
                encoded.startsWith("+") -> encoded.substring(1).also { if (!isSafeValue(it)) return null }
                else -> return null
            }
            items += SavedSetting(SettingRef(namespace, key), value)
        }
        return header[1] to items
    }
}
