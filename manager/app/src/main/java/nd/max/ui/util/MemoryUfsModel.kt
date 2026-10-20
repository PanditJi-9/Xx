/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.maxai.MemoryStall

/**
 * قواعد عرض «تردّدات الذاكرة وUFS» — **صافية كلها**: لا تقرأ عقدة، ولا تعرف Compose.
 *
 * ولماذا ملفٌ مستقلّ عن الشاشة: كل رقم يراه المستخدم هنا (نسبة البروفايل، والنصّ المعروض،
 * وسبب كون الصفّ مغلقًا، واقتراح الأرضية) قرارٌ يمكن أن يخطئ، والقرار الذي لا يُقاس باختبار
 * وحدة يُختبر على هاتفٍ — وهو أغلى وأبطأ وأقلّ دقّة. فلا تبقى في الشاشة إلا الترجمة والرسم.
 *
 * والقاعدة الحاكمة على كل دالة هنا: **المجهول يبقى مجهولًا**. لا صفر مكان قياس ناقص، ولا «100%»
 * حيث غابت الوحدة، ولا اقتراح أرضية على جهاز لم يُنشر سلّمه.
 */
object MemoryUfsModel {

    /**
     * البروفايلات كما تُعرض للمستخدم: نسبةٌ من **أعلى ما يمكن طلبه الآن** لا من كتالوج الجهاز.
     *
     * والفرق جوهريّ: الكتالوج قد يذكر 1.3GHz بينما سقف السائق الحيّ 754MHz (خفضٌ حراري من
     * المصنّع)، فبروفايل «القدرة» عند الكتالوج يطلب قيمة مستحيلة فيُقرأ فشلًا — وهو العطب
     * المقيس في مالك GPU. فالنسبة تُقاس على [MemoryUfsHardwareBackend.Device.configurableMaxFreq].
     */
    enum class Mode(val fraction: Float) {
        CAPABILITY(1.0f),
        SUSTAINED(0.75f),
        BALANCED(0.5f),
        EFFICIENCY(0.25f),
    }

    /** سبب كون الجهاز غير قابل للتحكّم — **رمزٌ لا جملة**، والترجمة في الشاشة. */
    enum class Lock {
        NO_DEVICE,
        UNIT_UNPROVEN,
        NO_PUBLISHED_LADDER,
        RANGE_READ_ONLY,
        NO_CLOCK_READING,
    }

    /** نسبة الأرضية المقترحة عند خنق ذاكرة مقيس — ٧٥٪ من أعلى ما يمكن طلبه. */
    const val FLOOR_FRACTION = 0.75f

    /**
     * طلبُ بروفايل لجهاز واحد؛ `null` حين لا يصلح (الفشل مغلق).
     *
     * @param floor أرضية صريحة بوحدة العقدة (من شريط التمرير أو من اقتراح الخنق)؛ `null` =
     *   بلا أرضية، فلا يُغيَّر حدّ لم يطلبه أحد.
     */
    fun request(
        device: MemoryUfsHardwareBackend.Device,
        mode: Mode,
        floor: Long? = null,
    ): MemoryUfsHardwareBackend.Request? {
        val max = MemoryUfsHardwareBackend.nodeForFraction(device, mode.fraction) ?: return null
        // الأرضية تُقصّ إلى السقف المطلوب لا تُسقط الطلب: مدى بـ`min > max` ترفضه النواة،
        // وطلبٌ يُلغى كلّه لأن أرضيته عالية يترك المستخدم بلا تغيير ولا سبب مفهوم.
        val min = floor?.takeIf { it > 0L }?.let { minOf(it, max) }
        return MemoryUfsHardwareBackend.requestForRange(device, (min ?: 0L).toString(), max.toString())
    }

    /** طلبُ مدى صريح من شرائح الشاشة (درجات معلنة أصلًا). */
    fun requestRange(
        device: MemoryUfsHardwareBackend.Device,
        minNode: Long,
        maxNode: Long,
    ): MemoryUfsHardwareBackend.Request? {
        val min = if (minNode <= 0L) null else minNode
        val request = MemoryUfsHardwareBackend.Request(minFreq = min, maxFreq = maxNode)
        return if (MemoryUfsHardwareBackend.validate(device, request) == null) request else null
    }

    /** درجة معلنة ← نِسبتها من أعلى ما يمكن طلبه (للتمرير: النسبة تُترجم إلى درجة مُعلنة). */
    fun fractionOf(device: MemoryUfsHardwareBackend.Device, node: Long?): Float? {
        val ceiling = device.configurableMaxFreq ?: return null
        val value = node ?: return null
        if (ceiling <= 0L || value <= 0L) return null
        return (value.toFloat() / ceiling.toFloat()).coerceIn(0f, 1f)
    }

    /** سبب الإغلاق، أو `null` حين الجهاز قابل للتحكّم. */
    fun lock(device: MemoryUfsHardwareBackend.Device?): Lock? {
        if (device == null) return Lock.NO_DEVICE
        if (!device.unitTrusted) return Lock.UNIT_UNPROVEN
        if (device.ladder.size < 2) return Lock.NO_PUBLISHED_LADDER
        if (!device.minWritable || !device.maxWritable) return Lock.RANGE_READ_ONLY
        if (device.currentFreq == null && device.maxFreq == null) return Lock.NO_CLOCK_READING
        return null
    }

    /** هل يمكن طلب بروفايل على هذا الجهاز الآن؟ */
    fun canControl(device: MemoryUfsHardwareBackend.Device?): Boolean = lock(device) == null

    /**
     * نصّ التردّد للعرض: `«1.5 GHz»` أو `«900 MHz»`، و`null` حين لا قياس.
     *
     * والعرض **قرارٌ لا زينة**: جهاز بوحدة غير مُثبتة يُعرض `null` ولا يُعرض رقمه الخام، لأن
     * رقمًا بوحدة مجهولة يُقرأ كأنه معنى — وهو أسوأ من فراغ معلوم.
     */
    fun frequencyText(device: MemoryUfsHardwareBackend.Device, raw: Long?): String? {
        val mhz = MemoryUfsHardwareBackend.megahertz(device, raw) ?: return null
        return if (mhz >= 1000L) {
            val tenths = (mhz + 50L) / 100L
            "${tenths / 10}.${tenths % 10} GHz"
        } else {
            "$mhz MHz"
        }
    }

    /** `«min – max»` بالدرجات المخزّنة (شرائح الشاشة تعمل بوحدة العقدة نفسها). */
    fun rangeText(device: MemoryUfsHardwareBackend.Device): String? {
        val min = device.minFreq ?: return null
        val max = device.maxFreq ?: return null
        val minText = frequencyText(device, min) ?: return null
        val maxText = frequencyText(device, max) ?: return null
        return "$minText – $maxText"
    }

    /** سلّم الجهاز للنصّ التشخيصي: أعلى ثلاث درجات معلنة، مفصولةً بـ`·`. */
    fun ladderText(device: MemoryUfsHardwareBackend.Device, take: Int = 3): String? {
        val ladder = device.ladder
        if (ladder.isEmpty()) return null
        return ladder.takeLast(take).mapNotNull { frequencyText(device, it) }.joinToString(" · ")
            .takeIf(String::isNotBlank)
    }

    /**
     * اقتراح أرضية الذاكرة عند **خنق مقيس** — الفكرة التي لا يملكها المشروع المرجعيّ.
     *
     * وكيف تُحسب: خنقُ الذاكرة (`full avg10 ≥ 10٪`) معناه أن جزءًا من الزمن يذهب **انتظارًا**
     * للذاكرة لا عملًا. ورفع الأرضية يقصّر ذلك الانتظار لأنه يُخرج وحدة التحكّم من أدنى
     * حالاتها قبل أن تُطلب. والسقف لا يُرفَع هنا: طلبُ الجهاز عند قدرته له مقبضه.
     *
     * والحدود الثلاثة التي تجعل الاقتراح لا يضرّ:
     * 1. **لا اقتراح بلا قياس:** `fullFraction = null` (PSI غير مدعوم) ⇒ `null`. الجهل لا يُنتج
     *    تغييرًا.
     * 2. **لا اقتراح تحت العتبة:** الخنق العادي (استرجاع صفحة) ليس داعيًا لقفل الأرضية.
     * 3. **ولا يتجاوز سقف المستخدم:** الاقتراح يُقصّ إلى [ceilingNode]، فمن اختار «اقتصادي»
     *    لا يُقترح عليه ما ينقض اختياره.
     *
     * وما لا يفعله: **لا يطبّق**. يُخرج قيمةً تُعرض على المستخدم بزرّ واحد؛ ولو طبّق لصار لدينا
     * حلقة كتابة تلقائية على الذاكرة بلا طلب — وهو مالا يُبنى بلا قرار مالك صريح.
     */
    fun floorAdvice(
        device: MemoryUfsHardwareBackend.Device,
        fullFraction: Float?,
        ceilingNode: Long? = null,
        threshold: Float = MemoryStall.STALL_FRACTION,
    ): Long? {
        if (fullFraction == null || fullFraction < threshold) return null
        val ladder = device.ladder
        if (ladder.size < 2) return null
        val target = MemoryUfsHardwareBackend.nodeForFraction(device, FLOOR_FRACTION) ?: return null
        val ceiling = ceilingNode?.takeIf { it > 0L }
        val bounded = if (ceiling != null) minOf(target, ceiling) else target
        val snapped = ladder.lastOrNull { it <= bounded } ?: return null
        return snapped
    }

    /** أهمّ ما يُقرأ في سطر الجهاز: الاسم المعلن + عائلته (بيانات، لا يُترجَم). */
    fun deviceLabel(device: MemoryUfsHardwareBackend.Device): String {
        val family = device.family.name.lowercase()
        return if (device.name.isBlank()) family else "${device.name} · $family"
    }
}
