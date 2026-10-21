/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max

import nd.max.contract.ContractFixtures
import nd.max.core.audio.AudioCapabilityReason
import nd.max.core.audio.AudioFeature
import nd.max.core.audio.AudioFeatureVerdict
import nd.max.core.audio.AudioSupport
import nd.max.core.audio.audioCapabilityRows
import nd.max.core.audio.audioCapabilitySummary
import nd.max.core.hardware.AccessLevel
import nd.max.core.hardware.FeatureCapability
import nd.max.core.hardware.HardwareFeature
import nd.max.core.hardware.capabilityRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **المحور §٤-و — إخفاء الميزات غير المتاحة على الجهاز (١٧ حالة · ١..١٧).**
 *
 * نصّ الخطة (سطر ١٣١): «أضف خيارًا واضحًا باسم قريب من «إخفاء الميزات غير المتاحة على جهازي»،
 * ويكون المستخدم حرًا في تفعيله أو تعطيله»، و(١٣٣): «عند إيقاف الخيار، تظهر الميزات غير المتاحة
 * بتصميم رمادي هادئ مع توضيح السبب وطريقة تفعيلها»، و(١٣٥): «عند تفعيله، تُخفى البطاقات والخيارات
 * غير المدعومة من الأماكن المناسبة دون حذف الوظائف المتاحة أو إخفاء معلومات أساسية».
 *
 * **وهذا المحور كان في `ScenarioMatrixTest`** ثمّ أُخرج إلى ملفّه حين بلغ المصفوفة سقف الألف سطر
 * (بوابة `code_health`): فصلُ محورٍ كاملٍ أنظف من قصّ تعليقاته، والأرقام أُعيدت ١..١٧ هنا.
 *
 * **وحدّ ما يُقاس هنا:** دالّة الطيّ النقيّة ([capabilityRows] و[audioCapabilityRows]) وحياكة
 * المفتاح في المصدر. **وما لا يُقاس بلا جهاز:** الرمادي الفعليّ على الشاشة، وتجاوب الصفوف مع RTL،
 * وأن مفتاح التخصيص يُقلب بلمسة — وهذه تُذكر في «حدود معلنة» بالمصفوفة ولا تُدّعى.
 */
class UnavailableFeaturesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // مصفوفة العتاد — «بلا وصول» تُخفى، و«قراءة» و«كتابة» تبقى
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `unavailable hardware capability is hidden only when the user asked`() {
        val none = FeatureCapability(HardwareFeature.CPU_FREQUENCY, AccessLevel.NONE, "cpufreq")
        val readOnly = FeatureCapability(HardwareFeature.CPU_GOVERNOR, AccessLevel.READ_ONLY, "sysfs")
        val writable = FeatureCapability(HardwareFeature.GPU_FREQUENCY, AccessLevel.READ_WRITE, "kgsl")
        val allRows = listOf(none, readOnly, writable)

        // (١) المفتاح مطفأ (الافتراضيّ) ⇒ صفوف «بلا وصول» تُعرض رماديّةً بسببها في الواجهة
        assertEquals(allRows, capabilityRows(allRows, showUnavailable = true))
        // (٢) المفتاح مفعّل ⇒ «بلا وصول» المقيس يُخفى
        assertEquals(listOf(readOnly, writable), capabilityRows(allRows, showUnavailable = false))
        // (٣) «للقراءة فقط» قدرة قراءة قائمة — تبقى في الحالتين
        assertEquals(
            listOf(readOnly),
            capabilityRows(listOf(readOnly), showUnavailable = false),
        )
        // (٤) «للقراءة والكتابة» — تبقى في الحالتين
        assertEquals(
            listOf(writable),
            capabilityRows(listOf(writable), showUnavailable = false),
        )
        // (٥) الإخفاء لا يُعيد ترتيب الباقي: الباقي بترتيبه الأصليّ
        val mixed = listOf(
            FeatureCapability(HardwareFeature.ZRAM, AccessLevel.NONE, "zram"),
            writable,
            readOnly,
            none,
        )
        assertEquals(listOf(writable, readOnly), capabilityRows(mixed, showUnavailable = false))
        // (٦) العدد المخفيّ = المجموع − المعروض (هو ما يُكتب سطرًا في بطاقة التشخيص)
        assertEquals(2, mixed.size - capabilityRows(mixed, showUnavailable = false).size)
        // (٧) لائحة فارغة تبقى فارغة في الحالتين — لا صفّ يُخترع ولا استثناء
        assertTrue(capabilityRows(emptyList(), showUnavailable = false).isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // مصفوفة الصوت — «غير متاحة» المقيسة تُخفى، و«لم تُقس» تبقى (ADR-07)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `unavailable audio capability hides measured absence, never unmeasured`() {
        val unavailable = AudioFeatureVerdict(
            AudioFeature.EQUALIZER,
            AudioSupport.UNAVAILABLE,
            AudioCapabilityReason.NOT_SUPPORTED,
        )
        val unmeasured = AudioFeatureVerdict(
            AudioFeature.BASS_BOOST,
            AudioSupport.UNKNOWN,
            AudioCapabilityReason.GLOBAL_NOT_MEASURED,
        )
        val adapter = AudioFeatureVerdict(
            AudioFeature.SPECTRUM,
            AudioSupport.NEEDS_ADAPTER,
            AudioCapabilityReason.SESSION_REQUIRED,
        )
        val writable = AudioFeatureVerdict(
            AudioFeature.VOLUME_GROUPS,
            AudioSupport.WRITABLE,
            AudioCapabilityReason.SUPPORTED_BY_DEVICE,
        )
        val readOnly = AudioFeatureVerdict(
            AudioFeature.VIRTUALIZER,
            AudioSupport.READ_ONLY,
            AudioCapabilityReason.NOT_DECLARED,
        )
        val allRows = listOf(unavailable, unmeasured, adapter, writable, readOnly)

        // (٨) المفتاح مطفأ ⇒ كل الصفوف تُعرض (المقيس غير المتاح رماديّ بسببه)
        assertEquals(allRows, audioCapabilityRows(allRows, showUnavailable = true))
        // (٩) المفتاح مفعّل ⇒ «غير متاحة» المقيسة تُخفى وحدها
        assertEquals(
            listOf(unmeasured, adapter, writable, readOnly),
            audioCapabilityRows(allRows, showUnavailable = false),
        )
        // (١٠) «لم تُقس» تبقى ظاهرة: النفي لا يُبنى على غياب قراءة (ADR-07)
        assertTrue(audioCapabilityRows(allRows, showUnavailable = false).contains(unmeasured))
        // (١١) «تحتاج محوّلًا» قدرة حقيقيّة لها طريق — تبقى
        assertTrue(audioCapabilityRows(allRows, showUnavailable = false).contains(adapter))
        // (١٢) والقدرتان القائمتان (قراءة وكتابة) لا تُخفى منهما واحدة
        assertEquals(
            listOf(writable, readOnly),
            audioCapabilityRows(listOf(writable, readOnly), showUnavailable = false),
        )
        // (١٣) والعدد المخفيّ واحد لا صفر ولا ثلاثة
        assertEquals(1, allRows.size - audioCapabilityRows(allRows, showUnavailable = false).size)
        // (١٤) والحصيلة تبقى على القياس الكامل: الإخفاء عرضٌ لا محوٌ — العدّاد ما زال يقول ١
        assertEquals(1, audioCapabilitySummary(allRows).unavailable)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // الحياكة — مفتاح واحد، ونصّ مفتاح واحد، وبوّابة واحدة
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `hide unavailable has one preference, one key and one gate`() {
        // بعد النقل: المفتاح يُكتب من بطاقة الإعدادات، ولا تبقى في استوديو السمة نسخةٌ ثانية منه
        val switch = mainSource("SettingsScreen.kt")
        val studio = mainSource("ThemeStudioSections.kt")
        val settings = mainSource("ThemeSettings.kt")
        // (١٥) المفتاح يُكتب بالمفتاح نفسه الذي تقرؤه البوابة — لا نصّ مفتاح ثانٍ في الشاشة
        assertTrue(switch.contains("ThemeKeys.HIDE_UNAVAILABLE"))
        assertTrue(switch.contains("R.string.str_hide_unavailable_features"))
        assertTrue(settings.contains("const val HIDE_UNAVAILABLE"))
        assertTrue(settings.contains("getBoolean(ThemeKeys.HIDE_UNAVAILABLE, false)"))
        // والاستوديو لم يعد يحمل المفتاح ولا عنوانه: البطاقة نُقلت لا نُسخت
        assertFalse(studio.contains("ThemeKeys.HIDE_UNAVAILABLE"))
        assertFalse(studio.contains("R.string.str_hide_unavailable_features"))
        // (١٦) والافتراضيّ «يُعرض»: لا إخفاء بلا طلب صريح من المستخدم
        assertFalse(settings.contains("getBoolean(ThemeKeys.HIDE_UNAVAILABLE, true)"))
        // (١٧) والباب واحد: مصفوفتا القدرات تمرّان بالبوابة، والبوابة وحدها تقرأ التفضيل
        assertTrue(mainSource("UnavailableFeatures.kt").contains("ThemeStudio.hideUnavailableFeatures"))
        assertTrue(mainSource("CapabilityMatrixCard.kt").contains("rememberUnavailableVisible()"))
        assertTrue(mainSource("AudioCapabilitySection.kt").contains("rememberUnavailableVisible()"))
    }

    /** نصّ ملفّ مصدر رئيسيّ بالاسم — حرسٌ بنيويّ يقرأ ما يُنفَّذ فعلًا، ويُتخطّى إن لم تُوجد الشجرة. */
    private fun mainSource(name: String): String =
        ContractFixtures.mainSourceFiles().first { it.name == name }.readText()
}
