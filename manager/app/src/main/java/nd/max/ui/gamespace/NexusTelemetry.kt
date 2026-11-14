/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.gamespace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.platform.HudSnapshot
import nd.max.ui.design.MaxSpace
import kotlin.math.roundToInt

/*
 * قراءة حيّة على شاشة الإطلاق — من `HudLive` نفسه الذي يغذّي التراكب، فلا مقياس ثانٍ.
 *
 * **وثلاث حالات لا اثنتان:** حيّ (عيّنة أحدث من خمس ثوانٍ) · «آخر قراءة» (عيّنة أقدم) ·
 * غياب كامل (لا جلسة لهذه اللعبة) فلا يُرسم شيء. والغياب لا يُعرض «—» ولا صفرًا: الشاشة
 * قبل الإطلاق لا تدّعي قياسًا لم يقع (ADR-07).
 */
@Immutable
data class NexusTelemetry(
    val live: Boolean,
    val fps: String?,
    val cpu: String?,
    val heat: String?,
)

private const val TelemetryLiveMs = 5_000L

/** `null` إن لم تكن هناك عيّنة، أو كانت العيّنة لِلعبة أخرى — والفارغ ليس صفرًا. */
internal fun nexusTelemetry(snapshot: HudSnapshot, packageName: String?, nowMs: Long): NexusTelemetry? {
    val reading = snapshot.reading ?: return null
    if (snapshot.sampledAtMs <= 0L) return null
    if (packageName != null && snapshot.owner != packageName) return null
    val result = NexusTelemetry(
        live = nowMs - snapshot.sampledAtMs <= TelemetryLiveMs,
        fps = reading.frames?.roundToInt()?.toString(),
        cpu = reading.cpu?.takeIf { it >= 0 }?.toString(),
        heat = reading.heat?.roundToInt()?.toString(),
    )
    return if (result.fps == null && result.cpu == null && result.heat == null) null else result
}

@Composable
internal fun NexusTelemetryRow(telemetry: NexusTelemetry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        telemetry.fps?.let { NexusTelemetryCell(stringResource(R.string.gs_tele_fps), it) }
        telemetry.cpu?.let { NexusTelemetryCell(stringResource(R.string.gs_tele_cpu), it) }
        telemetry.heat?.let { NexusTelemetryCell(stringResource(R.string.gs_tele_heat), it) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(MaxSpace.sm)
                    .clip(CircleShape)
                    .background(if (telemetry.live) Monolith.Mint else Monolith.Faint)
            )
            Text(
                text = stringResource(if (telemetry.live) R.string.gs_tele_live else R.string.gs_tele_stale),
                style = Monolith.label,
                color = Monolith.Muted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NexusTelemetryCell(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Text(text = value, style = Monolith.heading, color = Monolith.Bone, maxLines = 1)
        Text(text = label, style = Monolith.label, color = Monolith.Muted, maxLines = 1)
    }
}
