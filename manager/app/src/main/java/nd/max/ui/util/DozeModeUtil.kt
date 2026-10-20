/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * Backing types/helpers for the Doze Mode screen. DozeModeScreen.kt was
 * committed importing these from nd.max.ui.util but the file itself
 * never existed in the repo — this fills that gap.
 *
 * Everything here rides on Android's own DeviceIdleController shell
 * interface (`dumpsys deviceidle ...`) and the public PowerManager /
 * ActivityManager "app standby bucket" APIs — there's no MaxManager-specific
 * daemon involvement (the Rust/C services have no doze-related code), so
 * this talks to the platform directly via root shell where a public API
 * isn't available.
 *
 * Two things worth flagging explicitly rather than guessing silently:
 *
 * 1. "Idle cycles" / "Light idle cycles" stats: Android doesn't expose a
 *    historical counter of deep/light idle cycles through any stable API.
 *    What's implemented here is a cheap approximation — each time state is
 *    refreshed (screen open / after an action), it's compared against the
 *    last-seen state persisted in SharedPreferences, and the counter bumps
 *    on a transition into IDLE. That only catches transitions that happen
 *    while this screen (or a refresh) is active, not a true 24/7 system
 *    count — a real historical counter would need a background service
 *    listening for ACTION_DEVICE_IDLE_MODE_CHANGED.
 *
 * 2. GmsDozeMode (Default/Standard/Aggressive): there's no 3-tier "how
 *    strictly is this app doze-restricted" knob in Android — the closest
 *    real, stable mechanism is the App Standby Bucket
 *    (`am get/set-standby-bucket`), which *does* have graduated strictness.
 *    This maps the three UI options onto standby buckets (active / working_set
 *    / restricted). If a different mechanism was intended, this is the
 *    piece to swap out — everything else here is independent of it.
 *
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PowerManager
import com.topjohnwu.superuser.Shell

/** A single installed app as shown on the Doze Mode whitelist list. */
data class DozeAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isWhitelisted: Boolean
)

/** Mirrors DeviceIdleController's deep-doze state machine (`mState`). */
enum class DozeState {
    ACTIVE, IDLE_PENDING, SENSING, LOCATING, MAINTENANCE, IDLE, UNKNOWN
}

/**
 * How strictly Google Play Services is app-standby-restricted while the
 * device is idle. Backed by App Standby Buckets — see file header.
 */
enum class GmsDozeMode(val bucket: String) {
    DEFAULT("active"),
    STANDARD("working_set"),
    AGGRESSIVE("restricted")
}

object DozeModeUtil {

    private const val GMS_PACKAGE = "com.google.android.gms"
    private const val PREFS_NAME = "maxmanager_doze_stats"

    /** علّمتان تفصلان مخرجات الأوامر داخل **نداء واحد** (لا يظهران من الأوامر نفسها). */
    private const val MARK_LIGHT = "#MAX_DOZE_LIGHT#"
    private const val MARK_BUCKET = "#MAX_DOZE_BUCKET#"

    data class DozeStats(val idleCount: Int, val lightIdleCount: Int)

    /**
     * حالة الـDoze كاملة — **من رحلة صدفة واحدة**.
     *
     * **ولماذا (عطب سرعة مُبلَّغ عنه: فتح الشاشات):** كانت شاشة Doze تسأل أربع مرّات متتالية
     * (`isSupported` ← `get deep`، ثم `getGmsDozeMode` ← `am get-standby-bucket`، ثم
     * `getDozeState` ← `get deep` **مرة ثانية**، ثم `getLightDozeState` ← `get light`).
     * وكلّها تمرّ بالصدفة الواحدة المُسلسَلة، و`dumpsys` من أبطأ أوامرها — فالأربع تُجمع
     * زمنًا على أوّل إطار مفيد. وصارت الأوامر **أربعة في تنفيذ واحد** (`Shell.cmd(a,b,c,…)`
     * تُكتب كلّها في مدخل الصدفة نفسه، بترتيبها)، والفصل بعلامتين — وكل أمر **لم يُمسّ نصّه**
     * ولا تحليله، فلا يتغيّر معنى شيء.
     *
     * **والاحتياطي محفوظ كما كان:** إن عاد `get deep` فارغًا (نسخ OEM لا توصّله) يُقرأ
     * التفريغ الكامل ويُطلب فيه `mState=` — رحلة ثانية **تُدفع عند الحاجة فقط** كما كانت.
     */
    data class DozeSnapshot(
        val supported: Boolean,
        val deep: DozeState,
        val light: DozeState,
        val gmsMode: GmsDozeMode,
    )

    fun readSnapshot(): DozeSnapshot {
        val result = runCatching {
            Shell.cmd(
                "dumpsys deviceidle get deep",
                "echo $MARK_LIGHT",
                "dumpsys deviceidle get light",
                "echo $MARK_BUCKET",
                "am get-standby-bucket $GMS_PACKAGE",
            ).exec()
        }.getOrNull()
        val lines = result?.out?.map { it.trim() }.orEmpty()

        val lightAt = lines.indexOf(MARK_LIGHT)
        val bucketAt = lines.indexOf(MARK_BUCKET)
        val deepRaw = lines.take(lightAt.takeIf { it >= 0 } ?: lines.size)
            .filterNot(::isCommandNoise)
            .joinToString("").trim()
        val lightRaw = if (lightAt >= 0) {
            lines.subList(lightAt + 1, bucketAt.takeIf { it > lightAt } ?: lines.size)
                .filterNot(::isCommandNoise)
                .joinToString("").trim()
        } else {
            ""
        }
        val bucketRaw = if (bucketAt >= 0) {
            lines.drop(bucketAt + 1).filterNot(::isCommandNoise).joinToString("").trim().lowercase()
        } else {
            ""
        }

        val deepDirect = mapState(deepRaw)
        val light = mapState(lightRaw)
        // **وقراءة `get deep` الفاشلة لا تُسقط القدرة:** كانت `isSuccess` على التنفيذ كله
        // (وفيه `am get-standby-bucket` الذي قد يفشل بلا GMS) تُعلن «غير مدعوم» على جهاز
        // `deviceidle` فيه يعمل — وهو العطب المُبلَّغ عنه («المصدر deviceidle لا يقرأ»).
        // الآن يُقاس كل مصدر وحده، والاحتياطي هو التفريغ الكامل (`mState=`/`mLightState=`).
        val dump = if (deepDirect == DozeState.UNKNOWN || light == DozeState.UNKNOWN) fullDump() else null
        val deep = if (deepDirect != DozeState.UNKNOWN) deepDirect else dump?.deep ?: DozeState.UNKNOWN
        val resolvedLight = if (light != DozeState.UNKNOWN) light else dump?.light ?: DozeState.UNKNOWN

        return DozeSnapshot(
            supported = deep != DozeState.UNKNOWN || resolvedLight != DozeState.UNKNOWN,
            deep = deep,
            light = resolvedLight,
            gmsMode = GmsDozeMode.entries.firstOrNull { it.bucket == bucketRaw } ?: GmsDozeMode.DEFAULT,
        )
    }

    /**
     * سطر مخرجات ليس حالة: رسالة رفض، أو سطر مفقود من أمر غير مدعوم على نسخة OEM.
     *
     * وإسقاطه ضروري لأن هذه السطور تدخل في تجميع النصّ فتُقرأ «حالة مجهولة» بدل قراءة
     * السطر الحقيقي الذي يليها.
     */
    private fun isCommandNoise(line: String): Boolean {
        val lower = line.lowercase()
        return line.isBlank() || "can't find service" in lower || lower.startsWith("error") ||
            lower.startsWith("unknown command") || lower.startsWith("usage:") ||
            lower.startsWith("java.lang.") || lower.startsWith("exception")
    }

    /** الوضعان المقروءان من التفريغ الكامل. */
    private data class FullDump(val deep: DozeState, val light: DozeState)

    /**
     * احتياطي واحد للتفريغ الكامل: `mState=` للعميق و`mLightState=`/`mLightIdleState=` للخفيف.
     * **رحلة واحدة**، ولا تُدفع إلا حين لم يُقرأ مصدر من الأمر المباشر — فلا تُثقل فتح الشاشة
     * على جهاز يعمل فيه `get deep`.
     */
    private fun fullDump(): FullDump? {
        val out = runCatching { Shell.cmd("dumpsys deviceidle").exec().out }.getOrNull() ?: return null
        fun after(vararg keys: String): DozeState {
            keys.forEach { key ->
                val raw = out.firstOrNull { it.trim().startsWith(key) } ?: return@forEach
                val state = mapState(raw.substringAfter('='))
                if (state != DozeState.UNKNOWN) return state
            }
            return DozeState.UNKNOWN
        }
        return FullDump(
            deep = after("mState="),
            // أسماء الحقل تغيّرت بين إصدارات AOSP: `mLightState` و`mLightIdleState`.
            light = after("mLightIdleState=", "mLightState="),
        )
    }

    /** Whether this device's DeviceIdleController responds to `dumpsys deviceidle`. */
    fun isSupported(): Boolean {
        val result = Shell.cmd("dumpsys deviceidle get deep").exec()
        return result.isSuccess && result.out.joinToString("").isNotBlank()
    }

    /** Reads the current deep-doze state via `dumpsys deviceidle get deep`. */
    fun getDozeState(): DozeState {
        val direct = Shell.cmd("dumpsys deviceidle get deep").exec().out
            .joinToString("") { it.trim() }
            .trim()
        return mapState(if (direct.isNotBlank()) direct else fullDumpRaw())
    }

    /**
     * Fallback for devices/OEM forks where `get deep` isn't wired up or isn't wired the
     * same way: parse the full dump for "mState=<NAME>" — same parser as the snapshot's.
     */
    private fun fullDumpRaw(): String = fullDump()?.deep?.name.orEmpty()

    /** Reads the current light-doze state via `dumpsys deviceidle get light`. */
    private fun getLightDozeState(): DozeState {
        val raw = Shell.cmd("dumpsys deviceidle get light").exec().out
            .joinToString("") { it.trim() }
            .trim()
        return mapState(raw)
    }

    /**
     * تحويل نصّ حرّ إلى حالة — **بالمفردات لا بالمساواة الحرفية**.
     *
     * وكانت `when (name.uppercase())` تسقط على أي غلاف يضيفه المصنّع (`State: IDLE`،
     * `mState=IDLE`، `IDLE (deep)`) فتُعلن `UNKNOWN` على قراءة ناجحة — وهو نفس ما يراه
     * المستخدم «لا يقرأ». والآن يُختبر كل رمز في النصّ، وأول رمز تُعرف حالته هو الحكم.
     */
    internal fun mapState(name: String): DozeState {
        name.uppercase().split(Regex("[^A-Z_]+")).forEach { token ->
            stateOf(token)?.let { return it }
        }
        return DozeState.UNKNOWN
    }

    private fun stateOf(token: String): DozeState? = when (token) {
        "ACTIVE", "INACTIVE" -> DozeState.ACTIVE
        "IDLE_PENDING", "QUICK_DOZE_DELAY" -> DozeState.IDLE_PENDING
        "SENSING" -> DozeState.SENSING
        "LOCATING" -> DozeState.LOCATING
        "IDLE_MAINTENANCE", "MAINTENANCE" -> DozeState.MAINTENANCE
        "IDLE" -> DozeState.IDLE
        else -> null
    }

    fun forceIdle(): Boolean = Shell.cmd("dumpsys deviceidle force-idle").exec().isSuccess

    fun stepIdleState(): Boolean = Shell.cmd("dumpsys deviceidle step").exec().isSuccess

    fun unforceIdle(): Boolean = Shell.cmd("dumpsys deviceidle unforce").exec().isSuccess

    /**
     * Reads/updates the local idle-cycle counters. Call after any state
     * refresh (see file header for what this does and doesn't capture).
     */
    fun refreshStats(
        context: Context,
        currentDeep: DozeState,
        // والافتراض يحفظ سلوك المتصل القديم حرفيًّا (قراءة `get light` وحدها)، ومن يقرأ
        // اللقطة كلها مرّة واحدة يمرّر الحالة الخفيفة معها فلا تُقرأ ثانيةً.
        currentLight: DozeState = getLightDozeState(),
    ): DozeStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDeep = prefs.getString("last_deep_state", null)
        val lastLight = prefs.getString("last_light_state", null)

        var idleCount = prefs.getInt("idle_count", 0)
        var lightIdleCount = prefs.getInt("light_idle_count", 0)

        if (currentDeep == DozeState.IDLE && lastDeep != DozeState.IDLE.name) {
            idleCount += 1
        }
        if (currentLight == DozeState.IDLE && lastLight != DozeState.IDLE.name) {
            lightIdleCount += 1
        }

        prefs.edit()
            .putString("last_deep_state", currentDeep.name)
            .putString("last_light_state", currentLight.name)
            .putInt("idle_count", idleCount)
            .putInt("light_idle_count", lightIdleCount)
            .apply()

        return DozeStats(idleCount, lightIdleCount)
    }

    fun resetStats(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("idle_count", 0)
            .putInt("light_idle_count", 0)
            .apply()
    }

    /** Public, no-root read of whether [packageName] is exempt from battery optimizations. */
    fun isIgnoringBatteryOptimizations(context: Context, packageName: String): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return pm?.isIgnoringBatteryOptimizations(packageName) == true
    }

    /** Adds/removes [packageName] from the deep-doze whitelist ("unrestricted battery"). */
    fun setWhitelisted(packageName: String, whitelisted: Boolean): Boolean {
        val op = if (whitelisted) "+" else "-"
        return Shell.cmd("dumpsys deviceidle whitelist $op$packageName").exec().isSuccess
    }

    fun getInstalledApps(context: Context): List<DozeAppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        return packages.mapNotNull { appInfo ->
            try {
                DozeAppInfo(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                    isWhitelisted = isIgnoringBatteryOptimizations(context, appInfo.packageName)
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    fun getGmsDozeMode(): GmsDozeMode {
        val raw = Shell.cmd("am get-standby-bucket $GMS_PACKAGE").exec().out
            .joinToString("") { it.trim() }
            .trim()
            .lowercase()
        return GmsDozeMode.entries.firstOrNull { it.bucket == raw } ?: GmsDozeMode.DEFAULT
    }

    fun setGmsDozeMode(mode: GmsDozeMode): Boolean =
        Shell.cmd("am set-standby-bucket $GMS_PACKAGE ${mode.bucket}").exec().isSuccess
}
