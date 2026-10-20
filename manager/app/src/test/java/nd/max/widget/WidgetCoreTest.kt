/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.widget

import java.util.Locale
import nd.max.widget.core.ControlButton
import nd.max.widget.core.PulseStat
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetConfig
import nd.max.widget.core.WidgetConfigCodec
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetKind
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.core.WidgetTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يقيس نواة الودجات — **البِتّات التي لا يراها الجهاز**: ترجمة الإعداد وافتراضياته،
 * وحدود النبرة، والتنسيقات. وما لا يُقاس محليًّا (رسم المشغّل، لمسة، RTL حقيقيّ)
 * يبقى معلَنًا أنه يحتاج جهازًا ولا يُدّعى هنا.
 */
class WidgetCoreTest {

    private val us = Locale.US

    // ===== ترجمة الإعداد =====

    @Test
    fun `codec roundtrip keeps every field`() {
        val original = WidgetConfig(
            mode = WidgetThemeMode.Dark,
            accent = WidgetAccent.Rose,
            stats = setOf(PulseStat.Battery, PulseStat.Ram),
            // بترتيب العرض المعتمد: الترميز يوحّده عند الحفظ، فالاختبار يقيس الثبات لا إعادة الترتيب.
            buttons = listOf(ControlButton.Boost, ControlButton.Island),
            showStoragePercent = false,
            showStorageBar = true,
        )
        val decoded = WidgetConfigCodec.decode(
            WidgetConfigCodec.encode(original),
            WidgetConfig.defaults(WidgetKind.Pulse),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `unknown keys and junk values fall back to defaults`() {
        val fallback = WidgetConfig.defaults(WidgetKind.Controls)
        val decoded = WidgetConfigCodec.decode(
            mapOf(
                WidgetConfigCodec.KEY_MODE to "neon",
                WidgetConfigCodec.KEY_ACCENT to "42",
                WidgetConfigCodec.KEY_BUTTONS to "boost,not_a_button",
                WidgetConfigCodec.KEY_SHOW_BAR to "maybe",
            ),
            fallback,
        )
        assertEquals(fallback.mode, decoded.mode)
        assertEquals(fallback.accent, decoded.accent)
        assertEquals(listOf(ControlButton.Boost), decoded.buttons)
        assertEquals(fallback.showStorageBar, decoded.showStorageBar)
    }

    @Test
    fun `empty stats selection falls back to all four`() {
        val decoded = WidgetConfigCodec.decode(
            mapOf(WidgetConfigCodec.KEY_STATS to ""),
            WidgetConfig.defaults(WidgetKind.Pulse),
        )
        assertEquals(PulseStat.Default, decoded.stats)
    }

    @Test
    fun `buttons are capped at four and keep declared order`() {
        val decoded = WidgetConfigCodec.decode(
            mapOf(
                WidgetConfigCodec.KEY_BUTTONS to
                    "refresh,open_studio,open_control,open_storage,island,boost",
            ),
            WidgetConfig.defaults(WidgetKind.Controls),
        )
        assertEquals(ControlButton.MAX_SELECTED, decoded.buttons.size)
        assertEquals(
            listOf(
                ControlButton.Boost,
                ControlButton.Island,
                ControlButton.OpenStorage,
                ControlButton.OpenControl,
            ),
            decoded.buttons,
        )
    }

    // ===== المظهر =====

    @Test
    fun `system mode keeps both day and night accents apart`() {
        val colors = WidgetTheme.accent(WidgetAccent.Teal)
        assertNotEquals(colors.day, colors.night)
        assertEquals(colors.day to colors.night, colors.resolve(WidgetThemeMode.System))
    }

    @Test
    fun `forced modes collapse the pair to one value`() {
        val colors = WidgetTheme.accent(WidgetAccent.Violet)
        assertEquals(colors.day to colors.day, colors.resolve(WidgetThemeMode.Light))
        assertEquals(colors.night to colors.night, colors.resolve(WidgetThemeMode.Dark))

        val palette = WidgetTheme.palette(WidgetAccent.Violet, WidgetThemeMode.Dark)
        assertEquals(palette.accentNight, palette.accentDay)
        assertEquals(palette.backgroundNight, palette.backgroundDay)
        assertEquals(palette.textNight, palette.textDay)
    }

    @Test
    fun `every accent is defined for both modes`() {
        WidgetAccent.entries.forEach { accent ->
            val colors = WidgetTheme.accent(accent)
            assertTrue(colors.day != 0L)
            assertTrue(colors.night != 0L)
            // الفرق يعني أن الزوج مقصود لوضعين، لا لونًا واحدًا مكرَّرًا.
            assertNotEquals(colors.day, colors.night)
        }
    }

    // ===== النبرة =====

    @Test
    fun `battery percent tone thresholds sit at 15 and 30`() {
        assertEquals(WidgetTone.Neutral, WidgetTheme.batteryPercentTone(null))
        assertEquals(WidgetTone.Critical, WidgetTheme.batteryPercentTone(15))
        assertEquals(WidgetTone.Caution, WidgetTheme.batteryPercentTone(16))
        assertEquals(WidgetTone.Caution, WidgetTheme.batteryPercentTone(30))
        assertEquals(WidgetTone.Positive, WidgetTheme.batteryPercentTone(31))
    }

    @Test
    fun `load tone thresholds sit at 75 and 90`() {
        assertEquals(WidgetTone.Neutral, WidgetTheme.loadTone(null))
        assertEquals(WidgetTone.Positive, WidgetTheme.loadTone(74))
        assertEquals(WidgetTone.Caution, WidgetTheme.loadTone(75))
        assertEquals(WidgetTone.Caution, WidgetTheme.loadTone(89))
        assertEquals(WidgetTone.Critical, WidgetTheme.loadTone(90))
    }

    @Test
    fun `battery temperature tone thresholds sit at 38 and 43`() {
        assertEquals(WidgetTone.Positive, WidgetTheme.batteryTone(37.9f))
        assertEquals(WidgetTone.Caution, WidgetTheme.batteryTone(38f))
        assertEquals(WidgetTone.Critical, WidgetTheme.batteryTone(43f))
    }

    // ===== التنسيقات =====

    @Test
    fun `gigabyte formatting keeps one decimal`() {
        assertEquals("1.5", WidgetFormat.gbFromBytes(1_610_612_736L, us))
        assertEquals("2.0", WidgetFormat.gbFromMb(2048, us))
        assertEquals("0.5", WidgetFormat.gbFromMb(512, us))
    }

    @Test
    fun `null readings render dash not zero`() {
        assertEquals(WidgetFormat.DASH, WidgetFormat.percentOf(null))
        assertEquals(WidgetFormat.DASH, WidgetFormat.degreesOf(null, us))
        assertEquals("42%", WidgetFormat.percentOf(42))
        assertEquals("34°", WidgetFormat.degreesOf(34.2f, us))
    }

    @Test
    fun `ram percent never divides by zero`() {
        assertEquals(0, WidgetFormat.ramPercent(100, 0))
        assertEquals(50, WidgetFormat.ramPercent(4096, 8192))
        assertEquals(100, WidgetFormat.ramPercent(9999, 8192))
    }
}
