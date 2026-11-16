# تصليب الإصدار مقابل الهندسة العكسية — قرارات مُقاسة (٢٠٢٦-١١-١٦)

> **أمر المالك (حرفيًّا):** «ادرس كيف يعمل هذا https://github.com/morluto/rea وقم بحماية
> تطبيقي منه وايضا استخدم كل هذه الاشياء حتي لو استغرق الامر ساعات» — ثم قائمة ٢٦ مستودعًا.
> وحدّه الذي قاله بنفسه: **لا حماية مطلقة**.

هذا الملف يحفظ **قرار كل مستودع** بسبب مقيس، حتى لا يُعاد البحث من الصفر ولا يُدَّعى
تبنّي ما لم يُبنَ. وهو مرجع، لا وعد: ما لم يُشغَّل هنا يُكتب بجانبه «غير مُتحقَّق».

## 1. التهديد الحقيقي، مفكوكًا إلى ثلاثة

الهندسة العكسية ليست شيئًا واحدًا، وخلطها يُنتج حماية لا تحمي:

| التهديد | ما يفعله المهاجم | ما يُجديه فعلًا | ما لا يُجديه |
| --- | --- | --- | --- |
| **إعادة التغليف** | يفكّ الحزمة، يعدّل، يعيد التوقيع، ينشر باسمه | **كشف التوقيع المزدوج** (الكشف من المنصّة ومن الملف نفسه) + `ApkVerifier` v2/v3 | التعتيم — المهاجم لا يقرأ الشيفرة أصلًا |
| **التحليل الساكن** | JADX/Ghidra: يقرأ الأصناف والنصوص والمسارات | إزالة ما يسهّل التحليل (أسماء ملفّات المصدر، مسارات البناء، خريطة R8، رموز `.so`) + رفع كلفة القراءة (تعتيم/تنويع) | منع التحليل — **غير ممكن** على جهاز يحمله المستخدم |
| **فهم السلوك ونسخ الأفكار** | يشغّل التطبيق/يقرأ الشاشات ويكتب نظيرًا | إخفاء المنطق الحسّاس في native مُجرَّد، والسرعة في التطوير | أي تعتيم — الأفكار تُقرأ من الشاشة والقانون يحمي التعبير لا الفكرة |

## 2. `morluto/rea` — كيف يهجم بالضبط (مقروء من playbook الخاص به)

* أداة وكلاء (MIT، TypeScript) تقرأ الحزمة **ساكنًا** عبر JADX بلا تشغيل ولا محاكي
  (‏`android-applications.md` من المستودع نفسه).
* أدواته: `inspect_android_package` · `search_android_classes` · `inspect_android_class`
  · `inspect_android_method` · `trace_android_references` · `inspect_android_jni_bindings`.
* **أخطر سطر فيه:** «For an R8/ProGuard release build **with its `mapping.txt`** … each symbol
  reports `mapped` / `ambiguous` / `unmapped`» ⇒ **نشر `mapping.txt` يُلغي تعتيم R8 كله**.
  فلا يُرفع كأثر (artifact) ولا يُشحن — وهذا صار قاعدة في `hardening_guard.check_mapping_upload`.
* مساره الحيّ Frida (`start_frida_session` / `load_frida_script`) — وعلاماته مغطّاة أصلًا في
  `core/security` (`HOOK_LIBRARY_MARKERS` · `JAVA_HOOK_CLASSES` · `tracerAttached`).

## 3. ما كان قائمًا قبل هذه الجولة (فلا يُعاد بناؤه — §0.5)

`manager/app/src/main/java/nd/max/core/security/` — ١٣ ملفًا / ١٦٨٥ سطرًا:

* توقيع مزدوج: `IntegrityProbe.signerDigest()` (من المنصّة) و`ApkSigningBlock.signerCertificateDigest()`
  (من ملف APK نفسه) مقابل `SecurityPin.expected` المشتق وقت البناء من keystore الإصدار.
* `ApkContentVerification` — `ApkVerifier` v2/v3 على الحزمة المثبَّتة ⇒ `APK_CONTENT_MISMATCH`
  (الإشارة الوحيدة التي تنجو من تعديل DEX مع بقاء بايتات التوقيع).
* `AssetVault` (أصول مختومة AES/GCM بمفتاح مشتق من بصمة التوقيع) · `IntegrityProbe.nativeImageMismatch`
  (مقارنة `.so` على القرص بما في الحزمة) · `SecurityBridge.runtimeProbe()` (tracer/hook/debug port).
* `IntegrityLedger` يعدّ الإشارات المختلفة ولا يعتبر **الجذر** دليلًا أبدًا (`NEVER_EVIDENCE`) —
  وهذا صحيح لتطبيق مدير جذر.
* R8: `isMinifyEnabled`/`isShrinkResources`، `-repackageclasses 'o'`، `-overloadaggressively`،
  `-optimizationpasses 5`، `-renamesourcefileattribute SourceFile`، وإبقاءات مبرَّرة فقط.
* native: `[profile.release]` فيها `strip = true` و`panic = "abort"` و`lto = "thin"`.

## 4. ما أُضيف في هذه الجولة (كلّه مقيس هنا)

| الإضافة | الملف | ما تُثبته |
| --- | --- | --- |
| `-allowaccessmodification` | `manager/app/proguard-rules.pro` | كان مذكورًا في التعليق وحده؛ صار مُعلنًا ومراقَبًا |
| تضييق إبقاء JNI | نفس الملف | `{ native <methods>; }` بدل `{ *; }` — الرموز المُصدَّرة `Java_nd_max_core_jni_ProbeBridge_*` محفوظة، وبقيّة الأعضاء تُعاد تسميتها |
| تصليب DEX في CI | `.github/workflows/build.yml` | `bla-agents` على DEX ثم `dex_integrity` ثم `apk_repack` ثم إعادة توقيع، بمفتاح إيقاف `vars.BLA_SHIELD=0` |
| `tools/dex_integrity.py` | جديد | ترويسة DEX وAdler32/SHA-1 — ما لا يقرأه `apksigner` |
| `tools/apk_repack.py` | جديد | `resources.arsc` مُخزَّن (aapt2 يقرؤه mmap) وحذف توقيعات META-INF البائدة |
| `tools/apk_leak_scan.py` | جديد | جدول نصوص DEX: أسماء ملفّات مصدر، مسارات بناء، مسار keystore، ومدخلات محرَّمة |
| `tools/hardening_guard.py` | جديد | يمنع تراجع R8 والتقليص والإبقاء ونشر `mapping.txt` وحذف خطوات التصليب بصمت |
| `debug = false` | `manager/src/main/rust/Cargo.toml` | لم يكن مُعلنًا؛ صار |

**قياس حقيقي على APK موجود:** `apk_leak_scan` على `app-debug.apk` قرأ ٢٠ ملف DEX و٤٩٤٠٨٢ نصًّا،
وأبلغ ٨٢٢٢ أثرًا (٨٢١٧ اسم ملفّ مصدر + ٥ مسارات بناء). وهذا **متوقّع في debug** بالتصميم — ولهذا
تسقط البوابة على نسخة release وحدها، وdebug تعبر بإعلان.

## 5. قرارات المستودعات الستّة والعشرين

الترخيص قيس بـ`gh api repos/<owner>/<name>` — لا بالذاكرة.

| المستودع | الترخيص | القرار | السبب المقيس |
| --- | --- | --- | --- |
| `morluto/rea` | MIT | **لا يُدخَل** | أداة هجوم؛ استُخدمت لقراءة أسلوبه فقط (‏§2) |
| `qybaihe/bla` | MIT | **مُدخَل كأداة بناء في CI** | تصليب DEX؛ لا شيفرة تُشحن (‏`THIRD_PARTY_NOTICES.md`) |
| `xopJack/XopProtector` | Apache-2.0 | غير مُدخَل الآن | حزمة/VMP shell: تتطلب سلسلة بناء خارجية وإعادة توقيع، ولا تُقاس بلا جهاز — تُترك كخطوة يدوية إن طُلبت |
| `luoyesiqiu/dpt-shell` | MIT | غير مُدخَل الآن | DEX shell بتحميل وقت تشغيل: يغيّر مسار الإقلاع، ويحتاج قياسًا على جهاز قبل أي التزام |
| `CodingGay/BlackObfuscator` | Apache-2.0 | غير مُدخَل الآن | تسطيح تدفّق على DEX؛ يلزمه قياس أثر على الحجم والأداء |
| `Mobile-IoT-Security-Lab/Obfuscapk` | MIT | غير مُدخَل | يجمع أدوات إعادة تغليف؛ يفسد التوقيع ولا يضيف على `bla` |
| `Guardsquare/proguard` | GPL-2.0 | **مرفوض** | §0.4: لا GPL في مسار الإصدار؛ نستخدم R8 من AGP وقواعده Apache |
| `skidfuscatordev/skidfuscator` | MIT | غير منطبق | يعمل على JVM bytecode لا DEX |
| `talsec/Free-RASP-Android` | MIT | **مغطّى قائمًا** | كشفه هو نفسه ما في `core/security` (hook/tracer/debug/signature) — وموضعنا أدقّ لأن الجذر ليس دليلًا |
| `securevale/android-rasp` | Apache-2.0 | **مغطّى قائمًا** | نفس السبب |
| `PhuongDoZz/NativeShield` | MIT | **مغطّى قائمًا** | `strip` + مقارنة `.so` قائمة |
| `iamjosephmj/DeviceIntelligence` | MIT | **مغطّى قائمًا** | استعلام جهاز/إصدار لا يكشف تلاعبًا |
| `JungliCode/paranoid` | Apache-2.0 | **مغطّى قائمًا** | فحوص بيئة تنفيذ؛ `IntegrityProbe` يغطّيها |
| `stringcare/androidlibrary` | Apache-2.0 | **مغطّى قائمًا** | `AssetVault` يفعل ما يفعله (تعمية أصول بمفتاح) |
| `bytedance/AabResGuard` | Apache-2.0 | غير منطبق | يعمل على AAB؛ نُصدر APK، ومستودعه مؤرشف ٢٠٢١ |
| `Mobile-IoT-Security-Lab/ARMANDroid` | **AGPL-3.0** | **مرفوض** | §0.4: لا AGPL بشروط، حتى بلا نقل كود |
| `square/leakcanary` | Apache-2.0 | غير مُدخَل | أداة تطوير debug؛ لا تُشحن، والتسريب يُقاس بالبايتكوت لا بالجهاز |
| `ChuckerTeam/chucker` | Apache-2.0 | غير منطبق | لا شبكة HTTP في التطبيق |
| `detekt/detekt` | Apache-2.0 | غير مُدخَل | البوابات الحالية (`code_health`/`kt_balance`/`design_tokens`) تقوم بالمهمة بلا أداة JVM ثقيلة |
| `mockk/mockk` · `cashapp/turbine` · `cashapp/paparazzi` | Apache-2.0 | غير مُدخَل | تحتاج تشغيل Gradle محليًّا أو مصوّرًا — ممنوع (§0.10) |
| `android/architecture-samples` · `nowinandroid` · `platform-samples` | Apache-2.0 | **مرجع لا نقل** | §0.4: تُقرأ للأنماط، ولا يُنسخ كود ولا شكل |
| `fastlane/fastlane` | MIT | غير مُدخَل | النشر يدوي عندنا؛ لا خطّ متاجر مؤتمت |

**وما يخصّ الأداء من القائمة (R8 full mode · Baseline Profiles · App Startup · Coil · RecyclerView+DiffUtil
· Coroutines/Flow · OkHttp · Room · AAB):** قيس أنّ R8 الكامل مفعّل أصلًا (‏AGP 8)، و`Coil 2.7.0` موجود،
ولا يوجد `okhttp`/`room` في الكتالوج لأن التطبيق لا يستعملهما. و**Baseline Profile** فرصة حقيقية لكن
توليده يحتاج جهازًا/محاكيًا — فلا يُدَّعى هنا.

## 6. الحدود (تُقال ولا تُخفى)

1. **لا حماية مطلقة.** من يملك الجهاز يملك الحزمة؛ الهدف رفع الكلفة وكشف التلاعب، لا الاستحالة.
2. `bla-agents` **لم تُشغَّل في هذه البيئة** (لا شبكة npm)؛ حكمها الأول من CI، و`defenseScore` ليس برهانًا
   (تحذير أداة نفسها).
3. «حزمة مصلَّبة تعمل على جهاز» **غير مُتحقَّقة** — تحتاج جهازًا.
4. `apk_leak_scan` يقرأ جدول نصوص DEX ومدخلات الحزمة، **لا كل الشيفرة**؛ نظيفه ليس شهادة براءة.
5. البوّابات في `tools/` تقيس **الإعداد** أكثر مما تقيس الناتج؛ وما يقيس الناتج (`dex_integrity`،
   `apk_leak_scan`) يعمل على أثر البناء داخل CI.
