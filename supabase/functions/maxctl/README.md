# `maxctl` — دالّة التحكّم: التوقيع + الترخيص والمفاتيح

دالّة واحدة تحمل كلّ ما يحتاج المفتاحَ الخاصَّ: **نشر الإعداد الموقّع**، **إنشاء المفاتيح**،
**تفعيلها**، **تجديد شهادات الأجهزة**، والإلغاء والتمديد. والمفتاح الخاصّ **لا يدخل المستودع
ولا لوحة التحكّم ولا التطبيق** — يبقى سرًّا في Supabase ولا يخرج من هذه الدالّة.

---

## ١) توليد زوج المفاتيح (على جهازك، مرة واحدة في العمر)

```sh
# المفتاح الخاصّ (لا تشاركه ولا ترفعه ولا تلصقه في محادثة — ضعه في Supabase فقط)
openssl ecparam -genkey -name prime256v1 -noout -out mm-license-key.pem

# القيمة التي تُلصق في سرّ Supabase (PKCS#8 DER بصيغة base64)
openssl pkcs8 -topk8 -nocrypt -in mm-license-key.pem -outform DER | base64 -w0 > mm-license-key.pkcs8.b64

# المفتاح العامّ (هذا يُنشر، ويُبنى به التطبيق — لصقه في GitHub Secrets باسم CONFIG_PUBLIC_KEY)
openssl ec -in mm-license-key.pem -pubout -outform DER | base64 -w0 > mm-license-public.b64
```

- **احفظ `mm-license-key.pem` في مكان آمن** (مدير كلمات مرور). فقدانه = لا يمكن إصدار تفعيلات
  جديدة بالنسخ المنشورة؛ وأخذك نسخةً احتياطية الآن أرخص ألف مرة من استعادته لاحقًا.
- وشغّل `python3 tools/license_tool.py check --public-file mm-license-public.b64` للتأكد من صحة
  المفتاح العامّ قبل بنائه في التطبيق (الأداة تشرح أي خطأ بالأرقام).

## ٢) تنفيذ مخطّط قاعدة البيانات

الصق **كامل** `supabase/schema.sql` في `SQL Editor` مرة واحدة (القسم الجديد في آخره آمن
وإضافيّ: أعمدة وجداول جديدة فقط، بلا فقد بيانات). راجع رأس القسم في الملفّ نفسه.

## ٣) نشر الدالّة وضبط السرّ

```sh
supabase functions deploy maxctl --no-verify-jwt
supabase secrets set LICENSE_SIGNING_KEY_PKCS8="$(cat mm-license-key.pkcs8.b64)"
```

- `--no-verify-jwt` **مقصودة**: بوابة الحماية الحقيقية داخل الدالّة نفسها — تفعيل المفاتيح عامّ
  بطبعه (الزبون ليس مسجَّلًا بعد)، وكل عمليات المالك تتحقق من هويته ومن كونه في جدول `admins`
  قبل أن تنفّذ أي شيء. (وإلا لما استطاع زبون بلا حساب أن يفعّل مفتاحًا.)
- استبدال المفتاح لاحقًا ممكن (سرّ جديد) لكنه يُبطل الشهادات الصادرة بالقديم **بعد** انتهاء
  صلاحيتها — فوُلِّد المفتاح مرة والباقي احتياطٌ لا حاجة.

## ٤) الإعداد الذي يُبنى به التطبيق (GitHub → Settings → Secrets → Actions)

| السرّ | من أين | أين يُطبَّق |
| --- | --- | --- |
| `SUPABASE_URL` | لوحة Supabase → Project Settings → API | لوحة التحكّم + MaxManager |
| `SUPABASE_ANON_KEY` | الحقل `anon` / `publishable` نفسه | لوحة التحكّم + MaxManager |
| `CONFIG_PUBLIC_KEY` | `mm-license-public.b64` أعلاه | MaxManager (التحقق بلا اتصال) |

وارفع `mm-license-key.pem` وملفَّي `.b64` **خارج** المستودع (`..` أو خزنة كلمات مرور).

## ٥) التحقق من العمل (بعد النشر)

```sh
# (أ) الدالّة حيّة: نشر بلا جلسة مالِك يجب أن يُردّ 401
curl -s -X POST "$SUPABASE_URL/functions/v1/maxctl" \
  -H "content-type: application/json" \
  -d '{"action":"license.create","count":1,"days":30}'
# المتوقَّع: {"ok":false,"error":"unauthorized"}

# (ب) بعد تسجيل الدخول من اللوحة: أنشئ مفتاحًا واحدًا، ثم اختبر التفعيل:
HASH=$(printf 'MAXM-XXXXX-...' | tr -d ' -' | sha256sum | cut -d' ' -f1)   # المفتاح القانوني بلا فواصل
curl -s -X POST "$SUPABASE_URL/functions/v1/maxctl" -H "content-type: application/json" \
  -d "{\"action\":\"license.redeem\",\"key_hash\":\"$HASH\",\"device\":\"0123456789abcdef0123456789abcdef\"}"
# المتوقَّع: ok:true مع cert.payload وcert.signature
```

## ٦) سقف المعدّل (مُنفَّذ — كان بندًا «غير مُنفَّذ» في المواصفة §١٤)

على الوجهتين المفتوحتين بلا تسجيل دخول (`license.redeem` · `license.validate`)، **لكل بصمة عميل**:

| السقف | الحدّ | ما يعنيه |
| --- | --- | --- |
| فشل الدقيقة | ٨ | النداء التاسع في النافذة يُرفض بـ`rate_limited` |
| فشل الساعة | ٤٠ | سقف ممتدّ يمنع التخمين البطيء |
| كل المحاولات/دقيقة | ٣٠ | سقف «عنف» يشمل الناجحة — فنداءٌ بمعدّل ٣٠/دقيقة ليس سلوك تطبيق |

- البصمة = `SHA-256(عنوان العميل + سرّ)` مبتورًا — **لا يُخزَّن عنوان خامّ في أي صفّ** (سياسة
  «لا معرّفات» نفسها). والسرّ `RATE_SALT` إن ضبطته، وإلا مفتاح الخدمة:
  `supabase secrets set RATE_SALT="$(openssl rand -hex 32)"` (اختياري — تغييره يصفّر العدّ فقط).
- الرفض يحمل `retry_after` (ثوانٍ) و`reason` (`failures_minute` · `failures_hour` · `attempts_minute`)،
  والتطبيق يعرف رمز `rate_limited` ونصّه أصلًا. والقرار دالّة حتميّة **مقيسة**:
  `python3 tools/test_maxctl_pure.py --assert` (٢٨ حالة، وتُقاس بالنصّ نفسه لا بنسخة منه).
- والسجلّ `license_redeem_attempts` قصير العمر بطبيعته: حذفٌ نادر (١٪ من النداءات) لما تجاوز ٧ أيام،
  فلا يحتاج مهمّة صيانة.

## ٧) ما لا تفعله هذه الدالّة (يُعلن)

- لا تُصلح «جهازًا مروّتًا يعدّل التطبيق نفسه» — إصلاح ذلك دور التطبيق والشهادة الموقّعة،
  والبيان الصادق في `docs/ai/LICENSE-KEYS-01-spec.md` §5.
- لا تُلغي شهادةً صادرة قبل نهايتها: الإلغاء يُطبَّق عند **أول تجديد** لشهادة ذلك الجهاز.
- لا تكتب في `licenses`/`license_devices` من أي عميل مباشرة (المخطّط يسحب كل الصلاحيات —
  `<revoke all ... from anon, authenticated>` — والوصول كله من هذه الدالّة بمفتاح الخدمة).
