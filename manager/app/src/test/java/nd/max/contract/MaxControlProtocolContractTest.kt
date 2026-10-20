/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.contract

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * **عقد المنظومة الواحدة:** تطبيق المستخدم (`manager/`) ولوحة المالك (`app/` في الجذر) طرفان
 * لمنظومة واحدة — يتكلّمان دالّة Supabase واحدة (`maxctl`) وعرضًا موقّعًا واحدًا
 * (`config_signed`). وهذا الملفّ يحرس الاتفاق **من المصدرين معًا**، فلا ينكسر في صمت إن أضاف
 * أحد الطرفين فعلًا أو حقلًا لم يعرفه الآخر.
 *
 * **ولماذا اختبار لا مستند (§١–٢):** الطرفان كُتبا بلغتين (Kotlin وTypeScript) ولا يجمعهما
 * مُصرّف واحد؛ فالفشل الحقيقيّ المتوقّع هو «إضافة فعل في اللوحة لا تعرفه الدالّة» أو «حقل يقرؤه
 * التطبيق ولا يكتبه الخادم» — وكلاهما يظهر هنا في **ثوانٍ على JVM** بدل أن يُكتشف على جهاز.
 *
 * **وحدّه المعلن:** هذا حرس نصّيّ على العقد المرئيّ (أسماء الأفعال والحقول)، لا اختبار تنفيذ
 * للدالّة نفسها — تنفيذ `maxctl` يحتاج مشروع Supabase منشورًا (خارج هذه البيئة).
 */
class MaxControlProtocolContractTest {

    private fun text(relative: String): String {
        assumeTrue(
            "repository root not reachable; guard not evaluated",
            ContractFixtures.repoRoot.isDirectory,
        )
        val file = File(ContractFixtures.repoRoot, relative)
        assertTrue("المصدر المطلوب غير موجود: $relative", file.isFile)
        return file.readText()
    }

    private val managerClient by lazy { text("manager/app/src/main/java/nd/max/core/license/LicenseClient.kt") }
    private val configClient by lazy { text("manager/app/src/main/java/nd/max/core/remote/ConfigClient.kt") }
    private val panelApi by lazy { text("app/src/main/java/com/maxmanager/control/Api.kt") }
    private val edge by lazy { text("supabase/functions/maxctl/index.ts") }
    private val schema by lazy { text("supabase/schema.sql") }

    @Test
    fun `both clients talk to one function, and its folder is where both say`() {
        assertTrue(
            "التطبيق يسمّي الدالّة",
            managerClient.contains("const val FUNCTION = \"maxctl\""),
        )
        assertTrue(
            "اللوحة تنادي المسار نفسه",
            panelApi.contains("/functions/v1/maxctl"),
        )
        assertTrue(
            "ودالّة واحدة لا اثنتان: المجلّد هو `supabase/functions/maxctl`",
            File(ContractFixtures.repoRoot, "supabase/functions/maxctl/index.ts").isFile,
        )
    }

    @Test
    fun `every action the app sends is implemented on the server`() {
        listOf("license.redeem", "license.validate").forEach { action ->
            assertTrue(
                "التطبيق يرسل `$action` ولا مقابل لها في الدالّة",
                managerClient.contains("\"$action\"") && edge.contains("case \"$action\""),
            )
        }
    }

    @Test
    fun `every action the panel sends is implemented on the server`() {
        listOf(
            "config.publish",
            "license.create",
            "license.list",
            "license.detail",
            "license.revoke",
            "license.extend",
            "license.resetDevices",
        ).forEach { action ->
            assertTrue(
                "اللوحة ترسل `$action` ولا مقابل لها في الدالّة",
                panelApi.contains("\"$action\"") && edge.contains("case \"$action\""),
            )
        }
    }

    @Test
    fun `the fields the app sends are the fields the server reads`() {
        // تفعيل: بصمة المفتاح والجهاز والاسم والنسخة — ولا مفتاح خامًّا (يُقاس أدناه أيضًا).
        listOf("key_hash", "device", "label", "app_version").forEach { field ->
            assertTrue("التطبيق لا يرسل `$field`", managerClient.contains("\"$field\" to"))
            assertTrue("الدالّة لا تقرأ `body.$field`", edge.contains("body.$field"))
        }
        // تجديد: شهادة قائمة لجهاز مسجَّل تُعرَّف بمعرّفها لا ببصمة المفتاح من جديد.
        listOf("license_id", "device", "app_version").forEach { field ->
            assertTrue("التطبيق لا يرسل `$field`", managerClient.contains("\"$field\" to"))
            assertTrue("الدالّة لا تقرأ `body.$field`", edge.contains("body.$field"))
        }
    }

    @Test
    fun `the fields the panel sends are the fields the server reads`() {
        listOf("plan", "days", "device_limit", "count", "note").forEach { field ->
            assertTrue("اللوحة لا ترسل `$field`", panelApi.contains("\"$field\" to"))
            assertTrue("الدالّة لا تقرأ `body.$field`", edge.contains("body.$field"))
        }
        listOf("id", "revoked", "query", "value").forEach { field ->
            assertTrue("اللوحة لا ترسل `$field`", panelApi.contains("\"$field\" to"))
            assertTrue("الدالّة لا تقرأ `body.$field`", edge.contains("body.$field"))
        }
    }

    @Test
    fun `the fields the app reads back are the fields the server writes`() {
        // عقد السلك كما يقرؤه التطبيق: `ok` و`error` والشهادة والزمنان.
        assertTrue(managerClient.contains("val ok: Boolean"))
        assertTrue(managerClient.contains("val error: String"))
        assertTrue(managerClient.contains("val cert: LicenseEnvelope?"))
        assertTrue(managerClient.contains("@SerialName(\"server_time\")"))
        assertTrue(managerClient.contains("@SerialName(\"issued_at\")"))
        // وما يكتبه الخادم فعلًا في النجاح والرفض معًا.
        assertTrue("المنح يحمل الشهادة والزمن", edge.contains("cert: cert.envelope"))
        assertTrue(edge.contains("server_time: cert.serverTime"))
        assertTrue("والرفض يحمل الزمن أيضًا (مرساة منع رجوع الساعة)", edge.contains("server_time: new Date().toISOString()"))
        assertTrue(edge.contains("issued_at: now"))
    }

    @Test
    fun `the fields the panel reads back are the fields the server writes`() {
        listOf(
            "optBoolean(\"ok\"",
            "optString(\"error\")",
            "optString(\"message\")",
            "optJSONArray(\"keys\")",
            "optJSONArray(\"licenses\")",
            "optBoolean(\"revoked\"",
            "optInt(\"duration_days\")",
        ).forEach { read ->
            assertTrue("اللوحة تقرأ `$read`", panelApi.contains(read))
        }
        assertTrue(edge.contains("keys: keys.map("))
        assertTrue(edge.contains("licenses: out"))
        assertTrue(edge.contains("ok: true, revoked }"))
        assertTrue(edge.contains("duration_days: Math.min(next, MAX_DURATION_DAYS)"))
    }

    @Test
    fun `the raw key never crosses the wire from the app`() {
        assertTrue("التطبيق يرسل البصمة", managerClient.contains("\"key_hash\" to"))
        assertFalse(
            "حقل مفتاح خام في الطلب — البصمة وحدها تعبر",
            Regex("\"key\"\\s+to\\b").containsMatchIn(managerClient),
        )
        assertTrue("والدالّة تقرأ البصمة لا المفتاح", edge.contains("body.key_hash"))
        assertFalse(
            "الدالّة تقرأ مفتاحًا خامًا",
            Regex("body\\.key(?!_)").containsMatchIn(edge),
        )
    }

    @Test
    fun `the licence id check accepts what the server itself issues`() {
        // عطب مقيس حيًّا (٢٠٢٦-١٠-١٩): كان الشرط `HEX64.test(id.replaceAll("-", ""))` مع
        // `id.length == 36` — و`uuid` بعد إزالة الشرطات **٣٢** محرفًا، فلا يمكن إرضاؤه أبدًا:
        // ردّ `license.validate` على كل معرّف صحيح `bad_request` فلا تتجدّد شهادة أبدًا.
        // وهنا يُستخرج تعبير الدالّة نفسه ويُقاس به: صحيحٌ يُقبل، وبصمةُ مفتاحٍ ومعرّفٌ ناقصٌ يُرفضان.
        val pattern = Regex("const LICENSE_ID = /([^/]+)/;").find(edge)?.groupValues?.get(1)
        if (pattern == null) {
            fail("`LICENSE_ID` غير معرّف في الدالّة — حرس معرّف الرخصة غائب")
            return
        }
        val check = Regex(pattern)
        assertTrue(
            "معرّف `uuid` قياسيّ (وهو ما تُصدره القاعدة) يجب أن يُقبل",
            check.matches("6b1a2c33-8d4e-4f5a-9c7b-0e1d2a3b4c5d"),
        )
        assertFalse("بصمة المفتاح `HEX64` ليست معرّف رخصة", check.matches("a".repeat(64)))
        assertFalse("ومعرّفٌ ناقص حرفًا لا يُقبل", check.matches("6b1a2c33-8d4e-4f5a-9c7b-0e1d2a3b4c5"))
        assertTrue(
            "وفرع التجديد يفحص بالمعرّف لا بـ`HEX64`",
            edge.contains("LICENSE_ID.test(licenseId)") && !edge.contains("HEX64.test(licenseId"),
        )
    }

    @Test
    fun `the licence verdict is never a local flag — the server decides and signs`() {
        // §٣ من المهمّة بنصّه: لا `isPremium = true` حكمًا. ولا وجود له في مصادر التطبيق كلها.
        val offenders = ContractFixtures.mainSourceFiles()
            .filter { it.readText().contains(Regex("isPremium\\s*=\\s*true")) }
            .map { it.name }
        assertTrue("متغيّر محلي يُثبت الاشتراك: $offenders", offenders.isEmpty())
        // والقرار الخادميّ قائم: حراسة معدّل + توقيع داخل الدالّة.
        assertTrue("حراسة المعدّل في الدالّة", edge.contains("rateGuard"))
        assertTrue("والتوقيع داخلها", edge.contains("signEnvelope"))
    }

    @Test
    fun `one signed config source — the view the app reads is the one the schema creates`() {
        assertTrue(configClient.contains("/rest/v1/config_signed"))
        assertTrue(schema.contains("create or replace view public.config_signed"))
        assertTrue(
            "والقراءة ممنوحة للعميل المجهول وحده (لا كتابة)",
            schema.contains("grant select on public.config_signed to anon, authenticated"),
        )
    }

    @Test
    fun `the signing secret is server-side only, in both clients`() {
        assertTrue("الدالّة تقرأ السرّ من بيئة الخادم", edge.contains("LICENSE_SIGNING_KEY_PKCS8"))
        val managerLeaks = ContractFixtures.mainSourceFiles().filter { it.readText().contains("PRIVATE KEY") }
        assertTrue("مفتاح خاص داخل تطبيق المستخدم: $managerLeaks", managerLeaks.isEmpty())
        val panelLeaks = File(ContractFixtures.repoRoot, "app/src/main")
            .walkTopDown()
            .filter { it.isFile }
            .filter { it.readText().contains("PRIVATE KEY") }
            .map { it.name }
            .toList()
        assertTrue("مفتاح خاص داخل اللوحة: $panelLeaks", panelLeaks.isEmpty())
    }

    // ══ قناة الإحصاءات (`MAX-CONTROL-01` §١٠): عقدها بين التطبيق والمخطّط مباشرةً — لا مع دالّة
    //    `maxctl` (لا تعرفها عن قصد: الإشارة والسحب دالّتان في المخطّط، §١٣ بند ٥ و٦). ══

    private val statsRules by lazy { text("manager/app/src/main/java/nd/max/core/remote/StatsRules.kt") }
    private val trackClient by lazy { text("manager/app/src/main/java/nd/max/core/remote/TrackClient.kt") }
    private val installIdentity by lazy { text("manager/app/src/main/java/nd/max/core/remote/InstallIdentity.kt") }
    private val communityStats by lazy { text("manager/app/src/main/java/nd/max/core/access/CommunityStats.kt") }

    @Test
    fun `the stats signal talks to the same rpc functions the schema defines`() {
        assertTrue("الإشارة تنادي دالّة المخطّط نفسها", trackClient.contains("/rest/v1/rpc/track_install"))
        assertTrue("والسحب ينادي دالّته", trackClient.contains("/rest/v1/rpc/forget_install"))
        assertTrue(schema.contains("create or replace function public.track_install("))
        assertTrue(schema.contains("create or replace function public.forget_install(p_install uuid)"))
        assertTrue(
            "التوقيع الثلاثيّ القديم يُسقط قبل الإنشاء حتى لا يبقى توقيعان بنفس الاسم",
            schema.contains("drop function if exists public.track_install(uuid, text, text);"),
        )
        assertTrue(
            "والمفتاح المنشور وحده ينفّذهما (لا `public`)",
            Regex(
                "grant execute on function public\\.track_install" +
                    "\\(uuid, text, text, text, text, int, text, text, jsonb\\)\\s+to anon, authenticated;",
            ).containsMatchIn(schema),
        )
        assertTrue(
            "وسحبُ التنفيذ عن `public` قبل المنح",
            Regex("revoke all on function public\\.track_install\\([^)]*\\) from public;").containsMatchIn(schema),
        )
        assertTrue(
            schema.contains("grant execute on function public.forget_install(uuid) to anon, authenticated;"),
        )
    }

    @Test
    fun `the install fields the app sends are the parameters the schema reads`() {
        // الحقول بأسمائها المجرّدة: مصدر التطبيق يحتويها داخل نصّ JSON مُهرَّب، فالتطابق النصّيّ
        // هنا على الرمز نفسه — وهو ما يراه PostgREST مفتاحًا في الجسم.
        listOf(
            "p_install", "p_country", "p_version",
            "p_manufacturer", "p_model", "p_sdk", "p_release", "p_env", "p_health",
        ).forEach { field ->
            assertTrue("التطبيق لا يبني `$field`", statsRules.contains(field))
            assertTrue("المخطّط لا يقرأ `$field`", schema.contains(field))
        }
    }

    @Test
    fun `the health counters the app builds are the ones the schema stores`() {
        val healthModel = text("manager/app/src/main/java/nd/max/core/hardware/HealthModel.kt")
        assertTrue("عمود health غائب عن المخطّط", schema.contains("add column if not exists health jsonb"))
        assertTrue("وقيد الشكل غائب", schema.contains("installs_health_shape"))
        assertTrue("والدالّة لا تقرأ العدّادات", schema.contains("p_health jsonb default null"))
        assertTrue("والإشارة لا تحمل الحقل", statsRules.contains("p_health"))
        // المجالات المعلومة نفسها على الطرفين — قائمة واحدة لا قائمتان تنجرفان.
        listOf("cpu", "gpu", "memory", "storage", "thermal", "audio", "display", "touch", "charging", "spoof", "other")
            .forEach { domain ->
                assertTrue("المجال `$domain` غائب عن نموذج التطبيق", healthModel.contains("\"$domain\""))
                assertTrue("المجال `$domain` غائب عن منقّي الخادم", schema.contains("'$domain'"))
            }
        // والتصنيفات الثلاثة بأسمائها على الطرفين.
        listOf("confirmed", "rolled_back", "unknown").forEach { key ->
            assertTrue("التصنيف `$key` غائب عن نموذج التطبيق", healthModel.contains(key))
            assertTrue("التصنيف `$key` غائب عن منقّي الخادم", schema.contains("'$key'"))
        }
    }

    @Test
    fun `the panel stats sections call only rpcs the schema defines`() {
        listOf(
            "admin_country_stats",
            "admin_compat_stats",
            "admin_health_stats",
            "admin_audit_page",
        ).forEach { fn ->
            assertTrue("اللوحة تنادي `$fn`", panelApi.contains("/rest/v1/rpc/$fn"))
            assertTrue("والمخطّط لا يعرّف `$fn`", schema.contains("function public.$fn("))
        }
        // وسجلّ التدقيق لا يقبل تعديلًا ولا حذفًا — مشغّل يرمي لا منحًا ناقصًا فحسب.
        assertTrue(
            "سجلّ التدقيق قابل للتعديل",
            schema.contains("create trigger config_audit_guard") &&
                schema.contains("config_audit is append-only"),
        )
        assertTrue("ولا تقليم التثبيتات غائب", schema.contains("function public.admin_prune_installs("))
    }

    @Test
    fun `one signal a day is guarded on the server not on the client alone`() {
        val fn = schema.substringAfter("create or replace function public.track_install(")
        val guardAt = fn.indexOf("at time zone 'utc'")
        val insertAt = fn.indexOf("insert into public.installs (")
        assertTrue("الحارس اليوميّ غائب (§٤.٢ قاعدة ٧)", guardAt >= 0)
        assertTrue("والحارس يسبق الكتابة", guardAt < insertAt)
        assertTrue(
            "ونطاق اليوم بتوقيت UTC الصريح لا بتوقيت الخادم المحلّيّ",
            schema.contains("(i.last_seen at time zone 'utc')::date = (now() at time zone 'utc')::date"),
        )
    }

    @Test
    fun `the stats signal is off until the user says yes`() {
        assertTrue(
            "الافتراضيّ مطفأ في قراءة الموافقة",
            communityStats.contains("getBoolean(KEY_CONSENT, false)"),
        )
        assertTrue(
            "وشرطا الموافقة والإذن الخادميّ يقترنان في حكم واحد",
            statsRules.contains("if (!consent) return StatsDue.Skip(StatsSkip.CONSENT_OFF)") &&
                statsRules.contains("if (!collect) return StatsDue.Skip(StatsSkip.COLLECT_OFF)"),
        )
    }

    @Test
    fun `no hardware identifiers ever enter the stats channel`() {
        // وملفّا العدّادات الجديدان ضمن القناة نفسها: يُفحصان بالقائمة ذاتها لا باستثناء.
        val healthModel = text("manager/app/src/main/java/nd/max/core/hardware/HealthModel.kt")
        val healthLedger = text("manager/app/src/main/java/nd/max/core/hardware/HealthLedger.kt")
        listOf("ANDROID_ID", "getImei", "getSerial", "getAdvertisingId", "Settings.Secure").forEach { banned ->
            listOf(statsRules, trackClient, installIdentity, communityStats, healthModel, healthLedger).forEach { source ->
                assertFalse("معرّف عتاد ممنوع `$banned` في قناة الإحصاءات", source.contains(banned))
            }
        }
    }
}
