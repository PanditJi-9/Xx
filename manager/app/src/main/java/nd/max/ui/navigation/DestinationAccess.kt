/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.ui.navigation

import nd.max.core.privilege.AccessFilter
import nd.max.core.privilege.PrivilegeLevel

/** أدنى مسار تنفيذي قائم، لا صلاحية نطمح إلى دعمها. الشاشات المختلطة تبقى مفتوحة للقراءة. */
val MaxDestination.requiredPrivilege: PrivilegeLevel
    get() = when (this) {
        // أوامر منصّة يسمح بها مستوى ADB: `wm` و`pm` و`dumpsys deviceidle` و`am` — تعمل
        // بالجذر وبShizuku معًا، فتصنيفها ROOT كان يُخفيها ظلمًا في وضع Shizuku.
        MaxDestination.Resolution, MaxDestination.DebloatFreeze, MaxDestination.DozeMode ->
            PrivilegeLevel.SHIZUKU
        // كتابة sysfs أو ملفّات محميّة: مالك العتاد وحده.
        MaxDestination.CpuCoreControl, MaxDestination.GovernorSettings, MaxDestination.PreferenceTweaks,
        MaxDestination.GpuStudio, MaxDestination.ZramManager, MaxDestination.MemoryUfs,
        MaxDestination.TouchBoost, MaxDestination.FpsGo, MaxDestination.ThermalDetail,
        MaxDestination.BypassCharging, MaxDestination.BypassChargingCheck, MaxDestination.NetworkScheduler,
        MaxDestination.FileManager, MaxDestination.Dex2oat -> PrivilegeLevel.ROOT
        else -> PrivilegeLevel.NONE
    }

data class DestinationAccess(val level: PrivilegeLevel, val filter: AccessFilter) {
    fun supported(destination: MaxDestination): Boolean = level.atLeast(destination.requiredPrivilege)

    fun visible(destination: MaxDestination): Boolean {
        if (destination.hidden) return false
        if (filter == AccessFilter.EVERYTHING) return true
        if (!supported(destination)) return false
        // حوز فارغ ليس ميزة متاحة. المرجع يبقى شجرة الوجهات نفسها، لا قائمة وجهات ثانية.
        if (destination.parent == MaxDestination.Control && destination.route.startsWith("hub_")) {
            return MaxDestination.Visible.any { it.parent == destination && supported(it) } ||
                (destination == MaxDestination.MemoryHub && supported(MaxDestination.PreferenceTweaks))
        }
        return true
    }
}
