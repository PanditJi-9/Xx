/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

/**
 * يعزل نصًّا لاتينيّ الاتجاه (مسار، أو رقم ووحدته) داخل فقرة عربية.
 *
 * بلا عزل، الفاصل المحايد بين رقم ووحدة لاتينية يُحسم بالاتجاه العربي فيُقلب الترتيب
 * («dpi 520» · «% 100»)، والمسار يُقرأ بفواصل مضاعفة (`Android//data` · `//MaxManger`)
 * — قِيس على الجهاز في `resolutionscreen` و`max_backup`. حرفا LRI/PDI
 * (U+2066 / U+2069) لا يُرسمان، فلا يتغيّر النص إلا ترتيبه البصري.
 */
internal fun ltrIsolated(text: String?): String? =
    text?.takeIf { it.isNotEmpty() }?.let { "\u2066$it\u2069" }
