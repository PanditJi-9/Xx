/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.ui.util.AppConfig

/**
 * يقرأ معدلًا رقميًا موجبًا محفوظًا في `AppConfig.refresh_rate`، لا نتيجة تطبيقه.
 * يقبل العشري كما يفعل المطبّق الداخلي، لكنه يرفض NaN/Infinity صراحةً.
 * مسار الجذر يقرأ أعدادًا صحيحة فقط؛ لذلك لا يثبت هذا العدّ قبول الطلب في أي مسار.
 */
internal fun customRefreshHz(raw: String?): Int? {
    // `90.0` إعداد رقمي محفوظ، حتى لو لم يقبله مسار تنفيذ معيّن.
    val text = raw?.takeIf { it.isNotBlank() && it != DEFAULT_RATE } ?: return null
    // و`Infinity`/`NaN` ليست معدّلًا ولو صار أحدهما عددًا بعد التحويل (NaN ⇒ 0، وInfinity ⇒
    // `Int.MAX_VALUE`): الأول يسقط بالشرط أدناه، والثاني يُسقَط هنا صراحةً.
    val hz = text.toFloatOrNull()?.takeIf { it.isFinite() }?.toInt() ?: return null
    return hz.takeIf { it > 0 }
}

/**
 * معرّفات التطبيقات التي لها معدّل خاصّ، مرتّبة — فالناتج قابل للمقارنة في الاختبار
 * وللعرض إن احتاجه أحد لاحقًا.
 */
internal fun customRefreshPackages(profiles: Map<String, AppConfig>): List<String> =
    profiles.entries
        .filter { customRefreshHz(it.value.refresh_rate) != null }
        .map { it.key }
        .sorted()

/** فشل القراءة قد يترك خريطة قديمة؛ العدد حينها مجهول، لا صفر ولا العدد القديم. */
internal fun customRefreshCount(
    profiles: Map<String, AppConfig>,
    loading: Boolean,
    failed: Boolean,
): Int? = if (loading || failed) null else customRefreshPackages(profiles).size

/** القيمة التي تعني «اتبع النظام» في `AppConfig.refresh_rate` — نفس كلمة `InAppProfileRules`. */
private const val DEFAULT_RATE = "default"
