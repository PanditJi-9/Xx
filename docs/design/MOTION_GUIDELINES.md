# دليل الحركة — MaxManager

**الغاية:** الحركة تُفسّر انتقال حالة. وما لا يفسّر حالة فهو زينة تُبطئ الجهاز وتُشتّت.

## المدّة

`MaxDuration`: `instant 90` · `quick 160` · `standard 240` · `deliberate 360` ms.
`MaxMotion`: `fast 160` · `standard 260` · `emphasis 420`.

| الحالة | المدّة |
| --- | --- |
| استجابة لمس (تغيير لون/حدّ) | `instant` أو `quick` |
| ظهور بطاقة/صفّ | `standard` |
| فتح لوحة أو حوار | `standard` أو `deliberate` |
| تغيّر مؤشّر قياس (عدّاد، مؤشّر) | `MaxMotion.standard` أو الينبوع |

ما زاد على `MaxDuration.deliberate` **ممنوع**.

## الينابيع (`MaxMotion`)

`controlSpring` (LowBouncy / Medium) للعناصر التفاعليّة · `contentSpring` (NoBouncy /
MediumLow) للمحتوى والحاويات · `needleSpring` (MediumBouncy / Low) للمؤشّرات فقط.
الينبوع للموضع والمقاس؛ والمدّة للتلاشي والانزلاق.

## الدخول والخروج

`MaxMotion.enter()` = `fadeIn` + `slideInVertically(offset = it / 14)` بـ`FastOutSlowInEasing`.
`exit` = `fadeOut` + `slideOutVertically(-it / 18)`.
الإزاحة **جزء من ارتفاع العنصر** لا قيمة مطلقة — فتبقى معقولة على أي مقاس.

## تقليل الحركة

يُقرأ `Settings.Global.ANIMATOR_DURATION_SCALE` **مرّة عند التركيب**؛ `0` ⇒ سكون تامّ.
والمجهول ⇒ حركة طبيعيّة (لا تُلغَ بصمت). **والسكون لا يُخفي شيئًا:** ما يظهر بالحركة يجب أن
يكون ظاهرًا بالكامل عند إلغائها.

## الممنوع

- تدرّج زخرفيّ متحرّك، أو دوران لا ينتهي، أو نبض مستمرّ.
- حركة على كل عنصر عند الدخول (الحركة المتكرّرة تُقرأ عطبًا).
- مكتبة حركة جديدة: `MaxMotion` يكفي.
- `animateContentSize` على قائمة طويلة، أو أي تخصيص داخل `onFrame`/`update`.
- ادّعاء «سلس» بلا قياس على جهاز (Jank/Frame timing). ما لم يُقَس يُكتب «لم يُقَس».

## الكلفة

انقل ما يتحرّك إلى `graphicsLayer`/`transform` و`alpha` (خصائص لا تُعيد التركيب)، واحصر نطاق
الحركة في المكوّن المتحرّك، وتحقّق من عدم إعادة تركيب الأب.
