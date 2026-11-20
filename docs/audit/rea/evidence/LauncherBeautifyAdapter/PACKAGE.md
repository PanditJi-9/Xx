# REA package declaration

- package: `com.zte.beautifyadapter`
- version: `15.0.010.001.2604161654` (15010)
- sdk: min 31 / target 35
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 165 · resources 31 · permissions 15
- components: 6 declared (partial)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| provider | `com.zte.beautifyadapter.provider.FileProvider` | True | — | 0 |
| service | `com.zte.beautifyadapter.ThemeApplyService` | True | com.zte.beautifyadapter.applytheme | 1 |
| service | `com.zte.beautifyadapter.tryuse.TryResourceJobService` | False | android.permission.BIND_JOB_SERVICE | 0 |
| service | `com.zte.beautifyadapter.tryuse.ResetResourceJobService` | False | android.permission.BIND_JOB_SERVICE | 0 |
| service | `com.zte.beautifyadapter.helper.SerialWallpaperSetService` | False | android.permission.BIND_JOB_SERVICE | 0 |
| receiver | `com.zte.beautifyadapter.BootCompletedReceiver` | True | — | 1 |
