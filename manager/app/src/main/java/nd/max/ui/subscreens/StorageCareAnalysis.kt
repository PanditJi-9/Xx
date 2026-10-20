/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * العناية بالتخزين — **بطاقات التحليل** (`STORAGE-CARE-02` — إعادة بناء أقسام `STORAGE-CARE-01`).
 *
 * فُصلت مع أختها (`StorageCareSheets.kt`) لتُبقى `StorageCareScreen.kt` تحت سقف
 * `code_health`، وهي كتلة واحدة مترابطة: مسحٌ واحد، وشريط تركيب، وأكبر المجلدات، وأكبر
 * الملفات — كلها تقرأ من `StorageScanResult` و`FolderScan` ولا تملك مسارًا ولا رقمًا
 * خاصًّا بها. والنصوص من `values/`+`values-ar/` وحدهما، والقالب المعروض فيها (`bucketLabel`)
 * من السجلّ المشترك مع شاشة التفاصيل — فلا اسمان لنوعٍ واحد.
 *
 * وحدّها معلن: «لم تُقرأ» و«فشل القياس» لهما سطران مختلفان عن «لا شيء» — ADR-07.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.ui.component.NeuralCaption
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.NeuralSectionHeader
import nd.max.ui.component.NeuralTrack
import nd.max.ui.component.neuralPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.FolderScan
import nd.max.ui.util.StorageBucket
import nd.max.ui.util.StorageLargestItem
import nd.max.ui.util.StorageScanModel
import nd.max.ui.viewmodel.StorageCareUiState
import nd.max.ui.design.maxRadius

/** كم مجلدًا معروضًا في «أكبر المجلدات» قبل طيّ الباقي — ستة تكفي شاشةً. */
private const val FOLDER_ROWS = 6

/** كم ملفًّا معروضًا في «أكبر الملفات» قبل طيّ الباقي — ستة، والبقية في شاشة التفاصيل. */
private const val LARGEST_ROWS = 6

/**
 * قسم التحليل: مسح واحد، وتركيب مشهود، وأكبر المجلدات والملفات — وباب إلى التفاصيل الكاملة.
 *
 * أُعيد بناؤه بأمر المالك («Space analysis سيئ»): كان خمسة صفوف أنساب وحسب. والآن يجيب
 * الأسئلة الثلاثة بالترتيب الذي تُسأل به: **كم؟** (شريط التركيب ونِسَب الأنواع) — **أين؟**
 * (أكبر المجلدات: `Android/data` و`DCIM` و`Download`…) — **ما هو؟** (أكبر الملفات بأسمائها
 * ومساراتها، والنقر ينسخ المسار). ولا رقم يُقدَّر: كل الأرقام من مسح واحد، وحدوده معلنة تحته.
 */
@Composable
internal fun AnalysisSection(
    state: StorageCareUiState,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
    onDetails: () -> Unit,
    onCopyPath: (String) -> Unit,
    accent: Color,
) {
    val p = neuralPalette()
    val result = state.scanResult
    MaxSection(
        title = stringResource(R.string.storage_care_analysis_title),
        description = stringResource(R.string.storage_care_analysis_desc),
    ) {
        NeuralPanel(accent = if (state.scanning) p.muted else accent) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                NeuralPill(
                    text = when {
                        state.scanning -> stringResource(
                            R.string.storage_care_analysis_scanning,
                            state.scanProgress,
                        )

                        result == null -> stringResource(R.string.storage_care_analysis_scan)
                        else -> stringResource(R.string.storage_care_analysis_rescan)
                    },
                    accent = if (state.scanning) p.muted else accent,
                    filled = !state.scanning,
                    onClick = if (state.canScan) onScan else null,
                )
                if (state.scanning) {
                    // الإيقاف متاح ما دام المسح جاريًا — ونتيجة نصفية لا تُعرض أبدًا.
                    Spacer(Modifier.width(MaxSpace.sm))
                    NeuralPill(
                        text = stringResource(R.string.storage_care_analysis_cancel),
                        accent = p.warn,
                        compact = true,
                        onClick = onCancelScan,
                    )
                }
                if (result != null) {
                    Spacer(Modifier.width(MaxSpace.sm))
                    NeuralPill(
                        text = stringResource(R.string.storage_care_analysis_details),
                        accent = p.muted,
                        compact = true,
                        onClick = onDetails,
                    )
                }
            }
            when {
                result == null && !state.scanning ->
                    NeuralCaption(stringResource(R.string.storage_care_analysis_empty), color = p.muted)

                result != null -> {
                    NeuralCaption(
                        stringResource(
                            R.string.storage_care_analysis_summary,
                            StorageScanModel.formatBytes(result.measuredBytes),
                            result.scannedEntries,
                        ),
                        color = p.text,
                    )
                    val ranked = StorageScanModel.rank(result.buckets)
                    if (ranked.isNotEmpty()) {
                        CompositionBar(buckets = ranked, total = result.measuredBytes, accent = accent)
                    }
                    for (bucket in ranked) {
                        BucketRow(bucket = bucket, total = result.measuredBytes, accent = accent)
                    }
                    FoldersCard(
                        scan = state.folderScan,
                        failed = state.foldersFailed,
                        accent = accent,
                        onCopyPath = onCopyPath,
                    )
                    LargestCard(
                        items = result.largest,
                        accent = accent,
                        onCopyPath = onCopyPath,
                    )
                    if (result.skippedDirectories > 0) {
                        NeuralCaption(
                            stringResource(R.string.storage_care_analysis_skipped, result.skippedDirectories),
                            color = p.warn,
                        )
                    }
                    if (result.truncated) {
                        NeuralCaption(stringResource(R.string.storage_care_analysis_truncated), color = p.warn)
                    }
                }
            }
        }
    }
}

/**
 * شريط التركيب: شريحة لكل نوع بنسبته — نظرة واحدة تقول «ما الذي يملأ القرص».
 *
 * واللون واحد بتدرّج ألفا لا ألوان دلالية: `danger` الأحمر على نوع «فيديو» كان سيوهم بخطأ،
 * والألوان هنا **ترتيبٌ لا حكم** — والصفوف تحته تحمل الاسم والرقم، فالشريط تأطير لا مصدر وحيد.
 */
@Composable
internal fun CompositionBar(buckets: List<StorageBucket>, total: Long, accent: Color) {
    if (total <= 0L || buckets.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaxSpace.sm)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.control))),
    ) {
        buckets.forEachIndexed { index, bucket ->
            val fraction = bucket.bytes.toFloat() / total.toFloat()
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .height(MaxSpace.sm)
                        .background(accent.copy(alpha = (0.95f - index * 0.11f).coerceAtLeast(0.3f))),
                )
            }
        }
    }
}

/** صفّ نوع واحد: اسمه، حجمه، شريطه، وعدد ملفاته — العدد جزء من الخبر لا حشو. */
@Composable
internal fun BucketRow(bucket: StorageBucket, total: Long, accent: Color) {
    val p = neuralPalette()
    val percent = StorageScanModel.percentOf(bucket.bytes, total)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                bucketLabel(bucket.kind),
                color = p.text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                StorageScanModel.formatBytes(bucket.bytes),
                color = p.text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NeuralTrack(percent / 100f, accent)
        NeuralCaption(
            stringResource(R.string.detail_storage_bucket_note, bucket.files, percent),
            color = p.muted,
        )
    }
}

/**
 * أكبر المجلدات — **الجواب على «أين ذهبت المساحة»** بأسماء يعرفها المستخدم لا بأنواع ملفات.
 *
 * وثلاث حالات معلنة تعادلها الصدق: لم يُقس بعد (`null` قبل المسح) فلا سطر، وفشل القياس في
 * سطر تحذير، ومجلدات لم تُقرأ فالمجموع حدّ أدنى — ADR-07: لا ناتج ناقص يُعرض كأنه كامل.
 */
@Composable
internal fun FoldersCard(
    scan: FolderScan?,
    failed: Boolean,
    accent: Color,
    onCopyPath: (String) -> Unit,
) {
    val p = neuralPalette()
    when {
        failed -> NeuralCaption(stringResource(R.string.storage_care_folders_failed), color = p.warn)

        scan == null -> Unit

        scan.slices.isEmpty() ->
            NeuralCaption(stringResource(R.string.storage_care_folders_empty), color = p.muted)

        else -> {
            NeuralSectionHeader(
                title = stringResource(R.string.storage_care_folders_title),
                caption = stringResource(R.string.storage_care_folders_caption),
                accent = accent,
            )
            val fold = StorageScanModel.foldLargest(scan.slices, FOLDER_ROWS)
            val base = fold.totalBytes
            for (item in fold.shown) {
                RankRow(item = item, total = base, accent = accent, onCopyPath = onCopyPath)
            }
            if (fold.restCount > 0) {
                NeuralCaption(
                    stringResource(
                        R.string.storage_care_folders_more,
                        fold.restCount,
                        StorageScanModel.formatBytes(fold.restBytes),
                    ),
                    color = p.muted,
                )
            }
            if (scan.unreadable > 0) {
                NeuralCaption(
                    stringResource(R.string.storage_care_folders_unreadable, scan.unreadable),
                    color = p.warn,
                )
            }
        }
    }
}

/** أكبر الملفات المفردة — الاسم والمسار والمقاس، والنقر ينسخ المسار. */
@Composable
internal fun LargestCard(
    items: List<StorageLargestItem>,
    accent: Color,
    onCopyPath: (String) -> Unit,
) {
    val p = neuralPalette()
    if (items.isEmpty()) return
    NeuralSectionHeader(
        title = stringResource(R.string.storage_care_largest_title),
        caption = stringResource(R.string.storage_care_largest_caption),
        accent = accent,
    )
    val fold = StorageScanModel.foldLargest(items, LARGEST_ROWS)
    val base = fold.totalBytes
    for (item in fold.shown) {
        RankRow(item = item, total = base, accent = accent, onCopyPath = onCopyPath)
    }
    if (fold.restCount > 0) {
        NeuralCaption(
            stringResource(
                R.string.storage_care_largest_more,
                fold.restCount,
                StorageScanModel.formatBytes(fold.restBytes),
            ),
            color = p.muted,
        )
    }
}

/** صفّ عنصر كبير (مجلد أو ملفّ): الاسم، الأب، شريط نسبته، والمقاس — والنقر ينسخ المسار. */
@Composable
internal fun RankRow(
    item: StorageLargestItem,
    total: Long,
    accent: Color,
    onCopyPath: (String) -> Unit,
) {
    val p = neuralPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopyPath(item.path) },
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Text(
            item.name,
            color = p.text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                parentOf(item.path),
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                StorageScanModel.formatBytes(item.bytes),
                color = p.text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NeuralTrack(StorageScanModel.percentOf(item.bytes, total) / 100f, accent)
    }
}

/** أب المسار للعرض — `/` حين لا أب، فلا فراغ أثناء الثقة (سابقة تفصيل التخزين). */
internal fun parentOf(path: String): String =
    path.substringBeforeLast('/', "").ifBlank { "/" }
