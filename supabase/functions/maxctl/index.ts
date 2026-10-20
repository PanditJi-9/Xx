// ═══════════════════════════════════════════════════════════════════════════════════════════
// maxctl — دالّة التحكّم الوحيدة: توقيع الإعداد + الترخيص والمفاتيح (MAX-CONTROL-02)
// ═══════════════════════════════════════════════════════════════════════════════════════════
// تعمل على Supabase Edge Functions (Deno). ولماذا دالّة واحدة لا خمس:
//   • مفتاح التوقيع الخاصّ **لا يخرج من أسرار الدالّة** — لا في قاعدة البيانات ولا في اللوحة
//     ولا في التطبيق. ووجود نقطة توقيع واحدة يعني مكانًا واحدًا يُراجَع ويُدقَّق ويُدار.
//   • ونشرُ سطحٍ واحد أبسط من نشر خمس دوالّ كلٌّ بعقدها (وكلّ خطأ في عقدٍ منها = تفعيل معطّل).
//
// النشر (أمران):
//   supabase functions deploy maxctl --no-verify-jwt
//   supabase secrets set LICENSE_SIGNING_KEY_PKCS8="<base64 من PKCS#8 DER للمفتاح الخاصّ>"
// والتوليد وأخذ المفتاح العامّ: انظر README.md بجانب هذا الملفّ.
//
// ⚠️ الحدّ الصادق: هذه الدالّة تحكم على **الخادم** (مفتاح صالح؟ مقاعد؟ انتهاء؟ إلغاء؟)،
// والتطبيق يتحقق من الشهادة الموقّعة بلا اتصال. ولا شيء من هذا يجعل تطبيقًا يعمل على جهاز
// مروّت «مستحيل الاختراق» — البيان الكامل لما يُمنع وما لا يُمنع في `docs/ai/LICENSE-KEYS-01-spec.md` §5.

import { createClient } from "npm:@supabase/supabase-js@2";

// ── ثوابت العقد ─────────────────────────────────────────────────────────────────────────────

const SCHEMA = 1;
const BODY_LEN = 20; // 100 بت عشوائية
const ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"; // Crockford base32
const PREFIX = "MAXM";
const DAY_MS = 86_400_000;
const MAX_COUNT = 200;
const MAX_DURATION_DAYS = 3650;
const MAX_DEVICE_LIMIT = 50;

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });

const fail = (status: number, error: string, message = "") => json(status, { ok: false, error, message });

/**
 * رفضٌ يحمل **زمن الخادم** — وليس تجميلًا: التطبيق يثبّت به «مرساة الزمن» ضد رجوع ساعة الجهاز
 * (`LicenseStates.anchored`)، فجهازٌ انتهى مفتاحه ثم أرجع ساعته لا يُنقذه الرجوع بعد أول اتصال.
 * ولهذا يحمل كلُّ ردِّ رفضٍ هنا `server_time` حتى لو لم يحمل غيره.
 */
const denial = (error: string, message = "", extra: Record<string, unknown> = {}) =>
  json(200, { ok: false, error, message, server_time: new Date().toISOString(), ...extra });

// ── أدوات تشفير صغيرة (بلا مكتبات) ──────────────────────────────────────────────────────────

function toB64(bytes: Uint8Array): string {
  let s = "";
  for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s);
}

function fromB64(text: string): Uint8Array {
  const bin = atob(text.replaceAll("-", "+").replaceAll("_", "/"));
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function hexBytes(bytes: Uint8Array): string {
  return [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
}

// ── خزنة نصّ المفتاح: AES-256-GCM بمفتاح مشتقّ من سرّ الدالّة ────────────────────────────────
// المفتاح لا يخرج من Edge Function، والنصّ لا يُعاد إلا في `license.detail` بعد `requireAdmin`.
// و`LICENSE_VAULT_KEY` سرّ منفصل مستحسن؛ وغيابه يُشتقّ من مفتاح التوقيع حتى لا يتعطّل الإنشاء.

async function vaultMaterial(): Promise<Uint8Array | null> {
  const dedicated = Deno.env.get("LICENSE_VAULT_KEY") ?? "";
  const signing = Deno.env.get("LICENSE_SIGNING_KEY_PKCS8") ?? "";
  const source = dedicated || signing;
  if (!source) return null;
  const salt = dedicated ? "maxmanager-license-vault-v2" : "maxmanager-license-vault-v1";
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(`${salt}|${source}`),
  );
  return new Uint8Array(digest);
}

async function sealKey(display: string): Promise<string> {
  const raw = await vaultMaterial();
  if (!raw) return "";
  const key = await crypto.subtle.importKey("raw", raw, "AES-GCM", false, ["encrypt"]);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const cipher = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    new TextEncoder().encode(display),
  );
  return `v1.${toB64(iv)}.${toB64(new Uint8Array(cipher))}`;
}

async function openKey(sealed: string): Promise<string> {
  if (!sealed.startsWith("v1.")) return "";
  const parts = sealed.split(".");
  if (parts.length !== 3) return "";
  const raw = await vaultMaterial();
  if (!raw) return "";
  try {
    const key = await crypto.subtle.importKey("raw", raw, "AES-GCM", false, ["decrypt"]);
    const clear = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: fromB64(parts[1]) },
      key,
      fromB64(parts[2]),
    );
    return new TextDecoder().decode(clear);
  } catch {
    return "";
  }
}

// ── إثبات حيازة مفتاح الجهاز: توقيع RSA-2048 من Android Keystore ────────────────────────────
// `device` = أوّل 32 محرفًا من SHA-256 لمفتاح SPKI العامّ. الخادم يتحقّق من التوقيع على نصّ
// قياسيّ يحمل الوجهة والموضوع والزمن؛ فلا يكفي نسخ معرّف الجهاز من ملفّ لتقليد التثبيت.

const DEVICE_PROOF_PREFIX = "MAXMANAGER-LICENSE-V1";
const DEVICE_PROOF_WINDOW_MS = 10 * 60 * 1000;

function deviceProofPayload(action: string, device: string, subject: string, atMs: number): string {
  return [DEVICE_PROOF_PREFIX, action, device, subject, String(Math.trunc(atMs))].join("\n");
}

async function verifyDeviceProof(
  action: string,
  device: string,
  subject: string,
  body: Record<string, unknown>,
): Promise<"ok" | "legacy" | "bad"> {
  const publicKey = clean(body.device_public_key, 4096);
  const proof = clean(body.device_proof, 4096);
  const at = Math.trunc(Number(body.device_proof_at ?? 0));
  if (!publicKey && !proof) return "legacy";
  if (!publicKey || !proof || !Number.isFinite(at) || at <= 0) return "bad";
  if (Math.abs(Date.now() - at) > DEVICE_PROOF_WINDOW_MS) return "bad";
  try {
    const spki = fromB64(publicKey);
    const signature = fromB64(proof);
    const digest = await crypto.subtle.digest("SHA-256", spki);
    if (hexBytes(new Uint8Array(digest)).slice(0, 32) !== device) return "bad";
    const key = await crypto.subtle.importKey(
      "spki",
      spki,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );
    const ok = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      key,
      signature,
      new TextEncoder().encode(deviceProofPayload(action, device, subject, at)),
    );
    return ok ? "ok" : "bad";
  } catch {
    return "bad";
  }
}

/**
 * WebCrypto يُعيد توقيع ECDSA **خامًّا** (r‖s، ٦٤ بايتًا لـP-256)، و`SHA256withECDSA` في
 * جافا (وهو ما يتحقق به التطبيق) يتوقّع **DER**: SEQUENCE { INTEGER r, INTEGER s }.
 * والتحويل هنا لا اختياري: بدونه تُرفض كل شهادة صحيحة على الجهاز.
 */
function rawToDer(raw: Uint8Array): Uint8Array {
  const half = raw.length / 2;
  const int = (part: Uint8Array): number[] => {
    let i = 0;
    while (i < part.length - 1 && part[i] === 0) i++;
    let body = Array.from(part.slice(i));
    if (body[0] & 0x80) body = [0x00, ...body];
    return [0x02, body.length, ...body];
  };
  const r = int(raw.slice(0, half));
  const s = int(raw.slice(half));
  const seq = [...r, ...s];
  if (seq.length > 127) throw new Error("signature too long");
  return new Uint8Array([0x30, seq.length, ...seq]);
}

// ── المفتاح والحمولة الموقّعة ───────────────────────────────────────────────────────────────

let signingKey: CryptoKey | null = null;

async function loadSigningKey(): Promise<CryptoKey> {
  if (signingKey) return signingKey;
  const sec = Deno.env.get("LICENSE_SIGNING_KEY_PKCS8") ?? "";
  if (!sec.trim()) throw new Error("LICENSE_SIGNING_KEY_PKCS8 is not configured");
  const der = fromB64(sec.trim());
  signingKey = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  return signingKey;
}

/** يوقّع نصًّا ويُعيد الغلاف الذي يفكّه التطبيق: {schema, payload, signature}. */
async function signEnvelope(payload: string, extra: Record<string, unknown>): Promise<Record<string, unknown>> {
  const key = await loadSigningKey();
  const raw = new Uint8Array(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, new TextEncoder().encode(payload)));
  return { schema: SCHEMA, ...extra, payload: toB64(new TextEncoder().encode(payload)), signature: toB64(rawToDer(raw)) };
}

// ── المفاتيح: توليدٌ بصيغة يحقّقها التطبيق بلا اتصال ─────────────────────────────────────────

function randomBody(): string {
  const bytes = new Uint8Array(BODY_LEN);
  crypto.getRandomValues(bytes);
  let out = "";
  for (let i = 0; i < BODY_LEN; i++) out += ALPHABET[bytes[i] % 32];
  return out;
}

/** محرفا تحقّق (١٠ بت): كشف خطأ نسخ قبل أي اتصال — والخادم لا يعتمد عليهما. */
async function checksum(body: string): Promise<string> {
  const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(PREFIX + body)));
  return ALPHABET[digest[0] % 32] + ALPHABET[digest[1] % 32];
}

/** الصيغة القانونية للمفتاح (التي يُجزَّأ منها) = MAXM + الحمولة + التجزيء، بلا فواصل. */
async function newKey(): Promise<{ canonical: string; display: string; hint: string; hash: string }> {
  const body = randomBody();
  const check = await checksum(body);
  const canonical = PREFIX + body + check;
  const display = `${PREFIX}-${body.slice(0, 5)}-${body.slice(5, 10)}-${body.slice(10, 15)}-${body.slice(15, 20)}-${check}`;
  return { canonical, display, hint: `${body.slice(0, 5)}…${check}`, hash: await sha256Hex(canonical) };
}

// ── الإيقاف المؤقّت: إضافة أيام «قافزة» فوق النوافذ المجانية ───────────────────────────────

/**
 * يحسب نهاية صلاحية المفتاح مع مراعاة الفترات المجانية: عدّاد المفتاح **يتوقّف** داخل كل
 * نافذة مجانية تلامس مدّته — إضافة `durationDays` يومًا *عاملة* لا تقويمية.
 * والخوارزمية حتميّة بلا حالة: تُعاد بنفس النتيجة في كل نداء، فلا يُحتاج «مؤشّر» يُخزَّن
 * ولا يتضاعف الحساب (وهو ما يُثبت باختباراته في `tools/test_license_jvm.py` — النسخة
 * الكوتلينية تُقاس، وهذه نظيرتها).
 */
function addDaysSkipping(startMs: number, daysMs: number, windows: { from: number; to: number }[]) {
  const sorted = windows.filter((w) => w.to > w.from).sort((a, b) => a.from - b.from);
  let cursor = startMs;
  let left = daysMs;
  let pausedMs = 0;
  for (let guard = 0; left > 0 && guard < 1000; guard++) {
    const end = cursor + left;
    const w = sorted.find((x) => x.from < end && x.to > cursor);
    if (!w) {
      cursor = end;
      break;
    }
    const s = Math.max(w.from, cursor);
    const before = s - cursor;
    if (before > 0) {
      left -= before;
      cursor = s;
      if (left <= 0) break;
    }
    pausedMs += w.to - s;
    cursor = w.to;
  }
  return { untilMs: cursor, pausedMs };
}

async function promoWindows(svc: ReturnType<typeof createClient>): Promise<{ from: number; to: number }[]> {
  const { data } = await svc.from("app_config").select("payload").eq("key", "main").maybeSingle();
  const b64 = String((data as { payload?: string } | null)?.payload ?? "");
  if (!b64) return [];
  try {
    const parsed = JSON.parse(new TextDecoder().decode(fromB64(b64))) as { promos?: unknown };
    const list = Array.isArray(parsed.promos) ? parsed.promos : [];
    return list
      .map((p) => {
        const from = Date.parse(String((p as Record<string, unknown>)?.from ?? ""));
        const to = Date.parse(String((p as Record<string, unknown>)?.to ?? ""));
        return { from, to };
      })
      .filter((w) => Number.isFinite(w.from) && Number.isFinite(w.to) && w.to > w.from);
  } catch {
    return [];
  }
}

// ── الشهادة: حمولة موقّعة تُثبت «هذا المفتاح لهذا الجهاز إلى هذا الوقت» ─────────────────────

interface CertInput {
  licenseId: string;
  device: string;
  plan: string;
  startsAtMs: number;
  expiresAtMs: number;
  pausedMs: number;
  deviceLimit: number;
  devicesUsed: number;
}

async function licenseCert(input: CertInput) {
  const now = new Date().toISOString();
  const payload = JSON.stringify({
    schema: SCHEMA,
    kind: "license",
    license_id: input.licenseId,
    device: input.device,
    plan: input.plan,
    state: "active",
    issued_at: now,
    server_time: now,
    starts_at: new Date(input.startsAtMs).toISOString(),
    expires_at: new Date(input.expiresAtMs).toISOString(),
    paused_ms: input.pausedMs,
    device_limit: input.deviceLimit,
    devices_used: input.devicesUsed,
  });
  return {
    envelope: await signEnvelope(payload, { license: input.licenseId }),
    expiresAt: new Date(input.expiresAtMs).toISOString(),
    serverTime: now,
  };
}

// ── التحقق من المدخلات ──────────────────────────────────────────────────────────────────────

const HEX64 = /^[0-9a-f]{64}$/;
const DEVICE = /^[0-9a-f]{16,64}$/;
/**
 * معرّف الرخصة: `uuid` قياسيّ بشرطاته الأربعة (٣٦ محرفًا). وليس `HEX64`: بعد إزالة الشرطات يبقى
 * **٣٢** محرفًا فقط، فالشرط السابق `HEX64.test(id.replaceAll("-", ""))` لم يكن يُرضى أبدًا —
 * وقيست النتيجة حيًّا (٢٠٢٦-١٠-١٩): `license.validate` كان يردّ `bad_request` على كل معرّف صحيح،
 * فشهادةٌ لا تتجدّد أبدًا.
 */
const LICENSE_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

function clean(text: unknown, cap: number): string {
  return String(text ?? "").trim().slice(0, cap);
}

function intIn(value: unknown, min: number, max: number, fallback: number): number {
  const n = Math.trunc(Number(value));
  return Number.isFinite(n) && n >= min && n <= max ? n : fallback;
}

// ── سقف المعدّل: القرار دالّة حتميّة بلا حالة، والعدّ في جدول قصير العمر ─────────────────────

interface RateAttempt {
  at: number;
  ok: boolean;
}

interface RateLimits {
  minuteMs: number;
  maxFailuresMinute: number;
  hourMs: number;
  maxFailuresHour: number;
  maxAttemptsMinute: number;
}

interface RateVerdict {
  blocked: boolean;
  retryAfterSec: number;
  reason: string;
  failuresMinute: number;
  failuresHour: number;
  attemptsMinute: number;
}

/**
 * الحدود المعلَنة في مكان واحد، واختيارها مقصود: **الفشل** هو ما يُسقَف أوّلًا (تخمين مفاتيح ·
 * استنزاف مقاعد)، فالنجاح معدّله معدّل التطبيق؛ ويُسقَف فوقه سقفُ «عنف» على كل المحاولات — فنداءٌ
 * بمعدّل ٣٠/دقيقة ليس سلوك تطبيق حتى بمفتاح صالح. والسقوف **لكل بصمة عميل** لا لكل مفتاح: الهجوم
 * يأتي بمفتاح غير موجود أيضًا، ولا بصمة له.
 */
const RATE_LIMITS: RateLimits = {
  minuteMs: 60_000,
  maxFailuresMinute: 8,
  hourMs: 3_600_000,
  maxFailuresHour: 40,
  maxAttemptsMinute: 30,
};

/** عمر السجلّ: سقفٌ عمره دقيقة لا يحتاج أسبوعًا من التاريخ (والحذف نادر داخل `rateGuard`). */
const RATE_KEEP_DAYS = 7;

/**
 * قرار السقف — حتميّ بلا حالة، ويُقاس **بالنصّ نفسه** في `tools/test_maxctl_pure.py`.
 *
 * والنافذة نصف مفتوحة `[now − window, now]`: محاولة عمرها بالضبط = طول النافذة **خرجت** منها،
 * ومحاولة بطابع زمنيّ مستقبليّ (ساعة متقدّمة) لا تُحتسب — فلا يُلوَّث العدّ بها.
 * و`retry_after` لا يعني «جرّب بعد ثانية»: يعني متى يعود العدّ **تحت السقف** فعلًا (خروج ما يكفي
 * من العناصر، لا الأقدم وحده) — فطلبٌ مبكّر يبقى مرفوضًا، والانتظار الصادق خير من طلبٍ عبثيّ.
 */
function rateLimitDecision(nowMs: number, attempts: RateAttempt[], limits: RateLimits): RateVerdict {
  const waitMs = (times: number[], max: number, windowMs: number): number => {
    if (times.length < max) return 0;
    const sorted = [...times].sort((a, b) => a - b);
    const leaving = sorted[times.length - max];
    return Math.max(0, leaving + windowMs - nowMs);
  };

  const recent = attempts.filter((a) => a.at <= nowMs && nowMs - a.at < limits.hourMs);
  const minute = recent.filter((a) => nowMs - a.at < limits.minuteMs);
  const failedMinute = minute.filter((a) => !a.ok).map((a) => a.at);
  const failedHour = recent.filter((a) => !a.ok).map((a) => a.at);

  const checks = [
    { reason: "failures_minute", wait: waitMs(failedMinute, limits.maxFailuresMinute, limits.minuteMs) },
    { reason: "failures_hour", wait: waitMs(failedHour, limits.maxFailuresHour, limits.hourMs) },
    { reason: "attempts_minute", wait: waitMs(minute.map((a) => a.at), limits.maxAttemptsMinute, limits.minuteMs) },
  ];
  let worst = { reason: "", wait: 0 };
  for (const check of checks) if (check.wait > worst.wait) worst = check;

  return {
    blocked: worst.wait > 0,
    retryAfterSec: worst.wait > 0 ? Math.max(1, Math.ceil(worst.wait / 1000)) : 0,
    reason: worst.reason,
    failuresMinute: failedMinute.length,
    failuresHour: failedHour.length,
    attemptsMinute: minute.length,
  };
}

/** بصمة العميل: SHA-256 لـ(العنوان + سرّ) مبتورًا — أثرٌ للعدّ لا هوية تُخزَّن. */
async function clientBucket(req: Request, salt: string): Promise<string> {
  const forwarded = req.headers.get("x-forwarded-for") ?? req.headers.get("x-real-ip") ?? "";
  const address = forwarded.split(",")[0].trim() || "unknown";
  return (await sha256Hex(`${salt}|${address}`)).slice(0, 32);
}

/**
 * بوابة السقف على الوجهتين العامّتين: تقرأ محاولات البصمة في الساعة الأخيرة، تقرّر، وتُعيد إمّا
 * رفضًا جاهزًا يحمل `retry_after`، وإمّا دالّة **تسجيل** تُنادى بنتيجة النداء (نجاحًا أو فشلًا).
 * والقراءة قبل أي عمل آخر عن قصد: الرفض أرخص من العمل، فلا يُدفع ثمنُ عملٍ لطلبٍ مرفوض.
 *
 * وسرّ التجزئة `RATE_SALT` إن وُجد، وإلا مفتاح الخدمة: البصمة تبقى مستقرةً لكل نشر، ولا يُخزَّن
 * عنوان خامّ في أي صفّ — نفس سياسة «لا معرّفات» التي يعمل بها التطبيق.
 */
async function rateGuard(
  svc: ReturnType<typeof createClient>,
  req: Request,
  kind: string,
  salt: string,
): Promise<Response | ((ok: boolean) => Promise<void>)> {
  const bucket = await clientBucket(req, salt);
  const nowMs = Date.now();
  const { data: rows } = await svc
    .from("license_redeem_attempts")
    .select("at,ok")
    .eq("bucket", bucket)
    .gte("at", new Date(nowMs - RATE_LIMITS.hourMs).toISOString())
    .order("at", { ascending: false })
    .limit(500);
  const attempts = (rows ?? [])
    .map((row) => ({
      at: Date.parse(String((row as { at?: string }).at ?? "")),
      ok: (row as { ok?: boolean }).ok === true,
    }))
    .filter((a) => Number.isFinite(a.at));
  const verdict = rateLimitDecision(nowMs, attempts, RATE_LIMITS);
  if (verdict.blocked) {
    return denial("rate_limited", "محاولات كثيرة — أعد المحاولة بعد قليل", {
      retry_after: verdict.retryAfterSec,
      reason: verdict.reason,
    });
  }
  return async (ok: boolean) => {
    await svc.from("license_redeem_attempts").insert({ bucket, kind, ok });
    // تنظيف نادر (١٪ من النداءات): سجلّ السقف قصير العمر بطبيعته ولا يستحقّ مهمّة صيانة مستقلّة.
    if (Math.random() < 0.01) {
      await svc
        .from("license_redeem_attempts")
        .delete()
        .lt("at", new Date(Date.now() - RATE_KEEP_DAYS * DAY_MS).toISOString());
    }
  };
}

// ── الدالّة ─────────────────────────────────────────────────────────────────────────────────

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response(null, { status: 204 });
  if (req.method !== "POST") return fail(405, "method");

  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const svc = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  let body: Record<string, unknown>;
  try {
    body = (await req.json()) as Record<string, unknown>;
  } catch {
    return fail(400, "bad_request", "invalid JSON body");
  }
  const action = String(body.action ?? "");

  // الفحوص الإدارية: هوية مستخدم حقيقية **وهي في جدول المدراء**. ولا يُقبل رمز مجهول أبدًا.
  const requireAdmin = async (): Promise<string | Response> => {
    const header = req.headers.get("Authorization") ?? "";
    const jwt = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!jwt) return fail(401, "unauthorized");
    const userClient = createClient(url, anonKey || serviceKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: userData, error: userError } = await userClient.auth.getUser(jwt);
    const user = userData?.user;
    if (userError || !user) return fail(401, "unauthorized");
    const { data: admin } = await svc.from("admins").select("user_id").eq("user_id", user.id).maybeSingle();
    if (!admin) return fail(403, "forbidden");
    return user.id;
  };

  try {
    switch (action) {
      // ═══ تفعيل مفتاح (عامّ: بلا تسجيل دخول) ═══════════════════════════════════════════════
      case "license.redeem": {
        const guard = await rateGuard(svc, req, "redeem", serviceKey);
        if (guard instanceof Response) return guard;
        const record = guard;
        const deny = async (error: string, message = "", extra: Record<string, unknown> = {}) => {
          await record(false);
          return denial(error, message, extra);
        };
        const reject = async (message: string) => {
          await record(false);
          return fail(400, "bad_request", message);
        };

        const hash = String(body.key_hash ?? "").toLowerCase();
        const device = String(body.device ?? "").toLowerCase();
        if (!HEX64.test(hash)) return reject("key_hash");
        if (!DEVICE.test(device)) return reject("device");
        const legacyDevice = String(body.legacy_device ?? "").toLowerCase();
        if (legacyDevice && !DEVICE.test(legacyDevice)) return reject("legacy_device");
        const label = clean(body.label, 80);
        const version = clean(body.app_version, 20);
        const proof = await verifyDeviceProof("license.redeem", device, hash, body);
        if (proof === "bad") return reject("device_proof");
        const publicKey = clean(body.device_public_key, 4096);

        const { data: licenseRow } = await svc
          .from("licenses")
          .select("*")
          .eq("key_hash", hash)
          .maybeSingle();
        if (!licenseRow) {
          await svc.from("license_events").insert({ kind: "redeem_rejected", detail: "not_found" });
          return deny("not_found", "المفتاح غير موجود");
        }
        const lic = licenseRow as Record<string, unknown>;
        const licenseId = String(lic.id);
        if (lic.revoked === true) {
          await svc.from("license_events").insert({ license_id: licenseId, kind: "redeem_rejected", detail: "revoked" });
          return deny("revoked", "المفتاح ملغى");
        }

        const startsAtMs = lic.starts_at ? Date.parse(String(lic.starts_at)) : Date.now();
        const durationDays = intIn(lic.duration_days, 1, MAX_DURATION_DAYS, 30);
        const deviceLimit = intIn(lic.device_limit, 1, MAX_DEVICE_LIMIT, 1);

        const { data: devices } = await svc
          .from("license_devices")
          .select("device,device_public_key,active")
          .eq("license_id", licenseId);
        const activeDevices = (devices ?? []).filter((d) => (d as { active?: boolean }).active !== false);
        const knownRow = activeDevices.find((d) => String((d as { device?: string }).device) === device);
        const known = knownRow !== undefined;
        const legacyRow = !known && legacyDevice && legacyDevice !== device
          ? activeDevices.find((d) => String((d as { device?: string }).device) === legacyDevice)
          : undefined;
        const boundKey = String(
          (knownRow as { device_public_key?: string } | undefined)?.device_public_key ??
            (legacyRow as { device_public_key?: string } | undefined)?.device_public_key ?? "",
        );

        // جهاز معروف ومرتبط بمفتاح: لا يُقبل إلا بتوقيع يطابق المفتاح المحفوظ.
        if (known && boundKey) {
          if (proof !== "ok" || publicKey !== boundKey) {
            await svc.from("license_events").insert({ license_id: licenseId, kind: "redeem_rejected", detail: "device_proof" });
            return deny("device_proof_required", "هذا الجهاز مرتبط بمفتاح آخر");
          }
        }
        // ترحيل معرّف قديم (عشوائي) إلى الهوية الجديدة: نفس المقعد، بلا استهلاك إضافي.
        if (!known && legacyRow) {
          if (boundKey && (proof !== "ok" || publicKey !== boundKey)) {
            await svc.from("license_events").insert({ license_id: licenseId, kind: "rebind_rejected", detail: "device_proof" });
            return deny("device_proof_required", "الجهاز القديم مرتبط بمفتاح آخر");
          }
          if (proof !== "ok" || !publicKey) {
            await svc.from("license_events").insert({ license_id: licenseId, kind: "rebind_rejected", detail: "no_proof" });
            return deny("device_unknown", "أعد تفعيل المفتاح على النسخة الجديدة");
          }
        }
        if (!known && !legacyRow && activeDevices.length >= deviceLimit) {
          await svc.from("license_events").insert({ license_id: licenseId, kind: "redeem_rejected", detail: "device_limit" });
          return deny("device_limit", "بلغ المفتاح حدّه من الأجهزة", {
            devices_used: activeDevices.length,
            device_limit: deviceLimit,
          });
        }

        const windows = await promoWindows(svc);
        const { untilMs, pausedMs } = addDaysSkipping(startsAtMs, durationDays * DAY_MS, windows);
        if (untilMs <= Date.now()) {
          await svc.from("license_events").insert({ license_id: licenseId, kind: "redeem_rejected", detail: "expired" });
          return deny("expired", "انتهت صلاحية المفتاح");
        }

        const nowIso = new Date().toISOString();
        if (!known && legacyRow) {
          const { error: rebindError } = await svc
            .from("license_devices")
            .update({
              device,
              device_public_key: publicKey,
              label,
              app_version: version,
              last_seen: nowIso,
              active: true,
            })
            .eq("license_id", licenseId)
            .eq("device", legacyDevice);
          if (rebindError) return fail(500, "server", rebindError.message);
        } else {
          const { error: upsertError } = await svc.from("license_devices").upsert(
            {
              license_id: licenseId,
              device,
              device_public_key: publicKey || boundKey,
              label,
              app_version: version,
              last_seen: nowIso,
              active: true,
              ...(known ? {} : { first_seen: nowIso }),
            },
            { onConflict: "license_id,device" },
          );
          if (upsertError) return fail(500, "server", upsertError.message);
        }

        await svc.from("licenses").update({ starts_at: new Date(startsAtMs).toISOString(), updated_at: nowIso }).eq("id", licenseId);
        await svc.from("license_events").insert({
          license_id: licenseId,
          kind: known ? "device_seen" : legacyRow ? "device_rebound" : "device_added",
          detail: version,
        });

        const used = known || legacyRow ? activeDevices.length : activeDevices.length + 1;
        const cert = await licenseCert({
          licenseId,
          device,
          plan: String(lic.plan ?? ""),
          startsAtMs,
          expiresAtMs: untilMs,
          pausedMs,
          deviceLimit,
          devicesUsed: used,
        });
        await record(true);
        return json(200, {
          ok: true,
          cert: cert.envelope,
          plan: String(lic.plan ?? ""),
          expires_at: cert.expiresAt,
          paused_ms: pausedMs,
          devices_used: used,
          device_limit: deviceLimit,
          server_time: cert.serverTime,
        });
      }

      // ═══ تجديد الشهادة (عامّ): يكتشف الإلغاء ويبني الإيقاف المؤقّت الجديد ══════════════════
      case "license.validate": {
        const guard = await rateGuard(svc, req, "validate", serviceKey);
        if (guard instanceof Response) return guard;
        const record = guard;
        const deny = async (error: string, message = "", extra: Record<string, unknown> = {}) => {
          await record(false);
          return denial(error, message, extra);
        };
        const reject = async (message: string) => {
          await record(false);
          return fail(400, "bad_request", message);
        };

        const licenseId = String(body.license_id ?? "");
        const device = String(body.device ?? "").toLowerCase();
        if (!LICENSE_ID.test(licenseId)) {
          return reject("license_id");
        }
        if (!DEVICE.test(device)) return reject("device");
        const legacyDevice = String(body.legacy_device ?? "").toLowerCase();
        if (legacyDevice && !DEVICE.test(legacyDevice)) return reject("legacy_device");
        const version = clean(body.app_version, 20);
        const proof = await verifyDeviceProof("license.validate", device, licenseId, body);
        if (proof === "bad") return reject("device_proof");
        const publicKey = clean(body.device_public_key, 4096);

        const { data: licenseRow } = await svc.from("licenses").select("*").eq("id", licenseId).maybeSingle();
        if (!licenseRow) return deny("not_found");
        const lic = licenseRow as Record<string, unknown>;
        if (lic.revoked === true) return deny("revoked");

        const { data: existingRow } = await svc
          .from("license_devices")
          .select("device,device_public_key,active")
          .eq("license_id", licenseId)
          .eq("device", device)
          .maybeSingle();
        let deviceRow = existingRow as
          | { device?: string; device_public_key?: string; active?: boolean }
          | null;

        // ترحيل شفّاف: شهادة قديمة مربوطة بمعرّف عشوائي تنتقل إلى هوية المفتاح الجديدة
        // بنفس المقعد — بشرط إثبات حيازة المفتاح الخاصّ، لا بمجرّد معرفة المعرّف القديم.
        if (!deviceRow && legacyDevice && legacyDevice !== device) {
          const { data: legacyRow } = await svc
            .from("license_devices")
            .select("device,device_public_key,active")
            .eq("license_id", licenseId)
            .eq("device", legacyDevice)
            .maybeSingle();
          const legacy = legacyRow as
            | { device?: string; device_public_key?: string; active?: boolean }
            | null;
          if (legacy && legacy.active !== false) {
            const legacyKey = String(legacy.device_public_key ?? "");
            if (proof !== "ok" || !publicKey || (legacyKey && legacyKey !== publicKey)) {
              await svc.from("license_events").insert({ license_id: licenseId, kind: "rebind_rejected", detail: "device_proof" });
              return deny("device_proof_required", "الجهاز القديم مرتبط بمفتاح آخر");
            }
            const nowIso = new Date().toISOString();
            const { error: rebindError } = await svc
              .from("license_devices")
              .update({ device, device_public_key: publicKey, last_seen: nowIso, app_version: version })
              .eq("license_id", licenseId)
              .eq("device", legacyDevice);
            if (rebindError) return fail(500, "server", rebindError.message);
            await svc.from("license_events").insert({ license_id: licenseId, kind: "device_rebound", detail: version });
            deviceRow = { device, device_public_key: publicKey, active: true };
          }
        }

        if (!deviceRow) return deny("device_unknown");
        if (deviceRow.active === false) return deny("device_removed");

        const boundKey = String(deviceRow.device_public_key ?? "");
        if (boundKey && (proof !== "ok" || publicKey !== boundKey)) {
          await svc.from("license_events").insert({ license_id: licenseId, kind: "validate_rejected", detail: "device_proof" });
          return deny("device_proof_required", "هذا الجهاز مرتبط بمفتاح آخر");
        }
        if (!boundKey && proof === "ok" && publicKey) {
          // ترقية جهاز سبق تسجيله قبل إضافة الإثبات: يُثبَّت مفتاحه العامّ الآن.
          await svc
            .from("license_devices")
            .update({ device_public_key: publicKey })
            .eq("license_id", licenseId)
            .eq("device", device);
        }

        const { data: devices } = await svc.from("license_devices").select("active").eq("license_id", licenseId);
        const used = (devices ?? []).filter((d) => (d as { active?: boolean }).active !== false).length;

        const startsAtMs = lic.starts_at ? Date.parse(String(lic.starts_at)) : Date.now();
        const durationDays = intIn(lic.duration_days, 1, MAX_DURATION_DAYS, 30);
        const deviceLimit = intIn(lic.device_limit, 1, MAX_DEVICE_LIMIT, 1);
        const windows = await promoWindows(svc);
        const { untilMs, pausedMs } = addDaysSkipping(startsAtMs, durationDays * DAY_MS, windows);
        if (untilMs <= Date.now()) return deny("expired");

        await svc
          .from("license_devices")
          .update({ last_seen: new Date().toISOString(), app_version: version })
          .eq("license_id", licenseId)
          .eq("device", device);

        const cert = await licenseCert({
          licenseId,
          device,
          plan: String(lic.plan ?? ""),
          startsAtMs,
          expiresAtMs: untilMs,
          pausedMs,
          deviceLimit,
          devicesUsed: used,
        });
        await record(true);
        return json(200, {
          ok: true,
          cert: cert.envelope,
          plan: String(lic.plan ?? ""),
          expires_at: cert.expiresAt,
          paused_ms: pausedMs,
          devices_used: used,
          device_limit: deviceLimit,
          server_time: cert.serverTime,
        });
      }

      // ═══ إنشاء مفاتيح (مالك فقط): تُعرض مرة واحدة ولا تُخزَّن خامّة أبدًا ═════════════════
      case "license.create": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const count = intIn(body.count, 1, MAX_COUNT, 1);
        const days = intIn(body.days, 1, MAX_DURATION_DAYS, 30);
        const deviceLimit = intIn(body.device_limit, 1, MAX_DEVICE_LIMIT, 1);
        const plan = clean(body.plan, 40);
        const note = clean(body.note, 200);

        const keys: { display: string; hint: string; hash: string }[] = [];
        for (let i = 0; i < count; i++) keys.push(await newKey());

        const rows = [];
        for (const k of keys) {
          rows.push({
            key_hash: k.hash,
            key_hint: k.hint,
            key_ciphertext: await sealKey(k.display),
            plan,
            note,
            duration_days: days,
            device_limit: deviceLimit,
            created_by: who,
          });
        }
        const { error: insertError } = await svc.from("licenses").insert(rows);
        if (insertError) return fail(500, "server", insertError.message);
        await svc.from("license_events").insert({ kind: "created", detail: `${count}×${days}d/${deviceLimit}` });
        return json(200, { ok: true, keys: keys.map((k) => ({ key: k.display, hint: k.hint })) });
      }

      // ═══ قائمة المفاتيح (مالك فقط) ═════════════════════════════════════════════════════════
      case "license.list": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const query = clean(body.query, 40).replaceAll("…", "").toUpperCase();
        const limit = intIn(body.limit, 1, 500, 200);

        let request = svc
          .from("licenses")
          .select("id,key_hint,plan,note,duration_days,device_limit,revoked,starts_at,created_at")
          .order("created_at", { ascending: false })
          .limit(limit);
        if (query) request = request.ilike("key_hint", `%${query}%`);
        const { data: rows, error: listError } = await request;
        if (listError) return fail(500, "server", listError.message);

        const list = (rows ?? []) as Record<string, unknown>[];
        const ids = list.map((r) => String(r.id));
        let devices: Record<string, unknown>[] = [];
        if (ids.length) {
          const { data: devs } = await svc
            .from("license_devices")
            .select("license_id,active,last_seen")
            .in("license_id", ids);
          devices = (devs ?? []) as Record<string, unknown>[];
        }
        const windows = await promoWindows(svc);
        const out = list.map((r) => {
          const mine = devices.filter((d) => String(d.license_id) === String(r.id));
          const active = mine.filter((d) => d.active !== false);
          const startsAt = r.starts_at ? Date.parse(String(r.starts_at)) : null;
          const { untilMs } = startsAt
            ? addDaysSkipping(startsAt, intIn(r.duration_days, 1, MAX_DURATION_DAYS, 30) * DAY_MS, windows)
            : { untilMs: 0 };
          const lastSeen = active.map((d) => String(d.last_seen ?? "")).sort().pop() ?? "";
          return {
            id: r.id,
            hint: r.key_hint,
            plan: r.plan,
            note: r.note,
            duration_days: r.duration_days,
            device_limit: r.device_limit,
            revoked: r.revoked === true,
            starts_at: r.starts_at,
            expires_at: startsAt ? new Date(untilMs).toISOString() : null,
            created_at: r.created_at,
            devices_used: active.length,
            devices_total: mine.length,
            last_seen: lastSeen,
          };
        });
        return json(200, { ok: true, licenses: out });
      }

      // ═══ تفاصيل مفتاح + أجهزته (مالك فقط) ══════════════════════════════════════════════════
      case "license.detail": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const id = String(body.id ?? "");
        const { data: licenseRow } = await svc
          .from("licenses")
          .select("id,key_hint,key_ciphertext,plan,note,duration_days,device_limit,revoked,starts_at,created_at,updated_at")
          .eq("id", id)
          .maybeSingle();
        if (!licenseRow) return denial("not_found");
        const rawLicense = licenseRow as Record<string, unknown>;
        const sealed = String(rawLicense.key_ciphertext ?? "");
        const fullKey = sealed ? await openKey(sealed) : "";
        const license = { ...rawLicense };
        delete license.key_ciphertext;
        license.key = fullKey;
        license.key_recoverable = fullKey.length > 0;
        const { data: devices } = await svc
          .from("license_devices")
          .select("device,label,app_version,first_seen,last_seen,active,device_public_key")
          .eq("license_id", id)
          .order("last_seen", { ascending: false });
        const devicesOut = (devices ?? []).map((row) => {
          const { device_public_key, ...rest } = row as Record<string, unknown>;
          return { ...rest, bound: String(device_public_key ?? "").length > 0 };
        });
        const { data: events } = await svc
          .from("license_events")
          .select("kind,detail,at")
          .eq("license_id", id)
          .order("at", { ascending: false })
          .limit(50);
        return json(200, { ok: true, license, devices: devicesOut, events: events ?? [] });
      }

      // ═══ إلغاء / إعادة (مالك فقط) ═══════════════════════════════════════════════════════════
      case "license.revoke": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const id = String(body.id ?? "");
        const revoked = body.revoked !== false;
        const { error: revokeError } = await svc
          .from("licenses")
          .update({ revoked, updated_at: new Date().toISOString() })
          .eq("id", id);
        if (revokeError) return fail(500, "server", revokeError.message);
        await svc.from("license_events").insert({ license_id: id, kind: revoked ? "revoked" : "unrevoked" });
        return json(200, { ok: true, revoked });
      }

      // ═══ تمديد (مالك فقط): يزيد المدّة وتعاود شهادات الأجهزة البناء عند التجديد التالي ════
      case "license.extend": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const id = String(body.id ?? "");
        const days = intIn(body.days, 1, MAX_DURATION_DAYS, 30);
        const { data: licenseRow } = await svc.from("licenses").select("duration_days").eq("id", id).maybeSingle();
        if (!licenseRow) return denial("not_found");
        const next = intIn((licenseRow as { duration_days?: unknown }).duration_days, 1, MAX_DURATION_DAYS, 30) + days;
        const { error: extendError } = await svc
          .from("licenses")
          .update({ duration_days: Math.min(next, MAX_DURATION_DAYS), updated_at: new Date().toISOString() })
          .eq("id", id);
        if (extendError) return fail(500, "server", extendError.message);
        await svc.from("license_events").insert({ license_id: id, kind: "extended", detail: `+${days}d` });
        return json(200, { ok: true, duration_days: Math.min(next, MAX_DURATION_DAYS) });
      }

      // ═══ تحرير كل المقاعد (مالك فقط): الأثر يبقى والأجهزة تُعلَّم غير نشطة ══════════════════
      case "license.resetDevices": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const id = String(body.id ?? "");
        const { error: resetError } = await svc
          .from("license_devices")
          .update({ active: false })
          .eq("license_id", id)
          .eq("active", true);
        if (resetError) return fail(500, "server", resetError.message);
        await svc.from("license_events").insert({ license_id: id, kind: "devices_reset" });
        return json(200, { ok: true });
      }

      // ═══ نشر الإعداد الموقّع (مالك فقط): البايتات الموقّعة تُخزَّن كما هي ══════════════════
      case "config.publish": {
        const who = await requireAdmin();
        if (who instanceof Response) return who;
        const value = body.value;
        if (!value || typeof value !== "object" || Array.isArray(value)) {
          return fail(400, "bad_request", "value");
        }
        const { data: current } = await svc.from("app_config").select("revision").eq("key", "main").maybeSingle();
        const revision = Number((current as { revision?: number } | null)?.revision ?? 0) + 1;
        const issuedAt = new Date().toISOString();
        const payloadObject = { ...(value as Record<string, unknown>), schema: SCHEMA, revision, issued_at: issuedAt };
        const payloadText = JSON.stringify(payloadObject);
        const envelope = await signEnvelope(payloadText, {});
        const { error: publishError } = await svc.from("app_config").upsert(
          {
            key: "main",
            value: payloadObject,
            schema: SCHEMA,
            revision,
            payload: envelope.payload,
            signature: envelope.signature,
            issued_at: issuedAt,
            updated_by: who,
            updated_at: issuedAt,
          },
          { onConflict: "key" },
        );
        if (publishError) return fail(500, "server", publishError.message);
        return json(200, { ok: true, revision, issued_at: issuedAt });
      }

      default:
        return fail(400, "unknown_action", action);
    }
  } catch (error) {
    return fail(500, "server", error instanceof Error ? error.message : "unknown");
  }
});
