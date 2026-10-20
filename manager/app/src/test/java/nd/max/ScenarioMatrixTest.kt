/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max

import nd.max.core.atlas.AtlasCapabilityInputs
import nd.max.core.atlas.AtlasCapabilityMap
import nd.max.core.atlas.AtlasCapabilityRules
import nd.max.core.atlas.AtlasCapabilityState
import nd.max.core.atlas.AtlasControlTarget
import nd.max.core.atlas.AtlasRouteReason
import nd.max.core.atlas.AtlasRouteStatus
import nd.max.core.atlas.AtlasSafetyVerdict
import nd.max.core.audio.AudioSystemEvidence
import nd.max.core.audio.AudioSystemLayerStatus
import nd.max.core.audio.AudioSystemReason
import nd.max.core.audio.AudioWriteOutcome
import nd.max.core.audio.audioKnobNotAttempted
import nd.max.core.audio.audioKnobVerdict
import nd.max.core.audio.audioSystemLayerVerdict
import nd.max.core.audio.audioWriteVerdict
import nd.max.core.hardware.TouchRateMath
import nd.max.core.hardware.WriteVerification
import nd.max.core.license.LicenseEnvelope
import nd.max.core.license.LicenseFault
import nd.max.core.license.LicenseRecord
import nd.max.core.license.LicenseStates
import nd.max.core.license.LicenseStatus
import nd.max.core.maxai.ClampVerdict
import nd.max.core.privilege.PrivilegeCatalog
import nd.max.core.privilege.PrivilegeLevel
import nd.max.core.privilege.PrivilegeTier
import nd.max.ui.theme.NavBarStyle
import nd.max.ui.theme.ThemeContrast
import nd.max.ui.theme.ThemeFontScale
import nd.max.ui.theme.ThemeMotion
import nd.max.ui.theme.ThemeRoundness
import nd.max.ui.theme.ThemeStudioPresets
import nd.max.ui.util.CleanMeasurement
import nd.max.ui.util.CleanMeasurementSet
import nd.max.ui.util.StorageCareCategory
import nd.max.ui.util.StorageCareModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **مصفوفة السيناريوهات — الجزء القابل للقياس بلا جهاز.**
 *
 * نصّ المهمّة (§١٠) يطلب ≥ ١٠٠ حالة منطقية موزّعة على محاور، وألّا يكون مستندًا بديلًا عن
 * اختبار. فهذا الملف **اختبار** لا مستند: كل سطرٍ فيه حالةٌ تُشغَّل وتُحكم، والمحاور التي لا
 * تُقاس بلا عتاد تُعلن في نهايته بأسمائها — لا تُحاكى وتُسمّى «مُختبَرة».
 *
 * **والمعدود هنا ١٣٩ حالة (١..١٣٩ مكتوبة بجانب كل حكم) موزّعة على تسعة محاور:**
 *
 * 1. **الترخيص (١٩)** — صالح · منتهٍ · بلا شهادة · بلا معرّف · حدود اللحظة · ساعة مُرجَعة · ساعة مُقدَّمة.
 * 2. **الامتياز والقدرات (١٢)** — بلا روت · Shizuku · روت، وما يجوز لكل طبقة وما لا يجوز.
 * 3. **الكتابة ثم القراءة (٢١)** — مطابقة · اختلاف صيغة عدديّة · قمع من المنصّة · تعذّر قراءة (لا حكم) ·
 *    حقل powerhal وسط متجه سياسات.
 * 4. **الثيم واللغة والاتجاه (١٦ + ٨)** — قيمة مخزّنة غريبة · مطابقة الأنماط الجاهزة · ثبات مفاتيح
 *    التعداد · تدرّج المقاييس والتباين والاستدارة.
 * 5. **خريطة قدرات Atlas (١٨)** — سلّم الأسبقيّة: تحريم أمنيّ ← تحقّق ← كتابة ← مُلاءِم ← قراءة ←
 *    اسم مجهول، والغياب المُثبت، ورمز السجل، ورفض تكرار الهدف.
 * 6. **العناية بالتخزين (١٥)** — الاختيار الآمن لا يشير إلى ملفّ قد يملكه المستخدم، والمجموع يقول ما
 *    سيُحذف لا ما هو موجود، والفشل يخفض المجموع ويرفع رايته ولا يُقرأ صفرًا.
 * 7. **قياس اللمس (٦)** — العيّنة القصيرة مجهولة لا معدّلًا، والمعدّل أحداث على زمن مقيس.
 * 8. **الأستوديو الصوتي (٢٣)** — سلّم حكم واحد: مجهول ← محجوب ← مطبَّق/فاشل، والقراءة هي الدليل؛
 *    وسلّم طبقة النظام يعرض أوّل شرط لم يتحقّق لا الأبعد.
 * 9. **حدود معلنة (١)** — أسماء المحاور التي تحتاج جهازًا (بند «ما لا يُقاس»).
 *
 * **والمحور §٤-و (١٧ حالة) في `UnavailableFeaturesTest`** — أُخرج من هنا عند سقف الألف سطر
 * (`code_health`)، وأرقامه هناك ١..١٧؛ فغيابه من هذه القائمة ليس نقصًا في التغطية.
 *
 * وما لا يُقاس هنا: RTL الفعليّ · حجم الخط على الشاشة · تبديل مخرج الصوت · انقطاع الخدمة
 * والإنترنت أثناء عمليّة · إعادة التشغيل وتحديث التطبيق · SELinux والإقلاع والعتاد · الأداء
 * تحت الحمل. وهذه **تحتاج جهازًا** — وكُتبت بأسمائها في نهاية الملف كي لا تُقرأ مُغطّاة.
 */
class ScenarioMatrixTest {

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ١ — الترخيص (١٩ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `licence — no record, no verdict`() {
        // (١)
        assertEquals(LicenseStatus.NONE, LicenseStates.evaluate(null, 1_000L).status)
        // (٢) معرّف فارغ ⇒ لا حكم ولو كانت الشهادة موجودة
        assertEquals(
            LicenseStatus.NONE,
            LicenseStates.evaluate(record(id = "", payload = "signed"), 1_000L).status,
        )
        // (٣) حمولة شهادة فارغة ⇒ لا حكم ولو كانت الأرقام المحلية كلها سليمة
        assertEquals(
            "أرقام محلّيّة بلا شهادة موقّعة ليست ترخيصًا",
            LicenseStatus.NONE,
            LicenseStates.evaluate(record(payload = ""), nowMillis = 5_000L).status,
        )
        // (٤) الواجهة تقرأ `active` من الحالة، لا من رقم محلي
        assertFalse(LicenseStates.evaluate(record(payload = ""), 5_000L).active)
    }

    @Test
    fun `licence — expiry boundaries are exact, not approximate`() {
        val expiry = 10_000L
        // (٥) لحظة الانتهاء نفسها ⇒ منتهٍ (الحدّ مُوقَّع، والوصول إليه انتهاء)
        assertEquals(
            LicenseStatus.EXPIRED,
            LicenseStates.evaluate(record(expiresAt = expiry, payload = "s"), expiry).status,
        )
        // (٦) مللي ثانية واحدة قبل ذلك ⇒ صالح ببقيّة = ١
        val justBefore = LicenseStates.evaluate(record(expiresAt = expiry, payload = "s"), expiry - 1)
        assertEquals(LicenseStatus.ACTIVE, justBefore.status)
        assertEquals(1L, justBefore.remainingMillis)
        // (٧) وبعدها بمللي ⇒ منتهٍ، والبقيّة صفر لا سلبية
        val after = LicenseStates.evaluate(record(expiresAt = expiry, payload = "s"), expiry + 1)
        assertEquals(LicenseStatus.EXPIRED, after.status)
        assertEquals(0L, after.remainingMillis)
    }

    @Test
    fun `licence — rolling the clock back cannot resurrect an expired key`() {
        // (٨) الخادم شهد زمنًا بعد الانتهاء، والمستخدم أرجع ساعته قبل الانتهاء
        val record = record(expiresAt = 10_000L, serverMillis = 20_000L, payload = "s")
        assertEquals(
            "الزمن المرجعي = الأبعد بين ساعة الجهاز وزمن الخادم",
            20_000L,
            LicenseStates.trustedNow(record, nowMillis = 5_000L),
        )
        assertEquals(
            LicenseStatus.EXPIRED,
            LicenseStates.evaluate(record, nowMillis = 5_000L).status,
        )
        // (٩) وحتى مع ساعة مُرجَعة لزمن سابق جدًّا يبقى الحكم منتهيًا
        assertEquals(
            LicenseStatus.EXPIRED,
            LicenseStates.evaluate(record, nowMillis = 0L).status,
        )
    }

    @Test
    fun `licence — moving the clock forward ends the key on the device that did it`() {
        // (١٠) ساعة مُقدَّمة ⇒ منتهٍ ولو كان زمن الخادم قديمًا (ضرر على صاحبه لا مكسب)
        val record = record(expiresAt = 30_000L, serverMillis = 1_000L, payload = "s")
        assertEquals(LicenseStatus.EXPIRED, LicenseStates.evaluate(record, 40_000L).status)
        // (١١) وبلا زمن خادم (٠) يُعتمد على ساعة الجهاز وحدها
        val noServer = record(expiresAt = 30_000L, serverMillis = 0L, payload = "s")
        assertEquals(LicenseStatus.ACTIVE, LicenseStates.evaluate(noServer, 20_000L).status)
        assertEquals(LicenseStatus.EXPIRED, LicenseStates.evaluate(noServer, 30_000L).status)
    }

    @Test
    fun `licence — what the screen shows comes from the signed record`() {
        val record = record(
            id = "LIC-7",
            plan = "pro",
            expiresAt = 50_000L,
            payload = "s",
            deviceLimit = 3,
            devicesUsed = 2,
            pausedMillis = 1_500L,
        )
        val state = LicenseStates.evaluate(record, 10_000L)
        // (١٢) الخطة (١٣) المعرّف (١٤) الحدّ (١٥) المستخدم (١٦) الإيقاف
        assertEquals("pro", state.plan)
        assertEquals("LIC-7", state.licenseId)
        assertEquals(3, state.deviceLimit)
        assertEquals(2, state.devicesUsed)
        assertEquals(1_500L, state.pausedMillis)
    }

    @Test
    fun `licence — faults are classified, never merged into one catch-all`() {
        val codes = LicenseFault.entries.map { it.code }
        // (١٧) لا رمز فارغ
        assertTrue(codes.all { it.isNotBlank() })
        // (١٨) ولا رمزان متطابقان
        assertEquals("كل عطب برمزه", codes.size, codes.distinct().size)
        // (١٩) و«لا شبكة» ليست «خادم يرفض»: تصرّفان مختلفان (إعادة تلقائيّة مقابل خطأ إعداد)
        assertNotEquals(LicenseFault.NETWORK.code, LicenseFault.CLIENT.code)
        assertNotEquals(LicenseFault.SERVER.code, LicenseFault.NOT_CONFIGURED.code)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٢ — الامتياز والقدرات (١٢ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `privilege — no-root can never satisfy an ADB or root capability`() {
        val level = PrivilegeLevel.NONE
        // (٢٠) (٢١)
        assertFalse(level.atLeast(PrivilegeLevel.SHIZUKU))
        assertFalse("بلا روت: بلا جذر بالبداهة", level.atLeast(PrivilegeLevel.ROOT))
        // (٢٢) ويملك ما يملكه فعلًا: نفسه
        assertTrue(level.atLeast(PrivilegeLevel.NONE))
    }

    @Test
    fun `privilege — Shizuku covers ADB features and never root ones`() {
        val level = PrivilegeLevel.SHIZUKU
        // (٢٣) (٢٤)
        assertTrue(level.atLeast(PrivilegeLevel.NONE))
        assertTrue(level.atLeast(PrivilegeLevel.SHIZUKU))
        // (٢٥) امتياز ADB ليس جذرًا — وهذا ما يمنع إعلان ميزة جذرية على جهاز Shizuku
        assertFalse(level.atLeast(PrivilegeLevel.ROOT))
    }

    @Test
    fun `privilege — root covers everything`() {
        val level = PrivilegeLevel.ROOT
        // (٢٦) (٢٧) (٢٨)
        assertTrue(level.atLeast(PrivilegeLevel.NONE))
        assertTrue(level.atLeast(PrivilegeLevel.SHIZUKU))
        assertTrue(level.atLeast(PrivilegeLevel.ROOT))
    }

    @Test
    fun `privilege — the catalogue declares a real tier for every feature, once`() {
        val features = PrivilegeCatalog.features
        // (٢٩) لا معرّف مكرّر (المعرّفات تُسجَّل وتُقرأ)
        assertEquals(
            "معرّف الميزة مفتاح لا يتكرّر",
            features.size,
            features.map { it.id }.distinct().size,
        )
        // (٣٠) وكل ميزة تصرّح بطبقتها من التعداد نفسه (لا نصّ حرّ يُقاس لاحقًا)
        assertTrue(features.all { it.tier in PrivilegeTier.entries })
        // (٣١) والكتالوج يغطّي الثلاث طبقات فعلًا — فلا ميزانية مزيّفة بطبقة واحدة
        assertEquals(
            "الطبقات الثلاث ممثّلة في الكتالوج",
            PrivilegeTier.entries.size,
            features.map { it.tier }.distinct().size,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٣ — الكتابة ثم القراءة (٢١ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `write — a value that could not be read is a no-verdict, not a failure and not a match`() {
        // (٣٢)
        assertEquals(
            "تعذّر القراءة ⇒ لا حكم: لا «نجحت» ولا «فشلت»",
            WriteVerification.Outcome.READBACK_UNAVAILABLE,
            WriteVerification.compare("1", null),
        )
        // (٣٣) والقيمة الفارغة ليست «لم تُقرأ» بل قراءة أعطت لا شيء ⇒ اختلاف
        assertEquals(
            WriteVerification.Outcome.DIFFERS,
            WriteVerification.compare("1", ""),
        )
        // (٣٤) ولا يُقرأ الفشل نجاحًا حين تكون القيمة مختلفة
        assertEquals(
            WriteVerification.Outcome.DIFFERS,
            WriteVerification.compare("on", "off"),
        )
        // (٣٥) والقيمة التي كُتبت فعلًا تُطابق
        assertEquals(
            WriteVerification.Outcome.MATCHED,
            WriteVerification.compare("1", "1"),
        )
    }

    @Test
    fun `write — the same number in another shape is the same number`() {
        // (٣٦) (٣٧) (٣٨) (٣٩)
        assertEquals(WriteVerification.Outcome.MATCHED, WriteVerification.compare("500000", "0500000"))
        assertEquals(WriteVerification.Outcome.MATCHED, WriteVerification.compare("500000", " 500000 "))
        assertEquals(WriteVerification.Outcome.MATCHED, WriteVerification.compare("500000", "+500000"))
        assertEquals(WriteVerification.Outcome.MATCHED, WriteVerification.compare("1 2", "1    2"))
    }

    @Test
    fun `write — a device that clamps is reported honestly, not as a plain failure`() {
        // (٤٠) القيمة كُتبت لكن المنصّة قيّدتها ⇒ سطر منفصل عن «فشلت الكتابة»
        assertEquals(
            "قُيّدت من المنصّة: تُعلن كذلك ولا تُخفى في «فشل»",
            WriteVerification.Outcome.DIFFERS,
            WriteVerification.compare("3000000", "1800000"),
        )
        // (٤١) والقيمة النصّية غير الرقمية تُطابق بالمقارنة النصّيّة وحدها
        assertEquals(WriteVerification.Outcome.MATCHED, WriteVerification.compare("schedutil", "schedutil"))
        assertEquals(WriteVerification.Outcome.DIFFERS, WriteVerification.compare("schedutil", "performance"))
    }

    @Test
    fun `write — a powerhal node is judged on its own policy line inside a full vector`() {
        // الصيغة الحقيقيّة لعقدة المنصّة: كل سطر «فهرس السياسة ثم min ثم max»
        // (`0 300000 1300000` في متجه كامل الأجهزة) — والفهرس أوّلًا لُغة لا تجميلًا.
        val vector = """
            0 300000 1800000
            1 300000 2400000
        """.trimIndent()
        // (٤٢) السطر المطلوب يطابق وحده والنصّ الكامل مختلف ⇒ مطابقة (لا ٩٤٣ «differs» كاذبة)
        assertEquals(
            WriteVerification.Outcome.MATCHED,
            WriteVerification.comparePowerhalField(1, 300_000L, 2_400_000L, vector),
        )
        // (٤٣) سياسة أخرى لا تُقنّع المطلوب
        assertEquals(
            WriteVerification.Outcome.DIFFERS,
            WriteVerification.comparePowerhalField(2, 300_000L, 2_400_000L, vector),
        )
        // (٤٤) أرقام معطوبة في السطر ⇒ اختلاف لا «مطابقة»
        assertEquals(
            WriteVerification.Outcome.DIFFERS,
            WriteVerification.comparePowerhalField(1, 300_000L, 2_400_000L, "1 abc 2400000"),
        )
        // (٤٥) وتعذّر القراءة يبقى بلا حكم
        assertEquals(
            WriteVerification.Outcome.READBACK_UNAVAILABLE,
            WriteVerification.comparePowerhalField(1, 300_000L, 2_400_000L, null),
        )
    }

    @Test
    fun `write — clamp verdicts separate platform clamping from real divergence`() {
        // (٤٦) (٤٧)
        assertEquals(ClampVerdict.Verdict.MATCHED, ClampVerdict.classify("300000:2400000", "300000:2400000"))
        assertEquals(ClampVerdict.Verdict.MATCHED, ClampVerdict.classify("schedutil", "schedutil"))
        // (٤٨) الأرضية نفسها والسقف أدنى ⇒ قمع من المنصّة
        assertEquals(
            ClampVerdict.Verdict.CLAMPED_BELOW,
            ClampVerdict.classify("300000:2400000", "300000:1800000"),
        )
        // (٤٩) الأرضية تغيّرت ⇒ ليس قمعًا مقبولًا بل انحراف
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            ClampVerdict.classify("300000:2400000", "400000:1800000"),
        )
        // (٥٠) وسقف **أعلى** ممّا طُلب ليس قمعًا
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            ClampVerdict.classify("300000:1800000", "300000:2400000"),
        )
        // (٥١) وقيمة غائبة لا تُصنَّف مطابقة ولا مقموعة
        assertEquals(ClampVerdict.Verdict.DIVERGED, ClampVerdict.classify(null, "300000:1800000"))
        assertEquals(ClampVerdict.Verdict.DIVERGED, ClampVerdict.classify("300000:1800000", null))
        // (٥٢) ونصّ يشبه المدى وليس مدى لا يُصنَّف قمعًا أبدًا
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            ClampVerdict.classify("abc:def", "abc:def0"),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٤ — الثيم واللغة والاتجاه (١٦ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `theme — an unknown or absent stored value falls back, never crashes`() {
        // (٥٣) (٥٤) (٥٥) (٥٦) (٥٧)
        assertEquals(ThemeContrast.DEFAULT, ThemeContrast.fromKey(null))
        assertEquals(ThemeContrast.DEFAULT, ThemeContrast.fromKey("not-a-contrast"))
        assertEquals(ThemeRoundness.STANDARD, ThemeRoundness.fromKey(null))
        assertEquals(ThemeFontScale.STANDARD, ThemeFontScale.fromKey("HUGE"))
        assertEquals(NavBarStyle.FLOATING, NavBarStyle.fromKey(""))
        // (٥٨) وكل قيمة معروفة تُقرأ كما هي (شكل الشريط مثالًا)
        assertEquals(NavBarStyle.DOCKED, NavBarStyle.fromKey(NavBarStyle.DOCKED.key))
        // (٥٩) ومستوى الحركة: قيمة غريبة ⇒ الافتراضيّ لا «بلا حركة»
        assertEquals(ThemeMotion.fromKey(null), ThemeMotion.fromKey("none-such"))
    }

    @Test
    fun `theme — every enum key is stable and unique across the studio`() {
        // (٦٠) (٦١) (٦٢) (٦٣)
        listOf(
            ThemeContrast.entries.map { it.key },
            ThemeRoundness.entries.map { it.key },
            ThemeFontScale.entries.map { it.key },
            ThemeMotion.entries.map { it.key },
        ).forEach { keys ->
            assertEquals("مفاتيح التعداد بلا تكرار", keys.size, keys.distinct().size)
            assertTrue("ولا مفتاح فارغ", keys.all { it.isNotBlank() })
        }
    }

    @Test
    fun `theme — a preset matches on all four values and nothing less`() {
        val preset = ThemeStudioPresets.first()
        // (٦٤) تطابق كامل
        assertTrue(preset.matches(preset.seed, preset.mode, preset.contrast, preset.roundness))
        // (٦٥) لون مختلف ⇒ لا
        assertFalse(preset.matches(preset.seed + 1, preset.mode, preset.contrast, preset.roundness))
        // (٦٦) استدارة مختلفة ⇒ لا (النمط مجموعة متّسقة، لا لونًا وحده)
        assertFalse(
            preset.matches(
                preset.seed,
                preset.mode,
                preset.contrast,
                ThemeRoundness.entries.first { it != preset.roundness },
            ),
        )
        // (٦٧) ومظهر مختلف ⇒ لا
        assertFalse(
            preset.matches(
                preset.seed,
                nd.max.ui.theme.ColorMode.entries.first { it != preset.mode },
                preset.contrast,
                preset.roundness,
            ),
        )
        // (٦٨) ولا نمطان جاهزان يتطابقان مع المدخلات نفسها (وإلّا لظهر التوهّج على نمطين)
        val seeds = ThemeStudioPresets.map { it.seed to it.mode }
        assertEquals("لكل نمط بذرة/مظهر مختلف", ThemeStudioPresets.size, seeds.distinct().size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٤ (تكملة) — تدرّج القيم المعلنة في الاستوديو (٨ حالات)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `theme — scales stay inside the declared band and order correctly`() {
        // (٦٩) القيمة الاسميّة للمقياس الصغير داخل الحدّ المعلن (٠٫٩ – ١٫١٢)
        assertEquals(0.90, ThemeFontScale.SMALL.scale.toDouble(), 0.0001)
        // (٧٠) (٧١) التدرّج صاعد لا مقلوب
        assertTrue(ThemeFontScale.SMALL.scale < ThemeFontScale.STANDARD.scale)
        assertTrue(ThemeFontScale.STANDARD.scale < ThemeFontScale.LARGE.scale)
        // (٧٢) وكلّها داخل النطاق الذي لا يكسر شبكات الشاشات الضيّقة
        assertTrue(ThemeFontScale.entries.all { it.scale in 0.85f..1.13f })
        // (٧٣) (٧٤) وقيم التباين متدرّجة (مقيسة من المكتبة: 0.0 · 0.5 · 1.0)
        assertTrue(ThemeContrast.DEFAULT.level < ThemeContrast.MEDIUM.level)
        assertTrue(ThemeContrast.MEDIUM.level < ThemeContrast.HIGH.level)
        // (٧٥) (٧٦) والاستدارة كذلك
        assertTrue(ThemeRoundness.SHARP.scale < ThemeRoundness.STANDARD.scale)
        assertTrue(ThemeRoundness.STANDARD.scale < ThemeRoundness.ROUND.scale)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٥ — خريطة قدرات Atlas: سلّم الأسبقيّة (١٨ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `atlas — a safety denial outranks a verified eligible route`() {
        val capability = AtlasCapabilityRules.derive(
            inputs(
                safety = AtlasSafetyVerdict.Denied("thermal-trips", "سبب تشغيلي"),
                routeStatus = AtlasRouteStatus.ELIGIBLE,
                routeKnown = true,
                verifiedThisGeneration = true,
            ),
        )
        // (٧٧) الحكم الأمني لا يعلو عليه شيء — ولا حتى تحقّق سابق
        assertEquals(AtlasCapabilityState.NEVER_TOUCH, capability.state)
        // (٧٨) والسبب يسمّي القاعدة برمزها لا بجملة
        assertEquals("safety:thermal-trips", capability.reason)
        // (٧٩) ولا ينسب لنفسه تحقّقًا في حالة يُمنع لمسها
        assertFalse(capability.verified)
    }

    @Test
    fun `atlas — supported is the only state that claims success, and it needs a verified write`() {
        val supported = AtlasCapabilityRules.derive(
            inputs(routeStatus = AtlasRouteStatus.ELIGIBLE, routeKnown = true, verifiedThisGeneration = true),
        )
        // (٨٠) (٨١)
        assertEquals(AtlasCapabilityState.SUPPORTED, supported.state)
        assertTrue(supported.verified)
        // (٨٢) ومسار مؤهّل بلا تحقّق ⇒ «قابل للكتابة» لا «مدعوم»
        val writable = AtlasCapabilityRules.derive(
            inputs(routeStatus = AtlasRouteStatus.ELIGIBLE, routeKnown = true),
        )
        assertEquals(AtlasCapabilityState.WRITABLE, writable.state)
        // (٨٣) ولا يدّعي تحقّقًا لم يقع
        assertFalse(writable.verified)
    }

    @Test
    fun `atlas — an unproven absence is never read as unavailable`() {
        // (٨٤) لا قياس أصلًا ⇒ «غير معروف» لا «غير موجود»
        assertEquals(AtlasCapabilityState.UNKNOWN, AtlasCapabilityRules.derive(inputs()).state)
        // (٨٥) قيس ولم يجب ⇒ غير معروف، وسببه «لم يُلاحظ» لا «غائب»
        val notObserved = AtlasCapabilityRules.derive(inputs(measured = true))
        assertEquals(AtlasCapabilityState.UNKNOWN, notObserved.state)
        assertEquals("not-observed", notObserved.reason)
        // (٨٦) والغياب المُثبت وحده (قائمة غير فارغة بلا الاسم) يقول «غير متاح»
        val absent = AtlasCapabilityRules.derive(inputs(measured = true, absenceProved = true))
        assertEquals(AtlasCapabilityState.UNAVAILABLE, absent.state)
        assertEquals("absence-proved", absent.reason)
    }

    @Test
    fun `atlas — readable without a route is either read-only or needs an adapter, never confused`() {
        // (٨٧) يُقرأ ولا يزعم البناء منفذ كتابة ⇒ «للقراءة فقط» وسببها صريح
        val readOnly = AtlasCapabilityRules.derive(
            inputs(measured = true, readable = true, writeExpected = false),
        )
        assertEquals(AtlasCapabilityState.READ_ONLY, readOnly.state)
        assertEquals("read-only", readOnly.reason)
        // (٨٨) ويُقرأ وللبناء مسار كتابة مزعوم بلا مسار معروف ⇒ «يحتاج مُلاءِمًا»
        val needsAdapter = AtlasCapabilityRules.derive(inputs(measured = true, readable = true))
        assertEquals(AtlasCapabilityState.NEEDS_ADAPTER, needsAdapter.state)
        assertEquals("adapter-gap", needsAdapter.reason)
        // (٨٩) وسبب أدقّ يعرفه المنادي يُقال بدل الرمز العام
        val withReason = AtlasCapabilityRules.derive(
            inputs(measured = true, readable = true, gapReason = "unclassified-devfreq-node-seen"),
        )
        assertEquals("unclassified-devfreq-node-seen", withReason.reason)
    }

    @Test
    fun `atlas — the map holds one entry per target and its code carries state and reason`() {
        val capability = AtlasCapabilityRules.derive(
            inputs(
                routeStatus = AtlasRouteStatus.ELIGIBLE,
                routeKnown = true,
                verifiedThisGeneration = true,
                routeReason = AtlasRouteReason.VERIFIED_VENDOR_ROUTE,
            ),
        )
        // (٩٠) الرمز يبدأ بالحالة (حرف صغير) — تقرؤه السجلات لا الواجهة
        assertTrue("الرمز المقيس: ${capability.code}", capability.code.startsWith("map:supported:"))
        // (٩١) ويحمل سبب الحالة
        assertTrue(capability.code.contains("route-eligible+verified"))
        // (٩٢) وخلفيّة المسار حين عُرفت
        assertTrue(capability.code.endsWith("verified_vendor_route"))

        val first = AtlasCapabilityRules.derive(inputs(target = AtlasControlTarget.CPU_FREQUENCY))
        val second = AtlasCapabilityRules.derive(inputs(target = AtlasControlTarget.GPU_FREQUENCY))
        val map = AtlasCapabilityMap(listOf(first, second))
        // (٩٣) هدف واحد لكل قدرة، والإحصاء يجمع الخريطة كلها
        assertEquals(2, map.entries.size)
        assertEquals(2, map.counts().values.sum())
        assertEquals(AtlasCapabilityState.UNKNOWN, map.forTarget(AtlasControlTarget.GPU_FREQUENCY)?.state)
        // (٩٤) وتكرار الهدف يُرفض في البناء (لا يُمرّ صامتًا ليفسد العدّ)
        assertThrows(IllegalArgumentException::class.java) {
            AtlasCapabilityMap(listOf(first, first))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٦ — العناية بالتخزين: الاختيار الآمن والمجموع الصادق (١٥ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `storage — the safe selection never points a delete at files that may belong to the user`() {
        val safe = StorageCareModel.safeSelection()
        // (٩٥) المجموعة الآمنة معدودة (الخمسة الأصلية) ولا تخلو
        assertEquals(5, safe.size)
        // (٩٦) وكلّ ما فيها مُشّر افتراضيًّا بحكم النموذج لا بحكم الواجهة
        assertTrue(safe.all { it.defaultChecked })
        // (٩٧) ملفّات APK قد تكون ملك المستخدم ⇒ خارج الآمن
        assertFalse(StorageCareCategory.InstallerFiles in safe)
        // (٩٨) والمجلدات الفارغة قد تكون له ⇒ خارج الآمن
        assertFalse(StorageCareCategory.EmptyFolders in safe)
        // (٩٩) والمكرّر لا يُحذف بلا اختيار صريح (المحتوى المتطابق ليس دليل ملكيّة)
        assertFalse(StorageCareCategory.Duplicates in safe)
        // (١٠٠) و«سلة المحذوفات» تدميرٌ لا تنظيف ⇒ خارج الآمن
        assertFalse(StorageCareCategory.RecycleBin in safe)
    }

    @Test
    fun `storage — a root-only category is hidden without root, a shared one is not`() {
        // (١٠١) فئة جذر لا تُعرض بلا جذر (تُخفى عن قصد، لا تبقى صفًّا لا يمتلئ أبدًا)
        assertFalse(StorageCareModel.visibleAt(rootAvailable = false, category = StorageCareCategory.AppCaches))
        // (١٠٢) وبلا جذر يُعرض ما يُقرأ فعلًا
        assertTrue(StorageCareModel.visibleAt(rootAvailable = false, category = StorageCareCategory.Thumbnails))
        // (١٠٣) وبالجذر تُعرض الاثنتان
        assertTrue(StorageCareModel.visibleAt(rootAvailable = true, category = StorageCareCategory.AppCaches))
        assertTrue(StorageCareModel.visibleAt(rootAvailable = true, category = StorageCareCategory.Thumbnails))
    }

    @Test
    fun `storage — the total counts what will be deleted, not what exists`() {
        val measurements = CleanMeasurementSet(
            listOf(
                CleanMeasurement(StorageCareCategory.AppCaches, 1_000L, 10),
                CleanMeasurement(StorageCareCategory.Thumbnails, 500L, 4),
                CleanMeasurement(StorageCareCategory.SystemLogs, 999_999L, 3),
            ),
        )
        val total = StorageCareModel.total(
            setOf(StorageCareCategory.AppCaches, StorageCareCategory.Thumbnails),
            measurements,
        )
        // (١٠٤) المجموع من المختار وحده — وقياس فئة مطفأة لا يدخل الزرّ وإن كان رقمًا كبيرًا
        assertEquals(1_500L, total.bytes)
        // (١٠٥) وقيست كلّ المختارة ⇒ لا «حدّ أدنى» معلّق
        assertFalse(total.hasUnmeasured)
        // (١٠٦) وهناك ما يُحذف فعلًا فيُعرض الرقم
        assertTrue(total.hasAnything)
    }

    @Test
    fun `storage — a failed measurement lowers the total and is announced, never read as zero`() {
        val measurements = CleanMeasurementSet(
            listOf(
                CleanMeasurement(StorageCareCategory.AppCaches, 1_000L, 10),
                CleanMeasurement(StorageCareCategory.CorpseData, null, 0),
            ),
        )
        val total = StorageCareModel.total(
            setOf(StorageCareCategory.AppCaches, StorageCareCategory.CorpseData),
            measurements,
        )
        // (١٠٧) الفاشلة لا تدخل الرقم (لا تُقرأ صفرًا يُنقص المجموع)
        assertEquals(1_000L, total.bytes)
        // (١٠٨) وترفع راية «المجموع حدّ أدنى» ليُقال في الشاشة
        assertTrue(total.hasUnmeasured)
        // (١٠٩) وبلا اختيار لا رقم ولا «شيء» — الزرّ لا يوعد بما لن يُفعل
        val empty = StorageCareModel.total(emptySet(), measurements)
        assertEquals(0L, empty.bytes)
        assertFalse(empty.hasAnything)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٧ — قياس اللمس: ما يُقبل وما يُعلن مجهولًا (٦ حالات)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `touch — a sample too short in time is unknown, not a rate`() {
        // (١١٠) حدثان على ٥ مللي تُعطيان «٤٠٠ حدث/ث» وهي صدفة توقيت لا معدّل
        assertNull(TouchRateMath.eventsPerSecond(2, 5L))
        // (١١١) و٣٩٩ مللي أقل من الحدّ الأدنى ⇒ مجهول
        assertNull(TouchRateMath.eventsPerSecond(300, 399L))
        // (١١٢) و٤٠٠ مللي بالضبط تُقبل (الحدّ نفسه داخل النطاق): ١٠ أحداث ÷ ٠٫٤ ث = ٢٥
        assertEquals(25.0, TouchRateMath.eventsPerSecond(10, 400L)!!, 0.0001)
    }

    @Test
    fun `touch — no events is unknown too, and the rate is events over measured time`() {
        // (١١٣) صفر أحداث ليس «٠ هرتز» بل لا قياس
        assertNull(TouchRateMath.eventsPerSecond(0, 1_000L))
        // (١١٤) (١١٥) والمعدّل أحداث على زمن مقيس بالثانية
        assertEquals(120.0, TouchRateMath.eventsPerSecond(120, 1_000L)!!, 0.0001)
        assertEquals(60.0, TouchRateMath.eventsPerSecond(60, 1_000L)!!, 0.0001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٨ — الأستوديو الصوتي: الحكم من القراءة لا من النداء (٢٣ حالة)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `audio stream — a write is applied only when it was read back equal`() {
        // (١١٦) لم تُحاول ⇒ مجهول لا فشل (والمجهول لا يُعرض صفرًا)
        assertEquals(
            AudioWriteOutcome.NOT_ATTEMPTED,
            audioWriteVerdict(
                attempted = false, applied = false, verified = false,
                blocked = false, error = null, liveLevel = null,
            ).outcome,
        )
        // (١١٧) كُتبت وقُرئت مطابقة ⇒ طُبِّقت — والدليل قراءة لا ادّعاء
        val applied = audioWriteVerdict(
            attempted = true, applied = true, verified = true,
            blocked = false, error = null, liveLevel = 7,
        )
        assertEquals(AudioWriteOutcome.APPLIED, applied.outcome)
        // (١١٨) ونجاح بلا سبب — «السبب» للمنع والفشل
        assertNull(applied.reason)
        // (١١٩) و«طُبِّق» وحدها تحمل `isApplied`
        assertTrue(applied.isApplied)
    }

    @Test
    fun `audio stream — blocked is checked before failed, and an unverified write is a failure`() {
        // (١٢٠) منعتها المنصّة («لا تزعج»/قفل) ⇒ «محجوب» لا «فشل» عام يُوهم أن الشاشة معطوبة
        val blocked = audioWriteVerdict(
            attempted = true, applied = true, verified = true,
            blocked = true, error = "manual-lock", liveLevel = null,
        )
        assertEquals(AudioWriteOutcome.BLOCKED, blocked.outcome)
        // (١٢١) والحجب ليس تطبيقًا
        assertFalse(blocked.isApplied)
        // (١٢٢) كتبنا ولم تُقرأ المطابقة ⇒ فشل (والاسترجاع يقع خارج هذا الحكم لا داخله)
        assertEquals(
            AudioWriteOutcome.FAILED,
            audioWriteVerdict(
                attempted = true, applied = true, verified = false,
                blocked = false, error = "read-mismatch", liveLevel = 3,
            ).outcome,
        )
        // (١٢٣) ولا «تحقّق» بلا كتابة: الفشل يبقى فشلًا ولو قال المحكِّم «تحقّقت»
        assertEquals(
            AudioWriteOutcome.FAILED,
            audioWriteVerdict(
                attempted = true, applied = false, verified = true,
                blocked = false, error = "no-write", liveLevel = null,
            ).outcome,
        )
        // (١٢٤) والقراءة الغائبة تبقى غائبة لا صفرًا
        assertNull(
            audioWriteVerdict(
                attempted = true, applied = false, verified = false,
                blocked = false, error = "e", liveLevel = null,
            ).liveLevel,
        )
    }

    @Test
    fun `audio knob — the same ladder for knobs, with expected and read values carried for display`() {
        // (١٢٥) مؤثّر غير مُرفَق ⇒ لم تُحاول، وسببها يصل كما هو
        val notTried = audioKnobVerdict(
            attempted = false, blocked = false, applied = false, verified = false,
            expected = "70", actual = null, error = "effect-not-attached",
        )
        assertEquals(AudioWriteOutcome.NOT_ATTEMPTED, notTried.outcome)
        assertEquals("effect-not-attached", notTried.reason)
        // (١٢٦) والقيمة المتوقّعة تُحفظ للحكم (وما لم يُقرأ يبقى `null`)
        assertEquals("70", notTried.expected)
        assertNull(notTried.actual)
        // (١٢٧) قفل يدويّ ⇒ «محجوب» قبل الفشل
        assertEquals(
            AudioWriteOutcome.BLOCKED,
            audioKnobVerdict(
                attempted = true, blocked = true, applied = false, verified = false,
                expected = "70", actual = "40", error = "manual-lock",
            ).outcome,
        )
        // (١٢٨) كُتب وقُرئ ⇒ طُبِّق، وسبب قديم لا يُلحق بنجاح
        val applied = audioKnobVerdict(
            attempted = true, blocked = false, applied = true, verified = true,
            expected = "70", actual = "70", error = "stale-error",
        )
        assertEquals(AudioWriteOutcome.APPLIED, applied.outcome)
        assertNull("سبب قديم لا يُلحق بنجاح", applied.reason)
        // (١٢٩) وكُتب ولم تُقرأ المطابقة ⇒ فشل
        assertEquals(
            AudioWriteOutcome.FAILED,
            audioKnobVerdict(
                attempted = true, blocked = false, applied = true, verified = false,
                expected = "70", actual = "40", error = "read-mismatch",
            ).outcome,
        )
        // (١٣٠) وحكم «لم تُجرَّب» بسبب صريح حين لا يصل النداء للمحكِّم أصلًا
        val reason = audioKnobNotAttempted("identifier-out-of-range")
        assertEquals(AudioWriteOutcome.NOT_ATTEMPTED, reason.outcome)
        assertEquals("identifier-out-of-range", reason.reason)
    }

    @Test
    fun `audio system layer — the earliest unmet condition is the one shown, never a later one`() {
        // (١٣١) بلا جذر: لا يُقال «مسار الوحدة غير قابل للكتابة» — وهو أدنى في السلّم
        val noRoot = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = false, sourcePath = "/x", sourceParsed = true,
                additionReason = null, overlayReady = true, modulePathWritable = false,
            ),
        )
        assertEquals(AudioSystemLayerStatus.NEEDS_ROOT, noRoot.status)
        assertEquals(AudioSystemReason.ROOT_REQUIRED, noRoot.reason)
        // (١٣٢) ولا إعادة تشغيل في حالة لم يُكتب فيها شيء
        assertFalse(noRoot.requiresReboot)
        // (١٣٣) جذر بلا ملفّ تهيئة ⇒ مصدر غير متاح (لا «غير قابل للتحليل»)
        val noSource = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = null, sourceParsed = false,
                additionReason = null, overlayReady = false, modulePathWritable = false,
            ),
        )
        assertEquals(AudioSystemLayerStatus.SOURCE_UNAVAILABLE, noSource.status)
        assertEquals(AudioSystemReason.NO_EFFECTS_FILE, noSource.reason)
        // (١٣٤) وملفّ موجود لم يُحلَّل ⇒ «غير مقروء» لا نفي (ADR-07)
        val unreadable = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = "/x", sourceParsed = false,
                additionReason = null, overlayReady = true, modulePathWritable = true,
            ),
        )
        assertEquals(AudioSystemLayerStatus.SOURCE_UNREADABLE, unreadable.status)
        assertEquals(AudioSystemReason.SOURCE_UNPARSABLE, unreadable.reason)
        // (١٣٥) وسبب رفض الإضافة يُنقل بحرفه بدل استبداله برمز عام
        val refused = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = "/x", sourceParsed = true,
                additionReason = "duplicate-effect-uuid", overlayReady = true, modulePathWritable = true,
            ),
        )
        assertEquals(AudioSystemLayerStatus.ADDITION_REFUSED, refused.status)
        assertEquals("duplicate-effect-uuid", refused.reason)
        // (١٣٦) ولا إضافة مُقدَّمة ⇒ رفض عام بسبب صريح
        val noAddition = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = "/x", sourceParsed = true,
                additionReason = null, overlayReady = false, modulePathWritable = true,
            ),
        )
        assertEquals(AudioSystemLayerStatus.ADDITION_REFUSED, noAddition.status)
        assertEquals(AudioSystemReason.NO_ADDITION, noAddition.reason)
        // (١٣٧) ومسار الوحدة غير قابل للكتابة ⇒ حالته الخاصة لا «رفض الإضافة»
        val unwritable = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = "/x", sourceParsed = true,
                additionReason = null, overlayReady = true, modulePathWritable = false,
            ),
        )
        assertEquals(AudioSystemLayerStatus.MODULE_UNWRITABLE, unwritable.status)
        assertEquals(AudioSystemReason.MODULE_PATH_UNWRITABLE, unwritable.reason)
        // (١٣٨) وكل شرط تحقّق ⇒ جاهز، **بإعادة تشغيل معلنة** لا مخفيّة
        val ready = audioSystemLayerVerdict(
            AudioSystemEvidence(
                rootAvailable = true, sourcePath = "/x", sourceParsed = true,
                additionReason = null, overlayReady = true, modulePathWritable = true,
            ),
        )
        assertEquals(AudioSystemLayerStatus.READY, ready.status)
        assertTrue(ready.isReady)
        assertTrue("الطبقة تُركَّب ولا تعمل حتى يُعاد التشغيل", ready.requiresReboot)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // المحور ٩ — حدود معلنة: ما لا يُقاس بلا جهاز (لا حالات تُنفَّذ هنا)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `device-only axes are named here and nowhere claimed as covered`() {
        // (١٣٩) هذه الحالات لا تُحاكى: تُعلن كي لا يُظنّ أن المصفوفة غطّتها.
        val deviceOnlyAxes = listOf(
            "RTL الفعليّ واتجاه الشريط والرسوم",
            "أحجام الخطوط وأحجام الشاشات الصغيرة/الكبيرة",
            "تبديل مخرج الصوت وفصل السماعة أثناء التشغيل",
            "انقطاع الإنترنت أو الخادم أو المهلة أثناء عمليّة كتابة",
            "توقّف الخدمة الخلفيّة وإعادة تشغيلها واستعادة الجذر بعد فقدانه",
            "إعادة التشغيل · تحديث التطبيق · إعادة التثبيت · تغيير الجهاز",
            "اختلاف إصدارات Android ووحدات الشركات المصنّعة",
            "الطاقة والحرارة والملفات النشطة، والتعارض بين Max AI والملفات اليدويّة",
            "الأداء تحت الحمل والذاكرة المحدودة وقتل العمليّات",
            "الأمان: SELinux والإقلاع والعبث بالملفّات والجذر المزيّف",
        )
        assertEquals(
            "المحاور التي تحتاج جهازًا معدودة بأسمائها لا مُدّعاة",
            10,
            deviceOnlyAxes.distinct().size,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // أدوات بناء السيناريو
    // ─────────────────────────────────────────────────────────────────────────

    /** سجلّ ترخيص بحدوده الدنيا المقبولة (شهادة غير فارغة) — والأرقام تُمرَّر لما يُقاس. */
    private fun record(
        id: String = "LIC-1",
        plan: String = "basic",
        expiresAt: Long = 10_000L,
        serverMillis: Long = 0L,
        payload: String = "signed-payload",
        deviceLimit: Int = 1,
        devicesUsed: Int = 1,
        pausedMillis: Long = 0L,
    ): LicenseRecord = LicenseRecord(
        licenseId = id,
        plan = plan,
        serverMillis = serverMillis,
        expiresAtMillis = expiresAt,
        deviceLimit = deviceLimit,
        devicesUsed = devicesUsed,
        pausedMillis = pausedMillis,
        certificate = LicenseEnvelope(payload = payload),
    )

    /**
     * مدخلات خريطة القدرات — **كل ما لم يُذكر يبقى «لم يُثبت»**، لأن `false` في هذا العقد
     * تعني «لم يُثبت» لا «أُثبت الغياب». وهذا هو الفرق الذي تحرسه حالات المحور الخامس.
     */
    private fun inputs(
        target: AtlasControlTarget = AtlasControlTarget.CPU_FREQUENCY,
        safety: AtlasSafetyVerdict = AtlasSafetyVerdict.Allowed,
        measured: Boolean = false,
        readable: Boolean = false,
        absenceProved: Boolean = false,
        routeKnown: Boolean = false,
        routeStatus: AtlasRouteStatus? = null,
        routeReason: AtlasRouteReason? = null,
        verifiedThisGeneration: Boolean = false,
        gapReason: String? = null,
        writeExpected: Boolean = true,
    ): AtlasCapabilityInputs = AtlasCapabilityInputs(
        target = target,
        safety = safety,
        measured = measured,
        readable = readable,
        absenceProved = absenceProved,
        routeKnown = routeKnown,
        routeStatus = routeStatus,
        routeReason = routeReason,
        verifiedThisGeneration = verifiedThisGeneration,
        gapReason = gapReason,
        writeExpected = writeExpected,
    )
}
