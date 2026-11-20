# REA package declaration

- package: `cn.nubia.gameassist`
- version: `16.5.002.2605252104` (17260414)
- sdk: min 33 / target 37
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 9166 · resources 9336 · permissions 76
- components: 46 declared (partial)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| activity | `cn.nubia.gameassist.test.GameAssistTestActivity` | False | — | 0 |
| activity | `cn.nubia.gameassist.test.ScreenSaverTestActivity` | False | — | 0 |
| service | `cn.nubia.gameassist.service.GameAssistService` | True | cn.nubia.gameassist.permission.ACCESS | 1 |
| service | `cn.nubia.gameassist.service.GameScreenSaverService` | True | cn.nubia.gameassist.permission.ACCESS | 1 |
| activity | `cn.nubia.gameassist.test.Game3DTestActivity` | False | — | 1 |
| service | `cn.nubia.plugin.investigation.InvestigationModeService` | True | cn.nubia.gameassist.permission.ACCESS | 1 |
| receiver | `cn.nubia.gameassist.plugin.config.UpdatePluginConfigReceiver` | True | — | 2 |
| service | `cn.nubia.gameassist.plugin.config.UpdatePluginConfigService` | False | — | 0 |
| provider | `cn.nubia.gameassist.provider.FunctionCallProvider` | True | — | 0 |
| provider | `cn.nubia.gameassist.search.GlobalSearchProvider` | True | — | 0 |
| service | `cn.nubia.gameassist.meditationmode.CustomNotificationListener` | True | android.permission.BIND_NOTIFICATION_LISTENER_SERVICE | 1 |
| receiver | `cn.nubia.multisubscreen.receiver.MultiSubScreenBroadcastReceiver` | True | — | 3 |
| activity | `cn.nubia.multisubscreen.ui.MultiSubScreenSourceActivity` | True | — | 1 |
| activity | `cn.nubia.multisubscreen.ui.MultiSubScreenSinkActivity` | True | — | 1 |
| activity | `cn.nubia.multisubscreen.ui.BaseAlertActivity` | None | — | 0 |
| activity | `cn.nubia.multisubscreen.ui.CombinedAlertAty` | None | — | 0 |
| activity | `cn.nubia.multisubscreen.ui.ChooseDeviceAlertAty` | None | — | 0 |
| provider | `com.zte.gameassist.lowsugar.provider.LowSugarProvider` | True | — | 0 |
| provider | `com.zte.gameassist.lowsugar.provider.LowSugarOcrProvider` | True | cn.nubia.gameassist.permission.gameocr | 0 |
| service | `com.zte.gameassist.aiagent.GameAgentService` | True | — | 0 |
| service | `com.zte.aiassistant.subagent.GameSubAgentService` | True | — | 1 |
| provider | `com.zte.gameassist.subagent.GameSubAgentProvider` | True | cn.nubia.gameassist.permission.ACCESS | 0 |
| provider | `com.zte.plugin.reminder.GameReminderProvider` | True | com.zte.gameassist.permission.GAMEREMINDER | 0 |
| receiver | `com.zte.plugin.reminder.AlarmReceiver` | True | com.zte.gameassist.permission.GAMEREMINDER | 6 |
| service | `com.zte.plugin.reminder.AlarmService` | True | — | 0 |
| activity | `com.zte.plugin.reminder.permission.RequestPermissionActivity` | False | — | 0 |
| activity | `com.zte.plugin.reminder.permission.RequestPermissionActivityBase` | False | — | 0 |
| receiver | `com.zte.plugin.reminder.widget.GameReminderWidget` | True | com.zte.gameassist.permission.GAMEREMINDER | 1 |
| service | `com.zte.plugin.reminder.widget.GameReminderWidgetService` | False | android.permission.BIND_REMOTEVIEWS | 0 |
| service | `cn.nubia.projection.ProjectionPanelService` | True | — | 1 |
| receiver | `com.zte.performanceindicator.widget.PerformanceIndicatorWidget` | True | — | 1 |
| provider | `com.zte.performanceindicator.provider.PerformanceIndicatorProvider` | True | — | 0 |
| activity | `com.zte.gameassist.permission.PermissionApplyActivity` | False | — | 0 |
| receiver | `com.zte.distbus.receiver.DistBusReceiver` | True | — | 0 |
| service | `com.google.mlkit.common.internal.MlKitComponentDiscoveryService` | False | — | 0 |
| provider | `com.google.mlkit.common.internal.MlKitInitProvider` | False | — | 0 |
| activity | `com.google.android.gms.common.api.GoogleApiActivity` | False | — | 0 |
| provider | `androidx.startup.InitializationProvider` | False | — | 0 |
| receiver | `com.zte.toolsdk.producer.ToolsReceiver` | True | — | 0 |
| receiver | `com.zte.toolsdk.producer.RemoteViewsReceiver` | None | — | 0 |
| provider | `com.zte.toolsdk.consumer.SharedPrefsContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.consumer.CardContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.producer.AnimationEndContentProvider` | True | — | 0 |
| service | `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` | False | — | 0 |
| receiver | `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver` | False | — | 0 |
| receiver | `androidx.profileinstaller.ProfileInstallReceiver` | True | android.permission.DUMP | 4 |
