# CURRENT_ARCHITECTURE — تدقيق المستودع كما هو (قياس لا حكم)

**التاريخ:** 2026-11-03 · **الفرع:** `main` @ `c853fea7` · **الشجرة:** نظيفة (٠ ملفّ معدَّل).
**النوع:** تدقيق **قراءة فقط** — صفر تعديل كود · صفر بناء · صفر تشغيل للتطبيق.
**الأساس:** `MAXMANAGER-TRANSFORM-01-spec.md` §13 · `ARCHITECTURE-EVOLUTION-NOV26.md` §0 (التشخيصات الثلاثة).

> كل رقم هنا **مقيس في هذه الجلسة** بأمر مذكور في §11، أو منقول بوثيقته. والملفّ **لا يصدر حكمًا** على
> أيّ ملفّ: الأرقام تقيس الحجم والاقتران؛ وأيّ «حذف» يحتاج دليل استعمال (ADR-18).

---

## ١. البناء والاعتمادات

| العنصر | القيمة |
| --- | --- |
| Gradle wrapper | `9.5.1` |
| AGP | `9.2.0` |
| Kotlin | `2.3.10` |
| Compose BOM | `2025.10.01` |
| `kotlinxSerialization` | `1.11.0` |
| اعتمادات `:app` المعلنة | **٤٠** (`implementation`/`api`/`compileOnly`/`ksp`/`kapt`) |
| وحدات Gradle | `:app` · `xposedapi` · جذور Rust/C مستقلّة |

**وثلاثة قيود مقيسة من تعليقات `libs.versions.toml` نفسها:** `navigation-compose 2.10.1` و
`lifecycle-*-compose 2.11.0` تطلب `compileSdk 37` و`:app` على `android-36`؛ و`haze 2.0.0-alpha02`
مبنيّ على Compose 1.9. ⇒ **هشاشة إصدارات موثَّقة في المشروع نفسه**، ولا تُحلّ بتغيير رقم بلا قياس.

---

## ٢. خريطة الحزم — الحجم الفعليّ

### ٢.١ `ui/` — **٤٢٤ ملفًّا** · الحجم بالأسطر

| الحزمة | ملفّات | أسطر |
| --- | --- | --- |
| `ui/subtcreens` → `subscreens` | **٨٩** | **٤٣٬٥٨٧** |
| `ui/component` | **٩٠** | **٢٦٬٥٧٩** |
| `ui/util` | **١١٤** | **٢٠٬٥٣٩** |
| `ui/mainscreens` | ٣٩ | ١٣٬٣٨٩ |
| `ui/viewmodel` | ٣٢ | ١١٬٠٣٢ |
| `ui/design` | ٢٨ | ٧٬٧٢١ |
| `ui/navigation` | ١٠ | ٢٬٠٨٩ |
| `ui/overlay` | ٤ | ١٬١٢٢ |
| `ui/theme` | ٦ | ١٬١٠٨ |
| `ui/license` | ٤ | ٩٠٣ |
| `ui/activitylauncher` | ٣ | ٧٢٠ |
| `ui/settings` | ٥ | ٦٨٠ |

⇒ **`ui/util` أكبر من `ui/mainscreens`** بـ١٫٥× في الأسطر: منطق غير مرئيّ يعيش في طبقة العرض. وهذا
**موضع القياس** لشرط ADR-11 (لا كتابة من `ui/**`) — انظر §٥.

### ٢.٢ `core/` — الحجم بالملفّات

| الحزمة | ملفّات | ملاحظة |
| --- | --- | --- |
| `core/hardware` | **٦٥** | القدرات كشيفرة (التشخيص ت١) |
| `core/audio` | ٤٥ | |
| `core/gamespace` | ٤٢ | |
| `core/atlas` | ٣٠ | |
| `core/spoof` | ٣٠ | |
| `core/maxai` | ٢٩ | |
| `core/platform` | ١٦ | |
| `core/security` | ١٣ | |
| `core/diagnostics` | ١١ | |
| `core/privilege` | ١٠ | |
| `core/remote` | ١٠ | |
| `core/jni` | ٩ | |
| `core/license` · `core/update` | ٨ · ٨ | |
| `core/daemon` | ٦ | |
| `core/access` | ٥ | |
| `core/emuhub` | ٥ | |
| `core/persist` | ٣ | |
| `core/ipc` · `core/recommendation` · `core/di` · `core/threading` | ٢ · ٢ · ٢ · ١ | |

**والإجمالي:** ٨٥٣ ملفّ Kotlin · **٢١٤٬٥١٢ سطرًا** في `manager/app/src/main/java`.

---

## ٣. سطح المانيفست — مقيس

| المكوّن | العدد |
| --- | --- |
| `activity` | **٥** |
| `service` | **١٣** |
| `receiver` | **٢٧** |
| `provider` | **٣** |
| `uses-permission` | **٣٢** |

⇒ **٤٨ مكوّنًا** معلنًا في المنصّة. كل `receiver` نقطة استيقاظ محتملة، وكل `service` سطح عمر مستقلّ.
**والقياس لا الحكم:** هذه الأرقام هي ما يجب أن تُقارن بـ`dumpsys package` على جهاز (§10).

---

## ٤. الطبقة الأصلية — أربع جذور، ثلاث لغات

| الجذر | اللغة | الأسطر |
| --- | --- | --- |
| `archdaemon/` | C (+ JNI) | **١٠٬٣٨١** |
| `maxfx/` | C (+ AIDL) | **٨٬٤٨٢** |
| `binprofiles/` | Rust | **٢٬٩٣١** |
| `thermalcore/` | Rust | **٢٬٥١٧** |
| **الإجمالي** | | **٢٤٬٣١١** |

**وحدود مقيسة:** `jni_symbols --assert` = ٢٣ تصريحًا · ٠ ثنائيات ⇒ **الطبقة الثانية (الرموز في `.so`
الحقيقية) غير مُتحقَّقة في هذه البيئة**. و`archdaemon/tests` له Makefile مضيف مستقلّ (parity) لم يُشغَّل هنا.

---

## ٥. مسارات الامتياز — **خمسة**، لا أربعة

| # | المسار | الملفّ | الأسطر |
| --- | --- | --- | --- |
| A | binder | `RootIpcManager` + `RootNodeService` | ٩٥ |
| B | خدمة عُقدة | `core/ipc/RootNodeService` + `RootNodeChannel` | ضمن A |
| C | Shizuku/shell | `core/privilege/PrivilegeManager` + `ShizukuExec` | ٦٤ + ٣١ |
| D | الـdaemon | `sys.maxmanager-service` + `core/daemon/DaemonIpc` | ٢٢٠ |
| E | رفيق `app_process` | `AppMonitor.kt` | **٣٬٨١٩** |

⇒ **خمس سياسات فشل**، وخمس طرق تحقّق كتابة. والخطة: **وسيط واحد يغلّفها** ولا يدمجها (§4 من المواصفة).

### ٥.١ قراءة العتاد من طبقة العرض — مقيسة

| القياس | القيمة |
| --- | --- |
| ملفّات `ui/**` تحمل `"/sys/` أو `/proc/` | **٣٠** |
| منها في `ui/util` | الغالبية |
| أمثلة (أسماء فقط) | `DeviceInfoCollect.kt` · `MemoryUfsScreen.kt` · `ZramManagerScreen.kt` · `BatteryHealthUtil.kt` · `CpuTopologyUtil.kt` · `MtkUtils.kt` · `SELinuxCheckerUtil.kt` · `StorageHealthUtil.kt` · `ZramPlatformUtil.kt` · `BootHistoryUtil.kt` |

**ولا يُستنتج من العدد أن كلها «كتابة».** القياس المطلوب في M2: **كم منها كتابة** (تكسر ADR-11) وكم قراءة
(تكسر عقد الرصد). البوّابة القائمة `presentation_hw_writes` تعدّ التجاوزات وسقفها ٠ — والفرق بينها وبين
العدد ٣٠ هو **ما يجب أن يُقاس** لا يُفترض.

---

## ٦. سطح التنقّل — ٦٥ وجهة

| القياس | القيمة |
| --- | --- |
| `data object … : MaxDestination` | **٦٥** |
| `isPrimary = true` | **٤**: `Now` · `Apps` · `Control` · `Settings` |
| `hidden = true` | **٣** |
| `MaxRisk.Advanced` أو `Dangerous` | **٨** |
| محاور `Control` | **١٠**: `CpuHub` `GpuHub` `MemoryHub` `DisplayHub` `ResponsivenessHub` `ThermalHub` `PowerHub` `StorageHub` `NetworkHub` `AudioHub` |
| `MaxAi` | **بطاقة لا مقعد** (قرار سابق) |

⇒ العمود الحاليّ **٤ مقاعد** والقراءة والكتابة **مختلطتان في `Control`**. وهذا مربط مقترح `Monitor`
في المواصفة §6.1.

---

## ٧. دورة الحياة والاستقصاء — ما يُقاس الآن

| القياس | القيمة | ما يحتاجه |
| --- | --- | --- |
| `newWakeLock` / `PARTIAL_WAKE_LOCK` | **٠ ملفّ** | جيّد، ويجب أن يبقى ٠ |
| `postDelayed` | **٤** مواضع | كل موضع يجب أن يُعلن وتيرته |
| `scheduleAtFixedRate` / `fixedRateTimer` | **٠** | |
| `while (true)` | **٤٦ حلقة في ٣٢ ملفًّا** | كل حلقة تحتاج: شرط خروج، وتيرة، وإلغاء عند الخفاء |

**والقياس المطلوب (M3):** لكل حلقة — `cadence` معلنة · `cancellation` عند الخفاء · `single-flight` يمنع
التزامن. والبوّابة: صفر حلقة بلا وتيرة معلنة. **والحكم على الـ٤٦ مؤجَّل** حتى تُصنَّف: حلقة قراءة حدثية
ليست كحلقة استقصاء.

---

## ٨. الاختبارات — ٣٨٣ ملفًّا (الأساس الحقيقيّ)

| الحزمة | ملفّات |
| --- | --- |
| `core` | **٢٣٢** |
| `ui` | **١٢٦** |
| `contract` | **١٦** |
| `island` | ٥ |
| `architecture` · `widget` | ١ · ١ |

⇒ **٦١٪ من الاختبارات على `core`** — أي أن الطبقة التي ستُعاد هيكلتها محميّة. وهذا **أثمن رصيد في
المستودع**، وقاعدة ADR-18 («لا هدم ما يعمل») مبنية عليه. والتحويل يجب أن **يستهلكه** لا يتجاوزه.

**وحدود:** آخر رقم اختبارات مُسجَّل في `NEXT_TASK`/`HANDOFF` هو **٣٦٩٠–٣٧٠٥** اختبارًا في تشغيلات سابقة؛
**لم تُشغَّل هنا** (بلا Gradle كامل). ⇒ تُكتب «غير مُتحقَّقة في هذه البيئة» ولا تُدَّعى.

---

## ٩. الدَّين المقيس والبوّابات

| القياس | القيمة | البوّابة |
| --- | --- | --- |
| `kt_balance` | ٣١٩٠ ملفًّا · ٠ عوائق | ✅ |
| `code_health` | صحّة ٠ · دَين **٩/٢٦/٥/٤/٠** (ملفّات كبيرة/…/…) عند السقف | ✅ |
| `design_tokens` | **٢٥٠/٣٠٠** حرفيًّا | ✅ (٥٠ تحت السقف) |
| `i18n_coverage` | ٠ عوائق · ٠ يتيم | ✅ |
| `resource_compile` | مجلّدات `res` تُترجم بـ`aapt2` حقيقيّ | ✅ |
| `jni_symbols` | ٢٣/٠/٠ · **الطبقة ٢ غير مُتحقَّقة** | ✅ مع حدّ |
| `license_audit` | لا GPL في مسار الإصدار | ✅ |
| **`makers_mark`** | **ينهار** — أصل `docs/assets/makers-mark-*.svg` غائب | ❌ **سبب CI الأحمر** |
| CI (`MaxManager` + `MaxManager docs`) | **failure** على `c853fea7` (وسابقيه) | ❌ M0 |

**أكبر الملفّات (مرشّحات التقسيم، لا أحكام):**
`AppMonitor.kt` ٣٬٨١٩ · `MaxAiEngine.kt` ٢٬٢٨٧ · `MaxAiScreen.kt` ١٬٨٣٧ · `AppSettingsScreen.kt` ١٬٧٩٣ ·
`CustomThemeScreen.kt` ١٬٦٠١ · `GpuHardwareBackend.kt` ١٬٤٣٠ · `CpuCoreControlScreen.kt` ١٬٢٤٦ ·
`ExpressiveListComponent.kt` ١٬٠٨٤.

---

## ١٠. ما لا يُقاس في هذه البيئة — صريح

| القياس | الحاجة | الحالة |
| --- | --- | --- |
| CPU/PSS خامل | جهاز + `dumpsys meminfo` | **غير مُتحقَّقة** |
| استيقاظات/ساعة | جهاز + `batterystats` | **غير مُتحقَّقة** |
| استهلاك البطارية | جهاز + خطّ أساس قبل/بعد | **غير مُتحقَّقة** |
| اللمس/الجزيرة/النافذة | جهاز | **غير مُتحقَّقة** |
| Shizuku فعليًّا · `getThermalHeadroom` | جهاز | **غير مُتحقَّقة** |
| الإقلاع · SELinux على جهاز | جهاز | **غير مُتحقَّقة** |
| الترجمة (Gradle كامل) | Codespace/CI | **غير مُتحقَّقة هنا** |
| رموز JNI في `.so` | NDK | **غير مُتحقَّقة** |

---

## ١١. الأوامر التي أنتجت كل رقم

```sh
# الحجم
find manager/app/src/main/java -name '*.kt' | wc -l                        # 853
find manager/app/src/main/java -name '*.kt' -print0 | xargs -0 wc -l | tail -1   # 214512
for d in manager/app/src/main/java/nd/max/ui/*/; do … find "$d" -name '*.kt' | wc -l; done

# المانيفست
grep -cE '<service' manager/app/src/main/AndroidManifest.xml   # 13 (و٢٧ receiver · ٥ activity · ٣ provider · ٣٢ إذنًا)

# العتاد
grep -rhoE '"/sys/[^"]+"' manager/app/src/main/java/nd/max/core/hardware | sort -u | wc -l   # 41
grep -rhoE '"/proc/[^"]+"' … | sort -u | wc -l                                              # 15
grep -rlE '"/sys/' manager/app/src/main/java/nd/max/core/hardware | wc -l                   # 9
grep -rlE '"/sys/|/proc/' manager/app/src/main/java/nd/max/ui --include=*.kt | wc -l        # 30

# الحالة ودورة الحياة
grep -rhoE 'postDelayed|scheduleAtFixedRate|fixedRateTimer' … | sort | uniq -c   # 4 postDelayed
grep -rc 'while (true)' … | awk -F: '$2>0{…}'                                   # 32 ملفًّا · 46 حلقة
grep -rlE 'PARTIAL_WAKE_LOCK|newWakeLock' … | wc -l                             # 0

# التنقّل
grep -cE 'data object [A-Za-z0-9]+ : MaxDestination' …/MaxDestinations.kt   # 65
grep -cE 'isPrimary = true' …                                                # 4
grep -cE 'hidden = true' …                                                   # 3

# الكتالوج
grep -cE 'const val [A-Z0-9_]+' …/HardwareControlKey.kt   # 14
ls …/core/hardware/*Backend*.kt                            # 6

# البوّابات (٢٢ نداءً كما في build.yml — الخطوة ٥)
python3 tools/kt_balance.py --assert        # … إلخ؛ النتائج في §9
git status --porcelain | wc -l              # 0
git rev-parse --short HEAD                  # c853fea7
```

---

## ١٢. الخلاصة المقيسة — ثلاث فجوات عقود، لا ثلاث «مشاكل»

| # | الفجوة | الرقم الحاكم | ما يُغلقها |
| --- | --- | --- | --- |
| ت١ | **القدرات شيفرة لا بيانات** | ٤١ مسار `/sys` + ١٥ `/proc` في ٩ ملفّات، بلا سجلّ | `ControlKey` catalog (M1) |
| ت٢ | **خمسة مسارات امتياز** | ٥ مسارات · خمس سياسات فشل | `PrivilegeBroker` (M2) |
| ت٣ | **الرصد مبعثر** | ٣٠ ملفّ `ui` يقرأ العتاد؛ ٤٦ حلقة بلا وتيرة معلنة | `Observation` layer (M3) |

**ولا يُحذف شيء لإغلاق أيّ منها.** الفجوة **عقديّة**، وحلّها **إعلان** لا هدم — وهذا هو الفرق بين
«إعادة كتابة» و«تحويل» في هذه المواصفة.
