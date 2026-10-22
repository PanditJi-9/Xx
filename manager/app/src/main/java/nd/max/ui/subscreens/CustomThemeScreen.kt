/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package nd.max.ui.subscreens


import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import nd.max.ui.design.maxEdgeFade
import nd.max.ui.design.maxBleed
import android.app.Activity
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlinx.coroutines.launch
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxConfirmDialog
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.maxRadius
import nd.max.ui.design.MaxSectionSpec
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.LocalMaxRadiusScale
import nd.max.ui.design.LocalMaxCardStyle
import nd.max.ui.design.LocalMaxSpaceScale
import nd.max.ui.design.MaxCardStyle
import nd.max.R
import nd.max.ui.component.*
import nd.max.ui.design.MaxSplitScreen
import nd.max.ui.theme.ColorMode
import nd.max.ui.theme.MaxManagerDefaultKeyColor
import nd.max.ui.theme.NavBarStyle
import nd.max.ui.theme.NavLabelMode
import nd.max.ui.theme.ThemeContrast
import nd.max.ui.theme.ThemeController
import nd.max.ui.theme.ThemeDensity
import nd.max.ui.theme.ThemeFontScale
import nd.max.ui.theme.ThemeKeys
import nd.max.ui.theme.ThemeMotion
import nd.max.ui.theme.ThemePreset
import nd.max.ui.theme.ThemeRoundness
import nd.max.ui.theme.TopBarStyle
import nd.max.ui.theme.ThemeStudio
import nd.max.ui.theme.animateColorSchemeAsState
import nd.max.ui.util.clearHeaderImage
import nd.max.core.platform.EventLog
import nd.max.ui.util.getBannerGradientAlpha
import nd.max.ui.util.getHeaderImage
import nd.max.ui.util.isBannerImageEnabled
import nd.max.ui.util.saveHeaderImage
import nd.max.ui.util.saveMediaDirectly
import nd.max.ui.util.setBannerGradientAlpha
import nd.max.ui.util.setBannerImageEnabled


// ─── Palette of selectable key colors ─────────────────────────────────────────
private val keyColorOptions = listOf(
    Color(0xFFF44336).toArgb(),
    Color(0xFFE91E63).toArgb(),
    Color(0xFF9C27B0).toArgb(),
    Color(0xFF673AB7).toArgb(),
    Color(0xFF3F51B5).toArgb(),
    Color(0xFF2196F3).toArgb(),
    Color(0xFF00BCD4).toArgb(),
    Color(0xFF009688).toArgb(),
    Color(0xFF4FAF50).toArgb(),
    Color(0xFFFFEB3B).toArgb(),
    Color(0xFFFFC107).toArgb(),
    Color(0xFFFF9800).toArgb(),
    Color(0xFF795548).toArgb(),
    Color(0xFF607D8F).toArgb(),
    Color(0xFFFF9CA8).toArgb(),
)

/**
 * المعاينة الثابتة في الوضع الرأسيّ: ارتفاع الهاتف يكفي لشريط التنقل والبطاقات دون قصّ،
 * والعرض يتبع نسبة الهاتف نفسها.
 */
private val PreviewPhoneHeight: Dp = 320.dp
private val PreviewPhoneWidth: Dp = PreviewPhoneHeight * 0.48f

/**
 * هل تُرسم ضوابط اللافتة في شاشة السمة؟ — لا (أمر المالك).
 *
 * والسبب **مقيس لا مُقدَّر**: `BannerCard` في `ui/component/HomeComponents.kt` لا منادي
 * له في الشجرة كلها (`grep -rn BannerCard manager/app/src/main` ⇒ تعريفه وحده)، والشاشة
 * الرئيسية الجديدة تبني ترويستها الخاصّ (`HomeHeader`) ولا تقرأ صورة اللافتة. فصارت
 * الضوابط تَعِد بأثر لا يقع: مفتاح وشاشة منتقي ملف ودرجات شفافية تُكتب في التفضيلات
 * ولا يراها أحد.
 *
 * والكود باقٍ كما هو بلا حذف (ADR-18): هذا المفتاح يعيد القسم بكلمة واحدة، ومُشغّل
 * اللافتة نفسه (`BannerImageUtil`) لم يُمسّ حتى لا يُصبح رجوعه إعادة كتابة.
 */
private val ThemeBannerSectionVisible = false

// ─── Root screen ──────────────────────────────────────────────────────────────
@Composable
fun ColorPaletteScreen(navController: NavController) {
    val context = LocalContext.current
    // موارد من `LocalResources.current`: رسائل الشريط السفلي تُبنى داخل `launch`/لامبدات النتائج.
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isBannerEnabled by rememberSaveable { mutableStateOf(context.isBannerImageEnabled()) }
    var bannerGradientAlpha by rememberSaveable { mutableFloatStateOf(context.getBannerGradientAlpha()) }
    var customBannerUri by remember { mutableStateOf(context.getHeaderImage()) }
    var pendingCropUriPath by rememberSaveable { mutableStateOf<String?>(null) }
    var isBlurEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("expressive_blur_ui", false)) }

    val colorScheme = MaterialTheme.colorScheme

    // ── Crop launcher ─────────────────────────────────────────────────────────
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UCrop.getOutput(result.data!!)?.let {
                context.saveHeaderImage(it.toString())
                customBannerUri = it.toString()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.str_banner_updated))
                }
            }
        } else {
            pendingCropUriPath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
        pendingCropUriPath = null
    }

    // ── Image / video picker ──────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            val isGif = mimeType == "image/gif"
            val isVideoOrGif = isVideo || isGif

            var sizeBytes = 0L
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
            if (sizeBytes > 50 * 1024 * 1024L) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.str_max_video_size))
                }
                return@let
            }

            if (isVideo) {
                var durationMs = 0L
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, sourceUri)
                    durationMs = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
                if (durationMs > 30_000L) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(resources.getString(R.string.str_video_too_long))
                    }
                    return@let
                }
            }

            if (isVideoOrGif) {
                coroutineScope.launch {
                    val ext = if (isVideo) "mp4" else "gif"
                    val saved = context.saveMediaDirectly(sourceUri, ext)
                    if (saved != null) {
                        context.saveHeaderImage(saved)
                        customBannerUri = saved
                        snackbarHostState.showSnackbar(resources.getString(R.string.str_pick_media_success))
                    } else {
                        snackbarHostState.showSnackbar(resources.getString(R.string.str_pick_media_fail))
                    }
                }
            } else {
                val bannerDir = File(context.filesDir, "banners").also { if (!it.exists()) it.mkdirs() }
                val destFile = File(bannerDir, "banner_${System.currentTimeMillis()}.jpg")
                pendingCropUriPath = destFile.absolutePath
                val destUri = Uri.fromFile(destFile)

                val options = UCrop.Options().apply {
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                    setToolbarColor(colorScheme.surface.toArgb())
                    setToolbarWidgetColor(colorScheme.onSurface.toArgb())
                    setRootViewBackgroundColor(colorScheme.surfaceContainerLowest.toArgb())
                    setActiveControlsWidgetColor(colorScheme.primary.toArgb())
                    setCropFrameColor(colorScheme.primary.toArgb())
                    setCropGridColor(colorScheme.primary.copy(alpha = 0.5f).toArgb())
                    setDimmedLayerColor(colorScheme.scrim.copy(alpha = 0.6f).toArgb())
                }
                cropLauncher.launch(
                    UCrop.of(sourceUri, destUri).withAspectRatio(20f, 9f).withOptions(options)
                        .getIntent(context)
                )
            }
        }
    }

    // ── Theme state ───────────────────────────────────────────────────────────
    var currentColorMode by remember { mutableStateOf(ThemeController.getAppSettings(context).colorMode) }
    var currentKeyColor by remember { mutableIntStateOf(ThemeController.getAppSettings(context).keyColor) }
    var currentColorSpec by remember { mutableStateOf(ThemeController.getAppSettings(context).colorSpec) }
    // خيارات «استوديو السمة» الجديدة — مصدرها الواحد `ThemeStudio` (المفتاح والقراءة والافتراضيّ
    // في ملفّ واحد)، وكلٌّ منها يُحفظ عند اختياره في موضعه من الأقسام أدناه.
    var currentContrast by remember { mutableStateOf(ThemeStudio.contrast(prefs)) }
    var currentRoundness by remember { mutableStateOf(ThemeStudio.roundness(prefs)) }
    var currentFontScale by remember { mutableStateOf(ThemeStudio.fontScale(prefs)) }
    var currentNavStyle by remember { mutableStateOf(ThemeStudio.navBarStyle(prefs)) }
    var currentTopStyle by remember { mutableStateOf(ThemeStudio.topBarStyle(prefs)) }
    var currentMotion by remember { mutableStateOf(ThemeStudio.motion(prefs)) }
    // محورا §٦-ج: كثافة المعلومات وشكل البطاقات — نفس عقد `ThemeStudio` (الافتراضيّ = اليوم).
    var currentDensity by remember { mutableStateOf(ThemeStudio.density(prefs)) }
    var currentCardStyle by remember { mutableStateOf(ThemeStudio.cardStyle(prefs)) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
    val amoledMode = currentColorMode == ColorMode.DARKAMOLED

    // ── تطبيق نمط جاهز: الخصائص الأربع معًا بضغطة — القيم تُحدَّث فورًا (الشاشة والمعاينة) وتُكتب
    // في التفضيلات فلا تُفقد عند الخروج، وحدث واحد باسم النمط لا أربعة أحداث.
    val applyPreset: (ThemePreset) -> Unit = { preset ->
        currentKeyColor = preset.seed
        currentColorMode = preset.mode
        currentContrast = preset.contrast
        currentRoundness = preset.roundness
        EventLog.userAction(screen = "CustomTheme", field = "theme_preset", old = "custom", new = preset.id)
        prefs.edit {
            putInt("key_color", preset.seed)
            putInt("color_mode", preset.mode.value)
            putString(ThemeKeys.CONTRAST, preset.contrast.key)
            putString(ThemeKeys.ROUNDNESS, preset.roundness.key)
        }
    }

    // ── إعادة الضبط: مفاتيح هذه الشاشة كلها إلى قيمها الأصلية (نفسها قيم أول تشغيل)، بنقرة
    // تأكيد واحدة. ولا يُمسّ شيء خارج نطاق الشاشة (لا لافتة ولا صور).
    val applyThemeReset: () -> Unit = {
        currentColorMode = ColorMode.SYSTEM
        currentKeyColor = MaxManagerDefaultKeyColor
        currentColorSpec = ColorSpec.SpecVersion.entries.first()
        currentContrast = ThemeContrast.DEFAULT
        currentRoundness = ThemeRoundness.STANDARD
        currentFontScale = ThemeFontScale.STANDARD
        currentNavStyle = NavBarStyle.FLOATING
        currentTopStyle = TopBarStyle.COMPACT_ICON
        currentMotion = ThemeMotion.FULL
        currentDensity = ThemeDensity.STANDARD
        currentCardStyle = MaxCardStyle.OUTLINED
        isBlurEnabled = false
        EventLog.userAction(screen = "CustomTheme", field = "theme_reset", old = "custom", new = "default")
        prefs.edit {
            putInt("color_mode", ColorMode.SYSTEM.value)
            putInt("key_color", MaxManagerDefaultKeyColor)
            putString("color_spec", ColorSpec.SpecVersion.entries.first().name)
            putString(ThemeKeys.CONTRAST, ThemeContrast.DEFAULT.key)
            putString(ThemeKeys.ROUNDNESS, ThemeRoundness.STANDARD.key)
            putString(ThemeKeys.FONT_SCALE, ThemeFontScale.STANDARD.key)
            putString(ThemeKeys.NAV_BAR_STYLE, NavBarStyle.FLOATING.key)
            putString(ThemeKeys.TOP_BAR, TopBarStyle.COMPACT_ICON.key)
            putString(ThemeKeys.MOTION, ThemeMotion.FULL.key)
            putString(ThemeKeys.DENSITY, ThemeDensity.STANDARD.key)
            putString(ThemeKeys.CARD_STYLE, MaxCardStyle.OUTLINED.key)
            putBoolean("expressive_blur_ui", false)
        }
        coroutineScope.launch {
            snackbarHostState.showSnackbar(resources.getString(R.string.theme_reset_done))
        }
    }

    ScreenAccentProvider(MaterialTheme.colorScheme.primary) {
        // The split shell hosts both orientations: a two-pane Row in landscape and one lazy column
        // in portrait, with the top bar, insets and snackbar owned by the shell.
        MaxSplitScreen(
            title = stringResource(R.string.theme),
            onBack = { navController.popBackStack() },
            accentIcon = Icons.Filled.Colorize,
            accent = MaterialTheme.colorScheme.primary,
            snackbarHostState = snackbarHostState
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(
                            LocalMaxRadiusScale provides currentRoundness.scale,
                            LocalMaxSpaceScale provides currentDensity.scale,
                            LocalMaxCardStyle provides currentCardStyle,
                        ) {
                            ThemePreviewCard(
                                keyColor = currentKeyColor,
                                colorSpec = currentColorSpec,
                                isDark = isDark,
                                isAmoled = amoledMode,
                                isLandscape = true,
                                navStyle = currentNavStyle,
                                topBarStyle = currentTopStyle,
                                motion = currentMotion,
                                density = currentDensity,
                                cardStyle = currentCardStyle
                            )
                        }
                    }

                    LazyColumn(
                        // اللوحة اليمنى تمتدّ إلى حافة الشاشة وحدها، فالسوابح في داخلها تمتدّ معها دون أن تغطّي
                        // اللوحة المعاينة على اليسار.
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .maxBleed(start = 0.dp, end = MaxSpace.gutter),
                        // الهيكل يحجز أسفل الشاشة أصلًا (`floatingBottomBarPadding`)، فـ16dp هنا
                        // تُضاف فوق الحجز فتبقى فراغًا لا يراه أحد. والباقي بخطوة الرموز.
                        contentPadding = PaddingValues(start = MaxSpace.gutter, top = MaxSpace.md, end = MaxSpace.gutter, bottom = MaxSpace.sm),
                        verticalArrangement = Arrangement.spacedBy(MaxSectionSpec.spaceBefore)
                    ) {
                        item {
                            StudioSectionHeader(
                                title = stringResource(R.string.theme),
                                subtitle = stringResource(R.string.theme_subtitle),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        themeSettingsItems(
                            swatchStartBleed = 0.dp,
                            currentColorMode = currentColorMode,
                            currentKeyColor = currentKeyColor,
                            currentColorSpec = currentColorSpec,
                            currentContrast = currentContrast,
                            currentRoundness = currentRoundness,
                            currentFontScale = currentFontScale,
                            currentDensity = currentDensity,
                            currentCardStyle = currentCardStyle,
                            currentNavStyle = currentNavStyle,
                            currentTopStyle = currentTopStyle,
                            currentMotion = currentMotion,
                            advancedExpanded = advancedExpanded,
                            isDark = isDark,
                            isBannerEnabled = isBannerEnabled,
                            bannerGradientAlpha = bannerGradientAlpha,
                            customBannerUri = customBannerUri,
                            isBlurEnabled = isBlurEnabled,
                            prefs = prefs,
                            onColorModeChange = { currentColorMode = it },
                            onKeyColorChange = { currentKeyColor = it },
                            onColorSpecChange = { currentColorSpec = it },
                            onContrastChange = { currentContrast = it },
                            onRoundnessChange = { currentRoundness = it },
                            onFontScaleChange = { currentFontScale = it },
                            onDensityChange = { currentDensity = it },
                            onCardStyleChange = { currentCardStyle = it },
                            onNavStyleChange = { currentNavStyle = it },
                            onTopBarChange = { currentTopStyle = it },
                            onMotionChange = { currentMotion = it },
                            onAdvancedExpandedChange = { advancedExpanded = it },
                            onApplyPreset = applyPreset,
                            onResetTheme = { showResetDialog = true },
                            onBannerEnabledChange = {
                                isBannerEnabled = it; context.setBannerImageEnabled(it)
                            },
                            onBannerGradientAlphaChange = {
                                bannerGradientAlpha = it; context.setBannerGradientAlpha(it)
                            },
                            onBannerUpdated = { customBannerUri = it },
                            onBlurEnabledChange = {
                                EventLog.userAction(screen = "CustomTheme", field = "expressive_blur_ui", old = isBlurEnabled.toString(), new = it.toString())
                                isBlurEnabled = it; prefs.edit { putBoolean("expressive_blur_ui", it) }
                            },
                            imagePicker = imagePicker,
                            context = context,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // المعاينة ثابتة فوق الإعدادات: لا تنزلق مع القائمة، فيرى المستخدم أثر كل اختيار لحظة اختياره.
                    CompositionLocalProvider(
                        LocalMaxRadiusScale provides currentRoundness.scale,
                        LocalMaxSpaceScale provides currentDensity.scale,
                        LocalMaxCardStyle provides currentCardStyle,
                    ) {
                        ThemePreviewCard(
                            keyColor = currentKeyColor,
                            colorSpec = currentColorSpec,
                            isDark = isDark,
                            isAmoled = amoledMode,
                            isLandscape = false,
                            navStyle = currentNavStyle,
                            topBarStyle = currentTopStyle,
                            motion = currentMotion,
                            density = currentDensity,
                            cardStyle = currentCardStyle
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().maxBleed().weight(1f),
                        contentPadding = PaddingValues(start = MaxSpace.gutter, top = MaxSpace.md, end = MaxSpace.gutter, bottom = MaxSpace.sm),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        themeSettingsItems(
                            currentColorMode = currentColorMode,
                            currentKeyColor = currentKeyColor,
                            currentColorSpec = currentColorSpec,
                            currentContrast = currentContrast,
                            currentRoundness = currentRoundness,
                            currentFontScale = currentFontScale,
                            currentDensity = currentDensity,
                            currentCardStyle = currentCardStyle,
                            currentNavStyle = currentNavStyle,
                            currentTopStyle = currentTopStyle,
                            currentMotion = currentMotion,
                            advancedExpanded = advancedExpanded,
                            isDark = isDark,
                            isBannerEnabled = isBannerEnabled,
                            bannerGradientAlpha = bannerGradientAlpha,
                            customBannerUri = customBannerUri,
                            isBlurEnabled = isBlurEnabled,
                            prefs = prefs,
                            onColorModeChange = { currentColorMode = it },
                            onKeyColorChange = { currentKeyColor = it },
                            onColorSpecChange = { currentColorSpec = it },
                            onContrastChange = { currentContrast = it },
                            onRoundnessChange = { currentRoundness = it },
                            onFontScaleChange = { currentFontScale = it },
                            onDensityChange = { currentDensity = it },
                            onCardStyleChange = { currentCardStyle = it },
                            onNavStyleChange = { currentNavStyle = it },
                            onTopBarChange = { currentTopStyle = it },
                            onMotionChange = { currentMotion = it },
                            onAdvancedExpandedChange = { advancedExpanded = it },
                            onApplyPreset = applyPreset,
                            onResetTheme = { showResetDialog = true },
                            onBannerEnabledChange = {
                                isBannerEnabled = it; context.setBannerImageEnabled(it)
                            },
                            onBannerGradientAlphaChange = {
                                bannerGradientAlpha = it; context.setBannerGradientAlpha(it)
                            },
                            onBannerUpdated = { customBannerUri = it },
                            onBlurEnabledChange = {
                                EventLog.userAction(screen = "CustomTheme", field = "expressive_blur_ui", old = isBlurEnabled.toString(), new = it.toString())
                                isBlurEnabled = it; prefs.edit { putBoolean("expressive_blur_ui", it) }
                            },
                            imagePicker = imagePicker,
                            context = context,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }
    }

    // وبرّ التأكيد: إعادة الضبط تمسّ مفاتيح الشاشة الثمانية دفعةً، فلا تقع بلمسة عابرة على زرٍّ في الذيل.
    if (showResetDialog) {
        MaxConfirmDialog(
            visible = true,
            title = stringResource(R.string.theme_reset_title),
            message = stringResource(R.string.theme_reset_message),
            confirmLabel = stringResource(R.string.theme_reset_confirm),
            onConfirm = {
                applyThemeReset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
            icon = Icons.Filled.Restore,
        )
    }
}

// ─── Lazy list items ──────────────────────────────────────────────────────────
private fun androidx.compose.foundation.lazy.LazyListScope.themeSettingsItems(
    swatchStartBleed: Dp = MaxSpace.gutter,
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    currentColorSpec: ColorSpec.SpecVersion,
    currentContrast: ThemeContrast,
    currentRoundness: ThemeRoundness,
    currentFontScale: ThemeFontScale,
    currentDensity: ThemeDensity,
    currentCardStyle: MaxCardStyle,
    currentNavStyle: NavBarStyle,
    currentTopStyle: TopBarStyle,
    currentMotion: ThemeMotion,
    advancedExpanded: Boolean,
    isDark: Boolean,
    isBannerEnabled: Boolean,
    bannerGradientAlpha: Float,
    customBannerUri: String?,
    isBlurEnabled: Boolean,
    prefs: android.content.SharedPreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    onContrastChange: (ThemeContrast) -> Unit,
    onRoundnessChange: (ThemeRoundness) -> Unit,
    onFontScaleChange: (ThemeFontScale) -> Unit,
    onDensityChange: (ThemeDensity) -> Unit,
    onCardStyleChange: (MaxCardStyle) -> Unit,
    onNavStyleChange: (NavBarStyle) -> Unit,
    onTopBarChange: (TopBarStyle) -> Unit,
    onMotionChange: (ThemeMotion) -> Unit,
    onAdvancedExpandedChange: (Boolean) -> Unit,
    onApplyPreset: (ThemePreset) -> Unit,
    onResetTheme: () -> Unit,
    onBannerEnabledChange: (Boolean) -> Unit,
    onBannerGradientAlphaChange: (Float) -> Unit,
    onBannerUpdated: (String?) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {

    // ── Section: Presets ───────────────────────────────────────────────────
    // أوّل ما تقع عليه العين بعد المعاينة: «طبعات» كاملة تُطبَّق بضغطة — للمستخدم الجديد
    // أقصر طريق إلى نتيجة متناسقة، وللخبير نقطة انطلاق يُدقّقها بالتفاصيل تحته مباشرةً.
    item {
        ThemeSectionHeader(
            // «الطبعات» لوحةُ ألوان تامة تُطبَّق بضغطة — فالرمز لوحةٌ (`Palette`)، لا نجمةَ
            // الذكاء: النجمة صارت في التطبيق **رمز Max AI وحده** (ملاحظة §٩: «AI مفرط»).
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.theme_presets_title),
            modifier = Modifier.padding(bottom = MaxSectionSpec.spaceAfter)
        )
    }

    item {
        ThemePresetsRow(
            colorSpec = currentColorSpec,
            isSystemDark = isDark,
            selectedKeyColor = currentKeyColor,
            selectedMode = currentColorMode,
            selectedContrast = currentContrast,
            selectedRoundness = currentRoundness,
            swatchStartBleed = swatchStartBleed,
            onApply = onApplyPreset,
        )
    }

    // ── Section: Accent Color ──────────────────────────────────────────────
    item {
        ThemeSectionHeader(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.accent_color),
            // `MaxSplitScreen` يملك هامش الصفحة (`MaxSpace.gutter` 20dp)، وكان هذا السطر يضيف
            // `start = 20.dp, end = 16.dp` فوقه ⇒ 40dp يسارًا و36dp يمينًا: لا متماثلَين ولا
            // مطابقَين لأي عنصر آخر في الشاشة. والفراغان الرأسيان من عقد القسم نفسه.
            modifier = Modifier.padding(
                top = MaxSectionSpec.spaceBefore,
                bottom = MaxSectionSpec.spaceAfter
            )
        )
    }

    item {
        AccentSwatchRow(
            palette = keyColorOptions,
            swatchStartBleed = swatchStartBleed,
            currentKeyColor = currentKeyColor,
            currentColorSpec = currentColorSpec,
            isDark = isDark,
            prefs = prefs,
            onKeyColorChange = onKeyColorChange
        )
    }

    // ── Section: Appearance (dark / light) ────────────────────────────────
    item { Spacer(Modifier.height(24.dp)) }

    item {
        ThemeSectionHeader(
            icon = Icons.Outlined.DarkMode,
            title = stringResource(R.string.appearance),
            modifier = Modifier.padding(bottom = MaxSectionSpec.spaceAfter)
        )
    }

    item {
        // الأسماء من الموارد لا حرفيّة: كانت `"System"/"Light"/"Dark"/"AMOLED"` تُطبع
        // في واجهة عربية (عيب لغوي وبصري معًا)، وكانت الوسوم فارغة (`label = {}`) فلا
        // يقرأ المستخدم إلا أربع أيقونات بلا كلمة — وهو ما يجعل الصفّ يبدو بلا خيارات.
        val options = listOf(
            ColorMode.SYSTEM to (Icons.Filled.Brightness4 to stringResource(R.string.theme_mode_system)),
            ColorMode.LIGHT to (Icons.Filled.Brightness7 to stringResource(R.string.theme_mode_light)),
            ColorMode.DARK to (Icons.Filled.Brightness3 to stringResource(R.string.theme_mode_dark)),
            ColorMode.DARKAMOLED to (Icons.Filled.Brightness1 to stringResource(R.string.theme_mode_amoled)),
        )
        SingleChoiceSegmentedButtonRow(
            // `MaxSplitScreen` يملك هامش الصفحة (`MaxSpace.gutter`)؛ وكان 16dp هنا يُضاف فوقه
            // ⇒ 36dp لكل جهة بدل 20dp في بقية الشاشات.
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, (mode, iconAndLabel) ->
                val (icon, label) = iconAndLabel
                SegmentedButton(
                    selected = currentColorMode == mode,
                    onClick = {
                        onColorModeChange(mode)
                        EventLog.userAction(screen = "CustomTheme", field = "color_mode", old = currentColorMode.value.toString(), new = mode.value.toString())
                        prefs.edit { putInt("color_mode", mode.value) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    // النصّ يشرح والأيقونة تدلّ: وسمٌ بلا كلمة كان يترك الاختيار تخمينًا.
                    icon = { Icon(imageVector = icon, contentDescription = null) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }

    // ── Section: Advanced (spec · contrast · roundness · font) ──────────────
    // التفاصيل الدقيقة خلف باب واحد مطويّ: المبتدئ لا يغرق، والخبير يفتح فيجد الخيارات كلها
    // حيّة. ومواصفة الألوان انتقلت إلى هنا ولم تُحذف — تفصيل تقنيّ لا خيار يوميّ، ومكانها الطبيعيّ
    // بجانب أقرانها (التباين · الاستدارة · حجم النص) لا في صفّ مستقلّ في وجه المبتدئ.
    item { Spacer(Modifier.height(24.dp)) }

    item {
        ThemeAdvancedSection(
            expanded = advancedExpanded,
            onExpandedChange = onAdvancedExpandedChange,
            currentColorSpec = currentColorSpec,
            onColorSpecChange = onColorSpecChange,
            currentContrast = currentContrast,
            onContrastChange = onContrastChange,
            currentRoundness = currentRoundness,
            onRoundnessChange = onRoundnessChange,
            currentFontScale = currentFontScale,
            onFontScaleChange = onFontScaleChange,
            currentDensity = currentDensity,
            onDensityChange = onDensityChange,
            currentCardStyle = currentCardStyle,
            onCardStyleChange = onCardStyleChange,
            prefs = prefs,
        )
    }

    if (ThemeBannerSectionVisible) {
    // ── Section: Banner ───────────────────────────────────────────────────
    item { Spacer(Modifier.height(24.dp)) }

    item {
        ThemeSectionHeader(
            icon = Icons.Outlined.Image,
            title = stringResource(R.string.banner),
            modifier = Modifier.padding(bottom = MaxSectionSpec.spaceAfter)
        )
    }

    item {
        ExpressiveColumn(
            modifier = Modifier,
            content = buildList {
                add {
                    Column {
                        ExpressiveSwitchItem(
                            icon = Icons.Outlined.Image,
                            title = stringResource(R.string.str_enable_banner),
                            checked = isBannerEnabled,
                            onCheckedChange = onBannerEnabledChange
                        )

                        AnimatedVisibility(
                            visible = isBannerEnabled,
                            enter = expandVertically(tween(MaxDuration.deliberate)) + fadeIn(tween(MaxDuration.deliberate)),
                            exit = shrinkVertically(tween(MaxDuration.deliberate)) + fadeOut(tween(MaxDuration.deliberate))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = MaxCardSpec.padding,
                                        end = MaxCardSpec.padding,
                                        bottom = MaxCardSpec.padding
                                    ),
                                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    nd.max.ui.component.StudioOutlinedButton(
                                        onClick = {
                                            imagePicker.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(
                                            topStart = 50.dp, bottomStart = 50.dp
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.str_pick_media),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    nd.max.ui.component.StudioOutlinedButton(
                                        onClick = {
                                            context.clearHeaderImage()
                                            onBannerUpdated(null)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.str_default_banner_toast)
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(
                                            topEnd = 50.dp, bottomEnd = 50.dp
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.default_label),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Gradient control card — shown only when the banner is on
    item {
        AnimatedVisibility(
            visible = isBannerEnabled,
            enter = expandVertically(tween(MaxDuration.deliberate)) + fadeIn(tween(MaxDuration.deliberate)),
            exit = shrinkVertically(tween(MaxDuration.deliberate)) + fadeOut(tween(MaxDuration.deliberate))
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                ExpressiveColumn(
                    modifier = Modifier,
                    content = buildList {
                        add {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaxCardSpec.padding),
                                verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
                            ) {
                                // Banner preview
                                BannerGradientPreview(
                                    gradientAlpha = bannerGradientAlpha,
                                    customBannerUri = customBannerUri
                                )

                                // Gradient opacity row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LeadingIcon(icon = Icons.Outlined.Gradient)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.str_adjust_gradient),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.str_gradient_opacity),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    R.string.str_bannergradientalpha_100_toint,
                                                    (bannerGradientAlpha * 100).toInt()
                                                ),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { onBannerGradientAlphaChange(0.5f) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Restore,
                                                    contentDescription = stringResource(R.string.reset),
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    MaxSlider(
                                        value = bannerGradientAlpha,
                                        onValueChange = { v ->
                                            onBannerGradientAlphaChange(
                                                if (v in 0.47f..0.53f) 0.5f else v
                                            )
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    }

    // ── Section: Interface ────────────────────────────────────────────────
    item { Spacer(Modifier.height(24.dp)) }

    item {
        ThemeSectionHeader(
            icon = Icons.Outlined.Tune,
            title = stringResource(R.string.str_interface),
            modifier = Modifier.padding(bottom = MaxSectionSpec.spaceAfter)
        )
    }

    item {
        ThemeInterfaceControls(
            currentTopBar = currentTopStyle,
            onTopBarChange = onTopBarChange,
            currentNavStyle = currentNavStyle,
            onNavStyleChange = onNavStyleChange,
            currentMotion = currentMotion,
            onMotionChange = onMotionChange,
            prefs = prefs,
        )
    }

    item { Spacer(Modifier.height(MaxSpace.xl)) }

    item {
        ExpressiveColumn(
            modifier = Modifier,
            content = buildList {
                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.BlurOn,
                        title = stringResource(R.string.str_expressive_blur),
                        summary = stringResource(R.string.str_expressive_blur_summary),
                        checked = isBlurEnabled,
                        onCheckedChange = onBlurEnabledChange
                    )
                }
            }
        )
    }

    // ── Reset ──────────────────────────────────────────────────────────────
    item { Spacer(Modifier.height(MaxSpace.xl)) }

    item {
        ThemeResetRow(onClick = onResetTheme)
    }

    item { Spacer(Modifier.height(16.dp)) }
}

// ─── Section header helper ────────────────────────────────────────────────────
@Composable
private fun ThemeSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Banner gradient preview ──────────────────────────────────────────────────
@Composable
private fun BannerGradientPreview(gradientAlpha: Float, customBannerUri: String?) {
    val colorScheme = MaterialTheme.colorScheme

    // **ولا لمعان دوريّ (§٩):** كان شريط بيضاء قطريًّا يعبر المعاينة كل 3.2 ثانية «ليُبقيها حيّة».
    // والمعاينة تُظهر ما اختاره المستخدم فعلًا؛ حركةٌ لا تحمل معلومة تُقرأ إعلانًا لا أداة.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.inset))),
        color = colorScheme.surfaceContainerHighest
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = customBannerUri,
                animationSpec = tween(MaxDuration.deliberate),
                label = "banner_crossfade"
            ) { uri ->
                MediaBannerRenderer(uriString = uri, modifier = Modifier.fillMaxSize())
            }
            // Bottom fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colorScheme.surfaceContainerLow.copy(alpha = gradientAlpha)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp),
                color = colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.str_preview),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ─── Theme preview phone card ─────────────────────────────────────────────────

/**
 * ارتفاع كتلة الشريط في المعاينة بحسب النمط — **الرقم تابع للبنية لا ذوقًا**: الأنماط الثلاثة
 * الجديدة صفٌّ ثابت (لا عنوان كبير يتمدّد) فأقصر من الكلاسيكيّ، والشفّاف أعلى قليلًا ليتّسع
 * لتدرّجه، والكلاسيكيّ على ما كان عليه فلا تتغيّر معاينةُ من لم يختر.
 */
private fun previewHeroHeight(style: TopBarStyle): Dp = when (style) {
    TopBarStyle.COMPACT_ICON -> 40.dp
    TopBarStyle.TITLE_ONLY -> 40.dp
    TopBarStyle.TRANSLUCENT -> 44.dp
    TopBarStyle.CLASSIC -> 62.dp
}
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    colorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    isAmoled: Boolean,
    isLandscape: Boolean,
    navStyle: NavBarStyle,
    topBarStyle: TopBarStyle,
    motion: ThemeMotion,
    density: ThemeDensity,
    cardStyle: MaxCardStyle
) {
    val context = LocalContext.current

    val targetColorScheme = if (keyColor == 0) {
        val base = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else lightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = base.primary,
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec,
            primary = base.primary,
            secondary = base.secondary,
            tertiary = base.tertiary,
            neutral = base.surface,
            neutralVariant = base.surfaceVariant,
            error = base.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor),
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec
        )
    }

    val cs = animateColorSchemeAsState(targetColorScheme)

    // **هالٌ ساكن لا نابض (§٩):** كان يتنفّس بين 0.12 و0.28 كل ثانيتين حول معاينة الهاتف.
    // والمعاينة تحاكي الشكل لا الحياة؛ فالشدّة ثابتة، والحركة باقية فقط لخيارات المستخدم.
    val glowAlpha = 0.16f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 20.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft colored halo
        Box(
            modifier = Modifier
                .size(
                    width = if (isLandscape) 130.dp else 120.dp,
                    height = if (isLandscape) 270.dp else 250.dp
                )
                .clip(RoundedCornerShape(maxRadius(MaxRadius.sheet)))
                .background(cs.primary.copy(alpha = glowAlpha * 0.5f))
        )

        // Phone surface
        Surface(
            modifier = if (isLandscape) {
                Modifier.fillMaxWidth(0.80f).aspectRatio(0.48f)
            } else {
                Modifier.size(width = PreviewPhoneWidth, height = PreviewPhoneHeight)
            },
            color = cs.surface,
            shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 12.dp
        ) {
            // عمودان: الداخل يحمل حشو الشاشة والمحتوى، والخارج يملك الشريط السفلي وحده —
            // فيصل الشريط «الملتصق» إلى حافة الهاتف في المعاينة تمامًا كما يصل إليها في التطبيق.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                ) {
                // Camera notch pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 28.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(cs.outlineVariant.copy(alpha = 0.6f))
                )

                // Status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 30.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cs.onSurface.copy(alpha = 0.25f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(cs.onSurface.copy(alpha = 0.25f))
                            )
                        }
                    }
                }

                // Banner / hero strip — **والنمط العلويّ يُرى هنا أيضًا**: المعاينة وعدٌ يُصدَّق
                // بنقرة. الكلاسيكيّ بلمعته وارتفاعه؛ والثلاثة الباقية صفٌّ ثابت: «أيقونة» بمربّع
                // الأيقونة قبل العنوان (بدل الرجوع)، و«العنوان» بسطر العنوان وحده، و«شفّاف» بلا
                // سطح معتم — تدرّج يذوب إلى الشفّاف ودائرة الرجوع قبل العنوان.
                Surface(
                    modifier = Modifier.fillMaxWidth().height(previewHeroHeight(topBarStyle)),
                    shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
                    color = if (topBarStyle.overlaysContent) {
                        Color.Transparent
                    } else {
                        cs.secondaryContainer.copy(alpha = 0.65f)
                    }
                ) {
                    Box(
                        contentAlignment = if (topBarStyle.compactBar) {
                            Alignment.CenterStart
                        } else {
                            Alignment.BottomStart
                        }
                    ) {
                        if (topBarStyle == TopBarStyle.CLASSIC) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                cs.primary.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        if (topBarStyle.overlaysContent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                cs.secondaryContainer.copy(alpha = 0.9f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Row(
                            modifier = Modifier.padding(
                                start = 8.dp,
                                bottom = if (topBarStyle.compactBar) 0.dp else 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (topBarStyle.showsGlyph && topBarStyle.compactBar) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(maxRadius(MaxRadius.control) / 3))
                                        .background(cs.primary.copy(alpha = 0.45f))
                                )
                                Spacer(Modifier.width(6.dp))
                            } else if (topBarStyle.showsBack && topBarStyle.compactBar) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(cs.onSecondaryContainer.copy(alpha = 0.3f))
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cs.onSecondaryContainer.copy(alpha = 0.55f))
                            )
                        }
                    }
                }

                // Two info tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(cs.primaryContainer, cs.secondaryContainer).forEach { tileColor ->
                        Surface(
                            modifier = Modifier.weight(1f).height(44.dp),
                            color = tileColor,
                            shape = RoundedCornerShape(maxRadius(MaxRadius.control))
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 20.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(cs.onPrimaryContainer.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }

                // Settings-like list block — **المعاينة تكشف محورَي §٦-ج بلمسة**: القشرة وحدها
                // (حدّ/خلفية) بحسب `cardStyle`، وارتفاع الكتلة وحشوها بحسب `density`. وهي معاينة
                // رمزية بنفس الأعراف (نفس الرمز والحدّ)، لا مكوّن حقيقيّ — فالمكوّن الحقيقي
                // يقرأ المحورين من السمة الجارية عند عرضه فعلًا.
                val previewBlockPadding = 8.dp * density.scale
                val previewBlockHeight = 80.dp * density.scale
                Surface(
                    modifier = Modifier.fillMaxWidth().height(previewBlockHeight),
                    color = when (cardStyle) {
                        MaxCardStyle.OUTLINED -> cs.surfaceColorAtElevation(2.dp)
                        MaxCardStyle.FILLED -> cs.surfaceContainerHigh
                        MaxCardStyle.FLAT -> Color.Transparent
                    },
                    shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
                    border = if (cardStyle == MaxCardStyle.OUTLINED) {
                        BorderStroke(MaxCardSpec.borderWidth, cs.outlineVariant.copy(alpha = MaxAlpha.border))
                    } else {
                        null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(previewBlockPadding),
                        verticalArrangement = Arrangement.spacedBy(6.dp * density.scale)
                    ) {
                        repeat(3) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cs.primary.copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cs.onSurface.copy(alpha = 0.18f))
                                )
                                if (row == 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 9.dp)
                                            // الرقم 5 لم يكن على سلّم 4dp ولا في أي عقد؛ أقرب المرام إلى نيّته.
                                            .clip(RoundedCornerShape(MaxSpace.xs))
                                            .background(cs.primary.copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }

                }

                // Bottom nav bar — الشكل نفسه الجاري في التطبيق، بأيّ نمط من الخمسة: المعاينة وعدٌ
                // يُصدَّق بنقرة، فلا يُعرض نمطٌ لم يُختر ولا يغيب نمطٌ اختير. والقواعد تُقرأ من
                // `navStyle` نفسه (عائم · نصّ · تمدّد · حجم الأيقونة) فلا تُكتب مرّتين ثم تتباعد.
                val dockedBar = !navStyle.floating
                val pagePlane = navStyle.pagePlane
                val activeWeight = if (navStyle.expandsActive) 1.9f else 1f
                val iconRatio = navStyle.iconSize.value / 23f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (dockedBar) {
                                Modifier
                            } else {
                                Modifier.padding(
                                    horizontal = if (pagePlane) MaxSpace.gutter else MaxSpace.md,
                                    vertical = MaxSpace.sm
                                )
                            }
                        )
                        .clip(
                            if (dockedBar) {
                                RoundedCornerShape(
                                    topStart = maxRadius(MaxRadius.row),
                                    topEnd = maxRadius(MaxRadius.row)
                                )
                            } else {
                                RoundedCornerShape(
                                    maxRadius(if (pagePlane) MaxRadius.group else MaxRadius.pill)
                                )
                            }
                        )
                        .background(
                            if (pagePlane) cs.surfaceContainerHigh else cs.surfaceColorAtElevation(3.dp)
                        )
                        .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm)
                        .heightIn(min = navStyle.itemMinHeight / 2),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        val active = i == 0
                        val showLabel = when (navStyle.labels) {
                            NavLabelMode.ALWAYS -> true
                            NavLabelMode.ACTIVE_ONLY -> active
                            NavLabelMode.NONE -> false
                        }
                        Column(
                            modifier = Modifier.weight(if (active) activeWeight else 1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs, Alignment.CenterVertically)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp * iconRatio)
                                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                                    .background(
                                        if (active) cs.primaryContainer
                                        else cs.onSurface.copy(alpha = 0.15f)
                                    )
                            )
                            if (showLabel) {
                                // سطر النصّ في المعاينة: شريطٌ قصير تحت الأيقونة — يكفي ليقول
                                // «هذا النمط يكتب النصّ» أو «لا يكتبه»، وهو الفرق المرئيّ المطلوب.
                                Box(
                                    modifier = Modifier
                                        .size(width = 20.dp, height = 3.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) cs.onPrimaryContainer.copy(alpha = 0.7f)
                                            else cs.onSurface.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Individual color swatch ──────────────────────────────────────────────────
@Composable
internal fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    colorSpec: ColorSpec.SpecVersion,
    label: String = "",
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val targetScheme = if (color == Color.Unspecified) {
        val base = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else lightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = base.primary, isDark = isDark, specVersion = colorSpec,
            primary = base.primary, secondary = base.secondary, tertiary = base.tertiary,
            neutral = base.surface, neutralVariant = base.surfaceVariant, error = base.error
        )
    } else {
        rememberDynamicColorScheme(seedColor = color, isDark = isDark, specVersion = colorSpec)
    }

    val cs = animateColorSchemeAsState(targetScheme)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "swatch_scale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "swatch_border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(maxRadius(MaxRadius.group)),
            color = cs.surfaceContainer,
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .border(
                    width = 2.dp,
                    color = cs.primary.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(maxRadius(MaxRadius.group))
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Dual-tone arc
                Canvas(modifier = Modifier.size(42.dp)) {
                    drawArc(
                        color = cs.primaryContainer,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = cs.tertiaryContainer,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }

                // Check badge or dot
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit = fadeOut() + scaleOut(targetScale = 0.7f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(cs.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = cs.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit = fadeOut() + scaleOut(targetScale = 0.7f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(cs.primary)
                    )
                }
            }
        }

        // Label — shown only for the "Auto" swatch
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
