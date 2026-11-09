/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

/**
 * استهلاك الطاقة لكل تطبيق من تفريغ المنصّة (`dumpsys batterystats`) — **قراءة وتحليل فقط**.
 *
 * والحدّ الصادق أولًا: هذا **تقدير المنصّة** لا قياسُنا. يوزّع أندرويد الطاقة على المكوّنات
 * (شاشة، راديو، معالج…) وينسب حصّةً لكل UID، والتفريغ يقول ذلك بصيغته. فلا يُعرض الرقم كأنه
 * قراءة مباشرة من عدّاد عتاد، ويُقال مصدره في الواجهة.
 *
 * **ولماذا لا يُقرأ تلقائيًّا عند فتح الشاشة:** التفريغ الكامل ثقيل (ميغابايتات نصًّا على أجهزة
 * كثيرة)، وفتح شاشة البطارية لا يستحقّ هذه الكلفة. فيُطلب بضغطة صريحة، ويُقصّ بـ`grep` إلى
 * قسم واحد قبل أن يصل إلى الذاكرة.
 */
object BatteryUidPower {

    /** UID واحد وحصّته بالملّي أمبير/ساعة كما قدّرتها المنصّة. */
    data class Entry(val uid: Int, val milliampHours: Float)

    /**
     * ما قُرئ: مدخلات مرتّبة تنازليًّا، ومجموعها.
     *
     * و[totalMilliampHours] مجموع **ما قُرئ** لا استهلاك الجهاز كلّه: أندرويد يحسب مكوّنات
     * غير منسوبة لتطبيق (شاشة، راديو)، فالمجموع أصغر من التفريغ الفعليّ بطبيعته — ويُسمّى
     * في الواجهة «مجموع المقيس» لا «الاستهلاك».
     */
    data class Reading(val entries: List<Entry>, val totalMilliampHours: Float)

    private val UID_LINE = Regex("""^\s*[Uu][Ii][Dd]\s+([A-Za-z0-9]+):\s*([0-9]+(?:\.[0-9]+)?)""")
    private val APP_UID = Regex("""^u(\d+)a(\d+)$""")
    private val ISOLATED_UID = Regex("""^u(\d+)i(\d+)$""")
    private const val PER_USER_RANGE = 100_000
    private const val APP_ID_OFFSET = 10_000
    private const val ISOLATED_ID_START = 99_000

    private fun userUid(user: Int, appId: Int): Int? {
        if (user < 0 || appId < 0 || user > 999) return null
        return user * PER_USER_RANGE + appId
    }

    /** `u0a123` → 10123 · `u10a5` → 1010005 · `1000` → 1000 · ما لا يُفهم يُسقَط. */
    fun decodeUid(token: String): Int? {
        token.trim().toIntOrNull()?.let { return it.takeIf { value -> value >= 0 } }
        APP_UID.find(token.trim())?.let { match ->
            val user = match.groupValues[1].toIntOrNull() ?: return null
            val appId = match.groupValues[2].toIntOrNull() ?: return null
            return userUid(user, APP_ID_OFFSET + appId)
        }
        ISOLATED_UID.find(token.trim())?.let { match ->
            val user = match.groupValues[1].toIntOrNull() ?: return null
            val isolated = match.groupValues[2].toIntOrNull() ?: return null
            return userUid(user, ISOLATED_ID_START + isolated)
        }
        return null
    }

    /**
     * يحلّل قسم «Estimated power use» وحده.
     *
     * `null` حين لا يوجد القسم أو لا يحمل سطر UID صالحًا: التفريغ قد ينجح ويعود بقسم فارغ على
     * نواة لا تحسب الطاقة، و«صفر استهلاك» جواب كاذب — فالغياب يُعلَن غيابًا.
     */
    fun parse(text: String): Reading? {
        var inSection = false
        val byUid = LinkedHashMap<Int, Float>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trimEnd()
            if (!inSection) {
                if (line.contains("Estimated power use", ignoreCase = true)) inSection = true
                continue
            }
            if (line.isBlank()) continue
            // قسم جديد يبدأ بترويسة غير مُزاحة. والتوقّف عندها مقصود: نافذة `grep -A`
            // قد تلتقط قسمًا تاليًا، وسطرُ `UID` فيه ليس استهلاك تطبيق من هذا القسم.
            if (!line.first().isWhitespace()) break
            val match = UID_LINE.find(line) ?: continue
            val uid = decodeUid(match.groupValues[1]) ?: continue
            val milliampHours = match.groupValues[2].toFloatOrNull() ?: continue
            if (milliampHours <= 0f) continue
            byUid[uid] = (byUid[uid] ?: 0f) + milliampHours
        }
        if (byUid.isEmpty()) return null
        val entries = byUid.map { (uid, value) -> Entry(uid, value) }
            .sortedByDescending { it.milliampHours }
        return Reading(entries, entries.sumOf { it.milliampHours.toDouble() }.toFloat())
    }
}
