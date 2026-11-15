# معمارية الواجهة — MaxManager

> كل ما هنا **مقروء من الكود**. لا يُفترض إطارٌ ولا نمط: Jetpack Compose وحدها، ونقطة الملاحة واحدة.

## ١. الهيكل

- **الإطار:** Jetpack Compose (لا XML Views في الشاشات الجديدة). الموضوع في `ui/theme/`،
  والرموز في `ui/design/MaxTokens.kt`.
- **الملّاح:** `MainActivity.kt` هو المالك الوحيد لـ`NavHost` (`rememberNavController`)،
  و`startDestination` يُختار من `hasCompletedGetStarted`.
- **إجراءات الملاحة المشتركة:** `ui/navigation/MaxNavActions.kt` — تبويبٌ يُبدَّل بـ
  `popUpTo(navController.graph.startDestinationId) { saveState = true }` ليحفظ كل تبويب موضعه.
- **الشاشات:** `ui/mainscreens/` = جذور التبويبات · `ui/subscreens/` = صفحات التفصيل ·
  `ui/component/` = مكوّنات مركّبة · `ui/design/` = الطبقة المشتركة.

**ممنوع مسار ملاحة ثانٍ.** أي شاشة جديدة تُسجَّل في الملّاح نفسه، ولا تُفتح بحوارٍ يعمل كصفحة.

## ٢. الهيكل الخارجيّ للشاشة (واحد من أربعة)

من `ui/design/MaxScreenScaffold.kt`:

| الدالّة | الاستعمال |
| --- | --- |
| `MaxScreen(title, onBack, subtitle, accentIcon, accent, condition, banner, snackbarHostState, actions, floatingAction, topBar, scrollEnabled, scrollState, content)` | صفحة عاديّة بشريط علويّ واحد |
| `MaxListScreen` | قائمة طويلة (Lazy) بشريط وحالة محتوى |
| `MaxSplitScreen` | عمودان (لوحتان) — وفيه يُعلَن العمود الفعّال بـ`MaxSize.activeRing` |
| `MaxFullScreen` | شاشة بلا شريط (لوحة/تشغيل/معاينة) |

الشريط العلويّ ينهار بالتمرير (`exitUntilCollapsedScrollBehavior`). وشاشةٌ ذات تبويبات تمرّر
`scrollState` بنفسها ليبدأ التبويب الجديد من أعلى القسم.

## ٣. الحالة

- **مالك واحد للحالة.** الحالة التي تعيش مع الشاشة في `remember`/`rememberSaveable`، والتي تعيش
  أطول في حامل حالة (`ViewModel` كما في `ui/mainscreens/HomeScreen.kt` و`MaxAiScreen.kt`).
  **ولا نسخة ثانية** في `object` أو في الأب (`ADR-18`/`ADR-11`).
- الحالة تُمرَّر إلى الأسفل كقيمة وأحداث (`value` + `onEvent`)، لا يُقرأ المصدر من داخل المكوّن.
- **المجهول يُعرض مجهولًا** (`status_unknown`)، لا صفرًا ولا فراغًا (`ADR-07`).

## ٤. التكيّف (بلا تخطيط ثانٍ)

`ui/component/AdaptiveLayout.kt` يوفّر وسيطين فقط:

- `Modifier.maxAdaptiveContentWidth()` — سقف 920dp (المحتوى العريض).
- `Modifier.maxAdaptiveControlWidth()` — سقف 760dp (شاشات التفضيلات).

على الهاتف لا يتغيّر شيء (العرض أقلّ من السقف أصلًا). **ولا تخطيط ثانٍ للتابلت**: التوسيع
يُضاف إلى العمود نفسه، لا يُبنى مسار موازٍ. والوضع الأفقيّ يُقاس لا يُفترض.

## ٥. الحالات الأربع + المجهولة

`ui/component/ContentStateComponents.kt`: `MaxContentState` · `MaxEmptyState` · `MaxLoadingState` ·
`MaxErrorState` · `MaxInlineEmptyState` · `MaxStateTransition`. وكل شاشة تُعلن ماذا تعرض في كل
حالة — والشاشة التي لم تُصمَّم حالتها الفارغة تُقرأ معطّلة عند أوّل استخدام حقيقيّ.

## ٦. كيف تُضاف شاشة (الترتيب الملزم)

1. `/ui-audit` أو `ui-architecture` لتخطيط الشجرة وإعادة الاستعمال.
2. `/ui-blueprint` — الشجرة والحالة والتفاعلات والحالات والنصوص، **موافقة قبل الكود**.
3. `/ui-implement` عبر وكيل `android-engineering` — الهيكل أحد الأربعة، والقيم رموز.
4. `/ui-visual-test` — لقطة وقياس (أو إعلان تعذّره لغياب جهاز).
5. `/ui-review` ثم `/ui-regression`.
