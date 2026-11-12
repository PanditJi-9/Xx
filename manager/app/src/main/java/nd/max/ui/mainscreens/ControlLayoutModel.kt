/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * The Control page's layout model.
 *
 * The page has two presentations of the same content — a compact grouped list and
 * an expanded page that opens every screen out — and the requirement on it is
 * that they can never drift: adding or removing a destination updates both, with
 * nothing to remember twice.
 *
 * That is why this file exists: both layouts read this one model, and the model
 * itself is derived from [MaxDestination] (the registry) plus
 * [maxDestinationRole] (the role table). Only the editorial part — which hubs
 * belong to which band of the page, and in what order — is written down here.
 *
 * Pure Kotlin on purpose: no Compose imports, so the sync contract is testable in
 * a plain JVM unit test (see ControlLayoutModelTest).
 */
package nd.max.ui.mainscreens

import androidx.annotation.StringRes
import nd.max.R
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.navigation.maxDestinationRole
import nd.max.ui.navigation.maxHubDescription
import nd.max.ui.navigation.maxHubRows

/** A destination plus the one line that explains it on this page. */
data class ControlEntry(val destination: MaxDestination, @StringRes val subtitleRes: Int)

/** A domain hub together with every screen it owns. */
data class ControlHubSpec(
    val hub: MaxDestination,
    @StringRes val hubSubtitleRes: Int,
    val features: List<ControlEntry>,
)

/** One titled band of the page. */
data class ControlGroupSpec(
    val key: String,
    @StringRes val titleRes: Int,
    val hubs: List<ControlHubSpec>,
)

/**
 * The gated toolbox: not a domain, so it is not part of the bands above.
 *
 * `Max Backup` و`AppOps` انتقلا إلى هنا من شاشة التطبيقات بقرار مالك: كلاهما أداة تفتح
 * على **كل** التطبيقات ولها شاشة رئيسية مستقلة، فلا معنى لعرضها داخل شاشة تطبيق واحد.
 * والترتيب مقصود: الأدوات التي تُفتح بلا نيّة سابقة (نسخ · صلاحيات) قبل أدوات النظام.
 *
 * وكانت مراقب المهام ووحدة السجل وطرفية الأوامر تُطلب من **مسار التشخيص في الإعدادات**، فنُقلت
 * إلى هنا لأن الأدوات تُطلب من مكان واحد، والتنقّل إليها كان يشترط أن يُعرف أنها تحت الإعدادات
 * أو تحت التشخيص — شرط لا معنى له من جهة المستخدم.
 *
 * ثمّ فرّقت ثلاث دفعات صغيرة ما جُمع (M1a ردّت مراقب المهام إلى محور الذاكرة، وM1b رُفضت لأنها
 * كانت ستجعل المختبر صندوق أدوات وهو **اختبارُ عتاد**، وM1c ردّت وحدة السجل والنسخ والصلاحيات
 * والألوان إلى التشخيص والإعدادات) — **ثم نسخها أمر المالك في تكملة ٨٥:** «انقل … إلى شاشة
 * التحكم»، فعادت هذه الأدوات إلى هنا، وصار الباب واحدًا بعد أن حُذفت شاشة التشخيص نفسها.
 * وما بقي خارج الصفحة أداةٌ لها محورها: مدير الملفات في `StorageHub`، ومراقب المهام في
 * `MemoryHub`، وسجلّ البطارية وشحن البايباس في `PowerHub`.
 *
 * وأما محرّر الإعدادات ومشغّل الأنشطة فبقيا هنا بعد أن جُرِّبت إحالتهما إلى **مختبر الجهاز**
 * ورُفضت: فمن يفتح المختبر ليجرب اللمس والسماعة لا يُعرض عليه محرّر إعدادات.
 *
 * و**شاشة الأداء** (`MaxDestination.FpsOverlay`) دخلت هنا بأمر مالك: كانت في `ResponsivenessHub`
 * فتُقرأ على أنها «ضبط معدل الإطارات» وهو `FpsGo` نفسه. وموضعها هنا بعد أدوات النظام: هي طبقة
 * تُرى فوق التطبيقات لا إعداد يُضبط داخل مجال، والقارئ يطلبها ليعرف لا ليضبط.
 *
 * و**الجزيرة الديناميكية** (`MaxDestination.DynamicIsland`) جارتها في الموضع نفسه وللسبب نفسه:
 * تراكبٌ فوق التطبيقات يُفتح لتُرى حالته، لا ضبطٌ داخل مجال.
 */
val ControlToolDestinations: List<MaxDestination> = listOf(
    MaxDestination.FpsOverlay,
    MaxDestination.GameSessions,
    MaxDestination.DynamicIsland,
    MaxDestination.SetEdit,
    MaxDestination.ActivityLauncher,
    // أدوات التشخيص بقيّة: شاشة التشخيص حُذفت بأمر المالك، فبقيت أدواتها هنا.
    MaxDestination.DeviceLab,
    MaxDestination.UsageStats,
    MaxDestination.Logs,
    MaxDestination.MaxBackup,
    MaxDestination.Permissions,
    MaxDestination.DebloatFreeze,
    MaxDestination.ColorPalette,
    MaxDestination.ColorScheme,
    // الأخطف آخرًا: لا يُفتح بلمسة عابرة.
)

/** Which hubs belong to which band, and in what order. Editorial, nothing more. */
private class ControlBand(@StringRes val titleRes: Int, val hubs: List<MaxDestination>)

private val ControlBands = listOf(
    ControlBand(
        R.string.control_group_performance,
        listOf(
            MaxDestination.CpuHub,
            MaxDestination.GpuHub,
            MaxDestination.MemoryHub,
            MaxDestination.ResponsivenessHub,
        ),
    ),
    ControlBand(
        R.string.control_group_environment,
        listOf(
            MaxDestination.ThermalHub,
            MaxDestination.PowerHub,
            MaxDestination.DisplayHub,
        ),
    ),
    ControlBand(
        R.string.control_group_system,
        listOf(
            MaxDestination.StorageHub,
            MaxDestination.NetworkHub,
            // والعقدة العاشرة (`AU-01`): الصوت عالم نظام — يُقرأ مع الشبكة والتخزين لا مع التطبيقات.
            MaxDestination.AudioHub,
        ),
    ),
)

/**
 * The single description of the page, consumed by both layouts.
 *
 * A band with no hubs left is dropped rather than rendered empty, so removing the
 * last hub of a band reads as a layout change and not as a blank section.
 */
fun controlLayoutModel(access: nd.max.ui.navigation.DestinationAccess? = null): List<ControlGroupSpec> = ControlBands.mapNotNull { band ->
    val hubs = band.hubs.filter { access == null || access.visible(it) }.map { hub ->
        ControlHubSpec(
            hub = hub,
            hubSubtitleRes = maxHubDescription(hub),
            features = maxHubRows(hub, access).map { feature -> ControlEntry(feature, maxDestinationRole(feature)) },
        )
    }
    ControlGroupSpec(
        key = hubKey(band.titleRes),
        titleRes = band.titleRes,
        hubs = hubs,
    ).takeIf { hubs.isNotEmpty() }
}

/** Rows of the advanced-tools band, from the same role table as everything else. */
fun controlToolEntries(access: nd.max.ui.navigation.DestinationAccess? = null): List<ControlEntry> =
    ControlToolDestinations.filter { access == null || access.visible(it) }
        .map { destination -> ControlEntry(destination, maxDestinationRole(destination)) }

/**
 * **نطاقٌ في صندوق الأدوات — عنوانٌ يجمع صفوفًا تجمعها وظيفة واحدة.**
 *
 * الصندوق كان عشرة صفوف متتالية بلا جامع: من يفتح الصفحة ليرى أسطحها الحيّة يمرّ على
 * محرّر إعدادات ومدير ملفات قبل أن يجدها. وهذه النطاقات **لا تنقل وجهةً من موضعها** —
 * بل تقطع القائمة الطويلة إلى ثلاث قراءات: ما يُشاهد، وما يُحفظ ويُفتح، وما يُعدَّل.
 */
data class ControlToolGroup(
    val key: String,
    @StringRes val titleRes: Int,
    val entries: List<ControlEntry>,
)

/**
 * ترتيب النطاقات وأعضاؤها. والأعضاء **أسماء من السجل**، والصفوف نفسها تُشتقّ من
 * [controlToolEntries] — فبوابة الرؤية وجدول الأدوار يبقيان مصدرًا واحدًا، ولا تُكتب
 * الوجهة مرتين لتظهر في الصفحة.
 */
private val ControlToolBands: List<Pair<Int, List<MaxDestination>>> = listOf(
    R.string.control_tools_live to listOf(
        MaxDestination.FpsOverlay,
        MaxDestination.GameSessions,
        MaxDestination.DynamicIsland,
    ),
    R.string.control_tools_advanced to listOf(
        MaxDestination.SetEdit,
        MaxDestination.ActivityLauncher,
    ),
    // أدوات النظام: ما يقرأ الجهاز ويُدير تطبيقاته وشحنه — لا يُخفى خلف «متقدّم».
    R.string.control_tools_system to listOf(
        MaxDestination.DeviceLab,
        MaxDestination.UsageStats,
        MaxDestination.Logs,
        MaxDestination.Permissions,
        MaxDestination.MaxBackup,
        MaxDestination.DebloatFreeze,
    ),
    R.string.control_tools_appearance to listOf(
        MaxDestination.ColorPalette,
        MaxDestination.ColorScheme,
    ),
)

/** النطاقان الباقيان، مرشَّحان بنفس بوابة الصفحة، ونطاقٌ فرغ من صفوفه يُسقَط لا يُعرض فارغًا. */
fun controlToolGroups(access: nd.max.ui.navigation.DestinationAccess? = null): List<ControlToolGroup> {
    val entries = controlToolEntries(access)
    return ControlToolBands.mapNotNull { (titleRes, destinations) ->
        val rows = entries.filter { entry -> entry.destination in destinations }
        ControlToolGroup(
            key = "control_tools_$titleRes",
            titleRes = titleRes,
            entries = rows,
        ).takeIf { rows.isNotEmpty() }
    }
}

/**
 * Stable list key: the band's own title resource, so reordering bands does not
 * recycle the wrong item state in either layout.
 */
private fun hubKey(@StringRes titleRes: Int): String = "control_band_$titleRes"
