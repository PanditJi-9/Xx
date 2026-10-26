package nd.max.core.gamespace

import nd.max.core.privilege.PrivilegedShell
import kotlin.math.roundToInt

/** إعداد مسبق لدقّة الشاشة: العرض والارتفاع بالبكسل والكثافة، و`id` ثابت يعرفه باقي التطبيق. */
data class ScalePreset(val id: String, val widthPx: Int, val heightPx: Int, val dpi: Int)

/** قراءة الشاشة الآن: الأصل من العتاد، والفعّال بعد أيّ تجاوز (`wm size` و`wm density`). */
data class DisplayReading(
    val nativeW: Int,
    val nativeH: Int,
    val nativeDpi: Int,
    val activeW: Int,
    val activeH: Int,
    val activeDpi: Int,
) {
    /** هل يوجد تجاوز فعلي يختلف عن الأصل؟ */
    val overridden: Boolean
        get() = activeW != nativeW || activeH != nativeH || activeDpi != nativeDpi
}

/**
 * مقياس اللوحة للعبة: ثلاثة إعدادات مسبقة مشتقّة من الدقّة الأصلية بالنِّسَب نفسها في شاشة
 * «مقياس اللوحة» في الإعدادات (٨٠٪ متوازن · ٦٠٪ أداء · ٤٥٪ توفير).
 *
 * **جلسة لا دائم:** لا يكتب شيئًا في الخصائص المحفوظة، فلا يبقى التغيير بعد اللعبة. وتُعاد الشاشة
 * إلى ما كانت عليه قبل أوّل تغيير ([restore]) حين تغادر اللعبة إلى تطبيق آخر أو تُوقف الخدمة.
 */
object PanelResolution {
    private val PHYSICAL_SIZE = Regex("Physical size:\\s*(\\d+)x(\\d+)")
    private val OVERRIDE_SIZE = Regex("Override size:\\s*(\\d+)x(\\d+)")
    private val PHYSICAL_DENSITY = Regex("Physical density:\\s*(\\d+)")
    private val OVERRIDE_DENSITY = Regex("Override density:\\s*(\\d+)")

    /** الدقّة الحالية عبر المسار الموحَّد، أو `null` إن لم يُقرأ الأصل (أو رُفض الأمر في الوضع المختار). */
    suspend fun read(): DisplayReading? {
        val outcome = PrivilegedShell.exec("wm size; wm density") as? PrivilegedShell.Outcome.Ran ?: return null
        return parse(outcome.output)
    }

    /** تحليل خَرْج `wm size` و`wm density` — **نقيّ** فيُقاس على JVM. */
    fun parse(out: String): DisplayReading? {
        val physical = PHYSICAL_SIZE.find(out) ?: return null
        val density = PHYSICAL_DENSITY.find(out) ?: return null
        val nw = physical.groupValues[1].toIntOrNull() ?: return null
        val nh = physical.groupValues[2].toIntOrNull() ?: return null
        val nd = density.groupValues[1].toIntOrNull() ?: return null
        if (nw <= 0 || nh <= 0 || nd <= 0) return null
        val override = OVERRIDE_SIZE.find(out)
        val overrideDensity = OVERRIDE_DENSITY.find(out)
        return DisplayReading(
            nativeW = nw,
            nativeH = nh,
            nativeDpi = nd,
            activeW = override?.groupValues?.get(1)?.toIntOrNull() ?: nw,
            activeH = override?.groupValues?.get(2)?.toIntOrNull() ?: nh,
            activeDpi = overrideDensity?.groupValues?.get(1)?.toIntOrNull() ?: nd,
        )
    }

    /** الإعدادات المسبقة الأربعة من الدقّة الأصلية؛ العرض زوجيّ دائمًا ليكون صديقًا للمعالج الرسومي. */
    fun presets(r: DisplayReading): List<ScalePreset> {
        fun scaled(id: String, fraction: Float): ScalePreset {
            val w = (r.nativeW * fraction).roundToInt().let { it - (it % 2) }
            val h = (w.toFloat() / r.nativeW * r.nativeH).roundToInt()
            val d = (r.nativeDpi * fraction).roundToInt()
            return ScalePreset(id, w, h, d)
        }
        return listOf(
            ScalePreset("native", r.nativeW, r.nativeH, r.nativeDpi),
            scaled("balanced", 0.8f),
            scaled("performance", 0.6f),
            scaled("battery_saver", 0.45f),
        )
    }

    /** معرّف الإعداد الحالي: `native` إن لم يوجد تجاوز، ومعرّف المسبق المطابق، وإلا `custom`. */
    fun activeId(r: DisplayReading): String {
        if (!r.overridden) return "native"
        return presets(r).firstOrNull { p ->
            p.widthPx == r.activeW && p.heightPx == r.activeH && p.dpi == r.activeDpi
        }?.id ?: "custom"
    }

    /** يطبّق إعدادًا مسبقًا في الجلسة الحالية؛ `native` يُعيد الأصل. يعيد `true` إن نُفِّذ الأمر ونجح. */
    suspend fun apply(p: ScalePreset): Boolean =
        execute(if (p.id == "native") RESET else "wm size ${p.widthPx}x${p.heightPx}; wm density ${p.dpi}")

    /** يعيد الشاشة إلى ما كانت عليه قبل الجلسة: تجاوزها الأصلي إن وُجد، وإلا الأصل. */
    suspend fun restore(before: DisplayReading): Boolean =
        execute(if (!before.overridden) RESET else "wm size ${before.activeW}x${before.activeH}; wm density ${before.activeDpi}")

    private const val RESET = "wm size reset; wm density reset"

    private suspend fun execute(command: String): Boolean =
        (PrivilegedShell.exec(command) as? PrivilegedShell.Outcome.Ran)?.ok == true
}
