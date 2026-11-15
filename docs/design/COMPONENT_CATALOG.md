# كتالوج المكوّنات — MaxManager

**القاعدة قبل أي مكوّن جديد:** ابحث عن القائم أوّلًا (`AGENTS.md` §0.5). فإن وُجد ⇒ **حسّنه**؛ وإن لم
يوجد ⇒ أضِف بعضويّة واحدة في الطبقة الصحيحة، لا نسخة ثانية من سلوك موجود.

## الطبقات

| الطبقة | المسار | ما يوضع فيها |
| --- | --- | --- |
| الرموز | `ui/design/MaxTokens.kt` · `ui/component/MaxMotion.kt` · `ui/theme/` | القيم والحركة والسمة فقط |
| الهيكل والسطح | `ui/design/` | `MaxScreen` · `MaxListScreen` · `MaxSplitScreen` · `MaxFullScreen` · `MaxCard` · `MaxCardShell` · `MaxSection` · `MaxGroup` · `MaxRow` · `MaxScrollRow` · `MaxEdgeFade` · `MaxVerticalEdgeFade` · `MaxInk` |
| البيانات | `ui/design/` | `MaxMetricLine` · `MaxMetricReadout` · `MaxMetricProvenance` · `MaxDeltaRow` · `MaxSparkline` · `MaxProgressStrip` · `MaxUsageBar` · `MaxWeightBar` · `MaxForecastChart` · `MaxCurvePlot` · `MaxShareTrack` · `MaxGovernorStrip` · `MaxFrequencyTrack` · `MaxEqualizerBars` · `MaxDataTrust` · `MaxTrustChip` |
| التحكّم | `ui/design/` | `MaxSwitchRow` · `MaxSliderRow` · `MaxSegmented` · `MaxChoiceRow` · `MaxSelectableTile` · `MaxSearchField` · `MaxTabStrip` · `MaxViewMenu` · `MaxCommandMenu` · `MaxContextMenu` · `MaxDrawer` |
| الحوار | `ui/design/` | `MaxConfirmDialog` · `MaxInputDialog` · `MaxTabbedDialog` · `MaxHelpAction` |
| الحالة والانتباه | `ui/design/` · `ui/component/` | `MaxConditionGate` · `MaxConditionNotice` · `MaxConditionPanel` · `MaxOwnerBadge` · `MaxBullet(s)` · `MaxCollapsibleGroup` · `MaxNavigationRow` · `MaxCausalStage` |
| وسائط وGame Space | `ui/component/` | `GamePanelSurface` · `GamePanelCockpit` · `GamePanelHandle` · `GamePanelPortrait` · `HudSurface` · `CockpitRails` · `CockpitDeck` · `CubeParts`/`CubeCanvas`/`CubeRail` · `IslandSurface`/`IslandCapsule`/`IslandCardGallery` · `CyberRotaryKnob`/`CyberCurveEqualizer`/`CyberAudioVortex` |
| حالة المحتوى | `ui/component/ContentStateComponents.kt` | `MaxContentState` · `MaxEmptyState` · `MaxLoadingState` · `MaxErrorState` · `MaxInlineEmptyState` · `MaxStateTransition` |
| السطح والزينة | `ui/component/` | `MaxSurface` · `MaxSurfaceBox` · `MaxSnackbarHost` · `MaxStatusPill` · `MaxReveal` · `MaxAnimatedHourglass` · `MaxRollingTicker` · `MaxOrbitScene` · `MaxSignatureMark` · `ScreenAccentProvider`/`ScreenAccentGlyph` |

## عقد المكوّن

كل مكوّن في الطبقة يقبل نطاق تعديل (modifier) ويحترمه، ويأخذ قيمه من الرموز لا من الوسائط الحرفيّة،
ويُعلن حالته (`enabled`, `selected`, `tone`) صراحةً بدل أن يستنتجها من اللون. ومكوّن بلا مستهلك
**يُحذف** لا يُترك «للمستقبل» (`VisualIdentityTest` تُسقط المفتاح/الرمز اليتيم في النصوص، والعرف نفسه
على المكوّنات).

## كيف تضيف

1. `grep` باسم السلوك لا بالاسم المرشّح (مثال: «لوحة جانبيّة» تُبحث بـ`Drawer`/`Rail`).
2. اقرأ أقرب مكوّن قائم، وحدّد الفارق الحقيقيّ (حالة ناقصة، توقيت، حفظ/استرجاع).
3. إن كان الفارق حشوًا ⇒ وسّع القائم بوسيط افتراضيّه يحفظ السلوك القديم.
4. أضِف المكوّن الجديد في `ui/design/` (وليس في الشاشة)، وأعطه رمزًا لكل رقم.
5. لا تُضِف تبعيّة Gradle لمكوّن يُرسم بـCompose أصلًا.
