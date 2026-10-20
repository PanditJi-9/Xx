/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * **القنوات الأربع لمعدّل اللمس — منفصلة في النموذج والواجهة والسجل.**
 *
 * وهذا هو جوهر §٧-ب: أربعة أسئلة مختلفة لا يجوز أن تُجمع ولا أن يُستنتج أحدها من الآخر:
 *
 * 1. [TouchChannel.HARDWARE_SAMPLING] — معدّل أخذ العيّنات في وحدة اللمس نفسها. لا يوفّر Android
 *    مسار قراءة عامًّا له، ولا يُخمَّن من اسم الهاتف أو الشريحة؛ يبقى `null` حتى يُسجَّل دليل مقيس.
 * 2. [TouchChannel.DOCUMENTED_LIMIT] — الحدّ الرسميّ المعلَن، **بمصدره ونطاق انطباقه**. ولا يوجد
 *    إدخال موثّق في المستودع اليوم؛ وغياب الإدخال يُعلَن [TouchChannelState.NOT_REGISTERED]
 *    بدل ملء الرقم من جهاز آخر.
 * 3. [TouchChannel.ANDROID_EVENTS] — ما يسلّمه Android إلى الشاشة فعلًا. **قياس** ([TouchRateSample])
 *    لا إعداد، ورفعه لا يثبت أن العتاد يعمل بالمعدّل نفسه.
 * 4. [TouchChannel.EXPERIMENTAL_VENDOR] — أي تغيير خارج الإعدادات الرسمية. لا يُتاح إلا بطريق
 *    كتابة **مُثبَت** في محوّل الشركة؛ وإلا فالحكم [TouchChannelState.REFUSED_CONTRACT] — رفضٌ
 *    بالعقد لا كتابة أعمى (انظر `TouchControlBackend.VendorBridge`).
 *
 * **وحدّ السجلّ:** [TouchRateRegistry] فارغ عن قصد اليوم. إدخالٌ فيه يعني دعوى قدرةٍ على عائلة
 * محكّم؛ فلا يُضاف إلا بدليل مقيس وتوثيق يسمّي المصدر والنطاق. والاختبارات تمنع إدخالًا بلا مصدر.
 */
enum class TouchChannel { HARDWARE_SAMPLING, DOCUMENTED_LIMIT, ANDROID_EVENTS, EXPERIMENTAL_VENDOR }

/**
 * حال القناة — أقلّ ما يمكن قوله بصدق عن كل سؤال.
 *
 * وليست `0` ولا `Unsupported` عامّة: «لا مسار قراءة» ≠ «لم يُقس بعد» ≠ «لا إدخال موثّق» ≠
 * «رُفض بالعقد». ولكلٍّ نصّه في الواجهة كما يراها المستخدم.
 */
enum class TouchChannelState {
    /** قيمة مقيسة أو مسجَّلة بدليل، معروضة بثقتها. */
    MEASURED,

    /** قيمة موثّقة بمصدر (الحدّ الرسميّ). */
    DOCUMENTED,

    /** لا مسار قراءة عامّ لهذا السؤال على هذا النظام — وليس عطبًا عارضًا. */
    NOT_READABLE,

    /** لا إدخال موثّق لهذه العائلة؛ والمجهول يبقى مجهولًا. */
    NOT_REGISTERED,

    /** السؤال لم يُطرح بعد (لا عيّنة) أو عيّنة أقصر من أن تُحسب. */
    NOT_MEASURED,

    /** النقل معلن ولا طريق كتابة مثبَت — **رفض بالعقد**، لا فشل. */
    REFUSED_CONTRACT,

    /** لا محوّل شركة معلن أصلًا على هذا الجهاز. */
    UNAVAILABLE,

    /** طريق كتابة مُثبَت متاح (الوضع التجريبي يبقى مطفأً افتراضيًّا). */
    AVAILABLE,
}

/** ثقة الاكتشاف — تُطلب في §٧-ج لكل وضع، ولا تُطوى في «مدعوم/غير مدعوم». */
enum class TouchConfidence {
    /** مقيس على هذا الجهاز الآن أو على عيّنة هذا الجهاز. */
    MEASURED,

    /** موثّق بمصدر خارجيّ محدّد النطاق. */
    DOCUMENTED,

    /** مجهول: لا دليل بعد، ولا يُخمَّن. */
    NONE,

    /** رفض بالعقد: الطريق غير مثبَت، فلا تُدَّعى قدرة. */
    REFUSED,
}

/** نوع الصلاحية الذي يتطلّبه الوضع — يُعرض بجانب الوضع، لا يُخفى. */
enum class TouchRequirement { NONE, READ_ONLY, ROOT, VENDOR_SERVICE }

/**
 * قراءة قناة واحدة. `value = null` تعني **مجهول** — لا صفرًا ولا قيمةً من قناة أخرى.
 *
 * @param unit وحدة القيمة (`Hz` أو `events/s`)، أو `null` لقناة ليست رقمًا أصلًا.
 * @param source رمز منشأ ثابت (بلا ترجمة) يُترجم في الواجهة.
 * @param note رمز سبب ثابت — ثوابت [TouchRateChannels] — لا جملة عشوائية.
 */
data class TouchChannelReading(
    val channel: TouchChannel,
    val value: Double?,
    val unit: String?,
    val state: TouchChannelState,
    val source: String,
    val confidence: TouchConfidence,
    val requirement: TouchRequirement,
    val note: String,
)

/**
 * إدخال سجلّ موثّق لعائلة محكّم.
 *
 * @param samplingHz المعدّل الأصلي **المقيس** لهذه العائلة (يُدخل بعد قياس، لا بعد قراءة اسم).
 * @param documentedLimitHz الحدّ الرسميّ المعلَن من الشركة المصنّعة.
 * @param documentation مصدر الحدّ ونطاق انطباقه (وثيقة/صفحة/إصدار). إلزاميّ إذا وُجد حدّ.
 */
data class TouchRateRegistryEntry(
    val samplingHz: Int? = null,
    val documentedLimitHz: Int? = null,
    val documentation: String? = null,
)

/**
 * السجلّ الوحيد للقدرات الموثّقة — **فارغ اليوم عن قصد**.
 *
 * ولماذا لا تُملأ الفراغات بأرقام شائعة (240/480/1000): لأن الرقم بلا دليل على هذه العائلة
 * بعينها هو ادّعاء قدرة. ومَن أضاف إدخالًا فعليه أن يسمّي مصدره؛ والاختبار
 * `every documented entry names its source` يمنع إدخالًا ناقصًا في المستقبل.
 */
object TouchRateRegistry {

    val entries: Map<TouchControllerFamily, TouchRateRegistryEntry> = emptyMap()

    fun entry(family: TouchControllerFamily): TouchRateRegistryEntry? = entries[family]
}

/** بناء القنوات الأربع من الأدلة المتاحة — دالّة نقيّة تُقاس على JVM. */
object TouchRateChannels {

    const val UNIT_HZ = "Hz"
    const val UNIT_EVENTS_PER_SECOND = "events/s"

    const val SOURCE_HARDWARE = "touch-hardware"
    const val SOURCE_DOCUMENTED = "documented-registry"
    const val SOURCE_ANDROID = "android-event-sample"
    const val SOURCE_VENDOR = "vendor-touch-service"

    const val NOTE_VERIFIED_ENTRY = "registry-entry-verified"
    const val NOTE_NO_PUBLIC_READ_PATH = "no-public-read-path"
    const val NOTE_NO_DOCUMENTED_ENTRY = "no-documented-entry"
    const val NOTE_NOT_MEASURED = "not-measured"
    const val NOTE_SAMPLE_TOO_SHORT = "sample-too-short"
    const val NOTE_SINGLE_SAMPLE = "single-sample"
    const val NOTE_VENDOR_WRITE_PATH_PROVEN = "vendor-write-path-proven"
    const val NOTE_VENDOR_CONTRACT_UNPROVEN = "vendor-mode-contract-unproven"
    const val NOTE_NO_VENDOR_TRANSPORT = "no-vendor-transport"

    /**
     * المعدّل الأصلي: من سجلّ مقيس **فقط**، وإلا `null` مع سبب صريح.
     * ولا يُشتقّ من [androidEvents] ولا من [documentedLimit] — والاختبار يشهد بذلك.
     */
    fun hardwareSampling(
        family: TouchControllerFamily,
        registry: Map<TouchControllerFamily, TouchRateRegistryEntry> = TouchRateRegistry.entries,
    ): TouchChannelReading {
        val hz = registry[family]?.samplingHz
        return if (hz != null) {
            TouchChannelReading(
                channel = TouchChannel.HARDWARE_SAMPLING,
                value = hz.toDouble(),
                unit = UNIT_HZ,
                state = TouchChannelState.MEASURED,
                source = SOURCE_HARDWARE,
                confidence = TouchConfidence.MEASURED,
                requirement = TouchRequirement.READ_ONLY,
                note = NOTE_VERIFIED_ENTRY,
            )
        } else {
            TouchChannelReading(
                channel = TouchChannel.HARDWARE_SAMPLING,
                value = null,
                unit = UNIT_HZ,
                state = TouchChannelState.NOT_READABLE,
                source = SOURCE_HARDWARE,
                confidence = TouchConfidence.NONE,
                requirement = TouchRequirement.READ_ONLY,
                note = NOTE_NO_PUBLIC_READ_PATH,
            )
        }
    }

    /**
     * الحدّ الموثّق: رقمٌ **بمصدره**، وإلا `null` و[TouchChannelState.NOT_REGISTERED].
     *
     * والحدّ بلا `documentation` لا يُعرض: رقمٌ بلا مصدر لا يمكن التحقق منه ولا تصحيحه.
     */
    fun documentedLimit(
        family: TouchControllerFamily,
        registry: Map<TouchControllerFamily, TouchRateRegistryEntry> = TouchRateRegistry.entries,
    ): TouchChannelReading {
        val entry = registry[family]
        val hz = entry?.documentedLimitHz
        val source = entry?.documentation
        return if (hz != null && !source.isNullOrBlank()) {
            TouchChannelReading(
                channel = TouchChannel.DOCUMENTED_LIMIT,
                value = hz.toDouble(),
                unit = UNIT_HZ,
                state = TouchChannelState.DOCUMENTED,
                source = SOURCE_DOCUMENTED,
                confidence = TouchConfidence.DOCUMENTED,
                requirement = TouchRequirement.NONE,
                note = NOTE_VERIFIED_ENTRY,
            )
        } else {
            TouchChannelReading(
                channel = TouchChannel.DOCUMENTED_LIMIT,
                value = null,
                unit = UNIT_HZ,
                state = TouchChannelState.NOT_REGISTERED,
                source = SOURCE_DOCUMENTED,
                confidence = TouchConfidence.NONE,
                requirement = TouchRequirement.NONE,
                note = NOTE_NO_DOCUMENTED_ENTRY,
            )
        }
    }

    /**
     * أحداث Android: من العيّنة وحدها. والعيّنة القصيرة تُبقى [TouchChannelState.NOT_MEASURED]
     * بسببها — لا تُقرَّب إلى رقم (انظر `TouchRateMath.MIN_DURATION_MS`).
     */
    fun androidEvents(sample: TouchRateSample?): TouchChannelReading {
        val rate = sample?.eventsPerSecond
        val (state, note) = when {
            rate != null -> TouchChannelState.MEASURED to NOTE_SINGLE_SAMPLE
            sample != null -> TouchChannelState.NOT_MEASURED to NOTE_SAMPLE_TOO_SHORT
            else -> TouchChannelState.NOT_MEASURED to NOTE_NOT_MEASURED
        }
        return TouchChannelReading(
            channel = TouchChannel.ANDROID_EVENTS,
            value = rate,
            unit = UNIT_EVENTS_PER_SECOND,
            state = state,
            source = SOURCE_ANDROID,
            confidence = if (rate != null) TouchConfidence.MEASURED else TouchConfidence.NONE,
            requirement = TouchRequirement.NONE,
            note = note,
        )
    }

    /**
     * الوضع التجريبي: متاح **فقط** بطريق كتابة مُثبَت، ومرفوض بالعقد إن كان النقل معلنًا وحده.
     *
     * والقيمة الرقمية `null` دائمًا: هذه قناة قدرة لا معدّل مُدَّعى؛ تطبيق القيمة والتحقق منها
     * واستعادتها مسؤولية [TouchControlBackend.Reconciler] القائم، لا هذا النموذج.
     */
    fun experimentalVendor(
        transportPresent: Boolean,
        writePathProven: Boolean,
    ): TouchChannelReading {
        val (state, confidence, note) = when {
            writePathProven ->
                Triple(TouchChannelState.AVAILABLE, TouchConfidence.MEASURED, NOTE_VENDOR_WRITE_PATH_PROVEN)
            transportPresent ->
                Triple(TouchChannelState.REFUSED_CONTRACT, TouchConfidence.REFUSED, NOTE_VENDOR_CONTRACT_UNPROVEN)
            else ->
                Triple(TouchChannelState.UNAVAILABLE, TouchConfidence.NONE, NOTE_NO_VENDOR_TRANSPORT)
        }
        return TouchChannelReading(
            channel = TouchChannel.EXPERIMENTAL_VENDOR,
            value = null,
            unit = null,
            state = state,
            source = SOURCE_VENDOR,
            confidence = confidence,
            requirement = TouchRequirement.VENDOR_SERVICE,
            note = note,
        )
    }

    /**
     * اللقطة الكاملة: أربع قنوات، بهذا الترتيب، كلٌّ منها بحكمها.
     * ووجودها دالّة واحدة يمنع شاشةً تجمع رقمين في سطر واحد.
     */
    fun snapshot(
        family: TouchControllerFamily,
        sample: TouchRateSample?,
        vendorTransportPresent: Boolean,
        vendorWritePathProven: Boolean,
        registry: Map<TouchControllerFamily, TouchRateRegistryEntry> = TouchRateRegistry.entries,
    ): List<TouchChannelReading> = listOf(
        hardwareSampling(family, registry),
        documentedLimit(family, registry),
        androidEvents(sample),
        experimentalVendor(vendorTransportPresent, vendorWritePathProven),
    )
}
