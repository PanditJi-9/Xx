/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import nd.max.BuildConfig
import nd.max.R
import nd.max.core.access.RemoteAccess
import nd.max.core.update.ConfigUpdates
import nd.max.core.update.UpdateError
import nd.max.core.update.UpdateGate
import nd.max.core.update.UpdateManager
import nd.max.core.update.UpdateManifests
import nd.max.core.update.UpdateVerdict
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.VersionIdentity
import nd.max.ui.viewmodel.UpdateViewModel
import nd.max.ui.design.maxRadius

/**
 * سطح التحديث — النافذة والحجب كلاهما، في مكوّن واحد يُستضاف في الجذر.
 *
 * **ولماذا في الجذر لا في شاشة:** الحكم أمرٌ **على التطبيق كله** لا على وجهة. ونافذةٌ تُبنى
 * داخل شاشةٍ تعني أن من لا يزور تلك الشاشة لا يرى التحديث أبدًا — وهو العطب الذي كان في
 * التحديث القديم القائم على الوحدة (يعمل لأن `MainActivity` تفتحه صدفةً). أمّا الحجب فلا
 * جدال فيه: يجب أن يمنع **أي** شاشة، ولذلك يُرسم فوق كل شيء بـ`zIndex`.
 *
 * وهذا الملفّ يحمل الأحكام الثلاثة كواجهات منفصلة صراحةً:
 *
 *  * [UpdateDialogSurface] — «يوجد جديد»: يُغلق (إلا إن كان إلزاميًّا)، ويحمل «تحديث الآن».
 *  * [UpdateBlockOverlay] — «هذه النسخة لم تُعد مسموحة» أو تعليقٌ حاجب: لا زرّ إغلاق،
 *    و`BackHandler` يبتلع الرجوع، والمخرج الوحيد هو التحديث (أو صفحة التنزيل إن وُجدت).
 *  * [UpdateCheckRow] — فحص يدوي في الإعدادات، حتى لا يكون الفحص رهن دورة تشغيل واحدة.
 */

/** يستضيف [UpdateSurface] في طبقة الحوارات الجذرية، ويطلق الفحص الخلفيّ مرةً واحدة. */
@Composable
fun UpdateHost(viewModel: UpdateViewModel = hiltViewModel()) {
    val dialogs = LocalRootDialogs.current
    val key = remember { UUID.randomUUID().toString() }
    DisposableEffect(key, viewModel) {
        dialogs[key] = @Composable { UpdateSurface(viewModel) }
        onDispose { dialogs.remove(key) }
    }
    LaunchedEffect(viewModel) { viewModel.autoCheck() }
}

@Composable
private fun UpdateSurface(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsState()
    // مفتاح «تعطيل التحديثات» (من شاشة التحكّم): يُسكِت **الإعلان** ولا يُسقط **الحجب** —
    // حجب النسخة الممنوحة أمانٌ لا إزعاج، وتطبيقٌ مقفل بلا نافذة أسوأ من نافذةٍ مزعجة.
    val updatesDisabled by UpdateGate.disabled.collectAsState()
    val decision = state.decision
    val maintaining = decision.verdict == UpdateVerdict.MAINTENANCE
    when {
        decision.verdict == UpdateVerdict.BLOCKED -> UpdateBlockOverlay(state, viewModel)
        maintaining && decision.blocking -> UpdateBlockOverlay(state, viewModel)
        updatesDisabled -> Unit
        maintaining && !state.noticeDismissed -> UpdateDialogSurface(state, viewModel)
        decision.verdict == UpdateVerdict.OPTIONAL || decision.verdict == UpdateVerdict.REQUIRED ->
            UpdateDialogSurface(state, viewModel)
        else -> Unit
    }
}

@Composable
private fun UpdateDialogSurface(state: UpdateManager.State, viewModel: UpdateViewModel) {
    val decision = state.decision
    val required = decision.verdict == UpdateVerdict.REQUIRED
    val maintaining = decision.verdict == UpdateVerdict.MAINTENANCE
    val busy = state.phase == UpdateManager.Phase.DOWNLOADING || state.phase == UpdateManager.Phase.INSTALLING
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val notes = remember(decision.message, language) { UpdateManifests.localized(decision.message, language) }
    val local = remember(context) { VersionIdentity.readApp(context) }

    AlertDialog(
        // الإلزامي لا يُغلق بأي طريق: لا نقرة خارجية ولا زرّ رجوع (AlertDialog يقبل الاثنين).
        onDismissRequest = {
            when {
                required -> Unit
                maintaining -> viewModel.dismissNotice()
                else -> viewModel.skip()
            }
        },
        shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
        icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
        title = {
            Text(
                text = stringResource(
                    when {
                        maintaining -> R.string.update_maintenance_title
                        required -> R.string.update_dialog_title_required
                        else -> R.string.update_dialog_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                val release = decision.release
                if (release != null) {
                    UpdateBodyText(stringResource(R.string.update_dialog_body, releaseLabel(release), local.display))
                    if (!maintaining) {
                        Text(
                            text = stringResource(R.string.update_release_notes_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (maintaining) {
                    // تعليقٌ بلا إصدارٍ أحدث يُعرض: نصّ المالك إن كتبه، وإلّا فكلمة افتراضية.
                    UpdateBodyText(stringResource(R.string.update_maintenance_body))
                }
                notes?.let { UpdateBodyText(it) }
                if (release != null && release.size > 0L) {
                    UpdateBodyText(
                        stringResource(R.string.update_size, Formatter.formatShortFileSize(context, release.size))
                    )
                }
                UpdateProgress(state)
                UpdateErrorLine(state)
            }
        },
        confirmButton = {
            if (maintaining) {
                TextButton(onClick = { viewModel.dismissNotice() }) {
                    Text(stringResource(R.string.update_button_close))
                }
            } else {
                TextButton(
                    onClick = { viewModel.install() },
                    enabled = !busy && decision.release != null,
                ) {
                    Text(stringResource(R.string.update_button_now))
                }
            }
        },
        dismissButton = {
            if (!required && !maintaining) {
                TextButton(onClick = { viewModel.skip() }) {
                    Text(stringResource(R.string.update_button_later))
                }
            }
        },
    )
}

/**
 * حجب النسخة القديمة أو التعليق الحاجب — الشاشة التي لا مخرج منها إلا التحديث.
 *
 * والحجب **لا يعتمد على لونٍ ولا على نصّ صغير**: لا يوجد زرّ رجوع، و`BackHandler` مُسجَّل
 * لابتلاع الرجوع، والخلفية معتمة بالكامل بلا شفافية يرى من تحتها شيء — فلا تُقرأ الشاشة
 * «تحذيرًا يُتجاهل».
 */
@Composable
private fun UpdateBlockOverlay(state: UpdateManager.State, viewModel: UpdateViewModel) {
    BackHandler(enabled = true) { }
    val decision = state.decision
    val maintaining = decision.verdict == UpdateVerdict.MAINTENANCE
    val release = decision.release
    val busy = state.phase == UpdateManager.Phase.DOWNLOADING || state.phase == UpdateManager.Phase.INSTALLING
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val notes = remember(decision.message, language) { UpdateManifests.localized(decision.message, language) }
    val local = remember(context) { VersionIdentity.readApp(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.lg),
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(MaxSpace.xxl),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    if (maintaining) R.string.update_maintenance_title else R.string.update_block_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            UpdateBodyText(
                when {
                    release != null ->
                        stringResource(R.string.update_block_body, local.display, releaseLabel(release))
                    else -> stringResource(R.string.update_block_body_no_release, local.display)
                }
            )
            notes?.let { UpdateBodyText(it) }
            UpdateProgress(state)
            UpdateErrorLine(state)
            if (!busy) {
                Button(
                    onClick = { viewModel.install() },
                    enabled = release != null,
                    shape = RoundedCornerShape(maxRadius(MaxRadius.control)),
                ) {
                    Text(stringResource(R.string.update_button_now))
                }
            }
            release?.page?.takeIf { it.isNotBlank() }?.let { page ->
                TextButton(onClick = { openInBrowser(context, page) }) {
                    Text(stringResource(R.string.update_button_open_page))
                }
            }
        }
    }
}

@Composable
private fun UpdateBodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UpdateProgress(state: UpdateManager.State) {
    when (state.phase) {
        UpdateManager.Phase.DOWNLOADING -> {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            // ونسبة لا تُعرض إلا إذا كان للمجموع معنى: خادمٌ لا يعلن الطول يعطي 0 ⇒ كلمة
            // «جارٍ التنزيل» بلا نسبة كاذبة.
            UpdateBodyText(
                if (state.progress > 0f) {
                    stringResource(R.string.update_state_downloading, (state.progress * 100f).roundToInt())
                } else {
                    stringResource(R.string.update_state_downloading_unknown)
                }
            )
        }

        UpdateManager.Phase.INSTALLING -> UpdateBodyText(stringResource(R.string.update_state_installing))

        else -> Unit
    }
}

@Composable
private fun UpdateErrorLine(state: UpdateManager.State) {
    val error = state.error ?: return
    if (state.phase != UpdateManager.Phase.IDLE) return
    val message = when (error) {
        UpdateError.NETWORK -> R.string.update_error_network
        UpdateError.HTTP -> R.string.update_error_http
        UpdateError.FORMAT -> R.string.update_error_format
        UpdateError.TOO_LARGE -> R.string.update_error_too_large
        UpdateError.INTEGRITY -> R.string.update_error_integrity
        UpdateError.INSTALL -> R.string.update_error_install
        UpdateError.NOT_CONFIGURED -> R.string.update_error_not_configured
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/**
 * سطر «التحقّق من التحديثات» في الإعدادات — فحصٌ في متناول اليد لا رهن دورة تشغيل،
 * و**قناة اللوحة أولًا** (الإعداد الموقّع الذي يكتبه المالك).
 *
 * قِيس أن الفحص القديم كان يدقّ على رابط مانيفست **404** (المستودع خاصّ، وGitHub لا يخدم
 * `raw` لمجهول) فيقول للمستخدم «تعذّر التحقّق» — والعطب في الرابط لا في الشبكة. والآن:
 *
 *  * إصدار أحدث منشور من اللوحة ⇒ تُفتح **نافذة التحديث** (والطلب اليدوي يمسح التخطّي —
 *    نفس قاعدة `UpdateManager.checkNow`).
 *  * المنشور هو المثبَّت ⇒ «أنت على أحدث إصدار» — ولا «خضراء كاذبة»: كل نتيجة لها كلمتها،
 *    و[UpdateManager.CheckResult.NOT_CONFIGURED] تقولها صراحةً.
 *  * إصدار بلا رابط أو لا إصدار منشور ⇒ عبارة صريحة تقول الحقيقة، **إلا** إذا كان للبناء
 *    مانيفست مضبوط صريحًا (`-Pmaxmanager.updateManifestUrl=…`) فتُجرَّب قناته كعهدها.
 */
@Composable
fun UpdateCheckRow(viewModel: UpdateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val installed = remember(context) { VersionIdentity.readApp(context) }

    fun say(resId: Int) {
        Toast.makeText(context, resources.getString(resId), Toast.LENGTH_SHORT).show()
    }

    ExpressiveListItem(
        onClick = {
            if (checking) return@ExpressiveListItem
            when (ConfigUpdates.decide(snapshot.config.latest, installed.versionName)) {
                ConfigUpdates.Verdict.AVAILABLE -> ConfigUpdates.requestManualCheck()
                ConfigUpdates.Verdict.UP_TO_DATE -> say(R.string.update_check_uptodate)
                ConfigUpdates.Verdict.LINK_MISSING -> say(R.string.update_check_link_missing)
                ConfigUpdates.Verdict.NOT_PUBLISHED -> {
                    if (BuildConfig.UPDATE_MANIFEST_URL.isBlank()) {
                        say(R.string.update_check_none_published)
                    } else {
                        checking = true
                        scope.launch {
                            val result = viewModel.checkNow()
                            checking = false
                            Toast.makeText(context, resources.getString(result.messageRes()), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        },
        headlineContent = { Text(stringResource(R.string.update_check_title)) },
        supportingContent = { Text(stringResource(R.string.update_check_supporting, BuildConfig.VERSION_NAME)) },
        leadingContent = { LeadingIcon(icon = Icons.Rounded.SystemUpdate) },
        trailingContent = {
            if (checking) {
                CircularProgressIndicator(modifier = Modifier.size(MaxSpace.xl), strokeWidth = MaxSpace.hairline)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        },
    )
}

private fun UpdateManager.CheckResult.messageRes(): Int = when (this) {
    UpdateManager.CheckResult.UPDATED -> R.string.update_check_updated
    UpdateManager.CheckResult.UP_TO_DATE -> R.string.update_check_uptodate
    UpdateManager.CheckResult.FAILED -> R.string.update_check_failed
    UpdateManager.CheckResult.NOT_CONFIGURED -> R.string.update_check_not_configured
}

private fun releaseLabel(release: nd.max.core.update.UpdateRelease): String =
    release.name.ifBlank { release.code.toString() }

private fun openInBrowser(context: Context, page: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
