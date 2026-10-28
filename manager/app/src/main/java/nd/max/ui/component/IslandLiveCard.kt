/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.island.core.IslandSettings
import nd.max.island.engine.DynamicIslandUi
import nd.max.island.engine.IslandLevel
import nd.max.island.engine.IslandMode
import nd.max.island.engine.IslandTheme
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/**
 * بطاقة المعاينة الحيّة — قلب شاشة الجزيرة.
 *
 * **وما تراه هنا هو ما يُرسم فوق التطبيقات حرفيًّا:** مكوّن `DynamicIslandUi` نفسه الذي ترسمه
 * الطبقة، وبداخل `IslandTheme` نفسه — فاللون والإضاءة والحركة تتبع إعدادات الجزيرة لا لون
 * شاشة الإعدادات. وكانت في الاستوديو `PreviewCard`؛ نُقلت هنا بعد الدمغ مع فارق واحد مقصود:
 * القياسات تُقرأ من [IslandSettings] مباشرة (كما في الاستوديو)، فأي منزلق في الشاشة ينعكس
 * على هذه المعاينة في اللحظة نفسها.
 *
 * والرقائق تحت المسرح: «نافذة» (شكل النافذة السريعة) ثم الأنواع السبعة — نقرة تُبدّل العيّنة،
 * واختيار البطاقة يعيدها إلى الوضع الموسّع. وهذا هو سطح الاختبار نفسه الذي في قسم «جرّب الآن»،
 * لكن هنا بلا تنفيذ: معاينة فقط، فلا شيء يُنبَّه عليه فعليًّا.
 */
@Composable
internal fun IslandLiveCard(modifier: Modifier = Modifier) {
    val scale by IslandSettings.scale.collectAsState()
    val w by IslandSettings.widthScale.collectAsState()
    val h by IslandSettings.heightScale.collectAsState()
    val corner by IslandSettings.corner.collectAsState()
    val speed by IslandSettings.animSpeed.collectAsState()
    val glow by IslandSettings.glow.collectAsState()
    val haptics by IslandSettings.haptics.collectAsState()
    val appAccentOn by IslandSettings.islandAppAccent.collectAsState()
    val splitIsland by IslandSettings.splitIsland.collectAsState()
    val cutoutAnchor by IslandSettings.cutoutAnchor.collectAsState()
    val hyperCharge by IslandSettings.hyperCharge.collectAsState()
    val themeStyle by IslandSettings.islandThemeStyle.collectAsState()

    var demo by remember { mutableStateOf(IslandDemo.Notification) }
    var level by remember { mutableStateOf(IslandLevel.Expanded) }

    Surface(
        shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(bottom = MaxSpace.md)) {
            IslandTheme {
                IslandDemoStage(height = 210.dp, modifier = Modifier.padding(10.dp)) {
                    // محاكاة ثقب الكاميرا الأمامية حسب موضع الارتكاز (وسط / يسار / يمين)
                    val punchAlign = when (cutoutAnchor) {
                        1 -> Alignment.TopStart
                        2 -> Alignment.TopEnd
                        else -> Alignment.TopCenter
                    }
                    val punchPad = when (cutoutAnchor) {
                        1 -> Modifier.padding(start = 24.dp, top = 26.dp)
                        2 -> Modifier.padding(end = 24.dp, top = 26.dp)
                        else -> Modifier.padding(top = 26.dp)
                    }
                    Box(
                        modifier = Modifier
                            .align(punchAlign)
                            .then(punchPad)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF2E2E30), CircleShape)
                    )

                    Box(Modifier.padding(top = 14.dp)) {
                        val activeMode = if (demo == IslandDemo.Charging && hyperCharge) {
                            IslandMode.Charging(88, true, 120)
                        } else {
                            demo.toMode()
                        }
                        val secMode = if (splitIsland && level == IslandLevel.Compact && demo == IslandDemo.Media) {
                            IslandMode.Timer(42, 60)
                        } else null

                        DynamicIslandUi(
                            mode = activeMode,
                            level = level,
                            scale = if (level == IslandLevel.Banner) scale else scale.coerceAtMost(1.25f),
                            widthScale = w,
                            heightScale = h,
                            cornerFactor = corner,
                            animSpeed = speed,
                            glow = glow,
                            haptics = haptics,
                            secondary = secMode,
                            appAccent = if (appAccentOn) MaterialTheme.colorScheme.primary else null,
                            onClick = {
                                level = if (level == IslandLevel.Expanded) IslandLevel.Compact else IslandLevel.Expanded
                            },
                            onLongPress = { level = IslandLevel.Expanded },
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                FilterChip(
                    selected = level == IslandLevel.Banner,
                    onClick = { level = IslandLevel.Banner },
                    label = { Text(stringResource(R.string.ify_preview_banner)) },
                    leadingIcon = { Icon(Icons.Rounded.Notifications, null, Modifier.size(18.dp)) }
                )
                IslandDemo.entries.forEach { d ->
                    FilterChip(
                        selected = demo == d && level != IslandLevel.Banner,
                        onClick = { demo = d; level = IslandLevel.Expanded },
                        label = { Text(stringResource(d.label)) },
                        leadingIcon = { Icon(d.icon, null, Modifier.size(18.dp)) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.ify_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.lg)
            )
        }
    }
}
