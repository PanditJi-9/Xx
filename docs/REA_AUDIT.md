# REA_AUDIT — حالة الأبحاث والمصادر كما هي فعلًا (المرحلة 0)

> مكتوب بعد بحث فعلي على القرص وقياس، لا بعد افتراض. كل ما هنا يمكن إعادة التحقق منه بالأوامر المذكورة.

## 1. ما وُجد وما لم يوجد

| ما بحثنا عنه | النتيجة | الدليل |
| --- | --- | --- |
| نسخة محلية من REA **لمنظومة RedMagic** (فك/تقارير/patches للسوق) | **غير موجودة** | `find /storage/emulated/0 -maxdepth 4 -iname '*rea*'` ⇒ لا مجلد بحث لمنظومة الألعاب |
| مجلد اسمه `REA-Decompiled` | موجود لكنه **MaxManager/Bubble Translate**، لا صلة له بالمنظومة | `ls /storage/emulated/0/Download/REA-Decompiled` ⇒ zips: `MaxManager-V4-506-rea.zip`, `MaxManager-V5.0-550-rea.zip`, `Bubble-Translate-4.5.7-rea.zip` |
| مصدر APK للمنظومة | **`apps/` داخل المستودع** (24 APK، 1.8 GB) | `docs/audit/apk-inventory.json` |
| بصمات الملفات الأصلية | مأخوذة قبل أي تعديل | `docs/audit/apk-sha256.txt` (24 سطرًا) |

**الخلاصة:** «REA» في توجيه المالك ليس مجلدًا جاهزًا على هذا الجهاز، بل **الأداة**
([morluto/rea](https://github.com/morluto/rea)) التي تُشغَّل على التطبيقات. لذلك المرحلة 0 هنا = تثبيت
خط الأساس (بصمات + جرد) وفتح قناة REA، لا «حفظ مجلد أبحاث» غير موجود.

## 2. حالة الأصول (Preservation)

- **لم يُعدَّل أي APK**: البصمات في `docs/audit/apk-sha256.txt` هي خط الأساس؛ أي تغيير مستقبلي يُكشف بمقارنتها.
- **`NubiaRedmagickyi_RedMagic.apk` مستثنى بأمر المالك**: لم يُفكّ، ولم يُحلَّل، ولم يدخل أي تحليل
  (أدوات `apk_xref.py` و`apk_ipc.py` تستثنيه صراحةً؛ و`apk_inventory.py` بـ`--exclude`).
- الأشجار المفكوكة التجريبية السابقة (`/tmp/ga_sm`, `/tmp/gs_sm`, …) **ليست أصلًا** ولا تُستخدم كمرجع دائم.

## 3. REA — ما هي، وما قياس تشغيلها هنا

REA = خادم MCP لأدوات الهندسة العكسية (ثنائيات، تطبيقات، سلوك وقت التشغيل)، تعمل تحليلاته محليًا وتُعيد
النتيجة **مع دليلها وحدودها**. تثبيتها: `npx rea-agents setup`، وتحليل Android يستعمل مزوّد JADX.

**قياس التشغيل على هذا الجهاز (فشل موثّق):**

```text
$ node -v                       → v22.22.1
$ npx -y rea-agents@latest --help
npm WARN EBADENGINE package: 'rea-agents@6.9.0'
npm WARN EBADENGINE   required: { node: '^22.23.4 || ^24.22.1 || >=26.12.1' }
npm WARN EBADENGINE   current:  { node: 'v22.22.1', npm: '9.2.0' }
[npm ERR! process terminated — SIGTERM بعد 180 ثانية تثبيت native addons]
```

⇒ **REA لا تعمل على Node المثبَّت هنا.** المسار المعتمد: تشغيلها في CI بـNode 24
(`.github/workflows/rea-analysis.yml`)، والنسخة المحلية تُحدَّث فقط عند الحاجة الفعلية.

## 4. ما استُغني عنه ولماذا

| الأداة | القرار | السبب |
| --- | --- | --- |
| `apktool` لفك 23 APK | **لا** للجرد | فك 1.8 GB إلى smali = ساعات، بينما الـmanifest وDEX يحملان الجواب في ثوانٍ (`apk_inventory.py` < 1ث، `apk_xref.py` ~30ث) |
| `aapt2` | **لا** | يسقط بـSIGILL على هذا الجهاز (مقيس في جولات سابقة) |
| REA | **نعم، لاحقًا** | لأسلوب «كيف يعمل هذا الزر» في المرحلة 2، لا للجرد الحتمي |

## 5. خط الأساس الذي تُقارن به كل مرحلة

| المرجع | الملف |
| --- | --- |
| جرد 23 تطبيقًا | `docs/audit/APK_INVENTORY.md` |
| 519 مكوّنًا معلنًا | `docs/audit/COMPONENT_CATALOG.md` |
| خريطة الترابط | `docs/audit/APP_DEPENDENCY_GRAPH.md` |
| عقد IPC والصلاحيات | `docs/IPC_AND_INTEGRATION.md` |
| بصمات الأصول | `docs/audit/apk-sha256.txt` |
| الخطة الحاكمة | `docs/MASTER_PLAN.md` |
