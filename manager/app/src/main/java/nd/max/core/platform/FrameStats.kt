/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * توزيع زمن الإطار الحقيقي من `dumpsys gfxinfo <pkg> framestats`.
 *
 * **ولماذا هذا الملفّ، وما الفرق عن [FrameWindow]:** `FrameWindow` يعيّن معدّل الإطارات في اللحظة
 * ويقول بصراحة إنّه **ليس** زمن إطار. أمّا هنا فيُقرأ الطابع الزمني لكل إطار من حلقة الرسم نفسها
 * (`IntendedVsync` ← `FrameCompleted`)، فالمئين والتقطيع رقمان مقيسان لا مُشتقّان من معدّل.
 *
 * **وحدوده:** `gfxinfo` يخصّ **الحزمة الأمامية** لا الشاشة كلها، ولا يُعيد إطارات لعبة Vulkan
 * خارج مسار HWUI بنفس التفصيل على كل بناء. فغياب القسم أو نقص الأعمدة يعيد `null` — لا صفرًا
 * ولا توزيعًا مُخترعًا.
 */
data class FrameStats(
    /** عدد الإطارات الصالحة التي دخلت الحساب. */
    val frames: Int,
    /** وسيط زمن الإطار بالميلي ثانية. */
    val p50Ms: Float,
    /** المئين ٩٠ — الذيل الذي يبدأ عنده التقطيع. */
    val p90Ms: Float,
    /** المئين ٩٩ — أسوأ ١٪ تقريبًا. */
    val p99Ms: Float,
    /** نسبة الإطارات التي تجاوزت ميزانية إطار واحد. */
    val jankPercent: Int,
    /** عدد الإطارات البطيئة نفسها — يُعرض مع النسبة كي لا تُخفي عيّنةً صغيرة. */
    val slowFrames: Int,
)

/** محلّل نقيّ — يُقاس على JVM بلا جهاز، ويفصل الصيغة عن تنفيذ الصدفة. */
object FrameStatsParser {

    private const val PROFILEDATA = "---PROFILEDATA---"

    /**
     * @param raw مخرَج `dumpsys gfxinfo <pkg> framestats` كما هو.
     * @param refreshHz معدّل تحديث الشاشة؛ صفر أو غير منتهٍ يعني ميزانية ٦٠ هرتز.
     */
    fun parse(raw: String?, refreshHz: Float): FrameStats? {
        if (raw.isNullOrBlank()) return null
        val lines = raw.lineSequence().map { it.trim() }.toList()
        val start = lines.indexOf(PROFILEDATA)
        if (start < 0) return null

        val headerIndex = (start + 1 until lines.size).firstOrNull { lines[it].isNotEmpty() } ?: return null
        val header = lines[headerIndex].split(',')
        val intended = header.indexOf("IntendedVsync")
        val completed = header.indexOf("FrameCompleted")
        if (intended < 0 || completed < 0) return null

        val budgetMs = if (refreshHz.isFinite() && refreshHz > 0f) 1_000f / refreshHz else 1_000f / 60f
        val durations = ArrayList<Float>(128)
        var slow = 0

        for (index in headerIndex + 1 until lines.size) {
            val line = lines[index]
            if (line.isEmpty() || line == PROFILEDATA) break
            val columns = line.split(',')
            val beginNs = columns.getOrNull(intended)?.toLongOrNull() ?: continue
            val endNs = columns.getOrNull(completed)?.toLongOrNull() ?: continue
            if (beginNs <= 0L || endNs <= beginNs) continue
            val durationMs = (endNs - beginNs) / 1_000_000f
            // إطار أطول من ثانية ليس قياسًا: غالبًا طوابع غير مهيّأة أو إطار معلّق.
            if (durationMs <= 0f || durationMs > 1_000f) continue
            durations.add(durationMs)
            if (durationMs > budgetMs) slow++
        }
        if (durations.isEmpty()) return null

        val sorted = durations.sorted()
        return FrameStats(
            frames = sorted.size,
            p50Ms = percentile(sorted, 0.50f),
            p90Ms = percentile(sorted, 0.90f),
            p99Ms = percentile(sorted, 0.99f),
            jankPercent = (slow * 100f / sorted.size).roundToInt(),
            slowFrames = slow,
        )
    }

    /** رتبة أقرب — نفس قاعدة [FrameWindow.percentile] كي لا يختلف مئينان في الشاشة نفسها. */
    private fun percentile(sorted: List<Float>, p: Float): Float {
        val rank = ceil(p * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }
}
