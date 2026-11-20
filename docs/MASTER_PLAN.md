# MASTER_PLAN — إعادة بناء منظومة RedMagic مستقلة

> **حالة:** مرجع التخطيط الحاكم لمشروع المنظومة. يحلّ محلّ `docs/PLAN.md` (خطة الترقيع السابقة، التي بقيت
> ملزمة فقط لـ«طبقة توافق GameTurbo الحالية» حتى تُسلَّم المرحلة 1).
> كل رقم في هذا الملف **مقيس** بأداة في `tools/` ويمكن إعادة توليده؛ وما ليس مقيسًا مكتوب بجانبه.

---

## 1. قرارات المالك التي تحكم الخطة (مأخوذة نصًّا من توجيهه)

| القرار | الأثر على الخطة |
| --- | --- |
| «استخدم نفس لغة البرمجة المستخدم فيهم، لا تقيّد نفسك بـKotlin» | **لغة كل وحدة = لغة أصلها**: Java حيث الأصل Java، Kotlin حيث الأصل Kotlin، XML layouts حيث الأصل XML، وCompose فقط حيث يخدم إعادة الإنتاج. لا إجبار على Compose، ولا على Kotlin في وحدة أصلها Java/JNI. |
| «REA = https://github.com/morluto/rea» | REA هو **محرك التحليل** لمرحلة الفهم العميق (JADX/Ghidra providers)، لا بديلًا عن أدوات القياس عندنا. القياس الحتمي يبقى في `tools/`، وREA يُستخدم للسؤال «كيف يعمل هذا السلوك». |
| «استفد من MaxManager في عملك» | يُستفاد من **أنماط MaxManager وتجاربه المقيسة** (أدوات البوابات، عقد التسليم، تجربة التراكب/الخدمة الأمامية/الـarbiter) — استفادة معرفية وأدوات، **ولا يُنقل كود إلى MaxManager ولا يُعدّل** (§25 من التوجيه). |
| «اعتمد على GitHub Actions في أغلب الأشياء» | كل قياس ثقيل (جرد، فك، بناء APK، تحليل REA) يجري في CI؛ الجهاز يُستخدم للقياس الحقيقي فقط (لمس، تراكب، عتاد). لا بناء محلي إلا عند حاجة حقيقية. |

---

## 2. ما قِيس فعلًا قبل أي تصميم (المرحلة 1 — منجزة)

| المقياس | القيمة | الأداة | الملف |
| --- | --- | --- | --- |
| تطبيقات في المجموعة | **23** (+1 مستثنى) | `apk_inventory.py` | `docs/audit/APK_INVENTORY.md` |
| إجمالي الحجم | **1.8 GB** / 24 APK | نفسها | نفسه |
| مكوّنات معلنة (act/svc/rcv/prv) | **519** صفًّا | `make_component_catalog.py` | `docs/audit/COMPONENT_CATALOG.md` |
| صلاحيات تصنيعية معلنة | 20+، منها `cn.nubia.gameassist.permission.ACCESS` = `signature\|privileged` | `apk_ipc.py` | `docs/IPC_AND_INTEGRATION.md` |
| ترابط على مستوى الأصناف بين التطبيقات | **≈صفر** (أعلى قيمة 5 أصناف) | `apk_xref.py` | `docs/audit/APP_DEPENDENCY_GRAPH.md` |
| أنواع معرَّفة داخل كل تطبيق | 7,094 – 15,879 لكل تطبيق | `apk_xref.py` | نفسه |
| targetSdk | 35/36، و**37** في `GameAssist` و`VirtualGameHandle` | `apk_inventory.py` | `APK_INVENTORY.md` |
| جدول الموارد لكل تطبيق | **23 تطبيقًا** بأعداد لكل نوع: 10,827 `string` · 19,410 `color` · 23,400 `dimen` · 6,031 `layout` · 51,699 `drawable` · 1,119 `mipmap` · 1,105 `anim` · 506 `animator` · 21,937 `style` · 1,082 `xml` · 182 `raw` | `apk_resources.py` | `docs/audit/RESOURCE_INVENTORY.md` |
| تسوية `resources N` مع دليل REA | مطابقة في **22/23** حرفًا بحرف (رقم jadx = عدد مدخلات ZIP)؛ الشاذّ الوحيد `NubiaGameWidget` 5111 مقابل 5109 في الملف | نفسه | نفسه §Reconciliation |

**الاستنتاج المعماري الحاكم:** المنظومة **مترابطة بالعقود (أسماء أفعال/مكوّنات/صلاحيات) لا بالكود**.
لذلك إعادة البناء المستقلة تحافظ على **سطح IPC** (أفعال، مكوّنات، authorities) وعلى **أسماء المنتجات**،
ولا تحتاج محاكاة أصناف بعضها لبعض. هذا يقلّل المخاطرة جذريًّا ويبرّر Multiple Apps بدل تطبيق واحد.

**أسماء الحزم الحقيقية (مقيسة، لا مفترضة):** `cn.nubia.gamelauncher` (GameSpace) · `cn.nubia.gameassist` ·
`cn.zte.gamefloat` (GameFloatWindow) · `cn.zte.gameaiasst` · `com.zte.gamecardassist` ·
`cn.nubia.gamehelperline` · `cn.nubia.gamepi` · `com.zte.game.plugintrigger` · `com.nubia.gamecenter` ·
`cn.nubia.gamelab` · `cn.nubia.gamenotes` · `cn.nubia.gamewidget` · `cn.nubia.gamepad` ·
`cn.nubia.gamehighlights` · `cn.nubia.gamehelpmodule` · `cn.nubia.virtualgamehandle` ·
`com.zte.xr.gamelauncher` (XRLauncher) · `com.zte.gallerylockscreen` (Wallpaper) · عائلة Launcher/Beautify.

---

## 3. سياسة الهوية والإصدار والتوقيع (قبل سطر كود)

| البند | القرار | السبب |
| --- | --- | --- |
| Package ID | `dev.redlab.<app>` مستقل لكل تطبيق | منع تعارض/انتحال هوية Nubia وتوقيعها (§5 من التوجيه) |
| Application Label | أسماء المنتجات كما هي: GameSpace · GameAssist · GameTurbo … | مطلب المالك الصريح |
| التوقيع | مفتاح واحد لكل تطبيقات المنظومة (`redlab-ecosystem-v1`) | الثقة بين الحزم قائمة على `signature`؛ مفتاحان = تعطّل التعاون |
| الإصدار | `versionName` = `1.<phase>.<build>` و`versionCode` متزايد لكل تطبيق؛ وثيقة `VERSIONS.md` تفصل إصدار الأصل عن إصدار البناء | لا ادّعاء «إصدار رسمي» |
| التكامل مع تطبيقات Nubia | `PackageManager` detection + Intent/Deep-Link/Provider **عمومي** فقط؛ لا API خاص بلا توثيق إصداره | §14 من التوجيه |

---

## 4. المراحل والمخرجات وشرط القبول

| # | المرحلة | المخرج | شرط القبول (مقيس) |
| --- | --- | --- | --- |
| 0 | Preservation | `docs/REA_AUDIT.md` + وسم حالة المستودع | كل ملف أصلي موجود قبل/بعد بنفس البصمة (SHA-256) |
| 1 | Inventory | `APK_INVENTORY` · `COMPONENT_CATALOG` · `APP_DEPENDENCY_GRAPH` · `IPC_AND_INTEGRATION` | 23/23 APK مقروءة، 0 خطأ، الأدوات تُعيد التوليد في CI |
| 2 | Reverse engineering ✅ | `REVERSE_ENGINEERING.md` + `FEATURE_CATALOG.md` + `RESOURCE_INVENTORY.md` | لكل ميزة: دليل (مكوّن/فعل/مورد) + حدّ ما تعذّر تحليله — **مُحقَّق على المستويين**: 579 عقدًا مكوّنًا/فعلًا، و23 تطبيقًا موردًا. المرئي (لقطة/مقطع) للمرحلة 3 |
| 3 | UX reconstruction | `SCREEN_INVENTORY` · `VISUAL_DESIGN_SPEC` · `DESIGN_SYSTEM` · `ANIMATION_CATALOG` | لكل شاشة لقطة مرجعية + قيم مقيسة (مقاس، لون، مدة) |
| 4 | Architecture ✅ | `SYSTEM_ARCHITECTURE.md` · `ECOSYSTEM_MODULES.md` · `CAPABILITY_REGISTRY.md` · `GAME_SESSION_SPEC.md` · `OVERLAY_SPEC.md` · `HARDWARE_CONTROL_SAFETY.md` | كل عقد IPC موثّق باسمه المقيس من المرحلة 1 — **مُحقَّق**: 23 أصلًا → 14 وحدة، ولكل قدرة مالك واحد ونقل صريح |
| 5 | Foundation 🟡 | مشروع Gradle `redlab/` + `:core` (`CapabilityState`/`Access` · `CapabilityRegistry` · `HardwareControlArbiter` · `GuardedNode` · `ValuePolicy` · `SysfsDiscovery`) | اختبارات وحدة خضراء في CI ✅ (**32 اختبارًا**، تشغيل `44813027394` نجح) + تقرير قدرات على جهازين ⏳ (يحتاج جهازًا). وقواعد الكتابة تحقّقت بإعادة القراءة والتراجع وفصل `AlreadySatisfied` عن `Applied` (منهج مقيس من MaxManager) |
| 6 | GameSpace | تطبيق المكتبة/الجلسات | فتح لعبة من الواجهة يُنتج `SessionManager=ACTIVE` على الجهاز |
| 7 | GameAssist + Overlay | لوحة عائمة + إيقاظ | ضغطة واحدة: نافذة تظهر وتختفي وتستعيد موضعها، بلا `startService` خلفي |
| 8 | GameTurbo | لوحة الأداء | كل عنصر تحكم له `CapabilityState` مقيس، ولا تحكم وهمي |
| 9 | بقية التطبيقات | بحسب ما يثبته الدليل فقط | لكل تطبيق: قائمة ميزات مُثبتة أو `UNSUPPORTED` معلن |
| 10 | Integration | IPC مستقل موثّق | سيناريو شامل: اكتشاف لعبة ← جلسة ← لوحة ← إحصاءات ← خروج |
| 11 | Universal compatibility | Shizuku/Root/SoC adapters | نفس السيناريو على جهاز ثانٍ مختلف SoC |
| 12 | Parity | `VISUAL_COMPARISON` · `ANIMATION_COMPARISON` · `FEATURE_PARITY` | فرق مكتوب لكل شاشة، لا انطباع |
| 13 | Performance | `PERFORMANCE_PLAN` + قياسات | خمول: CPU/RAM مقيسان قبل/بعد بفارق مذكور |
| 14 | Release | APK موقّعة + تثبيت/تحديث | تثبيت وتحديث فعليان على جهاز، وتوقيع واحد مُثبت |

**قاعدة التنفيذ:** لا تُفتح مرحلة قبل اكتمال شرط قبول سابقتها، إلا بجولة موازية معلنة (مثال: 2 و3 متوازيتان).

---

## 5. أين يعمل ماذا

| نوع العمل | المكان | الأداة |
| --- | --- | --- |
| جرد/تحليل/فك APK | CI (`ecosystem-audit.yml`) | `apk_inventory.py` · `apk_xref.py` · `apk_ipc.py` · `make_component_catalog.py` |
| تحليل سلوك عميق (REA) | CI (`rea-analysis.yml`) أو محليًا | `npx rea-agents` + JADX provider |
| بناء APK المنظومة | CI حصرًا | Gradle + مفتاح التوقيع من `secrets` |
| قياس حقيقي (لمس/تراكب/عتاد) | الجهاز عبر `adb` | `dumpsys` · `logcat` · `appops` |
| القرار المعماري والوثائق | المستودع | هذا الملف وما يتبعه |

**لا أسرار في المستودع:** مفتاح التوقيع في `secrets.SIGNING_*` فقط.

---

## 6. الوثائق المطلوبة وحالتها

| الوثيقة | الحالة اليوم |
| --- | --- |
| `MASTER_PLAN.md` | ✅ هذا الملف |
| `REA_AUDIT.md` | ✅ مقيس (بما فيه: لا توجد نسخة REA محلية لمنظومة RedMagic) |
| `APK_INVENTORY.md` · `COMPONENT_CATALOG.md` · `APP_DEPENDENCY_GRAPH.md` · `IPC_AND_INTEGRATION.md` | ✅ مولّدة من APK |
| `FEATURE_CATALOG.md` | ✅ مولّد من دليل REA المخزّن (`tools/feature_catalog.py`) — 24 تطبيقًا، 579 مكوّنًا: 270 `DECLARED` · 56 `TRACED` · 253 `UNVERIFIED`. السلوك نفسه لم يُقس: `UNVERIFIED` تعني «البيان يذكره فقط» |
| `docs/audit/RESOURCE_INVENTORY.md` | ✅ مولّد (`tools/apk_resources.py`, `--self-test` يمرّ): أسماء `layout`، وألوان `color` الحرفية، ومقاسات الأيقونة، وأعداد كل نوع. ما لا يقيسه: شكل الرسم والحركة ومحتوى `xml` |
| `SCREEN_INVENTORY` · `ICON_CATALOG` · `ANIMATION_CATALOG` · `INTERACTION_SPEC` · `VISUAL_DESIGN_SPEC` · `DESIGN_SYSTEM` | ⏳ المرحلة 3 (تحتاج لقطات ومقاطع من الجهاز) — والمدخل المقيس لها جاهز في `RESOURCE_INVENTORY.md` |
| `SYSTEM_ARCHITECTURE.md` · `ECOSYSTEM_MODULES.md` · `CAPABILITY_REGISTRY.md` · `GAME_SESSION_SPEC.md` · `OVERLAY_SPEC.md` · `HARDWARE_CONTROL_SAFETY.md` | ✅ المرحلة 4 — مبنية على الأرقام المقيسة: 14 وحدة من 23 أصلًا، ملكية قدرة واحدة لكل عائلة (ومنع تضارب `PowerPanelService` بين `GameSpace` و`GameFloatWindow`)، وآلة جلسة بخمس حالات، وتراكب بنوع `TYPE_APPLICATION_OVERLAY`. لا شهادة سلوك: كل بنود القبول تحتاج جهازًا |
| `DEVICE_MATRIX` · `TEST_STRATEGY` · `PERFORMANCE_PLAN` · `LICENSES` · `MIGRATION_PLAN` · `IMPLEMENTATION_ROADMAP` | ⏳ قبل المرحلة 5 |

---

## 7. ما لا تدّعيه هذه الخطة

- الجرد يثبت **وجود** مكوّن، لا **عمله**. التشغيل يحتاج جهازًا لكل صفّ.
- غياب الترابط على مستوى الأصناف **لا يعني** استقلالًا كاملًا: الاعتماد الحقيقي على خدمات النظام
  (overlay، تنبيهات، SELinux) لم يُقس بعد.
- REA أداة تحليل، لا مصدر حقيقة: كل نتيجة منه تُقابَل بقياس في `tools/` أو على الجهاز.
- الخطة لا تعد بموعد؛ كل مرحلة تُقاس بشرط قبولها.
