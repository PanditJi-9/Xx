/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * الخيوط — **زمن المعالج لكل خيط** داخل العملية المفتوحة.
 *
 * كان السطح يقول عدد الخيوط فقط («٢٤ خيطًا»)، والعدد لا يقول أين يذهب المعالج: عمليةٌ بخيط
 * واحد مشغول تُقرأ كعملية هادئة ذات أربعة وعشرين خيطًا. فالناقص لم يكن عدًّا آخر بل **توزيعًا**:
 * من يستهلك ومن ينتظر.
 *
 * والقياس من `/proc/<pid>/task/<tid>/stat` عبر [ProcessFeed.threadTimes] — زمن تراكمي منذ
 * إنشاء الخيط، لا لقطة لحظية تحتاج عيّنتين. والحصة المعروضة هي من **زمن هذه العملية نفسها**
 * لا من زمن الجهاز كلّه، وهذا مكتوب في الوصف حتى لا يُقرأ الرقم على غير وجهه.
 *
 * وحدوده المعلنة (ADR-07): ما لم يُقرأ لا يُعرض، والثواني تحتاج `_SC_CLK_TCK` من النواة؛
 * فإن غاب الثابت عُرضت الحصة وحدها وموضع الثواني «—»، لا رقم مخمَّن على أساس ١٠٠ نبضة.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import nd.max.R
import nd.max.core.platform.ProcessFeed
import nd.max.core.platform.ThreadTime
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/** كم خيطًا يُعرض. الستة الأعلى استهلاكًا تصف العملية؛ والباقي ذيلٌ لا يغيّر القرار. */
private const val THREADS_SHOWN = 6

/** ألفة مسار الخيط الفارغ — يُرى الحدّ ولا يُقرأ كأنه قياس. */
private const val ThreadTrackAlpha = 0.18f

/**
 * توزيع زمن المعالج على خيوط العملية.
 *
 * @param times مرتّبة تنازليًّا من الطبقة البيانات؛ `null` تعني «لم تُقرأ» فتُعرض لا شيء
 *        بدل قائمة فارغة تُوهم بأن العملية بلا خيوط (ADR-07).
 */
@Composable
internal fun ProcessThreadsSection(times: List<ThreadTime>?) {
    if (times.isNullOrEmpty()) return
    val colors = MaterialTheme.colorScheme
    val ticksPerSecond = remember { ProcessFeed.clockTicksPerSecond() }
    val total = times.sumOf { it.ticks }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(
            text = stringResource(R.string.processmgr_threads_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.processmgr_threads_caption),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(MaxSpace.xs))
        times.take(THREADS_SHOWN).forEach { thread ->
            ThreadRow(thread = thread, total = total, ticksPerSecond = ticksPerSecond)
        }
    }
}

/** خيط واحد: اسمه، وزمنه، وحصته من زمن العملية — ثم مسارٌ يقول الحصة بلا قراءة رقم. */
@Composable
private fun ThreadRow(thread: ThreadTime, total: Long, ticksPerSecond: Long?) {
    val colors = MaterialTheme.colorScheme
    // النصوص تُحسب قبل التخطيط: `stringResource` نداء @Composable لا يُستدعى داخل تعبير شرطي
    // خارج نطاق التركيب.
    val seconds = if (ticksPerSecond != null && ticksPerSecond > 0L) {
        stringResource(
            R.string.processmgr_threads_seconds,
            "%.1f".format(thread.ticks.toFloat() / ticksPerSecond)
        )
    } else {
        MAX_VALUE_UNAVAILABLE
    }
    val share = if (total > 0L) {
        stringResource(R.string.processmgr_threads_share, (thread.ticks * 100 / total).toInt())
    } else {
        MAX_VALUE_UNAVAILABLE
    }
    // النواة تُعرض حين قرأتها النواة؛ وغيابها يُسقط النصّ ولا يُكتب «النواة —» بلا معنى.
    val core = if (thread.lastCore != null) {
        stringResource(R.string.processmgr_threads_core, thread.lastCore)
    } else {
        null
    }
    val fraction = if (total > 0L) {
        (thread.ticks.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.processmgr_threads_tid, thread.tid),
                // `bodySmall` لا `labelSmall`: أرضية النصّ في المستودع ١٢sp، وlabelSmall ١١.
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = thread.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (core != null) {
                Text(
                    text = core,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                text = seconds,
                // أرقام بعرض ثابت فتنتظم في عمود واحد تحت بعضها.
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.onSurfaceVariant
            )
            Text(
                text = share,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaxSpace.xs)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(colors.onSurfaceVariant.copy(alpha = ThreadTrackAlpha))
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(colors.primary)
                )
            }
        }
    }
}
