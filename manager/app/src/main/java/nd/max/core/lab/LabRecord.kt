/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.lab

/**
 * نتيجة اختبار عتاد كما تُحفظ: رمز النتيجة، وزمن آخر فحص.
 *
 * **والرمز نصّ لا enum:** المخزن لا يعرف شاشةً ولا لونًا ولا ترجمة، فلا يُربط بواجهة
 * تُعاد تسميتها. والشاشة وحدها هي التي تترجم الرمز إلى كلمة ولون، ومن لا يعرف الرمز
 * يعرض «لم يُجرَّب» بدل أن يخترع نتيجة.
 */
data class LabRecord(val outcome: String, val atMs: Long)

/**
 * ترميز السجلّ في قيمة واحدة `outcome|atMs` — قراءةٌ وكتابةٌ في مكان واحد.
 *
 * **ولماذا الترميز صريح:** قيمةٌ واحدة لكل اختبار تعني أن مسح النتيجة إزالةُ مفتاح
 * واحد، لا تعديل ثلاثة مفاتيح قد تتباعد. والدالّتان نقيّتان بلا Android، فتُقاسان
 * في اختبار JVM (انظر `LabRecordTest`).
 */
fun encodeLabRecord(record: LabRecord): String = "${record.outcome}|${record.atMs}"

/**
 * يفكّ ما كتبته [encodeLabRecord]، ويعيد `null` لكل ما لا يُفهم.
 *
 * والرفض مقصود: قيمة قديمة أو مقطوعة أو مكتوبة بيدٍ لا تُقرأ كنتيجة — لأن قراءتها
 * خطأً تعني أن الشاشة تقول «يعمل» عن اختبار لم يُجرَّب.
 */
fun decodeLabRecord(raw: String?): LabRecord? {
    val parts = raw?.split('|') ?: return null
    if (parts.size != 2) return null
    val outcome = parts[0].takeIf { it.isNotBlank() } ?: return null
    val atMs = parts[1].toLongOrNull() ?: return null
    if (atMs <= 0L) return null
    return LabRecord(outcome, atMs)
}
