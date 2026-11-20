# ECOSYSTEM_MODULES — خريطة الوحدات وحدودها

> مخرج المرحلة 4. كل صفّ هنا يقابل **تطبيقًا أصليًّا مقيسًا** (أو دمجًا مُعلَّلًا) وأرقامه من
> `docs/audit/rea/evidence/*/components.json` و`docs/FEATURE_CATALOG.md` و`docs/audit/RESOURCE_INVENTORY.md`.
> الحزمة `redlab-core` هي الوحيدة التي لا تقابل تطبيقًا؛ مبرّرها في §1.

---

## 1. الوحدة المشتركة الوحيدة

| الوحدة | النوع | ما فيها | ما ليس فيها |
| --- | --- | --- | --- |
| `redlab-core` | AAR تُضمَّن في كل تطبيق | `CapabilityState` · `CapabilityRegistry` · `HardwareControlArbiter` (أولوية SAFETY) · `ContractVersion` · `SessionContract` · `Diagnostics` | لا شبكة، لا UI، لا وصول عتاد، لا معرفة بأي تطبيق |

**لماذا AAR لا خدمة:** خدمة مركزية تعني وجودًا دائمًا لعملية، وفشلًا واحدًا يُسقط الجميع، وسطح
اعتماد مخصّص. والمقيس (`docs/audit/APP_DEPENDENCY_GRAPH.md`) أن التطبيقات الأصلية **لا تتشارك أصنافًا** —
فتضمين مكتبة صغيرة أرخص وأصدق.

---

## 2. الوحدات المقابلة للتطبيقات (23 أصلًا → 14 وحدة)

| # | وحدة | الحزمة | الأصل (مكوّنات) | الدور | لماذا وحدة مستقلة |
| --- | --- | --- | --- | --- | --- |
| 1 | `gamespace` | `dev.redlab.gamespace` | `GameSpace` **66** (14 act · 8 svc · 13 rcv · 9 prv) | مكتبة الألعاب، الجلسة، لوحة الأداء | أكبر سطح شاشات + `GameEventService` = مصدر الجلسة |
| 2 | `gameassist` | `dev.redlab.gameassist` | `GameAssist` **46** (2 act · 8 svc · 8 rcv · 10 prv) | التراكب الرئيسي + الوكيل الصوتي + التعدّد الشاشي | أكثر عقود providers (10) وخدمات (8) في المنظومة |
| 3 | `floatwindow` | `dev.redlab.floatwindow` | `GameFloatWindow` **36** (1 act · 10 svc · 7 rcv · 7 prv) | النافذة العائمة، لوحة الطاقة، المعادل، مساعدات AI | 10 خدمات تعمل أثناء اللعب — تُعزل عن التراكب |
| 4 | `gamepad` | `dev.redlab.gamepad` | `Gamepad_160` **15** + `VirtualGameHandle` **9** | يد اللعب، الأزرار الافتراضية، المقبض | كلاهما يشتركان في **اسم provider منطقي** `GameHandleProvider` ⇒ تنظيمهما في وحدة واحدة يمنع تضارب ملكية البيانات |
| 5 | `highlights` | `dev.redlab.highlights` | `NBHighLights_RedMagic` **18** (5 svc) | تسجيل/قصّ اللقطات وزر عائم للتصوير | 5 خدمات تسجيل — أعلى طلب صلاحيات وسائط في المنظومة |
| 6 | `notes` | `dev.redlab.notes` | `NubiaGameNotes` **11** (1 act · 2 svc · 2 prv) | ملاحظات عائمة واستراتيجيات | كيان بيانات مستقل (`GameNotesProvider` + `StrategyStationProvider`) |
| 7 | `widget` | `dev.redlab.widget` | `NubiaGameWidget` **11** (8 rcv · 1 prv · **0 svc**) | Widgets ومؤشرات على الشاشة الرئيسية | **لا عملية تعمل** — دمجها مع أي خدمة يُنشئ عملية بلا سبب |
| 8 | `helperline` | `dev.redlab.helperline` | `GameHelperLine` **11** (2 svc · 5 rcv) + `NubiaGameHelperModule` **8** (3 svc) | خط المساعدة/التصويب + وحدة المساعدة | `AimService`/`LineService`/`CoreService`/`SyncService` تعمل كمجموعة واحدة في اللعب |
| 9 | `gamepi` | `dev.redlab.gamepi` | `GamePi` **2** (1 svc · 1 rcv) | خدمة أداء صغيرة | مكوّنان فقط ⇒ وحدة صغيرة مقصودة، لا تُدمج لتضخيم غيرها |
| 10 | `xr` | `dev.redlab.xr` | `XRLauncher` **9** (4 act · 1 svc) | وضع XR/التحكم عن بعد | يعتمد على `UnityPlayerActivity` وسطح شاشة مختلف ⇒ عزل |
| 11 | `wallpaper` | `dev.redlab.wallpaper` | `RedMagicWallPaper_Abroad` **22** (6 act · 1 svc · 1 prv) | خلفيات وبوسترات الألعاب | تطبيق معرض كامل، لا علاقة له بالجلسة |
| 12 | `theme` | `dev.redlab.theme` | `LauncherBeautify_MFV_abroad` **26** + `LauncherBeautifyAdapter` **6** + `LauncherResource_MyOSAbroad` **0** | سمة/أيقونات/خطوط + حزمة موارد | حزمة الموارد **صفر مكوّنات** بقرار الأصل: بيانات فقط |
| 13 | `launcher-adapter` | `dev.redlab.launcheradapter` | `LauncherAdapter_MFV` **3** + `Launcher_MFV_abroad` **54** | واجهة مشغّل + محوّل بياناته | `Launcher_MFV_abroad` سطح ضخم (17 prv) لكنه **مشغّل عام** لا جزء من اللعب ⇒ وحدة منفصلة صريحة |
| 14 | `store` | `dev.redlab.store` | `NBGameCenter_Abroad` **149** | تنزيل/تحديث الألعاب | أكبر بيان في المنظومة؛ **نطاقه مُقلَّم** في §4 |

### 2.1 تطبيقات مقيسة بلا وحدة (قرار مُعلَن)

| الأصل | مكوّنات | القرار | السبب |
| --- | --- | --- | --- |
| `GameAIAsst` | 4 (svc + act + prv) | يُدمج في `gameassist` كـ**قدرة OCR/وكيل** لا كوحدة | `GameAiAsstService` + `PermissionActivity` + `GameAIAsstProvider`: لا سطح مستقل يُبرّر عملية |
| `NubiaGameLab_MFV` | 2 (svc + prv) | يُدمج في `gameassist` (قدرة `ocr`) | `OcrProvider` وحده لا يصنع تطبيقًا |
| `GamePluginTrigger` | 7 | يُدمج في `gameassist` (قدرة `plugins`) | `MultiPluginService`/`PluginTriggerService` تعمل لصالح التراكب |
| `GameCardAssist` | 3 | يُدمج في `floatwindow` (قدرة `cards`) | `ACTION_SHOW_PANEL`/`ACTION_HIDE_PANEL` = سطح نافذة عائمة |
| `GameAIAsst`+`GameCardAssist` معًا | — | **لا يُدمجان معًا** | مالك البيانات مختلف (`GameAIAsstProvider` مقابل لا provider) |
| `NubiaRedmagickyi_RedMagic` | مستثنى | **لا وحدة، لا تحليل** | أمر المالك: لا فك، لا تحليل، لا رفع |

> الدمج هنا **دمج سطح لا دمج كود**: كل قدرة مدموجة تحتفظ بعقدها (فعل/صلاحية/اسم منطقي) كما قيس،
> فلا يفقد المستخدم أو أي تطبيق آخر شيئًا من أسماء السطح.

---

## 3. جدول الملكية (من يملك ماذا)

| القدرة | الوحدة المالكة | الدليل على وجودها في الأصل |
| --- | --- | --- |
| حالة الجلسة (لعبة في المقدمة) | `gamespace` | `GameSpace`: `GameEventService` + `GameEventService$GameBroadcastService` (13 receiver) |
| التراكب/اللوحة الرئيسية | `gameassist` | `GameAssistService` بصلاحية `cn.nubia.gameassist.permission.ACCESS` |
| عرض/إخفاء لوحة البطاقات | `floatwindow` | `com.zte.gamecardassist.ACTION_SHOW_PANEL` / `ACTION_HIDE_PANEL` |
| حدّ الأداء (PowerPanel) | `gamespace` (مالك) · `floatwindow` (عَرْض فقط) | `PowerPanelService` في **كلٍ منهما** ⇒ ملكية واحدة معلنة، والثاني يطلب عبر العقد |
| خط التصويب | `helperline` | `AimService` · `LineService` |
| التسجيل/اللقطة | `highlights` | `ScreenClipService` · `GameHighLightsClipService` |
| الملاحظات | `notes` | `FloatWindowService` + `GameNotesProvider` |
| يد اللعب/الأزرار الافتراضية | `gamepad` | `GamepadService` · `GameHandleService` · `ACTION` `cn.nubia.camerakeytouch` |
| OCR/الوكيل الذكي | `gameassist` | `OcrProvider` · `GameAiAsstService` · `com.zte.aiassistant.SUB_AGENT` |
| الإحصاءات على الشاشة | `widget` | 8 receivers بصفر خدمات (سطح بيانات لا عملية) |

**قاعدة الملكية:** قدرة واحدة = وحدة مالكة واحدة. مالك ثانٍ يُرفض في `CAPABILITY_REGISTRY.md` §3، والـarbiter
يرفض التنفيذ المتزامن.

---

## 4. نطاق مُقلَّم لـ`store` (قرار صريح لا صمت)

`NBGameCenter_Abroad` فيه 149 مكوّنًا (9 act · 8 rcv · 6 svc · 3 prv) — منها تنزيل وتحديث وحساب
ودفع (`LoginActivity` · `DownloadService` · `AutoUpdateJobService` · `WXEntryActivity`).
**ما يُنفَّذ:** سطح الإقلاع فقط (`DeeplinkDelegate`-المكافئ، فتح صفحة لعبة، اكتشاف الحزمة)، **بلا**
حساب ولا دفع ولا تنزيل من خوادم Nubia (لا نملك واجهاتها، ولا يجوز تقليدها).
**ما يُعلن:** بقية السطح `UNSUPPORTED` بسببه، لا «قيد التطوير».

---

## 5. قيود البناء لكل وحدة

| القيد | القيمة |
| --- | --- |
| `applicationId` | `dev.redlab.<unit>` — لا استثناء |
| `minSdk` | `28` (أقل `minSdk` مقيس في المنظومة؛ `GamePi` 28 · `NBGameCenter` 21 استثناء تاريخي لا نقلّده) |
| `targetSdk` | `36` (أعلى قيمة مقيسة، وموافقة لـ`platforms;android-36` في CI) |
| لغة الكود | **لغة أصل الوحدة**: Java/XML حيث الأصل Java/XML · Kotlin حيث الأصل Kotlin · JNI فقط عند مسار عتاد مقيس |
| النصوص | `values/` (إنجليزي) + `values-ar/` (عربي) فقط — قرار المالك |
| التوقيع | `redlab-ecosystem-v1` من `secrets` — لا يُخزَّن في المستودع |

---

## 6. ترتيب العمل (بحسب القياس لا بحسب الرغبة)

1. `redlab-core` (بلا شاشات) — أول ما يُبنى، لأنه عقد الجميع.
2. `gamespace` — مصدر الجلسة، وبلا جلسة لا معنى لبقية الوحدات.
3. `gameassist` — أكبر مستهلك للجلسة وأغنى سطح providers.
4. `floatwindow` · `helperline` · `gamepad` — تعمل داخل الجلسة.
5. `highlights` · `notes` · `widget` · `gamepi` — ميزات أثناء اللعب.
6. `store` · `theme` · `launcher-adapter` · `wallpaper` · `xr` — سطح مستقل عن الجلسة.

السبب: التبعية على **عقد الجلسة** لا على الشاشة؛ وبناء الوحدات المستقلة أولًا يعني اختبار عقد
الجلسة قبل أن يصبح حجرًا يصعب تحريكه.
