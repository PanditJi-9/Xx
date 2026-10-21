/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * حكم الملكية: هل هذا السحب الأفقي **تنقّلٌ** يملكه الجذر، أم **يملكه عنصر في الشاشة**؟
 *
 * **العطب الذي يمنعه.** كان الجذر يلغي التنقّل متى استهلكت أي حركة من الأبناء بعد حدّ اللمس.
 * لكن كل شاشة تقريبًا فيها قائمة رأسية، وهي تستهلك الحركة بمجرد أن يتجاوز الإصبع الحدّ رأسيًّا،
 * ولو كان السحب أفقيًّا في معظمه. فيُلغى التنقّل فوق الشاشات كلها تقريبًا، ويبدو التمرير الأفقي معطّلًا.
 * ولا يكفي سؤال «هل استُهلكت؟» لأن Compose لا يخبر أيّ محور استهلكته الحركة.
 *
 * **الحلّ: المحور يُعرف من المصدر.** كل قابل للتمرير يُبلغ الأجداد عبر [NestedScrollConnection]
 * بما استهلكه فعلًا وعلى أيّ محور:
 *  - صفّ أو Pager أفقي استهلك على المحور X ⇒ هو صاحب السحب الأفقي، فيتنحّى الجذر.
 *  - قائمة رأسية تحرّكت على المحور Y ⇒ لا تُلغي التنقّل، فهي لا تملك المحور الأفقي.
 *  - عنصر غير تمرير (منزلق، مقبض، تحديد نص) استهلك حركة أفقية قبل أي حركة رأسية ⇒ صاحبها، فيتنحّى.
 *
 * **حدّ الحكم:** صفّ أفقي في نهايته لا يستهلك شيئًا، فلا يُعدّ مالكًا، ويصير السحب تنقّلًا كالمعتاد.
 *
 * كل شيء على خيط الواجهة وحده، ويُصفَّر عند نزول كل إصبع جديد.
 */
internal class SwipeGestureArbiter {

    /** صفّ أو Pager أفقي استهلك جزءًا من حركة هذه الإيماءة. */
    private var horizontalScrollTookIt = false

    /** قائمة رأسية استهلكت جزءًا من حركة هذه الإيماءة. */
    private var verticalScrollMoved = false

    /** عنصر غير تمرير استهلك حركة أفقية، ولم تتحرّك قبلها قائمة رأسية. */
    private var nonScrollTookIt = false

    /** هل أخذ محتوى الشاشة هذه الإيماءة لنفسه؟ فإن كان، فلا يُعدّ السحب تنقّلًا. */
    val isTakenByContent: Boolean
        get() = horizontalScrollTookIt || nonScrollTookIt

    /** يُستدعى عند نزول إصبع جديد. */
    fun beginGesture() {
        horizontalScrollTookIt = false
        verticalScrollMoved = false
        nonScrollTookIt = false
    }

    /**
     * يُستدعى من مستشعر الجذر مع كل حدث، بعد أن مرّ على الأبناء.
     *
     * @param consumedPastSlop هل استهلك أحدٌ هذا الحدث، والإصبع قد تجاوز حدّ اللمس؟
     * @param dx إزاحة الإصبع الأفقية منذ النزول.
     * @param dy إزاحة الإصبع الرأسية منذ النزول.
     */
    fun onPointerEvent(consumedPastSlop: Boolean, dx: Float, dy: Float) {
        // الاستهلاك الذي سبقته حركة رأسية منسوب إلى القائمة، لا إلى عنصر أفقي آخر.
        if (consumedPastSlop && !verticalScrollMoved && abs(dx) > abs(dy) * SWIPE_SLOPE_RATIO) {
            nonScrollTookIt = true
        }
    }

    /** الاتصال المتداخل الذي يسجّله الجذر ليرى ما استهلكه الأبناء القابلون للتمرير. */
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source == NestedScrollSource.UserInput) {
                if (consumed.x != 0f) horizontalScrollTookIt = true
                if (consumed.y != 0f) verticalScrollMoved = true
            }
            return Offset.Zero
        }
    }
}
