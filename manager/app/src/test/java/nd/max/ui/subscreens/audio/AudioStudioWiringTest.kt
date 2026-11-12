/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.subscreens.audio

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * **حارس الباب — لا حارس الشكل.**
 *
 * العطب الذي وُلد هذا الملفّ لأجله مقيس: `AudioSoundPresetsSection` (٢٨٠ سطرًا) و`AudioPresetController`
 * و`applySoundPreset`/`compareSound`/`diagnoseSound` كانت كلّها مُصرَّفة في الشجرة، ولا نداء واحد لها من
 * الشاشة — فقدرةٌ كاملة بلا باب. والبناء يمرّ، والاختبارات تمرّ، والمستخدم لا يرى شيئًا.
 *
 * فلا يكفي أن تُوجد الدالّة: يجب أن تُنادى من الشاشة، وثلاثتها. وهذا ما يُقاس هنا على نصّ الشاشة، لأنّ
 * الحارس النصّيّ هو الوسيلة الوحيدة المتاحة بلا جهاز.
 */
class AudioStudioWiringTest {

    @Test
    fun `the sound presets section is mounted on the studio screen`() {
        val source = screenSource() ?: return
        assertTrue(
            "AudioSoundPresetsSection must be called from the studio screen, not only declared",
            source.contains("AudioSoundPresetsSection("),
        )
    }

    @Test
    fun `every preset action the section offers is wired to the view model`() {
        val source = screenSource() ?: return
        listOf("applySoundPreset(", "compareSound(", "diagnoseSound(").forEach { call ->
            assertTrue(
                "the preset section offers an action that is not wired: $call",
                source.contains(call),
            )
        }
    }

    @Test
    fun `the presets are offered before the manual knobs`() {
        val source = screenSource() ?: return
        val presets = source.indexOf("AudioSoundPresetsSection(")
        val knobs = source.indexOf("CyberCurveEqualizer(")
        assertTrue("both the presets and the equalizer must be on the screen", presets >= 0 && knobs >= 0)
        assertTrue("one tap to a sound profile comes before manual tuning", presets < knobs)
    }

    @Test
    fun `the preset chips write through the arbiter like the preset section does`() {
        val source = screenSource() ?: return
        val start = source.indexOf("onPresetSelect = {")
        assertTrue("the equalizer must still offer preset chips", start >= 0)
        val block = source.substring(start, minOf(source.length, start + 700))
        assertTrue(
            "the chips must go through the view model, not AudioEffectService directly",
            block.contains("applySoundPreset("),
        )
        assertTrue(
            "a second write path for the same action is a duplicate owner (ADR-11)",
            !block.contains("AudioEffectService.setPreset("),
        )
    }

    /** يقرأ ملف الشاشة من شجرة المصادر، ويُعلن التخطّي حين لا تُوجد (تشغيل من حزمة APK). */
    private fun screenSource(): String? {
        val file = File("src/main/java/nd/max/ui/subscreens/audio/AudioStudioScreen.kt")
        assumeTrue("screen source not available in this runtime", file.isFile)
        return file.readText()
    }
}
