/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import nd.max.core.privilege.AccessFilterStore
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeManager

/**
 * يربط سياسة [DestinationAccess] النقيّة بمصادر الحالة الحيّة (الوضع + الفلتر + لقطة الامتياز).
 *
 * **ولماذا ملفٌّ ثانٍ:** بقاء `@Composable` في ملفّ السياسة كان يمنع قياسها على JVM بلا
 * Compose — وهي سياسة تُقرّر ما يراه المستخدم، فتستحقّ اختبارًا مستقلًّا عن الواجهة.
 */
@Composable
fun rememberDestinationAccess(): DestinationAccess {
    val selection by ExecutionModeStore.state.collectAsState()
    val filter by AccessFilterStore.state.collectAsState()
    val snapshot by PrivilegeManager.snapshot.collectAsState()
    return DestinationAccess(snapshot.effectiveLevel(selection.mode), filter)
}
