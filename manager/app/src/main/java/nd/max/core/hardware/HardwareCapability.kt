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

    /**
     * هامش الحرارة من المنصّة (ADPF) — يُقرأ ولا يُكتب، وبلا جذر ولا عقدة `/sys`.
     *
     * وقدرةٌ مستقلّة عن [THERMAL_ZONES] عن قصد: تلك تقيس حرارةً **حدثت**، وهذه تقدّر خنقًا
     * **سيقع**؛ وغياب إحداهما لا يقول شيئًا عن الأخرى. فتُعرض في المصفوفة بصفّها لا مدموجة.
     */
    THERMAL_HEADROOM,

    /**
     * حالة الخنق الحرارية من المنصّة (`PowerManager.getThermalStatus`) — قراءة فقط.
     *
     * وصفٌّ ثالث مستقلّ عن [THERMAL_ZONES] (حرارة مقيسة) و[THERMAL_HEADROOM] (خنق متوقّع): هنا
     * **قرار المنصّة الحاليّ**، وهو ما يبني عليه النظام خفض التردّد. ودائرة باردة مع حالة `SEVERE`
     * تعني أن التبريد تأخّر عن القرار — ولا يظهر ذلك في أيّ من الصفّين الآخرين.
     */
    THERMAL_STATUS,
    BATTERY_TELEMETRY,
    ZRAM,
    DISPLAY_REFRESH,
    DISPLAY_RESOLUTION,
    TOUCH_CONTROL,

    /**
     * مقابض `/proc/sys/vm` — تحكّم Kernel VM (FKM: ١٧ مسارًا، والمختار منها عشرة).
     *
     * **ولماذا ميزة مستقلّة:** ليست ذاكرةً فيزيائية ([MEMORY_FREQUENCY]) ولا ضغطًا ([ZRAM])، بل
     * سياسة النواة في الكتابة القذرة والكاش والتجاوز. وقدرتُها تُشتقّ من الكتالوج
     * (`vm.*`) لا من مسار محفور — فأوّل ميزة تُولد من البيانات وحدها.
     */
    KERNEL_VM,
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

    /**
     * مفاتيح الكتالوج التي فسّرت هذه القدرة (M1 §6 · ADR-87).
     *
     * **ولماذا حقل مستقلّ لا لاحقة في [backend]:** `backend` يُعرض ويُطبع في بصمة الجهاز، فتغييره
     * يُغيّر نصًّا يراه المستخدم مقابل إضافة قياس. الحقل الجديد لا يلمس أي نصّ قائم، ويقول أيّ مدخلٍ
     * بيانات أعطى هذه القدرة — وهو ما ستحتاجه المرحلة الرابعة لتوليد جدول القدرات من الكتالوج.
     */
    val catalogKeys: List<String> = emptyList(),
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
