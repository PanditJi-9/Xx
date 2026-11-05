/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import nd.max.receiver.BatterySampleReceiver
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * A single point of the device's battery history.
 *
 * The values are the platform's own reports, not estimates:
 *  - level / voltage / temperature / status / plugged come from the sticky
 *    `ACTION_BATTERY_CHANGED` broadcast;
 *  - current comes from `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`;
 *  - screen state comes from `PowerManager.isInteractive`.
 *
 * **Why the screen state is stored at all:** a drain rate is only meaningful
 * when it says whether the screen was on. A device losing 4%/h with the panel
 * lit and the same 4%/h with it dark are two different machines, and averaging
 * them hides exactly the number the user came for. `null` means the state was
 * not recorded (older samples), not "off".
 *
 * Nothing here needs root, Shizuku or a new runtime permission.
 */
data class BatterySample(
    val atMillis: Long,
    val levelPercent: Int,
    val voltageMv: Int?,
    val temperatureC: Float?,
    val status: Int,
    val plugged: Int,
    val currentUa: Long?,
    val screenOn: Boolean? = null
) {
    /** True while the charger is not supplying power. */
    val discharging: Boolean
        get() = plugged == 0 && status != BatteryManager.BATTERY_STATUS_CHARGING &&
            status != BatteryManager.BATTERY_STATUS_FULL
}

/**
 * App-local battery history.
 *
 * **Why a JSONL file and not a database:** the record is an append-only series
 * of at most [MAX_SAMPLES] points; a database would add a schema, a migration
 * and a dependency for a list we only ever read in time order. One line per
 * sample is also inspectable with `cat` when a user exports a bug report.
 *
 * **Why AlarmManager and not WorkManager:** this build has no WorkManager
 * dependency, and the sampling work is a single sticky-broadcast read plus one
 * line appended. An inexact repeating alarm is the smallest platform primitive
 * that survives process death without pulling a new library.
 *
 * **Why background recording is opt-in:** a performance tool that starts
 * writing a timeline without asking is not trustworthy. [isEnabled] defaults
 * to false; the History screen is the only place that turns it on.
 */
object BatteryHistoryStore {

    const val SAMPLE_INTERVAL_MS = AlarmManager.INTERVAL_FIFTEEN_MINUTES
    const val ACTION_SAMPLE = "nd.max.action.BATTERY_SAMPLE"

    private const val MAX_SAMPLES = 4 * 24 * 35 // 35 days at one sample per 15 min
    private const val FILE_NAME = "battery-history.jsonl"
    private const val EXPORT_PREFIX = "battery-history-"
    private const val CSV_HEADER =
        "timestamp_iso,epoch_ms,level_percent,voltage_mv,temperature_c,status,plugged,current_ua,discharging"
    private const val PREFS_NAME = "battery_history"
    private const val KEY_ENABLED = "enabled"

    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "battery-history").apply { isDaemon = true }
    }

    fun isEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    /**
     * Turns background recording on or off and keeps the alarm in sync.
     *
     * Enabling records one point immediately, so the screen never shows an
     * empty chart until the first fifteen-minute tick.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        if (enabled) {
            ensureScheduled(app)
            recordNow(app)
        } else {
            cancel(app)
        }
    }

    /** Reads the current battery state without writing anything. */
    fun readCurrent(context: Context): BatterySample? {
        val app = context.applicationContext
        val intent = runCatching {
            app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return null

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        val batteryManager = app.getSystemService(BatteryManager::class.java)
        val currentUa = runCatching {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }.getOrNull()?.takeIf { it != Long.MIN_VALUE && it != 0L }

        return BatterySample(
            atMillis = System.currentTimeMillis(),
            levelPercent = (level * 100 / scale).coerceIn(0, 100),
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                .takeIf { it > 0 },
            temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }
                ?.let { it / 10f },
            status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            ),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            currentUa = currentUa,
            // A refused read stays `null`; the drain split then ignores the
            // sample rather than guessing which side it belonged to.
            screenOn = runCatching {
                (app.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
            }.getOrNull()
        )
    }

    /** Appends one point asynchronously. Returns the point that was requested. */
    fun recordNow(context: Context): BatterySample? {
        val sample = readCurrent(context) ?: return null
        val app = context.applicationContext
        io.execute { append(app, sample) }
        return sample
    }

    /** Reads every stored point, oldest first. Call from a background thread. */
    fun read(context: Context): List<BatterySample> {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return runCatching {
            file.useLines { lines ->
                lines.mapNotNull(::parse).toList()
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        io.execute {
            runCatching { File(app.filesDir, FILE_NAME).delete() }
        }
    }

    /**
     * The stored points as CSV, one row per sample, oldest first.
     *
     * **Why the columns are named and ordered here, not in the screen:** the
     * file is meant to be opened in a spreadsheet, so its shape is part of the
     * contract. A screen-local builder would drift the moment a second caller
     * appears. Missing readings are empty fields, never `0` — a voltage that
     * was not reported must not read as a measured zero.
     *
     * Timestamps are ISO-8601 in UTC (`Instant.toString()`) plus the raw epoch
     * milliseconds, so a tool that cannot parse ISO still has a number.
     */
    fun csv(samples: List<BatterySample>): String {
        val out = StringBuilder(64 + samples.size * 64)
        out.append(CSV_HEADER).append('\n')
        samples.forEach { sample ->
            out.append(java.time.Instant.ofEpochMilli(sample.atMillis).toString())
            out.append(',').append(sample.atMillis)
            out.append(',').append(sample.levelPercent)
            out.append(',').append(sample.voltageMv ?: "")
            out.append(',').append(sample.temperatureC ?: "")
            out.append(',').append(sample.status)
            out.append(',').append(sample.plugged)
            out.append(',').append(sample.currentUa ?: "")
            out.append(',').append(if (sample.discharging) '1' else '0')
            out.append('\n')
        }
        return out.toString()
    }

    /**
     * Writes [csv] to a fresh file in the app cache and returns it, or `null`.
     *
     * **Why the cache and not external storage:** the file is handed to one app
     * the user picks through `FileProvider` (already declared under
     * `cache-path`), and it dies with the cache instead of leaving a second
     * copy of the history on shared storage. Earlier exports are deleted first
     * so repeated taps cannot pile up copies of the same timeline.
     */
    fun writeCsv(context: Context, samples: List<BatterySample>): File? {
        val cache = context.applicationContext.cacheDir
        return runCatching {
            cache.listFiles { file -> file.name.startsWith(EXPORT_PREFIX) }
                ?.forEach { old -> runCatching { old.delete() } }
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            val file = File(cache, "$EXPORT_PREFIX$stamp.csv")
            file.writeText(csv(samples))
            file
        }.getOrNull()
    }

    /**
     * Idempotent: the same PendingIntent is updated, so calling this from the
     * screen, the boot receiver and the package-replaced receiver cannot stack
     * three alarms.
     */
    fun ensureScheduled(context: Context) {
        if (!isEnabled(context)) return
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + SAMPLE_INTERVAL_MS,
                SAMPLE_INTERVAL_MS,
                sampleIntent(app)
            )
        }
    }

    private fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarmManager.cancel(sampleIntent(app)) }
    }

    private fun sampleIntent(context: Context): PendingIntent {
        val intent = Intent(context, BatterySampleReceiver::class.java).apply {
            action = ACTION_SAMPLE
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun append(context: Context, sample: BatterySample) {
        val file = File(context.filesDir, FILE_NAME)
        runCatching {
            file.appendText(sample.toJson().toString() + "\n")
            pruneIfNeeded(file)
        }
    }

    private fun pruneIfNeeded(file: File) {
        if (file.length() < 256_000L) return
        val lines = file.useLines { it.toList() }
        if (lines.size <= MAX_SAMPLES) return
        file.writeText(lines.takeLast(MAX_SAMPLES).joinToString(separator = "\n", postfix = "\n"))
    }

    private fun parse(line: String): BatterySample? {
        if (line.isBlank()) return null
        return runCatching {
            val json = JSONObject(line)
            val at = json.optLong("at", 0L)
            val level = json.optInt("level", -1)
            if (at <= 0L || level < 0) return null
            BatterySample(
                atMillis = at,
                levelPercent = level.coerceIn(0, 100),
                voltageMv = if (json.isNull("voltage")) null else json.optInt("voltage"),
                temperatureC = if (json.isNull("temp")) null else json.optDouble("temp").toFloat(),
                status = json.optInt("status", BatteryManager.BATTERY_STATUS_UNKNOWN),
                plugged = json.optInt("plugged", 0),
                currentUa = if (json.isNull("current")) null else json.optLong("current"),
                screenOn = if (json.has("screen") && !json.isNull("screen")) {
                    json.optBoolean("screen")
                } else {
                    null
                }
            )
        }.getOrNull()
    }

    private fun BatterySample.toJson(): JSONObject = JSONObject().apply {
        put("at", atMillis)
        put("level", levelPercent)
        put("voltage", voltageMv ?: JSONObject.NULL)
        put("temp", temperatureC ?: JSONObject.NULL)
        put("status", status)
        put("plugged", plugged)
        put("current", currentUa ?: JSONObject.NULL)
        // Absent, not `false`: a sample written before this field existed must
        // stay unknown instead of silently counting as a screen-off point.
        if (screenOn != null) put("screen", screenOn)
    }
}
