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

package nd.max.widget.core

import android.content.Context
import androidx.annotation.StringRes
import nd.max.R

/**
 * ودجات MaxManager — نموذج الإعداد وقراءته وكتابته.
 *
 * ثلاثة أنواع، ولكل نوع مخزون خيارات مستقلّ ([PulseStat] للنبض، [ControlButton]
 * للتحكّم، ومفتاحا العرض للتخزين)، ومظهر مشترك ([WidgetThemeMode] + [WidgetAccent]).
 *
 * **والفصل مقصود:** [WidgetConfigCodec] نقيّ (لا `Context` ولا `SharedPreferences`)
 * فيُختبر في JVM عادي — والاختبار يقيس الافتراضيات والتحقّق من القيم الفاسدة، وهو
 * ما لا يُقاس على جهاز في هذه البيئة. و[WidgetConfigStore] طبقة رقيقة فوق
 * `SharedPreferences` لا منطق فيها.
 */
enum class WidgetKind(val key: String) {
    Pulse("pulse"),
    Controls("controls"),
    Storage("storage"),
}

/** نمط المظهر: يتبع النظام افتراضيًّا، أو يُفرض فاتحًا/داكنًا. */
enum class WidgetThemeMode(val key: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun from(key: String?): WidgetThemeMode =
            entries.firstOrNull { it.key == key } ?: System
    }
}

/** لون التمييز داخل الودجة — من عائلة ألوان MaxManager، لا من شكل تطبيق آخر. */
enum class WidgetAccent(val key: String) {
    Brand("brand"),
    Teal("teal"),
    Violet("violet"),
    Amber("amber"),
    Rose("rose"),
    Green("green");

    companion object {
        fun from(key: String?): WidgetAccent =
            entries.firstOrNull { it.key == key } ?: Brand
    }
}

/** مقاييس ودجة النبض — يختار المستخدم أيّها تظهر. */
enum class PulseStat(val key: String, @StringRes val labelRes: Int) {
    Battery("battery", R.string.widget_stat_battery),
    Temp("temp", R.string.widget_stat_temp),
    Cpu("cpu", R.string.widget_stat_cpu),
    Ram("ram", R.string.widget_stat_ram);

    companion object {
        /** الترتيب هنا هو ترتيب العرض، والمجموعة ليست مجموعة حالة. */
        val Ordered = listOf(Battery, Temp, Cpu, Ram)
        val Default: Set<PulseStat> = Ordered.toSet()

        fun from(keys: Collection<String>): Set<PulseStat> {
            val picked = Ordered.filter { it.key in keys }.toSet()
            return picked.ifEmpty { Default }
        }
    }
}

/** أزرار ودجة التحكّم — حتى [MAX_SELECTED] منها، بترتيب العرض هذا. */
enum class ControlButton(val key: String, @StringRes val labelRes: Int) {
    Boost("boost", R.string.widget_action_boost),
    Island("island", R.string.widget_action_island),
    OpenStorage("open_storage", R.string.widget_action_open_storage),
    OpenControl("open_control", R.string.widget_action_open_control),
    OpenStudio("open_studio", R.string.widget_action_open_studio),
    Refresh("refresh", R.string.widget_action_refresh);

    companion object {
        val Ordered = listOf(Boost, Island, OpenStorage, OpenControl, OpenStudio, Refresh)
        val Default: List<ControlButton> = listOf(Boost, Island, OpenStorage)
        const val MAX_SELECTED = 4

        fun from(keys: Collection<String>): List<ControlButton> {
            val picked = Ordered.filter { it.key in keys }.take(MAX_SELECTED)
            return picked.ifEmpty { Default }
        }
    }
}

/** إعداد ودجة واحدة — كل الحقول لها افتراضيات معقولة. */
data class WidgetConfig(
    val mode: WidgetThemeMode = WidgetThemeMode.System,
    val accent: WidgetAccent = WidgetAccent.Brand,
    val stats: Set<PulseStat> = PulseStat.Default,
    val buttons: List<ControlButton> = ControlButton.Default,
    val showStoragePercent: Boolean = true,
    val showStorageBar: Boolean = true,
) {
    companion object {
        /** افتراضيّ يختلف باختلاف النوع — تلزمه الودجة إن غاب الإعداد. */
        fun defaults(kind: WidgetKind): WidgetConfig = when (kind) {
            WidgetKind.Pulse -> WidgetConfig(stats = PulseStat.Default)
            WidgetKind.Controls -> WidgetConfig(buttons = ControlButton.Default)
            WidgetKind.Storage -> WidgetConfig()
        }
    }
}

/**
 * ترجمة الإعداد إلى خريطة نصّية (وكل مفاتيحه بادئة `w_`) — والعكس بمَنطق
 * «القيمة غير الصالحة تسقط إلى الافتراضي» لا «تُرمى استثناءً»: ودجة أُنشئت في
 * نسخة أقدم أو ملف إعداد تلف لا يجوز أن يُسقطا الودجة عن الشاشة.
 */
object WidgetConfigCodec {

    const val KEY_MODE = "w_mode"
    const val KEY_ACCENT = "w_accent"
    const val KEY_STATS = "w_stats"
    const val KEY_BUTTONS = "w_buttons"
    const val KEY_SHOW_PERCENT = "w_show_percent"
    const val KEY_SHOW_BAR = "w_show_bar"

    fun encode(config: WidgetConfig): Map<String, String> = mapOf(
        KEY_MODE to config.mode.key,
        KEY_ACCENT to config.accent.key,
        KEY_STATS to PulseStat.Ordered.filter { it in config.stats }.joinToString(",") { it.key },
        KEY_BUTTONS to ControlButton.Ordered.filter { it in config.buttons }.joinToString(",") { it.key },
        KEY_SHOW_PERCENT to config.showStoragePercent.toString(),
        KEY_SHOW_BAR to config.showStorageBar.toString(),
    )

    fun decode(source: Map<String, *>, fallback: WidgetConfig): WidgetConfig {
        fun text(key: String): String? = source[key] as? String
        fun flag(key: String): Boolean? = (source[key] as? String)?.toBooleanStrictOrNull()

        return WidgetConfig(
            mode = text(KEY_MODE)?.let { WidgetThemeMode.from(it) } ?: fallback.mode,
            accent = text(KEY_ACCENT)?.let { WidgetAccent.from(it) } ?: fallback.accent,
            stats = text(KEY_STATS)?.split(",")?.let { PulseStat.from(it) } ?: fallback.stats,
            buttons = text(KEY_BUTTONS)?.split(",")?.let { ControlButton.from(it) } ?: fallback.buttons,
            showStoragePercent = flag(KEY_SHOW_PERCENT) ?: fallback.showStoragePercent,
            showStorageBar = flag(KEY_SHOW_BAR) ?: fallback.showStorageBar,
        )
    }
}

/** آخر نتيجة «تحرير ذاكرة» — تُعرض حتى الضغطة التالية. */
data class LastBoost(val atMs: Long, val freedMb: Int?, val executed: Boolean)

/**
 * مخزن إعداد كل ودجة — ملفّ واحد `max_widgets` ومفاتيح مُنَطّقة برقم الودجة،
 * فلا تتخاصم ودجتان على مفتاح واحد. ولا يُقرأ منه شيء خارج هذه الحزمة.
 */
class WidgetConfigStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun load(id: Int, kind: WidgetKind): WidgetConfig {
        val prefix = "$PREFIX$id."
        val source = prefs.all
            .filterKeys { it.startsWith(prefix) }
            .mapKeys { (key, _) -> key.removePrefix(prefix) }
        return WidgetConfigCodec.decode(source, WidgetConfig.defaults(kind))
    }

    fun save(id: Int, config: WidgetConfig) {
        val editor = prefs.edit()
        WidgetConfigCodec.encode(config).forEach { (key, value) ->
            editor.putString("$PREFIX$id.$key", value)
        }
        editor.apply()
    }

    fun clear(id: Int) {
        val prefix = "$PREFIX$id."
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }

    fun lastBoost(id: Int): LastBoost? {
        val at = prefs.getLong("$PREFIX$id.$KEY_LAST_BOOST_AT", -1L)
        if (at < 0L) return null
        return LastBoost(
            atMs = at,
            freedMb = prefs.getInt("$PREFIX$id.$KEY_LAST_BOOST_MB", -1).takeIf { it >= 0 },
            executed = prefs.getBoolean("$PREFIX$id.$KEY_LAST_BOOST_OK", false),
        )
    }

    fun saveLastBoost(id: Int, result: LastBoost) {
        prefs.edit()
            .putLong("$PREFIX$id.$KEY_LAST_BOOST_AT", result.atMs)
            .putInt("$PREFIX$id.$KEY_LAST_BOOST_MB", result.freedMb ?: -1)
            .putBoolean("$PREFIX$id.$KEY_LAST_BOOST_OK", result.executed)
            .apply()
    }

    companion object {
        const val PREFS_FILE = "max_widgets"
        private const val PREFIX = "w_"
        private const val KEY_LAST_BOOST_AT = "last_boost_at"
        private const val KEY_LAST_BOOST_MB = "last_boost_mb"
        private const val KEY_LAST_BOOST_OK = "last_boost_ok"

        /** رقم الودجة من `appWidgetId` غير صالح — الحرس المشترك للنشاط والناقلات. */
        const val INVALID_ID = -1
    }
}
