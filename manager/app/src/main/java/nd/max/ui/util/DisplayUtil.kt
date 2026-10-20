/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.util

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Generates downscale factor steps from 0.30 to 0.95 in 0.05 increments,
 * with "default" (native, no downscale) placed last. 1.00 is omitted
 * since it's functionally identical to "default".
 *
 * **وُجدت قبل §9.5 وبقيت مرجعًا للنطاق:** هذا سلّم المعاملات الخام الذي كان يُعرض للمستخدم
 * أرقامًا («0.50») بلا معنى، وهو اليوم سجلّ الحدّ الذي بُني عليه النطاق المدعوم في
 * [canvasScaleOptions] — ولا يُوسَّع ولا يُضاف إليه خيار جديد.
 */
fun getSupportedDownscaleFactors(): List<String> {
    val factors = mutableListOf<String>()
    var current = 0.30
    while (current <= 0.951) {
        factors.add(String.format(Locale.US, "%.2f", current))
        current += 0.05
    }
    factors.add("default")
    return factors
}

/** لماذا لا تُعرض دقة مقترحة على هذه الشاشة — سببٌ بجملة تُقرأ وحدها، لا استبعاد صامت. */
enum class CanvasScaleSkip {
    /** الطلب ≥ دقة الشاشة: ليس تحجيمًا أصلًا، و«الأصلية» خيار قائم يغنيه. */
    MatchesScreen,

    /** أقل من أدنى معامل في المسار الحالي (0.30، نطاق [getSupportedDownscaleFactors]). */
    BelowRange,
}

/** كيف يُسمّى الخيار للمستخدم — ولا رقم خام إطلاقًا (§9.5). */
sealed interface CanvasScaleLabel {
    /** الدقة الأصلية للشاشة (`default`). */
    object Native : CanvasScaleLabel

    /** دقة مفهومة بالضلع القصير (720p · 1080p …). */
    data class ShortSide(val px: Int) : CanvasScaleLabel

    /** قيمة مخزونة قديمة/مخصّصة تُعرض نسبتها من الأصلية («مخصّص · 60%»). */
    data class Percent(val percent: Int) : CanvasScaleLabel
}

/**
 * خيار واحد من خيارات الدقة: ما يُخزَّن للخادم، وكيف يُسمّى، وإن كان يُعرض أصلًا.
 *
 * @param stored القيمة بالحرف كما يقرؤها مسار التطبيق (`default` أو `0.67`) — **ولا تتغيّر
 *   الصيغة** لأن `resolution_downscale` عقد قائم بين الواجهة ومُبدّل الدقة في الرفيق
 *   (`ResolutionChanger.c` يقرؤها معاملًا لواجهة Game Mode). فما تغيّر هنا هو **الواجهة**
 *   وحدها: تسمية مفهومة، وحساب مذكور — لا بروتوكول جديد.
 */
data class CanvasScaleOption(
    val stored: String,
    val label: CanvasScaleLabel,
    val skip: CanvasScaleSkip? = null,
) {
    val supported: Boolean get() = skip == null
}

/**
 * §9.5 — **من المعامل الخام إلى دقة مفهومة، بحساب لا بافتراض.**
 *
 * «720p» ليست دائمًا 0.50: هي حاصل `720 ÷ الضلع القصير لدقة الشاشة` — فيصير 0.67 على شاشة
 * 1080، و0.50 على شاشة 1440، و1.00 على شاشة 720 (وهي عندها «الأصلية» لا خيارًا). ولذلك
 * تُحسب الخيارات من [nativeShortSidePx] المقروء من `DisplayMetrics` في موضع العرض، ويُقاس
 * هذا الحساب في اختبار JVM بلا جهاز.
 *
 * **والآلية التي يقرؤها الرفيق** (`cmd game set --mode 2 --downscale …`) تستهدف **دقة تصيير
 * اللعبة الداخلية** عبر واجهات Android — لا دقة الشاشة المادية، ولا `wm size`. وهذا ما تقوله
 * الواجهة نصًّا (انظر `canvas_scale_mechanism`) بدل الإيحاء بتغيير الشاشة.
 *
 * @param nativeShortSidePx الضلع القصير لدقة الشاشة بالبكسل (‏0 أو أقل = غير معروفة، فلا
 *   تُخترع مقترحات حسابها باطل — يبقى «الأصلية» وحده).
 * @param includeStored القيمة المخزونة الآن؛ إن لم تكن ضمن المقترحات (اختيار قديم أو يدويّ)
 *   تُضاف صفًّا بنسبتها المئوية فلا يختفي إعداد المستخدم من القائمة.
 */
fun canvasScaleOptions(nativeShortSidePx: Int, includeStored: String? = null): List<CanvasScaleOption> {
    val options = mutableListOf(CanvasScaleOption("default", CanvasScaleLabel.Native))
    if (nativeShortSidePx > 0) {
        for (px in intArrayOf(1080, 720, 480)) {
            val factor = px.toFloat() / nativeShortSidePx
            val rounded = (factor * 100).roundToInt() / 100f
            val skip = when {
                // 0.99 لا 1.00: وإن قاربت الواحد فمآلها «الأصلية»، فلا يُعرض خيارٌ ثانٍ بمعناها.
                rounded >= 0.99f -> CanvasScaleSkip.MatchesScreen
                rounded < 0.30f -> CanvasScaleSkip.BelowRange
                else -> null
            }
            options += CanvasScaleOption(
                stored = String.format(Locale.US, "%.2f", rounded),
                label = CanvasScaleLabel.ShortSide(px),
                skip = skip,
            )
        }
    }
    if (includeStored != null && includeStored != "default" && options.none { it.stored == includeStored }) {
        val factor = includeStored.toFloatOrNull()
        if (factor != null && factor > 0f && factor < 1f) {
            options += CanvasScaleOption(
                stored = includeStored,
                label = CanvasScaleLabel.Percent((factor * 100).roundToInt()),
            )
        }
    }
    return options
}
