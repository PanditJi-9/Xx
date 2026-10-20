-- Max Manager control center: run this whole file once in Supabase → SQL Editor.
--
-- Admin setup (after running this file):
-- 1) Authentication → Users → Add user: your email + password, tick "Auto Confirm User".
-- 2) Run:  insert into public.admins (user_id) select id from auth.users where email = 'YOUR_EMAIL';

create table if not exists public.admins (
  user_id uuid primary key references auth.users (id) on delete cascade
);

create table if not exists public.app_config (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz not null default now()
);

-- One row per install: a random id, a 2-letter country code and the app version.
-- No name, email or phone number is stored.
create table if not exists public.installs (
  install_id uuid primary key,
  country text not null default 'XX',
  app_version text not null default '',
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now()
);
create index if not exists installs_last_seen_idx on public.installs (last_seen);

-- ── توسعة التوافق (§١٣ بند ٣ و٤ من `MAX-CONTROL-01`): أعمدة تُقاس ويُقيَّد كلٌّ منها ────────────
-- تُضاف بـ`if not exists` فلا تُمسّ صفوف قائمة (القيم الافتراضية `null`)، وفيها ما يُحلّل التوافق
-- بلا معرّف واحد: الشركة والطراز كما يعلنهما النظام، إصدار أندرويد ومستواه، ودرجة التحكّم.
alter table public.installs add column if not exists manufacturer text;
alter table public.installs add column if not exists model text;
alter table public.installs add column if not exists android_sdk int;
alter table public.installs add column if not exists android_release text;
alter table public.installs add column if not exists control_env text;

-- عدّادات صحّة التحكّم (§١٠.١): كائن JSON بأعداد مصنَّفة لكل مجال — لا قيم ولا قراءات.
-- `if not exists` لأن المشروع القائم قد يكون نفّذ القسم السابق؛ والصفوف القديمة تبقى `null`
-- (لا صفر مُختلق: «لا نعرف» ليست «صفر حوادث»).
alter table public.installs add column if not exists health jsonb;

alter table public.installs drop constraint if exists installs_country_shape;
alter table public.installs add constraint installs_country_shape
  check (country ~ '^[A-Z]{2}$');

alter table public.installs drop constraint if exists installs_control_env_values;
alter table public.installs add constraint installs_control_env_values
  check (control_env is null or control_env in ('none', 'shizuku', 'magisk', 'kernelsu', 'apatch', 'root_other'));

alter table public.installs drop constraint if exists installs_sdk_range;
alter table public.installs add constraint installs_sdk_range
  check (android_sdk is null or android_sdk between 1 and 100);

alter table public.installs drop constraint if exists installs_text_lengths;
alter table public.installs add constraint installs_text_lengths
  check (
    (manufacturer is null or char_length(manufacturer) <= 40)
    and (model is null or char_length(model) <= 40)
    and (android_release is null or char_length(android_release) <= 20)
  );

-- شكل العدّادات: كائن JSON بسقف ٤ ك.ب — والقيد يحرس **الشكل والحجم** لا القيم (القيم تُنقّى
-- في الدالّة أدناه: مجالات معلومة، ثلاث إحصاءات لكل مجال، أعداد صحيحة من ٩ خانات).
alter table public.installs drop constraint if exists installs_health_shape;
alter table public.installs add constraint installs_health_shape
  check (
    health is null
    or (jsonb_typeof(health) = 'object' and octet_length(health::text) <= 4096)
  );

alter table public.admins enable row level security;
alter table public.installs enable row level security;
alter table public.app_config enable row level security;

drop policy if exists "config is public read" on public.app_config;
create policy "config is public read" on public.app_config for select using (true);

revoke all on public.admins, public.installs from anon, authenticated;
revoke insert, update, delete on public.app_config from anon, authenticated;

create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid());
$$;

-- Called by the Max Manager app with the anon key — one signal a day at most.
--
--   ★ الحارس اليوميّ على الخادم لا على العميل (§٤.٢ بند ٧): إشارة ثانية في اليوم نفسه لا
--     تكتب شيئًا (تُقرأ `last_seen` أولًا وتُعاد بلا فعل) — فحتى نسخة معدَّلة لا تُغرق الجدول.
--   ★ والمعرّف لا يقرأ صفوفًا: الدالّة `security definer` والحقلان العامّان هما مَن يمرّان.
--   ★ والتوقيع الثمانيّ (قبل عدّادات الصحّة) يُسقط كذلك قبل إنشاء التاسع — القاعدة نفسها:
--     توقيع واحد حَيّ لكل دالّة، والقديم الميت لا يبقى ليُنادى بنصف معنى.
drop function if exists public.track_install(uuid, text, text);
drop function if exists public.track_install(uuid, text, text, text, text, int, text, text);
create or replace function public.track_install(
  p_install uuid,
  p_country text,
  p_version text,
  p_manufacturer text default null,
  p_model text default null,
  p_sdk int default null,
  p_release text default null,
  p_env text default null,
  p_health jsonb default null
)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_env text;
  v_health jsonb;
begin
  if exists (
    select 1 from public.installs i
    where i.install_id = p_install
      and (i.last_seen at time zone 'utc')::date = (now() at time zone 'utc')::date
  ) then
    return;
  end if;

  v_env := case
    when p_env in ('none', 'shizuku', 'magisk', 'kernelsu', 'apatch', 'root_other') then p_env
    else null
  end;

  -- تنقية عدّادات الصحّة (§١٠.١): تُقبل المجالات المعلومة وحدها، وكلٌّ منها ثلاثة أعداد صحيحة
  -- غير سالبة. وما خالف ذلك يُسقَط كاملًا بدل أن يُخزَّن نصف صحيح؛ وإن لم يبقَ شيء فالقيمة null
  -- (لا `{}`: «لا أعداد» ليست «صفر حوادث»، والقارئ يفرّق بينهما).
  if p_health is not null
     and jsonb_typeof(p_health) = 'object'
     and octet_length(p_health::text) <= 4096 then
    select jsonb_object_agg(kept.domain, kept.counters)
    into v_health
    from (
      select entry.key as domain, entry.value as counters
      from jsonb_each(p_health) as entry
      where entry.key in (
              'audio', 'charging', 'cpu', 'display', 'gpu', 'memory',
              'other', 'spoof', 'storage', 'thermal', 'touch'
            )
        and jsonb_typeof(entry.value) = 'object'
        and entry.value ?& array['confirmed', 'rolled_back', 'unknown']
        and not exists (
          select 1
          from jsonb_each(entry.value) as counter
          where counter.key not in ('confirmed', 'rolled_back', 'unknown')
             or jsonb_typeof(counter.value) <> 'number'
             or counter.value::text !~ '^[0-9]{1,9}$'
        )
    ) as kept;
  end if;
  if v_health is not null and v_health = '{}'::jsonb then
    v_health := null;
  end if;

  insert into public.installs (
    install_id, country, app_version, manufacturer, model,
    android_sdk, android_release, control_env, health, last_seen
  )
  values (
    p_install,
    case when coalesce(p_country, '') ~ '^[A-Za-z]{2}$' then upper(p_country) else 'XX' end,
    left(coalesce(p_version, ''), 20),
    left(nullif(coalesce(p_manufacturer, ''), ''), 40),
    left(nullif(coalesce(p_model, ''), ''), 40),
    case when coalesce(p_sdk, 0) between 1 and 100 then p_sdk else null end,
    left(nullif(coalesce(p_release, ''), ''), 20),
    v_env,
    v_health,
    now()
  )
  on conflict (install_id) do update
    set country = excluded.country,
        app_version = excluded.app_version,
        manufacturer = excluded.manufacturer,
        model = excluded.model,
        android_sdk = excluded.android_sdk,
        android_release = excluded.android_release,
        control_env = excluded.control_env,
        -- إشارة بلا عدّادات لا تمحو ما عُرف: `coalesce` تحفظ آخر كائن صالح.
        health = coalesce(excluded.health, installs.health),
        last_seen = now();
end;
$$;

-- السحب بضغطة واحدة (§١٠.٣ بند ٣): مفتاحُ الموافقة حين يُطفأ يُرسل طلب حذف واحدًا بهذا
-- المعرّف، فيُحذف الصفّ — فلا تبقى بيانات جهاز سحب موافقته، إن شاء قبلها أو بعدها.
create or replace function public.forget_install(p_install uuid)
returns void
language plpgsql security definer set search_path = public as $$
begin
  delete from public.installs where install_id = p_install;
end;
$$;

-- Used by the control center: installs per country (admin only).
create or replace function public.admin_country_stats()
returns table (country_code text, total_count bigint, active_count bigint)
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  return query
    select i.country,
           count(*)::bigint,
           count(*) filter (where i.last_seen > now() - interval '30 days')::bigint
    from public.installs i
    group by i.country
    order by count(*) desc;
end;
$$;

-- النشر **غير الموقّع** — مسارٌ قديم (يبقى لمن يقرأ `value`، ولا تنشر منه اللوحة).
--
-- منذ `LICENSE-KEYS-01` تنشر اللوحة عبر دالّة `maxctl` (‏`config.publish`): هي التي توقّع
-- بمفتاح خاصّ لا يدخل أي تطبيق وتكتب `payload`/`signature`/`revision`، وتكتب `value` أيضًا
-- (السطر أعلاه) فتبقى النسخ التي تقرأ العمود القديم متزامنة. وأمّا تطبيق MaxManager فلا يقرأ
-- إلا العرض الموقّع `config_signed` — فالكتابة من هنا **لا تُغيّر شيئًا عنده**، ولهذا لا
-- تستعملها اللوحة، وتبقى الدالّة للتزامن الخلفيّ وللنسخ القديمة فقط.
create or replace function public.admin_save_config(p_value jsonb)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  update public.app_config set value = p_value, updated_at = now() where key = 'main';
end;
$$;

revoke all on function public.admin_country_stats() from public, anon;
revoke all on function public.admin_save_config(jsonb) from public, anon;
grant execute on function public.admin_country_stats() to authenticated;
grant execute on function public.admin_save_config(jsonb) to authenticated;

-- ── الدالّتان العامّتان لقناة الإحصاءات: الإشارة والسحب — بلا قراءة صفوف ولا كتابة مباشرة ────
-- `public` يسقط أولًا (منح Postgres الافتراضيّ للدوالّ)، ثم يُمنح للمفتاح المنشور وحده:
-- نسخةٌ قديمة من التطبيق تنادي التوقيع الثلاثيّ كانت ستجد دالّةً سقطت — وهي القاعدة المعلنة
-- في §١٣ بند ٥ (تعديل `track_install` لا إبقاء قديمها). والتوقيع الجديد تسعة حقول كلّها افتراضية.
revoke all on function public.track_install(uuid, text, text, text, text, int, text, text, jsonb) from public;
grant execute on function public.track_install(uuid, text, text, text, text, int, text, text, jsonb)
  to anon, authenticated;
revoke all on function public.forget_install(uuid) from public;
grant execute on function public.forget_install(uuid) to anon, authenticated;

-- Initial config, same structure as config.json. Edit it from the control center afterwards.
insert into public.app_config (key, value)
values ('main', $json$
{
  "schema": 1,
  "app": "Max Manager",
  "latest": {"version": "2.1", "released": "2026-10-14", "download_page": "https://github.com/nader295/Max-Manger/releases/tag/v2.1"},
  "subscription": {"url": "https://example.com/subscribe", "payment_text_ar": "هذه الميزة تحتاج اشتراكًا. اضغط للاشتراك.", "payment_text_en": "This feature needs a subscription. Tap to subscribe."},
  "features": {
    "gamespace": {"title": "Game Space", "state": "on", "message_ar": "", "message_en": ""},
    "audio": {"title": "Audio", "state": "on", "message_ar": "", "message_en": ""},
    "atlas": {"title": "Atlas", "state": "on", "message_ar": "", "message_en": ""},
    "maxai": {"title": "Max AI", "state": "on", "message_ar": "", "message_en": ""},
    "spoof": {"title": "Device spoof", "state": "on", "message_ar": "", "message_en": ""},
    "emuhub": {"title": "Emulator hub", "state": "on", "message_ar": "", "message_en": ""},
    "bypass_charge": {"title": "Bypass charging", "state": "on", "message_ar": "", "message_en": ""}
  },
  "changelog": [
    {"version": "2.1", "date": "2026-10-14", "notes_ar": ["ملاحظة تجريبية: استبدلها بملاحظات الإصدار الحقيقية"], "notes_en": ["Sample note: replace with the real release notes"]}
  ],
  "announcement": {"enabled": false, "text_ar": "", "text_en": ""}
}
$json$::jsonb)
on conflict (key) do nothing;

-- ═══════════════════════════════════════════════════════════════════════════════════════════
-- MAX-CONTROL-02 · الترخيص والمفاتيح — قسمٌ **إضافيّ آمن** (يُنفَّذ على المشروع القائم بلا فقد)
-- ═══════════════════════════════════════════════════════════════════════════════════════════
-- كيف يُنفَّذ: الصق هذا القسم كاملًا في `SQL Editor` مرة واحدة (أو نفّذ الملفّ كاملًا على مشروع
-- جديد). كل الأعمدة `add column if not exists` بقيم افتراضية، وكل الجداول `if not exists`،
-- فلا يتغيّر صفٌّ موجود ولا يُفقد شيء.
--
-- ولماذا لا يكفي `value jsonb` وحده: التوقيع يقع على **البايتات بالضبط**، وإعادة تسلسل jsonb
-- تُغيّر ترتيب المفاتيح والمسافات فيسقط توقيعٌ سليم. لذلك يُخزَّن `payload` كنصّ base64
-- للبايتات الموقّعة نفسها، ويقرؤه التطبيق من العرض `config_signed` أدناه.
--
-- ولا `pgcrypto` هنا: المفاتيح تُولَّد وتُجزَّأ (hash) خارج SQL — في دالّة `maxctl` وحدها،
-- ولا تُخزَّن أي مفاتيح خامّ في قاعدة البيانات (تُخزَّن البصمة `key_hash` فقط).
-- التوثيق الكامل في `docs/ai/LICENSE-KEYS-01-spec.md`، ودالّة التوقيع في `supabase/functions/maxctl/`.

-- ── ١) أعمدة النشر الموقّع على صفّ الإعداد ────────────────────────────────────────────────────
alter table public.app_config add column if not exists schema int not null default 1;
alter table public.app_config add column if not exists revision bigint not null default 0;
alter table public.app_config add column if not exists signature text not null default '';
alter table public.app_config add column if not exists payload text not null default '';
alter table public.app_config add column if not exists issued_at timestamptz;
alter table public.app_config add column if not exists updated_by uuid;

-- العرض الذي يقرؤه تطبيق MaxManager: أربعة أعمدة لا غير — لا `value` ولا أسرار، وصفّ الإعداد
-- الواحد `key = 'main'` فقط. (نفس العقد الذي يطلبه `core/remote/ConfigClient.kt` حرفيًّا.)
create or replace view public.config_signed as
  select schema, revision, payload, signature, issued_at
  from public.app_config
  where key = 'main';
grant select on public.config_signed to anon, authenticated;

-- ── ٢) جدول المفاتيح ──────────────────────────────────────────────────────────────────────────
-- لا يُخزَّن المفتاح الخامّ أبدًا: `key_hash` = SHA-256 للمفتاح بصيغته القانونية (في `maxctl`)،
-- و`key_hint` بأوّل خمسة محارف وآخر محرفين يعرضهما المالك للتعرّف فقط.
create table if not exists public.licenses (
  id uuid primary key default gen_random_uuid(),
  key_hash text not null unique,
  key_hint text not null default '',
  plan text not null default '',
  note text not null default '',
  duration_days integer not null default 30 check (duration_days between 1 and 3650),
  device_limit integer not null default 1 check (device_limit between 1 and 50),
  starts_at timestamptz,               -- يُملأ عند أول تفعيل: العدّاد يبدأ من التفعيل لا من الإنشاء
  revoked boolean not null default false,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now()
);
create index if not exists licenses_created_idx on public.licenses (created_at desc);

-- ── ٣) جدول أجهزة كل مفتاح ───────────────────────────────────────────────────────────────────
-- `device` معرّف عشوائي يُنشئه التطبيق لكل تثبيت (لا معرّف عتاد — سياسة الخصوصية).
-- `active = false` تعني «أُزيل من اللوحة» فيتحرّر مقعد من `device_limit` ويبقى السجلّ للأثر.
create table if not exists public.license_devices (
  license_id uuid not null references public.licenses (id) on delete cascade,
  device text not null,
  label text not null default '',
  app_version text not null default '',
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now(),
  active boolean not null default true,
  primary key (license_id, device)
);
create index if not exists license_devices_license_idx on public.license_devices (license_id, active);

-- ── ٤) سجلّ الأحداث (تدقيق) ──────────────────────────────────────────────────────────────────
create table if not exists public.license_events (
  id bigint generated always as identity primary key,
  license_id uuid,
  kind text not null,
  detail text not null default '',
  at timestamptz not null default now()
);
create index if not exists license_events_license_idx on public.license_events (license_id, at desc);

-- ── ٥) محاولات الوجهتين العامّتين: سقف المعدّل ────────────────────────────────────────────────
-- سجلّ قصير العمر يقيس سقف المعدّل على `license.redeem` و`license.validate` — الوجهتين الوحيدتين
-- المفتوحتين بلا تسجيل دخول. **ولا يُخزَّن عنوان العميل خامًّا**: `bucket` = SHA-256 لـ(عنوان
-- العميل + سرّ) مبتورًا — أثرٌ للعدّ لا هوية، على سياسة «لا معرّفات» نفسها.
-- والقرار نفسه في `rateLimitDecision` داخل الدالّة، ويُقاس بالنصّ في `tools/test_maxctl_pure.py`.
create table if not exists public.license_redeem_attempts (
  id bigint generated always as identity primary key,
  bucket text not null,
  kind text not null default 'redeem',
  ok boolean not null default false,
  at timestamptz not null default now()
);
create index if not exists license_redeem_attempts_bucket_idx
  on public.license_redeem_attempts (bucket, at desc);

-- ── ٦) الإغلاق الكامل: لا يقرأ العميل هذه الجداول ولا يكتبها مباشرة ─────────────────────────
-- الوصول كله عبر دالّة `maxctl` بمفتاح الخدمة بعد تحقّق من هوية المالك؛ فحتى لو تسرّب مفتاح
-- anon المنشور (وهو منشور بطبيعته في كل نسخة)، لا يُقرأ صفٌّ واحد من هذه الجداول.
alter table public.licenses enable row level security;
alter table public.license_devices enable row level security;
alter table public.license_events enable row level security;
alter table public.license_redeem_attempts enable row level security;
revoke all on public.licenses from anon, authenticated;
revoke all on public.license_devices from anon, authenticated;
revoke all on public.license_events from anon, authenticated;
revoke all on public.license_redeem_attempts from anon, authenticated;

-- ── ٧) ما لا يفعله هذا القسم (يُعلن ولا يُسكت عنه) ──────────────────────────────────────────
-- • لا يمنح الجداول أي «تعبئة تلقائية»: الأعمدة الجديدة على الصفّ القائم تبقى بقيمها الافتراضية
--   (`revision = 0` · `payload = ''` · `signature = ''`) فيقرؤها التطبيق «لا إعداد موقّع»
--   ويعمل بسلوكه المحلي — حتى يضغط المالك «انشر» في لوحة التحكّم فيُوقّع أول إعداد.
-- • ولا يُنشئ دالّة التوقيع: هذه `supabase/functions/maxctl/index.ts` وتُنشر بأمر واحد
--   (`supabase functions deploy maxctl --no-verify-jwt`) وسرّ المفتاح الخاصّ يبقى في
--   `supabase secrets` ولا يدخل المستودع ولا الحزمة ولا أي سجلّ.

-- ═══════════════════════════════════════════════════════════════════════════════════════════
-- MAX-CONTROL-01 §١٣ بند ٧ · §٦.٣ و§٦.٤ — لوحة الصحّة والتوافق وسجلّ تدقيق الإعداد
-- ═══════════════════════════════════════════════════════════════════════════════════════════
-- قسمٌ **إضافيّ آمن** مثل سابقه: كل الجداول `if not exists`، وكل الدوالّ `create or replace`،
-- وكل المشغّلات تُسقط ثم تُنشأ. ويُنفَّذ على المشروع القائم بلا فقد صفّ.
--
-- لماذا هنا في آخر الملفّ: مشغّط التدقيق يقرأ `updated_by` الذي يُضاف في قسم الترخيص أعلاه؛
-- فوضعه قبل ذلك كان سيفشل على مشروعٍ جديد يُنفّذ الملفّ كاملًا أول مرة.

-- ── ١) سجلّ تدقيق تغييرات الإعداد (§١٣ بند ٢) ───────────────────────────────────────────────
-- كل نشرة (موقّعة من `maxctl` أو غير موقّعة من `admin_save_config`) تترك صفًّا: مَن، وأيّ مراجعة،
-- والقيمة قبل وبعد. وهذا ما يعرضه `admin_audit_page` في اللوحة.
create table if not exists public.config_audit (
  id bigint generated always as identity primary key,
  key text not null default 'main',
  revision bigint not null default 0,
  actor uuid,
  old_value jsonb,
  new_value jsonb,
  at timestamptz not null default now()
);
create index if not exists config_audit_at_idx on public.config_audit (at desc, id desc);

alter table public.config_audit enable row level security;
revoke all on public.config_audit from anon, authenticated;

create or replace function public.config_audit_capture()
returns trigger
language plpgsql security definer set search_path = public as $$
declare
  v_old jsonb;
begin
  -- `OLD` غير معيَّن في INSERT، فقراءته داخل فرع UPDATE وحده — لا في تعبير CASE يُقيَّم دائمًا.
  if tg_op = 'UPDATE' then
    v_old := old.value;
  end if;
  insert into public.config_audit (key, revision, actor, old_value, new_value)
  values (
    new.key,
    coalesce(new.revision, 0),
    -- `maxctl` يكتب `updated_by` بهوية المالك؛ والنشر غير الموقّع لا يمرّ بدالّة فيبقى `auth.uid()`.
    coalesce(new.updated_by, auth.uid()),
    v_old,
    new.value
  );
  return new;
end;
$$;

drop trigger if exists config_audit_capture on public.app_config;
create trigger config_audit_capture
after insert or update on public.app_config
for each row execute function public.config_audit_capture();

-- السجلّ **يُلحق فقط**: لا تعديل ولا حذف حتى من مالك الجدول (المشغّل يرمي قبل تنفيذ العملية).
create or replace function public.config_audit_guard()
returns trigger
language plpgsql as $$
begin
  raise exception 'config_audit is append-only';
end;
$$;

drop trigger if exists config_audit_guard on public.config_audit;
create trigger config_audit_guard
before update or delete on public.config_audit
for each row execute function public.config_audit_guard();

-- ── ٢) تجميع عدّادات الصحّة على الخادم (§٦.٣) ────────────────────────────────────────────────
-- **لا صفّ جهاز يخرج من هنا**: الناتج صفٌّ لكل مجال بأربعة أعداد مجموعها — لا معرّف ولا طراز.
-- والمجالات المعلومة هي نفسها قائمة المنقّي في `track_install` (تطابق مقصود ومُقاس في العقد).
create or replace function public.admin_health_stats()
returns table (domain text, confirmed bigint, rolled_back bigint, unknown_count bigint, installs bigint)
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  return query
    select d.domain,
           coalesce(sum(coalesce((i.health -> d.domain ->> 'confirmed')::bigint, 0)), 0)::bigint,
           coalesce(sum(coalesce((i.health -> d.domain ->> 'rolled_back')::bigint, 0)), 0)::bigint,
           coalesce(sum(coalesce((i.health -> d.domain ->> 'unknown')::bigint, 0)), 0)::bigint,
           (count(*) filter (where i.health ? d.domain))::bigint
    from public.installs i
    cross join unnest(array[
      'audio', 'charging', 'cpu', 'display', 'gpu', 'memory',
      'other', 'spoof', 'storage', 'thermal', 'touch'
    ]) as d(domain)
    where i.health is not null
    group by d.domain
    having count(*) filter (where i.health ? d.domain) > 0
    order by 2 desc, d.domain;
end;
$$;

-- ── ٣) توزيع التوافق (§٦.٤) ──────────────────────────────────────────────────────────────────
-- الشركة والطراز والإصدار وبيئة التحكّم مجمّعةً بعددها ونشاطها (٣٠ يومًا) — بلا معرّف تثبيت.
create or replace function public.admin_compat_stats()
returns table (
  manufacturer text,
  model text,
  android_sdk int,
  control_env text,
  total_count bigint,
  active_count bigint
)
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  return query
    select coalesce(i.manufacturer, ''),
           coalesce(i.model, ''),
           i.android_sdk,
           coalesce(i.control_env, 'none'),
           count(*)::bigint,
           (count(*) filter (where i.last_seen > now() - interval '30 days'))::bigint
    from public.installs i
    group by 1, 2, 3, 4
    order by 5 desc, 1, 2, 3
    limit 200;
end;
$$;

-- ── ٤) تقليم الصفوف الميتة (§١٣ بند ٩) ───────────────────────────────────────────────────────
-- صيانة بيد المالك لا حذفًا تلقائيًّا: تُرجع عدد ما حُذف، والمدى محصور بين يوم و١٠ سنوات.
create or replace function public.admin_prune_installs(p_days int default 90)
returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_days int;
  v_deleted bigint;
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  v_days := least(greatest(coalesce(p_days, 90), 1), 3650);
  delete from public.installs where last_seen < now() - make_interval(days => v_days);
  get diagnostics v_deleted = row_count;
  return v_deleted;
end;
$$;

-- ── ٥) صفحة التدقيق للوحة ────────────────────────────────────────────────────────────────────
create or replace function public.admin_audit_page(p_limit int default 50)
returns table (
  id bigint,
  key text,
  revision bigint,
  actor uuid,
  old_value jsonb,
  new_value jsonb,
  at timestamptz
)
language plpgsql security definer set search_path = public as $$
declare
  v_limit int;
begin
  if not public.is_admin() then
    raise exception 'forbidden';
  end if;
  v_limit := least(greatest(coalesce(p_limit, 50), 1), 200);
  return query
    select a.id, a.key, a.revision, a.actor, a.old_value, a.new_value, a.at
    from public.config_audit a
    order by a.at desc, a.id desc
    limit v_limit;
end;
$$;

-- ── ٦) المنح: للمالك المسجَّل وحده، و`public`/`anon` لا يريان شيئًا ───────────────────────────
revoke all on function public.admin_health_stats() from public, anon;
revoke all on function public.admin_compat_stats() from public, anon;
revoke all on function public.admin_prune_installs(int) from public, anon;
revoke all on function public.admin_audit_page(int) from public, anon;
grant execute on function public.admin_health_stats() to authenticated;
grant execute on function public.admin_compat_stats() to authenticated;
grant execute on function public.admin_prune_installs(int) to authenticated;
grant execute on function public.admin_audit_page(int) to authenticated;
