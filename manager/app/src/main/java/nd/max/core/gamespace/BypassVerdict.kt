/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

/**
 * حكم تجاوز الشحن — **من القراءة بعد الكتابة، لا من نداء الكتابة**.
 *
 * **العطب الذي وُلد منه (مقيس في هذه الجولة):** بلاطة الإعدادات السريعة كانت تكتب الخصيصة ثم
 * تُعلن التفعيل فورًا (`tile.state = Tile.STATE_ACTIVE`) **قبل أي قراءة**. وعلى جهاز بلا جذر —
 * أو على روم يرفض `set` بسياق التطبيق (الرفض التساعي المقيس في `pstore`) — تبقى البلاطة تقول
 * «مفعّل» ولا شيء تغيّر. وهو نصّ §11: **لا يُظهر نجاحًا لم يحدث**.
 *
 * والحكم هنا دالّة نقيّة تُقاس على JVM بلا أندرويد: ما طُلب وما قُرئ، والنتيجة حكمٌ لا تخمين.
 */
object BypassVerdict {

    /**
     * هل طُبِّق ما طُلب؟ الشرط: القراءة بعد الكتابة تساوي القيمة المطلوبة نفسها.
     *
     * وكلّ ما عدا ذلك **لم يُطبَّق** — فراغ ([String]? فارغ) أو `null` (خصيصة غير موجودة) أو قيمة
     * أخرى (روم/وحدة كتبت غير ما أردنا). ولا يُقبل «قريب»: `1` ليست `on`، والغائب ليس نجاحًا.
     */
    fun applied(requestedOn: Boolean, readBack: String?): Boolean =
        readBack?.trim() == if (requestedOn) "1" else "0"
}
