# CAPABILITY_REGISTRY — سجل القدرات وحالة «لا أعرف»

> مخرج المرحلة 4 وميثاق المرحلة 5. يترجم قاعدتين إلى كود: **لا تليمتري مُصنَّع** و**لا تحكّم وهمي**.
> كل قدرة لها: مالك واحد، طريقة كشف مقيسة، وأربع حالات صريحة. ما لا يُكشف = `UNKNOWN`، ولا يُعرض
> فعّالًا أبدًا.
>
> **تنبيه اسم:** هذا ملفّ **مكتوب بيد**، وليس `docs/CAPABILITIES.md` المولَّد من كتالوج MaxManager
> (`tools/gen_capabilities_doc.py`). لا يُخلطان: ذاك فهرس قدرات MaxManager الخاصّ، وهذا سجل المنظومة.

---

## 1. تعريف القدرة

القدرة هي **أصغر شيء يمكن أن يوجد أو لا يوجد على جهاز معيّن**، ويُعرض للمستخدم كعنصر تحكّم واحد.
ليست ميزة (الميزة قد تحتاج قدرات عدّة)، وليست مكوّنًا (المكوّن وسيلة تنفيذ).

```kotlin
enum class CapabilityState { AVAILABLE, UNSUPPORTED, NEEDS_PERMISSION, UNKNOWN }
enum class Access { NONE, READ_ONLY, READ_WRITE }

data class Capability(
    val id: String,                 // "<unit>.<domain>.<name>" مثل "gamespace.perf.cpu_max_freq"
    val owner: String,              // وحدة واحدة فقط
    val state: CapabilityState,
    val access: Access,             // سلّم §2.1
    val reason: String?,            // نصّ حرفي من الفشل، لا صياغة تجميلية
    val evidence: String?,          // ما الذي قيس فعلًا (مسار، قيمة، استثناء)
    val measuredAtMillis: Long?,
)
```

**قاعدة الذهب:** `state` لا يُستنتج من `Build.MODEL` ولا من اسم الشركة المصنّعة. يُقاس **بالفعل**:
قراءة ملف، فتح مقبض، استدعاء API، أو محاولة كتابة تجريبية آمنة. اسم الجهاز = فرضية، لا دليل.

**قاعدتان غير قابلتين للتمثيل في النموذج** (يرفضهما مُنشئ `init` في `:core`):
`AVAILABLE` بلا `evidence`، و`UNSUPPORTED` بلا `reason`. لا يمكن كتابة كذبة كهذه حتى بالخطأ.

---

## 2. عائلات القدرات (المقيس من الأصل)

| # | العائلة | أمثلة `id` | كشفها | مالكها |
| --- | --- | --- | --- | --- |
| 1 | الجلسة | `gamespace.session.foreground_game` | `UsageStatsManager` أو `ActivityManager` + حزمة معروفة كلعبة | `gamespace` |
| 2 | التراكب | `gameassist.overlay.window` | `Settings.canDrawOverlays` + إضافة نافذة تجريبية وإزالتها | `gameassist` |
| 3 | التسجيل | `highlights.capture.screen` | `MediaProjection` token فعلي (لا مجرد `checkSelfPermission`) | `highlights` |
| 4 | اللمس المحقون | `gamepad.input.inject_touch` | محاولة حقن حدث واحد ثم `InputManager`/رمي | `gamepad` |
| 5 | freq/الحاكم | `gamespace.perf.cpu_max_freq` | قراءة `scaling_max_freq` + كتابة **القيمة نفسها** | `gamespace` |
| 6 | حرارة/بطارية | `gamespace.perf.battery_temp` | `BatteryManager` أو `thermal_zone*` | `gamespace` |
| 7 | GPU | `gamespace.perf.gpu_freq` | `/sys/class/kgsl` أو `/sys/class/devfreq` قابل للقراءة | `gamespace` |
| 8 | root | `platform.access.root` | `su -c id` بنتيجة مقيسة، لا وجود ملف `su` | `platform` |
| 9 | Shizuku | `platform.access.shizuku` | `Shizuku.pingBinder()` + نسخة البروتوكول | `platform` |
| 10 | OCR | `gameassist.ai.ocr` | تحميل نموذج/محرّك وإرجاع نتيجة على صورة اختبار | `gameassist` |
| 11 | التعدّد الشاشي | `gameassist.display.multi` | `DisplayManager.getDisplays().size > 1` + دعم `Presentation` | `gameassist` |
| 12 | الإحصاءات الحيّة | `widget.stats.live` | وجود مصدر إحصاءات (arbiter) + أول تحديث مستلم | `widget` |
| 13 | تخطيط اليد | `gamepad.config.layout` | قراءة/كتابة provider التخطيط بنجاح | `gamepad` |
| 14 | الملاحظات | `notes.store.write` | كتابة صفّ في `notes` provider وقراءته | `notes` |

**مصدر الأسماء:** مكوّنات مقيسة في `FEATURE_CATALOG.md` (`PowerPanelService` · `AimService` ·
`ScreenClipService` · `GamepadService` · `OcrProvider` · `GameNotesProvider` ·
`MultiSubScreenSourceActivity`) — لا تخمين وظيفة.

### 2.1 سلّم الوصول — ثلاثة مستويات لا اثنان

وجود القدرة لا يعني قابلية التحكّم فيها. لذلك كل قدرة تحمل، مع الحالة، مستوَى وصول:

| المستوى | ما يعنيه | كيف يُقاس |
| --- | --- | --- |
| `NONE` | لا عقدة ولا واجهة | لا مرشّح يُقرأ |
| `READ_ONLY` | يُقرأ ولا يُكتب | قُرئ، ثم **رُفضت كتابة القيمة الحالية** (SELinux/صلاحية) |
| `READ_WRITE` | يُكتب ويُقرأ | كُتبت القيمة الحالية وقُبلت |

**اختبار الكتابة لا يغيّر شيئًا:** يُكتب ما هو مكتوب بالفعل؛ قبوله دليل قابلية الكتابة، ورفضه جواب
SELinux الحرفي. ولهذا لا يُعلن `READ_WRITE` من اسم جهاز ولا من وجود ملف — التنفيذ في
`SysfsDiscovery.probe` بـ`:core`، وله اختبارات JVM.

**الفرق العملي:** صفّ `READ_ONLY` يبقى ظاهرًا ومفيدًا (تشخيص يُقرأ)، ولا يُعرض كمفتاح يُحرَّك.

---

## 3. ملكية القدرات ومنع التضارب

المقيس: `PowerPanelService` يظهر في **`GameSpace` (66 مكوّنًا) و`GameFloatWindow` (36)**. في الأصل
هما تطبيقان قد يتنافسان على العقدة نفسها. عندنا:

| القدرة | المالك | من يطلب | كيف |
| --- | --- | --- | --- |
| `gamespace.perf.*` (كل عائلة الأداء) | `gamespace` | `floatwindow` · `widget` · `gamepi` | `bindService` إلى عقد الأداء |
| `floatwindow.power.panel_ui` (العرض) | `floatwindow` | `gamespace` | Intent صريح |
| `gamepad.input.*` | `gamepad` | `gamespace` · `gameassist` | عقد اليد |

**قاعدة الملكية:** قدرة واحدة = وحدة مالكة واحدة. مُنشئ `:core` يرفض إعلان مالك ثانٍ، و`record`
يرفض قدرة لم تُعلن — فلا تُخترع قدرة بالخطأ. والـarbiter يرفض التنفيذ المتزامن.

---

## 4. `HardwareControlArbiter` — العقد الداخلي

```kotlin
interface HardwareControlArbiter {
    fun claim(request: HardwareRequest): ClaimResult   // Granted | Denied | Unsupported | BlockedByManualLock
    fun release(token: ClaimToken): Boolean
    fun lockByUser(id: String, by: String = "user")    // قفل المستخدم اليدوي
    fun tripSafety(reason: String)                     // سقف سلامة
}
```

قواعد ملزمة (خطوط المستودع الحمراء + دروس مقيسة):

1. **`SAFETY` مطلق:** يسبق `USER`/`GAME`/`SYSTEM`، ولا يُسبَق، ولا يمنعه قفل يدوي.
2. **قفل المستخدم يعلو على الأتمتة:** ما ضبطه بيده لا يكتبه مالك آليّ؛ الرفض `BlockedByManualLock`
   صريح لا تجاهل صامت. (المنهج من تجربة MaxManager المقيسة: `INV-3`.)
3. **لا كتابة بلا قراءة سابقة، ولا نجاح بلا إعادة قراءة:** النجاح يُقاس بما صار في العقدة، لا بقيمة
   إرجاع `write()`. وإن لم تبلغ القيمة، تُلغى المحاولة وتُعاد القيمة السابقة.
4. **لا فضل كاذب:** قيمة **موجودة أصلًا** تُبلَّغ `AlreadySatisfied` لا `Applied`. العطب المقيس في
   MaxManager: طلب فوق سقف كتبناه بأنفسنا مرّ بلا كتابة وقُرئ نجاحًا، فبقي الجهاز على القيمة القديمة.
   ولذلك فُصل في `GuardedNode`: `Applied` (كتبنا وتحقّقنا) · `AlreadySatisfied` (كانت كذلك) ·
   `NotRealized` (قُبلت الكتابة ولم تبلغ، فأُعيدت).
5. **سياسة القيمة تُعلن:** مفتاح سقف (`max_freq`) يُحكم بـ`MaxCeiling` (قيمة أقل تلبّي)، ومفتاح عددي
   أدنى بـ`MinFloor`، وتعداد/نصّ بـ`Exact`. مقارنة السقف بالتساوي تُصنّف تلبيةً حقيقية فشلًا ثم
   تسترجعها — وهو ما مُنع هنا بالتصميم لا بالانتباه.
6. **`UNSUPPORTED` نهائي في الجلسة:** لا إعادة محاولة عمياء؛ إعادة الكشف تحتاج حدثًا (إعادة تشغيل،
   إعداد، منح إذن).
7. **كل قرار يُسجَّل:** `id`, `state`, `reason`, `evidence`, `callerUid` — التشخيص بلا سجل تخمين.

---

## 5. كيف تُعرض الحالة للمستخدم

| الحالة | العنصر | النصّ | السلوك |
| --- | --- | --- | --- |
| `AVAILABLE` + `READ_WRITE` | فعّال | القيمة المقيسة | يعمل |
| `AVAILABLE` + `READ_ONLY` | للقراءة | القيمة + «قراءة فقط» | لا يكتب |
| `UNSUPPORTED` | معطّل | «غير مدعوم على هذا الجهاز» + السبب الحرفي | لا ينفّذ شيئًا |
| `NEEDS_PERMISSION` | فعّال بصريًّا | «يحتاج إذنًا» | يفتح شاشة الإذن |
| `UNKNOWN` | مخفي أو بعلامة استفهام | «لم يُقس بعد» | لا يدّعي حالة |

**ولا أرقام مخترعة:** إن لم يصل تحديث، تُعرض القيمة الأخيرة **مع وقتها** أو `—`، لا صفر (الصفر يعني
«مقيس وصفر»، وهو رقم كاذب إن لم يُقس).

---

## 6. ما لا يفعله هذا السجل

- لا يدّعي أن أي قدرة `AVAILABLE` على أي جهاز قبل قياسها؛ كل الأجهزة هنا **غير مقيسة** بعد.
- لا يُصدر «تقرير قدرات» نهائيًّا قبل تشغيله على جهازين (بند قبول المرحلة 5 المفتوح).
- لا يعرّف قدرات من مكوّنات `UNVERIFIED` (253 عقدًا في الأصل) — تُضاف عند إثباتها.
