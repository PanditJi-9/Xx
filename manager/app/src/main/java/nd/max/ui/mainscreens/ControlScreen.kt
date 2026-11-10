/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * The Control primary destination.
 *
 * **Screen card — archetype: Matrix · object: the machine's ten domains ·
 * carrying element: the band list.** The person arrives with one question
 * ("where do I change X?") and leaves by a row; so the screen is a grid of
 * domains, not a dashboard of numbers. Nothing on this page plots a series —
 * a plot belongs to the screen that owns that series, and a row that carries a
 * chart is read as decoration.
 *
 * Product shape: this is the old tuning workspace, kept in the new design
 * system. Grouped bands of destinations, one row per screen, a short line under
 * each title.
 *
 * Four things below are deliberate:
 *
 * 1. The screen opens straight into its bands. The explanatory header card
 *    ("global control" + scope paragraph) is gone: it pushed every real control
 *    below the fold, and the same copy already lives behind the top-bar help
 *    action.
 * 2. Two presentations, one model. `compact` is a responsive deck of illustrated domains,
 *    linking out to the domain's own hub screen. `expanded` skips that link:
 *    the hub screen shows nothing but the same rows (see
 *    MaxDomainHubScreen), so a header that only led there was a tap to
 *    nowhere new. Instead each domain gets a small label and its rows sit
 *    in one grouped list right there — a labelled group per domain rather
 *    than a group container inside a group container, and the same
 *    working-list presentation. Both
 *    presentations read [controlLayoutModel], so adding or removing a
 *    destination shows up in both at once.
 * 3. The layout switch is one top-bar icon that opens the two names as a menu
 *    (see MaxViewMenu); the two-chip control it replaced crowded the help and
 *    side actions on phones.
 * 4. The advanced tools carry no risk chips. They are still gated in Settings
 *    (ADR-16); on this screen the label was noise repeated on every row.
 */
package nd.max.ui.mainscreens

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.ui.component.RootAppDialog
import nd.max.ui.component.rememberConfigBackupFlow
import nd.max.ui.design.MaxHelpAction
import nd.max.ui.design.MaxCardData
import nd.max.ui.design.MaxCardGrid
import nd.max.ui.design.MaxCardSize
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxIllustration
import nd.max.ui.design.MaxListScreen
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxViewMenu
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.navigation.MaxNavActions
import nd.max.ui.navigation.illustrationKind
import nd.max.ui.viewmodel.TweakViewModel

/** Layout preference store — the same "settings" file the rest of the UI uses. */
private const val CONTROL_PREFS = "settings"

/** `false` (the default) is the compact list; `true` is the expanded page. */
private const val CONTROL_LAYOUT_KEY = "control_layout_expanded"

/** Index of the expanded option in the layout menu. */
private const val EXPANDED_INDEX = 1

@Composable
fun ControlScreen(navController: NavHostController) {
    val actions = MaxNavActions(navController)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: TweakViewModel = viewModel()

    val prefs = remember(context) { context.getSharedPreferences(CONTROL_PREFS, Context.MODE_PRIVATE) }
    var expandedLayout by rememberSaveable {
        mutableStateOf(prefs.getBoolean(CONTROL_LAYOUT_KEY, false))
    }

    // **ومُوجِّد الشاشات في شريط الصفحة لا في جسمها:** الصفحة تعرض نحو أربعين صفًّا في تسع
    // مجالات، فمن يبحث عن شاشة يعرف اسمًا لا مجالًا. والحقل داخل الصفحة كان يُزيح أوّل صفّ
    // إلى أسفل الطيّة؛ وزرٌّ في الشريط لا يأخذ من القراءة شيئًا حتى يُستدعى.
    // (وأمر المالك في هذه الجولة: «أوّلًا زرّ بحث … يدعم كل اللغات» — والعربي منه يُقاس في
    // `ScreenFinderTest`، والأربع والثمانون لغة تُطوى بنفس الدالّة.)
    var showFinder by rememberSaveable { mutableStateOf(false) }

    val backupFlow = rememberConfigBackupFlow(viewModel) { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // One model, two presentations. Nothing below decides what the page contains:
    // it only decides how much of it is opened out.
    val access = nd.max.ui.navigation.rememberDestinationAccess()
    val bands = controlLayoutModel(access)
    val toolGroups = controlToolGroups(access)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    HomeTourScrollBinding(listState)

    MaxListScreen(
        scrollState = listState,
        title = stringResource(R.string.max_nav_control),
        onBack = actions::back,
        accentIcon = MaxDestination.Control.icon,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { showFinder = true }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.screen_finder_title),
                )
            }
            MaxViewMenu(
                labels = listOf(
                    stringResource(R.string.control_view_compact),
                    stringResource(R.string.control_view_expanded),
                ),
                selectedIndex = if (expandedLayout) EXPANDED_INDEX else 0,
                contentDescription = stringResource(R.string.control_view_switch_cd),
                onSelect = { index ->
                    expandedLayout = index == EXPANDED_INDEX
                    prefs.edit().putBoolean(CONTROL_LAYOUT_KEY, expandedLayout).apply()
                },
            )
            MaxHelpAction(
                title = stringResource(R.string.control_map_title),
                body = listOf(stringResource(R.string.control_workspace_subtitle), stringResource(R.string.control_map_desc)).joinToString(separator = "\n\n"),
            )
            IconButton(onClick = backupFlow::showSheet) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = stringResource(R.string.cd_backup_restore),
                )
            }
        },
    ) {
        bands.forEachIndexed { bandIndex, band ->
            item(key = band.key) {
                val content: @Composable () -> Unit = {
                    ControlBand(
                        titleRes = band.titleRes,
                        hubs = band.hubs,
                        expanded = expandedLayout,
                        onOpen = actions::navigateTo,
                    )
                }
                // **مرساة الجولة على المجال الأول** (جولة أول فتح): الخطوة تعرض «كل مجال بابٌ إلى
                // شاشته» على أوّل مجموعة معروضة، فلا تحتاج تمريرًا ولا تفتح شاشةً أخرى لتشرح هذه.
                if (bandIndex == 0) {
                    HomeTourAnchor(HomeTourTarget.ControlDomains) { content() }
                } else {
                    content()
                }
            }
        }

        // **ولا مرساة جولة هنا:** هذه آخر مجموعة في الصفحة (تحت الطيّة)، والجولة لا تمرّر غير
        // الرئيسية — فمرساةٌ لا تُرى تعني خطوةً بلا عنصر مُضاء. خطوات التبويبات في أعلى صفحتها.
        toolGroups.forEach { group ->
            item(key = group.key) {
                ControlBand(
                    titleRes = group.titleRes,
                    entries = group.entries,
                    expanded = expandedLayout,
                    onOpen = actions::navigateTo,
                )
            }
        }

        // **ولا مفتاح إلغاء تحديثات هنا (أمر المالك):** كان صفًّا في هذه الشاشة، وصار القرار
        // قرار اللوحة (`com.maxmanager.control`) لا قرار المستخدم: المالك ينشر السياسة في الإعداد
        // الموقّع (`updates.disabled`) فتُسكَت نوافذ التحديث الثلاث للمستخدمين جميعًا. والفحص
        // اليدويّ والحجب الأمنيّ كما هما — عقده في `core/update/UpdateGate`.
    }

    // والورقة في جذر التطبيق لا داخل هذه الشاشة: مُوجِّد يعرض نتائج من كل مكان يجب أن يرتفع
    // فوق الشريط السفلي، وهو ما يضمنه تركيبُها في مضيف الحوارات الجذريّ.
    RootAppDialog {
        ScreenFinderSheet(
            visible = showFinder,
            onDismiss = { showFinder = false },
            onOpen = { destination ->
                showFinder = false
                // **والتبويبات الأربعة بمسارها الخاصّ:** فتحُها بـ`navigateTo` ينشئ نسخةً ثانية
                // في الرجوع، فتصير «رجوع» من الإعدادات تُرجع إلى الإعدادات نفسها.
                if (destination.isPrimary) {
                    actions.navigateToPrimary(destination)
                } else {
                    actions.navigateTo(destination)
                }
            },
        )
    }
}

/**
 * One titled band of the page, in either presentation.
 *
 * `hubs` are the domains (their own screens); `entries` are loose destinations
 * that belong to no domain, which is how the advanced-tools band renders.
 * Compact gives domains an illustrated deck and keeps loose tools in a grouped list.
 * Expanded opens each hub out into its own labelled grouped list of feature
 * rows, and lists any loose entries the same way.
 */
@Composable
private fun ControlBand(
    @StringRes titleRes: Int,
    expanded: Boolean,
    onOpen: (MaxDestination) -> Unit,
    hubs: List<ControlHubSpec> = emptyList(),
    entries: List<ControlEntry> = emptyList(),
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TweaksSectionTitle(text = stringResource(titleRes))

        if (expanded) {
            // One labelled list per domain. Two gaps have to stay clearly different or the
            // page reads as one unbroken column: MaxSpace.section between two lists, and
            // MaxSection's own MaxSpace.md between a label and the rows it names.
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.section)) {
                hubs.forEach { spec ->
                    ExpandedHubGroup(spec = spec, onOpen = onOpen)
                }
                if (entries.isNotEmpty()) {
                    ControlEntryGroup(entries, onOpen)
                }
            }
        } else {
            // Domains are visual doors; loose tools remain a compact working list.
            if (hubs.isNotEmpty()) {
                MaxCardGrid(
                    cards = hubs.map { spec ->
                        MaxCardData(
                            title = stringResource(spec.hub.titleRes),
                            icon = spec.hub.icon,
                            illustration = spec.hub.illustrationKind,
                            onClick = { onOpen(spec.hub) },
                        )
                    },
                    size = MaxCardSize.Medium,
                )
            }
            if (entries.isNotEmpty()) ControlEntryGroup(entries, onOpen)
        }
    }
}

/** One boundary around related tools, not a separate card around every action. */
@Composable
private fun ControlEntryGroup(
    entries: List<ControlEntry>,
    onOpen: (MaxDestination) -> Unit,
) {
    MaxGroup {
        entries.forEachIndexed { index, entry ->
            if (index > 0) MaxGroupDivider()
            ControlRow(entry = entry, onOpen = onOpen)
        }
    }
}

/** Grouped-list row: the one presentation of any destination on this page. */
@Composable
private fun ControlRow(entry: ControlEntry, onOpen: (MaxDestination) -> Unit) {
    MaxRow(
        title = stringResource(entry.destination.titleRes),
        subtitle = stringResource(entry.subtitleRes),
        icon = entry.destination.icon,
        onClick = { onOpen(entry.destination) },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
}

/**
 * One domain's rows on the expanded page: a label naming it, then its rows as
 * a grouped list — the same component, row rhythm and 6dp card gaps the compact
 * page uses, so switching views changes the grouping, not the look of a row.
 *
 * This used to be a clickable header that also linked out to
 * [ControlHubSpec.hub]'s own screen. That screen (MaxDomainHubScreen) shows
 * nothing but these same rows, so the link led nowhere the page didn't
 * already show — it is a label now, not a row, and opens nothing. The name
 * itself still earns its place: the preference editor is cross-listed under
 * both the CPU and Memory domains, and without a label the two copies would
 * be indistinguishable.
 */
@Composable
private fun ExpandedHubGroup(spec: ControlHubSpec, onOpen: (MaxDestination) -> Unit) {
    MaxSection(
        title = stringResource(spec.hub.titleRes),
        trailing = { MaxIllustration(kind = spec.hub.illustrationKind) },
    ) {
        // **وحوزٌ بلا صفوف يُفتح بلمسة — وهو استثناء واحد مُعلَن (تكملة ٢٢٦، `AU-01`):**
        // قاعدة «التسمية لا تفتح شيئًا» صحيحة لكل حوز **صفوفه هي نفسها صفوفه في الصفحة**،
        // فالرابط حينها يقود إلى ما يُعرض أصلًا. أما **الصوت** فعقده يُقرأ داخل شاشة الحوز
        // (`AudioHubPreview`: الأجهزة والمعدّل والمؤثرات) ولا صفوف له بعد — فلو بقيت التسمية
        // تسمية لصارت الصفحة الموسّعة تسمّي مجالًا لا سبيل إلى فتحه. فالفرع: صفٌّ واحد يقود
        // إليه. وأمّا ما له صفوف فلم يتغيّر حرفيًّا (وكل بوّابات الصفوف القائمة تبقى كما هي).
        val rows = if (spec.features.isEmpty()) {
            listOf(ControlEntry(spec.hub, spec.hubSubtitleRes))
        } else {
            spec.features
        }
        ControlEntryGroup(rows, onOpen)
    }
}
