# REA package declaration

- package: `cn.zte.gamefloat`
- version: `2.5.0001.2605230533` (250001)
- sdk: min 33 / target 36
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 4785 · resources 7609 · permissions 37
- components: 36 declared (complete)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| service | `cn.zte.gamefloat.precisionctl.PrecisionCtlService` | True | — | 1 |
| service | `cn.zte.gamefloat.report.GameReportService` | True | — | 1 |
| service | `cn.zte.gamefloat.aitip.AiTipService` | True | — | 1 |
| service | `cn.zte.gamefloat.imgsearch.ImageSearchService` | True | — | 1 |
| provider | `androidx.core.content.FileProvider` | False | — | 0 |
| provider | `cn.zte.gamefloat.chatassistant.db.ChatAssistantContentProvider` | True | — | 0 |
| activity | `cn.zte.gamefloat.chatassistant.ui.ChatAssistantSettingsActivity` | True | — | 1 |
| activity | `cn.zte.gamefloat.chatassistant.ui.DeleteDialog` | None | — | 0 |
| activity | `cn.zte.gamefloat.chatassistant.ui.AddRecorderActivityDialog` | None | — | 0 |
| activity | `cn.zte.gamefloat.chatassistant.ui.AddVoicePackActivityDialog` | None | — | 0 |
| activity | `cn.zte.gamefloat.chatassistant.ui.UploadTextActivity` | None | — | 0 |
| service | `cn.zte.gamefloat.chatassistant.ChatAssistantService` | True | — | 1 |
| service | `cn.zte.gamefloat.powerpanel.PowerPanelService` | True | — | 1 |
| service | `cn.zte.gamefloat.gamekeys.GameKeysService` | True | — | 1 |
| service | `cn.zte.gamefloat.equalizer.EqualizerService` | True | — | 1 |
| service | `cn.zte.gamefloat.gamecounter.GameCounterService` | True | — | 1 |
| receiver | `cn.zte.gamefloat.equalizer.DtsStateReceiver` | True | — | 1 |
| receiver | `cn.zte.gamefloat.receiver.GameForegroundChangeReceiver` | True | — | 1 |
| receiver | `cn.zte.gamefloat.receiver.GameModeReceiver` | False | — | 1 |
| receiver | `com.dts.dtssdk.DtsBroadcastManager` | True | — | 1 |
| receiver | `com.dts.dtssdk.util.BluetoothConnectionManager$BluetoothConnectionReceiver` | True | — | 1 |
| receiver | `com.dts.dtssdk.util.UsbConnectionManager$RouteChangeReceiver` | True | — | 1 |
| receiver | `com.dts.dtssdk.DtsCore$ServiceBroadcastReceiver` | False | — | 1 |
| provider | `cn.zte.gamefloat.equalizer.data.EqualizerProvider` | True | cn.zte.gamefloat.permission.READ_EQUALIZER | 0 |
| provider | `cn.zte.gamefloat.equalizer.data.PriviewFileProvider` | True | cn.zte.gamefloat.permission.READ_EQUALIZER | 0 |
| service | `cn.zte.gamefloat.wrp.WinRatePredictionService` | True | — | 0 |
| provider | `cn.zte.gamefloat.wrp.db.WinRateContentProvider` | True | — | 0 |
| service | `com.dts.dtssdk.util.NotificationService` | False | — | 0 |
| provider | `androidx.startup.InitializationProvider` | False | — | 0 |
| service | `androidx.room.MultiInstanceInvalidationService` | False | — | 0 |
| receiver | `androidx.profileinstaller.ProfileInstallReceiver` | True | android.permission.DUMP | 4 |
| receiver | `com.zte.toolsdk.producer.ToolsReceiver` | True | — | 0 |
| receiver | `com.zte.toolsdk.producer.RemoteViewsReceiver` | None | — | 0 |
| provider | `com.zte.toolsdk.consumer.SharedPrefsContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.consumer.CardContentProvider` | True | — | 0 |
| provider | `com.zte.toolsdk.producer.AnimationEndContentProvider` | True | — | 0 |
