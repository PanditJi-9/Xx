/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.util

import android.view.Choreographer

/**
 * `AR-32` — كشف **انسداد الخيط الرئيسي** بأثر مقيس، لا بشكوى «التطبيق تجمّد».
 *
 * المشكلة التي يحلّها: عندما يقول مستخدم «الواجهة تعلّقت» لا يوجد عندنا أي سطر يقيس ذلك،
 * فيبقى التقرير غير قابل للفحص. هنا نقيسه داخل عمليتنا بالطريقة الأرخص المتاحة:
 * [Choreographer] يطالبنا بإطار عند كل مزامنة رأسيّة، فإذا تأخّر النداء عن الحدّ فمعناه
 * أن الخيط الرئيسي كان مشغولًا — وهذا **قياس لا استنتاج**.
 *
 * **حدّ مُعلَن عن قصد:** هذا يقيس **خيط تطبيقنا فقط**. لا نستطيع قياس الخيط الرئيسي لتطبيق
 * آخر بلا امتياز أعلى، ولا نُوهم العكس. كذلك **لا يُدَّعى** أن الإطارات المُسقَطة = إطارات
 * النظام: هذا مؤشّر على تجربة استخدام تطبيقنا، وليس محلّل أداء للجهاز (ذلك `AR-07`).
 *
 * **الكلفة كانت هي العطب (قِيست في ٢٠٢٦-١١-١٦):** كان `postFrameCallback` بلا تأجيل يعيد
 * تسجيل نفسه كل مزامنة رأسيّة، فيستيقظ الخيط الرئيسي **١٢٠ مرّة في الثانية** على كل شاشة
 * بلا رسم شيء: `atrace view gfx` أعطى ~٩٠ شريحة `animation`/ثانية على `apps` و`settings`،
 * و`RenderThread` صفرًا، و`thr2.py` أعطى ٦٪ من نواة على الخيط الرئيسي في كل مسار.
 * وهو تشخيص، فلا يجوز أن يكون **أكبر مستهلك** في التطبيق.
 *
 * **والعلاج:** العيّنة تُؤخذ بوتيرة الحدّ نفسه (٧٠٠ م.ث ⇒ ~١٫٤ استيقاظًا/ثانية) لا بوتيرة
 * الإطار، ولا يتغيّر الحكم: نطلب الإطار التالي بعد `thresholdMs` ونسجّل **تأخّر النداء عن
 * موعده** (زمن الآن ناقص الزمن المتوقّع)، فيبقى [StallMath.classify] كما هو ويعطي المدّة
 * الحقيقية للانسداد. ودقّة ٧٠٠ م.ث كافية لحدٍّ هو ٧٠٠ م.ث — ولا تُنقص قدرة الكشف.
 * والسجل ببوابة `DETAILED_LOG` كبقية الأحداث.
 */
class MainThreadStallDetector(
    private val thresholdMs: Long = StallMath.DEFAULT_THRESHOLD_MS,
    private val onStall: (StallReport) -> Unit = {},
) : Choreographer.FrameCallback {

    private var expectedFrameNanos = 0L
    private var running = false
    private var count = 0L
    private var worstMs = 0L

    /** كم انسدادًا مقيسًا وقع منذ [start]. */
    val stallCount: Long get() = count

    /** أسوأ انسداد مقيس بالمللي — نقطة المقارنة التي تُثبت التحسّن أو نفيه. */
    val worstStallMs: Long get() = worstMs

    /** يبدأ المراقبة. الآمن أن يُنادى من `onStart` ويُقابله [stop] في `onStop`. */
    fun start() {
        if (running) return
        running = true
        // ‏0 تعني «لا موعد سابق» ⇒ أول نداء لا يُحكم عليه بشيء (لا مقارنة بمجهول).
        expectedFrameNanos = 0L
        arm()
    }

    /** يوقف المراقبة. لا يمسح العدّادات — التقرير يبقى صالحًا بعد التوقّف. */
    fun stop() {
        if (!running) return
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        expectedFrameNanos = 0L
    }

    /**
     * يطلب النداء التالي بعد [thresholdMs] لا في الإطار القادم.
     *
     * **ولماذا التأجيل لا الإطار:** الطلب في كل إطار يعني استيقاظًا ١٢٠ مرّة/ثانية بلا رسم
     * (قِيس: ٦٪ من نواة على كل شاشة). والحدّ الذي نحكم به ٧٠٠ م.ث، فعيّنة بوتيرة الحدّ تكشف
     * كل انسداد يستحقّ الاسم بتكلفة ١٫٤ استيقاظًا/ثانية.
     */
    private fun arm() {
        expectedFrameNanos = System.nanoTime() + thresholdMs * 1_000_000L
        Choreographer.getInstance().postFrameCallbackDelayed(this, thresholdMs)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val expected = expectedFrameNanos
        if (expected > 0L) {
            // الفجوة المقيسة = تأخّر النداء عن موعده، لا الفرق بين إطارين متتاليين.
            // و`expected` يجعل [StallMath] يحكم على التأخّر نفسه بلا أي تغيير في منطقه.
            when (val verdict = StallMath.classify(expected, System.nanoTime(), thresholdMs)) {
                is StallMath.Verdict.Stall -> {
                    count += 1
                    if (verdict.durationMs > worstMs) worstMs = verdict.durationMs
                    onStall(StallReport(verdict.durationMs, worstMs, count))
                }
                // فجوة أطول من حدّ المعقول: الحلقة كانت متوقّفة (شاشة مطفأة/خلفية)، ولا دليل
                // على انسداد. نتجاهلها بدل أن ندّعي عيبًا لم نقسه.
                is StallMath.Verdict.Ignored, StallMath.Verdict.None -> Unit
            }
        }
        arm()
    }
}

/** تقرير واحد: هذه المدّة، وأسوأ ما سُجّل، وكم مرّة حتى الآن. */
data class StallReport(
    val durationMs: Long,
    val worstMs: Long,
    val count: Long,
)

/**
 * الحكم على فجوة بين إطارين — **خالصة وبلا أي اعتماد على أندرويد**، لذلك قابلة للاختبار
 * بلا جهاز ولا محاكي. وهي وحدها التي تقرّر: انسداد مقيس، أم شيء لا يجوز ادّعاؤه.
 */
internal object StallMath {

    /** الحدّ الافتراضي: ٧٠٠ مللي (نفس عتبة النظير `uperf` لاستعادة الحالة). */
    const val DEFAULT_THRESHOLD_MS = 700L

    /**
     * فوق هذه المدّة لا نستطيع التمييز بين انسداد حقيقي وحلقة رسم **متوقّفة** (شاشة مطفأة،
     * تطبيق في الخلفية، انقطاع مزامنة). فحكمنا هنا: **لا ندّعي** — وهذا فرق جوهري عن
     * «أطلقنا إنذارًا ولم نتحقّق».
     */
    const val MAX_CREDIBLE_MS = 5_000L

    sealed interface Verdict {
        /** إطار طبيعي، أو لا إطار سابق للمقارنة. */
        data object None : Verdict

        /** فجوة تجاوزت الحدّ وضمن نطاق نصدّقه ⇒ انسداد مقيس. */
        data class Stall(val durationMs: Long) : Verdict

        /** فجوة طويلة إلى درجة لا تُسمّى انسدادًا (الحلقة كانت متوقّفة). */
        data class Ignored(val gapMs: Long) : Verdict
    }

    /**
     * @param previousFrameNs زمن الإطار السابق بالنانو (‏0 أو أقل = لا إطار سابق ⇒ [Verdict.None]).
     * @param frameNs زمن الإطار الحالي بالنانو.
     */
    fun classify(
        previousFrameNs: Long,
        frameNs: Long,
        thresholdMs: Long = DEFAULT_THRESHOLD_MS,
        ceilingMs: Long = MAX_CREDIBLE_MS,
    ): Verdict {
        if (previousFrameNs <= 0L) return Verdict.None
        // ساعة رجعت للخلف أو تكرّر الإطار: لا مقارنة صالحة، ولا انسداد.
        if (frameNs <= previousFrameNs) return Verdict.None
        val gapMs = (frameNs - previousFrameNs) / 1_000_000L
        return when {
            gapMs > ceilingMs -> Verdict.Ignored(gapMs)
            gapMs >= thresholdMs -> Verdict.Stall(gapMs)
            else -> Verdict.None
        }
    }
}
