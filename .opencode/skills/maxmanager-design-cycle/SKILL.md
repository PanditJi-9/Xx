---
name: maxmanager-design-cycle
description: دورة تصميم MaxManager الستّ — تحليل مرجع، إنشاء تصميم، تنفيذ Compose، التقاط لقطة، مقارنة بصرية، إصلاح فروق. استخدمها عند أي عمل UI/UX في هذا المستودع، أو عند مقارنة شاشة بلقطة المالك.
---

# MaxManager Design Cycle

اتبع `docs/ai/DESIGN-TOOLS.md` حرفيًّا. الأدوات قد تكون غير متاحة (مصادقة/جهاز/مضيف)؛ **قل ذلك ولا تستبدل أداة بأخرى بصمت**.

## ⓪ قبل البدء
1. اقرأ `docs/ai/DESIGN-TOOLS.md` (الحالة المقيسة) و`AGENTS.md` §0.4 (لا تقليد هوية) و§0.10 (لا Gradle محليًّا).
2. إن كان المرجع لقطة: قِسها بـ`python3 tools/read_image_text.py` عند الحاجة، ولا تخمّن نصًّا.
3. سمِّ ما هو متاح فعلًا بالقياس لا بالاسم:
   - `figma-ui` = خادم يعمل، لكن `figma_write`/`figma_read` تحتاج **Figma Desktop + Plugin Bridge**.
   - `openpencil` = خادم بـ١٣٢ أداة، لكن استدعاء المستند يحتاج **تطبيق OpenPencil عاملًا**.
   - `comfyui` = خادم بـ٤١ أداة و٥٦ حزمة، لكن التوليد يحتاج **خلفية ComfyUI** (لا خلفية هنا).
   - `adb` = ٨١ أداة و`doctor` يعمل، لكن الالتقاط يحتاج **جهازًا** (لا جهاز هنا).
   - `figma` (remote) = يحتاج **OAuth**؛ `pen` CLI = يحتاج **حساب pen.dev**؛ `openstitch` = **ليس MCP** ويحتاج Docker+Ollama.
   - ما يعمل بلا مصادقة/جهاز: `impeccable` + `ui-ux-pro-max` + قواعد `ui/design/`.

## ① تحليل المراجع
- استخرج الفروق المرقّمة D-xx بمسارات `file:line`.
- لاستعلام التصميم: `python3 .opencode/skills/ui-ux-pro-max/scripts/search.py "<query>" --design-system -p "MaxManager …" --stack jetpack-compose`. **v2.15.0 لا يدعم `--rtl`**؛ للعربية اعتمد `ui/design/` عندنا لا خيارًا غير موجود.
- Figma MCP لا يقرأ لقطة محلية؛ يحتاج رابط Figma بعد المصادقة.

## ② إنشاء التصميم
- إن توفر Pencil/OpenPencil/Figma: أنشئ/عدّل التصميم ثم صدّره. تحقّق أولًا من المضيف:
  `figma_status` → `pluginConnected:true`، أو `openpencil.list_pages` بلا خطأ.
- للأصول السينمائية: `comfyui` جاهز كخادم لكنه لا يولّد بلا خلفية ComfyUI؛ لا تدّعِ صورة مولّدة.
- وإلا: اكتب العقد في Compose/design tokens داخل `manager/app/src/main/java/nd/max/ui/design/`، وأعلن أن أداة التصميم غير متاحة.

## ③ التنفيذ
- Kotlin/Compose داخل نطاق الملفات، والنصوص في `values/` و`values-ar/` فقط.
- لا كتابة sysfs من الواجهة، ولا تليمتري مصنَّع (ADR-07/ADR-11).
- البوابات: `kt_balance` · `code_health` · `i18n_coverage` (+`--prune all`) · `design_tokens` · `resource_compile` · `provenance_guard` · `git diff --check`.

## ④ التقاط الصور
- جهاز موصول: أداة `adb` مثل `screenshot`/`describe_ui`، أو `adb exec-out screencap -p`.
- بلا جهاز: استخدم لقطة المالك واكتب «لم تُلتقط هنا».
- لا تستخدم أدوات Gradle في ADB MCP؛ البناء عبر GitHub Actions فقط.

## ⑤ المقارنة
- قارن اللقطة المرجعية بلقطة التنفيذ: الموضع، الحجم، التباين، RTL، الحالات الفارغة.
- `diff_visual` من OpenPencil فقط إذا كان التطبيق عاملًا ومفتوحًا.
- المخرج قائمة P0/P1/P2 بدليل، لا «يبدو مطابقًا».

## ⑥ إصلاح الفروق
- أصلح P0 ثم P1، أعد البوابات واللقطة.
- سجّل في `HANDOFF` عند نهاية المهمة فقط (§0.3)، وما لا يُثبته اختبار يبقى «غير مُتحقَّق».
