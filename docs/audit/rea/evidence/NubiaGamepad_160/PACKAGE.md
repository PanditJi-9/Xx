# REA package declaration

- package: `cn.nubia.gamepad`
- version: `160.00.00.2605230254` (160000)
- sdk: min 28 / target 35
- engine: jadx-headless-mcp 0.7.1 (sha256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)
- classes 1134 · resources 1552 · permissions 21
- components: 15 declared (complete)

| kind | name | exported | permission | filters |
| --- | --- | --- | --- | --- |
| service | `cn.nubia.gamepad.GamepadService` | True | — | 1 |
| service | `cn.nubia.gamepad.GamepadCaseInsertService` | True | — | 1 |
| service | `cn.nubia.gamepad.HandStankService` | True | — | 1 |
| service | `cn.nubia.gamepad.motioncollection.MotionCollectionService` | True | — | 1 |
| provider | `cn.nubia.gamepad.provider.GamepadDataProvider` | True | cn.nubia.gamepad.permission.READ_PERMISSION | 0 |
| provider | `cn.nubia.gamepad.provider.FileProvider` | True | cn.nubia.gamepad.permission.READ_PERMISSION | 0 |
| activity | `cn.nubia.gamepad.GamepadSchemeListActivity` | True | — | 1 |
| service | `cn.nubia.gamepad.controlcenter.ControlCenterService` | True | — | 1 |
| service | `cn.nubia.camerakeytouch.CameraKeyVirtualTouchService` | True | — | 1 |
| provider | `cn.nubia.camerakeytouch.data.FileProvider` | True | cn.nubia.gamepad.permission.READ_PERMISSION | 0 |
| provider | `cn.nubia.camerakeytouch.data.CvtDataProvider` | True | — | 0 |
| service | `cn.nubia.tgk.allscene.TgkToastService` | True | — | 1 |
| provider | `cn.nubia.sensoroperation.provider.SensorDataProvider` | False | — | 0 |
| service | `cn.nubia.sensoroperation.SensorOperationService` | True | — | 1 |
| service | `cn.nubia.tpgamepartition.TGPMotionCollectionService` | True | — | 1 |
