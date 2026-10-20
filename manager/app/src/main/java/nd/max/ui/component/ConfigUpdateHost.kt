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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import nd.max.R
import nd.max.core.access.RemoteAccess
import nd.max.core.update.ConfigUpdateStore
import nd.max.core.update.ConfigUpdates
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.VersionIdentity

/**
 * نافذة «توفّر تحديث» من **قناة اللوحة**: الإصدار ورابط التنزيل كما نشرهما المالك في
 * Max Control، والزرّ يفتح الرابط في المتصفّح — لا تنزيل داخل التطبيق ولا مسار تثبيت.
 *
 * ## لماذا في الجذر
 *
 * الحكم على التطبيق كلّه لا على شاشة (كما في `UpdateHost`) — فمن لا يزور شاشة بعينها يجب أن
 * يرى التحديث عند فتح التطبيق. والاستضافة عبر `LocalRootDialogs` نفسها، فلا تُبنى طبقة ثانية.
 *
 * ## دورة الظهور — ثلاث قواعد لا رابعة
 *
 * ① **تلقائيًّا مرّة**: إعداد منشور أحدث من المثبَّت ولم يُتخطَّ ⇒ تظهر. و«لاحقًا» أو
 *    «حدّث الآن» يسجّلان الإصدار نفسه فلا تعود حتى ينشر المالك إصدارًا أحدث منه.
 * ② **بالطلب**: «الإعدادات ← التحقّق من التحديثات» يمسح التخطّي ويعرض النافذة — من طلب بنفسه
 *    يريد أن يرى ما تخطّاه (نفس قاعدة `UpdateManager.checkNow`).
 * ③ **بالجديد**: إصدار أحدث يُنشر ⇒ يعود الظهور من نفسه ولو كان السابق متخطّى.
 *
 * والنافذة لا تحجب: `CustomContentDialog` يقبل الإغلاق من كل طريق (نقرة خارجية أو رجوع)،
 * فالإلزام يبقى لقناة المانيفست وحدها (`UpdateVerdict.BLOCKED`).
 */
@Composable
fun ConfigUpdateHost() {
    val dialogs = LocalRootDialogs.current
    val key = remember { UUID.randomUUID().toString() }
    DisposableEffect(key) {
        dialogs[key] = @Composable { ConfigUpdateSurface() }
        onDispose { dialogs.remove(key) }
    }
}

@Composable
private fun ConfigUpdateSurface() {
    val context = LocalContext.current
    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val manualRequests by ConfigUpdates.manualRequests.collectAsStateWithLifecycle()
    val store = remember(context) { ConfigUpdateStore(context) }
    val installed = remember(context) { VersionIdentity.readApp(context) }

    val latest = snapshot.config.latest
    val verdict = remember(latest?.version, latest?.page, installed.versionName) {
        ConfigUpdates.decide(latest, installed.versionName)
    }

    var visible by remember { mutableStateOf(false) }
    var dismissedVersion by remember { mutableStateOf(store.dismissedVersion()) }

    // ① الظهور التلقائي — لمرّة لكل إصدار منشور.
    LaunchedEffect(verdict, latest?.version, dismissedVersion) {
        val published = latest?.version?.trim().orEmpty()
        if (verdict == ConfigUpdates.Verdict.AVAILABLE && published != dismissedVersion) {
            visible = true
        }
    }

    // ② الطلب اليدوي من الإعدادات — يمسح التخطّي أولًا فلا يبقى سؤالٌ بلا جواب.
    LaunchedEffect(manualRequests) {
        if (manualRequests > 0L) {
            store.clear()
            dismissedVersion = ""
            if (verdict == ConfigUpdates.Verdict.AVAILABLE) visible = true
        }
    }

    if (!visible) return

    val version = latest?.version?.trim().orEmpty()
    val language = LocalConfiguration.current.locales[0].language
    val notes = remember(snapshot.revision, version, language) {
        snapshot.config.releaseNotes(version, language)
    }
    val page = latest?.page?.trim().orEmpty()

    fun settle() {
        if (version.isNotEmpty()) store.dismiss(version)
        dismissedVersion = version
        visible = false
    }

    CustomContentDialog(
        visible = true,
        title = stringResource(R.string.update_dialog_title),
        onDismiss = { settle() },
        onConfirm = {
            settle()
            openUpdatePage(context, page)
        },
        confirmText = stringResource(R.string.update_button_now),
        dismissText = stringResource(R.string.update_button_later),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            Text(
                text = stringResource(R.string.update_dialog_body, version, installed.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val released = latest?.released?.trim().orEmpty()
            if (released.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.update_released_label, released),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (notes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.update_release_notes_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                notes.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** فتح صفحة التنزيل في المتصفّح — القناة كلها رابطٌ يُفتح، ولا تنزيل داخل التطبيق. */
private fun openUpdatePage(context: Context, page: String) {
    if (page.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
