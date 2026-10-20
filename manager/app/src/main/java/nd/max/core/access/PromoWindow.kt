/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.access

/**
 * نوافذ «المجّاني للجميع» — مِنحةُ المالك في مناسبة أو مسابقة، **وبايتاتها موقّعة** في الإعداد
 * البعيد فلا يمكن اختراعها على جهازٍ مروّت (وإلّا صار كل جهاز يمنح نفسه عطلةً أبدية).
 *
 * ## ولماذا هذه الحزمة بالذات
 *
 * الحزمة `access` هي **ملتقى الطرفين**: العقد الموقّع (`remote`) يعرف النوافذ لأن المالك ينشرها
 * فيه، والترخيص (`license`) يقرأ منها النافذة السارية عند حساب الوصول. ولو وُضعت في إحدى
 * الحزمتين لصار على الأخرى أن تعتمد عليها، ولانعكس الاتجاه يومًا. هنا لا تعتمد النافذة على
 * أحد — يقرؤها الاثنان.
 *
 * والنافذة نصف مفتوحة `[from, to)`: اللحظة الواحدة لا تُحسب في نافذتين متلاصقتين، وهو ما يجعل
 * جمع النوافذ في الخادم (الإيقاف المؤقّت) حتميًّا بلا حساب مزدوج.
 */
data class PromoWindow(
    val fromMillis: Long,
    val toMillis: Long,
    val note: String = "",
) {
    fun covers(millis: Long): Boolean = millis >= fromMillis && millis < toMillis
}

object Promos {

    /** النافذة السارية **الآن**، أو `null`. وعند تلاصق نافذتين تُعتمد الأولى ترتيبًا. */
    fun active(windows: List<PromoWindow>, nowMillis: Long): PromoWindow? =
        windows.firstOrNull { it.covers(nowMillis) }

    /**
     * أبعد نهاية بين النوافذ السارية الآن — تُعرض للمستخدم «مجّاني حتى …» بلا ادّعاء نافذةٍ
     * واحدة: لو تلاصقت نافذتان فالوعد يمتدّ إلى ما تراه القائمة كاملة.
     */
    fun until(windows: List<PromoWindow>, nowMillis: Long): Long? =
        windows.filter { it.covers(nowMillis) }.maxOfOrNull { it.toMillis }
}
