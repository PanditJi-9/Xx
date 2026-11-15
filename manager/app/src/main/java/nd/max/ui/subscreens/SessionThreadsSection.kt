/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * خيوط الجلسة المنتهية — امتدادٌ لصفّ الخيوط نفسه، لا ثانٍ له.
 *
 * اللقطة الحيّة تقول من يستهلك **الآن**؛ وهذه تقول أين ذهب المعالج **طوال الجلسة**. والفرق
 * في المصدر لا في الشكل: فرق نبضات بين قراءتين عند حدّي التسجيل، لا لقطة لحظية ولا معدّل
 * مخترع. فإن غابت إحدى القراءتين غاب القسم كله — لا أصفارًا تُقرأ قياسًا (ADR-07).
 */
package nd.max.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.platform.SessionThreadLoad

/**
 * أكثر خيوط اللعبة استهلاكًا في الجلسة، مع حصتها من زمن اللعبة نفسها.
 *
 * ولا نواة هنا: الخيط ينتقل بين النوى على مدى جلسة، فكتابة نواةٍ واحدة له كذبٌ بنيّته حسنة.
 */
@Composable
internal fun SessionThreadsSection(loads: List<SessionThreadLoad>?) {
    ThreadRowsSection(
        title = stringResource(R.string.game_sessions_threads_title),
        caption = stringResource(R.string.game_sessions_threads_caption),
        rows = remember(loads) {
            loads?.map { ThreadRowFacts(it.tid, it.name, core = null, ticks = it.ticks) }
        }
    )
}
