/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nd.max.R
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.HomeNoteStore

/** أقصى عدد أحرف تُحفظ. سقفٌ معلن يكفي اسمًا أو جملةً أو فقرات، ويمنع لصقَ نصٍّ يُثقل التخزين. */
internal const val HOME_NOTE_MAX_CHARS = 5000

/**
 * نافذة الكتابة الحرّة، تُفتح من كلمة **MAX** في رأس الشاشة الرئيسية.
 *
 * ما يُكتب فيها يُحفظ عند الإغلاق، بالزرّ أو بالرجوع، ويعود عند فتحها مرّة أخرى.
 *
 * **النصّ الطويل له شكلٌ محسوب** لا يُترك للصدفة:
 *  - الحقل يملأ ما بين الرأس والتذييل، فيلفّ الجملة عند حافّته، ولا يخرج اسمٌ طويل عن الشاشة.
 *  - ما زاد ارتفاعه عن المساحة يُمرَّر داخل الحقل نفسه ويتبع المؤشّر، ويبقى التذييل ظاهرًا.
 *  - الكتابة محدودة بـ[HOME_NOTE_MAX_CHARS] حرفًا، وهو سقفٌ معلن.
 *
 * **ولوحة المفاتيح:** النافذة حافّةٌ إلى حافّة (`decorFitsSystemWindows = false`)، و`safeDrawing`
 * يشمل شريطَي النظام ولوحة المفاتيح معًا، فيتقلّص الحقل فوق اللوحة بدل أن تغطيه.
 */
@Composable
internal fun HomeNoteWindow(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { HomeNoteStore.of(context) }
    // المحفوظ يُقرأ لحظة الفتح، ويبقى المكتوب عند تدوير الشاشة.
    var text by rememberSaveable { mutableStateOf(store.text) }
    // الحفظ عند مغادرة التكوين: يشمل الإغلاق بالزرّ وبالرجوع معًا.
    DisposableEffect(store) {
        onDispose { store.text = text }
    }

    val scheme = MaterialTheme.colorScheme
    // لون النصّ صريح: حقلٌ أساسيّ بلا لون يرث الأسود (وهذا ما أصلحناه في شاشة التطبيقات).
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.home_note_close),
                            tint = scheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_note_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(start = MaxSpace.xs),
                    )
                }

                // الحقل يملأ ما بقي. التمرير داخله يعطيه ارتفاعه الحقيقيّ، فلا يُقصّ السطر الأخير.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = MaxSpace.lg),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(HOME_NOTE_MAX_CHARS) },
                        textStyle = bodyStyle,
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.home_note_placeholder),
                                        style = bodyStyle,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_note_saved_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.home_note_counter, text.length, HOME_NOTE_MAX_CHARS),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
