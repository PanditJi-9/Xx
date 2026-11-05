/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

/**
 * نافذة الوضع الليلي: هل نحن داخلها الآن، ومتى تتغيّر الحالة تاليًا.
 *
 * **ولماذا دالّة نقية بلا أندرويد:** الجدولة كلها — قراءة الساعة، وكتابة المفتاح،
 * وضبط المنبّه — أوامر جانبية لا تُختبر هنا؛ أمّا الحكم «داخل النافذة أم خارجها»
 * فهو حساب واحد يتكرّر كل يوم، ويُختبر بلا جهاز. فصلُه يعني أن خطأ منتصف الليل
 * يُكشف في اختبار لا على هاتف المالك.
 *
 * **والنافذة قد تعبر منتصف الليل:** من ٢٢:٠٠ إلى ٠٧:٠٠ أيامًا كثيرة؛ فالمقارنة
 * ليست `now in start..end` بل حالتان: نافذة عادية (البداية أصغر) ونافذة عابرة
 * (البداية أكبر). ولهذا لا تُستعمل `CoerceIn` هنا.
 */
object NightLightWindow {

    const val MINUTES_PER_DAY = 24 * 60

    /** دقيقة اليوم بعد تدويرها في ٢٤ ساعة — قيمة سالبة أو أكبر من يوم تُقبل وتُدوَّر. */
    fun normalise(minute: Int): Int = ((minute % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

    /**
     * هل [nowMinute] داخل النافذة؟
     *
     * البداية **داخلة** والنهاية **خارجة**، كي لا تُشعل النافذة وتُطفئها في الدقيقة
     * نفسها لو تساوتا. وبداية تساوي النهاية تعني «نافذة بلا مدى» ⇒ غير مفعّلة،
     * لأن التفسير الآخر (٢٤ ساعة) يُشعل الضوء بلا نهاية صامتًا.
     */
    fun isActive(nowMinute: Int, startMinute: Int, endMinute: Int): Boolean {
        val now = normalise(nowMinute)
        val start = normalise(startMinute)
        val end = normalise(endMinute)
        if (start == end) return false
        return if (start < end) now >= start && now < end else now >= start || now < end
    }

    /**
     * كم دقيقة تبقّى حتى التغيّر القادم — للجدولة، لا للعرض.
     *
     * يُعاد دائمًا أكبر من صفر (وإلا لانتهى المنبّه إلى نفسه)، والناتج لا يتجاوز
     * ١٤٤٠. وحين تكون النافذة ملغاة (بداية = نهاية) يُعاد يوم كامل: لا تغيير
     * متوقّع، فلا نوقظ الجهاز عبثًا قبل الغد.
     */
    fun nextSwitch(nowMinute: Int, startMinute: Int, endMinute: Int): Int {
        val now = normalise(nowMinute)
        val target = if (isActive(now, startMinute, endMinute)) {
            normalise(endMinute)
        } else {
            normalise(startMinute)
        }
        val delta = target - now
        return if (delta > 0) delta else delta + MINUTES_PER_DAY
    }
}
