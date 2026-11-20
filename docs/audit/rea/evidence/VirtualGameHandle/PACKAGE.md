# REA package declaration

- package: `cn.nubia.virtualgamehandle`
- version: `16.0.000.2605230000` (17260415)
- sdk: min 34 / target 37
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 3660 · resources 2140 · permissions 10
- components: 9 declared (complete)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| activity | `cn.nubia.virtualgamehandle.settings.VirtualHandleSpaceActivity` | False | — | 1 |
| activity | `cn.nubia.virtualgamehandle.settings.UnitTestActivity` | None | — | 0 |
| service | `cn.nubia.virtualgamehandle.GameHandleService` | True | — | 1 |
| provider | `cn.nubia.virtualgamehandle.db.GameHandleProvider` | True | — | 0 |
| provider | `cn.nubia.virtualgamehandle.OwlSettingsProvider` | True | — | 1 |
| receiver | `cn.nubia.virtualgamehandle.track.OwlSysTrackReceiver` | False | — | 1 |
| service | `cn.nubia.virtualgamehandle.track.OwlSysTrackService` | None | — | 0 |
| provider | `androidx.startup.InitializationProvider` | False | — | 0 |
| receiver | `androidx.profileinstaller.ProfileInstallReceiver` | True | android.permission.DUMP | 4 |
