/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.overlay

/**
 * رياضيات موضع نافذة التراكب — **نقيّة وبلا Android**، فتُقاس بلا جهاز.
 *
 * **ولماذا خرجت من `OverlayWindow`:** كانت داخل دوالّ خاصّة تلمس `params` و`View` و`WindowManager`،
 * فلا سبيل لاختبارها إلا بجهاز أو Robolectric. وهي أخطر منطق في التراكب: نافذة تخرج عن الشاشة
 * **لا تُسحَب عودة** على أغلب الأجهزة، وحساب حافة خاطئ يعني تراكبًا مفقودًا بلا أيّ إشارة.
 * خطة `GAME-SPACE-PLAN.md` §GS-03 تطلب صراحةً «اختبارات لرياضيات الموضع/الحدود».
 *
 * **والحدّ مُعلَن:** كل القيم بكسل صحيح في إحداثيّات النافذة (`WindowManager.LayoutParams.x/y`)،
 * وهي **غير سالبة أصلًا** لأن `x/y` في `LayoutParams` تُقاس من أعلى-يسار الشاشة بلا سالب. لذلك
 * [clamp] لا يقبل سالبًا ولا يُنتجه.
 */
object OverlayGeometry {

    /**
     * أقصى `x` مسموح: عرض الشاشة ناقص عرض النافذة، **ولا ينزل تحت الصفر**.
     *
     * ونافذة أعرض من الشاشة (`width > screenW`) تعطي صفرًا لا سالبًا — وهذا هو الفرق بين نافذة
     * ملتصقة بالحافة اليسرى ونافذة مُزاحة خارجها بلا رجعة.
     */
    fun maxOffset(screen: Int, size: Int): Int = (screen - size).coerceAtLeast(0)

    /**
     * يحبس الإزاحة داخل الشاشة. النافذة التي خرجت لا تُسحَب عودةً في أغلب الأجهزة.
     *
     * **والقياس الصفريّ (`size <= 0`) يُعيد الإزاحة كما هي** لأن [maxOffset] يعطي عرض الشاشة
     * كاملًا: المدى `[0, screen]` لا يمسّ إزاحة مشروعة، فلا تقفز نافذة لم تُقس بعد إلى الزاوية.
     */
    fun clamp(offset: Int, screen: Int, size: Int): Int = offset.coerceIn(0, maxOffset(screen, size))

    /**
     * الحافة الأقرب أفقيًّا: `0` لليسار، أو أقصى إزاحة لليمين.
     *
     * والقرار بمركز النافذة لا بحافّتها — نافذة تجاوز مركزها منتصف الشاشة تُسحَب إلى الحافة
     * الأخرى، وهو ما يتوقّعه الإصبع.
     *
     * **والعرض الصفريّ (`width <= 0`) يُعيد الإزاحة كما هي:** نافذة لم تُقس بعد (`view.width` قبل
     * أوّل تخطيط) لا مركز لها، فسحبها إلى حافة يكون تخمينًا. وهذا حال حقيقيّ: الالتقاط يقع قبل
     * `onGlobalLayout` أحيانًا.
     */
    fun snapTarget(offset: Int, screen: Int, size: Int): Int {
        if (size <= 0) return offset
        val far = maxOffset(screen, size)
        return if (offset + size / 2 < screen / 2) 0 else far
    }

    /**
     * الحقول التي يغيّرها ملء الشاشة، **نسخة نقيّة عن `LayoutParams`** بلا أندرويد في هذا الملف.
     *
     * `fullScreen` تعني أن العرض والارتفاع `MATCH_PARENT`، و`layoutInScreen` تعني وجود
     * `FLAG_LAYOUT_IN_SCREEN`. أمّا `x`/`y` فهما إحداثيّا النافذة كما يقرأهما المستدعي.
     *
     * **ولماذا وُجدت (تكملة `PERF-AUDIT-01`):** `GamePanelService.placeWindow()` ينادي
     * `setFullScreen(false)` **كل استطلاع** (كل ثانيتين في الخمول) ليبقي المقبض بحجمه. وكان
     * كل نداء يدفع `updateViewLayout` إلى `WindowManager` ولو كان الإطار مطابقًا — أي معاملة
     * Binder كل ثانيتين ما دامت لعبة مفعّلة، والخدمة تعيش دائمًا. فصار الطلب يُقارن بالقائم أولًا.
     */
    data class Frame(
        val fullScreen: Boolean,
        val x: Int,
        val y: Int,
        val layoutInScreen: Boolean
    )

    /**
     * الإطار المطلوب بعد طلب ملء الشاشة — تشغيلًا أو إيقافًا.
     *
     * - **تشغيل:** `MATCH_PARENT` عند الأصل `(0,0)` مع عَلَم التمدّد تحت شرائط النظام.
     * - **إيقاف:** `WRAP_CONTENT` بلا العَلَم، والإحداثيّان المطلوبان إن مُرِّرا — و`null` يعني
     *   «أبقِ الحالي»، حتى لا يُحرَّك المقبض إلى موضع قديم عند نداءٍ لا يمرّر إحداثيات.
     *
     * والدالّة نقيّة: المقارنة `target == current` في المستدعي هي التي تمنع المعاملة الزائدة.
     */
    fun fullScreenTarget(on: Boolean, current: Frame, atX: Int? = null, atY: Int? = null): Frame =
        if (on) {
            Frame(fullScreen = true, x = 0, y = 0, layoutInScreen = true)
        } else {
            Frame(
                fullScreen = false,
                x = atX ?: current.x,
                y = atY ?: current.y,
                layoutInScreen = false
            )
        }

    /**
     * مستطيل قصّة (نتوء) بإحداثيّات الشاشة — **نسخة نقيّة عن `android.graphics.Rect`** لأن
     * الوحدة لا تلمس أندرويد (فلا `Rect` غير المُختبر في JVM هنا)، والقصّة نفسها تُقرأ من
     * `Display.getCutout()` في موضع النداء وتُحوَّل إلى هذا الشكل.
     */
    data class Cutout(val left: Int, val top: Int, val right: Int, val bottom: Int)

    /**
     * يُزيح مستطيل النافذة كي لا يقع تحت نتوء — **بأقرب إزاحة ممكنة**، أو يتركه كما هو.
     *
     * **ولماذا دالّة مستقلّة:** بند §2 «منع تداخل اللوحة مع حواف الشاشة أو النتوءات» —
     * والنافذة الواقعة تحت النتوء يفقد جزءها العلوي/الجانبي تحت الحافة الميتة، وما تحت النتوء
     * لا يُسحَب عودةً بالإصبع لأنه لا يُرى. والحساب رياضيّ بحت يُقاس بلا جهاز.
     *
     * والقواعد في سطور:
     *  - لا قصّة أو لا تقاطع ⇒ **الموضع كما هو** بالحرف (لا تحريك لمن لا يحتاجه).
     *  - عند التقاطع: أربع إزاحات مرشّحة (يسار القصّة · يمينها · أعلاها · أسفلها)، يُرفض ما
     *    يخرج عن الشاشة أو يخرج سالبًا، ويُختار **أقربها** (أقلّ بكسلات حركة) — فينزلق التراكب
     *    أقلّ ما يمكن عن موضع طلب المستخدم.
     *  - وما لا يُحلّ (نافذة لا تتّسع في أي فراغ متاح) **يبقى مكانه**: إزاحة تخرج عن الشاشة
     *    أسوأ من تداخل جزئيّ مرئيّ.
     *  - والقصّات تُعالَج بالتتابع (الشلال يعطي حتى أربعًا)، وتتكرّر الجولة إن تحرّك شيء
     *    (إزاحة لأجل الأولى قد تُدخل الثانية)، **بسقف دورات = عدد القصّات + ١** فلا حلقة لا تنتهي.
     */
    fun avoidCutout(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        screenW: Int,
        screenH: Int,
        cutouts: List<Cutout>
    ): Pair<Int, Int> {
        if (cutouts.isEmpty() || width <= 0 || height <= 0) return x to y
        var px = x
        var py = y
        var passes = 0
        var changed = true
        while (changed && passes <= cutouts.size) {
            changed = false
            passes++
            for (cutout in cutouts) {
                val overlaps = px < cutout.right && px + width > cutout.left &&
                    py < cutout.bottom && py + height > cutout.top
                if (!overlaps) continue

                var best: Pair<Int, Int>? = null
                var bestCost = Int.MAX_VALUE
                fun consider(nx: Int, ny: Int, cost: Int) {
                    if (nx < 0 || ny < 0) return
                    if (nx + width > screenW || ny + height > screenH) return
                    if (cost < bestCost) {
                        bestCost = cost
                        best = nx to ny
                    }
                }
                // التقاطع يضمن أن كل تكلفة أدناه موجبة: الإزاحة تغادر المستطيل فعلًا.
                consider(cutout.left - width, py, px - (cutout.left - width))
                consider(cutout.right, py, cutout.right - px)
                consider(px, cutout.top - height, py - (cutout.top - height))
                consider(px, cutout.bottom, cutout.bottom - py)

                val target = best ?: continue
                if (target.first != px || target.second != py) {
                    px = target.first
                    py = target.second
                    changed = true
                }
            }
        }
        return px to py
    }
}
