/*
 * Copyright (C) 2026-2027 Rapli
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

package nd.max.ui.subscreens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.ui.design.MaxSize
import java.io.File
import java.io.FileOutputStream

/**
 * ## غلافٌ لكل لعبة — الفنّ الذي لا نملكه
 *
 * الشاشة كانت تُبنى من **مربّع واحد**: أيقونة اللعبة المثبَّتة. وكل واجهات الألعاب الاحترافية تقف على
 * فنٍّ (key art) يمتدّ تحت النصّ ويلوّن المشهد، ولذلك كانت شاشتنا تُقرأ «مرسومة» لا «مُصيَّرة» مهما
 * بلغ عدد طبقات الضوء: الطبقات تُجمّل فراغًا، ولا تصنع فنًّا.
 *
 * فالمصدر الجديد — المُعتمَد من المالك («ج) الاثنان: أ الآن، وب لاحقًا») — هو **صورة يختارها المستخدم**
 * لكل لعبة من معرضه. تُحفظ داخل مجلد التطبيق (لا صلاحية ولا مكتبة ولا ترخيص)، وتُقرأ بلا تعديل:
 *  - اللوح: تُقصّ مربّعةً (ContentScale.Crop) فتصير مادّة اللوح.
 *  - الخلفية: تُقصّ بعرض الشاشة بشفافية منخفضة فتصير المشهد كلّه.
 *
 * ## لماذا ملفّ لا `SharedPreferences`
 * وجودُ الملفّ هو الحالة. مفتاحٌ في التفضيلات يمكن أن يشير إلى ملفّ محذوف (سابقة اللافتة في
 * `BannerImageUtil`)، فيعود المشهد فارغًا بلا سبب ظاهر. هنا: لا ملفّ ⇒ لا غلاف، ويُعاد بناء المشهد.
 *
 * ## ولماذا `inSampleSize` مرّتين
 * صورة المعرض قد تكون ٤٠٠٠px. نقرأ الأبعاد أولًا (`inJustDecodeBounds`) لنحسب أكبر تصغير بلا فصل،
 * ثم نفكّ مرّة واحدة على المقاس. ولا تُحفظ الصورة الأصليّة: نكتب JPEG عند `CockpitCoverMaxPx`،
 * فالمجلّد يبقى صغيرًا والقراءة سريعة على المعالج.
 */
internal object GameCoverStore {

    /**
     * نسخة كل غلاف. `mutableStateMapOf` هنا ليست ترفًا: بعد الاختيار يجب أن يُعاد فكّ الصورة
     * ويُعاد رسم الشاشة، ولا مرجع آخر يجمع الحالتين (المخزن على القرص والشاشة).
     */
    val versions = mutableStateMapOf<String, Int>()

    fun bump(packageName: String) {
        versions[packageName] = (versions[packageName] ?: 0) + 1
    }
}

/** أقصى بُعد يُحفظ به الغلاف — يغطّي ٨٥٠×٣٩٢dp بكثافة ٣ (٢٥٥٠px) بلا وزن. */
internal const val CockpitCoverMaxPx = 1440

/** أقصى بُعد يُفكّ للعرض: اللوح ٣٠٠dp والخلفيّة تشترك في نفس الصورة، فلا داعي لأكثر. */
internal const val CockpitCoverDecodePx = 1024

private const val CoverDir = "covers"
private const val CoverQuality = 88

/** اسم ملفّ آمن: اسم الحزمة يحوي نقاطًا و`_`، ولا نسمح بمسارات خارجة عن مجلّدنا. */
private fun coverKey(packageName: String): String =
    packageName.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

internal fun coverFile(context: Context, packageName: String): File =
    File(File(context.filesDir, CoverDir), coverKey(packageName) + ".jpg")

internal fun hasCover(context: Context, packageName: String?): Boolean =
    packageName != null && coverFile(context, packageName).isFile

/**
 * يحفظ صورة المعرض غلافًا للعبة. يفكّ على القرص (IO) — لا يُنادى من الخيط الرئيسيّ.
 * يعيد `true` فقط إذا كُتب ملفّ غير فارغ؛ وما دون ذلك لا يُعلَن نجاح.
 */
internal fun setGameCover(context: Context, packageName: String, uri: Uri): Boolean = runCatching {
    val dir = File(context.filesDir, CoverDir).apply { mkdirs() }
    val target = File(dir, coverKey(packageName) + ".jpg")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

    var sample = 1
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    while (longest / (sample * 2) >= CockpitCoverMaxPx) sample *= 2

    val scaled = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching false

    var wrote = false
    FileOutputStream(target).use { out ->
        wrote = scaled.compress(Bitmap.CompressFormat.JPEG, CoverQuality, out)
    }
    scaled.recycle()
    if (wrote) GameCoverStore.bump(packageName)
    wrote
}.getOrDefault(false)

internal fun clearGameCover(context: Context, packageName: String) {
    if (coverFile(context, packageName).delete()) GameCoverStore.bump(packageName)
}

private fun decodeCover(file: File, maxPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    while (longest / (sample * 2) >= maxPx) sample *= 2
    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
}.getOrNull()

/**
 * غلاف اللعبة المختارة، أو `null` إن لم تختر لها غلافًا بعد. يُقرأ مرّة عند تغيّر اللعبة أو النسخة،
 * على `Dispatchers.IO` — لا فكّ صور على الخيط الرئيسيّ.
 */
@Composable
internal fun rememberGameCover(
    packageName: String?,
    maxPx: Int = CockpitCoverDecodePx,
): ImageBitmap? {
    val context = LocalContext.current
    val version = if (packageName == null) 0 else GameCoverStore.versions[packageName] ?: 0
    return produceState<ImageBitmap?>(null, packageName, version) {
        value = if (packageName == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                coverFile(context, packageName).takeIf { it.isFile }?.let { decodeCover(it, maxPx) }
            }
        }
    }.value
}

// ── زرّ الغلاف ────────────────────────────────────────────────────────────────
// المقاسات في ثوابت لا في جسم الـcomposable: البوابة `design_tokens.py` تقيس الحرفيّات، ولأن
// العنصر واحد فله اسم واحد. ولمسة ٤٨dp على الأقلّ كما تشترط بقية الشاشة.
private val CoverTouchBox = MaxSize.minTouchTarget
private val CoverVisual = 34.dp
private val CoverGlyph = 18.dp

/**
 * زرّ واحد بمعنيين: إن لم يكن للعبة غلاف فهو «تعيين غطاء» يفتح معرض الصور، وإن كان لها غلاف
 * فهو «إزالة الغطاء». معنى واحد لكل حالة، بلا قائمة ولا حوار.
 */
@Composable
internal fun GameCoverButton(
    packageName: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasCover = hasCover(context, packageName) && packageName != null

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val pkg = packageName
        if (uri != null && pkg != null) {
            scope.launch(Dispatchers.IO) { setGameCover(context, pkg, uri) }
        }
    }

    val label = if (hasCover) {
        stringResource(R.string.gs_cover_clear)
    } else {
        stringResource(R.string.gs_cover_set)
    }

    Box(
        modifier = modifier
            .size(CoverTouchBox)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button) {
                val pkg = packageName
                when {
                    pkg == null -> Unit
                    hasCover -> scope.launch(Dispatchers.IO) { clearGameCover(context, pkg) }
                    else -> picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CoverVisual)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hasCover) Icons.Rounded.Close else Icons.Rounded.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(CoverGlyph),
            )
        }
    }
}
