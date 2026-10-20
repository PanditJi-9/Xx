/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import nd.max.ui.overlay.OverlayGeometry
import org.junit.Test

/**
 * الجزيرة الديناميكية — **الهندسة والأصل الأسود**، مقيسان على JVM بلا أندرويد وبلا نافذة.
 *
 * والعقود هنا هي التي تحرس طلب المالك نفسه: «تحريكه بأعلى وأسفل أو يمين ويسار … وتعريضه أو
 * تصغيره، تطويله أو تقصيره» — وحدود الحركة والتوسيع **أرقام**، فتُقاس هنا قبل أن تُرى على جهاز:
 *
 * 1. **الحبس** — قيمة محفوظة قديمة أو معدَّلة يدويًّا لا تدفع الكبسولة خارج مداها.
 * 2. **الأرضية والسقف** — العرض لا ينكمش إلى ما دون أرضية ولا يبلغ حافة الشاشة، والمضاعف
 *    يحرّكهما معًا (وإلا صار اسم «أضيق/أعرض» كذبةً في أحد طرفيه).
 * 3. **الإزاحة الأفقية تسكن بما يسمح به عرض الكبسولة** — كبسولة واسعة عند أقصى اليمين تخرج
 *    حافّتها لو حُبست بحدٍّ ثابت.
 * 4. **المسرح بمعنى واحد مع أقصى إزاحة رأسية** — ثابتان يفترقان يعنيان معاينة تكذب عند حافّتها.
 * 5. **الأسود هو الأصل** — والنقل عند الترقية يمسّه وحده: لونٌ اختاره المستخدم لا يُنقل.
 */
class IslandGeometryTest {

    // ── ١. الحبس ────────────────────────────────────────────────────────────

    @Test
    fun `scales and offsets clamp to their declared range`() {
        assertEquals(IslandGeometry.WIDTH_SCALE_MIN, IslandGeometry.clampWidthScale(0.1f), 0f)
        assertEquals(IslandGeometry.WIDTH_SCALE_MAX, IslandGeometry.clampWidthScale(9f), 0f)
        assertEquals(1f, IslandGeometry.clampWidthScale(1f), 0f)

        assertEquals(IslandGeometry.HEIGHT_SCALE_MIN, IslandGeometry.clampHeightScale(0f), 0f)
        assertEquals(IslandGeometry.HEIGHT_SCALE_MAX, IslandGeometry.clampHeightScale(4f), 0f)

        assertEquals(-IslandGeometry.OFFSET_X_LIMIT, IslandGeometry.clampOffsetX(-9f), 0f)
        assertEquals(IslandGeometry.OFFSET_X_LIMIT, IslandGeometry.clampOffsetX(9f), 0f)
        assertEquals(
            "والرأسي نزولٌ وحده: الجزيرة شريط حالة، ورفعها فوق الصفر يخرج نصفها من الشاشة",
            0f,
            IslandGeometry.clampOffsetY(-0.5f),
            0f
        )
        assertEquals(IslandGeometry.OFFSET_Y_LIMIT, IslandGeometry.clampOffsetY(9f), 0f)
    }

    @Test
    fun `vertical padding scale is the height multiplier under the same clamp`() {
        assertEquals(1f, IslandGeometry.verticalPaddingScale(1f), 0f)
        assertEquals(
            IslandGeometry.HEIGHT_SCALE_MAX,
            IslandGeometry.verticalPaddingScale(3f),
            0f
        )
    }

    // ── ٢. الأرضية والسقف ───────────────────────────────────────────────────

    @Test
    fun `width floor and ceiling follow the multiplier, and the ceiling never reaches the edge`() {
        assertEquals(0.34f, IslandGeometry.minWidthFraction(1f), 0.0001f)
        assertEquals(0.72f, IslandGeometry.maxWidthFraction(1f), 0.0001f)

        // أعرض ما تكون: السقف المطلق يحبس الضرب لا يتركه يبلغ الحافة.
        assertEquals(
            IslandGeometry.MAX_WIDTH_FRACTION_CAP,
            IslandGeometry.maxWidthFraction(IslandGeometry.WIDTH_SCALE_MAX),
            0.0001f
        )
        // وأصغر مضاعف يقلّص الأرضية والسقف معًا بنسبتهما.
        assertEquals(0.34f * 0.70f, IslandGeometry.minWidthFraction(0.70f), 0.0001f)
        assertEquals(0.72f * 0.70f, IslandGeometry.maxWidthFraction(0.70f), 0.0001f)
    }

    @Test
    fun `the floor never overtakes the ceiling across the whole multiplier range`() {
        var scale = IslandGeometry.WIDTH_SCALE_MIN
        while (scale <= IslandGeometry.WIDTH_SCALE_MAX) {
            val floor = IslandGeometry.minWidthFraction(scale)
            val ceiling = IslandGeometry.maxWidthFraction(scale)
            assertTrue("أرضية فوق سقف عند $scale: $floor > $ceiling", floor <= ceiling)
            assertTrue("وسقف فوق حافة الشاشة عند $scale", ceiling <= IslandGeometry.MAX_WIDTH_FRACTION_CAP)
            scale += 0.01f
        }
    }

    // ── ٣. الإزاحة الأفقية ومساحة الكبسولة ──────────────────────────────────

    @Test
    fun `horizontal shift stops where the island width says it must`() {
        // شاشة ١٠٠٠ وكبسولة ٤٠٠: لكل جهة ٣٠٠ لا أكثر.
        assertEquals(300, IslandGeometry.clampOffsetXPx(500, 1000, 400))
        assertEquals(-300, IslandGeometry.clampOffsetXPx(-500, 1000, 400))
        // وكبسولة أعرض من الشاشة لا مساحة لها: صفر، لا رقم سالب يُقلب اتجاهًا.
        assertEquals(0, IslandGeometry.clampOffsetXPx(200, 1000, 1400))
        // وما دون المساحة يمرّ كما هو.
        assertEquals(120, IslandGeometry.clampOffsetXPx(120, 1000, 400))
    }

    @Test
    fun `the fraction form of the clamp agrees with the pixel form`() {
        // ٠٫٣٥ من ١٠٠٠ = ٣٥٠، والمساحة ٣٠٠ ⇒ ٠٫٣٠.
        assertEquals(0.30f, IslandGeometry.clampOffsetXF(0.35f, 1000, 400), 0.0001f)
        assertEquals(-0.30f, IslandGeometry.clampOffsetXF(-0.35f, 1000, 400), 0.0001f)
        // عرض صفريّ (قبل أوّل قياس) يسقط إلى الحبس بحده المعلن لا إلى صفر.
        assertEquals(0.35f, IslandGeometry.clampOffsetXF(0.35f, 0, 0), 0.0001f)
    }

    @Test
    fun `offsets turn into pixels by their share of the screen`() {
        assertEquals(350, IslandGeometry.offsetXPx(0.35f, 1000))
        assertEquals(600, IslandGeometry.offsetYPx(0.30f, 2000))
        // والمخزّن يُحبس قبل الضرب: قيمة قديمة خارج المدى لا تُترجم إلى بكسل خارج الشاشة.
        assertEquals(350, IslandGeometry.offsetXPx(4f, 1000))
        assertEquals(0, IslandGeometry.offsetYPx(-4f, 2000))
    }

    // ── ٤. المسرح ───────────────────────────────────────────────────────────

    @Test
    fun `the stage represents the same share of the screen as the deepest shift`() {
        assertEquals(
            "ثابتان بمعنى واحد: ما يُسحب إلى آخر المسرح هو آخر ما يستطيع الحفظ بلوغه",
            IslandGeometry.PREVIEW_TOP_FRACTION,
            IslandGeometry.OFFSET_Y_LIMIT,
            0f
        )
        assertEquals(0f, IslandGeometry.previewYFraction(0f), 0f)
        assertEquals(0.5f, IslandGeometry.previewYFraction(IslandGeometry.OFFSET_Y_LIMIT / 2f), 0.0001f)
        assertEquals(1f, IslandGeometry.previewYFraction(IslandGeometry.OFFSET_Y_LIMIT), 0f)
        assertEquals("وما فوق المدى يُحبس عند آخر المسرح", 1f, IslandGeometry.previewYFraction(9f), 0f)
    }

    @Test
    fun `the stage aspect follows the device, and a broken size falls back to square`() {
        // هاتف ٣٦٠×٨٠٠: المسرح أعلى ٣٠٪ = ٢٤٠ ⇒ النسبة ١٫٥.
        assertEquals(1.5f, IslandGeometry.previewAspect(360, 800), 0.0001f)
        // لوح ٨٠٠×١٢٨٠: ٣٨٤ ارتفاعًا ⇒ ٢٫٠٨ — أي أن الإطار يتبع الجهاز لا نسبة محفوظة.
        assertEquals(2.083f, IslandGeometry.previewAspect(800, 1280), 0.001f)
        assertEquals(1f, IslandGeometry.previewAspect(0, 800), 0f)
        assertEquals(1f, IslandGeometry.previewAspect(360, 0), 0f)
    }

    // ── ٥. الأسود هو الأصل ──────────────────────────────────────────────────

    @Test
    fun `the island is black by default`() {
        assertEquals("#000000", IslandPrefs.DEFAULT_COLOR_HEX)
        assertEquals("#000000", defaults.colorHex)
    }

    @Test
    fun `upgrade moves the old default to black and leaves a chosen colour alone`() {
        assertEquals(
            "ما يساوي الافتراضيّ القديم يُنقل: من لم يلمس اللون يرى ما أمر به المالك",
            IslandPrefs.DEFAULT_COLOR_HEX,
            IslandPrefs.migrateColorOnUpgrade("#00E5FF")
        )
        assertEquals(
            "بلا حساسية لحالة الأحرف — المفتاح مخزّن نصًّا",
            IslandPrefs.DEFAULT_COLOR_HEX,
            IslandPrefs.migrateColorOnUpgrade("#00e5ff")
        )
        assertEquals(
            "ومن اختار لونًا بنفسه لا يُمسّ اختياره",
            "#FF1744",
            IslandPrefs.migrateColorOnUpgrade("#FF1744")
        )
        assertNull(
            "و«لا لون محفوظ» يعني الافتراضيّ الجديد، لا قيمة تُكتب",
            IslandPrefs.migrateColorOnUpgrade(null)
        )
    }

    // ── أسفل الكاميرا: الأساس الرأسي، والقصّة الأقرب إلى الوسط ──────────────

    @Test
    fun `the anchor is the top edge, the status bar bottom, or the camera bottom`() {
        assertEquals(0, IslandGeometry.anchorYPx(IslandPrefs.Position.Top, 96, 110))
        assertEquals(96, IslandGeometry.anchorYPx(IslandPrefs.Position.BelowStatus, 96, 110))
        assertEquals(110, IslandGeometry.anchorYPx(IslandPrefs.Position.UnderCamera, 96, 110))
    }

    @Test
    fun `under the camera falls back to the status bar bottom when no camera is found`() {
        assertEquals(96, IslandGeometry.anchorYPx(IslandPrefs.Position.UnderCamera, 96, null))
    }

    @Test
    fun `the camera is the top cutout nearest the centre, and its bottom is the anchor`() {
        val centred = OverlayGeometry.Cutout(480, 0, 600, 90)
        val nearRightEdge = OverlayGeometry.Cutout(900, 0, 960, 80)
        assertEquals(centred, IslandGeometry.cameraCutout(listOf(nearRightEdge, centred), 1080, 2400))
        assertEquals(90, IslandGeometry.cameraBottomPx(listOf(centred), 1080, 2400))
    }

    @Test
    fun `a cutout in the lower half or at the side of the screen is not a camera`() {
        val lower = OverlayGeometry.Cutout(480, 1500, 600, 1600)
        val side = OverlayGeometry.Cutout(0, 10, 40, 60)
        assertNull(IslandGeometry.cameraCutout(listOf(lower, side), 1080, 2400))
    }

    @Test
    fun `the preview maps a share of the screen onto its top third`() {
        org.junit.Assert.assertEquals(0.1, IslandGeometry.previewYFractionOf(0.03f).toDouble(), 0.001)
        org.junit.Assert.assertEquals(1.0, IslandGeometry.previewYFractionOf(0.9f).toDouble(), 0.001)
    }

    private companion object {
        /** الافتراضيّات المخزّنة نفسها — تُقرأ من النموذج لا من الشاشة. */
        val defaults = IslandPrefs.State()
    }
}
