# REA package declaration

- package: `com.zte.mifavor.launcher`
- version: `16.0.010.000.2605260038` (160000)
- sdk: min 31 / target 36
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 4502 · resources 8910 · permissions 79
- components: 54 declared (partial)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| service | `com.android.quickstep.TouchInteractionService` | True | android.permission.STATUS_BAR_SERVICE | 1 |
| activity | `com.android.quickstep.RecentsActivity` | None | — | 0 |
| provider | `com.android.quickstep.LauncherSearchIndexablesProvider` | True | android.permission.READ_SEARCH_INDEXABLES | 1 |
| provider | `androidx.core.content.FileProvider` | False | — | 0 |
| activity | `com.android.launcher3.proxy.ProxyActivityStarter` | False | — | 0 |
| activity | `com.android.quickstep.interaction.GestureSandboxActivity` | True | — | 1 |
| activity | `com.android.quickstep.interaction.AllSetActivity` | True | android.permission.REBOOT | 1 |
| activity | `com.android.launcher3.WidgetPickerActivity` | True | com.zte.mifavor.launcher.permission.START_WIDGET_PICKER_ACTIVITY | 1 |
| activity | `com.android.launcher3.uioverrides.QuickstepLauncher` | True | — | 1 |
| activity | `com.android.quickstep.SplitActivity` | True | launchermfv.permission.SPLITSELECT | 1 |
| activity | `com.android.launcher3.settings.ChooseLockHelperExt` | False | — | 0 |
| receiver | `com.android.launcher3.SessionCommitReceiver` | True | — | 1 |
| receiver | `com.android.launcher3.AppWidgetsRestoredReceiver` | True | — | 1 |
| service | `com.android.launcher3.notification.NotificationListener` | True | android.permission.BIND_NOTIFICATION_LISTENER_SERVICE | 1 |
| activity | `com.android.launcher3.dragndrop.AddItemActivity` | True | — | 1 |
| provider | `com.android.launcher3.LauncherProvider` | True | — | 0 |
| provider | `com.android.launcher3.graphics.LauncherCustomizationProvider` | True | android.permission.BIND_WALLPAPER | 0 |
| provider | `com.android.launcher3.testing.TestInformationProvider` | True | — | 0 |
| activity | `com.android.launcher3.secondarydisplay.SecondaryDisplayLauncher` | True | — | 1 |
| activity | `com.android.launcher3.settings.SettingsActivityMFV` | True | — | 2 |
| activity | `com.android.launcher3.settings.SubscriptActivity` | True | launchermfv.permission.SET_SUBSCRIPT | 2 |
| activity | `com.android.launcher3.settings.DesktopGridActivity` | True | launchermfv.permission.SET_SUBSCRIPT | 0 |
| activity | `com.android.launcher3.settings.HomeScreenLayoutActivity` | True | launchermfv.permission.SET_SUBSCRIPT | 0 |
| activity | `com.android.launcher3.settings.RecentAppStyleActivity` | True | launchermfv.permission.SET_SUBSCRIPT | 0 |
| activity | `com.android.launcher3.settings.CustomPageSelectionLayoutActivity` | True | launchermfv.permission.SET_SUBSCRIPT | 0 |
| activity | `com.android.launcher3.settings.desktopclock.DesktopClockSettingsActivity` | True | — | 0 |
| activity | `com.android.launcher3.mfvUtil.DefaultErrorActivity` | False | — | 0 |
| activity | `com.android.launcher3.mfvUtil.PermissionGrantDialog` | False | — | 0 |
| activity | `com.android.launcher3.FoldHelpTipAty` | False | — | 0 |
| provider | `com.android.launcher3.resource.ResourceDatabaseProvider` | False | — | 0 |
| provider | `com.android.launcher3.backup.BackupProvider` | True | launchermfv.permission.USE_BACKUP_RESTORE | 0 |
| provider | `com.android.launcher3.resource.TaskProvider` | True | launchermfv.permission.READ_TASK | 0 |
| provider | `com.android.launcher3.resource.DynamicShowHiddenAppsProvider` | True | launchermfv.permission.DYNAMIC_SHOW_HIDDEN_APPS | 0 |
| provider | `com.android.launcher3.ShareFileProvider` | False | — | 0 |
| provider | `com.android.launcher3.resource.UnReadInfoProvider` | True | — | 0 |
| provider | `com.android.launcher3.resource.WechatUnreadProvider` | True | — | 0 |
| provider | `com.android.launcher3.resource.OtherUnreadProvider` | True | — | 0 |
| provider | `com.android.launcher3.widget.MFVWidgetProvider` | True | — | 0 |
| service | `com.android.launcher3.backup.BackupWeShareService` | True | launchermfv.permission.USE_BACKUP_SERVICE | 1 |
| service | `com.android.launcher3.ZBackupService` | True | launchermfv.permission.Z_BACKUP_SERVICE | 1 |
| service | `com.android.launcher3.ZRecoverService` | True | launchermfv.permission.Z_RECOVER_SERVICE | 1 |
| receiver | `com.android.launcher3.resource.ResourceOnline` | True | launchermfv.permission.USE_ONLINE_RECEIVER | 1 |
| receiver | `com.android.launcher3.InstallShortcutReceiver` | True | com.android.launcher.permission.INSTALL_SHORTCUT | 1 |
| provider | `com.android.launcher3.nubia.NubiaUnreadMarkProvider` | True | — | 0 |
| provider | `com.android.launcher3.UnlockAnimationProvider` | True | — | 0 |
| receiver | `com.android.launcher3.stk.SimCardLoadReceiver` | True | — | 2 |
| provider | `com.android.launcher3.globalsearch.GSOuterProvider` | True | launchermfv.permission.GS_LAUNCHER_DATA | 0 |
| activity | `androidx.slice.compat.SlicePermissionActivity` | None | — | 0 |
| receiver | `androidx.profileinstaller.ProfileInstallReceiver` | True | android.permission.DUMP | 4 |
| receiver | `com.zte.toolsdk.producer.ToolsReceiver` | True | — | 0 |
| receiver | `com.zte.toolsdk.producer.RemoteViewsReceiver` | None | — | 0 |
| provider | `com.zte.toolsdk.consumer.SharedPrefsContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.consumer.CardContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.producer.AnimationEndContentProvider` | True | — | 0 |
