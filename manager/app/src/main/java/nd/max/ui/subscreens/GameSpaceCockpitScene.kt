/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.AppIconCache
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.lobbyPress
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.theme.DisplayFontFamily

/**
 * **طبقات المشهد: ما يجعل الشاشة تُقرأ مُصيَّرة لا مرسومة.**
 *
 * المبدأ المقيس في نموذج ١:١ (٨٥٠×٣٩٢dp): الأرضيّة بمنظور تُثبّت الكاميرا، والهالات تُجمع بـ
 * `BlendMode.Plus` لأن الضوء يُضاف لا يُغطّى، والأيقونات البعيدة تُصغَّر إلى ١٦px ثم تُكبَّر فيتولّى
 * المُرشِّح الخطّيّ تمويهها (`Modifier.blur` يحتاج API 31 و`minSdk` عندنا 29).
 *
 * ولا صورة ولا أصل جديد: كل شيء تدرّج أو أيقونة تطبيق مثبَّتة من `AppIconCache`.
 */

/**
 * خلفيّة المشهد: قاعدة + شبكة أرضيّة + ضوء مجمَّع + حجب + غبار.
 *
 * كلها `Canvas` واحد يُعاد رسمه مع الشاشة فقط: لا حركة ولا تخصيص في الإطار، ولا ملف صورة.
 * والهالات تُرسم بـ`BlendMode.Plus` لأن الضوء في المشاهد الحقيقيّة **يُجمع**؛ ولو رُسمت شفافيّة
 * عاديّة لقرأت بقعًا رماديّة فوق بعضها — وهذا بالضبط ما جعل النسخ السابقة تبدو مسطّحة.
 */
@Composable
internal fun CockpitScene(accent: Color, ignition: Float, modifier: Modifier) {
    val motes = remember {
        val rnd = kotlin.random.Random(CockpitDustSeed)
        List(CockpitDustCount * 3) { rnd.nextFloat() }
    }
    val grain = rememberGrain()
    // الفرشاة تُحفظ لا تُنشأ كل إطار: `ShaderBrush` يخزّن تظليله بحسب المقاس.
    val grainBrush = remember(grain) { GrainBrush(grain) }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val lit = ignition.coerceIn(0f, 1f)
        val horizon = h * CockpitHorizon

        drawRect(
            Brush.verticalGradient(listOf(CockpitSkyTop, CockpitSkyMid, CockpitFloor))
        )

        // الأرضيّة: الأشعّة تتقارب نحو نقطة التلاشي، والخطوط الأفقيّة تتكاثف كلّما اقتربت.
        val gridColor = accent.copy(alpha = CockpitGridAlpha)
        val stroke = CockpitGridStroke.toPx()
        val half = CockpitGridRays / 2
        for (i in 0..CockpitGridRays) {
            val spread = (i - half) / half.toFloat()
            drawLine(
                color = gridColor,
                start = Offset(w * 0.5f, horizon),
                end = Offset(w * 0.5f + spread * w * CockpitGridReach, h + stroke),
                strokeWidth = stroke,
            )
        }
        for (k in 1..CockpitGridRows) {
            val t = k / CockpitGridRows.toFloat()
            val y = horizon + (h - horizon) * t * t
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = stroke,
            )
        }
        // حجاب يذيب الشبكة عند الأفق: الخطّ لا ينتهي بحدّ حادّ.
        drawRect(
            Brush.verticalGradient(
                listOf(CockpitFloor, CockpitFloor.copy(alpha = 0.92f), Color.Transparent),
                startY = horizon,
                endY = horizon + (h - horizon) * 0.45f,
            )
        )

        // الضوء: ثلاث هالات تُجمع، لا تُغطّى.
        drawRect(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = CockpitHaloAccent * lit), Color.Transparent),
                center = Offset(w * 0.30f, h * 0.36f),
                radius = w * 0.46f,
            ),
            blendMode = BlendMode.Plus,
        )
        // الانحراف اللونيّ: نفس الهالة بإزاحة بكسلين — الحافّة الحمراء والباردة تفصلها عن الخلف.
        drawRect(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFFF5A7A).copy(alpha = CockpitFringeWarm * lit),
                    Color.Transparent,
                ),
                center = Offset(w * 0.30f - CockpitFringePx * 2f, h * 0.36f),
                radius = w * 0.30f,
            ),
            blendMode = BlendMode.Plus,
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFF3AD8FF).copy(alpha = CockpitFringeCool * lit),
                    Color.Transparent,
                ),
                center = Offset(w * 0.30f + CockpitFringePx * 2f, h * 0.36f),
                radius = w * 0.30f,
            ),
            blendMode = BlendMode.Plus,
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(CockpitCool.copy(alpha = CockpitHaloCool * lit), Color.Transparent),
                center = Offset(w * 0.16f, h * 0.92f),
                radius = w * 0.36f,
            ),
            blendMode = BlendMode.Plus,
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(CockpitWarm.copy(alpha = CockpitHaloWarm * lit), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.12f),
                radius = w * 0.30f,
            ),
            blendMode = BlendMode.Plus,
        )

        // حجب: الشريط العلويّ والتنقّل يُقرآن فوق أي لعبة، بلونها مهما كان فاتحًا.
        drawRect(
            Brush.verticalGradient(
                listOf(CockpitScrim.copy(alpha = 0.80f), Color.Transparent),
                startY = 0f,
                endY = h * 0.20f,
            )
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, CockpitScrim.copy(alpha = 0.92f), CockpitScrim),
                startY = h * 0.58f,
                endY = h,
            )
        )
        // فينييت: الحوافّ تهبط فتُثبَّت العين على المركز.
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent,
                0.58f to Color.Transparent,
                1f to CockpitScrim.copy(alpha = 0.62f),
                center = Offset(w * 0.5f, h * 0.46f),
                radius = w * 0.78f,
            )
        )

        // غبار معلّق: ثلاثيّات ثابتة (بذرة واحدة) فلا يهتزّ المشهد بين إعادة رسم وأخرى.
        var i = 0
        while (i + 2 < motes.size) {
            val x = motes[i] * w
            val y = motes[i + 1] * h * 0.92f
            val s = motes[i + 2]
            drawCircle(
                color = Color.White.copy(alpha = 0.03f + 0.09f * s),
                radius = (0.4f + s) * density,
                center = Offset(x, y),
            )
            i += 3
        }

        // حبيبات: نسيج مبلَّط يُضاف لا يُغطّي — يمنع التدرّج من القراءة مسطّحًا رقميًّا.
        drawRect(
            brush = grainBrush,
            alpha = CockpitGrainAlpha,
            blendMode = BlendMode.Plus,
        )
    }
}

/** نسيج حبيبات ثابت: يُولَّد مرّة واحدة بذرة ثابتة، لا ملف صورة ولا أصل خارجيّ. */
@Composable
private fun rememberGrain(): ImageBitmap = remember {
    val n = CockpitGrainPx
    val px = IntArray(n * n)
    val rnd = kotlin.random.Random(CockpitDustSeed + 7)
    for (k in px.indices) {
        val v = rnd.nextInt(0, CockpitGrainMax + 1)
        px[k] = (v shl 24) or 0x00FFFFFF
    }
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    bmp.setPixels(px, 0, n, 0, 0, n, n)
    bmp.asImageBitmap()
}

private class GrainBrush(private val image: ImageBitmap) : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        ImageShader(image, TileMode.Repeated, TileMode.Repeated)
}

/** موضع واحد من العمق الميدانيّ: أيقونة لعبة أخرى تُقرأ ضوءًا لا تفصيلًا. */
private data class CockpitBokehSpot(
    val align: Alignment,
    val dx: Dp,
    val dy: Dp,
    val size: Dp,
    val alpha: Float,
)

private val CockpitBokehSpots = listOf(
    CockpitBokehSpot(Alignment.TopEnd, 42.dp, 6.dp, 208.dp, 0.20f),
    CockpitBokehSpot(Alignment.BottomEnd, 96.dp, 44.dp, 148.dp, 0.16f),
    CockpitBokehSpot(Alignment.BottomStart, 12.dp, 78.dp, 176.dp, 0.13f),
)

/**
 * العمق الميدانيّ في الخلف: أيقونات ألعاب أخرى خارج التركيز.
 *
 * ولا `Modifier.blur` هنا: هو يحتاج API 31 و`minSdk` عندنا 29. فالضباب يُصنع حقيقةً لا مرشّحًا:
 * تُصغَّر الأيقونة إلى ١٦px ثم تُكبَّر بـ`ContentScale.Crop`، فيتولّى المُرشِّح الخطّيّ التمويه.
 */
@Composable
internal fun CockpitBokeh(games: List<GameApp>, selected: String?, modifier: Modifier) {
    val others = remember(games, selected) {
        games.filter { it.packageName != selected }.take(CockpitBokehCount)
    }
    if (others.isEmpty()) return
    val flip = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(modifier) {
        others.forEachIndexed { i, game ->
            val spot = CockpitBokehSpots[i]
            val soft = rememberSoftIcon(game.packageName)
            if (soft != null) {
                Box(
                    modifier = Modifier
                        .align(spot.align)
                        .offset(x = spot.dx * flip, y = spot.dy)
                        .size(spot.size)
                        .graphicsLayer { alpha = spot.alpha }
                        .clip(RoundedCornerShape(CockpitSlabRadius)),
                ) {
                    Image(
                        bitmap = soft,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

/** نسخة مصغَّرة جدًّا من الأيقونة: تكبيرها هو الضباب المطلوب. */
@Composable
private fun rememberSoftIcon(packageName: String, px: Int = CockpitBokehPx): ImageBitmap? {
    val soft by produceState<ImageBitmap?>(null, packageName, px) {
        var icon = AppIconCache.get(packageName)
        var tries = 0
        while (icon == null && tries < CockpitAccentTries) {
            delay(CockpitAccentRetryMs)
            icon = AppIconCache.get(packageName)
            tries++
        }
        val source = icon?.asAndroidBitmap() ?: return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching {
                Bitmap.createScaledBitmap(source, px, px, true).asImageBitmap()
            }.getOrNull()
        }
    }
    return soft
}

/**
 * اللوح: أيقونة اللعبة كما هي، مكبَّرةً ومقطوعة بالحافتين، بإضاءة حافّة وحجاب سفليّ.
 *
 * وهو **طبقة على مستوى الشاشة** لا عنصرًا داخل صفّ: صفّ المشهد ١٨٤dp فقط، واللوح ٣٠٠dp — فلو جلس
 * داخله لصغُر إلى أيقونة عابرة، وهذا ما فعله التركيب السابق. ولهذا يخرج من الأعلى ومن حافة البداية.
 */
@Composable
internal fun CockpitSlab(
    packageName: String?,
    accent: Color,
    ignition: Float,
    modifier: Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outward = if (rtl) CockpitSlabBleed else -CockpitSlabBleed
    if (packageName == null) return
    Box(modifier, contentAlignment = Alignment.TopStart) {
        Box(
            modifier = Modifier
                .offset(x = outward, y = -CockpitSlabBleed)
                .size(CockpitSlabSize)
                .graphicsLayer {
                    alpha = ignition
                    translationY = (1f - ignition) * CockpitSlabBleed.toPx() * 2f
                }
                .drawBehind {
                    // هالة اللوح: ضوء اللعبة نفسه يُجمع تحت الحافة.
                    val r = size.minDimension * 1.05f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                            center = center,
                            radius = r,
                        ),
                        radius = r,
                        center = center,
                        blendMode = BlendMode.Plus,
                    )
                }
                .clip(RoundedCornerShape(CockpitSlabRadius))
                .background(
                    Brush.linearGradient(
                        listOf(accent, GameSpacePalette.VioletDeep, CockpitFloor)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // المادّة: الأيقونة نفسها مصغَّرة إلى ٢٤px ثم مكبَّرة — تمويه حقيقيّ لا مرشّح
            // (`Modifier.blur` يحتاج API 31 والحدّ الأدنى هنا 29)، فتقرأ نسيجًا لا أيقونةً ثانية.
            val material = rememberSoftIcon(packageName, CockpitSlabMaterialPx)
            if (material != null) {
                Box(
                    modifier = Modifier.matchParentSize().graphicsLayer {
                        scaleX = CockpitSlabMaterialScale
                        scaleY = CockpitSlabMaterialScale
                        alpha = CockpitSlabMaterialAlpha
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = material,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            // صبغة اللعبة + تعتيم سفليّ في طبقة واحدة: الحافّة العليا تبقى الأفتح.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.34f),
                                GameSpacePalette.VioletDeep.copy(alpha = 0.58f),
                                CockpitSlabShade.copy(alpha = 0.94f),
                            )
                        )
                    )
            )
            // لمعة زاويّة: تقرأ السطح زجاجًا لا ورقًا.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        // بلا `start`/`end`: التدرّج الافتراضيّ يقطَع حدود الرسم نفسها (قطريًّا).
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
            // الشعار الحادّ فوق المادّة: الأيقونة الحقيقيّة، لا بديل مرسوم.
            AppIconImage(
                packageName = packageName,
                size = CockpitSlabEmblem,
                contentDescription = null,
            )
            // إضاءة الحافّة: دافئة أعلى البداية، باردة أسفل النهاية، وخيط أبيض بينهما.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val radius = CornerRadius(CockpitSlabRadius.toPx())
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    CockpitWarm.copy(alpha = 0.95f),
                                    Color.Transparent,
                                    CockpitCool.copy(alpha = 0.70f),
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                            cornerRadius = radius,
                            style = Stroke(width = CockpitSlabRim.toPx()),
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.22f),
                            cornerRadius = radius,
                            style = Stroke(width = CockpitSlabHairline.toPx()),
                        )
                    }
            )
        }
    }
}

/** الفعل الأساسيّ: قرص أبيض واحد، وبقيّة الشاشة داكنة — لا زرّان يتنازعان النظر. */
@Composable
internal fun CockpitPlay(
    enabled: Boolean,
    launching: Boolean,
    accent: Color,
    onLaunch: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(MaxRadius.pill)
    Row(
        modifier = Modifier
            .height(CockpitPlayHeight)
            .lobbyPress(interaction, 0.96f)
            .drawBehind {
                // هالة دافئة تحت الزرّ: الضوء يخرج من اللوح إلى الفعل.
                drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = size.width * 0.62f,
                    ),
                    cornerRadius = CornerRadius(size.height * 0.5f),
                    blendMode = BlendMode.Plus,
                )
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFEDE6FF), Color(0xFFDCD2FF))
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !launching,
                role = Role.Button,
                onClick = onLaunch,
            )
            .padding(horizontal = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Box(
            modifier = Modifier
                .size(CockpitPlayDot)
                .clip(CircleShape)
                .background(GameSpacePalette.GlassDeep),
            contentAlignment = Alignment.Center,
        ) {
            if (launching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(CockpitPlayGlyph),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(CockpitPlayGlyph)
                        .graphicsLayer { if (rtl) scaleX = -1f },
                )
            }
        }
        Text(
            text = stringResource(R.string.gs_launch),
            color = GameSpacePalette.GlassDeep,
            fontSize = 17.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.padding(end = MaxSpace.md),
        )
    }
}

/** الفعل الثانويّ: دائرة زجاجيّة، هدفها ٥٢dp وحدّها خيط لا إطار. */
@Composable
internal fun CockpitRoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(CockpitRoundButton)
            .lobbyPress(interaction, 0.92f)
            .clip(CircleShape)
            .background(CockpitGlassVeil)
            .border(MaxSize.hairlineBorder, CockpitEdgeLight, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = GameSpacePalette.Ink.copy(alpha = 0.9f),
            modifier = Modifier.size(CockpitRoundGlyph),
        )
    }
}

