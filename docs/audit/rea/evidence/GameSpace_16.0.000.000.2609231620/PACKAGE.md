# REA package declaration

- package: `cn.nubia.gamelauncher`
- version: `16.0.000.000.2609231620` (261)
- sdk: min 33 / target 36
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 5696 · resources 3294 · permissions 67
- components: 61 declared (complete)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| activity | `cn.nubia.gamelauncher.activity.GameSpaceActivity` | True | — | 1 |
| activity | `cn.nubia.gamelauncher.activity.HostModeGameSpace` | True | — | 1 |
| activity | `cn.nubia.gamelauncher.activity.ExperienceHostMode` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.activity.AppAddActivity` | True | — | 1 |
| activity | `cn.nubia.gamelauncher.activity.LargeGameActivity` | True | — | 1 |
| activity | `cn.nubia.gamelauncher.fragment.SplashScreen` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.activity.UninstallActivity` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.activity.AtmosphereActivity` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.activity.UpgradeDialogActivity` | True | — | 1 |
| provider | `cn.nubia.gamelauncher.db.AppAddProvider` | True | — | 0 |
| receiver | `cn.nubia.gamelauncher.receiver.BuryReceiver` | True | — | 1 |
| service | `cn.nubia.gamelauncher.aimhelper.AimService` | True | — | 1 |
| receiver | `cn.nubia.gamelauncher.aimhelper.StartSightAssistReceiver` | True | — | 1 |
| receiver | `cn.nubia.gamelauncher.receiver.TopGameReceiver` | True | — | 1 |
| receiver | `cn.nubia.gamelauncher.receiver.TopGameReceiver` | True | — | 1 |
| service | `cn.nubia.upgrade.service.UpgradeService` | False | — | 0 |
| service | `cn.nubia.upgrade.service.UpgradeJobService` | True | android.permission.BIND_JOB_SERVICE | 0 |
| provider | `cn.nubia.gamelauncher.upgrade.UpgradeFileProvider` | False | — | 0 |
| activity | `cn.nubia.gamelauncher.aimhelper.AimHelperPermissionDialogActivity` | False | — | 1 |
| receiver | `cn.nubia.gamelauncher.broadcast.GameEventReceiver` | True | com.tencent.inlab.tcsystem.permission.SOLARCORE_RECEIVER | 1 |
| receiver | `cn.nubia.gamelauncher.broadcast.GameLifeCycleChangeReceiver` | True | — | 1 |
| service | `cn.nubia.gamelauncher.broadcast.GameEventService$GameBroadcastService` | True | — | 1 |
| service | `cn.nubia.gamelauncher.broadcast.GameEventService` | True | — | 1 |
| activity | `cn.nubia.plug.PlugUnitActivity` | False | — | 1 |
| service | `cn.nubia.gamepanel.PowerPanelService` | True | — | 1 |
| receiver | `cn.nubia.gamepanel.GameForegroundChangeReceiver` | True | — | 1 |
| activity | `cn.nubia.studio.TouPingActivity` | True | — | 0 |
| provider | `cn.nubia.globalsearch.GlobalSearchProvider` | True | — | 0 |
| activity | `cn.nubia.resourcelibrary.ResourceLibraryActivity` | True | — | 1 |
| service | `cn.nubia.tgk.TgkService` | True | — | 1 |
| provider | `cn.nubia.tgk.data.FileProvider` | True | cn.nubia.tgk.permission.FILE_PROVIDER | 0 |
| service | `cn.nubia.gamepad.GamepadService` | True | — | 1 |
| provider | `cn.nubia.tgk.data.TgkDataProvider` | True | — | 0 |
| activity | `cn.nubia.tgk.TgkFilePickActivity` | True | — | 1 |
| provider | `cn.nubia.resourcelibrary.ResourceLibProvider` | False | — | 0 |
| service | `cn.nubia.gamelauncher.service.GameFeatureService` | True | — | 0 |
| provider | `cn.nubia.gamelauncher.gamecontrolpanel.EnergyCubeProvider` | True | — | 0 |
| activity | `cn.nubia.studio.TouPingGravitationActivity` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.xgravitation.XGravitationActivity` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.redmagicplanet.ui.BehaviorLearnedActivity` | None | — | 0 |
| activity | `cn.nubia.gamelauncher.redmagicplanet.ui.GameAgentActivity` | None | — | 0 |
| activity | `cn.nubia.gamecenter.settings.GameCenterActivity` | True | — | 1 |
| activity | `cn.nubia.gamecenter.settings.GameCarrerActivity` | True | — | 1 |
| activity | `cn.nubia.gamecenter.settings.recordSettings.GameRecordActivity` | False | — | 1 |
| activity | `cn.nubia.gamecenter.settings.records.activity.RecordsActivity` | True | — | 1 |
| activity | `cn.nubia.gamecenter.settings.recordSettings.GameManualActivity` | True | — | 1 |
| activity | `cn.nubia.gamecenter.settings.net.ManagerAcceleratedAppActivity` | None | — | 0 |
| activity | `cn.nubia.gamecenter.settings.barrageMessage.BarrageMessageSourceActivity` | None | — | 0 |
| activity | `cn.nubia.gamecenter.settings.basic.GameChickenModeActivity` | True | — | 1 |
| activity | `cn.nubia.gamecenter.settings.wallpaper.WallpaperCropActivity` | None | — | 0 |
| receiver | `cn.nubia.settings.owlsysaciton.OwlSysCvReceiver` | True | — | 1 |
| provider | `cn.nubia.gamecenter.settings.OwlSettingsProvider` | True | — | 1 |
| provider | `cn.nubia.gamecenter.settings.GameCenterFileProvider` | False | — | 0 |
| provider | `cn.nubia.gamecenter.settings.recordsdb.RedMagicTimeProvider` | True | — | 0 |
| receiver | `cn.nubia.gamecenter.settings.net.WifiStateChangedReceiver` | True | — | 1 |
| service | `cn.nubia.settings.owlsysaciton.OwlSysCvInitService` | None | — | 0 |
| activity | `cn.nubia.gamecenter.settings.basic.GameRefreshRateActivity` | True | — | 1 |
| receiver | `cn.nubia.gamecenter.settings.applearning.AppLearningReceiver` | True | — | 1 |
| service | `cn.nubia.gamecenter.settings.applearning.AppTimeLockService` | None | — | 0 |
| receiver | `cn.nubia.gamecenter.settings.barrageMessage.PackageChangeReceiver` | True | — | 2 |
| provider | `androidx.startup.InitializationProvider` | False | — | 0 |
