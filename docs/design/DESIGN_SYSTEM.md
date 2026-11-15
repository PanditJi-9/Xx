# نظام التصميم — MaxManager

> **حالة الملف:** مرجعٌ مشتقٌّ **من الكود القائم** لا من ذوقٍ جديد. لا يُنشئ نظامًا موازيًا:
> مصدر الحقيقة يبقى `ui/design/MaxTokens.kt` و`ui/component/MaxMotion.kt` و`ui/theme/`.
> كل رقم هنا مقروء من هناك، وأي اختلاف بين هذا الملف والكود ⇒ **الكود هو الصحيح**.

## ١. المصادر (لا تُعدّل بلا سبب، ولا تُنسخ قيمها حرفيًّا في الشاشات)

| الملف | ما يحكمه |
| --- | --- |
| `manager/app/src/main/java/nd/max/ui/design/MaxTokens.kt` | `MaxSpace` · `MaxRadius` · `MaxAlpha` · `MaxSize` · `MaxDuration` · `MaxTone` · `MaxSectionSpec` |
| `.../ui/component/MaxMotion.kt` | `MaxMotion` (الحركة والينابيع) وقارئ تقليل الحركة |
| `.../ui/theme/` (`MaxTypography.kt` · `Type.kt` · `Shape.kt` · `Theme.kt` · `Fonts.kt` · `ThemeSettings.kt`) | السلّم الطباعيّ والأشكال والسمة |
| `.../ui/subscreens/GameSpaceFrame.kt` | `GameSpacePalette` (هويّة Game Space) وقيم إطارها |
| `manager/app/src/main/res/values/` + `values-ar/` | النصوص — الإنجليزية والعربية فقط |

**البوابة:** `python3 tools/design_tokens.py --assert` تُسقط أي حرفيّة تصميم فوق السقف. الشاشة التي
تحتاج رقمًا جديدًا تُضيف رمزًا في الطبقة، لا رقمًا في الملف.

## ٢. المسافات (`MaxSpace`)

| الرمز | القيمة | الاستعمال |
| --- | --- | --- |
| `hairline` | 2 | فصل شعريّ |
| `xs` · `sm` · `md` | 4 · 8 · 12 | داخل العنصر |
| `lg` · `xl` · `xxl` | 16 · 20 · 28 | بين العناصر والأقسام |
| `gutter` | 20 | **حاشية المحتوى الواحدة** — كل المحتوى على هذا الحدّ |
| `section` | 28 | بين قسمين |
| `row` | 8 | داخل الصفّ بين عناصره |
| `groupPadding` | 4 | صفوف المجموعة |
| `rowPaddingHorizontal` · `rowPaddingVertical` | 14 · 12 | حشو صفّ القائمة |
| `pageBottom` | 40 | أسفل الصفحة |
| `bottomBarReserve` | 112 | مساحة محفوظة للشريط السفليّ — لا عنصر تحته |

## ٣. الاستدارة (`MaxRadius`)

`control 12` · `row 14` · `group 22` · `sheet 28` · `pill 100` · `tile 18` · `inset 16` ·
`chip 10` · `deck 20` · `tick 3` (dp).

- **الحبّة (pill) للحالة وحدها** (وسم، مرشّح، شريحة)، **لا للفعل الأساسيّ**: الفعل الأساسيّ
  `control`، لأن الاستدارة الكاملة تُلبس الفعل شكل الوسم فتُفقد الأولويّة.
- عائلة واحدة لكل مستوى: البطاقة `group`، صفّها `row`، عنصرها `control`.

## ٤. الأبعاد (`MaxSize`)

`minTouchTarget 48` (أرضيّة لا تُكسر) · `iconGlyph 20` · `iconGlyphSmall 16` ·
`rowIconContainer 34` · `iconContainer 40` · `hairlineBorder 1` · `emphasisBorder 1.5` ·
`activeRing 2`.

## ٥. الشفافيّة (`MaxAlpha`) — «الطبقة» بدل الظلّ

`border .16` · `borderStrong .28` · `toneWash .045` · `toneContainer .10` ·
`toneContainerStrong .18` · `disabledContent .38` · `supportingText .80` · `edgeLight .22` ·
`haloGlow .07`.

## ٦. المدّة (`MaxDuration`) والحركة (`MaxMotion`)

`instant 90` · `quick 160` · `standard 240` · `deliberate 360` ms. وما زاد على `deliberate`
**زينة ممنوعة**. التفصيل في `MOTION_GUIDELINES.md`.

## ٧. المعنى (`MaxTone`)

`Neutral` · `Accent` · `Positive` · `Caution` · `Critical` · `Inactive`.
**اللون ليس حامل المعنى وحده**: يلزمه نصّ وأيقونة. و`Caution/Critical/Positive` **لا** تُؤخذ من
المخطّط الديناميكيّ (Material You) حتى لا يمحوها لون الجهاز.

## ٨. الهويّة اللونيّة (`GameSpacePalette`، Game Space)

الخلفيّة والعمق: `Void 050318` · `Deep 0A0420` · `Panel 171A2E` · `PanelRaised 1E2736` ·
`Glass 14142B` · `GlassDeep 0A0A16` · `Plum 1A1140` · `Night 0E0A26` · `PanelSelected 262046` ·
`Track 2D2250`.
التمييز: `Violet 7C3AED` · `VioletDeep 55309F` · `VioletSoft A855F7` · `Magenta FF2D6B` ·
`Cyan 38BDF8` · `Lavender D8B4FE` · `Blue 3B6CF6`.
المعنى: `Positive 4ADE80` · `Mint 34D399` · `Caution/Orange FF8A3D` · `Red EF4444` ·
`RedBright F87171`.
النصّ: `Ink F2F3F8` · `Muted 8B86A8` · `Steel 94A3B8`. الحدود: `Hairline 33A855F7`.
أبعاد الإطار: `GameSpaceRailWidth = MaxSize.minTouchTarget*2` ·
`GameSpaceStatusHeight = MaxSize.minTouchTarget + MaxSpace.sm` · `GameSpaceBoardMaxWidth 300` ·
`GaugeDiameter 72`.

> **ملاحظة ملزمة:** مهارة `design-taste-frontend` تحمل قاعدة «لا بنفسجيّ ولا لافندر» من أصلها
> الشبكيّ — وهي **ملغاة هنا** لأن هويّة Game Space مبنيّة عليهما. تُقرأ للفكّة من التوليد
> العامّ (composition/typography/density)، لا لتغيير اللوحة.

## ٩. السلّم الطباعيّ

الترتيب الملزم من الأكبر إلى الأصغر، **ولا طبقتان بالحجم نفسه** (من `MaxSectionSpec`):
عنوان صفحة (`headlineSmall`+) ← عنوان قسم (`titleMedium` عريض) ← عنوان بطاقة (`titleSmall` في
`MaxCard`) ← وصف مساند (`bodySmall`).
عقد عنوان القسم: شرطة `MaxSpace.xs × (MaxSpace.lg+2)` باستدارة 2 **بجانب** الاسم، ونسخة أفقيّة
`MaxSpace.xxl × MaxSpace.xs` **فوق** الاسم، وفجوة `MaxSpace.md`.

## ١٠. قواعد الشاشة

1. **حاشية واحدة** (`MaxSpace.gutter`) لكل المحتوى — الحدود متساوية.
2. **عنصر مهيمن واحد** لكل شاشة، وكل ما سواه أدنى منه بالحجم أو باللون.
3. **كل حالة مصمّمة:** فارغة · تحميل · خطأ · معطّلة · مجهولة. المجهول يُعرض «غير معروف» لا صفرًا.
4. **الحالة بالفعل لا بالزينة:** الفعل الأساسيّ واحد (فعل + مفعول: «ابدأ التسجيل»).
5. **لا ظلّ ولا تدرّج لسدّ نقص في التسلسل** — التسلسل بالمسافة والطبقة والحجم.
6. **اللمس:** كل عنصر تفاعليّ ≥ `MaxSize.minTouchTarget`، ولا شيء تحت `bottomBarReserve`.
7. **اللغة:** النصّ الجديد في `values/` **و**`values-ar/` معًا.
8. **الأداء جزء من الذوق:** لا صورة بلا حدّ أبعاد، ولا حساب في مسار الرسم، ولا تخصيص في إطار.
9. **القياس قبل الوصف:** لا «يبدو مطابقًا» — راجع `VISUAL_QA.md`.
