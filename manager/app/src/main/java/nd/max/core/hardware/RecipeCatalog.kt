/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import android.content.Context

/**
 * من كتالوج التحكّم إلى مدخلات محرّك الوصفات — تحويلٌ لا اجتهاد.
 *
 * **ولماذا طبقة تحويل أصلًا:** `CatalogControlIndex` يقرأ الكتالوج لقياس القدرات (مرشّحات كثيرة،
 * بعضها قالب أو مجلّد)، ومحرّك الوصفات يحتاج قرارًا واحدًا: **مسار حرفيّ واحد** لكل مفتاح. فالتحويل
 * يُسقط كل مرشّح غير حرفيّ ويُبقي الباقي بترتيبه، ولا يُرتّب ولا يُفضّل من عنده — الترتيب من الكتالوج.
 *
 * **وحدوده:** لا يتحقّق من وجود العقدة ولا من صلاحية الكتابة؛ ولا يُصلح كتالوجًا ناقصًا. مدخلٌ بلا
 * مرشّح حرفيّ يصل المحرّك بلا مرشّحين فيُرفض بـ`NO_NODE` — وهذا هو الصواب: مسار مجهول لا يُخترع.
 *
 * **وحدّ مقيس في بيانات اليوم:** عائلة `cpu_limits:{policy}` و`gpu_frequency:{device}` مرشّحاتها
 * مسارات **اكتشاف** (مجلّدات وعقد قراءة) لا مقابض كتابة؛ ولهذا يُرفض المفتاح القالبي بـ`TEMPLATED`
 * قبل النظر في مرشّحاته. أمّا عائلة `vm.*` و`cpu_boost` فمرشّحها الحرفيّ هو عقدتها نفسها.
 */
object RecipeCatalog {

    /** مسار حرفيّ مطلق: بلا قالب `{}`، بلا `$`، بلا نمط `*`. */
    private fun isLiteral(path: String): Boolean = CatalogControlIndex.isLiteralNode(path)

    /** تحويل فهرسٍ جاهز — داخليّ لأن [CatalogControlIndex] نفسه داخليّ. */
    internal fun entries(index: CatalogControlIndex): List<RecipePlan.Entry> = index.entries.map { control ->
        RecipePlan.Entry(
            key = control.key,
            transport = control.transport,
            candidates = control.candidates.filter(::isLiteral),
            writePrivilege = control.writeTiers,
            danger = control.danger,
            kind = control.kind,
        )
    }

    /**
     * من نصّ JSON مباشرةً — يُستعمل في الاختبار وفي أي قارئ لا يملك `Context`.
     * وكتالوج غير مقروء يعود قائمةً فارغة (المحرّك يرفض كل سطر بـ`UNKNOWN_KEY`) لا استثناءً يُسقط شاشة.
     */
    fun fromJson(json: String): List<RecipePlan.Entry> = entries(CatalogControlIndex.parse(json))

    /** من أصلّ التطبيق. فشل القراءة يعود فارغًا لا يرمي — نفس عقد [fromJson]. */
    fun load(context: Context): List<RecipePlan.Entry> = runCatching {
        context.applicationContext.assets.open(CatalogControlIndex.ASSET)
            .bufferedReader()
            .use { it.readText() }
    }.map(::fromJson).getOrDefault(emptyList())
}
