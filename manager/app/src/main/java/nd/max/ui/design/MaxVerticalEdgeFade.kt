/*
 * MaxManager Design Language — vertical edge fade.
 *
 * A scrolling list clipped by a bar ends in a hard horizontal line, which reads as a broken
 * layout. A short mask from transparent to opaque at that edge reads as "more content this way".
 * The fade applies only in the direction where content really continues, so the first and the
 * last row are never dimmed while they are fully visible.
 */
package nd.max.ui.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Fades the top and/or bottom edge of the element to transparent with an alpha mask.
 * A colour overlay would leave a visible band on the dark surface, so the mask is used instead.
 *
 * Apply it to the **viewport**, i.e. before `verticalScroll` or a `LazyColumn` in the chain.
 * Placed after a scroll modifier it is drawn over the whole scrolled content, so its band lands
 * at the end of the page, outside the visible area, and the edge never fades.
 *
 * The default [height] is [MaxSpace.sm] (8dp), deliberately shorter than the side fade
 * ([maxEdgeFade], [MaxSpace.gutter] = 20dp): the vertical edge only has to say "more content
 * this way", it should not darken the content next to the bar.
 */
fun Modifier.maxVerticalEdgeFade(
    fadeTop: Boolean,
    fadeBottom: Boolean,
    height: Dp = MaxSpace.sm,
): Modifier =
    if (!fadeTop && !fadeBottom) {
        this
    } else {
        this
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val band = height.toPx().coerceAtMost(size.height / 2f)
                if (fadeTop) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = band,
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, band),
                        blendMode = BlendMode.DstIn,
                    )
                }
                if (fadeBottom) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - band,
                            endY = size.height,
                        ),
                        topLeft = Offset(0f, size.height - band),
                        size = Size(size.width, band),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
    }
