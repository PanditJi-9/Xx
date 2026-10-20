/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * العناية بالتخزين — الشاشة الشاملة (`STORAGE-CARE-01`).
 *
 * حلّت محلّ «التنظيف الفائق»، والسؤال الذي تجيبه أوسع: **ما الذي يأخذ مساحتي، وما الذي
 * أستطيع تحريره الآن، وكيف أدير ما بقي؟** ولذلك ستة أقسام بهذا الترتيب:
 *
 * 1. **السعة:** كم ممتلئ القرص — من نفس مصدر شاشة التخزين (`StorageUtil.readMounts`) لا من
 *    حساب جديد: رقمان لسعة واحدة يفترقان.
 * 2. **النطاق:** عند أي طبقة امتياز أعمل، وما الذي يوسّعه الجذر بالاسم — وهذا السطر هو الفرق
 *    بين «التطبيق لا ينظّف» و«هذا كل ما يُنظَّف في وضعك» (§10.3).
 * 3. **التنظيف الذكي:** صفّ لكل فئة **مرئية** عند الطبقة الحالية، بمقاس مقيس أو «لم يُقس».
 *    والمربّع يؤشّر للدفعة، **ونقر الصفّ يفتح سرد «ما سيُحذف بالضبط»** بزرّه الذي ينفّذ الفئة
 *    وحدها (العرض في `StorageCareSheets`) — والسرد من نفس قالب الحذف، والقرارات كلها في
 *    `StorageCareModel` والقوالب في `StorageCareEngine`: الشاشة لا تملك قرارًا ولا مسارًا.
 * 4. **تحليل المساحة:** مسح واحد يجيب «كم؟ أين؟ ما هو؟» — شريط تركيب، وكل الأنواع بعدد
 *    ملفاتها، وأكبر المجلدات، وأكبر الملفات (والنقر ينسخ المسار) — وبابُ `StorageDetail`
 *    للشجرة الكاملة. العرض في `StorageCareAnalysis`، ولا رقم ثانٍ يُحسب هنا.
 * 5. **أكبر التطبيقات:** الأيقونة + تفصيل الأجزاء (حزمة · بيانات · كاش) — بصلاحية الاستخدام
 *    أرقام كاملة، وبدونها حجم الحزمة وحده، والدرجة معلنة في سطر القسم. والنقر يفتح ورقة
 *    التطبيق: مسح الكاش وحده · مسح البيانات خلف تأكيد · إعدادات النظام.
 * 6. **أدوات:** أبواب `FileManager` و`Dex2oat` و`DebloatFreeze` — شاشة تجمع ما تفرّق.
 *
 * **وما لا تفعله الشاشة:** لا تكتب مسارًا ولا تمرّر مسارًا إلى المحرّك (القوالب كلها في
 * `StorageCareEngine`)، ولا تعرض فئة الجذر بلا جذر **لتُظهر زرًّا يعمل نصف عمل** (تُخفى
 * الأربع، وسطر النطاق يقول ما ينقص بالاسم)، ولا تعرض «حُرِّر» قبل أن يُقاس الفرق فعلًا، ولا
 * تقديرًا مكان فشل قياس.
 */
package nd.max.ui.subscreens

import android.content.ClipData
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.privilege.PrivilegeManager
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.ConfirmDialogHost
import nd.max.ui.component.CustomBottomSheet
import nd.max.ui.component.ExpressiveListItem
import nd.max.ui.component.NeuralCaption
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.NeuralSectionHeader
import nd.max.ui.component.NeuralTrack
import nd.max.ui.component.neuralPalette
import nd.max.ui.component.rememberConfirmDialog
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxListScreen
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.navigation.MaxNavActions
import nd.max.ui.navigation.maxDestinationRole
import nd.max.ui.navigation.openAppSettings
import nd.max.ui.util.AppStorageEntry
import nd.max.ui.util.AppStorageReader
import nd.max.ui.util.CleanMeasurement
import nd.max.ui.util.StorageCareCategory
import nd.max.ui.util.StorageCareModel
import nd.max.ui.util.StorageScanModel
import nd.max.ui.util.MountInfo
import nd.max.ui.viewmodel.StorageCareUiState
import nd.max.ui.viewmodel.StorageCareViewModel

@Composable
fun StorageCareScreen(
    navController: NavHostController,
    viewModel: StorageCareViewModel = hiltViewModel(),
) {
    val navActions = MaxNavActions(navController)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // الجذر يُقرأ هنا لكل تركيبة: من رجع من شاشة الصلاحيات يجب أن يرى الفئات الأربع فورًا.
    val root = PrivilegeManager.cachedRootGranted()

    // قياس واحد عند أول ظهور؛ وما بعده بتأشير المستخدم أو بعد التنظيف.
    // والجولة تعرف الذاكرة الصالحة نفسها: الفتح الثاني لا يعيد مسحًا بلا سبب.
    LaunchedEffect(Unit) { viewModel.measureAll() }

    // عند كل عودة: الصلاحية تُفحص من جديد — من منحها من الإعدادات يرى الأرقام الدقيقة فورًا.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshApps()
        onPauseOrDispose { }
    }

    // «مسح البيانات» يقف خلف تأكيد صريح: فعلٌ لا رجعة فيه، وبابه لا يُفتح بلمسة عابرة.
    val dataDialog = rememberConfirmDialog(
        onConfirm = { viewModel.clearAppDataNow() },
        onDismiss = {},
    )
    val dataConfirmTitle = stringResource(R.string.storage_care_app_data_confirm_title)
    val dataConfirmBody = stringResource(R.string.storage_care_app_data_confirm_body)
    val dataConfirmOk = stringResource(R.string.storage_care_app_data_confirm_ok)
    val dataConfirmCancel = stringResource(R.string.storage_care_app_data_confirm_cancel)

    // نسخ المسار: الحافظة قد ترفض، فلا يُقال «نُسخ» بلا قياس (نفس عقد شاشة تفصيل التخزين).
    val copyPath: (String) -> Unit = { path ->
        scope.launch {
            val copied = runCatching {
                clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            resources.getString(R.string.max_files_detail_copy_label),
                            path,
                        )
                    )
                )
            }.isSuccess
            snackbarHostState.showSnackbar(
                resources.getString(
                    if (copied) R.string.max_files_detail_copied else R.string.max_files_detail_copy_failed
                )
            )
        }
    }

    MaxListScreen(
        title = stringResource(R.string.storage_care_title),
        subtitle = stringResource(R.string.storage_care_tagline),
        accentIcon = Icons.Rounded.CleaningServices,
        accent = accent,
        onBack = { navActions.back() },
        snackbarHostState = snackbarHostState,
        // بلا شرط حجب: الشاشة تفتح فورًا، وكل فئة تقول «يُقاس…» حتى تصل نتيجتها وحدها.
    ) {
        item {
            CapacityPanel(mount = state.mount, accent = accent)
        }
        item {
            ScopeLine(root = root)
        }
        item {
            MaxSection(
                title = stringResource(R.string.storage_care_clean_title),
                description = stringResource(R.string.storage_care_clean_note),
            ) {
                MaxGroup {
                    for (category in StorageCareCategory.entries.filter { StorageCareModel.visibleAt(root, it) }) {
                        CategoryRow(
                            category = category,
                            measurement = state.measurements.of(category),
                            checked = category in state.selection,
                            pending = category in state.pendingCategories,
                            onToggle = { viewModel.toggle(category) },
                            onOpen = { viewModel.openCategorySheet(category) },
                        )
                    }
                }
            }
        }
        item {
            CleanActionPanel(
                state = state,
                onClean = viewModel::clean,
                onSelectSafe = viewModel::selectSafe,
                onMeasureAgain = { viewModel.measureAll(force = true) },
                accent = accent,
            )
        }
        item {
            AnalysisSection(
                state = state,
                onScan = viewModel::scan,
                onCancelScan = viewModel::cancelScan,
                onDetails = { navActions.navigateTo(MaxDestination.StorageDetail) },
                onCopyPath = copyPath,
                accent = accent,
            )
        }
        item {
            AppsSection(
                state = state,
                onGrant = { context.startActivity(AppStorageReader.usageAccessIntent()) },
                onOpenSheet = viewModel::openAppSheet,
                accent = accent,
            )
        }
        item {
            ToolsSection(navActions = navActions)
        }
    }

    // ورقتان فوق الشاشة: سرد الفئة («ما سيُحذف بالضبط») وتفصيل التطبيق (أجزاؤه وأفعاله).
    CustomBottomSheet(visible = state.categorySheet != null, onDismiss = viewModel::closeCategorySheet) {
        state.categorySheet?.let { sheet ->
            CategorySheetContent(
                sheet = sheet,
                checked = sheet.category in state.selection,
                onToggle = { viewModel.toggle(sheet.category) },
                onClean = viewModel::cleanCategorySheetNow,
                onCopyPath = copyPath,
                onClose = viewModel::closeCategorySheet,
            )
        }
    }
    CustomBottomSheet(visible = state.appSheet != null, onDismiss = viewModel::closeAppSheet) {
        state.appSheet?.let { sheet ->
            AppSheetContent(
                sheet = sheet,
                onClearCache = viewModel::clearAppCacheNow,
                onClearData = {
                    dataDialog.showConfirm(
                        title = dataConfirmTitle,
                        content = dataConfirmBody,
                        confirm = dataConfirmOk,
                        dismiss = dataConfirmCancel,
                    )
                },
                onOpenSettings = { navController.openAppSettings(sheet.app.packageName) },
                onClose = viewModel::closeAppSheet,
            )
        }
    }
    ConfirmDialogHost(handle = dataDialog)
}

/**
 * سعة القرص: النسبة الكبيرة، شريط، والمستخدم من الإجمالي والمتاح.
 *
 * وتُقرأ من **نفس مصدر شاشة التخزين** (`StorageUtil.readMounts`) لا من حساب جديد: رقمان
 * لسعة واحدة يفترقان. و«غير مقيسة» تُكتب نصًّا بلا شريط ولا صفر — شريط فارغ يُقرأ «فارغ».
 */
@Composable
private fun CapacityPanel(mount: MountInfo?, accent: Color) {
    val p = neuralPalette()
    val measured = mount?.isMeasured == true
    val fraction = mount?.usedFraction ?: 0f
    val tone = when {
        !measured -> p.muted
        fraction >= DANGER_FRACTION -> p.danger
        fraction >= BUSY_FRACTION -> p.warn
        else -> accent
    }
    NeuralPanel(accent = tone) {
        NeuralSectionHeader(
            title = stringResource(R.string.detail_storage),
            caption = stringResource(R.string.storage_care_intro),
            accent = tone,
        )
        if (!measured) {
            // «لا تُقرأ» ليست «فارغة»: بلا قراءة لا شريط ولا نسبة.
            Text(
                stringResource(R.string.max_home_unavailable),
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        } else {
            val percent = (fraction.coerceIn(0f, 1f) * 100f).toInt()
            Text(
                stringResource(R.string.storage_care_nearly_full, percent),
                color = tone,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            NeuralTrack(fraction.coerceIn(0f, 1f), tone)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val used = mount?.usedBytes
                val total = mount?.totalBytes
                if (used != null && total != null) {
                    NeuralCaption(
                        stringResource(
                            R.string.storage_care_used_of,
                            StorageScanModel.formatBytes(used),
                            StorageScanModel.formatBytes(total),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                mount?.freeBytes?.let { free ->
                    NeuralCaption(
                        stringResource(R.string.storage_care_free, StorageScanModel.formatBytes(free)),
                        color = tone,
                    )
                }
            }
        }
    }
}

/**
 * سطر النطاق: عند أي طبقة نعمل، وما الذي يوسّع التنظيف.
 *
 * وهو **دائمًا ظاهر** لا عند غياب الجذر وحده: من عنده جذر يقرأ أن كاش التطبيقات وبقايا
 * المُزالة وسجلات النظام **مشمولة**، فيعرف لماذا يرى صفوفًا لا يراها غيره — ومن لا جذر له
 * يقرأ ما ينقصه بالاسم.
 */
@Composable
private fun ScopeLine(root: Boolean) {
    val p = neuralPalette()
    NeuralPanel(accent = if (root) p.ok else p.muted) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralPill(
                text = stringResource(
                    if (root) R.string.max_privilege_level_root else R.string.home_access_basic
                ),
                accent = if (root) p.ok else p.muted,
                filled = true,
                dot = true,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            Text(
                stringResource(
                    if (root) R.string.storage_care_scope_root
                    else R.string.storage_care_scope_limited
                ),
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * صفّ فئة: اسم، وصف، مقاس، وسهم يعلن أن النقر يفتح السرد.
 *
 * **والتأشير والنقر فعلان مختلفان** (طلب المالك): المربّع يأشّر للدفعة، والنقر على الصفّ يفتح
 * **سرد «ما سيُحذف بالضبط»** وزرّه الحقيقي — لأن سؤال «ما هذا؟» سابق لسؤال «نظّفه».
 *
 * **والمقاس هو الحكم على الصفّ:** `pending` (يُقاس الآن) تعرض «يُقاس…» في صفّها وحده لا في
 * الشاشة كلها، و`null` بعد انتهاء الجولة تعني «فشل القياس» وتُعرض «لم يُقس». وفئة حجمها
 * صفر بطبيعتها (`EmptyFolders`) تُعرض **بالعدد** لا بـ`0 B` — لأن العدد هو الخبر.
 */
@Composable
private fun CategoryRow(
    category: StorageCareCategory,
    measurement: CleanMeasurement?,
    checked: Boolean,
    pending: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val p = neuralPalette()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sizeText = when {
        pending -> stringResource(R.string.storage_care_measuring)

        measurement == null || measurement.bytes == null ->
            stringResource(R.string.storage_care_unmeasured)

        StorageCareModel.showsCount(category) ->
            stringResource(R.string.storage_care_count_folders, measurement.count)

        else -> StorageScanModel.formatBytes(measurement.bytes)
    }
    val measured = !pending && measurement?.bytes != null
    MaxRow(
        title = stringResource(categoryTitle(category)),
        subtitle = stringResource(categoryDescription(category)),
        onClick = onOpen,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sizeText,
                    color = if (measured) p.text else p.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (measured) FontWeight.SemiBold else FontWeight.Normal,
                )
                // سهم الفتح: يُعكس في RTL فلا يشير إلى الخلف أبدًا.
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = p.muted,
                    modifier = Modifier
                        .size(MaxSize.iconGlyphSmall)
                        .rotate(if (rtl) 180f else 0f),
                )
                // والمربّع وحده يغيّر التأشير — والصفّ كله يقبل اللمس لفتح السرد.
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
            }
        },
        iconTone = if (checked) MaxTone.Accent else MaxTone.Neutral,
    )
}

/**
 * الزرّ وسطر النتيجة.
 *
 * **والزرّ يحمل الرقم لا «تنظيف» فقط** (§6.2): ما سيُحذف هو مجموع المختار المقيس. وحين لا
 * مجموع (`0`) لا يُعرض `Clean 0 B` بل يُعطَّل الزرّ ويُقال «تنظيف المختار» — والزرّ المعطّل
 * ليس صامتًا: لونه يتغيّر وسببه مكتوب تحته.
 */
@Composable
private fun CleanActionPanel(
    state: StorageCareUiState,
    onClean: () -> Unit,
    onSelectSafe: () -> Unit,
    onMeasureAgain: () -> Unit,
    accent: Color,
) {
    val p = neuralPalette()
    val total = state.totalBytes
    val enabled = state.canClean
    val freed = state.freedBytes
    NeuralPanel(accent = if (enabled) accent else p.muted) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralPill(
                text = if (total > 0L) {
                    stringResource(R.string.storage_care_clean_amount, StorageScanModel.formatBytes(total))
                } else {
                    stringResource(R.string.storage_care_clean_selected)
                },
                accent = if (enabled) accent else p.muted,
                filled = enabled,
                onClick = if (enabled) onClean else null,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.storage_care_select_safe),
                accent = p.muted,
                compact = true,
                onClick = onSelectSafe,
            )
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.storage_care_measure_again),
                accent = p.muted,
                compact = true,
                onClick = if (state.canMeasureAgain) onMeasureAgain else null,
            )
        }
        // سطر الصدق: مجموع ناقص يُعلَن، ونتيجة التنظيف تُعلَن بالرقم المقيس فقط.
        if (state.totalIsPartial) {
            NeuralCaption(stringResource(R.string.storage_care_partial), color = p.warn)
        }
        if (freed != null) {
            NeuralCaption(
                if (freed > 0L) {
                    stringResource(R.string.storage_care_freed, StorageScanModel.formatBytes(freed))
                } else {
                    stringResource(R.string.storage_care_freed_none)
                },
                color = if (freed > 0L) p.ok else p.muted,
            )
        }
        // وحالة القياس معلنة بلا حجب: الأرقام تجري في الخلفية والتصفّح حرّ.
        if (state.measuring) {
            NeuralCaption(stringResource(R.string.storage_care_measuring_background), color = p.muted)
        }
        // وأرقام الجولة السابقة تُعلَن كذلك — لا رقم قديم يُعرض بثقة جولة جديدة.
        if (state.measurementsFromCacheAtMs != null && !state.measuring) {
            NeuralCaption(stringResource(R.string.storage_care_from_cache), color = p.muted)
        }
    }
}

/**
 * قسم التطبيقات: أكبر [AppStorageReader.ROW_LIMIT] تطبيقًا — **بأيقوناتها وسبب كِبَرها**.
 *
 * بأمر المالك: «Biggest apps لا يعرض أيقونات التطبيق ولماذا…» — فالصفّ الآن: الأيقونة أوّلًا
 * (التطبيق يُعرف بها)، وتحتها **تفصيل الرقم** (حزمة · بيانات · كاش) وهو الجواب على «لماذا
 * هذا كبير؟»، والنقر يفتح ورقة التطبيق حيث كل جزء يُمسح وحده — الكاش بلا بيانات.
 *
 * **ودرجتا الصلاحية معلنتان:** ممنوحة ⇒ الأجزاء الثلاثة؛ غير ممنوحة ⇒ «حجم الحزمة فقط» مع
 * حبّة المنح — لا أرقام تبدو كاملة وهي نصف الحقيقة.
 */
@Composable
private fun AppsSection(
    state: StorageCareUiState,
    onGrant: () -> Unit,
    onOpenSheet: (AppStorageEntry) -> Unit,
    accent: Color,
) {
    val p = neuralPalette()
    MaxSection(
        title = stringResource(R.string.storage_care_apps_title),
        description = stringResource(
            if (state.usageAccess) {
                R.string.storage_care_apps_desc_precise
            } else {
                R.string.storage_care_apps_desc_apk
            }
        ),
    ) {
        if (!state.usageAccess) {
            NeuralPanel(accent = p.warn) {
                NeuralPill(
                    text = stringResource(R.string.storage_care_apps_grant),
                    accent = p.warn,
                    filled = true,
                    onClick = onGrant,
                )
            }
        }
        when {
            state.appsLoading -> NeuralPanel(accent = p.muted) {
                NeuralCaption(stringResource(R.string.storage_care_apps_loading), color = p.muted)
            }
            state.apps.isEmpty() -> NeuralPanel(accent = p.muted) {
                NeuralCaption(stringResource(R.string.storage_care_apps_empty), color = p.muted)
            }

            else -> MaxGroup {
                state.apps.forEachIndexed { index, app ->
                    if (index > 0) MaxGroupDivider()
                    AppRow(app = app, onClick = { onOpenSheet(app) })
                }
            }
        }
    }
}

/**
 * صفّ تطبيق: أيقونته، اسمه، **تفصيل أجزائه** (حزمة/بيانات/كاش حين دقّة الصلاحية)، وحجمه.
 *
 * والتفصيل هو «لماذا»: من يرى «كاش ٤GB» يعرف أن مسح الكاش وحده يكفي، ومن يرى بيانات ضخمة
 * يعرف أن علاجه مختلف. والصفّ يفتح الورقة التي تحمل الفعلين مفصولين.
 */
@Composable
private fun AppRow(app: AppStorageEntry, onClick: () -> Unit) {
    val p = neuralPalette()
    val unmeasured = stringResource(R.string.storage_care_unmeasured)
    val subtitle = run {
        val base = if (app.precise) {
            stringResource(
                R.string.storage_care_app_parts,
                app.packageBytes?.let(StorageScanModel::formatBytes) ?: unmeasured,
                app.dataBytes?.let(StorageScanModel::formatBytes) ?: unmeasured,
                app.cacheBytes?.let(StorageScanModel::formatBytes) ?: unmeasured,
            )
        } else {
            app.packageName
        }
        if (app.system) base + " · " + stringResource(R.string.storage_care_apps_system) else base
    }
    ExpressiveListItem(
        onClick = onClick,
        headlineContent = {
            Text(
                app.label,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                subtitle,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            AppIconImage(
                packageName = app.packageName,
                size = MaxSize.rowIconContainer,
                contentDescription = null,
            )
        },
        trailingContent = {
            Text(
                StorageScanModel.formatBytes(app.bytes),
                color = if (app.precise) p.text else p.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

/** أدوات تتفرّق عادةً في شاشات: مدير الملفات، ترجمة الـdex، وتجميد/debloat. */
@Composable
private fun ToolsSection(navActions: MaxNavActions) {
    MaxSection(title = stringResource(R.string.storage_care_tools_title)) {
        MaxGroup {
            MaxNavigationRow(
                title = stringResource(R.string.max_files_title),
                subtitle = stringResource(maxDestinationRole(MaxDestination.FileManager)),
                icon = Icons.Rounded.Folder,
                onClick = { navActions.navigateTo(MaxDestination.FileManager) },
            )
            MaxGroupDivider()
            MaxNavigationRow(
                title = stringResource(R.string.dex2oat_title),
                subtitle = stringResource(maxDestinationRole(MaxDestination.Dex2oat)),
                icon = Icons.Rounded.Science,
                onClick = { navActions.navigateTo(MaxDestination.Dex2oat) },
            )
            MaxGroupDivider()
            MaxNavigationRow(
                title = stringResource(R.string.debloat_freeze_title),
                subtitle = stringResource(maxDestinationRole(MaxDestination.DebloatFreeze)),
                icon = Icons.Rounded.CleaningServices,
                onClick = { navActions.navigateTo(MaxDestination.DebloatFreeze) },
            )
        }
    }
}

/** عنوان الفئة — من سجلّ واحد، فلا نصّان لفئة واحدة. */
internal fun categoryTitle(category: StorageCareCategory): Int = when (category) {
    StorageCareCategory.AppCaches -> R.string.storage_care_cat_app_caches
    StorageCareCategory.SharedCaches -> R.string.storage_care_cat_shared
    StorageCareCategory.CorpseData -> R.string.storage_care_cat_corpse
    StorageCareCategory.Thumbnails -> R.string.storage_care_cat_thumbs
    StorageCareCategory.InstallerFiles -> R.string.storage_care_cat_apk
    StorageCareCategory.EmptyFolders -> R.string.storage_care_cat_empty
    StorageCareCategory.SystemLogs -> R.string.storage_care_cat_logs
    StorageCareCategory.Duplicates -> R.string.storage_care_cat_duplicates
    StorageCareCategory.BrokenSymlinks -> R.string.storage_care_cat_broken_symlinks
    StorageCareCategory.Databases -> R.string.storage_care_cat_databases
    StorageCareCategory.RecycleBin -> R.string.storage_care_cat_recycle_bin
}

internal fun categoryDescription(category: StorageCareCategory): Int = when (category) {
    StorageCareCategory.AppCaches -> R.string.storage_care_cat_app_caches_desc
    StorageCareCategory.SharedCaches -> R.string.storage_care_cat_shared_desc
    StorageCareCategory.CorpseData -> R.string.storage_care_cat_corpse_desc
    StorageCareCategory.Thumbnails -> R.string.storage_care_cat_thumbs_desc
    StorageCareCategory.InstallerFiles -> R.string.storage_care_cat_apk_desc
    StorageCareCategory.EmptyFolders -> R.string.storage_care_cat_empty_desc
    StorageCareCategory.SystemLogs -> R.string.storage_care_cat_logs_desc
    StorageCareCategory.Duplicates -> R.string.storage_care_cat_duplicates_desc
    StorageCareCategory.BrokenSymlinks -> R.string.storage_care_cat_broken_symlinks_desc
    StorageCareCategory.Databases -> R.string.storage_care_cat_databases_desc
    StorageCareCategory.RecycleBin -> R.string.storage_care_cat_recycle_bin_desc
}
