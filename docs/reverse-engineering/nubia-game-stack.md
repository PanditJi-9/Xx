# الهندسة العكسية لحزمة ألعاب Nubia — ما قيس فعلًا

هذا الملف يوثّق نتائج تحليل ساكن لثلاثة ملفات APK من حزمة الألعاب في واجهة Nubia،
وهو مربوط بأدلّة مُنتَجة في GitHub Actions لا بتخمين محلي. لا شيء هنا شُغِّل على جهاز:
كل رقم مأخوذ من ملفّ في دليل الأدلّة المرفق مع التشغيل، أو من أداة في `tools/` تعمل في CI.

## كيف يُعاد التوليد

```
.github/workflows/reverse-engineer.yml       # التحليل: REA + apktool + jadx + التوقيع
.github/scripts/re_apk_analyze.sh            # خط الأنابيب لكل هدف
tools/re_apk_report.py                       # تركيب التقرير من الأدلّة
tools/re_apk_signature.py                    # هوية التوقيع (v1/v2/v3)
.github/workflows/gamespace-launch.yml       # ترقيع نقطة الدخول وإعادة التوقيع
tools/re_patch_launcher.py                   # أداة الترقيع نفسها
```

المدخلات تقرأ من **release مسوّدة** (`re-targets`) غير منشورة، فلا تُلتزم ملفات
APK في المستودع ولا تُنشر. التشغيل يدويّ (`workflow_dispatch`)؛ الحكم من سجلّ Actions.

## الأهداف كما قيست

| الملف | الحزمة | الإصدار | SDK | أصناف | موارد | SHA-256 |
| --- | --- | --- | --- | --- | --- | --- |
| `GameAssist.apk` | `cn.nubia.gameassist` | 16.5.000.2609232122 (16250908) | 33 → 36 | 8910 | 9389 | `7bd84186…53ef26a1` |
| `GameSpace_…2609231620.apk` | `cn.nubia.gamelauncher` | 16.0.000.000.2609231620 (261) | 33 → 36 | 5696 | 3294 | `22e29ee2…1302da6c` |
| `NubiaGameWidget.apk` | `cn.nubia.gamewidget` | 16.0.000.2609111451 (418) | 30 → 35 | 3091 | 5111 | `9339bc57…37ec94ba` |

الهوية الموقِّعة واحدة للثلاثة: شهادة `O=ZTE, OU=Smartphone Software Dept., CN=ZTE`،
تسلسل `CB8D6A9A457B6CC8`، بصمة `db4e3ab7…0ca051ce`، صالحة 2024-04-23 → 2051-09-09.
وقد قُرئت من v1 وv2 وv3 **واتفقت الثلاثة** — وهو تحقّق متقاطع لا ادّعاء.

## GameSpace — لماذا «تم التثبيت» ولا يفتح

السبب مقيس في البيان المفكوك: **لا يوجد أي مكوّن يعلن
`android.intent.category.LAUNCHER`**. أنشطة الدخول الثلاثة
(`GameSpaceActivity` · `HostModeGameSpace` · `LargeGameActivity`) تحمل
`android.intent.action.MAIN` وحدها، والفئتان المعلنتان في البيان كلّه هما
`DEFAULT` و`HOME`. فالتثبيت ينجح، ولا تُنشأ أيقونة، ولا توجد نقطة دخول من المشغّل.

الترقيع المطبَّق: إضافة `intent-filter` واحدة (MAIN + LAUNCHER) إلى
`cn.nubia.gamelauncher.activity.GameSpaceActivity`، وهي مُصدَّرة أصلًا
(`android:exported="true"`) فيمكن للمشغّل تشغيلها. لا يُعدَّل غيره، لأن أي تعديل
آخر سيكون تخمينًا عن جهاز لا تراه CI.

التطبيق يُوقَّع من جديد بمفتاح تصحيح، فلا يُثبَّت فوق النسخة الأصلية (تعارض توقيع):
يلزم إزالة القديم أولًا.

البناء المُنتَج: 165,922,816 بايت، SHA-256 `ce61603d…613123e6`، موقّع بـv1+v2+v3،
وقُرئت `android.intent.category.LAUNCHER` في البيان بعد إعادة الفك — أي أن الترقيع نجح في الملف المبني نفسه.

**عوائق التثبيت فُحصت في البيان:** لا `android:sharedUserId`، والثلاث `uses-library` كلها
`required="false"`، و`minSdk 33` / `targetSdk 36` ⇒ يُثبَّت على أي هاتف Android 13 أو أحدث.
توقيعه الآن مفتاح تصحيح، فلا يعلو نسخة ZTE — تُزال القديمة أولًا.

**وبوابات الجهاز قرئت في `smali` ولا تُرقَّع:** `DeviceUtils.isRedMagic()` هو
`Feature.getBoolean("ZTE_FEATURE_RED_MAGIC", false)`، و`FunctionAllocationHelper.isRedMagicDevice()`،
و`Util.isRedMagicRunOnMyOs()`. مواضع استعمالها الثلاثة **تختار نصًّا أو قيمة قائمة** (`if-eqz` بين
مورّدين)، ولا فرع `finish()` ولا رفض إقلاع — فجعلها `true` على جهاز آخر يوعد بميزات RedMagic غير موجودة.

**حدّ صريح:** «يفتح» مُتحقَّق في الـAPK (وجود LAUNCHER في البيان بعد إعادة البناء)،
أمّا «يعمل بكل ميزاته» فغير مُتحقَّق ولا يمكن ادّعاؤه: التطبيق يعلن 67 صلاحية نظام
(`INJECT_EVENTS` · `READ_FRAME_BUFFER` · `INTERNAL_SYSTEM_WINDOW` · `MANAGE_USERS` …)
ويفحص `isRedMagicDevice` و`ZTE_FEATURE_RED_MAGIC`، وكلها لا تُمنح لتطبيق مستخدم على
جهاز Xiaomi. فإن فتح ثم انهار، فسبب الانهيار يُقرأ من `logcat` ويُرقَّع بالدليل لا بالتخمين.

## GameAssist — مساعد داخل اللعبة يقرأ الشاشة

- 72 صلاحية، منها ما لا يُمنح إلا لتطبيق موقّع بمفتاح المنصّة: `INJECT_EVENTS` ·
  `READ_FRAME_BUFFER` · `INTERNAL_SYSTEM_WINDOW` · `MANAGE_ACTIVITY_STACKS` ·
  `STATUS_BAR` · `MANAGE_USERS` · `DELETE_PACKAGES` · `WRITE_SECURE_SETTINGS` ·
  `DEVICE_POWER` · `MONITOR_INPUT`.
- مكوّنات: 11 نشاطًا · 11 خدمة · 8 مستقبلات · 8 مزوّدات · 22 مُصدَّرًا.
- خدمات كاشفة: `GameAssistService` · `GameScreenSaverService` ·
  `ProjectionPanelService` (بثّ الشاشة) · `CustomNotificationListener` ·
  `GameAgentService` (وكيل ذكاء) · `InvestigationModeService`.
- مزوّدات محتوى مُصدَّرة: `FunctionCallProvider` · `GlobalSearchProvider` ·
  `LowSugarProvider` · `LowSugarOcrProvider` · `PerformanceIndicatorProvider`.
- مكتبات أصلية (كلها PIE+RELRO+NX): `libmlkit_google_ocr_pipeline.so` (10.55 MiB) و
  `libyoloXncnn.so` (10.25 MiB). ومعها أصول نماذج:
  `assets/yolox/lowsugar/lowsugar.bin` (35.8 MB) و`assets/vibrate.bin` (9.4 MB).
  أي أن المساعد **يقرأ الشاشة بصريًّا** (YOLOX + ML Kit OCR) لا عبر واجهات نظام فقط.
- أذونات داخلية مطلوبة من نظام Nubia: `cn.nubia.permission.PROCESS_MANAGER` ·
  `cn.nubia.gamehelper.permission.GAME_HELPER_CTRL` · `cn.nubia.gamelab.permission.gameocr`.

## GameSpace — ميزات ومسارات

- 67 صلاحية بنفس المجموعة عالية الامتياز، و28 نشاطًا و40 مكوّنًا مُصدَّرًا.
- نشاطات كاشفة: `GameSpaceActivity` · `HostModeGameSpace` · `AppAddActivity` ·
  `LargeGameActivity` · `TouPingActivity` (بثّ) · `ResourceLibraryActivity` ·
  `GameCenterActivity` · `GameRefreshRateActivity` · `GameChickenModeActivity` ·
  `RecordsActivity` · `GameManualActivity`.
- خدمات: `AimService` (مساعد تصويب) · `UpgradeJobService` · `GameEventService`.
- أذونات داخلية: `cn.nubia.launcher.permission.READ_SETTINGS` و`WRITE_SETTINGS` ·
  `cn.nubia.permissioncapsule.BIND_USAGE_STATS` · `cn.nubia.permission.SET_NUBIA_JOB`.
- ميزات مؤكَّدة نصًّا في الموارد: `aim_helper` = Aiming Assist، `fd_shock_title` = 4D Vibrate،
  `gamekey_lamp_switch` = RGB Gaming Light، `function_redmagic_quality_title`، `fps`.
- نقاط شبكة: `arkbase-api.nubia.com/gamecover/get_by_packagename` (أغلفة الألعاب) ·
  `h5-appstore.nubia.com/vip/vip.html`.

## NubiaGameWidget — ودجت وبثّ، لا شاشة

- 18 صلاحية، أهمّها `SYSTEM_ALERT_WINDOW` (نافذة طافية) و`REQUEST_INSTALL_PACKAGES`
  و`WRITE_SECURE_SETTINGS` و`MANAGE_USERS` و`STOP_APP_SWITCHES`.
- مكوّنات: **0 نشاط** · خدمة واحدة · 8 مستقبلات · مزوّدان · 9 مُصدَّرين.
  أي أنه لا يملك واجهة تُفتح أصلًا، بل مستقبلات تُنبَّه بها من النظام.
- لا مكتبات أصلية مضمَّنة.

## تشغيله على هاتف فعلي — قياس لا ادّعاء

التطبيق ثُبّت على `24129RT7CC` (Xiaomi, Android 16) عبر `adb 192.168.1.4:33995`، وأُخذ
تاريخ الانهيار من `logcat -b crash` ثم أُصلح:

| المحاولة | ما ظهر |
| --- | --- |
| 1 — بعد إضافة LAUNCHER فقط | `SecurityException: must have one of [WRITE_SECURE_SETTINGS]` من `CommonUtil.notifyHandheldModeChanged` داخل `CommonApplication.onCreate` |
| 2 — بعد إبطال تلك الدالة | نفس الاستثناء من `GameLauncherApplication.doConfig` |
| 3 — بعد لفّ الدوال | **لا انهيار**، و`topResumedActivity` = `cn.nubia.gamelauncher/.activity.ExperienceHostMode` |

الفكرة: الكتابة في `Settings.Global/System` تحتاج `WRITE_SECURE_SETTINGS`، وهي صلاحية لا تُمنح
لتطبيق موقّع بغير مفتاح المنصّة، و`pm grant` تردّها هذه النواة على الـshell. فبدل إسقاط العملية،
تُلفّ كل دالة `void` تكتب في الإعدادات بـ`catch SecurityException`.
العدد المقيس: **93 دالة في 50 ملفًا** لفّتها `tools/re_patch_smali.py --sweep`، وتُخطّي 14 فيها
`.catch` أصلًا و4 تُعيد قيمة (تغييرها يُغيّر السلوك بصمت).

والأخطاء المتبقية غير قاتلة ومتوقّعة: `WRITE_SETTINGS` مرفوضة (تُسجَّل تحذيرًا فقط)،
و`TurboSchedMonitor` لا يجد دالة sched على نظام ليس Nubia.

## الحجم

166 MB → **121.6 MB** بصيغة 7z (73%)، وSHA-256 للحزمة `fa250639…d7193c90`.
والغالب وسائط لا تُضغط: 52 MB PNG · 29 MB MP4 · 24 MB WAV — ولهذا لا يتجاوز التوفير الربع.

## حدود لا تُتجاوز

- تحليل ساكن فقط: لا تثبيت ولا تشغيل ولا رصد سلوك.
- التشويش (R8) يعيد تسمية الرموز، ولم يُوفَّر `mapping.txt`؛ الأسماء كما يعرضها المفكِّك.
- `jadx` أنهى بأخطاء: 2919 من 6144 صنفًا في GameAssist، و84 من 3708 في GameSpace،
  وعدد مماثل في الودجت — أي أن بعض الأجساد جزئية أو غائبة، والعدد في `jadx.stdout`.
- `inspect-android-jni-bindings` غير موجود في REA 6.7.0 المثبَّت، و`inspect-signature`
  يرد `capability_unavailable` لهذه الأهداف؛ لذلك يُقرأ التوقيع بأداة المستودع.
- ما يخصّ العتاد والسلوك على جهاز بعينه يبقى **يحتاج جهازًا**.
