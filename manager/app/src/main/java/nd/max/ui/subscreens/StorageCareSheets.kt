/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * العناية بالتخزين — **أوراق الشاشة** (`STORAGE-CARE-02` — إعادة بناء أقسام `STORAGE-CARE-01`).
 *
 * فُصلت عن `StorageCareScreen.kt` بعد أن بلغ ١٣٤٩ سطرًا وتجاوز سقف `code_health` (١٠٠٠):
 * والورقتان كتلة قائمة بذاتها — سردُ «ما سيُحذف بالضبط» في الفئة، وتفصيلُ أجزاء التطبيق
 * وفعلاها منفصلين — لا فروعَ شاشةٍ ثانية. والعقد بين الملفّين واحد: الشاشة تنادي، والورقة
 * تعرض وتُرجع الفعل إلى الـViewModel.
 *
 * **والدوال هنا `internal` لا `private`:** نفس القاعدة التي اختارتها شاشة تفصيل التخزين
 * لـ`bucketLabel` — الوحدة الظاهرة للوحدة نفسها (`nd.max.ui.subscreens`)، فالشاشة تناديها
 * والغريب لا يراها.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.ExpressiveListItem
import nd.max.ui.component.NeuralCaption
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.neuralPalette
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.CareTarget
import nd.max.ui.util.StorageCareEngine
import nd.max.ui.util.StorageCareModel
import nd.max.ui.util.StorageScanModel
import nd.max.ui.viewmodel.AppSheetState
import nd.max.ui.viewmodel.CategorySheetState

/**
 * ورقة الفئة: **السرد قبل الفعل**.
 *
 * بأمر المالك: «الضغط على كاش التخزين المشترك لا يظهر لي بالضبط ما هي الذي سيُمسح منها أي
 * الملفات … ولا يوجد زرّ حقيقي للتنظيف في كل خيار». فالورقة تجيب السؤالين معًا: صفوفٌ بأسماء
 * التطبيقات ومساراتها ومقاساتها (تُنسخ بلمسة)، وزرّ **«نظّف الآن»** ينفّذ هذه الفئة **وحدها**
 * لا الدفعة. والقالب الحقيقي (`/data/data/<pkg>/cache`) يُعرض بنصّه لأنه هو نفسه الذي يُنفَّذ —
 * فما يُعرض هنا وما يُحذف هناك من مصدر واحد.
 */
@Composable
internal fun CategorySheetContent(
    sheet: CategorySheetState,
    checked: Boolean,
    onToggle: () -> Unit,
    onClean: () -> Unit,
    onCopyPath: (String) -> Unit,
    onClose: () -> Unit,
) {
    val p = neuralPalette()
    val accent = MaterialTheme.colorScheme.primary
    val targets = sheet.targets
    val cleanable = !sheet.cleaning && targets != null && !targets.failed && !targets.isEmpty
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(categoryTitle(sheet.category)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(categoryDescription(sheet.category)),
            color = p.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(MaxSpace.sm))
        Text(
            stringResource(R.string.storage_care_sheet_pattern, StorageCareEngine.targetPattern(sheet.category)),
            color = p.muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(MaxSpace.md))

        when {
            sheet.loading ->
                NeuralCaption(stringResource(R.string.storage_care_sheet_reading), color = p.muted)

            targets == null || targets.failed ->
                NeuralCaption(stringResource(R.string.storage_care_sheet_failed), color = p.warn)

            targets.isEmpty ->
                NeuralCaption(stringResource(R.string.storage_care_sheet_empty), color = p.muted)

            else -> {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = MaxSize.dialogListMax)) {
                    items(targets.rows, key = { it.path ?: it.label }) { target ->
                        TargetRow(target = target, onCopyPath = onCopyPath)
                    }
                    if (targets.restCount > 0) {
                        item {
                            NeuralCaption(
                                stringResource(
                                    R.string.storage_care_sheet_more,
                                    targets.restCount,
                                    StorageScanModel.formatBytes(targets.restBytes),
                                ),
                                color = p.muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(MaxSpace.sm))
                NeuralCaption(
                    stringResource(
                        R.string.storage_care_sheet_total,
                        if (StorageCareModel.showsCount(sheet.category) && targets.measuredBytes == 0L) {
                            stringResource(R.string.storage_care_count_folders, targets.totalItems)
                        } else {
                            StorageScanModel.formatBytes(targets.measuredBytes)
                        },
                    ),
                    color = p.text,
                )
                if (targets.anyUnmeasured) {
                    NeuralCaption(stringResource(R.string.storage_care_partial), color = p.warn)
                }
            }
        }

        Spacer(Modifier.height(MaxSpace.md))
        val amount = targets?.takeIf { !it.failed }?.measuredBytes ?: 0L
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralPill(
                text = if (amount > 0L) {
                    stringResource(
                        R.string.storage_care_sheet_clean_amount,
                        StorageScanModel.formatBytes(amount),
                    )
                } else {
                    stringResource(R.string.storage_care_sheet_clean)
                },
                accent = if (cleanable) accent else p.muted,
                filled = cleanable,
                onClick = if (cleanable) onClean else null,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.storage_care_sheet_close),
                accent = p.muted,
                compact = true,
                onClick = onClose,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Text(
                stringResource(R.string.storage_care_sheet_include),
                color = p.text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        if (sheet.cleaning) {
            NeuralCaption(stringResource(R.string.storage_care_sheet_cleaning), color = p.muted)
        }
        sheet.outcome?.let { outcome ->
            NeuralCaption(
                text = when {
                    !outcome.executed -> stringResource(R.string.storage_care_sheet_clean_failed)
                    outcome.freedBytes > 0L -> stringResource(
                        R.string.storage_care_freed,
                        StorageScanModel.formatBytes(outcome.freedBytes),
                    )

                    else -> stringResource(R.string.storage_care_freed_none)
                },
                color = when {
                    !outcome.executed -> p.warn
                    outcome.freedBytes > 0L -> p.ok
                    else -> p.muted
                },
            )
        }
    }
}

/**
 * صفّ هدف واحد: أيقونة التطبيق (أو رمز مجلد)، الاسم، المسار، والمقاس — والنقر ينسخ المسار.
 *
 * والأيقونة باسم الحزمة وحدها: الهدف الذي لا حزمة له (مجلد، ملفّ APK) يأخذ رمز نوعه ولا
 * يُنسب إلى تطبيق لم يُسمَّ.
 */
@Composable
internal fun TargetRow(target: CareTarget, onCopyPath: (String) -> Unit) {
    val p = neuralPalette()
    ExpressiveListItem(
        onClick = target.path?.let { path -> { onCopyPath(path) } },
        headlineContent = {
            Text(
                target.label,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            target.path?.let { path ->
                Text(
                    path,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            val packageName = target.packageName
            if (packageName != null) {
                AppIconImage(
                    packageName = packageName,
                    size = MaxSize.rowIconContainer,
                    contentDescription = null,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = p.muted,
                    modifier = Modifier.size(MaxSize.iconGlyph),
                )
            }
        },
        trailingContent = {
            Text(
                target.bytes?.let(StorageScanModel::formatBytes)
                    ?: stringResource(R.string.storage_care_unmeasured),
                color = if (target.bytes != null) p.text else p.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

/**
 * ورقة التطبيق: **أين ذهبت مساحة هذا التطبيق** — وأفعال أجزائه منفصلة.
 *
 * الجواب على «لماذا هذا التطبيق كبير؟» ثلاث كتل لا رقم: الحزمة، والبيانات، والكاش. ولذلك
 * «مسح الكاش» و«مسح البيانات» فعلان مختلفان: الأول لا يمسّ بيانات المستخدم، والثاني لا
 * رجعة فيه ويقف خلف تأكيد. وبلا جذر لا يُدّعى فعل لا يقع: زرّ الكاش يتعطّل، ويُفتح للنظام
 * في إعدادات التطبيق حيث يمسحه المستخدم بنفسه — والباب باقٍ.
 */
@Composable
internal fun AppSheetContent(
    sheet: AppSheetState,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val p = neuralPalette()
    val accent = MaterialTheme.colorScheme.primary
    val app = sheet.app
    val idle = !sheet.busy
    val canClearCache = sheet.rootAvailable && idle && (app.cacheBytes ?: 0L) > 0L
    val canClearData = sheet.rootAvailable && idle
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                packageName = app.packageName,
                size = MaxSize.iconContainer,
                contentDescription = null,
            )
            Spacer(Modifier.width(MaxSpace.md))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(MaxSpace.md))
        PartRow(label = stringResource(R.string.storage_care_app_part_pkg), bytes = app.packageBytes)
        PartRow(label = stringResource(R.string.storage_care_app_part_data), bytes = app.dataBytes)
        PartRow(label = stringResource(R.string.storage_care_app_part_cache), bytes = app.cacheBytes)
        if (!app.precise) {
            NeuralCaption(stringResource(R.string.storage_care_app_part_apk_only), color = p.muted)
        }
        Spacer(Modifier.height(MaxSpace.md))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralPill(
                text = stringResource(R.string.storage_care_app_clear_cache),
                accent = if (canClearCache) accent else p.muted,
                filled = canClearCache,
                onClick = if (canClearCache) onClearCache else null,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.storage_care_app_clear_data),
                accent = if (canClearData) p.danger else p.muted,
                compact = true,
                onClick = if (canClearData) onClearData else null,
            )
        }
        Spacer(Modifier.height(MaxSpace.sm))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralPill(
                text = stringResource(R.string.storage_care_app_settings),
                accent = p.muted,
                compact = true,
                onClick = onOpenSettings,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.storage_care_sheet_close),
                accent = p.muted,
                compact = true,
                onClick = onClose,
            )
        }
        if (!sheet.rootAvailable) {
            NeuralCaption(stringResource(R.string.storage_care_app_need_root), color = p.warn)
        }
        if (sheet.busy) {
            NeuralCaption(
                stringResource(
                    if (sheet.clearingData) {
                        R.string.storage_care_app_clearing_data
                    } else {
                        R.string.storage_care_app_clearing_cache
                    }
                ),
                color = p.muted,
            )
        }
        sheet.outcome?.let { outcome ->
            NeuralCaption(
                text = when {
                    !outcome.executed -> stringResource(R.string.storage_care_sheet_clean_failed)
                    outcome.freedBytes > 0L -> stringResource(
                        R.string.storage_care_freed,
                        StorageScanModel.formatBytes(outcome.freedBytes),
                    )

                    else -> stringResource(R.string.storage_care_freed_none)
                },
                color = when {
                    !outcome.executed -> p.warn
                    outcome.freedBytes > 0L -> p.ok
                    else -> p.muted
                },
            )
        }
    }
}

/** سطر جزء واحد من مساحة التطبيق: اسم ورقم — ثلاثة أسطر تجيب «لماذا هو كبير؟». */
@Composable
internal fun PartRow(label: String, bytes: Long?) {
    val p = neuralPalette()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = p.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            bytes?.let(StorageScanModel::formatBytes) ?: stringResource(R.string.storage_care_unmeasured),
            color = if (bytes != null) p.text else p.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
