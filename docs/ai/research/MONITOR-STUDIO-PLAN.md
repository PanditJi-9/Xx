# استوديو المراقبة — خطة التنفيذ (Monitor studio)

**الحالة:** خطة مكتوبة قبل التنفيذ. لا بند منها «مشحون» حتى يمرّ البناء الكامل.

## ١. ما هو

مراقب واحد عائم عندنا اليوم (`FpsOverlayService` + `OverlayWindow` واحد + `HudSurface`). الاستوديو
يجعلها **عدّة مراقبات مستقلّة** في الوقت نفسه: مراقب للإطارات أعلى اليسار، ومراقب للحرارة أسفل
اليمين… لكل واحد حقوله وشكله وحجمه وموضعه.

## ٢. ما لا يُبنى (قيود مقيسة)

| القيد | السبب |
| --- | --- |
| لا `HudSampler` ثانٍ | حلقة واحدة بخريطة مالكين؛ تكرارها يعني قراءتين متعارضتين لنفس العتاد |
| لا حقل GPU في حلقة الثانية | `HudSession` §الحدّ المعلن: اكتشاف GPU ثقيل ويخصّ Game Space |
| لا خدمة ثانية | `FpsOverlayService` تصبح مضيفًا لقائمة نوافذ، لا نسخة لكل مراقب |
| لا `MaxDestination` جديد | يغيّر عدّ الشاشات ٦٨ ويجرّ README/الأصول/المتجر — القسم داخل شاشة التراكب القائمة |
| لا تفضيلات جديدة لمراقب واحد | المفاتيح الحالية `hud_*` تبقى للمراقب الأول (توافق خلفي) |

## ٣. النموذج

```kotlin
data class MonitorSpec(
    val id: String,              // "hud" للمراقب الأول، ثم "m1", "m2"…
    val fields: Set<HudField>,
    val form: HudForm,
    val arrangement: HudArrangement,
    val colorHex: String,
    val textSizeSp: Float,
    val backgroundAlpha: Float,
    val widthScale: Float,
    val showGraph: Boolean,
    val graphSpan: Int,
    val snapEdges: Boolean,
    val posX: Int,
    val posY: Int
)
```

- الحدّ الأقصى **٣ مراقبات** (`MAX_MONITORS = 3`): كل مراقب نافذة + تركيب Compose + مالك عيّنات،
  والثلاثة يبقون داخل ميزانية الذاكرة المعتادة. الزائد يُرفض برسالة، لا يُقصّ صامتًا.
- المالك في `HudSampler` = `"monitor:<id>"` (و`"overlay"` يبقى للمراقب الأول فلا ينكسر استئناف
  `PersistRestorer`). الحقول تُجمع في `unionFields()` كما هي — قراءة واحدة للجميع.
- التخزين: `SharedPreferences` واحدة (`maxmanager_fps_overlay`) بمفتاح `hud_monitors` = JSON
  لقائمة `MonitorSpec`، مع **قراءة المفاتيح القديمة** إن غاب المفتاح (ترحيل صامت لا يفقد إعدادًا).
- المراقب الأول يظلّ هو ما تقرأه `FpsOverlayPrefs.load/save` اليوم؛ لا تتغيّر واجهتها.

## ٤. الخدمة

```kotlin
private val panels = LinkedHashMap<String, OverlayWindow>()   // id -> نافذة

onStartCommand:
  specs = MonitorPrefs.load(this)
  specs.forEach { spec ->
      if (panels[spec.id] == null) {
          val window = OverlayWindow(this, avoidCutout = true,
              onMoved = { x, y -> MonitorPrefs.savePosition(this, spec.id, x, y) })
          val mounted = window.mount(dragEnabled = true,
              snapToEdges = { MonitorPrefs.load(this).first { it.id == spec.id }.snapEdges }) {
              MonitorPanel(spec)         // نفس `HudSurface` + `HudLive.snapshot`
          }
          if (mounted) {
              MonitorPrefs.loadPosition(this, spec.id)?.let { (x, y) -> window.restorePosition(x, y) }
              panels[spec.id] = window
              HudSampler.start(this, owner = "monitor:${spec.id}", fields = { spec.fields })
          }
      }
  }
  // إزالة ما حُذف من الإعداد
  (panels.keys - specs.map { it.id }.toSet()).forEach { id ->
      panels.remove(id)?.unmount(); HudSampler.stop("monitor:$id")
  }
```

- `onDestroy` يُنظّف القائمة كلها بلا استثناء.
- **ولا ادّعاء نجاح:** إن فشل `mount` لمراقب، يُسقَط من `panels` وتُوقف عيّناته ويُبلَّغ في الشاشة
  (عدّاد «مراقبات عاملة: ٢ من ٣») بدل أن يبدو العدد مطابقًا للإعداد.

## ٥. الواجهة (داخل `FpsOverlayScreen`)

قسم جديد `hud_section_monitors` أسفل الحقول:

- صفّ لكل مراقب: اسمه + حقوله المختصرة + زرّ «تعديل» + زرّ «حذف».
- «إضافة مراقب» معطّل عند بلوغ `MAX_MONITORS` مع سبب ظاهر.
- اختيار المراقب يفتح نفس عناصر التحكّم القائمة (الشكل/الحقول/اللون/الحجم/المنحنى/الالتصاق)
  **مقيَّدة على المراقب المختار** — لا شاشة إعداد ثانية ولا نمط جديد في `ui/design`.
- كل النصوص في `values/` و`values-ar/` فقط (ADR-14).

## ٦. معيار القبول

1. البوابات السبع خضراء، و`:app:compileReleaseKotlin` أخضر.
2. `HudSessionTest` و`FpsOverlayPresetTest` بلا تعديل (لا يتغيّر `HudField` ولا `HudPreset`).
3. عدّ الشاشات يبقى ٦٨ (`gen_readme_assets`… لا تُشغَّل أصلًا لأن لا مقصد جديد).
4. المراقب الأول يعمل كما كان بالضبط عند غياب `hud_monitors`.
5. ما لا يُثبته إلا جهاز (تركيب نوافذ متعدّدة، سحب كل نافذة، استجابة اللمس) يُكتب
   «يحتاج جهازًا» في التسليم.
