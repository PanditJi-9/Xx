/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * حالة الخنق الحرارية كما تُعلنها المنصّة (`PowerManager.getThermalStatus`، API 29).
 *
 * **ولماذا لا تُقاس من `sysfs`:** `thermal_zone` يقول «كم الحرارة الآن»، والحالة تقول «ماذا قرّرت
 * المنصّة أن تفعل» — وهي التي يبني عليها النظام خفض التردّد وإطفاء الشاشة. الرقمان ليسا الشيء نفسه،
 * ودائرة حرارةٍ باردة مع حالة `SEVERE` تعني أن المنصّة ما زالت تخنق بعد (تأخّر التبريد).
 */
enum class ThermalStatus {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,

    /** لا قراءة: إصدار أقدم، أو الخدمة غائبة، أو رمز خارج المعروف. */
    UNKNOWN,
    ;

    /** هل هذا خنق فعليّ (يستحقّ تغيير السلوك)؟ */
    val isThrottling: Boolean get() = this >= MODERATE && this <= SHUTDOWN

    companion object {
        /** يحوّل رمز المنصّة؛ وأي رمز خارج ٠..٦ يصير [UNKNOWN] لا استثناءً. */
        fun of(code: Int?): ThermalStatus = when (code) {
            0 -> NONE
            1 -> LIGHT
            2 -> MODERATE
            3 -> SEVERE
            4 -> CRITICAL
            5 -> EMERGENCY
            6 -> SHUTDOWN
            else -> UNKNOWN
        }
    }
}

/** عيّنة واحدة: الحالة، والرمز الخامّ (يصير `null` حين لا قراءة). */
data class ThermalStatusSample(
    val status: ThermalStatus,
    val raw: Int? = null,
) {
    val supported: Boolean get() = status != ThermalStatus.UNKNOWN
}

/**
 * قراءة حالة الحرارة من المنصّة.
 *
 * **والآلية (لا الكود) من DevCheck:** التطبيق هناك يستمع إلى
 * `OnThermalStatusChangedListener` بدل قياس `sysfs` وحده. ونحن نقرأها عند الطلب — الاستماع يحتاج
 * دورة حياة، والقراءة تُغطّي ما تعرضه الشاشة الآن.
 */
object ThermalReadout {

    /** `getThermalStatus` أُضيف في أندرويد ١٠. */
    const val MIN_API = Build.VERSION_CODES.Q

    /**
     * التصنيف — **دالّة نقيّة**: الإصدار والقراءة يُحقنان، فتُقاس كل الحالات بلا جهاز.
     * ولا تُستدعى [read] إن كان الإصدار أقدم من [MIN_API].
     */
    fun classify(apiLevel: Int, read: () -> Int?): ThermalStatusSample {
        if (apiLevel < MIN_API) return ThermalStatusSample(ThermalStatus.UNKNOWN)
        val raw = runCatching { read() }.getOrNull()
        return ThermalStatusSample(ThermalStatus.of(raw), raw)
    }

    @Suppress("NewApi")
    fun observe(context: Context): ThermalStatusSample {
        if (Build.VERSION.SDK_INT < MIN_API) return ThermalStatusSample(ThermalStatus.UNKNOWN)
        val power = context.getSystemService(PowerManager::class.java)
            ?: return ThermalStatusSample(ThermalStatus.UNKNOWN)
        return classify(Build.VERSION.SDK_INT) { power.currentThermalStatus }
    }
}
