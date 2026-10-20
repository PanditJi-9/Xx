/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * استوديو الصوت — **سطح التحكّم** (`AU-03` + `AU-05` + `AQ-02`…`AQ-09`): ثلاثة تبويبات، وبفرقٍ واحد
 * هو كلّ الفرق: **كل مقبض هنا يكتب فعلًا**، وكل ما لا يُكتب يُسمّى بسببه.
 *
 * ───────────────────── إعادة التصميم (طلب المالك: «تجربة الاستخدام صعبة والواجهة سيئة») ─────────────────────
 * **والعطب كان بنيويًّا لا ذوقًا**، وثلاثة أرقام تقوله:
 *
 * ① **اختيار التبويب كان داخل التمرير.** الشاشة كانت `عمود ← بطاقة محرّك ← شريط تبويبات ← الأقسام`،
 *    وقسم المحرّك وحده ٧٩٣ سطرًا. فمن نزل ليضبط معادلًا لم يبقَ أمامه إلّا أن يعود إلى أعلى الصفحة
 *    ليبدّل تبويبًا — أي أنّ التبويب الآخر كان **يحتاج تمريرًا كاملًا**. والشريط الآن خارج التمرير
 *    ([AudioTabBar] في `Box` فوق الشاشة) فلا يفارق العين أبدًا.
 * ② **أوّل ما يُرى لم يكن له علاقة بالصوت**: عنوانٌ ورقمان ثمّ قائمة. والآن أوّله **بطاقة البطل**
 *    ([AudioHeroCard]): شكلٌ من قراءةٍ حقيقيّة، والجهاز النشط، وحالتا المحرّك والالتقاط، وثلاثة أرقام.
 * ③ **المعادل كان عشرة أشرطة وقراءة**: تضبط نطاقًا في السابعة من عشرة فتفقد صورة المجموع. والمنحنى
 *    الآن **يُسحب** ([AudioEqSection]) فيُكتب نطاقه ويُقرأ — والمنحنى نفسه يُرى في البطل.
 *
 * **والتبويبات الثلاثة حدودٌ لا ذوق:** «مستويات» ما يكتبه الطريق الوحيد القائم (دفقات)، و«محرّك»
 * المؤثّرات (معادل · ديناميكيّ · بسيطة · مازج)، و«نظام» ما يُقاس ويُخزَّن (مصفوفة القدرات · الطبقة
 * النظاميّة · التوجيه · الطيف · البصمات). و«ثلاثة» حدٌّ لا اختيار: الشريط يسع ثلاثةً بأسماءٍ عربيّة
 * غير مبثورة على شاشةٍ ضيّقة.
 *
 * **ولا عرضَ جردٍ هنا:** أجهزة الإخراج والمؤثرات المُعلَنة في «معلومات الجهاز» — فلهذه الشاشة بابٌ واحد
 * إليه (`MaxDeviceInfoShortcut`)، فلا تُعرض الحقيقة الواحدة في موضعين.
 *
 * **والكتابة تمرّ بالـViewModel لا من هنا (ADR-11):** حزمة `ui/` لا تكتب ولا تلمس `AudioEffect`؛
 * الشاشة تنادي `viewModel.*`، وهو يمرّرها إلى المحرّك ← `HardwareControlArbiter`، ثم تُقرأ.
 *
 * **وثلاث حالات لا رابع:** قراءة (شريط حيّ) · غياب قراءة (`—` وشريط مُعطَّل بسببه) · كتابة لم تُطبَّق
 * (سطر بسبب مكتوب، لا `applied=true` كاذبة).
 *
 * **تعديل 2026-10-12 — بطلب المالك (أقلّ، وأوضح، وأسهل سحبًا):**
 * • **أُخفي** ما لا يضبط المستخدم صوته به: مقابض مؤثّر المصنّع، والتحكّم المتقدّم (قيادة المحرّك
 *   والديناميكا والمازج)، وقدرات المحرّك، وسلّم المحرّكات، واستعادة الضبط. **ولم يُحذف منها شيء:** كل
 *   واحد خلف مفتاح إخفاء أعلى الملف، فإعادته قلبُ مفتاحٍ لا إعادةُ كتابة.
 * • **تقطّع المعادل كان في شيفرته:** مفتاح إيماءته كان قائمة النطاقات نفسها، وكل خطوة ديسيبل تُغيّرها،
 *   فتُعاد تشغيل الإيماءة في منتصف السحب — فينقطع السحب ويأخذه التمرير. الآن تُقرأ القيم من مرجعٍ
 *   متحرّك، والإيماءة تُشغَّل مرّة واحدة لكل لمسة.
 * • **والتمرير يتوقّف طوال السحب** (`scrollEnabled` في `MaxScreen`)، لا الإيماءة وحدها.
 * • **التبويب الأوّل سُمّي «المعادل»** بدل «المحرّك»، لأن اسم المحرّك لم يعد يُعرض في هذه الشاشة.
 */
package nd.max.ui.subscreens.audio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nd.max.R
import nd.max.core.audio.AudioBackendId
import nd.max.core.audio.AudioDeviceCatalog
import nd.max.core.audio.AudioEffectKind
import nd.max.core.audio.MaxFxModel
import nd.max.core.audio.AudioWriteOutcome
import nd.max.core.audio.audioBackendRoleOf
import nd.max.core.audio.audioDbFormat
import nd.max.core.audio.AudioSessionTracker
import nd.max.core.audio.AudioSoundPreset
import nd.max.service.AudioEffectService
import nd.max.ui.component.CyberAudioVortex
import nd.max.ui.component.CyberCurveEqualizer
import nd.max.ui.component.CyberRotaryKnob
import nd.max.ui.component.MaxDeviceInfoShortcut
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.viewmodel.AudioStudioUiState
import nd.max.ui.viewmodel.AudioStudioViewModel

/*
 * مفاتيح الإخفاء — طلب المالك (2026-10-12). كلها `false`، فلا يُعرض شيءٌ منها على الشاشة.
 * كودها باقٍ في مكانه ولم يُحذف، فإعادة أيٍّ منها قلب مفتاحه إلى `true` لا إعادة كتابة.
 */
private const val SHOW_FACTORY_KNOBS = false        // مقابض مؤثّر المصنّع (VendorEffectSection)
private const val SHOW_ADVANCED_CONTROLS = false    // التحكّم المتقدّم: قيادة المحرّك · الديناميكا · المازج
private const val SHOW_ENGINE_CAPABILITIES = false  // قدرات المحرّك (مصفوفة القدرات)
private const val SHOW_ENGINE_LADDER = false        // سلّم المحرّكات: أيّ محرّك يقود الصوت الآن
private const val SHOW_RESTORE_DEFAULTS = false     // استعادة الضبط الافتراضي

@Composable
fun AudioStudioScreen(
    navController: NavController,
    // `hiltViewModel()` لا `viewModel()`: لهذا الـViewModel مُنشئ بوسائط (المحكِّم)، و`viewModel()`
    // بلا مصنع ينادي مُنشئًا بلا وسائط — فيخرج التطبيق لحظة فتح الشاشة (نفس نصّ `GpuStudioScreen`).
    viewModel: AudioStudioViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.state
    val title = stringResource(R.string.max_audio_studio_title)

    LaunchedEffect(Unit) { viewModel.load(context) }

    // تدفّق حيّ: وصل جهازٌ أو نُزع (سمّاعة · بلوتوث) ⇒ تُعاد القراءة. والإلغاء في `onDispose`.
    DisposableEffect(context) {
        val stop = AudioDeviceCatalog.observeDevices(context) { viewModel.load(context) }
        onDispose { stop?.invoke() }
    }

    // والالتقاط يتوقّف عند مغادرة الشاشة — فلا يبقى مؤشّر تسجيل مضاءً ولا معالجةٌ بلا مستخدم.
    DisposableEffect(Unit) { onDispose { viewModel.stopSpectrum() } }

    // وتصدير البصمات يُنسخ من الشاشة (شأن واجهة) — والمخزن يُعطي النصّ في الحالة ثمّ يُفرَّغ.
    val exported = state.profilesExport
    LaunchedEffect(exported) {
        if (exported.isNullOrEmpty()) return@LaunchedEffect
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("maxmanager-audio-profiles", exported))
        viewModel.consumeExport()
    }

    // **والتبويب يُحفظ باسمه لا بكائن التعداد:** `rememberSaveable` يحفظ أنواعًا أساسيّة، فالاسم هو
    // الطريق، والرجوع إلى «مستويات» عند اسمٍ غريب لا يترك الشاشة بلا تبويب.
    // **والتبويب الأوّل هو المحرّك** لا المستويات: ما يُطلب من هذه الشاشة هو المنحنى والأنماط
    // ومؤثّر المصنّع، والمستويات تحكّمٌ عاديّ يُطلب مرّةً في الشهر — فلا يُقدَّم ما يُطلب أقلّ.
    var tabName by rememberSaveable { mutableStateOf(AudioTab.Engine.name) }
    val selected = remember(tabName) { AudioTab.entries.firstOrNull { it.name == tabName } ?: AudioTab.Engine }

    // قفل التمرير أثناء سحب نطاقٍ من المعادل أو مقبضٍ: الشاشة لا تتحرّك تحت إصبعٍ يسحب شيئًا آخر.
    var interactionLocked by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        MaxScreen(
            // و`fillMaxSize` صريحة لا ضمنيّة: الشاشة هنا داخل `Box` لأن الشريط العائم يُرسم **فوقها**
            // لا داخلها (ولو كان داخل التمرير لفارق العين مع أوّل تمرير — وهو أصل العطب).
            modifier = Modifier.fillMaxSize(),
            // قفل التمرير لحظة سحب نطاقٍ أو مقبض (انظر `interactionLocked`).
            scrollEnabled = !interactionLocked,
            title = title,
            onBack = { navController.popBackStack() },
            subtitle = stringResource(R.string.max_audio_studio_subtitle),
            accentIcon = Icons.AutoMirrored.Rounded.VolumeUp,
            condition = if (state.loading) {
                MaxCondition(
                    kind = MaxConditionKind.Loading,
                    title = title,
                    detail = stringResource(R.string.max_audio_studio_loading),
                )
            } else {
                null
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.section),
            ) {
                // **وحكم آخر مقبض يُعرض في الشاشة لا في تبويب:** المقابض تُكتب من التبويبات الثلاثة
                // (معادل · ديناميكيّ · مازج · توجيه · طبقة نظاميّة)، فإخفاؤه داخل تبويبٍ واحد يعني أن
                // من كتب من تبويبٍ آخر لا يرى حكمَ كتابته أصلًا.
                KnobVerdictNotice(state)

                when (selected) {
                    AudioTab.Levels -> AudioLiveTab(
                        state = state,
                        onCommit = { token, level -> viewModel.apply(context, token, level) },
                        onCaptureHint = { tabName = AudioTab.System.name },
                    )

                    AudioTab.Engine -> AudioEngineTab(
                        state = state,
                        viewModel = viewModel,
                        context = context,
                        onInteractionChange = { interactionLocked = it },
                    )

                    AudioTab.System -> AudioSystemTab(navController, state, viewModel, context)
                }

                // ويُحجَز للشريط العائم قدرُه المُعلَن، فلا يقف آخر صفٍّ تحته (الرمز واحد يُشار إليه).
                Spacer(Modifier.height(audioTabBarReserve))
            }
        }

        AudioTabBar(
            selected = selected,
            onSelect = { tabName = it.name },
            engineActive = engineActive(state),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = MaxSpace.gutter, vertical = MaxSpace.sm),
        )
    }
}

/**
 * هل المحرّك يعمل الآن؟ — **سؤالٌ له جوابٌ مقروء لا تقدير**.
 *
 * والجواب: مؤثّرٌ واحد على الأقلّ **مُمكَّن فعلًا** كما قرأته المنصّة (`enabled` تُقرأ بعد كل كتابة،
 * و`null` فيها تعني «لم تُقرأ» فلا تُحسب عملًا)، **أو** التقاطٌ جارٍ يغذّي الشاشة بقراءة. ومع غياب
 * الاثنين تسكن أعمدة تبويب المحرّك — فلا أيقونة تنبض بلا قراءة تسندها.
 */
private fun engineActive(state: AudioStudioUiState): Boolean =
    state.spectrumRunning || state.enabled.values.any { it == true } || AudioEffectService.masterEnabled

/**
 * `LIVE` — **أُعيد بناؤه من الصفر**: بطاقة المخرج ([AudioHeroCard])، ثم بطاقة واحدة لكل الدفقات
 * ([AudioStreamLevelsCard]) بدل قسمٍ قابلٍ للطيّ فيه صفوفٌ متطابقة.
 *
 * والكتابة عند إنهاء السحب لا مع كل بكسل. ودفقٌ لا قراءة له يُعرض مُعطَّلًا **بسببه**، لا بشريطٍ يقبل
 * سحبًا لا يصل. والديسيبل من المنصّة (`getStreamVolumeDb`) — وهي الوحدة الصادقة.
 */
@Composable
private fun AudioLiveTab(
    state: AudioStudioUiState,
    onCommit: (String, Int) -> Unit,
    onCaptureHint: () -> Unit,
) {
    AudioHeroCard(state = state, onCaptureHint = onCaptureHint)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        Text(
            text = stringResource(R.string.max_audio_levels_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = MaxSpace.xs),
        )

        val verdict = state.lastVerdict
        if (verdict != null) {
            AudioVerdictNotice(
                outcome = verdict.outcome,
                reason = verdict.reason,
                token = state.lastWrittenToken,
            )
        }

        if (state.readings.isEmpty()) {
            AudioNotice(
                title = stringResource(R.string.max_audio_levels_unreadable),
                description = stringResource(R.string.max_audio_levels_unreadable_desc),
                icon = Icons.Rounded.ErrorOutline,
            )
        } else {
            AudioStreamLevelsCard(
                readings = state.readings,
                dbOf = { token -> audioDbFormat(state.volumes.firstOrNull { it.token == token }?.db) },
                footnote = stringResource(R.string.max_audio_levels_description),
                onCommit = onCommit,
            )
        }
    }
}

/**
 * `ENGINE`: **ما يُضبط به الصوت فعلًا** — زرّ التشغيل الرئيسيّ، ثمّ المعادل بمنحناه، ثمّ مقابض المؤثّرات
 * الأربعة. وما خلفها ممّا لا يحتاجه المستخدم العادي (مؤثّر المصنّع، والتحكّم المتقدّم) مُخفًى بمفاتيحه
 * أعلى الملف، ولم يُحذف.
 *
 * **ترتيبٌ مقصود:** المعادل يلي الزرّ مباشرة، لأنه موضع السحب الذي تُعنى به الشاشة كلّها، وكل تمريرٍ
 * يقع فوقه يُوقَف طوال السحب ([onInteractionChange]).
 */
@Composable
private fun AudioEngineTab(
    state: AudioStudioUiState,
    viewModel: AudioStudioViewModel,
    context: Context,
    onInteractionChange: (Boolean) -> Unit,
) {
    val tracker = remember { AudioSessionTracker.get() }
    val activeSessions by tracker.activeSessions.collectAsState()
    var masterEnabled by remember { mutableStateOf(AudioEffectService.masterEnabled) }
    var eqBands by remember { mutableStateOf(AudioEffectService.manualEqBands) }
    var bassBoost by remember { mutableIntStateOf(AudioEffectService.bassBoostPercent) }
    var virtualizer by remember { mutableIntStateOf(AudioEffectService.virtualizerPercent) }
    var loudnessMb by remember { mutableIntStateOf(AudioEffectService.loudnessGainMb) }
    var vocalClarity by remember { mutableIntStateOf(0) }
    var activePreset by remember { mutableStateOf(AudioEffectService.activePreset) }

    // لوحة القيادة الصوتية السيبرانية (HUD Vortex Reactor) — زرّ التشغيل الرئيسيّ
    CyberAudioVortex(
        enabled = masterEnabled,
        activeSessionsCount = activeSessions.size,
        // الاسم والنوع من **جهاز الإخراج النشط** (له `typeCode` حقيقيّ) — كان النوع يُمرَّر من `id` الجهاز
        // وهو ليس نوعًا، فتبقى الأيقونة مكبّرًا حتى مع السمّاعة.
        outputDeviceName = state.activeDevice?.productName
            ?: state.route?.active?.productName
            ?: stringResource(R.string.max_audio_device_speaker),
        deviceType = state.activeDevice?.typeCode,
        onToggle = {
            val next = !masterEnabled
            masterEnabled = next
            AudioEffectService.toggleMaster(context, next)
        },
    )

    // معادل الترددات بالمنحنى الانسيابي (Cyber Curve Equalizer)
    CyberCurveEqualizer(
        bands = eqBands,
        enabled = masterEnabled,
        activePreset = activePreset,
        onBandChange = { bandIndex, gainDb ->
            val updated = eqBands.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = gainDb
                eqBands = updated
                activePreset = AudioSoundPreset.OFF
                AudioEffectService.updateEqualizerBand(bandIndex, gainDb)
            }
        },
        onPresetSelect = { preset ->
            activePreset = preset
            AudioEffectService.setPreset(context, preset)
            eqBands = AudioEffectService.manualEqBands
            bassBoost = AudioEffectService.bassBoostPercent
            virtualizer = AudioEffectService.virtualizerPercent
            loudnessMb = AudioEffectService.loudnessGainMb
        },
        // أسماء الأنماط من موارد المنصّة المترجمة — لا نصوصٌ إنجليزيّة ثابتة في الواجهة
        presetLabel = { preset -> stringResource(audioPresetLabelRes(preset)) },
        onInteractionChange = onInteractionChange,
    )

    // المقابض الأربعة في شبكةٍ واحدة بمسافةٍ واحدة — كانت صفّين بفاصل قسمٍ كامل بينهما
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            CyberRotaryKnob(
                value = bassBoost,
                range = 0..100,
                title = stringResource(R.string.audio_knob_bass_title),
                unit = "%",
                icon = Icons.Rounded.GraphicEq,
                enabled = masterEnabled,
                onValueChange = {
                    bassBoost = it
                    AudioEffectService.updateBassBoost(it)
                },
                onInteractionChange = onInteractionChange,
                modifier = Modifier.weight(1f),
            )
            CyberRotaryKnob(
                value = virtualizer,
                range = 0..100,
                title = stringResource(R.string.audio_knob_surround_title),
                unit = "%",
                icon = Icons.Rounded.Waves,
                enabled = masterEnabled,
                onValueChange = {
                    virtualizer = it
                    AudioEffectService.updateVirtualizer(it)
                },
                onInteractionChange = onInteractionChange,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            CyberRotaryKnob(
                value = vocalClarity,
                range = 0..100,
                title = stringResource(R.string.audio_knob_clarity_title),
                unit = "%",
                icon = Icons.Rounded.RecordVoiceOver,
                enabled = masterEnabled,
                onValueChange = {
                    vocalClarity = it
                    val gainDb = (it * 12 / 100).coerceIn(-15, 15)
                    AudioEffectService.updateEqualizerBand(5, gainDb)
                    eqBands = AudioEffectService.manualEqBands
                },
                onInteractionChange = onInteractionChange,
                modifier = Modifier.weight(1f),
            )
            CyberRotaryKnob(
                value = (loudnessMb / 100).coerceIn(0, 30),
                range = 0..30,
                title = stringResource(R.string.audio_knob_loudness_title),
                unit = "dB",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                enabled = masterEnabled,
                onValueChange = {
                    val mb = it * 100
                    loudnessMb = mb
                    AudioEffectService.updateLoudness(mb)
                },
                onInteractionChange = onInteractionChange,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // مؤثّر المصنّع العتادي (Dolby Atmos / DAP) — مُخفًى بمفتاح `SHOW_FACTORY_KNOBS`.
    if (SHOW_FACTORY_KNOBS) {
        VendorEffectSection(
            verdict = state.vendor?.verdict,
            probe = state.vendorProbe,
            role = audioBackendRoleOf(AudioBackendId.VENDOR_EFFECT, state.backend),
            open = state.vendorOpen,
            dapEnabled = state.vendorDapEnabled,
            effectEnabled = state.vendorEffectEnabled,
            profile = state.vendorProfile,
            values = state.vendorValues,
            knob = state.vendorKnob,
            onOpen = { viewModel.openVendor() },
            onClose = { viewModel.closeVendor() },
            onDapEnabled = { enabled -> viewModel.setVendorDapEnabled(context, enabled) },
            onEffectEnabled = { enabled -> viewModel.setVendorEffectEnabled(context, enabled) },
            onProfile = { profile -> viewModel.setVendorProfile(context, profile) },
            onValue = { param, values -> viewModel.writeVendorDapValues(context, param, values) },
        )
    }

    // التحكّم المتقدّم (قيادة المحرّك · الديناميكا · المازج) — مُخفًى بمفتاح `SHOW_ADVANCED_CONTROLS`.
    if (SHOW_ADVANCED_CONTROLS) {
        MaxSection(
            title = stringResource(R.string.audio_presets_advanced),
            description = stringResource(R.string.audio_presets_advanced_note),
            collapsible = true,
            initiallyExpanded = false,
        ) {
            AudioEngineLeadGroup(backend = state.backend, ladder = state.ladder)
            AudioDynamicsSection(
                snapshot = state.dynamics,
                reason = state.engineReasons[AudioEffectKind.DYNAMICS],
                onEqGain = { stage, index, gain -> viewModel.writeEqBandGain(context, stage, index, gain) },
                onEqCutoff = { stage, index, hz -> viewModel.writeEqBandCutoff(context, stage, index, hz) },
                onMbc = { index, param, value -> viewModel.writeMbcParam(context, index, param, value) },
                onLimiter = { param, value -> viewModel.writeLimiterParam(context, param, value) },
                onBalance = { left, right -> viewModel.writeBalance(context, left, right) },
            )
            AudioMixerSection(
                snapshot = state.mixer,
                onRequest = { attribute -> viewModel.requestMixer(context, attribute) },
                onClear = { viewModel.clearMixer(context) },
            )
        }
    }
}

/**
 * `SYSTEM`: الجلسات النشطة، وجهاز الإخراج، وإحالةٌ إلى معلومات الجهاز.
 *
 * ما كان هنا من قدرات المحرّك وسلّمه، واستعادة الضبط، **مُخفًى بمفاتيحه أعلى الملف** — والحدّ هنا أن
 * الشاشة تُري المستخدم ما يخصّ صوته، لا ما يخصّ المحرّك الذي خلفه.
 */
@Composable
private fun AudioSystemTab(
    navController: NavController,
    state: AudioStudioUiState,
    viewModel: AudioStudioViewModel,
    context: Context,
) {
    AudioActiveSessionsSection()
    AudioDeviceProfileSection(snapshot = state.route, latency = state.latency)
    AudioLatencySection(reading = state.latency)
    if (SHOW_ENGINE_CAPABILITIES) AudioCapabilitySection(state.capability)
    if (SHOW_ENGINE_LADDER) {
        AudioBackendLadderSection(
            vendor = state.vendor,
            probe = state.vendorProbe,
            ladder = state.ladder,
            backend = state.backend,
            onRemeasure = { viewModel.remeasureBackends(context) },
        )
    }
    if (SHOW_RESTORE_DEFAULTS) AudioResetSection(context = context)
    MaxDeviceInfoShortcut(navController = navController, from = MaxDestination.AudioStudio)
}

/** حكم مقبض المحرّك سطرًا واحدًا — وسببُه مكتوب حين لم يُطبَّق. */
@Composable
private fun KnobVerdictNotice(state: AudioStudioUiState) {
    val verdict = state.knobVerdict ?: return
    val target = knobTargetText(state.knobTarget)
        ?.let { stringResource(it) }
        ?: stringResource(R.string.max_audio_knob_generic)
    val reason = verdict.reason?.let { engineReasonText(it) }
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.max_audio_write_reason_none)
    val text = when (verdict.outcome) {
        AudioWriteOutcome.APPLIED -> stringResource(R.string.max_audio_knob_applied, target)
        AudioWriteOutcome.BLOCKED -> stringResource(R.string.max_audio_knob_blocked, target, reason)
        AudioWriteOutcome.FAILED -> stringResource(R.string.max_audio_knob_failed, target, reason)
        AudioWriteOutcome.NOT_ATTEMPTED -> stringResource(R.string.max_audio_knob_not_attempted, target, reason)
    }
    AudioNotice(
        title = text,
        description = listOfNotNull(verdict.expected, verdict.actual).joinToString("  →  "),
        icon = if (verdict.isApplied) Icons.Rounded.GraphicEq else Icons.Rounded.ErrorOutline,
    )
}

/** حكم الكتابة سطرًا واحدًا — وسببُه مكتوب حين لم تُطبَّق. */
@Composable
private fun AudioVerdictNotice(outcome: AudioWriteOutcome, reason: String?, token: String?) {
    val titleRes = when (outcome) {
        AudioWriteOutcome.APPLIED -> R.string.max_audio_write_applied
        AudioWriteOutcome.BLOCKED -> R.string.max_audio_write_blocked
        AudioWriteOutcome.FAILED -> R.string.max_audio_write_failed
        AudioWriteOutcome.NOT_ATTEMPTED -> R.string.max_audio_write_not_attempted
    }
    val stream = token?.let { stringResource(streamTitle(it)) }
        ?: stringResource(R.string.max_audio_stream_unknown)
    AudioNotice(
        title = stringResource(titleRes, stream),
        // والسبب يأتي من المحكِّم حرفيًّا: `manual-lock` · `apply-not-verified-baseline-restored`.
        description = reason?.let { engineReasonText(it) }
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.max_audio_write_reason_none),
        icon = if (outcome == AudioWriteOutcome.APPLIED) {
            Icons.Rounded.GraphicEq
        } else {
            Icons.Rounded.ErrorOutline
        },
    )
}

@Composable
private fun AudioNotice(title: String, description: String, icon: ImageVector) {
    MaxGroup {
        MaxRow(title = title, subtitle = description, icon = icon)
    }
}

/** رمز الدفق ← نصّه في الموارد (غير `@Composable`: جدول ثابت لا يرسم). */
internal fun streamTitle(token: String): Int = when (token) {
    "media" -> R.string.max_audio_stream_media
    "call" -> R.string.max_audio_stream_call
    "ring" -> R.string.max_audio_stream_ring
    "notification" -> R.string.max_audio_stream_notification
    "alarm" -> R.string.max_audio_stream_alarm
    "system" -> R.string.max_audio_stream_system
    else -> R.string.max_audio_stream_unknown
}

/**
 * **يُعلن مجلّد مكتبات التطبيق وعمود المعالج** (مخلَصا خطّة المكتبة) قبل ضغط زرّ التثبيت.
 *
 * **ولماذا يُقرأ من `applicationInfo.nativeLibraryDir` لا من مسار مكتوب:** التطبيق يعيش تحت مسارٍ
 * يتغيّر مع كل تحديث (`/data/app/~~<عشوائيّ>/nd.max-<عشوائيّ>/…`)، فمسارٌ مثبَّت عندنا يكون صحيحًا
 * اليوم وخطأً بعد أوّل تحديث — **والمصنّع يقرأه من المنصّة لا من ذاكرتنا**.
 *
 * ولماذا هذه الطبقة (الواجهة) لا الـViewModel: المجلّد والعمود **سياقُ جهاز**، والـViewModel يعمل بلا
 * `android.content` عمدًا، فتبقى الخلفيّة قابلة للقياس بمعزل عن المنصّة.
 */
private fun declareEffectLibrary(viewModel: AudioStudioViewModel, context: Context) {
    val info = context.applicationInfo
    viewModel.declareEffectLibrary(
        // ‏`sourceDir` هو الحزمة التي **فيها** مدخل `lib/<abi>/<اسم>.so` — تُقرأ منها لأنّ
        // `nativeLibraryDir` لا يكون مجلّدًا حقيقيًّا حين `extractNativeLibs=false`.
        apkPath = info.sourceDir ?: info.publicSourceDir,
        nativeLibraryDir = info.nativeLibraryDir,
        // ويُخرج الملفّ إلى مجلّد التطبيق، **والجذر يقرأ ما لا يقرؤه غيره** (`su` بـ`uid 0`).
        stagingDir = context.filesDir?.absolutePath,
        abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
    )
}
