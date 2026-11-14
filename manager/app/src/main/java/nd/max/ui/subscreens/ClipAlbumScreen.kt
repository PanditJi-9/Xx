/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.ClipAlbumMath
import nd.max.core.gamespace.ClipAlbumState
import nd.max.core.gamespace.ClipEntry
import nd.max.core.gamespace.ClipTrimFailure
import nd.max.core.gamespace.ClipTrimMath
import nd.max.core.gamespace.ClipTrimResult
import nd.max.core.gamespace.ClipTrimmer
import nd.max.ui.component.MaxEmptyState
import nd.max.ui.component.MaxErrorState
import nd.max.ui.component.MaxLoadingState
import nd.max.ui.design.MaxListScreen
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxVisualSpec
import nd.max.ui.design.maxRadius

/**
 * ألبوم المقاطع — مرآة ما سجّله المسجّل، لا مسجّل ثانٍ.
 *
 * ### لماذا شاشة جديدة وفجوة حقيقية
 *
 * `PanelScreenRecorder` يكتب فعلًا `Movies/MaxManager/Max_*.mp4` من لوحة اللعب، ولا شيء في
 * التطبيق يعرضها — فيظنّ المستخدم أن التسجيل لم يحدث. فهذه الشاشة تقرأ المعرّض ولا تنتج شيئًا:
 * لا خدمة، ولا إذنًا جديدًا عدا قراءة الفيديو، ولا مسار حفظ ثانٍ.
 *
 * ### والقاعدة: ثلاث حالات ليست حالة واحدة
 *
 * «لا إذن» و«المعرّض لم يجب» و«لا مقاطع» ثلاثة أخبار مختلفة. لو رُسمت الأولى «لا مقاطع بعد»
 * لظنّ المستخدم أن تسجيله لم يُحفظ وهو محفوظ. ولذلك `ClipAlbumMath.stateOf` يقرّر من ثلاثة
 * أسئلة مقيسة، والشاشة تقول ما يقوله النموذج.
 *
 * ### والحدّ
 *
 * البلايَر والحذف والتشغيل تفتح تطبيقات النظام: ما تفعله تلك التطبيقات لا يُقاس من CI. والمقاطع
 * تُقرأ عبر `MediaStore` لا بمسح مجلّدات، فما لم يُفهرس بعد التسجيل يظهر بعد أن يمسحه المسجّل
 * (`PanelScreenRecorder.stop` يبثّ `MEDIA_SCANNER_SCAN_FILE`).
 */
@Composable
fun ClipAlbumScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permission = remember { clipPermission() }
    // اللقطات تحتاج إذن الصور منفصلًا عن الفيديو: رفضه لا يُسقط الألبوم، بل يُخفي قسمه ويقول سببه.
    val imagePermission = remember { imagePermission() }

    var permitted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var imagePermitted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var deniedOnce by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(ClipAlbumState.Loading) }
    var clips by remember { mutableStateOf(emptyList<ClipEntry>()) }
    var shots by remember { mutableStateOf(emptyList<ClipEntry>()) }
    // الحذف يمرّ بسؤال: التسجيل لا رجعة فيه بعد تأكيد النظام، ومسار ما قبل ٣٠ كان يحذف فورًا.
    var pendingDelete by remember { mutableStateOf<ClipEntry?>(null) }
    // القصّ يفتح حواره أولًا: لا يُكتب ملفّ بمجرّد لمس أيقونة.
    var pendingTrim by remember { mutableStateOf<ClipEntry?>(null) }
    var trimming by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permitted = granted
        if (granted) refresh++ else deniedOnce = true
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        imagePermitted = granted
        if (granted) refresh++
    }

    // العودة تعني أحد أمرين: إذن مُنح من الإعدادات، أو تسجيل جديد تمّ والمسجّل بثّ مسحه. في الحالتين
    // القراءة من جديد أرخص من انتظار خروج ودخول — والألبوم الذي لا يُظهر ما سُجّل للتوّ ألبوم كاذب.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (now != permitted) permitted = now
        val imageNow =
            ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED
        if (imageNow != imagePermitted) imagePermitted = imageNow
        refresh++
    }

    LaunchedEffect(permitted, imagePermitted, refresh) {
        if (!permitted) {
            state = ClipAlbumState.NoPermission
        } else {
            state = ClipAlbumState.Loading
            val loaded = withContext(Dispatchers.IO) {
                runCatching { readClips(context.contentResolver) }
            }
            val found = loaded.getOrNull()
            clips = found?.let(ClipAlbumMath::sortNewestFirst).orEmpty()
            state = ClipAlbumMath.stateOf(permitted = true, read = loaded.isSuccess, count = clips.size)
        }
        // اللقطات تُقرأ بإذنها وحدها: لا تُعلَّق قراءة المقاطع على إذن الصور.
        shots = if (imagePermitted) {
            withContext(Dispatchers.IO) {
                runCatching { readScreenshots(context.contentResolver) }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) refresh++
    }

    fun uriOf(clip: ClipEntry): Uri =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, clip.id)

    fun announce(messageRes: Int) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(messageRes)) }
    }

    fun play(clip: ClipEntry) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriOf(clip), "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            announce(R.string.clip_album_play_failed)
        }
    }

    fun share(clip: ClipEntry) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, uriOf(clip))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.clip_album_share_title))
        )
    }

    fun uriOfShot(shot: ClipEntry): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, shot.id)

    fun openShot(shot: ClipEntry) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriOfShot(shot), "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            announce(R.string.clip_album_shots_open_failed)
        }
    }

    fun shareShot(shot: ClipEntry) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/*")
            .putExtra(Intent.EXTRA_STREAM, uriOfShot(shot))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.clip_album_share_title))
        )
    }

    fun delete(clip: ClipEntry) {
        val uri = uriOf(clip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { announce(R.string.clip_album_delete_failed) }
        } else {
            scope.launch {
                val deleted = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                        .getOrDefault(false)
                }
                if (deleted) refresh++ else announce(R.string.clip_album_delete_failed)
            }
        }
    }

    /**
     * القصّ يكتب **نسخة** لا يمسّ الأصل: الأصل يبقى قابلًا للمقارنة، والحذف قرار المستخدم.
     * والحوار يبقى مفتوحًا عند الفشل كي يرى السبب ويعيد المحاولة بحدود أخرى.
     */
    fun trim(clip: ClipEntry, startMs: Long, endMs: Long) {
        trimming = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ClipTrimmer.trim(context, uriOf(clip), startMs, endMs) }
                    .getOrElse { ClipTrimResult.Failed(ClipTrimFailure.Muxing) }
            }
            trimming = false
            when (result) {
                is ClipTrimResult.Saved -> {
                    pendingTrim = null
                    announce(R.string.clip_album_trim_saved)
                    refresh++
                }
                is ClipTrimResult.Failed -> announce(
                    when (result.reason) {
                        ClipTrimFailure.Unreadable -> R.string.clip_album_trim_fail_read
                        ClipTrimFailure.Unwritable -> R.string.clip_album_trim_fail_write
                        ClipTrimFailure.Muxing -> R.string.clip_album_trim_fail_mux
                    }
                )
            }
        }
    }

    MaxListScreen(
        title = stringResource(R.string.clip_album_title),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.VideoLibrary,
        accent = colors.primary,
        snackbarHostState = snackbarHostState
    ) {
        when (state) {
            ClipAlbumState.Loading -> item {
                MaxLoadingState(
                    title = stringResource(R.string.clip_album_loading_title),
                    message = stringResource(R.string.clip_album_loading_hint)
                )
            }

            ClipAlbumState.NoPermission -> item {
                MaxErrorState(
                    title = stringResource(R.string.clip_album_permission_title),
                    message = stringResource(R.string.clip_album_permission_hint),
                    retryLabel = stringResource(R.string.clip_album_permission_grant),
                    onRetry = {
                        if (deniedOnce) {
                            // رُفض الطلب مرّة: الحوار لا يظهر ثانيةً، فالمسار الصادق هو الإعدادات.
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }
                )
            }

            ClipAlbumState.Unreadable -> item {
                MaxErrorState(
                    title = stringResource(R.string.clip_album_unreadable_title),
                    message = stringResource(R.string.clip_album_unreadable_hint),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { refresh++ }
                )
            }

            ClipAlbumState.Empty -> item {
                MaxEmptyState(
                    title = stringResource(R.string.clip_album_empty_title),
                    message = stringResource(R.string.clip_album_empty_hint)
                )
            }

            ClipAlbumState.Ready -> items(clips, key = { it.id }) { clip ->
                ClipRow(
                    clip = clip,
                    onPlay = { play(clip) },
                    onShare = { share(clip) },
                    onTrim = {
                        if (ClipTrimMath.canTrim(clip.durationMs)) {
                            pendingTrim = clip
                        } else {
                            // المدة المجهولة ليست صفرًا: تُقال كما هي بدل قصٍّ أعمى من الصفر.
                            announce(R.string.clip_album_trim_unavailable)
                        }
                    },
                    onDelete = { pendingDelete = clip }
                )
            }
        }

        // لقطات الشاشة: قسم مستقلّ بإذنه. رفض إذن الصور لا يُسقط المقاطع، بل يعرض بطاقة طلب صغيرة
        // تشرح ما يُفتح به؛ والمقاطع تبقى مقروءة لأن إذنها لا يُلمس.
        item {
            Text(
                text = stringResource(R.string.clip_album_shots_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = MaxSpace.sm)
            )
        }
        if (imagePermitted) {
            if (shots.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.clip_album_shots_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                // مفتاح منفصل عن المقاطع: معرّف الصورة ومعرّف الفيديو جدولان مختلفان وقد يتصادفان.
                items(shots, key = { "shot_${it.id}" }) { shot ->
                    ShotRow(
                        shot = shot,
                        onOpen = { openShot(shot) },
                        onShare = { shareShot(shot) }
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaxSpace.hairline),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.clip_album_shots_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { imageLauncher.launch(imagePermission) }) {
                        Text(stringResource(R.string.clip_album_shots_grant))
                    }
                }
            }
        }
    }

    pendingDelete?.let { clip ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.clip_album_delete_title)) },
            text = { Text(stringResource(R.string.clip_album_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        delete(clip)
                    }
                ) { Text(stringResource(R.string.clip_album_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingTrim?.let { clip ->
        ClipTrimDialog(
            clip = clip,
            busy = trimming,
            onDismiss = { if (!trimming) pendingTrim = null },
            onConfirm = { startMs, endMs -> trim(clip, startMs, endMs) }
        )
    }
}

/**
 * حوار القصّ: مقابض بنصف ثانية وحدود تُصحَّح فورًا عبر [ClipTrimMath]، فلا يمكن اختيار
 * مقطع أقصر من ثانية ولا نهاية قبل البداية. الشاشة وجهة عادية داخل Activity، فـ`AlertDialog`
 * هنا آمن (خلاف لوحة اللعب التي تعيش في نافذة خدمة).
 */
@Composable
private fun ClipTrimDialog(
    clip: ClipEntry,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    val duration = clip.durationMs ?: return
    val colors = MaterialTheme.colorScheme
    var startMs by remember(clip.id) { mutableStateOf(0L) }
    var endMs by remember(clip.id) { mutableStateOf(duration) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.clip_album_trim_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Text(
                    text = clip.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.clip_album_trim_kept,
                        ClipAlbumMath.durationText(ClipTrimMath.keptMs(startMs, endMs))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.clip_album_trim_start, ClipAlbumMath.durationText(startMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Slider(
                    value = ClipTrimMath.fraction(startMs, duration),
                    onValueChange = { fraction ->
                        startMs = ClipTrimMath.clampStart(
                            ClipTrimMath.fromFraction(fraction, duration), duration
                        )
                        // النهاية تتبع البداية إن صار الفارق أقل من ثانية — تُدفع لا تُرفض.
                        if (endMs < startMs + ClipTrimMath.MIN_KEEP_MS) {
                            endMs = ClipTrimMath.clampEnd(startMs + ClipTrimMath.MIN_KEEP_MS, startMs, duration)
                        }
                    },
                    enabled = !busy
                )
                Text(
                    text = stringResource(R.string.clip_album_trim_end, ClipAlbumMath.durationText(endMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Slider(
                    value = ClipTrimMath.fraction(endMs, duration),
                    onValueChange = { fraction ->
                        endMs = ClipTrimMath.clampEnd(
                            ClipTrimMath.fromFraction(fraction, duration), startMs, duration
                        )
                    },
                    enabled = !busy
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startMs, endMs) },
                enabled = !busy
            ) {
                Text(
                    stringResource(
                        if (busy) R.string.clip_album_trim_working else R.string.clip_album_trim_confirm
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/** الإذن المطلوب: الفيديو على ٣٣+، والتخزين على ما قبله (minSdk 29). */
private fun clipPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * قراءة واحدة من `MediaStore` — مجلّد المسجّل وحده، لا كل فيديو الجهاز.
 *
 * والفشل يُرمى إلى المستدعي ليقول «لم يُقرأ» بدل أن يُعيد قائمة فارغة تُقرأ «لا مقاطع».
 */
private fun readClips(resolver: ContentResolver): List<ClipEntry> {
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATE_ADDED
    )
    val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("Movies/MaxManager%")
    val order = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    // معرّض بلا مؤشّر = طلب مرفوض، لا «صفر مقطع»: يُرمى فيقول المستدعي «لم يُقرأ».
    val cursor = resolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        order
    ) ?: error("MediaStore returned no video cursor")
    return cursor.use {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                add(
                    ClipEntry(
                        id = id,
                        name = cursor.getString(nameColumn) ?: "Max_$id.mp4",
                        sizeBytes = cursor.getLong(sizeColumn).takeIf { it > 0L },
                        durationMs = cursor.getLong(durationColumn).takeIf { it > 0L },
                        addedAtMs = cursor.getLong(addedColumn).takeIf { it > 0L }?.times(1_000L)
                    )
                )
            }
        }
    }
}

/** إذن الصور منفصل عن الفيديو: قسم اللقطات يُخفى وحده عند رفضه، ولا يُعلَّق عليه ألبوم المقاطع. */
private fun imagePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * لقطات `Pictures/MaxManager` — المجلّد الذي يكتب فيه مسار اللقطة دون سواه.
 *
 * والفشل هنا لا يُرمى: قسم اللقطات مساعد، فيُعاد فارغًا ويُقال «لا لقطات» بدل إسقاط ألبوم
 * المقاطع بسبب جدول صور لم يقرأ.
 */
private fun readScreenshots(resolver: ContentResolver): List<ClipEntry> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED
    )
    val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("Pictures/MaxManager%")
    val order = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val cursor = resolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        order
    ) ?: return emptyList()
    return cursor.use {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                add(
                    ClipEntry(
                        id = id,
                        name = cursor.getString(nameColumn) ?: "Max_$id.png",
                        sizeBytes = cursor.getLong(sizeColumn).takeIf { it > 0L },
                        durationMs = null,
                        addedAtMs = cursor.getLong(addedColumn).takeIf { it > 0L }?.times(1_000L)
                    )
                )
            }
        }
    }
}

/** صفّ لقطة: أيقونة واسم وحجم، وفعلان فقط (فتح ومشاركة) — لا قصّ ولا حذف لصورة ثابتة. */
@Composable
private fun ShotRow(
    shot: ClipEntry,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(MaxVisualSpec.instrument)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shot.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(MaxSpace.hairline))
            Text(
                text = ClipAlbumMath.sizeText(shot.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.clip_album_shots_share_cd),
                tint = colors.onSurfaceVariant
            )
        }
    }
}

/** صفّ مقطع: صورة مصغّرة، اسم، مدة وحجم، ثم فعلان لا يسرقان النقر من التشغيل. */
@Composable
private fun ClipRow(
    clip: ClipEntry,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onTrim: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
        color = colors.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(MaxSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            ClipThumbnail(clip = clip)
            Column(Modifier.weight(1f)) {
                Text(
                    text = clip.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(MaxSpace.hairline))
                Text(
                    text = stringResource(
                        R.string.clip_album_meta,
                        ClipAlbumMath.durationText(clip.durationMs),
                        ClipAlbumMath.sizeText(clip.sizeBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onTrim) {
                Icon(
                    imageVector = Icons.Rounded.ContentCut,
                    contentDescription = stringResource(R.string.clip_album_trim_cd),
                    tint = colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.clip_album_share_cd),
                    tint = colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.clip_album_delete_cd),
                    tint = colors.error
                )
            }
        }
    }
}

/** الصورة المصغّرة تُفكّ في الخلفية وعلى قياس العرض لا على قياس الملفّ. */
@Composable
private fun ClipThumbnail(clip: ClipEntry) {
    val resolver = LocalContext.current.contentResolver
    val uri = remember(clip.id) {
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, clip.id)
    }
    val frame by produceState<Bitmap?>(initialValue = null, clip.id) {
        value = withContext(Dispatchers.IO) {
            runCatching { resolver.loadThumbnail(uri, Size(ThumbWidth, ThumbHeight), null) }
                .getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .width(MaxVisualSpec.instrument)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = frame
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** قياس فكّ الصورة لا قياس رسمها — بكسل، ويُترك للعرض تصغيره. */
private const val ThumbWidth = 480
private const val ThumbHeight = 270
