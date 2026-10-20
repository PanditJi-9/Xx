/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import nd.max.R
import nd.max.island.core.IslandKind
import nd.max.island.core.IslandSettings
import nd.max.island.core.KindStyle
import nd.max.island.engine.DynamicIslandUi
import nd.max.island.engine.IslandLevel
import nd.max.island.engine.IslandTheme
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace

/** سطر موجز لنمط النافذة في قوائم الشاشة — «110% × 120% · 100%»، و«عادي» للافتراضيّ. */
@Composable
internal fun kindStyleSummary(s: KindStyle): String {
    val normal = stringResource(R.string.ify_preset_normal)
    return if (s == KindStyle()) {
        normal
    } else {
        "${(s.w * 100).roundToInt()}% × ${(s.h * 100).roundToInt()}%  ·  ${(s.text * 100).roundToInt()}%"
    }
}

/**
 * ورقة تعديل نافذة واحدة — أو كل النوافذ دفعةً (`all = true`).
 *
 * **وهي استوديو الجزيرة مصغَّرًا في ورقة:** كانت `KindEditorSheet` تسكن نشاطًا مستقلًّا، والآن
 * تفتح من شاشة الجزيرة نفسها بهوية MaxManager: الورقة [CustomBottomSheet] المشتركة، والمنزلقات
 * [MaxSliderRow] المشتركة، وكل ما بقي خاصًّا بالجزيرة (المعاينة الحيّة والأيقونات) من
 * [IslandUiKit] و[IslandLiveCard].
 *
 * **والتعديل يُطبَّق على مجموعة مختارة لا على نوع واحد دائمًا:** فتح «كل النوافذ» يجعل كل منزلق
 * هنا يكتب في كل أنواع النوافذ المُلحَقة — نفس سلوك الاستوديو حرفيًّا، فلا يفاجأ من اعتاده.
 *
 * @param kind النافذة المفتوحة، أو `null` مع [all] = تعديل المجموعة كلها.
 * @param all true = مجمّع النوافذ السريعة (كل أنواع `popup`) تُعدَّل معًا.
 */
@Composable
internal fun IslandKindSheet(kind: IslandKind?, all: Boolean, onClose: () -> Unit) {
    val visible = all || kind != null
    val kinds = remember(all, kind) {
        when {
            all -> IslandKind.entries.filter { it.popup }
            kind != null -> listOf(kind)
            else -> emptyList()
        }
    }
    val previewKind = kinds.firstOrNull() ?: IslandKind.Notification
    val styles by IslandSettings.kindStyles.collectAsState()
    val s = styles[previewKind.key] ?: KindStyle()
    val globalCorner by IslandSettings.corner.collectAsState()
    val appScale by IslandSettings.scale.collectAsState()
    val speed by IslandSettings.animSpeed.collectAsState()
    val glow by IslandSettings.glow.collectAsState()
    val appAccentOn by IslandSettings.islandAppAccent.collectAsState()

    val popup = previewKind.popup
    var showCard by rememberSaveable { mutableStateOf(false) }
    val smallLevel = if (popup) IslandLevel.Banner else IslandLevel.Compact

    // التعديل الواحد يُطبَّق على كل النوافذ المفتوحة في هذه الورقة ثم يُحفظ: تدفّق واحد، وكتابة واحدة
    fun update(n: KindStyle) = kinds.forEach { IslandSettings.setKindStyle(it, n) }
    fun commit() = IslandSettings.save()
    fun preset(w: Float, h: Float, t: Float) {
        update(s.copy(w = w, h = h, text = t))
        commit()
    }

    CustomBottomSheet(visible = visible, onDismiss = onClose) {
        if (!visible) return@CustomBottomSheet
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaxSpace.xl),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(previewKind.icon(), null, Modifier.size(22.dp))
                Spacer(Modifier.width(MaxSpace.sm))
                Text(
                    text = if (all) stringResource(R.string.ify_banner_all) else stringResource(previewKind.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // المعاينة الحيّة لنفس النافذة — بالمحرّك الحقيقي لا برسم يدويّ
            IslandTheme {
                IslandDemoStage(height = if (showCard) 300.dp else 170.dp) {
                    Box(Modifier.padding(top = 14.dp)) {
                        DynamicIslandUi(
                            mode = previewKind.demo(),
                            level = if (showCard) IslandLevel.Expanded else smallLevel,
                            scale = if (showCard) 0.85f else appScale.coerceAtMost(1f),
                            cornerFactor = globalCorner,
                            animSpeed = speed,
                            glow = glow,
                            haptics = false,
                            appAccent = if (appAccentOn) MaterialTheme.colorScheme.primary else null,
                            onClick = { showCard = !showCard },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                FilterChip(
                    selected = !showCard,
                    onClick = { showCard = false },
                    label = { Text(stringResource(if (popup) R.string.ify_preview_banner else R.string.ify_preview_pill)) }
                )
                FilterChip(
                    selected = showCard,
                    onClick = { showCard = true },
                    label = { Text(stringResource(R.string.ify_preview_card)) }
                )
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                BouncyChip(stringResource(R.string.ify_preset_normal)) { preset(1f, 1f, 1f) }
                BouncyChip(stringResource(R.string.ify_preset_large)) { preset(1.3f, 1.3f, 1.2f) }
                BouncyChip(stringResource(R.string.ify_preset_wide)) { preset(1.5f, 1.2f, 1.1f) }
                BouncyChip(stringResource(R.string.ify_preset_tall)) { preset(1.2f, 1.6f, 1.3f) }
            }

            SubTitle(stringResource(if (popup) R.string.ify_kind_sec_banner else R.string.ify_kind_sec_pill))
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_banner_width),
                    value = s.w,
                    onValueChange = { update(s.copy(w = it)) },
                    valueText = "${(s.w * 100).roundToInt()}%",
                    valueRange = 0.8f..1.8f,
                    steps = 9,
                    onValueChangeFinished = { commit() }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_banner_height),
                    value = s.h,
                    onValueChange = { update(s.copy(h = it)) },
                    valueText = "${(s.h * 100).roundToInt()}%",
                    valueRange = 0.8f..2f,
                    steps = 11,
                    onValueChangeFinished = { commit() }
                )
            }

            SubTitle(stringResource(R.string.ify_kind_sec_card))
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_card_width),
                    value = s.cardW,
                    onValueChange = { update(s.copy(cardW = it)); showCard = true },
                    valueText = "${(s.cardW * 100).roundToInt()}%",
                    valueRange = 0.7f..1.3f,
                    steps = 5,
                    onValueChangeFinished = { commit() }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_card_height),
                    value = s.cardH,
                    onValueChange = { update(s.copy(cardH = it)); showCard = true },
                    valueText = "${(s.cardH * 100).roundToInt()}%",
                    valueRange = 0.7f..1.6f,
                    steps = 8,
                    onValueChangeFinished = { commit() }
                )
            }

            SubTitle(stringResource(R.string.ify_kind_sec_look))
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_banner_text),
                    value = s.text,
                    onValueChange = { update(s.copy(text = it)) },
                    valueText = "${(s.text * 100).roundToInt()}%",
                    valueRange = 0.8f..1.8f,
                    steps = 9,
                    onValueChangeFinished = { commit() }
                )
                val cornerShown = if (s.corner < 0f) globalCorner else s.corner
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_roundness),
                    value = cornerShown,
                    onValueChange = { update(s.copy(corner = it)) },
                    valueText = "${(cornerShown * 100).roundToInt()}%",
                    valueRange = 0.2f..1f,
                    steps = 7,
                    onValueChangeFinished = { commit() }
                )
                if (popup) {
                    MaxGroupDivider()
                    MaxSliderRow(
                        title = stringResource(R.string.ify_banner_duration),
                        value = s.durationSec.toFloat(),
                        onValueChange = { update(s.copy(durationSec = it.roundToInt())) },
                        valueText = if (s.durationSec == 0) {
                            stringResource(R.string.ify_banner_auto)
                        } else {
                            stringResource(R.string.ify_cust_seconds, s.durationSec)
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        onValueChangeFinished = { commit() }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaxSpace.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { update(KindStyle()); commit() }) {
                    Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(MaxSpace.sm))
                    Text(stringResource(R.string.ify_banner_reset))
                }
                Button(onClick = onClose) { Text(stringResource(R.string.ify_common_done)) }
            }
        }
    }
}

/** عنوان فرعيّ داخل الورقة — فوق كل زمرة منزلقات. */
@Composable
private fun SubTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = MaxSpace.xs)
    )
}
