/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

/**
 * قراءات أعمق من اللوحة الأساسيّة: تردّد المعالج، حرارة الشريحة، الرسّام، الذاكرة
 * المشتركة، القرص، وجهد البطارية.
 *
 * ### القاعدة نفسها: الغائب `null`
 *
 * كل قارئ هنا يقرأ عقدة حقيقيّة في `sysfs` أو `/proc`. إن غابت العقدة أو رفضت القراءة
 * فالنتيجة `null` — لا صفر ولا قيمة مشتقّة من اسم الجهاز. وهذا هو الفرق بين لوحة تقيس
 * ولوحة تُخمّن: من لا عقدة له عنده يُعرض «—».
 *
 * ### ولماذا ليست هذه القراءات في `FpsMonitorUtil`
 *
 * ذاك قارئ **الإطارات والعمليّة** ويُستدعى كل ثانية من التراكب؛ وهذه قراءات عتاد بطيئة
 * التغيّر (تردّد، حرارة، انشغال) فتُخزَّن لثوانٍ قليلة قبل إعادة القراءة. الخلط بينهما
 * كان يجعل كل نبضة تفتح عشرات الملفّات بلا فائدة مقيسة.
 */
object HudDeepReaders {

    /** تردّد المعالج يتغيّر أسرع من الحرارة، لكن ليس أسرع من ثانية. */
    private const val CPU_CACHE_MS = 1_000L

    /** الحرارة تُقرأ من عشرات المناطق؛ ثانيتان تكفيان ولا تُحرق النبضة. */
    private const val THERMAL_CACHE_MS = 2_000L

    /** الرسّام يُستقصى من عدّة عقد؛ ثانيتان تحدّان الكلفة. */
    private const val GPU_CACHE_MS = 2_000L

    /** تردّد الذاكرة المشتركة يتحرّك ببطء. */
    private const val DDR_CACHE_MS = 10_000L

    private const val CPU_ROOT = "/sys/devices/system/cpu"

    /** عقد تردّد الرسّام المعروفة: كوالكوم ثم عامّ ثم Mali. */
    private val GPU_CLOCK_NODES = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        "/sys/kernel/gpu/gpu_clock",
        "/sys/class/misc/mali0/device/clock",
        "/sys/class/misc/mali0/device/devfreq/cur_freq"
    )

    /** عقد انشغال الرسّام: زوج (busy total) عند كوالكوم، ونسبة جاهزة عند Mali. */
    private val GPU_BUSY_NODES = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/kernel/gpu/gpu_busy"
    )

    private val DDR_NAME_HINTS = listOf("ddr", "dmc", "mem", "ebi")

    private var cpuAtMs = 0L
    private var cpuMhz: Int? = null

    private var thermalAtMs = 0L
    private var socTempC: Float? = null

    private var gpuAtMs = 0L
    private var gpuMhz: Int? = null
    private var gpuLoad: Int? = null

    private var ddrAtMs = 0L
    private var ddrMhz: Int? = null

    /**
     * أعلى تردّد جارٍ بين عناقيد المعالج — الرقم الذي يجيب «هل رفع الجهاز تردّده؟».
     *
     * والمتوسّط كان سيخفي الاندفاع: عنقود صغير في الأدنى وعنقود كبير في الأعلى يعطيان
     * رقمًا وسطًا لا يصف أيًّا منهما. والسقف هو ما يُقاس عند سؤال الأداء.
     */
    fun cpuFreqMhz(): Int? {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - cpuAtMs < CPU_CACHE_MS) return cpuMhz
        cpuAtMs = now
        val policies = runCatching {
            File(CPU_ROOT).listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("cpu") }
                ?.flatMap { cpu ->
                    val cpufreq = File(cpu, "cpufreq")
                    if (cpufreq.isDirectory) listOf(cpufreq) else emptyList()
                }
                ?.mapNotNull { dir -> readLong(File(dir, "scaling_cur_freq"))?.let { khzToMhz(it) } }
                ?.takeIf { it.isNotEmpty() }
                ?.maxOrNull()
        }.getOrNull()
        cpuMhz = policies
        return cpuMhz
    }

    /**
     * حرارة الشريحة: أعلى منطقة حرارية مصنّفة معالجًا/SoC.
     *
     * **ولا تُخلط بحرارة البطارية** ([HudReading.heat]): هما مستشعران مختلفان، والخلط بينهما
     * كان يجعل «الحرارة» تعني شيئًا في مكان وشيئًا آخر في غيره.
     */
    fun socTempC(): Float? {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - thermalAtMs < THERMAL_CACHE_MS) return socTempC
        thermalAtMs = now
        socTempC = runCatching {
            ThermalUtil.readThermalZones()
                .asSequence()
                .filter { zone ->
                    val text = "${zone.label} ${zone.category}".lowercase()
                    SOC_HINTS.any { it in text } && "batt" !in text && "gpu" !in text
                }
                .map { it.temperatureC.toFloat() }
                .filter { it > 0f && it < 120f }
                .maxOrNull()
        }.getOrNull()
        return socTempC
    }

    /** تردّد الرسّام بالميغاهرتز من أوّل عقدة تُجيب. */
    fun gpuFreqMhz(): Int? {
        refreshGpu()
        return gpuMhz
    }

    /** انشغال الرسّام % من عقدة الانشغال؛ `null` إن لم تُعلن. */
    fun gpuLoadPercent(): Int? {
        refreshGpu()
        return gpuLoad
    }

    /** تردّد الذاكرة المشتركة من `devfreq` بالاسم لا بالترتيب. */
    fun ddrFreqMhz(): Int? {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - ddrAtMs < DDR_CACHE_MS) return ddrMhz
        ddrAtMs = now
        ddrMhz = runCatching {
            val root = File("/sys/class/devfreq")
            root.listFiles()?.asSequence()?.mapNotNull { device ->
                val name = readText(File(device, "name"))?.lowercase() ?: device.name.lowercase()
                if (DDR_NAME_HINTS.none { it in name }) return@mapNotNull null
                readLong(File(device, "cur_freq"))?.let { hzToMhz(it) }
            }?.firstOrNull()
        }.getOrNull()
        return ddrMhz
    }

    /** جهد البطارية بالميلي فولت من البثّ اللاصق — لا من ملفّ خاصّ بجهاز. */
    fun batteryVoltageMv(context: Context): Int? = runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)?.takeIf { it in 2_000..6_000 }
    }.getOrNull()

    private fun refreshGpu() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - gpuAtMs < GPU_CACHE_MS) return
        gpuAtMs = now
        gpuMhz = GPU_CLOCK_NODES.firstNotNullOfOrNull { node ->
            readLong(File(node))?.let { hzToMhz(it) }
        }
        gpuLoad = GPU_BUSY_NODES.firstNotNullOfOrNull { node ->
            readText(File(node))?.let { DiskIoSampler.parseGpuBusyPercent(it) }
        }
    }

    private fun readText(file: File): String? = runCatching {
        if (file.isFile) file.readText().trim().takeIf { it.isNotEmpty() } else null
    }.getOrNull()

    private fun readLong(file: File): Long? = readText(file)?.toLongOrNull()

    private val SOC_HINTS = listOf("cpu", "soc", "ap", "cluster", "big", "little", "prime", "mtktscpu")
}

/**
 * عدّاد القرص: الفرق بين عيّنتين هو المعدّل، وأوّل عيّنة لا تعطي رقمًا.
 *
 * والقراءة من `/proc/diskstats` لا من `StatFs`: الأوّل يقيس **الحركة**، والثاني يقيس
 * **المساحة**. وشاشة تقول «القرص يعمل» يجب أن تقرأ الأوّل.
 */
class DiskIoSampler {

    private var lastReadSectors = -1L
    private var lastWriteSectors = -1L
    private var lastAtMs = 0L

    /** يعيد (قراءة، كتابة) بالبايت/ثانية، أو `null` قبل أن تكتمل عيّنتان. */
    fun sample(nowMs: Long = android.os.SystemClock.elapsedRealtime()): Pair<Float?, Float?> {
        val text = runCatching { File("/proc/diskstats").readText() }.getOrNull() ?: return null to null
        val (read, write) = parseDiskSectors(text)
        val had = lastReadSectors >= 0L && lastWriteSectors >= 0L && lastAtMs > 0L
        val elapsedMs = nowMs - lastAtMs
        val readBps: Float?
        val writeBps: Float?
        if (had && elapsedMs in 1..60_000L && read >= lastReadSectors && write >= lastWriteSectors) {
            val seconds = elapsedMs / 1000f
            readBps = (read - lastReadSectors) * SECTOR_BYTES / seconds
            writeBps = (write - lastWriteSectors) * SECTOR_BYTES / seconds
        } else {
            readBps = null
            writeBps = null
        }
        lastReadSectors = read
        lastWriteSectors = write
        lastAtMs = nowMs
        return readBps to writeBps
    }

    companion object {
        const val SECTOR_BYTES = 512L

        /** أجهزة كتليّة أساسيّة فقط — الأقسام تُضاعف العدّاد، و`loop`/`zram` ليست قرصًا. */
        private val BASE_DEVICE = Regex("^(sd[a-z]+|mmcblk\\d+|nvme\\d+n\\d+|vd[a-z]+|dm-\\d+)$")

        /**
         * مجموع القطاعات المقروءة والمكتوبة من نصّ `/proc/diskstats`.
         *
         * ترتيب الحقول ثابت في النواة: بعد الاسم، القراءات ودمجها **وقطاعات القراءة**، ثم
         * الكتابات ودمجها **وقطاعات الكتابة**. والفهرس يُحسب من الاسم لا من تخمين طول السطر.
         */
        fun parseDiskSectors(text: String): Pair<Long, Long> {
            var read = 0L
            var write = 0L
            for (line in text.lineSequence()) {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 10) continue
                val name = parts[2]
                if (!BASE_DEVICE.matches(name)) continue
                read += parts[5].toLongOrNull() ?: 0L
                write += parts[9].toLongOrNull() ?: 0L
            }
            return read to write
        }

        /** نسبة انشغال الرسّام من نصّ عقدة: زوج (busy total) أو نسبة جاهزة ٠..١٠٠. */
        fun parseGpuBusyPercent(text: String): Int? {
            val parts = text.trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
            if (parts.size >= 2) {
                val total = parts[1]
                if (total <= 0L) return null
                return ((parts[0].toDouble() / total) * 100.0).toInt().coerceIn(0, 100)
            }
            val single = parts.firstOrNull() ?: return null
            return when {
                single in 0L..100L -> single.toInt()
                // بعض العقد تعطي جزءًا من مليون.
                single in 101L..1_000_000L -> (single / 10_000L).toInt().coerceIn(0, 100)
                else -> null
            }
        }
    }
}

/** هرتز ← ميغاهرتز، مع تمييز الوحدات الثلاث التي تظهر في عقود مختلفة. */
fun hzToMhz(raw: Long): Int? = when {
    raw <= 0L -> null
    // فوق ١٠٠ مليون: هرتز قطعًا (١٫٨ جيجاهرتز = 1_800_000_000) ⇒ ÷ ١e٦.
    raw >= 100_000_000L -> (raw / 1_000_000L).toInt()
    // وما دونها كيلوهرتز (عقد cpufreq: 1_800_000 كيلوهرتز = ١٨٠٠ ميجاهرتز) ⇒ ÷ ١e٣.
    else -> (raw / 1_000L).toInt()
}

/** كيلوهرتز ← ميغاهرتز (عقود `cpufreq` تُعلن بالكيلوهرتز). */
fun khzToMhz(raw: Long): Int? = if (raw <= 0L) null else (raw / 1_000L).toInt()
