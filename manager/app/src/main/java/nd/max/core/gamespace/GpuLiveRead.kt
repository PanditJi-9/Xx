/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.MtkGpuOppTable

/**
 * قراءة الرسّام **الحيّة الخفيفة** للوحة اللعبة.
 *
 * **العطب المقيس:** كل نبضة (٧٠٠ مللي ثانية) كانت تستدعي `GpuHardwareBackend.refresh()` **مرّتين**،
 * وهي إعادة اكتشاف كاملة: سرد `/sys/class/devfreq`، ثم لكل مرشّح نحو عشر قراءات، وجدول OPP، وثلاثة
 * `test -w` عبر صدفة الجذر، ثم مسح **كل** مناطق `/sys/class/thermal` بقراءة `type` لكلٍّ منها.
 * فرقمُ المعالج (قراءتان) يظهر فورًا، بينما الرسّام يتأخّر ثوانيَ — أو يتلاشى حين تزدحم صدفة الجذر
 * بوقت إقلاع اللعبة.
 *
 * الحيّ في الرسّام ثلاث عقد فقط: `cur_freq` و`max_freq` وعقدة قفل OPP (إن وُجدت). وكل ما سواها
 * (السلّم والوحدة وأسماء العقد) **ثابت** يُكتشف مرة ويُحفظ، فهنا تُشتقّ الأرقام من الجهاز المحفوظ
 * بالدلالة نفسها التي يطبّقها `GpuHardwareBackend` عند الاكتشاف الكامل — لا قيمة مختلقة:
 * ما لا يُحلّ بوحدة موثوقة يبقى `null`.
 */
object GpuLiveRead {

    /** القراءات الخام للعقد الحيّة، كما وردت من العتاد (بلغة العقدة نفسها). */
    data class Raw(val cur: Long?, val max: Long?, val lock: String?)

    /** الناتج بالميغاهرتز. `null` = لم يُقرأ بوحدة موثوقة. */
    data class Live(val curMhz: Int?, val dynamicMaxMhz: Int?)

    fun derive(device: GpuHardwareBackend.Device, raw: Raw): Live {
        if (device.mtkFixedIndexPath != null) return deriveMtk(device, raw)
        return Live(
            curMhz = GpuHardwareBackend.frequencyMHz(device, raw.cur)?.toInt(),
            dynamicMaxMhz = GpuHardwareBackend.frequencyMHz(device, raw.max)?.toInt()
        )
    }

    /**
     * مسار MediaTek ذو قفل OPP: نفس ترتيب الاكتشاف الكامل —
     * التردد المقفول إن وُجد قفل قائم، وإلا قيمة العقدة بشرط أن تكون **درجة مُعلنة** في السلّم.
     * والحدّ الأعلى يعود إلى آخر درجة معلنة حين لا تُقرأ العقدة.
     */
    private fun deriveMtk(device: GpuHardwareBackend.Device, raw: Raw): Live {
        val locked = lockedFrequencyHz(device, raw.lock)
        val cur = locked ?: normalizeToAdvertisedHz(raw.cur, device.frequencies)
        val max = locked ?: normalizeToAdvertisedHz(raw.max, device.frequencies)
            ?: device.frequencies.lastOrNull()
        return Live(
            curMhz = hzToMhz(cur),
            dynamicMaxMhz = hzToMhz(max)
        )
    }

    private fun lockedFrequencyHz(device: GpuHardwareBackend.Device, lockRaw: String?): Long? {
        val index = lockRaw?.let(MtkGpuOppTable::parseIndex)?.takeIf { it != "-1" } ?: return null
        return device.mtkOppIndexByFrequency.entries
            .firstOrNull { MtkGpuOppTable.parseIndex(it.value) == index }?.key
    }

    internal fun normalizeToAdvertisedHz(raw: Long?, advertisedHz: List<Long>): Long? {
        if (raw == null || raw <= 0L) return null
        return listOf(raw, raw * 1_000L, raw * 1_000_000L).distinct().singleOrNull { it in advertisedHz }
    }

    private fun hzToMhz(hz: Long?): Int? =
        hz?.let { it / 1_000_000L }?.takeIf { it in 1L..10_000L }?.toInt()

    /**
     * عقد الحمل بالترتيب نفسه الذي يجرّبه الاكتشاف الكامل. العقدة التي تجيب أولًا تُحفظ فلا تُجرَّب
     * البقية كل نبضة (العقدة الغائبة كانت تُكلّف رحلة صدفة كاملة).
     */
    fun loadCandidates(device: GpuHardwareBackend.Device): List<String> = buildList {
        add("${device.path}/load")
        add("${device.path}/device/load")
        if (device.family == GpuHardwareBackend.Family.QUALCOMM) add("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
        if (device.family == GpuHardwareBackend.Family.MALI) add("/sys/module/ged/parameters/gpu_idle")
    }

    /** نسبة الحمل 0..100 من نص عقدة؛ `gpu_idle` معكوسة (خمول لا انشغال). المجهول `null`. */
    fun loadPercent(path: String, raw: String?): Int? {
        val parsed = GpuHardwareBackend.parseLoad(raw ?: return null) ?: return null
        return if (path.endsWith("gpu_idle")) (100 - parsed).takeIf { it in 0..100 } else parsed
    }
}

/**
 * يمنع «—» الوامضة: قراءة رسّام فاشلة واحدة (صدفة الجذر مزدحمة لحظةً) لا تُفرغ العدّاد.
 *
 * كانت الخدمة تستبدل الحالة كلها بآخر قراءة (`clocks = fresh`) فيُمحى آخر رقم صحيح للرسّام فور أول
 * إخفاق، مع أن تعليق الدمج الجزئي نفسه يَعِد بألّا تُمحى القيمة. وهنا يُحتفظ بآخر قيمة صحيحة
 * **ما دام الإخفاق متتاليًا أقلّ من [MAX_MISSES] قراءة** (نحو ٤ ثوانٍ)، ثم يُترك الفراغ يظهر —
 * فلا يُعرض رقم قديم بلا نهاية كأنه حيّ. وما قرأه العتاد فعلًا يُعرض كما هو دائمًا.
 */
object PanelClockHold {
    const val MAX_MISSES = 6

    data class Held(val clocks: PanelClocks, val misses: Int)

    fun merge(old: PanelClocks, fresh: PanelClocks, misses: Int): Held {
        if (fresh.gpuShownMhz != null) return Held(fresh, 0)
        if (old.gpuShownMhz == null || misses >= MAX_MISSES) return Held(fresh, misses + 1)
        return Held(
            fresh.copy(
                gpuMhz = old.gpuMhz,
                gpuCeilingMhz = old.gpuCeilingMhz,
                gpuFloorMhz = old.gpuFloorMhz ?: fresh.gpuFloorMhz,
                gpuCapabilityMaxMhz = old.gpuCapabilityMaxMhz ?: fresh.gpuCapabilityMaxMhz,
                gpuLoadPct = fresh.gpuLoadPct ?: old.gpuLoadPct
            ),
            misses + 1
        )
    }
}
