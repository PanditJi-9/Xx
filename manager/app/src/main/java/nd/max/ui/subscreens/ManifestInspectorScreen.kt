/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.ui.component.AppIconImage
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricReadout
import nd.max.ui.design.MaxMetricSize
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * `MANIFEST-INSPECTOR-01` — فاحص بيان الحزمة: كل ما يُصرّح به تطبيق مثبَّت عن نفسه، مقروءًا
 * من `PackageManager` وحده.
 *
 * لماذا شاشة: هذا هو السطح الوحيد الذي يشرح **لماذا** يطلب تطبيقٌ إذنًا، أو لماذا يظهر في
 * المنسدلة، أو لماذا يفتح نفسه تلقائيًّا بعد الإقلاع — بدل أن يبقى ذلك تخمينًا. والمراجع
 * (fkm و devcheck) اتفقت عليه، ولم يوجد له نظير في الشجرة: أقرب موجود `DeviceInfoScreen`
 * (معلومات الجهاز، لا التطبيق) و`AppSettingsScreen` (ضبط التشغيل، لا البيان).
 *
 * وحدوده معلنة في الشاشة نفسها، لا مخفيّة:
 * - `<uses-feature>` الخاصّ بتطبيقٍ **غير مقروء** لأيّ تطبيق (المنصّة لا تكشفه)؛ ما نعرضه هو
 *   ما يقوله النظام عن **الجهاز**، ونكتب ذلك بجانبه.
 * - `compileSdkVersion` ليس في `PackageInfo` ⇒ يُعرض `—` ولا يُخترع.
 * - القراءة وحدها: لا كتابة ولا تعطيل ولا منح إذن من هنا (كل ذلك في شاشته القائمة).
 *
 * Two panes in landscape: the left is the installed-app picker, the right is the selected
 * package's manifest. `scrollEnabled = false` because each pane owns its own scroll — a
 * single shared scroll would fight the `LazyColumn`.
 */

/** الشرطة التي تعني «لا نعرف» — لا `0` ولا فراغ (ADR-07). */
private const val MiDash = "—"

private val MiPaneWidth = 292.dp
private val MiGap = MaxSpace.md
private val MiIconSize = 34.dp

/**
 * لقطة بيان حزمة واحدة **بعد** القراءة — نموذجٌ ثابت لا يمسك `PackageInfo` في الواجهة،
 * فالحقول تُقرأ مرّة على `Dispatchers.IO` ثم تُعرض كما هي.
 */
internal data class ManifestSnapshot(
    val packageName: String,
    val label: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val installedAt: String?,
    val updatedAt: String?,
    val installer: String?,
    val uid: Int?,
    val dataDir: String?,
    val debuggable: Boolean?,
    val systemApp: Boolean?,
    val enabled: Boolean?,
    val permissions: List<ManifestPermission>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val features: List<String>,
    val signature: String?,
)

/** إذن واحد مع تصنيف الحماية الذي يقرّره النظام (خطر · توقيع · عاديّ). */
internal data class ManifestPermission(val name: String, val protection: Int, val granted: Boolean)

/**
 * يقرأ البيان مرّة. وكلُّ ما يمكن أن يرمي (`NameNotFoundException`، حزمة تُنزَّع أثناء القراءة،
 * صلاحية سحب الشهادات) داخل `runCatching` — فالفشل يُعرض `—` ولا يُسقط الشاشة.
 */
private fun readManifest(context: android.content.Context, packageName: String): ManifestSnapshot? {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val flags = PackageManager.GET_PERMISSIONS or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS or
        PackageManager.GET_SIGNING_CERTIFICATES
    val info: PackageInfo = runCatching {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, flags)
    }.getOrNull() ?: return null

    val app: ApplicationInfo? = info.applicationInfo
    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val requested = info.requestedPermissions ?: emptyArray()
    val grantedFlags = info.requestedPermissionsFlags
    val permissions = requested.mapIndexed { index, name ->
        val protection = runCatching {
            @Suppress("DEPRECATION")
            pm.getPermissionInfo(name, 0).protection
        }.getOrDefault(0)
        val granted = grantedFlags != null &&
            index < grantedFlags.size &&
            (grantedFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        ManifestPermission(name, protection, granted)
    }

    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    val signature = runCatching {
        info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString(":") { "%02X".format(it) }
        }
    }.getOrNull()

    return ManifestSnapshot(
        packageName = packageName,
        label = runCatching { app?.let { pm.getApplicationLabel(it).toString() } }.getOrNull(),
        versionName = info.versionName,
        versionCode = runCatching { info.longVersionCode }.getOrNull(),
        minSdk = runCatching { app?.minSdkVersion }.getOrNull(),
        targetSdk = runCatching { app?.targetSdkVersion }.getOrNull(),
        installedAt = info.firstInstallTime.takeIf { it > 0 }?.let { stamp.format(Date(it)) },
        updatedAt = info.lastUpdateTime.takeIf { it > 0 }?.let { stamp.format(Date(it)) },
        installer = installer,
        uid = runCatching { app?.uid }.getOrNull(),
        dataDir = runCatching { app?.dataDir }.getOrNull(),
        debuggable = app?.let { (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 },
        systemApp = app?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 },
        enabled = runCatching { app?.enabled }.getOrNull(),
        permissions = permissions,
        activities = info.activities?.map { it.name } ?: emptyList(),
        services = info.services?.map { it.name } ?: emptyList(),
        receivers = info.receivers?.map { it.name } ?: emptyList(),
        providers = info.providers?.map { it.name } ?: emptyList(),
        features = runCatching {
            pm.systemAvailableFeatures?.mapNotNull { it.name }?.take(24) ?: emptyList()
        }.getOrDefault(emptyList()),
        signature = signature,
    )
}

/** صفّ واحد في قائمة التطبيقات: اسمٌ يقرأه الإنسان وحزمةٌ تُعرّفه. */
private data class MiApp(val label: String, val packageName: String)

private fun readApps(context: android.content.Context): List<MiApp> {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val installed = runCatching { pm.getInstalledPackages(0) }.getOrDefault(emptyList())
    return installed.mapNotNull { pkg ->
        val app = pkg.applicationInfo ?: return@mapNotNull null
        MiApp(
            label = runCatching { pm.getApplicationLabel(app).toString() }
                .getOrDefault(pkg.packageName),
            packageName = pkg.packageName,
        )
    }.sortedBy { it.label.lowercase(Locale.getDefault()) }
}

@Composable
fun ManifestInspectorScreen(navController: NavController) {
    val context = LocalContext.current
    val apps by produceState(initialValue = emptyList<MiApp>(), context) {
        value = withContext(Dispatchers.IO) { readApps(context) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    // القراءة الأولى تختار أوّل تطبيق كي لا تُفتح الشاشة فارغة: الاختيار حالةٌ لا شرط.
    val current = selected ?: apps.firstOrNull()?.packageName
    val snapshot by produceState(initialValue = null as ManifestSnapshot?, current) {
        value = current?.let { withContext(Dispatchers.IO) { readManifest(context, it) } }
    }

    val shown = remember(apps, query) {
        val needle = query.trim().lowercase(Locale.getDefault())
        if (needle.isEmpty()) apps else apps.filter {
            it.label.lowercase(Locale.getDefault()).contains(needle) ||
                it.packageName.lowercase(Locale.getDefault()).contains(needle)
        }
    }

    MaxScreen(
        title = stringResource(R.string.manifest_inspector_title),
        subtitle = stringResource(R.string.manifest_inspector_subtitle),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.AppSettingsAlt,
        accent = GameSpacePalette.VioletSoft,
        scrollEnabled = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaxSpace.lg),
            horizontalArrangement = Arrangement.spacedBy(MiGap),
        ) {
            Column(modifier = Modifier.width(MiPaneWidth).fillMaxHeight()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.mi_search)) },
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = MaxSpace.sm),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                ) {
                    items(shown, key = { it.packageName }) { item ->
                        MaxRow(
                            title = item.label,
                            subtitle = item.packageName,
                            onClick = { selected = item.packageName },
                            trailing = if (item.packageName == current) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = GameSpacePalette.Mint,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MaxSpace.sm),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
            ) {
                val s = snapshot
                if (s == null) {
                    Text(
                        text = stringResource(R.string.mi_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameSpacePalette.Muted,
                    )
                } else {
                    ManifestHeader(s)
                    IdentitySection(s)
                    PermissionsSection(s)
                    ComponentsSection(s)
                    FeaturesSection(s)
                    SignatureSection(s)
                }
            }
        }
    }
}

@Composable
private fun ManifestHeader(snapshot: ManifestSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        AppIconImage(
            packageName = snapshot.packageName,
            size = MiIconSize,
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.label ?: snapshot.packageName,
                style = MaterialTheme.typography.titleMedium,
                color = GameSpacePalette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = snapshot.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = GameSpacePalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun IdentitySection(snapshot: ManifestSnapshot) {
    MaxSection(title = stringResource(R.string.mi_section_identity)) {
        MaxGroup {
            MetricRow(stringResource(R.string.mi_version), snapshot.versionName)
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_version_code), snapshot.versionCode?.toString())
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_min_sdk), snapshot.minSdk?.toString())
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_target_sdk), snapshot.targetSdk?.toString())
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_installed), snapshot.installedAt)
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_updated), snapshot.updatedAt)
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_installer), snapshot.installer)
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_uid), snapshot.uid?.toString())
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_data_dir), snapshot.dataDir)
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_debuggable), yesNo(snapshot.debuggable))
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_system_app), yesNo(snapshot.systemApp))
            MaxGroupDivider()
            MetricRow(stringResource(R.string.mi_enabled), yesNo(snapshot.enabled))
        }
    }
}

@Composable
private fun PermissionsSection(snapshot: ManifestSnapshot) {
    val danger = snapshot.permissions.count {
        (it.protection and android.content.pm.PermissionInfo.PROTECTION_DANGEROUS) != 0
    }
    MaxSection(
        title = stringResource(R.string.mi_section_permissions),
        description = stringResource(R.string.mi_permissions_summary, snapshot.permissions.size, danger),
    ) {
        MaxGroup {
            if (snapshot.permissions.isEmpty()) {
                MetricRow(stringResource(R.string.mi_permissions_none), null)
            } else {
                snapshot.permissions.forEachIndexed { index, permission ->
                    if (index > 0) MaxGroupDivider()
                    MaxRow(
                        title = permission.name.substringAfterLast('.'),
                        subtitle = permission.name,
                        icon = null,
                        iconTone = if (permission.granted) MaxTone.Positive else MaxTone.Neutral,
                        trailing = {
                            Text(
                                text = stringResource(permissionLabel(permission)),
                                style = MaterialTheme.typography.labelSmall,
                                color = GameSpacePalette.Muted,
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun permissionLabel(permission: ManifestPermission): Int = when {
    (permission.protection and android.content.pm.PermissionInfo.PROTECTION_DANGEROUS) != 0 ->
        R.string.mi_perm_dangerous
    (permission.protection and android.content.pm.PermissionInfo.PROTECTION_SIGNATURE) != 0 ->
        R.string.mi_perm_signature
    permission.protection == 0 -> R.string.mi_perm_unknown
    else -> R.string.mi_perm_normal
}

@Composable
private fun ComponentsSection(snapshot: ManifestSnapshot) {
    MaxSection(title = stringResource(R.string.mi_section_components)) {
        ComponentGroup(stringResource(R.string.mi_activities), snapshot.activities)
        ComponentGroup(stringResource(R.string.mi_services), snapshot.services)
        ComponentGroup(stringResource(R.string.mi_receivers), snapshot.receivers)
        ComponentGroup(stringResource(R.string.mi_providers), snapshot.providers)
    }
}

@Composable
private fun ComponentGroup(title: String, names: List<String>) {
    MaxSection(title = title, description = names.size.toString()) {
        MaxGroup {
            if (names.isEmpty()) {
                MetricRow(stringResource(R.string.mi_components_none), null)
            } else {
                names.forEachIndexed { index, name ->
                    if (index > 0) MaxGroupDivider()
                    MaxRow(
                        title = name.substringAfterLast('.'),
                        subtitle = name,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturesSection(snapshot: ManifestSnapshot) {
    MaxSection(
        title = stringResource(R.string.mi_section_features),
        description = stringResource(R.string.mi_features_note),
    ) {
        MaxGroup {
            if (snapshot.features.isEmpty()) {
                MetricRow(stringResource(R.string.mi_features_none), null)
            } else {
                snapshot.features.forEachIndexed { index, feature ->
                    if (index > 0) MaxGroupDivider()
                    MaxRow(title = feature)
                }
            }
        }
    }
}

@Composable
private fun SignatureSection(snapshot: ManifestSnapshot) {
    MaxSection(
        title = stringResource(R.string.mi_section_signature),
        description = stringResource(R.string.mi_signature_note),
    ) {
        MaxGroup {
            MetricRow(stringResource(R.string.mi_signature_sha256), snapshot.signature)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String?) {
    MaxMetricReadout(
        metric = MaxMetric(label = label, value = value ?: MiDash),
        size = MaxMetricSize.Small,
    )
}

@Composable
private fun yesNo(value: Boolean?): String = when (value) {
    true -> stringResource(R.string.mi_yes)
    false -> stringResource(R.string.mi_no)
    null -> MiDash
}
