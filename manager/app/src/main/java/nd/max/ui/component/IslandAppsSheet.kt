/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.ui.component

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
import androidx.compose.ui.text.font.FontWeight
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
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/**
 * ورقة «التطبيقات» — من كان يفتحها تبويبًا في استوديو الجزيرة يجدها هنا ورقةً داخل الشاشة.
 *
 * **ولماذا ورقة لا تبويب:** الشاشة الموحّدة واحدة، والاختيار هنا قرار فرعيّ («مَن يُسمح له
 * بإنارة الجزيرة؟») لا شاشة قائمة بذاتها — فالورقة تفتح فوق الشاشة وتُغلق فتعود إلى موضعك.
 *
 * **والباقي منقول حرفيًّا من `AppsScreen`:** القائمة محسوبة على الخيط الخلفيّ وتُخزَّن في
 * `appsCache` (فتفتح فورًا ثم تُعاد القراءة)، وأيقوناتها في `iconCache` (تحميل العناوين هو
 * البطيء)، والاستقبال يُعاد الفحص عنده عند التثبيت/الإزالة/التحديث وعند العودة للتطبيق.
 * والقائمة تبقى محدودة الارتفاع (`heightIn`) لأن الورقة تُمرَّر هي نفسها.
 */
@Composable
internal fun IslandAppsSheet(visible: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val blocked by IslandSettings.blockedApps.collectAsState()
    var apps by remember { mutableStateOf<List<AppItem>?>(appsCache) }
    var query by rememberSaveable { mutableStateOf("") }     // يعيش عبر فتح الورقة وإغلاقها
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    // تثبيت / إزالة / تحديث أثناء فتح الورقة أو عند العودة إلى التطبيق: أعد الفحص
    DisposableEffect(ctx, visible) {
        if (!visible) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.data?.schemeSpecificPart?.let { iconCache.remove(it) }
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

    LaunchedEffect(refreshTick, visible) {
        if (!visible) return@LaunchedEffect
        val fresh = withContext(Dispatchers.Default) {
            scanApps(ctx, appsCache, IslandSettings.blockedApps.value)
        }
        appsCache = fresh
        if (fresh != apps) apps = fresh   // القائمة نفسها = لا إعادة رسم
    }
    val base = remember(apps, showSystem) { apps?.filter { showSystem || !it.system } }
    val shown = remember(base, query) {
        base?.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    CustomBottomSheet(visible = visible, onDismiss = onClose) {
        if (!visible) return@CustomBottomSheet
        Column(Modifier.fillMaxWidth().padding(horizontal = MaxSpace.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Android, null, Modifier.size(22.dp))
                Spacer(Modifier.width(MaxSpace.sm))
                Text(
                    text = stringResource(R.string.ify_tab_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(MaxSpace.sm))
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
            FilterChip(
                selected = showSystem,
                onClick = { showSystem = !showSystem },
                label = { Text(stringResource(R.string.ify_apps_system)) }
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // عدّاد المحجوب يقلب صعودًا/هبوطًا عند تغيّره
                AnimatedContent(
                    targetState = blocked.size,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val up = targetState > initialState
                        (fadeIn(tween(180)) + slideInVertically(tween(220)) { if (up) it else -it })
                            .togetherWith(
                                fadeOut(tween(120)) + slideOutVertically(tween(180)) { if (up) -it else it }
                            )
                    },
                    label = "blockedCount"
                ) { n ->
                    Text(
                        stringResource(R.string.ify_apps_blocked, n),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    IslandSettings.blockedApps.value = emptySet(); IslandSettings.save()
                }) { Text(stringResource(R.string.ify_apps_allow_all)) }
                TextButton(onClick = {
                    IslandSettings.blockedApps.value =
                        IslandSettings.blockedApps.value + base.orEmpty().map { it.pkg }
                    IslandSettings.save()
                }) { Text(stringResource(R.string.ify_apps_block_all)) }
            }

            Crossfade(targetState = shown == null, label = "appsList") { loading ->
                if (loading) {
                    Box(
                        Modifier.fillMaxWidth().height(240.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(shown.orEmpty(), key = { it.pkg }) { app ->
                            val on = app.pkg !in blocked
                            // المحجوب يخفت قليلًا، والصفوف تنزلق حين تُرشِّحه البحث
                            val rowAlpha = if (on) 1f else 0.5f
                            ListItem(
                                headlineContent = { Text(app.label) },
                                leadingContent = { AppIcon(app.pkg) },
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
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

/** [system] = تطبيق نظام بلا أيقونة مشغّل: يُخفى حتى يُشغَّل خيار «تطبيقات النظام». */
private data class AppItem(val pkg: String, val label: String, val system: Boolean = false)

// يظهر فورًا عند فتح الورقة؛ ويُعاد الفحص عند كل فتح / عودة / تثبيت / إزالة
private var appsCache: List<AppItem>? = null
private val iconCache = android.util.LruCache<String, ImageBitmap>(160)

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}

/**
 * تطبيقات المشغّل + تطبيقات المستخدم بلا أيقونة مشغّل (+ تطبيقات النظام بلا أيقونة، معلَّمة)
 * + حِزم محجوبة لم تعد مثبَّتة، ليبقى رفع الحجب عنها ممكنًا.
 * وعناوين ما في [prev] تُعاد استعمالًا: تحميل العنوان هو الجزء البطيء.
 */
@Suppress("DEPRECATION")
private fun scanApps(ctx: Context, prev: List<AppItem>?, blocked: Set<String>): List<AppItem> {
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
private fun AppIcon(pkg: String) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = iconCache.get(pkg), pkg) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }
                    .getOrNull()
            }?.also { iconCache.put(pkg, it) }
        }
    }
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(maxRadius(MaxRadius.control))), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize()) else Icon(Icons.Rounded.Android, null)
    }
}
