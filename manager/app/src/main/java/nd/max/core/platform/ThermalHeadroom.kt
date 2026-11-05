/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi

/**
 * حالة قراءة هامش الحرارة (ADPF) — تُعلن **لماذا** غاب الرقم، لا تعيد صفرًا صامتًا.
 *
 * وهذا هو الفرق العملي: `NaN` من المنصّة تعني «غير مدعوم» لا «بارد»؛ وخلط الاثنين يجعل الشاشة
 * تقول «الحرارة ممتازة» على جهازٍ لا يقيسها أصلًا (ADR-07).
 */
enum class HeadroomStatus {
    /** قيمة قرأتها المنصّة. */
    LIVE,

    /** المنصّة أقدم من `PowerManager.getThermalHeadroom` (API 30). */
    NO_API,

    /** لا خدمة `PowerManager` في هذا السياق. */
    NO_SERVICE,

    /** المنصّة ردّت `NaN` — الجهاز لا يقدّر الهامش. */
    UNSUPPORTED,

    /** القراءة رمت استثناءً (ROM معدَّل، صلاحية، …). */
    THREW,
}

/**
 * عيّنة هامش حرارة واحدة.
 *
 * @property percent نسبة الزمن المتوقّع للخنق في أفق [horizonSeconds]؛ `>1` تعني «الخنق متوقّع
 * داخل الأفق». `null` حين لا قراءة.
 * @property status سبب الغياب حين لا قراءة.
 */
data class HeadroomSample(
    val horizonSeconds: Int,
    val percent: Float?,
    val status: HeadroomStatus,
) {
    /** «مدعوم» تعني قيمة مقيسة فعلًا — لا مجرّد وجود صنف. */
    val supported: Boolean get() = status == HeadroomStatus.LIVE
}

/**
 * هامش الحرارة من المنصّة — إشارة رسمية بلا جذر وبلا عقدة `/sys`.
 *
 * **الآلية (لا الكود) مأخوذة من فكرة DevCheck:** إشارة ADPF التي تسبق الخنق بدل قياس الحرارة
 * الحالية المتأخّرة عنه. والتنفيذ هنا مستقلّ تمامًا: واجهة أندرويد عامّة + عتبة ١٠ ثوانٍ، ولا
 * سطر من المرجع (مغلق المصدر).
 *
 * **وقاعدة الفشل مغلقة:** كل ما ليس قيمة صالحة يصير `status` معلنًا و`percent = null`. ولا يُعاد
 * صفرٌ نيابةً عن «غير مدعوم».
 */
object ThermalHeadroom {

    /** الأفق الذي يطلبه DevCheck ويطيقه `PowerManager`: عشر ثوانٍ (XR-08). */
    const val HORIZON_SECONDS = 10

    /** أدنى إصدار يملك `getThermalHeadroom`. */
    const val MIN_API = Build.VERSION_CODES.R

    /**
     * سقف الحماية: `getThermalHeadroom` قد تُرجع قيمًا بعيدة فوق ١ على أجهزة تُبلّغ عن أسوأ
     * سيناريو؛ تُقصّ عند ٥ لأن ما بعدها لا يغيّر قرارًا، ولأن رقمًا بلا حدّ يُفسد أي متوسط.
     */
    const val MAX_PERCENT = 5f

    /**
     * التصنيف — **دالّة نقيّة** تُقاس بالحقن، ولا تلمس أندرويد.
     *
     * @param apiLevel إصدار المنصّة (يُمرَّر لا يُقرأ، فيُقاس كل فرع).
     * @param read القراءة نفسها؛ **لا تُستدعى أبدًا** حين `apiLevel < MIN_API`.
     */
    fun classify(apiLevel: Int, read: () -> Float?): HeadroomSample = when {
        apiLevel < MIN_API -> HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.NO_API)

        else -> {
            val result = runCatching { read() }
            result.fold(
                onSuccess = { raw ->
                    when {
                        raw == null -> HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.NO_SERVICE)
                        raw.isNaN() -> HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.UNSUPPORTED)
                        else -> HeadroomSample(
                            HORIZON_SECONDS,
                            raw.coerceIn(0f, MAX_PERCENT),
                            HeadroomStatus.LIVE,
                        )
                    }
                },
                onFailure = { HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.THREW) },
            )
        }
    }

    /**
     * القراءة الحقيقية من المنصّة.
     *
     * `@Suppress("NewApi")` لأن النداء يقع داخل لامدا [classify]، فلا يرى فاحص Lint حارس
     * `SDK_INT` حوله؛ والحارس في [observe] **قبل** أي لمس للنظام (و`MIN_API` هو ٣٠ نفسه).
     */
    @Suppress("NewApi")
    fun observe(context: Context): HeadroomSample {
        if (Build.VERSION.SDK_INT < MIN_API) {
            return HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.NO_API)
        }
        val power = context.getSystemService(PowerManager::class.java)
            ?: return HeadroomSample(HORIZON_SECONDS, null, HeadroomStatus.NO_SERVICE)
        return classify(Build.VERSION.SDK_INT) { readHeadroom(power) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readHeadroom(power: PowerManager): Float? =
        power.getThermalHeadroom(HORIZON_SECONDS)
}
