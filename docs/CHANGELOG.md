# سجلّ التغييرات

> الجولات مرتّبة من الأحدث. كل سطر يقابل التزامًا على `main`، والتفصيل في `docs/ai/HANDOFF.md`.

## ROUND-CLOSE-47 — الحارس الذي أسقط البناء
- `c4e83217` — `fix(test): treat parent = null as no parent in the door guard`: `HmaCompanion`
  يعلن `parent = null`، وكان الحارس يعدّه اسم أب. البناء صار **success** (`42892510611`).

## ROUND-CLOSE-46 — المختبر يقيس الشحن
- `f8dd38f7` — `feat(lab): measure charging instead of labelling it`: نافذة ٦ ثوانٍ على عدّاد
  الشحنة، الاتجاه من إشارة الفرق، و`ChargeMath` + ٥ اختبارات، وزرّ «أعد القياس».

## ROUND-CLOSE-45 — صندوق الأدوات ثلاثة نطاقات
- `15f12a79` — `feat(control): split the tool box into three named bands`: ١٠ صفوف مسطّحة ←
  «النسخ والسجلّ والصلاحيات» · «الأسطح الحيّة» · «أدوات متقدّمة»، بلا نقل وجهة واحدة.

## ROUND-CLOSE-44 — المختبر اختبارُ عتاد
- `45fc2610` — `feat(lab): keep the device lab a device test and add camera and radio checks`:
  تراجع مُعلَن عن إحالة الأدوات إلى المختبر، وإضافة قراءة الكاميرات والاتصالات، وخمس بطاقات نوع شاشة.
- `5f459ea5` — `test(nav): guard every visible destination against having no door`.

## ROUND-CLOSE-43 — المختبر بيت أدواته (ثم رُفض)
- `c54ffdec` — `feat(ux): make the device lab the home of its advanced tools (M1b)` — **تراجعت
  في ROUND-CLOSE-44 بأمر المالك**: المختبر اختبار عتاد لا صندوق أدوات.

## ROUND-CLOSE-42 — M1a
- `3eec9e95` — `feat(nav): move process manager and battery history to their hubs (M1a)`:
  أبناء `Control` ٢٤ ← ٢٢، والعمق ٣٩/٢٢ ← ٣٧/٢٤.

## ROUND-CLOSE-41 وما قبله
- `78633a8d` — `docs: measured product map — 69 destinations, five structural defects, M0-M4 migration`.
- التفصيل الكامل في `docs/ai/HANDOFF.md` (سجلّ الجولات)، وخطة المنتج في `docs/ai/PRODUCT-MAP.md`.
