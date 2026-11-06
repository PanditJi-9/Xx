/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.net

/**
 * تحويل تردّدات Wi-Fi إلى قنوات، وتلخيص ما رآه آخر مسح.
 *
 * **ولماذا حسابٌ نقيّ:** التردّد رقم تعطيه المنصّة (`WifiInfo.getFrequency()` ونتائج المسح)،
 * أمّا «قناة ٦» و«موجة ٢٫٤» فاشتقاقٌ منه بقاعدة ثابتة. فصلُه يعني أن خطأ القناة على
 * ٥ غيغاهرتز — حيث الإزاحة ليست ٢٤٠٧ كما في ٢٫٤ — يُكشف في اختبار لا على هاتف.
 *
 * **وحدوده المُعلنة:** لا حكم على «أفضل قناة». عدد الشبكات على قناة ليس ازدحامًا بالمعنى
 * الهندسي (عرض القناة والعرض المتقاطع والتداخل خارج الحساب) — فيُعرض العدد كما قِيس،
 * ولا يُوصى بقناةٍ لم تُقَس.
 */
object WifiChannel {

    enum class Band { TwoFourGhz, FiveGhz, SixGhz, Unknown }

    data class Reading(val frequencyMhz: Int, val channel: Int, val band: Band)

    data class ChannelCount(val channel: Int, val band: Band, val networks: Int)

    data class Summary(
        val total: Int,
        val twoFour: Int,
        val five: Int,
        val six: Int,
        val busiest: List<ChannelCount>,
        val connected: Reading?,
    )

    /** تردّدات ٢٫٤ غيغاهرتز الفعلية؛ ٢٤٨٤ قناة ١٤ اليابانية، وخارجها لا قناة. */
    private const val TWO_FOUR_START = 2412
    private const val TWO_FOUR_END = 2472
    private const val CHANNEL_14_MHZ = 2484
    private const val TWO_FOUR_BASE = 2407

    private const val FIVE_START = 5000
    private const val FIVE_END = 5895
    private const val FIVE_BASE = 5000
    private const val FIVE_FIRST = 32
    private const val FIVE_LAST = 196

    private const val SIX_START = 5955
    private const val SIX_END = 7115
    private const val SIX_BASE = 5950

    fun bandOf(frequencyMhz: Int): Band = when (frequencyMhz) {
        in TWO_FOUR_START..CHANNEL_14_MHZ -> Band.TwoFourGhz
        in FIVE_START..FIVE_END -> Band.FiveGhz
        in SIX_START..SIX_END -> Band.SixGhz
        else -> Band.Unknown
    }

    /**
     * رقم القناة، أو `null` لتردّد لا يقع على قناة معروفة.
     *
     * ولا يُدوَّر إلى أقرب قناة: التردّد الذي لا يُطابق شبكةً قياسية (`2400` مثلًا) يعني
     * قراءةً لا نفهمها، وردّه إلى «قناة ١» يخترع معلومة.
     */
    fun channelOf(frequencyMhz: Int): Int? = when (bandOf(frequencyMhz)) {
        Band.TwoFourGhz -> when (frequencyMhz) {
            CHANNEL_14_MHZ -> 14
            in TWO_FOUR_START..TWO_FOUR_END -> (frequencyMhz - TWO_FOUR_BASE) / 5
            else -> null
        }
        // الأقواس حول القسمة لازمة: `5.takeIf` بلا قوسٍ يقرأ `(f - base) / (5.takeIf …)`.
        Band.FiveGhz -> ((frequencyMhz - FIVE_BASE) / 5)
            .takeIf { it in FIVE_FIRST..FIVE_LAST && frequencyMhz % 5 == 0 }
        Band.SixGhz -> ((frequencyMhz - SIX_BASE) / 5)
            .takeIf { it in 1..233 && frequencyMhz % 5 == 0 }
        Band.Unknown -> null
    }

    fun reading(frequencyMhz: Int): Reading? {
        val channel = channelOf(frequencyMhz) ?: return null
        return Reading(frequencyMhz, channel, bandOf(frequencyMhz))
    }

    /**
     * تلخيص ما رآه المسح: العدّ الكلّي، وعدّ الموجات، والقنوات مرتّبةً بعدد الشبكات.
     *
     * والترتيب تنازليّ ثم برقم القناة تصاعديًّا — فالقناة الأقلّ ازدحامًا تظهر أولًا عند
     * التساوي، بلا أن يُقال «هي الأفضل».
     */
    fun summarise(frequencies: List<Int>, connectedFrequencyMhz: Int? = null): Summary {
        val readings = frequencies.mapNotNull(::reading)
        val counts = readings.groupingBy { it.channel to it.band }.eachCount()
        val busiest = counts.entries
            .map { ChannelCount(it.key.first, it.key.second, it.value) }
            .sortedWith(compareByDescending<ChannelCount> { it.networks }.thenBy { it.channel })
        return Summary(
            total = readings.size,
            twoFour = readings.count { it.band == Band.TwoFourGhz },
            five = readings.count { it.band == Band.FiveGhz },
            six = readings.count { it.band == Band.SixGhz },
            busiest = busiest,
            connected = connectedFrequencyMhz?.let(::reading),
        )
    }
}
