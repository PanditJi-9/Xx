# DESIGN-TOOLS — دورة تصميم MaxManager والمصادر الرسمية

> **الغرض:** تثبيت أدوات التصميم التي طلبها المالك، وربطها بدورة عمل واحدة:
> **تحليل المراجع ← إنشاء التصميم ← التنفيذ ← التقاط الصور ← المقارنة ← إصلاح الفروق البصرية.**
> **القاعدة الحاكمة:** لا يُقال «ثُبّت» أو «اختُبر» إلا بدليل أمر ومخرجه. وما يحتاج جهازًا أو
> مصادقة يُكتب بحالته، ولا يُستبدل بأداة أخرى بصمت (أمر المالك ٢٠٢٦-١١-١٤).

## 0. الحالة المقيسة (تُحدَّث بعد كل قياس، لا تُدّعى)

| الأداة | المصدر الرسمي | مثبّتة؟ | متوافقة مع OpenCode؟ | الاختبار الفعلي | الحالة |
| --- | --- | --- | --- | --- | --- |
| **Figma MCP** (remote) | `https://mcp.figma.com/mcp` · `developers.figma.com/docs/figma-mcp-server` | نعم، في `mcp.servers.figma` | نعم — OpenCode مذكور في جدول توافق Figma الرسمي | `opencode mcp list` → `⚠ needs authentication`؛ `mcp auth` أصدر رابط OAuth ثم أُلغي بلا بيانات دخول | **مصادقة فقط** — لا tool call قبل تسجيل الدخول |
| **Android ADB MCP** | `github.com/iksnerd/adb-mcp` (MIT) | **نعم** — `~/.local/bin/adb-mcp` v0.25.0 من المصدر الرسمي | نعم — stdio محلي في `mcp.servers.adb` | `doctor` عمل عبر OpenCode وDeepSeek (جلسة `ses_e25bb7477ffexnJHo6UXwwFB3D`)؛ ٨١ أداة؛ لا جهاز موصول | **مثبت ومختبر**؛ الالتقاط يحتاج جهازًا |
| **Pencil MCP** (pen.dev) | `docs.pencil.dev/getting-started/ai-integration` | CLI فقط بعد التثبيت | **لا خادم MCP مستقل**: الـMCP يُقدَّم من تطبيق pen.dev المكتبي/إضافة IDE عبر stdio | لا يمكن اختباره بلا تطبيق يعمل؛ CLI التفاعلي يحتاج حسابًا/مفتاحًا | **محجوب بالمضيف والمصادقة** — لا يُستبدل بـOpenPencil |
| **OpenPencil MCP** (مرجع المستودع الثاني) | `github.com/open-pencil/open-pencil` (MIT) | **نعم** — `@open-pencil/mcp@0.16.0` في `/root/openpencil` | نعم — stdio في `mcp.servers.openpencil` | tools/list = **١٣٢ أداة**؛ `list_pages` أعاد `OpenPencil app is not connected` | **مثبت كخادم**؛ استدعاء المستند يحتاج تطبيقًا عاملًا — مختلف عن pen.dev |
| **Figma UI MCP** | `github.com/TranHoaiHung/figma-ui-mcp` (MIT) | **نعم** — `figma-ui-mcp@2.5.30` في `/root/figma` | نعم — stdio في `mcp.servers.figma-ui` | tools/list = ٥؛ `figma_status` → `pluginConnected:false`, bridge 38451 | **مثبت ويعمل كخادم**؛ الكتابة/القراءة تحتاج Figma Desktop + Plugin Bridge (بلا API key/OAuth) |
| **ComfyUI MCP** | `github.com/artokun/comfyui-mcp` | **نعم** — `comfyui-mcp@0.52.206` في `/root/comfyui` | نعم — stdio في `mcp.servers.comfyui` | tools/list = ٤١؛ `list_packs` → **٥٦** حزمة محلية مجانية؛ لا ComfyUI على 8188/8000 | **مثبت ومختبر كخادم**؛ التوليد يحتاج خلفية ComfyUI ونماذج |
| **OpenStitch** | `github.com/heldernoid/openstitch` (MIT) | **لا** | **ليس MCP** — تطبيق Docker Compose + Ollama | README يلزم Docker + Ollama؛ `docker` غير موجود وOllama لا يعمل | **غير قابل للتثبيت هنا**؛ لا يُوصل كـMCP |
| **Impeccable** | `github.com/pbakaus/impeccable` (Apache-2.0) | **نعم** — مهارة + أمر + محرّك v4.9.1 | نعم — OpenCode مدعوم صراحةً | كشف ٥ أنماط AI في `/tmp/impeccable_slop.html` | **مثبت ومختبر** بلا API key |
| **Pencil CLI** (pen.dev) | `docs.pencil.dev/getting-started/ai-integration` | **نعم** — `pen 0.3.16` | **لا خادم MCP مستقل** | `pen status` → Not authenticated؛ `pen interactive` → Authentication required | **CLI فقط**؛ لا يُضاف كـMCP بلا تطبيق pen.dev |
| **UI UX Pro Max** | `github.com/nextlevelbuilder/ui-ux-pro-max-skill` (MIT) | **نعم** — `ui-ux-pro-max-cli` v2.15.0 و`uipro` في `/usr/local/bin` | نعم — `uipro init --ai opencode` وضع مهاراته في `.opencode/skills/` | `search.py` باستعلام Compose أعاد نظامًا مرجعيًّا؛ `--domain ux` أعاد ٠ نتائج بصدق؛ لا `--rtl` في v2.15.0 | **مثبت ومختبر** — مرجع لا يُنسخ |

> **تنبيه إلزامي:** `github.com/open-pencil/open-pencil` **ليس** تنفيذًا لـPencil MCP. الأول محرّر
> تصميم مفتوح المصدر (بديل Figma)، والثاني منتج pen.dev التجاري. ذُكر الرابطان في أمر المالك معًا،
> فلذلك يُثبَّتان منفصلين ويُسمّى كلٌّ باسمه؛ ولا يُقال إن أحدهما «Pencil MCP» للآخر.

### 0.1 سجل القياس الحالي (يُعاد بعد كل تغيير)

```text
$ opencode mcp list
○ adb       pending → ثم ✓ connected بعد تحميل الأدوات
✓ context7  connected
⚠ figma     needs authentication
✓ gh_grep   connected

$ ~/.local/bin/adb-mcp --version
adb-mcp 0.25.0

$ # tools/list عبر stdio
TOOLS: 81 [...]

$ # tools/call doctor
✗ Android SDK: /root/Android/Sdk has no platform-tools/ — not an SDK install
✓ adb: Android Debug Bridge version 1.0.41
✗ emulator: not runnable
• devices: none attached
✓ java: openjdk 25.0.4
✓ gradle (system): Gradle 4.4.1
```

**اختبار النموذج الفعلي:** جلسة `opencode run --auto --format json` جديدة على
`opencode/deepseek-v4.1-flash-free` استدعت `execute → search(namespace=adb) → tools.adb.doctor()`
وأعادت مخرج `doctor` نصًّا. هذا يثبت أن DeepSeek V4.1 Flash يستدعي أدوات MCP عبر OpenCode، لا أن
الخادم «متوافق» نظريًّا. أثر الجلسة: `ses_e25bb7477ffexnJHo6UXwwFB3D`.

**Figma OAuth (بلا بيانات دخول):** `opencode mcp auth figma` طبع رابط
`https://www.figma.com/oauth/mcp?...` ثم انتظر، وأُلغي بعد ٢٥ ثانية. لا tool call قبل إتمام المتصفح.

## 0.2 قيود تثبيت مقيسة (ليست عيب توافق مع OpenCode أو DeepSeek)

- **Pencil MCP:** لا يوجد خادم MCP مستقلّ في `@pen.dev/cli@0.3.16`؛ الحزمة تعرّف `pen` و`pencil`
  فقط. الـMCP الخارجي يُفعَّل من تطبيق pen.dev المكتبي/إضافة IDE ويستمع على socket محلي. تشغيل
  الـCLI نفسه للأوامر التوليدية يحتاج حسابًا (`pen login` أو `PEN_CLI_KEY`). **لم يُختبر tool call
  هنا** لأن المضيف غير موجود والحساب غير مُدخل، ولا يُدّعى غير ذلك.
- **OpenPencil (مشروع مختلف):** `@open-pencil/mcp@0.16.0` يعرّف `openpencil-mcp` و`openpencil-mcp-http`؛
  أدواته تقرأ/تعدّل تصميمًا عبر تطبيق OpenPencil العامل. ليس بديلًا صامتًا لـpen.dev Pencil MCP.
- **npm على هذا الجهاز:** التثبيتات العالمية الكبيرة قد تفشل بـ`ENOENT rename …/_cacache/tmp` رغم
  وجود مساحة حرة (مُقاس مع `@pen.dev/cli` و`@open-pencil/mcp`) بينما نجح `ui-ux-pro-max-cli`.
  المسار الذي يعمل: cache تحت `/root`، `maxsockets=1`، وتثبيت **متسلسل** لا متوازٍ.
- **UI UX Pro Max v2.15.0:** `--dry-run` غير موجود (`unknown option`)، ولا خيار `--rtl`؛
  البحث يستخدم `--stack jetpack-compose` ويُقرأ كمَرجع لا كنسخ هوية (AGENTS §0.4).
- **ADB:** الخادم مثبّت ويعمل (v0.25.0، ٨١ أداة، `doctor` ناجح)، لكن لا AVD ولا جهاز موصول،
  لذلك `screenshot`/`describe_ui` غير قابلين للاختبار هنا. أدوات Gradle في الخادم **لا تُستخدم**
  (AGENTS §0.10).

## 1. أوامر التثبيت (المصادر الرسمية فقط)

```sh
# Figma — remote MCP رسمي، بلا تطبيق مكتبي
opencode mcp add --global figma --url https://mcp.figma.com/mcp
opencode mcp auth figma          # يفتح/يطبع رابط OAuth؛ لا تُكتب أي بيانات دخول في أي ملف

# Android ADB MCP — من المصدر الرسمي (Go)
GOBIN="$HOME/.local/bin" go install github.com/iksnerd/adb-mcp/cmd/adb-mcp@latest
# أو سكربت التثبيت الرسمي بعد قراءته:
# curl -fsSL https://raw.githubusercontent.com/iksnerd/adb-mcp/main/install.sh | sh

# Pencil (pen.dev) — CLI الرسمي؛ الـMCP نفسه يُفعَّل من داخل التطبيق المكتبي
npm install -g @pen.dev/cli
pen version && pen status

# OpenPencil — حزمة MCP الرسمية (منفصلة عن pen.dev)
npm install -g @open-pencil/mcp

# UI UX Pro Max — CLI الرسمي ثم تركيب skill داخل المشروع
npm install -g ui-ux-pro-max-cli
uipro init --ai opencode          # v2.15.0 لا يعرف --dry-run (مُقاس: unknown option)
```

**حدود منصّية مقيسة:** Node 22.22.1، npm 9.2.0، Go 1.27.3 (android/arm64)، `adb` 1.0.41 موجود،
`ANDROID_HOME` غير مضبوط، `/root/android-sdk` غير موجود، ولا جهاز ADB موصول. **لا بناء Gradle محليًّا**
(AGENTS §0.10)؛ أدوات ADB MCP الخاصة بـGradle لا تُستخدم للبناء في هذا المستودع، بل للقطات وفحص الواجهة فقط.

## 2. دورة التصميم الستّ في MaxManager

### ① تحليل المراجع
- لقطات المالك تُقرأ بـ`python3 tools/read_image_text.py <path|url>` عند الحاجة إلى النص، وتُقاس
  الحدود والألوان بأدوات الصور لا بالتخمين.
- `python3 .opencode/skills/ui-ux-pro-max/scripts/search.py "<query>" --design-system -p "MaxManager …"`
  لاستخراج نمط/لوحة/خطوط، مع `--stack jetpack-compose`. **v2.15.0 لا يدعم `--rtl`** (مُقاس)؛
  للعربية تُستخدم بياناتنا في `ui/design/` لا خيار غير موجود.
- Figma MCP يُستخدم فقط عندما يكون المرجع **رابط Figma** وتمّت المصادقة؛ لا يقرأ لقطة محلية كملف Figma.
- **المخرج:** قائمة فروق مرقّمة (D-xx) بمسارات `file:line`، لا انطباعات.

### ② إنشاء التصميم
- Pencil/OpenPencil/Figma لإنشاء أو تعديل تصميم قابل للتفتيش **إن كان المضيف/المصادقة متاحين**.
- وإلا فالتصميم يُكتب بعقد Compose/design tokens في المستودع (`ui/design/`)، وتُعلن حدود الأداة.
- لا يُنشأ مصدر حقيقة ثانٍ للحالة نفسها، ولا تُنسخ هوية بصرية من مرجع خارجي (AGENTS §0.4).

### ③ التنفيذ
- Compose/Kotlin داخل `manager/app/src/main/java/nd/max/**`، والنصوص في `values/` و`values-ar/` فقط.
- كل كتابة عتاد عبر الـarbiter؛ لا تليمتري مصنَّع (ADR-07/ADR-11).
- البوابات الخفيفة قبل أي دفع: `kt_balance` · `code_health` · `i18n_coverage` (+`--prune all`) ·
  `design_tokens` · `resource_compile` · `provenance_guard` · `git diff --check`.

### ④ التقاط الصور
- **بجهاز موصول:** ADB MCP `screenshot`/`describe_ui` (أو `adb exec-out screencap -p`)؛ يُذكر المقاس
  والكثافة واتجاه الشاشة.
- **بلا جهاز:** تُستخدم لقطة المالك كما هي، ويُكتب «لم تُلتقط هنا» بلا ادّعاء.
- لا تُستخدم أدوات ADB MCP لبناء Gradle؛ البناء عبر GitHub Actions فقط (§0.10).

### ⑤ المقارنة
- مقارنة بصرية بين اللقطة المرجعية ولقطة التنفيذ: المواضع، الأحجام، التباين، RTL، حالات الفراغ.
- `diff_visual` من OpenPencil يُستخدم فقط إذا كان تطبيق OpenPencil عاملًا ومفتوحًا على المستند.
- **المخرج:** قائمة فروق مصنّفة P0/P1/P2 مع دليل بصري/قياس، لا «يبدو مطابقًا».

### ⑥ إصلاح الفروق البصرية
- إصلاح P0 أولًا، ثم P1، مع إعادة البوابات ولقطة مقارنة جديدة.
- كل إصلاح يُسجَّل في `HANDOFF` عند نهاية المهمة فقط (§0.3)، ويُربط بدليل.
- ما لا يُثبته اختبار بصري يبقى «غير مُتحقَّق» حتى جهاز/لقطة جديدة.

## 3. التكامل مع OpenCode وDeepSeek V4.1 Flash

- **MCP عميل-side:** OpenCode هو العميل؛ النموذج يرى أدوات MCP كأدوات عادية (Code Mode يجمعها
  under `tools.<server>.<tool>`). لا تحتاج الأداة أن «تدعم DeepSeek»؛ تحتاج أن يعمل استدعاء الأدوات
  في OpenCode، وهذا مُثبت في هذه الجلسة.
- **الاختبار الحاسم:** جلسة `opencode run --auto` جديدة بعد إضافة الخادم، بطلب استدعاء أداة واحدة
  وإرجاع مخرجها. نجاح `mcp list` وحده لا يكفي.
- **Skills** ملفات تعليمات لا أدوات؛ متوافقة مع أي نموذج. `ui-ux-pro-max` يُستدعى عند عمل UI/UX،
  و`maxmanager-design-cycle` (أدناه) يربط الدورة كلها.
- **Auth blockers:** Figma OAuth (متصفح) · Pencil حساب pen.dev أو `PEN_CLI_KEY` · OpenPencil تطبيق يعمل ·
  ADB جهاز/محاكي. تُذكر هذه الحالات في التسليم، ولا تُخفى.

## 4. أين تعيش الإعدادات

- **الـMCP:** في `~/.config/opencode/opencode.json` (global) لأن الأدوات تخدم أكثر من مشروع؛
  النسخة الاحتياطية: `opencode.json.bak-2026-11-14`. لا أسرار في الملف — Figma OAuth يخزّن
  اعتماده في مخزن OpenCode لا في المستودع.
- **الـskills/الأوامر:** داخل المشروع في `.opencode/` كي تنتقل مع المستودع وتُراجع معه.
- **التوثيق:** هذا الملف هو المرجع؛ يُحدَّث بعد كل اختبار فعلي.

## 5. مراجع رسمية

- OpenCode MCP: <https://opencode.ai/docs/mcp-servers/>
- Pencil (pen.dev): <https://docs.pencil.dev/getting-started/ai-integration>
- OpenPencil: <https://github.com/open-pencil/open-pencil>
- Figma MCP: <https://developers.figma.com/docs/figma-mcp-server/> · <https://github.com/figma/mcp-server-guide>
- Android ADB MCP: <https://github.com/iksnerd/adb-mcp>
- UI UX Pro Max: <https://github.com/nextlevelbuilder/ui-ux-pro-max-skill>
- Figma UI MCP: <https://github.com/TranHoaiHung/figma-ui-mcp>
- ComfyUI MCP: <https://github.com/artokun/comfyui-mcp>
- OpenStitch: <https://github.com/heldernoid/openstitch>
- Impeccable: <https://github.com/pbakaus/impeccable>

## 6. مصفوفة الأدوات الست — قياس ٢٠٢٦-١١-١٤

| # | الأداة | المُثبَّت | الاختبار الفعلي | الحاجز/الحدّ |
| --- | --- | --- | --- | --- |
| 1 | **Figma UI MCP** | `figma-ui-mcp@2.5.30` في `/root/figma`، أُضيف global باسم `figma-ui` | stdio INIT + tools/list = **٥**؛ `figma_status` → bridge 38451، `pluginConnected:false`، sessions ٠ | **Figma Desktop + Plugin Bridge**؛ لا API key ولا OAuth. لا كتابة/قراءة بلا Plugin |
| 2 | **OpenPencil MCP** | `@open-pencil/mcp@0.16.0` في `/root/openpencil`، global باسم `openpencil` | tools/list = **١٣٢**؛ `list_pages` → `OpenPencil app is not connected` | **تطبيق OpenPencil عامل ومستند مفتوح**؛ مختلف عن pen.dev |
| 3 | **ComfyUI MCP** | `comfyui-mcp@0.52.206` في `/root/comfyui`، global باسم `comfyui` | tools/list = **٤١**؛ `list_packs` → **٥٦** حزمة محلية مجانية | **خلفية ComfyUI** على 8188/8000 + نماذج؛ `sharp` اختياري غير مثبت بسبب `--omit=optional` |
| 4 | **OpenStitch** | **لم يُثبَّت** | README الرسمي: Docker Compose + Ollama ثم `localhost:3000` | **ليس MCP**؛ `docker` غير موجود، Ollama لا يعمل — لا يُوصل كخادم |
| 5 | **Impeccable** | مهارة + أمر + محرّك v4.9.1 linux-arm64 في `.opencode/skills/impeccable` | كشف ٥ أنماط AI في صفحة اختبار (`gray-on-color`, `contrast 1.1:1`, `tiny-text 9px`, `Inter`, `purple/violet`) | لا حاجز؛ يعمل بلا API key |
| 6 | **ADB MCP** | `adb-mcp@0.25.0` في `~/.local/bin`، global باسم `adb` | tools/list = **٨١**؛ `doctor` عبر OpenCode+DeepSeek | **جهاز/محاكي موصول**؛ لا AVD هنا. لا تُستخدم أدوات Gradle للبناء (§0.10) |

**قبل الأدوات الست (سابقة الجولة):** Figma remote `mcp.figma.com` يحتاج OAuth؛ Pencil CLI
`@pen.dev/cli@0.3.16` لا يحتوي خادم MCP ويحتاج `pen login`/`PEN_CLI_KEY`؛ UI UX Pro Max
`2.15.0` مثبت؛ ADB سابق الذكر.

**ما لا يُدّعى:**
- لا `figma_write`/`figma_read` بلا Figma Desktop وPlugin Bridge — المُقاس `pluginConnected:false`.
- لا مستند OpenPencil — المُقاس `list_pages` error.
- لا توليد ComfyUI — لا خلفية ولا نماذج؛ `list_packs` وحده ليس توليدًا.
- لا OpenStitch — لا Docker/Ollama.
- لا تصميم Pencil — `pen interactive` يرفض بلا مصادقة.
- لا لقطة شاشة من ADB — لا جهاز؛ تُستخدم لقطة المالك وتُعلن حدودها.

**دورة التنفيذ الفعلية الآن:** ما يعمل بلا مصادقة/جهاز هو **Impeccable + UI UX Pro Max + قواعد التصميم
داخل Compose/design tokens**. أمّا Figma/OpenPencil/ComfyUI/ADB فهي جاهزة كخوادم لكنها تنتظر
المضيف/الجهاز/الخلفية، ويُقال ذلك في كل تسليم. الخطوة التالية بعد التوثيق: إعادة بناء GameSpace من
المرجع السينمائي `3d1c0d86-1af8-4de0-a35c-9c92fcec280d.png` (1680×944) بصريًّا أولًا، ثم Compose،
ثم مقارنة بلقطة حقيقية — لا ادّعاء تطابق بلا لقطة.
