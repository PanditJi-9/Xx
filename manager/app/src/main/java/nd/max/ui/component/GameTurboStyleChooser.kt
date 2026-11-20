/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import nd.max.R
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.util.GameTurboStyle
import nd.max.ui.util.GameTurboStylePrefs

/**
 * مُستضيف نافذة اختيار مظهر Game Turbo، **مُثبَّت داخل مساحة الألعاب** ([nd.max.ui.subscreens.GameLobbyScreen])
 * فيُسجّل نافذته في [LocalRootDialogs] ما دامت الشاشة حيّة (نفس نمط `ConfigUpdateHost`) — فلا تظهر على
 * الشاشة الرئيسية بأمر المالك.
 *
 * ### متى تظهر — قاعدتان لا ثالثة
 *
 * ① **أول مرة**: لم يختر المستخدم بعد ([GameTurboStylePrefs.chosen] = `false`) ⇒ تظهر مرة واحدة، ثم لا تعود.
 * ② **بالطلب**: [GameTurboStylePrefs.requestChooser] يعيد عرضها؛ ولا مستدعي له في الواجهة اليوم لأن صفّ
 *    «مظهر Game Turbo» في أدوات المساحة يحفظ الاختيار مباشرةً.
 *
 * [enabled] = `false` يُسكتها (أثناء الترحيب أو جولة التعريف) فلا تُزاحم شاشةً لم تنتهِ، وتظهر بعدها.
 */
@Composable
fun GameTurboStyleHost(enabled: Boolean) {
    val dialogs = LocalRootDialogs.current
    val key = remember { UUID.randomUUID().toString() }
    DisposableEffect(key, enabled) {
        if (enabled) dialogs[key] = @Composable { GameTurboStyleSurface() }
        onDispose { dialogs.remove(key) }
    }
}

@Composable
private fun GameTurboStyleSurface() {
    val context = LocalContext.current
    val requests by GameTurboStylePrefs.chooserRequests.collectAsStateWithLifecycle()
    var seenRequests by remember { mutableIntStateOf(requests) }
    var chosen by remember { mutableStateOf(GameTurboStylePrefs.chosen(context)) }
    val open = !chosen || requests != seenRequests
    if (!open) return

    // يبدأ من المحفوظ: الجديد (النيوني) إن لم يُختر شيء، وإلا ما اختاره المستخدم قبلًا.
    var selected by remember { mutableStateOf(GameTurboStylePrefs.style(context)) }

    fun settle(style: GameTurboStyle) {
        GameTurboStylePrefs.save(context, style)
        chosen = true
        seenRequests = requests
    }

    CustomContentDialog(
        visible = true,
        title = stringResource(R.string.game_turbo_style_title),
        // الرجوع أو النقر خارج النافذة = قبول الحالي بلا تبديل (النيوني أول مرة): لا تعود النافذة بعدها.
        onDismiss = { settle(GameTurboStylePrefs.style(context)) },
        onConfirm = { settle(selected) },
        confirmText = stringResource(R.string.game_turbo_style_confirm),
        dismissText = ""
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            Text(
                text = stringResource(R.string.game_turbo_style_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                StyleCard(
                    title = stringResource(R.string.game_turbo_style_neon),
                    tag = stringResource(R.string.game_turbo_style_default),
                    image = R.drawable.game_turbo_preview_neon,
                    selected = selected == GameTurboStyle.Neon,
                    onClick = { selected = GameTurboStyle.Neon },
                    modifier = Modifier.weight(1f)
                )
                StyleCard(
                    title = stringResource(R.string.game_turbo_style_classic),
                    tag = null,
                    image = R.drawable.game_turbo_preview_classic,
                    selected = selected == GameTurboStyle.Classic,
                    onClick = { selected = GameTurboStyle.Classic },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.game_turbo_style_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StyleCard(
    title: String,
    tag: String?,
    @DrawableRes image: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                if (selected) MaxSize.emphasisBorder else MaxSize.hairlineBorder,
                if (selected) scheme.primary else scheme.outlineVariant,
                shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_ASPECT)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) scheme.primary else scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (tag != null) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** نسبة صورتَي المعاينة (760×686): واحدة للبطاقتين فتتساويان ارتفاعًا. */
private const val PREVIEW_ASPECT = 760f / 686f
