/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.hardware

/** Features that a device may expose to MaxManager. */
enum class HardwareFeature {
    CPU_FREQUENCY,
    CPU_GOVERNOR,
    CPU_CORE_CONTROL,
    CPU_BOOST,
    GPU_FREQUENCY,
    GPU_GOVERNOR,
    GPU_BOOST,

    /**
     * تردّد الذاكرة (DVFS للذاكرة/عرض النطاق) — عُقَد `devfreq` التي صنّفها
     * [MemoryUfsHardwareBackend] `MEMORY`. وقدرةٌ مستقلة عن GPU عن قصد: مقبضٌ آخر، ومالكٌ آخر،
     * وفشلُ واحد لا يعني شيئًا عن الآخر.
     */
    MEMORY_FREQUENCY,

    /** تردّد التخزين (UFS/eMMC/NVMe) — نظير [MEMORY_FREQUENCY] لهدف أطلس `STORAGE`. */
    UFS_FREQUENCY,
    THERMAL_ZONES,
    THERMAL_COOLING,
    BATTERY_TELEMETRY,
    ZRAM,
    DISPLAY_REFRESH,
    DISPLAY_RESOLUTION,
    TOUCH_CONTROL,
}

enum class AccessLevel {
    NONE,
    READ_ONLY,
    READ_WRITE,
}

data class FeatureCapability(
    val feature: HardwareFeature,
    val access: AccessLevel,
    val backend: String,
    val evidence: List<String> = emptyList(),
) {
    val readable: Boolean get() = access != AccessLevel.NONE
    val writable: Boolean get() = access == AccessLevel.READ_WRITE
}

data class HardwareCapabilitySnapshot(
    val vendor: String,
    val platform: String,
    val features: Map<HardwareFeature, FeatureCapability>,
    val generatedAtMs: Long = System.currentTimeMillis(),
) {
    fun capability(feature: HardwareFeature): FeatureCapability? = features[feature]
    fun supports(feature: HardwareFeature): Boolean = features[feature]?.readable == true
    fun canWrite(feature: HardwareFeature): Boolean = features[feature]?.writable == true
}

/**
 * صفوف مصفوفة القدرات كما تُعرض: **المتاح يُعرض دائمًا، وغير المتاح (`NONE`) يُخفى حين يطلبه
 * المستخدم** — وهو خيار «إخفاء الميزات غير المتاحة على جهازي» (§٤-و) في شاشة الإعدادات.
 *
 * **ولماذا هنا لا في الشاشة:** `CapabilityMatrixCard` يرسم ما تعيده هذه الدالّة، واختبار JVM
 * يقيس الطيّ بلا أندرويد — فالسياسة تُقاس، والرسم يُرى.
 *
 * **وحدّها:** هي لا تقرّر *ما* هو متاح (ذلك حكم `AccessLevel` المقيس في المحلّل)، ولا تُسقط
 * `READ_ONLY` — فلقدرة القراءة قيمة قائمة (تشخيصٌ يُقرأ) وليست «غير متاحة».
 */
fun capabilityRows(
    features: Collection<FeatureCapability>,
    showUnavailable: Boolean,
): List<FeatureCapability> =
    if (showUnavailable) features.toList() else features.filter { it.access != AccessLevel.NONE }
