/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.ui.util.CanvasScaleLabel
import nd.max.ui.util.CanvasScaleOption
import nd.max.ui.util.CanvasScaleSkip

/**
 * أسماء خيارات الدقة المفهومة (§9.5) — ملفّ واحد يقرؤه الموضعان: شاشة Per-App ومنتقٍ في
 * ملفّ اللعبة، فلا تتفرّع التسمية بين شاشتين (وكان الرقم الخام «0.50» يظهر في الاثنتين).
 */
@Composable
fun canvasScaleLabel(option: CanvasScaleOption): String = when (val label = option.label) {
    CanvasScaleLabel.Native -> stringResource(R.string.canvas_scale_native)
    is CanvasScaleLabel.ShortSide -> stringResource(R.string.canvas_scale_shortside, label.px)
    is CanvasScaleLabel.Percent -> stringResource(R.string.canvas_scale_percent, label.percent)
}

/** سبب استبعاد دقة — يُكتب بجانب اسمها، فلا يُستبعد خيار صامتًا. */
@Composable
fun canvasScaleSkipReason(skip: CanvasScaleSkip): String = when (skip) {
    CanvasScaleSkip.MatchesScreen -> stringResource(R.string.canvas_scale_same_screen)
    CanvasScaleSkip.BelowRange -> stringResource(R.string.canvas_scale_below_range)
}
