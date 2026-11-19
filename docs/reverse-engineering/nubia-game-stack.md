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

## GameAssist — سلسلة الانهيار المقيسة (حتى 2026-11-18)

كل سطر ظهر في `adb logcat -b crash` بعد تثبيت البناء السابق؛ ولا سطر منها استنتاج.

| # | الانهيار | الإصلاح | النتيجة |
| --- | --- | --- | --- |
| 1 | `NoClassDefFoundError: GameAssistWrapper$Callback` في `Utils.<clinit>` | بدائل أصناف في `tools/re_stubs/gameassist/` (ترجع `null`/`false`/قائمة فارغة) | زال |
| 2 | `NullPointerException` على `GameCheck$GameLauncherObserver.c` لأن `GameCheck.c` الساكن فارغ | `--mode stub --return empty-list` على محدِّد الوصول | زال |
| 3 | `NoSuchMethodError: NotificationManager.setNotificationListenerAccessGranted` | `--mode wrap --catch nosuchmethod` على `NotificationManagerWrapper` | زال |
| 4 | NPE على `cn.nubia.componentcenter.service.GameAssistComService.a(Class)` | `--mode wrap --catch throwable` على `GameAssistApplication.onCreate` | `onCreate` **صار يكتمل** |
| 5 | نفس NPE من `VolumeSeekbarViewController.<init>` عبر `ThemeController` | — | **حدّ**: لوحة المساعد تُبنى من مركز مكوّنات Nubia |

**الحدّ:** `cn.nubia.componentcenter` (و`permissioncapsule` و`launcher`) حزم نظام من صورة Nubia؛ لا
تُثبَّت كتطبيق مستخدم ولا يُوفّرها repack. فما يمكن إصلاحه في كود التطبيق أُصلح، وما يعتمد على تلك
الخدمات لا يُصلَح — ويُعلن لا يُخفى.

## GameFloat — طريق اللوحة العائمة، مقيسًا حتى السقف (2026-11-19)

كل سطر أدناه من `adb logcat` على الجهاز نفسه بعد تثبيت البناء المرقّع، لا استنتاج.

| # | القياس | النتيجة |
| --- | --- | --- |
| 1 | بثّ `cn.zte.gamefloat.powerpanel.action.FOREGROUND_GAME_CHANGE` مع extra نصّي `packageName` إلى `cn.zte.gamefloat/.receiver.GameForegroundChangeReceiver` | **استُقبل** — المفتاح الصحيح للطافية |
| 2 | البوابة `ConfigUtil.isAppEnable` تقرأ `Settings.Global.redmagic_ce_switch` (فارغ عندنا، و`shell` لا يكتبه) | رُقّعت لترجع `true` ⇒ `Panel_Service : MSG msg.what = 0` |
| 3 | `PowerPanelService$2.handleMessage:105` ← `PowerPanelWindowManager.createPowerPanelView:33` ← `PowerPanelDetailsView.<init>:118` | `onCreate PowerPanelDetailsView` — **اللوحة أُنشئت فعلًا** |
| 4 | `InputChannelCompat.initInputMonitor` → `NoSuchMethodError: myInput(String,Context)InputChannel` | `InputChannelWrapper.register` يحمل `.catch` أصلًا؛ إضافة نطاق try فوقه **تقاطع غير صالح** في dex (المُفكِّك يرفض: `refusing to wrap … it already declares a .catch`) ⇒ حُيِّد الجسم كاملًا (`--mode noop`) |
| 5 | `PowerPanelDetailsView.initLayoutParams:206` → `RuntimeException: Adding window failed` / `SecurityException: DisplayPolicy: uid 10767 does not have android.permission.INTERNAL_SYSTEM_WINDOW` | **السقف** |

**تفصيل السقف:** نوع النافذة مقيس من `dexdump`: `const/16 v3, #int 2038` عند `+0x13` من
`initLayoutParams` (أي `TYPE_APPLICATION_OVERLAY`، لا نوع نظامي)، و`appops get cn.zte.gamefloat
SYSTEM_ALERT_WINDOW` يعيد `allow` (وجُرِّب `appops set --uid …` أيضًا). ومع ذلك يرفض `DisplayPolicy`
بطلب `INTERNAL_SYSTEM_WINDOW` — وهي صلاحية `signature|privileged` لا تُمنح لتطبيق مستخدم على MIUI.
⇒ **لا يُحلّ بترقيع smali**؛ يحتاج تثبيتًا كنظام/بصلاحيات platform. وهذا يُعلن لا يُخفى.

### GameSpace 16.5 — المدخل

`ExperienceHostMode` في 16.5 **غير مُصدَّر**: `am start` يرد
`SecurityException: Permission Denial: … not exported from uid 10762`. المدخل المُصدَّر العامل هو
`cn.nubia.gamelauncher/.activity.GameSpaceActivity` (مقيس: النافذة صارت `mCurrentFocus` بلا انهيار).
وانهيار `recycled bitmap` السابق لم يتكرّر في هذه الجولة (لم يظهر في `-b crash` بعد التشغيل).

### فرضيتان سقطتا بالقياس (2026-11-19)

بعد أن رُفضت النافذة، جرّبت فرضيتين مقيستان لا مُفترضتين، وكلتاهما **سقطت**:

| # | الفرضية | كيف نُفّذت | النتيجة |
| --- | --- | --- | --- |
| 1 | الرفض سببه **نوع النافذة** | كتابة `lp.type = 2038` بعد بناء الكائن مباشرة (`tools/re_force_window_type.py`)؛ التأكد من وجود الكتابة في dex البناء: `LayoutParams;.type:I` عددها **24** | الرفض **لم يتحرك**: نفس `INTERNAL_SYSTEM_WINDOW` |
| 2 | الرفض سببه **إعلان الصلاحية** في البيان | استبدال اسم `android.permission.INTERNAL_SYSTEM_WINDOW` بنفس الطول داخل string pool (`tools/re_scrub_manifest_permission.py`)، مع إثبات أن البيان كان مطابقًا بايتًا قبل الاستبدال | الرفض **لم يتحرك** كذلك |

⇒ الحكم: `DisplayPolicy` في هذا الـROM يطلب `INTERNAL_SYSTEM_WINDOW` بغضّ النظر عن نوع النافذة وعن الإعلان.
فهي صلاحية `signature|privileged` تُحسم عند التثبيت لا عند التشغيل، ولا يبلغها ترقيع dex.

### المسار البديل القائم داخل تطبيقنا (مقيس، لا مُقترح)

لوحة GameFloat نفسها لا يمكن نافذتها على MIUI، لكن **النظير موجود عندنا أصلًا**:

| الملف | ما يفعله | النوع |
| --- | --- | --- |
| `manager/app/src/main/java/nd/max/service/GameTurboStrip.kt` | شريط تراكب فوق لعبة متتبَّعة | `TYPE_APPLICATION_OVERLAY` |
| `manager/app/src/main/java/nd/max/ui/component/GamePanelCockpit.kt` | لوحة تعيش في نافذة خدمة بلا `AlertDialog` | `TYPE_APPLICATION_OVERLAY` |
| `manager/app/src/main/java/nd/max/ui/overlay/OverlayWindow.kt` | نافذة التراكب المشتركة | `TYPE_APPLICATION_OVERLAY` |

والقياس على هذا الجهاز نفسه: `appops get nd.max SYSTEM_ALERT_WINDOW` = **allow**، و`nd.max` مثبّت.
أي أن نوع النافذة الذي رفضه MIUI لـ`cn.zte.gamefloat` **مسموح لتطبيقنا** — الفرق في الهوية لا في النوع.

**وحدّ هذا القياس:** `allow` شرط لازب لا كافٍ؛ لم يُرصد بعد رسم فعلي لنافذة `nd.max` في هذه الجولة،
فلا يُكتب «يعمل» بل «مسموح، والرسم يحتاج رصدًا».

## ما ثُبِّت من `/red/apps` وقياسه (2026-11-19)

ثُبِّتت الحزم المتصلة بالحزمة كلها بأمر المالك، وكلٌّ منها قيس بـ`adb install` ثم `pm list packages`
(الفرق قبل/بعد يكشف اسم الحزمة، لأن البيان المُصرَّف لا يُقرأ نصيًّا).

| المجلد | الحزمة | النتيجة |
| --- | --- | --- |
| `GameAIAsst` | `cn.zte.gameaiasst` | جديد |
| `GameCardAssist` | `com.zte.gamecardassist` | جديد |
| `GameHelperLine` | `cn.nubia.gamehelperline` | جديد |
| `GamePi` | `cn.nubia.gamepi` | جديد |
| `GamePluginTrigger` | `com.zte.game.plugintrigger` | جديد |
| `NBGameCenter_Abroad` | `com.nubia.gamecenter` | جديد |
| `NubiaGameHelperModule` | `cn.nubia.gamehelpmodule` | جديد |
| `NubiaGameNotes` | `cn.nubia.gamenotes` | جديد |
| `VirtualGameHandle` | `cn.nubia.virtualgamehandle` | جديد |
| `NubiaGameWidget` | `cn.nubia.gamewidget` | تحديث (كان مثبّتًا) |
| `NubiaGamepad_160` | `cn.nubia.gamepad` | تحديث (كان مثبّتًا) |

**وقياس ما بعد التثبيت:**

- `GameSpace` ما زال يعمل: `mCurrentFocus` صار `cn.nubia.gamelauncher/.activity.GameSpaceActivity`،
  و`-b crash` **فارغ**.
- `cn.nubia.componentcenter` **ما زال غير موجود**: صفر حزمة بهذا الاسم، وصفر إشارة إليه في
  `com.nubia.gamecenter` و`cn.nubia.gamehelpmodule`. فحاجز GameAssist لم يتحرك بالتثبيت.
- `cn.nubia.gameassist` **لا يملك Activity بـLAUNCHER**: `monkey -p … LAUNCHER` يرد
  «No activities found to run». خدماته الأربعة كلها محميّة بـ`cn.nubia.gameassist.permission.ACCESS`،
  وواحد فقط (`GameSubAgentService`) غير محميّ.

**وأيها يُفتح بالضغط عليه؟** قيس بـ`cmd package resolve-activity --brief -a MAIN -c LAUNCHER -p <pkg>`:
واحد فقط من الأحد عشر يملك مدخلًا — `com.nubia.gamecenter` ← `zte.com.market.LoginActivity`.
الباقي (بما فيه GameAssist وGameFloat) **خدمات ومكوّنات تُستدعى من غيرها**، فلا مدخل لها ولا معنى
للابحث عن أيقونة لها في الدرج.

## السبب الجذريّ المقيس لرفض لوحة GameFloat (2026-11-19)

`dexdump` على الـAPK **المثبَّت** أظهر أن `PowerPanelDetailsView.initLayoutParams` يبني
النافذة بـ`type = 2038` أصلًا (`const/16 v3, #int 2038` ثم `LayoutParams.<init>(III)V`)، وأن
كتابتنا المفروضة للنوع موجودة بعده (`const/16 v4, #int 2038; iput v4, v0, …->type:I`).
أي أن النوع لم يكن المشكلة في أي لحظة — ولا في المحاولة الأولى ولا الثانية.

والسبب في AOSP `DisplayPolicy.validateAddingWindowLw`:

```java
if ((attrs.privateFlags & PRIVATE_FLAG_TRUSTED_OVERLAY) != 0) {
    mContext.enforcePermission(INTERNAL_SYSTEM_WINDOW, callingPid, callingUid, "DisplayPolicy");
}
```

وهو **نصّ الرفض نفسه** الذي قرأناه في السجل. والبتّ يضعه مُغلِّف ZTE:
`WindowManagerWrapper$LayoutParams.addHidePrivateTrustedOverlayFlags(lp)`، ويُستدعى في
`initLayoutParams` **بعد** ضبط النوع ومباشرةً قبل `addView`. فالمحكوم عليه لم يكن النوع
ولا إعلان الصلاحية في البيان، بل `privateFlags` — ولهذا لم يتحرّك الرفض بأيٍّ من الترقيعين.

**الإصلاح:** حذف نداء `addHidePrivateTrustedOverlayFlags` من الـsmali
(`python3 tools/re_strip_invoke.py --needle addHidePrivateTrustedOverlayFlags`). الدالة
تُعيد `void` ولا تفعل شيئًا غير إضافة أعلام خاصّة مميَّزة، فحذف النداء يُبقي اللوحة نافذة
تراكب عادية `2038` بلا البتّة المحجوزة للتطبيقات الموقّعة بالمنصّة — وبلا حاجة إلى
صلاحية توقيع ولا إلى جذر.

**ولغير الجذريين:** ما يبقى مطلوبًا هو منح `cn.zte.gamefloat` صلاحية التراكب
(`SYSTEM_ALERT_WINDOW`) من إعدادات النظام — إجراء مستخدم عادي، لا جذر فيه.

## ما صار يعمل بعد حذف نداء trusted-overlay (2026-11-19)

بعد ترقيع `addHidePrivateTrustedOverlayFlags` (التزام `590e2b6d`، تشغيل Actions
`44428041667`، ثم تثبيت الأرتيفاكت والقياس على الجهاز):

- **`INTERNAL_SYSTEM_WINDOW` لم يُرفع مرّة واحدة** بعد الترقيع — لا في `logcat` ولا في
  `-b crash`. الجدار الأول سقط بالقياس.
- **النافذة تُبنى وتُرسم وتُعرض فعلًا** على جهاز Xiaomi بلا جذر وبلا صلاحية توقيع:
  `wms.showSurfaceRobustly mWin:Window{eb8c233 u0 PowerPanelWindow}`،
  `vri.reportDrawFinished … Rect(402, 150 - 792, 241)`، ومحتواها 390×91.
- ثم تُغلق بعد ~١٢ ملّي ثانية بـ`MSG msg.what = 1` → `removePowerPanelView`؛ وهذا
  **مسار الإخفاء في تطبيق ZTE نفسه**، لا رفض من النظام.

### ما يبقى، مسمّى لا مُجمَّل

1. **حفظ اللعبة في `Settings.Global`:** الكتابة تحتاج `WRITE_SECURE_SETTINGS` (مقيس:
   `SecurityException` متكرّر من `DatabaseUtils`). **ولا يمنع ذلك وصول الاسم:** القياس
   الأخير طبع `uploadPowerPanelData: gamePkg = com.ddup.zombiewaves.zw` — فالـextra يصل
   والخدمة تحفظه في حقلها، والفشل محصور في مرآة الإعدادات وحدها.
2. **بدء الخدمة من الخلفية:** أندرويد 12+ يرفض `startservice` لتطبيق حديث التثبيت
   (`BackgroundServiceStartNotAllowedException`)؛ والتغلب المؤقّت كان بجلب عملية التطبيق
   إلى المقدمة أولًا. الحل الدائم يحتاج `FOREGROUND_SERVICE` صريحًا أو إشعارًا دائمًا.
3. **شرط الأمامية:** اللوحة تُعرض ثم تُخفى إن لم تكن اللعبة هي الأمامية فعلًا — وهذا سلوك
   التطبيق لا عطب ترقيع.

### التوقيع

كل نسخ الترقيع (GameFloat · GameAssist · GameSpace) تُوقَّع الآن بمفتاح واحد يُستعاد من
كاش Actions (`nubia-patch-signing-v1`)، فبصمتها `C=EG, O=MaxManager, CN=Nubia Patch`
ثابتة وتُثبَّت النسخة الجديدة فوق القديمة بلا حذف. والتوقيع الأصلي لـZTE غير قابل للإنتاج:
مفتاحه الخاصّ ليس عندنا.

### آخر قياس (2026-11-19 · 02:27)

`MSG msg.what = 0` → النافذة تُبنى وتُرسم وتُعرض (`wms.showSurfaceRobustly`)، ثم
`MSG msg.what = 1` بعد ٨٠ ملّي ثانية → `removePowerPanelView`. وفي الوقت نفسه يعلن
`uploadPowerPanelData: gamePkg = com.ddup.zombiewaves.zw` — أي أن اسم الحزمة وصل صحيحًا.

فكل جدران الترقيع سقطت: لا رفض صلاحية، ولا رفض نوع، ولا فقدان للـextra. والذي يقرّر بقاء
اللوحة هو **شرط التطبيق نفسه**: التطبيق الأمامي حينها كان `app.komikku` (قارئ كتب) لا
اللعبة، وهو يُخفي لوحته حين لا تكون اللعبة أمامية — سلوكه المقصود لا عطب.

ولذلك لا تُكتب النتيجة «اللوحة تبقى معروضة»: تُكتب «اللوحة تُبنى وتُرسم وتُعرض، ويُبقيها
شرط الأمامية» — وهذا الفرق بين ترقيع ناجح وادّعاء. ولاحظ أن اللعبة المستهدفة
(`com.ddup.zombiewaves.zw`) مثبّتة لكن `resolve-activity` يرد `No activity found`، فلا
مدخل لها من ADB لإبقائها أمامية في هذه البيئة.

## الحسم: اللوحة تعمل بلا جذر (2026-11-19 · 02:45)

السلسلة كاملةً، وكل حلقة منها مقيسة على الجهاز:

1. **حذف `addHidePrivateTrustedOverlayFlags`** (التشغيل `44428041667`) ⇒ سقط رفض
   `INTERNAL_SYSTEM_WINDOW` نهائيًا.
2. **تعطيل `PowerPanelService$1.run`** (التشغيل `44430349402`) ⇒ سقط «send stop service»
   الفوريّ الذي كان يقتل اللوحة بعد ~٨٠ ملّي.
3. **منح صلاحية التراكب** ⇒ `Settings.canDrawOverlays` صار `true`، فيسلك `onStartCommand`
   فرع العرض (`type == 0`) ويُنادي `sendMessageDelayed(0, 20)` ثم `MSG msg.what = 0`.

قياس البقاء بعد ذلك (`am startservice … --ei type 0 --es currentPkg <pkg>`):
`dumpsys window | grep -c PowerPanel` = **6** عند ١ث و٣ث و٦ث و**١١ث** — والنوافذ تُرسم
باستمرار (`BufferQueue … queueBuffer fps=1.0–2.0`). ولا `INTERNAL_SYSTEM_WINDOW` ولا
`SecurityException` واحدة.

وتصحيح لما سبق: فشل العرض في القياسات الوسيطة كان لأن **إعادة التثبيت تُعيد الـappop إلى
`ignore`**؛ و`canDrawOverlays=false` يجعل التطبيق نفسه يرسل الإخفاء (الفرع `:cond_1`)، لا
لأن الترقيع ناقص.

### وما يلزم أي مستخدم — بلا جذر

1. تثبيت `GameFloat-panel.apk` من الأرتيفاكت (موقّع بالمفتاح الثابت `CN=Nubia Patch`،
   فتُحدَّث النسخ فوق بعضها بلا حذف).
2. منح `cn.zte.gamefloat` صلاحية **«العرض فوق التطبيقات الأخرى»** من إعدادات النظام —
   مفتاح عادي للمستخدم، لا `su` ولا Shizuku ولا `WRITE_SECURE_SETTINGS`.

بعد ذلك يُستدعى العرض بالأمر القياسي:
`am startservice -n cn.zte.gamefloat/.powerpanel.PowerPanelService -a ACTION_FOREGROUND_PKG_CHANGE --es currentPkg <game> --ei type 0 --ei powerPanelDialogShow 1`
(`type = 0` عرض · `type = 1` إخفاء مسموح به · أي قيمة أخرى: لا شيء).
