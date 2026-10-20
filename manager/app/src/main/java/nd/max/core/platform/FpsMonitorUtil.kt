/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
قراءة الإطارات الحيّة وحالة الجهاز للوحة التراكب: ثلاثة مصادر للنواة (surfaceflinger ·
 * عقدة `measured_fps` · `dumpsys timestats`) والمصدر يُختار بما يُوجد فعلًا على الجهاز.
 *
 * وفضلُ كشف أمر `service call SurfaceFlinger 1013` (واجهة غير موثّقة في AOSP) لقارئ
 * مشروع vtools؛ وهو ذِكر فضل لا نقل كود: المقابلة النصّية معه مُقاسة في
 * `docs/PROVENANCE.md` (احتواء البنية 0٠٠٢ · صفر نصّ حرفي مشترك). */

package nd.max.core.platform

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import com.topjohnwu.superuser.Shell
import nd.max.core.hardware.RootFileAccess
import kotlin.math.abs
import java.util.regex.Pattern

enum class FpsReadMode { SURFACEFLINGER, KERNEL_NODE, DUMPSYS_TIMESTATS }

/**
 * قراءة الإطارات الحيّة ومقاييس الحِمل للوحة التراكب ([nd.max.service.FpsOverlayService]).
 *
 * وكل قراءة صدفة تمرّ بـlibsu [Shell] — طريق الجذر الواحد في التطبيق — أو بـ
 * [nd.max.core.hardware.RootFileAccess] حين تكون العقدة مقروءة من العملية أصلًا.
 */
object FpsMonitorUtil {

    private const val PREFS_NAME = "maxmanager_fps_overlay"
    private const val KEY_MODE = "fps_read_mode"

    /**
     * مهلة تهدئة المصدر الذي أخفق — ١٥ ثانية.
     *
     * **ولماذا لا صفر (كما كان):** كان البديل الثقيل (`dumpsys SurfaceFlinger --timestats`) يُسأل
     * في كل ثانية على جهاز لا تُجيب فيه معاملة SurfaceFlinger القديمة — واللوحة تعمل بلحظة اللعب.
     * و١٥ ثانية أقصر من أن تُخفي مصدرًا عاد للعمل، وأطول بما يكفي لينزع عن الثانية حمل ثلاث
     * محاولات. ومن أخفق جميعه تُعرض شرطة (لا رقم مُخترع) حتى انتهاء التهدئة.
     */
    private const val SOURCE_FAILURE_COOLDOWN_MS = 15_000L

    /** عمر مخبأ «المقدّمة» — رحلتان بالصدفة تُقاسان في اللوحة كل ١٫٥ ثانية، فالمخبأ ينزع تكرارهما. */
    private const val FOREGROUND_TTL_MS = 2_000L

    /** عمر مخبأ المُصيِّر — الخاصية **للحزمة** لا للحظة: لعبة بمُصيِّر واحد لا يتغيّر إلا بإعادة تشغيل. */
    private const val RENDERER_TTL_MS = 20_000L

    var currentMode: FpsReadMode = FpsReadMode.SURFACEFLINGER
        private set

    private var kernelFpsPath: String? = null
    private var lastSfFrames = -1L
    private var lastSfTime = -1L

    /** آخر مصدر أجاب — يُقدَّم على غيره؛ `null` حتى أوّل إجابة في هذه العملية. */
    private var lastGoodMode: FpsReadMode? = null

    /** موعد إعادة اختبار كل مصدر أخفق (بترتيب القائمة)، و`0` = لا تهدئة عليه. */
    private val sourceRetryAtMs = LongArray(FpsReadMode.entries.size)

    // عدادات دلتا /proc/stat — volatile لأن عدة حلقات (لوحة النظام،
    // حلقة التعلم، محرك التوصيات، حاكم الطاقة) تقرأها من خيوط IO
    // متزامنة، وlong غير ذري على ART 32-bit (قراءة ممزقة محتملة).
    @Volatile private var lastCpuTotal = 0L
    @Volatile private var lastCpuIdle = 0L

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idx = prefs.getInt(KEY_MODE, FpsReadMode.SURFACEFLINGER.ordinal)
        currentMode = FpsReadMode.entries.getOrElse(idx) { FpsReadMode.SURFACEFLINGER }
        if (currentMode == FpsReadMode.KERNEL_NODE) findKernelFpsNode()
    }

    fun setMode(context: Context, mode: FpsReadMode) {
        currentMode = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MODE, mode.ordinal).apply()
        lastSfFrames = -1L
        lastSfTime = -1L
        if (mode == FpsReadMode.KERNEL_NODE) findKernelFpsNode()
    }

    private fun shellOut(cmd: String): String =
        Shell.cmd(cmd).exec().out.joinToString("\n").trim()

    /**
     * مصدر الإطارات — **بالذاكرة والتهدئة لا بالتكرار الأعمى** (جولة الحرارة واللاغ).
     *
     * **ما كان يقع في كل ثانية (وهو أثقل مسار في التطبيق):** يُسأل المصدر المُختار، فإن لم يُجِب
     * يُجرَّب البديلان — وأحد البديلين `dumpsys SurfaceFlinger --timestats` وهو **الثقيل** (يُسلسل
     * تاريخ الإطارات كاملًا)، والآخر صدفة على عقدة. أي أنّ جهازًا لا تُجيب فيه معاملة SurfaceFlinger
     * القديمة (وهو حال كثير من بنيات البائعين) كان يُشغّل تركيبة ثقيلة **كل ثانية** — واللوحة
     * تعمل تحديدًا لحظة اللعب، أي حيث تُقاس الحرارة.
     *
     * **والقرار:** يبقى السلوك والترتيب نفسه، ويُضاف أمران لا يمسّان المعنى:
     * 1. **ذاكرة المصدر الذي أجاب** ([lastGoodMode]): يُقدَّم على غيره في الدورة التالية، فلا
     *    يبدأ كل استطلاع من مصدر يعرف أنّه أخفق على هذا الجهاز.
     * 2. **تهدئة عطب لكل مصدر** ([SOURCE_FAILURE_COOLDOWN_MS]): المصدر الذي أخفق لا يُسأل في
     *    الثانية التالية، وإن أخفق الجميع تُعاد المحاولة بعد التهدئة وحدها — وتُعرض شرطة بينها
     *    لا رقم مُخترع (نفس قاعدة `null` في [HudReading]).
     *
     * ولم يُحذف بديل: كل مصدر يبقى مُجرَّبًا، ومهلة التهدئة (١٥ ثانية) أقصر من أن تُخفي عطبًا
     * دائمًا أو تُطيل «صفر إطار» على جهاز سليم.
     */
    fun getFps(): Float {
        val now = System.currentTimeMillis()
        for (mode in sourcesToTry(lastGoodMode, currentMode, sourceRetryAtMs, now)) {
            val value = readByMode(mode)
            if (value > 0f) {
                lastGoodMode = mode
                sourceRetryAtMs[mode.ordinal] = 0L
                return value
            }
            sourceRetryAtMs[mode.ordinal] = now + SOURCE_FAILURE_COOLDOWN_MS
        }
        return 0f
    }

    /**
     * ترتيب المصادر التي تُجرَّب في هذه الدورة — **نقيّ ومُختبَر** (`FpsMonitorCooldownTest`).
     *
     * والقاعدة: المصدر الذي أجاب آخر مرّة أوّلًا، ثم المصدر المُختار من المستخدم، ثم البقية؛
     * و**ما دام في تهدئة يُستثنى**، فإن أخفق الجميع فالقائمة فارغة ⇒ `0` بلا أي نداء — وهو
     * الفرق بين «صفر بلا كلفة» و«صفر بثلاث محاولات في الثانية».
     */
    internal fun sourcesToTry(
        preferred: FpsReadMode?,
        configured: FpsReadMode,
        retryAtMs: LongArray,
        nowMs: Long,
    ): List<FpsReadMode> {
        val order = buildList {
            preferred?.let { add(it) }
            add(configured)
            FpsReadMode.entries.forEach { add(it) }
        }.distinct()
        return order.filter { retryAtMs.getOrElse(it.ordinal) { 0L } <= nowMs }
    }

    private fun readByMode(mode: FpsReadMode): Float = when (mode) {
        FpsReadMode.SURFACEFLINGER -> readSurfaceFlingerFps()
        FpsReadMode.KERNEL_NODE -> readKernelFps()
        FpsReadMode.DUMPSYS_TIMESTATS -> readDumpsysFps()
    }

    /** `service call SurfaceFlinger 1013` returns a running frame counter; FPS is its delta over time. */
    private fun readSurfaceFlingerFps(): Float {
        return try {
            val result = shellOut("service call SurfaceFlinger 1013")
            if (!result.contains("Parcel")) return 0f
            val matcher = Pattern.compile("([0-9a-fA-F]{8})\\s+([0-9a-fA-F]{8})").matcher(result)
            if (!matcher.find()) return 0f
            val lo = (matcher.group(1) ?: return 0f).toLong(16)
            val hi = (matcher.group(2) ?: return 0f).toLong(16)
            val frames = (hi shl 32) or lo
            val now = System.currentTimeMillis()
            var fps = 0f
            if (lastSfTime > 0 && lastSfFrames >= 0) {
                val dt = now - lastSfTime
                if (dt > 0) fps = (frames - lastSfFrames) * 1000f / dt
            }
            lastSfFrames = frames
            lastSfTime = now
            if (fps in 0f..240f) fps else 0f
        } catch (e: Exception) { 0f }
    }

    private val KERNEL_FPS_CANDIDATES = listOf(
        "/sys/class/drm/sde-crtc-0/measured_fps",
        "/sys/class/graphics/fb0/measured_fps",
        "/sys/class/video/fps_info"
    )

    private fun findKernelFpsNode() {
        // `[ -f … ]` في صدفة لكل مرشَّح ⇒ فحص وجود مباشر (وبدونه صدفتان لكل قراءة FPS).
        kernelFpsPath = KERNEL_FPS_CANDIDATES.firstOrNull { RootFileAccess.exists(it) } ?: ""
    }

    private fun readKernelFps(): Float {
        if (kernelFpsPath.isNullOrEmpty()) {
            findKernelFpsNode()
            if (kernelFpsPath.isNullOrEmpty()) return 0f
        }
        val line = runCatching { RootFileAccess.read(kernelFpsPath!!) }.getOrNull() ?: return 0f
        return secondField(line)?.toFloatOrNull() ?: 0f
    }

    /** الحقل الثاني من سطر — بديل `awk '{print $2}'` بلا صدفة، ومُختبَر مباشرة. */
    internal fun secondField(line: String): String? =
        line.trim().split("\\s+".toRegex()).getOrNull(1)

    private fun readDumpsysFps(): Float {
        return try {
            val out = shellOut(
                "(dumpsys SurfaceFlinger --timestats -dump && " +
                    "dumpsys SurfaceFlinger --timestats -clear -enable) | grep averageFPS"
            )
            val value = Regex("averageFPS\\s*[=:]\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
                .find(out)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return 0f
            value.takeIf { it in 1f..240f } ?: 0f
        } catch (e: Exception) { 0f }
    }

    // ── System load metrics shown alongside FPS in the overlay ──────────

    // ── مخبأ «المقدّمة» ───────────────────────────────────────────────
    // مُستدعيان في مسار ساخن: اللوحة العائمة (كل ثانية، عبر المُصيِّر) ولوحة اللعبة (كل ١٫٥ ثانية
    // — وكلّ نداء كان يفتح صدفة، وربّما ثلاثًا بالتتابع). والقيمة ذاتها تُقرأ في الثانية الواحدة
    // أكثر من مرّة بلا خبر جديد، فالمخبأ يعيدها داخل عمره.
    private var foregroundCachedAtMs = 0L
    private var foregroundCache = ""
    private var focusedWindowCachedAtMs = 0L
    private var focusedWindowCache: String? = null

    fun getForegroundPackage(): String {
        val now = System.currentTimeMillis()
        if (foregroundCache.isNotEmpty() && now - foregroundCachedAtMs < FOREGROUND_TTL_MS) {
            return foregroundCache
        }
        val resolved = try {
            listOf(
                "dumpsys activity activities | grep mResumedActivity",
                "dumpsys window | grep mCurrentFocus",
                "cmd activity get-top-activity"
            ).firstNotNullOfOrNull { cmd ->
                parsePackageFromDump(shellOut(cmd)).ifEmpty { null }
            } ?: ""
        } catch (e: Exception) { "" }
        if (resolved.isNotEmpty()) {
            foregroundCache = resolved
            foregroundCachedAtMs = now
        }
        return resolved
    }

    /**
     * التطبيق صاحب **نافذة التركيز** الآن (`mCurrentFocus` ثم `mFocusedApp`)، أو `null` إن لم يُقرأ.
     *
     * مصدر ثانٍ لـ[getForegroundPackage] يُستعمل لمراجعته: ما يملك التركيز هو ما يُرى على الشاشة،
     * بينما `mResumedActivity` قد يبقى اسمًا للعبة غادرها المستخدم.
     */
    fun getFocusedWindowPackage(): String? {
        val now = System.currentTimeMillis()
        if (now - focusedWindowCachedAtMs < FOREGROUND_TTL_MS) return focusedWindowCache
        val resolved = try {
            ForegroundAppResolver.parseResumedPackage(
                shellOut("dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp'")
            )
        } catch (e: Exception) { null }
        // الغياب يُخبَّأ أيضًا: العطب المتكرّر (بلا جذر، أو مخرَج بلا نافذة مركّزة) كان يُعاد
        // سؤاله في **كل** نداء — وهو أصل التكرار الذي يستحقّ الكتم لا الجواب نفسه.
        focusedWindowCache = resolved
        focusedWindowCachedAtMs = now
        return resolved
    }

    private fun parsePackageFromDump(raw: String): String {
        if (raw.isEmpty()) return ""
        val matcher = Pattern.compile("([a-zA-Z0-9_]+\\.[a-zA-Z0-9_.]+)/").matcher(raw)
        if (matcher.find()) {
            val found = matcher.group(1) ?: return ""
            if (found != "com.android.systemui" && found != "android") return found
        }
        return ""
    }

    /**
     * مُصيِّر الرسوم للتطبيق المقيم (`VULKAN` · `OPENGL` · `FPS` عند عدم القراءة).
     *
     * **وهو مُخبَّأ بالحزمة (‏[RENDERER_TTL_MS]):** كان يُقاس **في كل ثانية** من اللوحة العائمة
     * بصدفة `pidof` ثم قراءة `/proc/<pid>/maps` (خريطة ذاكرة قد تبلغ مئات الكيلوبايتات). والمُصيِّر
     * خاصيّة لا تتغيّر داخل جلسة اللعبة — يتغيّر بعد إعادة تشغيلها — فالسؤال كل ثانية كان استهلاكًا
     * بلا خبر. وقيمة الحزمة نفسها تُعاد داخل العمر، وحزمة أخرى تُقاس فورًا.
     */
    fun getCurrentRenderer(pkg: String): String {
        if (pkg.isEmpty()) return "FPS"
        val now = System.currentTimeMillis()
        if (pkg == rendererCachePkg && now - rendererCachedAtMs < RENDERER_TTL_MS) {
            return rendererCacheValue
        }
        val resolved = try {
            // `pidof` تحتاج صدفة (لا مقابل ملفي لها)، وأمّا خريطتَي الذاكرة فكانتا صدفتين
            // إضافيتين — صارتا قراءة واحدة: الملف يُقرأ، والبحث `contains` في الذاكرة.
            val pid = shellOut("pidof $pkg").split(" ").firstOrNull() ?: return "FPS"
            val maps = runCatching { RootFileAccess.read("/proc/$pid/maps") }.getOrNull()
                ?: return "FPS"
            if (maps.contains("libvulkan.so")) "VULKAN"
            else if (maps.contains("libGLES")) "OPENGL"
            else "FPS"
        } catch (e: Exception) { "FPS" }
        rendererCachePkg = pkg
        rendererCacheValue = resolved
        rendererCachedAtMs = now
        return resolved
    }

    private var rendererCachePkg = ""
    private var rendererCacheValue = "FPS"
    private var rendererCachedAtMs = 0L

    /**
     * حمل المعالج كنسبة مئوية من دلتا /proc/stat بين استدعاءين.
     *
     * @Synchronized لأن عدة حللات مراقبة (لوحة النظام كل 2 ث، التعلم
     * المعزز كل 3 ث، محرك التوصيات كل 5 ث، حاكم الطاقة كل 2 ث) تتقاسم
     * نفس العدادات الثابتة — بدون القفل تتنافس قراءة-تعديل-كتابة
     * فتُفقد فترات أو تُقرأ قيم ممزقة.
     *
     * صيغة الإجمالي تشمل كل حقول النواة (حتى steal) وiowait يُحسب
     * ضمن الخمول — انتظار IO ليس عملًا للمعالج. الصيغة السابقة
     * (user+nice+system+idle فقط) كانت تحسب iowait/irq/softirq/steal
     * كأنها عمل فتبالغ في الحمل ممنهجًا على الأنظمة كثيفة IO.
     */
    /** عيّنة من السطر الأول لـ`/proc/stat`: المجموع والخمول — الحقول كلها. */
    internal data class CpuSample(val total: Long, val idle: Long)

    /**
     * تحليل سطر `cpu …` — نقيّ ومُختبَر (`FpsMonitorParseTest`)، و`null` للسطر المشوّه:
     *فشلُ قراءة لا يُعرض صفرًا يُوهم بأن المعالج ساكن.
     */
    internal fun parseStatSample(line: String): CpuSample? {
        val p = line.trim().split("\\s+".toRegex())
        if (p.size < 5) return null
        val user = p[1].toLongOrNull() ?: return null
        val nice = p[2].toLongOrNull() ?: return null
        val system = p[3].toLongOrNull() ?: return null
        val idle = p[4].toLongOrNull() ?: return null
        val iowait = p.getOrNull(5)?.toLongOrNull() ?: 0L
        val irq = p.getOrNull(6)?.toLongOrNull() ?: 0L
        val softirq = p.getOrNull(7)?.toLongOrNull() ?: 0L
        val steal = p.getOrNull(8)?.toLongOrNull() ?: 0L
        val idleAll = idle + iowait
        return CpuSample(
            total = user + nice + system + idleAll + irq + softirq + steal,
            idle = idleAll,
        )
    }

    /** نسبة الحمل بين عيّنتين — نفس الصيغة السابقة حرفيًا، نقيّة ومُختبَرة. */
    internal fun loadPercent(previous: CpuSample, current: CpuSample): Int {
        val dTotal = current.total - previous.total
        val dIdle = current.idle - previous.idle
        if (dTotal <= 0L) return 0
        return (((dTotal - dIdle) * 100 / dTotal).toInt()).coerceIn(0, 100)
    }

    /**
     * مُقدِّر حمل معالج بنافذته الخاصة (تكملة ٢٦١). كل مستدعٍ يحمل عيّنته، فلا يسرق نداءٌ من حلقة أخرى
     * الفترةَ التي تقيسها هذه: `getCpuLoad` تشارك حالة واحدة بين حلقات مختلفة الإيقاع، فالفرق بين عيّنتين
     * قد يغطي أجزاء من الثانية، فتقفز النسبة بلا معنى.
     */
    class CpuLoadMeter {
        private var lastTotal = 0L
        private var lastIdle = 0L

        @Synchronized
        fun sample(): Int {
            val line = runCatching { RootFileAccess.read("/proc/stat") }.getOrNull()
                ?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: return 0
            val current = FpsMonitorUtil.parseStatSample(line) ?: return 0
            val previous = if (lastTotal == 0L) null else FpsMonitorUtil.CpuSample(lastTotal, lastIdle)
            lastTotal = current.total
            lastIdle = current.idle
            return previous?.let { FpsMonitorUtil.loadPercent(it, current) } ?: 0
        }
    }

    @Synchronized
    fun getCpuLoad(): Int {
        // **قراءة مباشرة لا صدفة:** `cat /proc/stat` كانت تولّد صدفة جذر لكل نداء، والدالة
        // تُنادى من أربع حلقات (لوحة كل ٢ث · تعلّم كل ٣ث · توصيات كل ٥ث · حاكم طاقة كل ٢ث).
        // وطبقة `RootFileAccess` تقرأ الملف مباشرة (أو عبر IPC) وتُبقي الصدفة احتياطًا أخيرًا
        // فيها — فالسلوك على جهاز لا يُقرأ فيه الملف محفوظ حرفيًّا.
        val line = runCatching { RootFileAccess.read("/proc/stat") }.getOrNull()
            ?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: return 0
        val sample = parseStatSample(line) ?: return 0
        val previous = if (lastCpuTotal == 0L) null else CpuSample(lastCpuTotal, lastCpuIdle)
        lastCpuTotal = sample.total
        lastCpuIdle = sample.idle
        return previous?.let { loadPercent(it, sample) } ?: 0
    }

    fun getPowerWatt(): Float {
        return try {
            // كانتا صدفتين منفصلتين ⇒ قراءتان في معاملة واحدة (والصدفة تبقى داخل الطبقة).
            val values = RootFileAccess.readMany(
                listOf(
                    "/sys/class/power_supply/battery/voltage_now",
                    "/sys/class/power_supply/battery/current_now",
                )
            )
            val v = values.getOrNull(0)?.toLongOrNull() ?: 0L
            val c = values.getOrNull(1)?.toLongOrNull() ?: 0L
            if (v > 0 && c != 0L) (abs(v * c).toDouble() / 1_000_000_000_000.0).toFloat() else 0f
        } catch (e: Exception) { 0f }
    }

    fun getBatteryTemp(context: Context): Float {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        } catch (e: Exception) { 0f }
    }

    data class RamInfo(val usedMb: Int, val totalMb: Int)

    fun getRamInfo(context: Context): RamInfo {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalMb = (info.totalMem / 1048576L).toInt()
            val usedMb = ((info.totalMem - info.availMem) / 1048576L).toInt()
            RamInfo(usedMb, totalMb)
        } catch (e: Exception) { RamInfo(0, 0) }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        if (packageName.isEmpty()) return null
        return try { context.packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }
    }
}
