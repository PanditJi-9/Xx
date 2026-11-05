/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.thermal

/**
 * عيّنة واحدة من تاريخ الحرارة: الزمن، والمنطقة الأسخن المقروءة، ودرجتها.
 *
 * **ولماذا المنطقة معها لا الدرجة وحدها:** «٧٨°» لا تقول أيّ منطقة بلغتها؛ والمقارنة بين
 * جلسة وأخرى تحتاج أن يُعرف أن السخونة كانت في `cpu-0` لا في `battery`. والاسم كما يعرضه
 * النظام، لا اسمًا من عندنا.
 */
data class ThermalSessionSample(
    val atMs: Long,
    val zone: String,
    val celsius: Int
)

/**
 * قواعد تاريخ الحرارة — خالصة، بلا `Context` ولا `Compose`، فتُختبر مباشرة.
 *
 * **وحدّها المعلن:** هذه لمحة عن الجلسة الحالية في الذاكرة، لا سجلّ دائم. عشر دقائق تقريبًا
 * (١٠٠ عيّنة × ٣ ثوانٍ) تكفي لرؤية منحدر التخفيف بعد مهمّة ثقيلة، ولا تملأ القرص بملفّ
 * يُكتب كل ثلاث ثوانٍ.
 */
object ThermalHistory {

    /** أقصى عدد عيّنات محفوظة: ١٠٠ × ٣ ثوانٍ = خمس دقائق تقريبًا. */
    const val CAPACITY = 100

    /** ترويسة CSV ثابتة: تُقرأ بآلة، فلا تُترجم ولا يتغيّر ترتيب أعمدتها. */
    const val CSV_HEADER = "elapsed_ms,zone,temp_c"

    /** يضيف عيّنة ويقصّ النافذة من البداية — الأحدث هو ما يبقى. */
    fun push(
        current: List<ThermalSessionSample>,
        sample: ThermalSessionSample
    ): List<ThermalSessionSample> = (current + sample).takeLast(CAPACITY)

    /**
     * الجلسة ملفًّا: الزمن نسبةً إلى أول عيّنة، ثم المنطقة، ثم الدرجة.
     *
     * **والزمن نسبيّ لا مطلق:** الجلسة تُقارن بمنحنى، لا بتقويم؛ وطابع زمني حقيقي في كل سطر
     * يجعل الملفّ أطول بلا معلومة إضافية عن الفاصل بين العيّنات.
     *
     * **والمنطقة تُهرَّب:** أسماء المناطق تأتي من النواة، وقد تحوي فاصلة (`cpu-0-1` لا، لكن
     * `mtk-ts,0` وارد) — فحقل بفاصلة بلا تنصيص يقلب الأعمدة صامتًا.
     */
    fun csv(samples: List<ThermalSessionSample>): String {
        val out = StringBuilder(CSV_HEADER.length + 16 + samples.size * 48)
        out.append(CSV_HEADER).append('\n')
        if (samples.isEmpty()) return out.toString()
        val origin = samples.first().atMs
        samples.forEach { sample ->
            out.append(sample.atMs - origin)
            out.append(',').append(escape(sample.zone))
            out.append(',').append(sample.celsius)
            out.append('\n')
        }
        return out.toString()
    }

    /**
     * المدّة المغطّاة بالثواني.
     *
     * **ولا تُحسب من العدد:** عدد العيّنات × ٣ يفترض أن كل دورة أخذت ثلاث ثوانٍ بالضبط؛
     * والدورة الحقيقية تقرأ ملفات sysfs وقد تتأخّر. فتُقاس من الزمن نفسه حين توجد عيّنتان
     * على الأقل — وتُقدَّر بالفاصل فقط قبل ذلك.
     */
    fun windowSeconds(samples: List<ThermalSessionSample>, intervalMs: Long): Int {
        val step = (intervalMs / 1000L).toInt().coerceAtLeast(0)
        if (samples.size < 2) return samples.size * step
        val span = (samples.last().atMs - samples.first().atMs) / 1000L
        return span.toInt().coerceAtLeast(0)
    }

    /** حقل CSV: يُنصَّص فقط حين يلزم، ويُهرَّب التنصيص بمضاعفته. */
    private fun escape(field: String): String {
        if (field.isEmpty()) return ""
        val needsQuotes = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
