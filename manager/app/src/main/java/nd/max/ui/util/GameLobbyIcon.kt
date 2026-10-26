/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import nd.max.GameLobbyEntryActivity

/**
 * يُظهر أو يُخفي أيقونة «لوبي الألعاب» المستقلّة في قائمة التطبيقات ([GameLobbyEntryActivity]).
 *
 * التبديل بحالة مكوّن في `PackageManager` (`DONT_KILL_APP`) فلا يُعاد تشغيل التطبيق ولا تُطلب صلاحية.
 * وقد تتأخّر بعض المشغّلات ثوانيَ في إظهار التغيير — وهذا يُقال للمستخدم. والمكوّن الوحيد الذي يبدّله
 * `service.sh` هو `.Launcher`، فلا تضارب.
 */
object GameLobbyIcon {
    private fun component(context: Context) =
        ComponentName(context.applicationContext, GameLobbyEntryActivity::class.java)

    /** هل الأيقونة المستقلّة ظاهرة الآن؟ (الافتراضي في المانيفست: لا.) */
    fun isEnabled(context: Context): Boolean = runCatching {
        context.packageManager.getComponentEnabledSetting(component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    /** يعيد `true` إن قبل النظام التغيير؛ والفشل يُعلن ولا يُخفى. */
    fun setEnabled(context: Context, enabled: Boolean): Boolean = runCatching {
        context.packageManager.setComponentEnabledSetting(
            component(context),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        true
    }.getOrDefault(false)
}
