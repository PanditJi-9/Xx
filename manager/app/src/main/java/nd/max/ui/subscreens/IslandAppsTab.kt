/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root.
 *
 * تبويب التطبيقات (Apps) — إدارة الإشعارات والتطبيقات المسموح لها بإضاءة الجزيرة.
 */
package nd.max.ui.subscreens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.island.core.IslandSettings
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

private data class AppItem(val pkg: String, val label: String, val system: Boolean = false)
private var appsTabCache: List<AppItem>? = null
private val appTabIconCache = android.util.LruCache<String, ImageBitmap>(160)

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}

@Suppress("DEPRECATION")
private fun scanAppsForTab(ctx: Context, prev: List<AppItem>?, blocked: Set<String>): List<AppItem> {
    val pm = ctx.packageManager
    val known = prev.orEmpty().associate { it.pkg to it.label }
    val launcher = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
    ).map { it.activityInfo.packageName }.toSet()

    val out = LinkedHashMap<String, AppItem>()
    for (ai in pm.getInstalledApplications(0)) {
        val pkg = ai.packageName
        if (pkg == ctx.packageName) continue
        val isSystem = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val hiddenByDefault = isSystem && pkg !in launcher
        val label = known[pkg] ?: runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(pkg)
        out[pkg] = AppItem(pkg, label, hiddenByDefault)
    }
    for (p in blocked) if (p !in out && p != ctx.packageName) out[p] = AppItem(p, p)
    return out.values.sortedBy { it.label.lowercase() }
}

@Composable
private fun TabAppIcon(pkg: String) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = appTabIconCache.get(pkg), pkg) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }
                    .getOrNull()
            }?.also { appTabIconCache.put(pkg, it) }
        }
    }
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(maxRadius(MaxRadius.control))), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize()) else Icon(Icons.Rounded.Android, null)
    }
}

/**
 * محتوى تبويب «التطبيقات»: إدارة الإشعارات لكل تطبيق، البحث، حظر الكل / السماح للكل.
 */
@Composable
internal fun AppsTabContent(
    blocked: Set<String>
) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>?>(appsTabCache) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.data?.schemeSpecificPart?.let { appTabIconCache.remove(it) }
                refreshTick++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val owner = ctx.findLifecycleOwner()
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refreshTick++ }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            runCatching { ctx.unregisterReceiver(receiver) }
            owner?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshTick) {
        val fresh = withContext(Dispatchers.Default) {
            scanAppsForTab(ctx, appsTabCache, IslandSettings.blockedApps.value)
        }
        appsTabCache = fresh
        if (fresh != apps) apps = fresh
    }

    val base = remember(apps, showSystem) { apps?.filter { showSystem || !it.system } }
    val shown = remember(base, query) {
        base?.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section)
    ) {
        MaxSection(
            title = stringResource(R.string.ify_tab_apps),
            description = stringResource(R.string.ify_apps_blocked, blocked.size)
        ) {
            MaxGroup {
                Column(Modifier.fillMaxWidth().padding(MaxSpace.md)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.ify_apps_search)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(MaxSpace.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showSystem,
                            onClick = { showSystem = !showSystem },
                            label = { Text(stringResource(R.string.ify_apps_system)) }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                IslandSettings.blockedApps.value = emptySet()
                                IslandSettings.save()
                            }) { Text(stringResource(R.string.ify_apps_allow_all)) }
                            TextButton(onClick = {
                                IslandSettings.blockedApps.value =
                                    IslandSettings.blockedApps.value + base.orEmpty().map { it.pkg }
                                IslandSettings.save()
                            }) { Text(stringResource(R.string.ify_apps_block_all)) }
                        }
                    }

                    Spacer(Modifier.height(MaxSpace.sm))

                    Crossfade(targetState = shown == null, label = "appsTabList") { loading ->
                        if (loading) {
                            Box(
                                Modifier.fillMaxWidth().height(240.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                                items(shown.orEmpty(), key = { it.pkg }) { app ->
                                    val on = app.pkg !in blocked
                                    val rowAlpha = if (on) 1f else 0.5f
                                    ListItem(
                                        headlineContent = { Text(app.label) },
                                        supportingContent = {
                                            Text(
                                                app.pkg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        leadingContent = { TabAppIcon(app.pkg) },
                                        trailingContent = {
                                            Switch(checked = on, onCheckedChange = null)
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .animateItem()
                                            .graphicsLayer { this.alpha = rowAlpha }
                                            .toggleable(
                                                value = on,
                                                role = androidx.compose.ui.semantics.Role.Switch,
                                                onValueChange = { v -> IslandSettings.setBlocked(app.pkg, !v) }
                                            )
                                    )
                                }
                                item { Spacer(Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
