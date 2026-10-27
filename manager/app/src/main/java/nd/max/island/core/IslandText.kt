/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **قصّ النصّ الذكيّ** بلا أندرويد، فيُقاس على JVM.
 *
 * ### لماذا لا يكفي `TextOverflow.Ellipsis`
 *
 * `Text` يقصّ عند **آخر ما اتّسع له السطر** بعد القياس، فقد يقطع كلمةً في منتصفها، أو يفقد
 * ذيلًا مهمًّا («…/Download/MyFile.part»)، أو يترك نقطةً واحدة في سطر ضيّق. والجزيرة تعرض
 * عنوان إشعار واسم مسار، والقاعدة هنا: **قصّ عند حدّ كلمة إن وُجد، ولا تقطع محرفًا مركّبًا،
 * وللاسم الطويل احفظ طرفيه**.
 *
 * ولا تُقاس هذه القواعد بالعين: لها اختبارات JVM.
 */
package nd.max.island.core

import java.lang.Character

object IslandText {
    /** المسافة البيضاء المتكرّرة تُطوى فلا تُحسب في الميزانية. */
    fun normalize(text: String): String {
        if (text.isEmpty()) return ""
        val out = StringBuilder(text.length)
        var space = false
        for (c in text.trim()) {
            if (c.isWhitespace()) {
                space = true
            } else {
                if (space && out.isNotEmpty()) out.append(' ')
                space = false
                out.append(c)
            }
        }
        return out.toString()
    }

    fun codePoints(text: String): Int = text.codePointCount(0, text.length)

    private fun takeCodePoints(text: String, count: Int): String {
        if (count <= 0) return ""
        val n = count.coerceAtMost(codePoints(text))
        return text.substring(0, text.offsetByCodePoints(0, n))
    }

    private fun dropCodePoints(text: String, count: Int): String {
        if (count <= 0) return text
        val total = codePoints(text)
        if (count >= total) return ""
        return text.substring(text.offsetByCodePoints(0, count))
    }

    /**
     * قصّ من النهاية. لا يقطع زوجًا بديلًا (إيموجي)، ويحاول الوقوف عند مسافة بعد نصف الميزانية
     * على الأقل، فيبقى المعنى مفهومًا بدل «رسالة من فل…».
     */
    fun fit(text: String, maxChars: Int, ellipsis: String = "…"): String {
        val clean = normalize(text)
        if (maxChars <= 0) return ""
        if (codePoints(clean) <= maxChars) return clean
        if (maxChars <= codePoints(ellipsis)) return takeCodePoints(ellipsis, maxChars)
        val ell = ellipsis
        val budget = (maxChars - codePoints(ell)).coerceAtLeast(1)
        val slice = takeCodePoints(clean, budget)
        val space = slice.lastIndexOf(' ')
        val cut = if (space >= 0 && codePoints(slice.substring(0, space)) >= budget / 2) {
            slice.substring(0, space).trimEnd()
        } else {
            slice
        }
        return if (cut.isEmpty()) ell else cut + ell
    }

    /**
     * قصّ من الوسط: يحفظ بداية الاسم ونهايته (الامتداد) — الأنسب للملفّات ومسارات التنزيل.
     */
    fun fitMiddle(text: String, maxChars: Int, ellipsis: String = "…"): String {
        val clean = normalize(text)
        if (maxChars <= 0) return ""
        val total = codePoints(clean)
        if (total <= maxChars) return clean
        if (maxChars <= codePoints(ellipsis)) return takeCodePoints(ellipsis, maxChars)
        val ell = ellipsis
        val budget = (maxChars - codePoints(ell)).coerceAtLeast(2)
        val headCount = (budget * 3) / 5
        val tailCount = (budget - headCount).coerceAtLeast(1)
        val head = takeCodePoints(clean, headCount).trimEnd()
        val tail = dropCodePoints(clean, total - tailCount).trimStart()
        return head + ell + tail
    }

    /** حرف أوّل صالح للعرض (اسم أوّل) بلا قطع زوج بديل — بديل نقيّ عن ICU في واجهة الأفاتار. */
    fun firstGrapheme(name: String): String {
        val clean = normalize(name)
        if (clean.isEmpty()) return "?"
        val end = clean.offsetByCodePoints(0, 1)
        // علامة مركّبة بعد الأساس (مثل النبرة) تُضمّ إلى الحرف الأوّل.
        var cut = end
        while (cut < clean.length && Character.getType(clean.codePointAt(cut)) == Character.NON_SPACING_MARK.toInt()) {
            cut += Character.charCount(clean.codePointAt(cut))
        }
        return clean.substring(0, cut).uppercase()
    }
}
