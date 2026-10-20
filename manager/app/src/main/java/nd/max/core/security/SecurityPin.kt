/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import nd.max.BuildConfig

/**
 * البصمة المثبَّتة لموقّع الحزمة — **تُشتقّ زمن البناء من مخزن المفاتيح نفسه، ولا تُكتب بيد**.
 *
 * ## لماذا الاشتقاق الزمنيّ لا ثابتٌ مكتوب
 *
 * البصمة الثابتة المكتوبة في الكود تُنتج عطبين: (١) لا تصحّ لبناء debug أصلًا فيرفض
 * التطبيق نفسه على جهاز المطوّر، (٢) وتصير **وثيقةً** تنزاح عن مخزن المفاتيح يوم يُعاد
 * توليده — وهو ما وقع فعلًا في تاريخ هذا المستودع: بصمة في `VALIDATION.md` وبصمة أحدث
 * في `build.yml`، والاثنتان لموقّعين مختلفين.
 *
 * فالحساب هنا يجري في `build.gradle.kts` من `signingConfigs` الفعليّ:
 * **نوع البناء نفسه هو ما يُثبَّت** — إصدارٌ يُثبّت موقّع الإصدار، وdebug يُثبّت موقّع
 * التصحيح. فلا مزلق يدويّ، ولا سرّ يُكتب في ملف، والقيمة المشتقّة (بصمة شهادة) **ليست
 * سرًّا**: هي مرئيّة أصلًا في كل حزمة موقّعة، وفي `build.yml` اليوم.
 *
 * ## وحدّ هذه الحماية — مُعلَن لا مسكوت عنه
 *
 * البصمة تصل إلى الحزمة في `BuildConfig`، ومُقنَّعة بـXOR موضعيّ فقط. فمَن يملك وقتًا
 * ومُفكِّكًا يجدها. وقيمتها الحقيقيّة أنها **تُبطل إعادة التوقيع بمفتاح آخر** — وهي أشهر
 * طريقة لصناعة نسخة MOD: تُعدّل الحزمة ثم تُوقّع بمفتاح المهاجم. أمّا من يعدّل الحزمة
 * **دون** إعادة توقيع فلا يُثبّتها النظام أصلًا. ولذلك: الهجوم الواقعيّ يقع دائمًا في
 * نطاق هذه البصمة، والهجوم خارجها (تعديل داخل الجذر على جهاز مملوك) لا يُوقف حمايةً
 * من هذا الصنف — ويُقال ذلك صريحًا بدل ادّعاء حماية مطلقة.
 *
 * ## ووضع «لا بصمة» مقصود
 *
 * لو تعذّر الاشتقاق زمن البناء (بلا مخزن مفاتيح، أو بناء تجريبيّ أوّل بلا `debug.keystore`)،
 * تصير [configured] `false` ويعمل [IntegrityProbe] **وضع رصد** لا رفض. وهذا مقصود:
 * بناءٌ في بيئة بلا مفتاح يجب أن يُنتج تطبيقًا يعمل، لا تطبيقًا يرفض نفسه.
 */
object SecurityPin {

    /** طول البصمة المتوقّع: SHA-256 لشهادة DER. أي طول آخر يعني إعدادًا معطوبًا. */
    const val DIGEST_BYTES = 32

    /**
     * البصمة المتوقّعة، أو `null` إن لم يُشتقّ زمن البناء.
     * وحُسِبت مرّة واحدة (البناء لا يتغيّر داخل العملية).
     */
    val expected: ByteArray? by lazy { derive() }

    /** هل ضُبطت بصمة أصلًا؟ الفرق بين «لا حماية مُعدّة» و«الحماية تعمل» يجب أن يُعرف. */
    val configured: Boolean get() = expected != null

    /** هل البصمة المرصودة هي المثبَّتة؟ ولا يُفشي شيء عن الاختلاف موضعه. */
    fun matches(actual: ByteArray?): Boolean = IntegrityDigest.constantTimeEquals(expected, actual)

    private fun derive(): ByteArray? {
        val mask = IntegrityDigest.decode(BuildConfig.SECURITY_PIN_MASK) ?: return null
        if (mask.isEmpty()) return null
        val low = IntegrityDigest.decodeMasked(BuildConfig.SECURITY_PIN_LO, mask) ?: return null
        val high = IntegrityDigest.decodeMasked(BuildConfig.SECURITY_PIN_HI, mask) ?: return null
        if (low.size + high.size != DIGEST_BYTES) return null
        val out = ByteArray(DIGEST_BYTES)
        low.copyInto(out, 0)
        high.copyInto(out, low.size)
        return out
    }
}
