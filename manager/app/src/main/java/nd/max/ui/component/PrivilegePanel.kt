/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.privilege.PrivilegeCatalog
import nd.max.core.privilege.PrivilegeLevel
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.privilege.PrivilegeTier
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/**
 * لوحة الامتياز والوصول (`AR-20`) — **السطح الواحد** للجذر وShizuku معًا.
 *
 * ترتيبها من الأهم إلى الأقل: الحالة الحالية، ثم الطبقتان (الجذر · Shizuku) كلٌّ في صفّ فيه زرّ
 * فعله بجانب اسمه، ثم «ما يعمل عند كل طبقة» مطويًّا. العنوان والمقدّمة تأتيان من الشاشة نفسها،
 * فلا تُكرَّر هنا؛ والتفاصيل تُفتح عند الطلب بدل أن تُقصّ في آخر الصفحة.
 *
 * **الاكتشاف تلقائي:** تُقرأ الطبقتان عند فتح الشاشة وعند العودة إليها (بلا استدعاء
 * صلاحية — القراءة السلبية أولًا في [PrivilegeManager])، ويبقى «أعِد الفحص» وزرّا
 * الجذر/Shizuku فعلين صريحين لمن أراد أن يطلب بنفسه. القراءة بلا سؤال، والطلب بضغطة.
 *
 * تُبنى على واجهات Material3 وحدها بلا تبعيات شاشة أخرى، لتُزرع في أي مكان.
 */
@Composable
fun PrivilegePanel(
    modifier: Modifier = Modifier,
    onLevelChanged: (PrivilegeLevel) -> Unit = {},
) {
    val snapshot by PrivilegeManager.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()

    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }
    // هل حاول المستخدم طلب الجذر من هنا؟ بعد المحاولة تحلّ نتيجتها محلّ وصف الصفّ (لا تُضاف فوقه).
    var rootAttempted by remember { mutableStateOf(false) }
    // التفاصيل مطوية افتراضيًّا، وتبقى على حالتها عبر إعادة التركيب.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }

    // ── الاكتشاف التلقائي ───────────────────────────────────────────────────────
    // يُسجّل المستمعين على الخيط الرئيسي (كما يشترط جسر Shizuku) ثم يقرأ الطبقتين على
    // خيط الإدخال/الإخراج، فلا تتجمّد الواجهة ولا يسأل التطبيق صلاحيةً من تلقاء نفسه.
    LaunchedEffect(Unit) {
        PrivilegeManager.start()
        withContext(Dispatchers.IO) { PrivilegeManager.refresh() }
        scanning = false
    }

    // عند العودة إلى الواجهة (بعد منح إذن من نافذة النظام مثلًا) نعيد القراءة تلقائيًا.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) PrivilegeManager.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /** إعادة الفحص يدويًّا — اختيارية: التلقائي يكفي، وهذا لمن أراد أن يتأكّد الآن. */
    fun rescan() {
        if (scanning) return
        scope.launch {
            scanning = true
            withContext(Dispatchers.IO) { PrivilegeManager.refresh() }
            scanning = false
        }
    }

    LaunchedEffect(snapshot.level) { onLevelChanged(snapshot.level) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(MaxSpace.lg)) {

            // ── الحالة الحالية: أول ما يُقرأ، وإعادة الفحص في طرفها ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(maxRadius(MaxRadius.group)),
                color = levelContainer(snapshot.level),
                contentColor = levelContent(snapshot.level),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(MaxSize.iconGlyph),
                    )
                    Spacer(Modifier.width(MaxSpace.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (scanning) R.string.max_privilege_scanning else R.string.max_privilege_current,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = stringResource(snapshot.level.labelRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (scanning || busy) {
                        // نفس مساحة زرّ الإعادة، فلا يتحرّك شيء حين يبدأ الفحص أو ينتهي.
                        Box(
                            modifier = Modifier.size(MaxSize.minTouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(MaxSize.iconGlyph),
                                strokeWidth = MaxSpace.hairline,
                                color = levelContent(snapshot.level),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { rescan() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = levelContent(snapshot.level),
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.max_privilege_rescan),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(MaxSpace.sm))

            Text(
                text = stringResource(R.string.max_privilege_layers_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaxSpace.sm))

            // ── الجذر ──
            PrivilegeLayer(
                title = stringResource(R.string.max_privilege_root_row),
                state = if (snapshot.rootGranted) {
                    stringResource(R.string.max_privilege_root_granted)
                } else {
                    stringResource(R.string.max_privilege_root_denied)
                },
                detail = stringResource(R.string.max_privilege_root_desc),
                ok = snapshot.rootGranted,
                // بعد المحاولة تحلّ نتيجتها محلّ الوصف، فلا يكبر الصفّ.
                hint = when {
                    !rootAttempted -> null
                    snapshot.rootGranted -> stringResource(R.string.root_grant_ok)
                    else -> stringResource(R.string.root_grant_denied)
                },
            ) {
                OutlinedButton(
                    enabled = !busy && !scanning,
                    onClick = {
                        busy = true
                        rootAttempted = true
                        scope.launch {
                            withContext(Dispatchers.IO) { PrivilegeManager.requestRoot() }
                            withContext(Dispatchers.IO) { PrivilegeManager.refresh() }
                            busy = false
                        }
                    },
                    shape = RoundedCornerShape(maxRadius(MaxRadius.inset)),
                ) {
                    // قبل المنح الزرّ «اطلب»، وبعده «افحص»: نفس الفعل لكن بصدق المرحلة.
                    Text(
                        stringResource(
                            if (snapshot.rootGranted) {
                                R.string.max_privilege_check_root
                            } else {
                                R.string.root_grant_title
                            },
                        ),
                    )
                }
            }

            Spacer(Modifier.height(MaxSpace.sm))

            // ── Shizuku ──
            PrivilegeLayer(
                title = stringResource(R.string.max_privilege_shizuku_row),
                state = when {
                    snapshot.shizuku.ready -> stringResource(R.string.max_privilege_shizuku_granted)
                    snapshot.shizuku.available -> stringResource(R.string.max_privilege_shizuku_denied)
                    else -> stringResource(R.string.max_privilege_shizuku_unavailable)
                },
                detail = stringResource(R.string.max_privilege_shizuku_desc),
                ok = snapshot.shizuku.ready,
            ) {
                Button(
                    enabled = !busy && !scanning && snapshot.shizuku.available,
                    onClick = {
                        busy = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (snapshot.shizuku.permissionGranted) {
                                    PrivilegeManager.refresh()
                                } else {
                                    nd.max.core.privilege.ShizukuGateway.requestPermission()
                                }
                            }
                            PrivilegeManager.refresh()
                            busy = false
                        }
                    },
                    shape = RoundedCornerShape(maxRadius(MaxRadius.inset)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.size(MaxSize.iconGlyphSmall),
                    )
                    Spacer(Modifier.width(MaxSpace.xs))
                    Text(stringResource(R.string.max_privilege_shizuku_connect))
                }
            }

            Spacer(Modifier.height(MaxSpace.sm))

            // ── ما يعمل عند كل طبقة: مطويّ افتراضيًّا، ومفتوح عند الطلب ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaxSize.minTouchTarget)
                    .clickable(role = Role.Button) { detailsOpen = !detailsOpen }
                    .padding(horizontal = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.max_privilege_features_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (detailsOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaxSize.iconGlyph),
                )
            }

            if (animationsEnabled) {
                AnimatedVisibility(
                    visible = detailsOpen,
                    enter = expandVertically(tween(MaxDuration.standard)) +
                        fadeIn(tween(MaxDuration.standard)),
                    exit = shrinkVertically(tween(MaxDuration.quick)) +
                        fadeOut(tween(MaxDuration.quick)),
                ) {
                    FeatureDetails(level = snapshot.level)
                }
            } else if (detailsOpen) {
                // بلا حركة (تفضيل النظام): يظهر المحتوى مباشرة.
                FeatureDetails(level = snapshot.level)
            }
        }
    }
}

/** تفاصيل مطوية: ما تعنيه اللوحة، وما يحدث عند طلب الجذر، ثم ما يعمل عند كل طبقة. */
@Composable
private fun FeatureDetails(level: PrivilegeLevel) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = MaxSpace.xs)) {
        Text(
            text = stringResource(R.string.max_privilege_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaxSpace.xs))
        Text(
            text = stringResource(R.string.max_privilege_auto_scan),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaxSpace.xs))
        Text(
            text = stringResource(R.string.root_grant_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaxSpace.md))
        Text(
            text = stringResource(R.string.max_privilege_honesty_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaxSpace.sm))
        TierSection(PrivilegeTier.NO_ROOT, level)
        Spacer(Modifier.height(MaxSpace.sm))
        TierSection(PrivilegeTier.SHIZUKU, level)
        Spacer(Modifier.height(MaxSpace.sm))
        TierSection(PrivilegeTier.ROOT, level)
    }
}

@Composable
private fun levelContainer(level: PrivilegeLevel): Color = when (level) {
    PrivilegeLevel.ROOT -> MaterialTheme.colorScheme.primaryContainer
    PrivilegeLevel.SHIZUKU -> MaterialTheme.colorScheme.tertiaryContainer
    PrivilegeLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun levelContent(level: PrivilegeLevel): Color = when (level) {
    PrivilegeLevel.ROOT -> MaterialTheme.colorScheme.onPrimaryContainer
    PrivilegeLevel.SHIZUKU -> MaterialTheme.colorScheme.onTertiaryContainer
    PrivilegeLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * طبقة واحدة (جذر أو Shizuku): الاسم وزرّ الفعل في سطر واحد، ثم الحالة بأيقونتها، ثم الوصف —
 * أو نتيجة المحاولة بعد الطلب تحلّ محلّه. كلها في بطاقة داخلية بلا حدّ.
 */
@Composable
private fun PrivilegeLayer(
    title: String,
    state: String,
    detail: String,
    ok: Boolean,
    hint: String? = null,
    action: @Composable () -> Unit,
) {
    // **ترحيل إلى القشرة:** القشرة ترسم حدًّا دائمًا، وهذه الطبقة كانت بلا حدّ — فيُمرّر
    // `Color.Transparent` صراحةً بدل ترك حدّ يظهر من الترحيل.
    MaxCardShell(
        modifier = Modifier.fillMaxWidth(),
        borderColor = Color.Transparent,
        contentPadding = 0.dp,
        verticalArrangement = Arrangement.Top,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaxSpace.rowPaddingHorizontal,
                vertical = MaxSpace.sm,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                action()
            }
            Spacer(Modifier.height(MaxSpace.xs))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall),
                )
                Spacer(Modifier.width(MaxSpace.xs))
                Text(
                    text = state,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ok) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.height(MaxSpace.xs))
            Text(
                text = hint ?: detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TierSection(tier: PrivilegeTier, level: PrivilegeLevel) {
    val features = PrivilegeCatalog.byTier(tier)
    if (features.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(tier.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(MaxSpace.xs))
        features.forEach { feature ->
            val available = PrivilegeCatalog.availableAt(feature, level)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaxSpace.hairline),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (available) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (available) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(MaxSize.iconGlyphSmall),
                )
                Spacer(Modifier.width(MaxSpace.sm))
                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (available) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                )
            }
        }
    }
}
