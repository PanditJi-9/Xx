/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.contract

import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nd.max.core.gamespace.GameProfileDocument
import nd.max.ui.util.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * عقد `gamelist/maxmanagerApplist.json` من طرف الكاتب — النصف Kotlin من
 * `fixtures/contracts/gamelist.compact.json`.
 *
 * العطب الذي وُلد هذا الملفّ من أجله
 * ----------------------------------
 * `AppLoader/AppLoader.c::reload_gamelist_cache()` هو القارئ الوحيد لهذا الملفّ، وهو مدخل **كل**
 * ميزة لكل تطبيق في الخادم. وكان يطالب بشكل واحد: `": {"` — بنقطتين وفراغ. والكتابة هنا
 * (`GameProfileDocument.patch(...).toString()`) تُخرج JSON مضغوطًا بلا فراغ: `":{"`.
 * ولا شيء كان يقيس التهجئة بين اللغتين، فصار سجلّ جهاز المستخدم يقول:
 *
 * ```
 * EVENT=GAMELIST_LOADED count=0    ← وللملفّ ١٢ تطبيقًا
 * ```
 *
 * ومن ثمّ `get_gamestart()` تُعيد NULL، فلا يُطبَّق بروفايل أداء ولا DND ولا تردد — «يبقى على
 * المتوازن ولو فتحت اللعبة».
 *
 * ولماذا هذا الاختبار يقيس شيئًا رغم أنه مقارنة نصّين
 * ------------------------------------------------
 * لأن الطرف الآخر يقيس **الملفّ نفسه**: `archdaemon/tests/app_loader.c` يُمرّر
 * `gamelist.compact.json` إلى `reload_gamelist_cache` الحقيقيّ ويؤكّد أنه يقرأ منه القيم المُعلَنة
 * (`performance` · `true` · `0.75` · `120` · `650000000`) ويجد التطبيق عبر `get_gamestart`. فالملفّ
 * نقطة التحام بين لغتين: إن انحرف الترميز هنا أو المسح هناك، سقط أحد الطرفين — بدل أن يبقى
 * الشكل ضمنيًّا في `AppConfig` وفي `strstr` لا يجمع بينهما شيء.
 *
 * ⚠️ وإن أُضيف حقل إلى `AppConfig` فسيسقط الاختبار الأول **عن قصد**: الشكل تغيّر، فيُعاد توليد
 * الملفّ ويُقاس الطرف الآخر من جديد. ولا يُحرَّر الملفّ ليمرّ الاختبار.
 */
class GamelistContractTest {

    /** نفس إعدادات `GameProfileRepository` — فالملفّ يُنتَج بمسار الكتابة الحقيقيّ لا بمثاله. */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val packageName = "com.example.game"

    /**
     * نفس القيم المُعلَنة في `archdaemon/tests/app_loader.c` — رقم واحد في البيانات يُقاس في لغتين.
     * وكل حقل بقيمة مختلفة عن غيره وعن الافتراضي، فانزياح حقل إلى حقل ظاهر لا مُخمَّن.
     */
    private fun fixtureConfig(): AppConfig = AppConfig(
        perf_lite_mode = "lite",
        app_priority = "high",
        game_preload = "true",
        cpu_boost = "true",
        cpu_policy_controls = "policy0|lock|300000|1800000",
        gpu_profile = "performance",
        cpu_governor = "performance",
        gpu_governor = "msm-adreno-tz",
        gpu_max_freq = "650000000",
        thermal_profile = "power",
        refresh_rate = "120",
        renderer = "vulkan",
        resolution_downscale = "0.75",
        dnd_on_gaming = "true",
        bypass_charging = "true",
        touch_boost = "true",
        haptic_feedback = "false",
        kill_bg_apps = "true",
        force_hw_ui = "true",
        disable_notifs = "true",
        wifi_no_sleep = "true",
    )

    /** مسار الكتابة كما هو: `AppConfig` → عضو كائن → مستند → `.toString()` المضغوط. */
    private fun documentFor(pkg: String, config: AppConfig): String {
        val known = json.parseToJsonElement(json.encodeToString(config)) as JsonObject
        return GameProfileDocument.patch(GameProfileDocument.parse("{}"), pkg, known).toString()
    }

    @Test
    fun `the writer reproduces the shared fixture byte for byte`() {
        assertEquals(
            "the writer and fixtures/contracts/gamelist.compact.json disagree; the C reader " +
                "asserts the values in that file, so one of the two moved",
            ContractFixtures.text("gamelist.compact.json"),
            documentFor(packageName, fixtureConfig()),
        )
    }

    @Test
    fun `the fixture is the compact shape — the one that used to be unreadable`() {
        val text = ContractFixtures.text("gamelist.compact.json")

        assertTrue(
            "the fixture must keep the writer's compact member separator `\":{\"`; a pretty-printed " +
                "fixture would let a space-requiring parser pass while the app writes no space",
            text.contains("\":{\""),
        )
        assertFalse(
            "the fixture must not contain the spaced form `\": {\"` — it is not what the writer emits, " +
                "and its presence would hide the defect this corpus exists to catch",
            text.contains("\": {\""),
        )
    }

    @Test
    fun `the legacy fixture carries both spellings so the C alias and its bound are measured`() {
        val document = json.parseToJsonElement(ContractFixtures.text("gamelist.legacy.json")).jsonObject

        // (أ) الملفّ المزروع يكتب `resolution_target` وحده ⇒ الاسم القديم يُقرأ.
        val seeded = document["com.legacy.seed"]!!.jsonObject
        assertEquals("0.5", seeded["resolution_target"]!!.jsonPrimitive.content)
        assertFalse("the alias entry must not carry the canonical key", "resolution_downscale" in seeded)

        // (ب) الاسم القانونيّ يفوز عند اجتماعهما.
        val both = document["com.both.keys"]!!.jsonObject
        assertEquals("0.75", both["resolution_downscale"]!!.jsonPrimitive.content)
        assertEquals("0.5", both["resolution_target"]!!.jsonPrimitive.content)

        // (ج) وصفّ بلا المفتاحين يقيس أن البحث لا يتجاوز كتلة التطبيق.
        val neither = document["com.no.res.key"]!!.jsonObject
        assertFalse("resolution_downscale" in neither)
        assertFalse("resolution_target" in neither)
    }

    @Test
    fun `every declared key of the shipped seed is a field AppConfig actually decodes`() {
        val seed = File(ContractFixtures.repoRoot, "maxmanagerApplist.json")
        assertTrue("the shipped seed is missing: ${seed.path}; guard not evaluated", seed.isFile)

        val document = json.parseToJsonElement(seed.readText()).jsonObject
        val known = AppConfig.serializer().descriptor.elementNames.toSet()

        assertTrue("the shipped seed must not be empty", document.isNotEmpty())

        document.forEach { (pkg, entry) ->
            val fields = entry.jsonObject
            val unknown = fields.keys.filterNot { it in known }
            assertTrue(
                "seed entry $pkg declares keys no AppConfig field decodes, so the value is dropped " +
                    "silently (the defect `resolution_target` was): $unknown",
                unknown.isEmpty(),
            )
            /*
             * والطرف الآخر: القيمة تُفكّ فعلًا إلى الحقل لا إلى الافتراضي.
             * `ignoreUnknownKeys` غير مُفعَّل هنا عن قصد، فمفتاح لا يفكّه حقل = استثناء لا مرور.
             */
            assertNotNull(
                "seed entry $pkg does not decode into AppConfig",
                Json.decodeFromString(AppConfig.serializer(), fields.toString()),
            )
        }
    }
}
