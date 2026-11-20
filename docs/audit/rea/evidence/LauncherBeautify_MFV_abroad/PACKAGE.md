# REA package declaration

- package: `com.zte.beautify`
- version: `16.1.000.000.2605231217` (161000)
- sdk: min 33 / target 36
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 7849 · resources 7759 · permissions 31
- components: 26 declared (partial)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| activity | `com.zte.beautify.BeautifyActivity` | True | theme.permission.LOCAL_RESOURCE | 2 |
| activity | `com.zte.beautify.view.common.preview.BeautyPreviewActivity` | False | zte.permission.PUSH_MESSAGE_3e4f5a41 | 2 |
| activity | `com.zte.beautify.view.common.preview.FullscreenPreviewActivity` | False | — | 1 |
| activity | `com.zte.beautify.view.common.gallerypreivew.GalleryPreivewActivity` | False | — | 0 |
| activity | `com.zte.beautify.BeautifyWallpaperActivity` | True | theme.permission.LOCAL_RESOURCE | 2 |
| activity | `com.zte.beautify.StyleActivity` | True | — | 2 |
| activity | `com.zte.beautify.IconChooseActivity` | True | theme.permission.LOCAL_RESOURCE | 2 |
| activity | `com.zte.beautify.FontSettingActivity` | True | theme.permission.LOCAL_RESOURCE | 0 |
| activity | `com.zte.beautify.NewStyleWallpaperActivity` | True | theme.permission.LOCAL_RESOURCE | 2 |
| activity | `com.zte.beautify.OnlineInnerActivity` | False | zte.permission.PUSH_MESSAGE_3e4f5a41 | 1 |
| activity | `com.zte.beautify.view.common.tools.NFCSettingActivity` | True | — | 2 |
| activity | `com.zte.beautify.view.beauty.AIWallpaper.AIWallpaperActivity` | True | — | 1 |
| activity | `com.zte.beautify.view.beauty.AIWallpaper.AIWallpaperResultDisplayActivity` | None | theme.permission.LOCAL_RESOURCE | 0 |
| activity | `com.zte.beautify.view.beauty.AIWallpaper.AIWallpaperSplashActivity` | True | theme.permission.LOCAL_RESOURCE | 0 |
| activity | `com.zte.beautify.view.common.tools.TransLiveWallpaperActivity` | True | — | 1 |
| receiver | `com.zte.beautify.view.common.set.KeySlideReceiver` | True | — | 1 |
| provider | `com.zte.beautify.service.ZteThemeSearchIndexablesProvider` | True | android.permission.READ_SEARCH_INDEXABLES | 1 |
| activity | `com.zte.beautify.view.beauty.AIWallpaper.PadAbroadAIWallpaperActivity` | True | — | 0 |
| activity | `com.zte.beautify.view.common.tools.NullActivity` | True | — | 2 |
| receiver | `com.zte.beautify.view.common.set.SetWallPaperBroadcastReceiver` | True | theme.permission.receivechange | 1 |
| receiver | `com.zte.beautify.model.local.utils.CommonActionReceiver` | True | — | 1 |
| receiver | `com.zte.beautify.service.AlarmManagerReceiver` | False | — | 1 |
| receiver | `com.zte.beautify.service.GpsChangeReceiver` | True | — | 1 |
| service | `com.zte.beautify.view.common.preview.tryuse.RealResetResourceJobService` | False | android.permission.BIND_JOB_SERVICE | 0 |
| provider | `com.zte.beautify.model.download.DldThemeResProvider` | True | theme.permission.READ_DOWNLOADED_RESOURCE | 0 |
| provider | `com.zte.beautify.model.FileProvider` | True | — | 0 |
