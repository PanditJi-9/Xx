/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مالك تردّدات الذاكرة وUFS — **تقيس هنا ما يُقاس**: التصنيف، وإثبات الوحدة، وترتيب الكتابة،
 * والاسترجاع، وقصّ الطلب إلى الدرجات المُعلنة.
 *
 * ولماذا هذه الاختبارات JVM صافية: الملفّ الذي تُقاس عليه لا يستورد `android.*` ولا يلمس عقدة
 * حقيقية — كل قراءة وكتابة تمرّ من منفذ [MemoryUfsHardwareBackend.Io] يُستبدل هنا بخريطة.
 * فما يُقاس هو **قواعدنا**: هل نصنّف العقدة صحيحًا؟ هل نرفض الكتابة حين لا تُثبت الوحدة؟ هل
 * نكتب السقف قبل الأرضية حين يجب؟ وما لا يُقاس هنا أبدًا: هل يقبل كيرنل جهازٍ بعينه الكتابة.
 */
class MemoryUfsHardwareBackendTest {

    // ── التصنيف ────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `mediatek dvfsrc and ufshci are classified as memory and storage`() {
        val io = fakeIo(
            "dvfsrc-a" to device(name = "1c00f000.dvfsrc", frequencies = HZ_LADDER),
            "dvfsrc-b" to device(name = "mtk-dvfsrc-devfreq", frequencies = HZ_LADDER),
            "ufs" to device(name = "112b0000.ufshci", frequencies = HZ_LADDER),
        )

        val discovery = MemoryUfsHardwareBackend.discover(io)
        val memory = discovery.of(MemoryUfsHardwareBackend.Kind.MEMORY).sortedBy { it.name }
        val storage = discovery.of(MemoryUfsHardwareBackend.Kind.STORAGE).map { it.name }

        // والاسم المعلن في `device_name` هو ما يُصنَّف، ودليل المجلد هو ما يُقرأ منه.
        assertEquals(listOf("1c00f000.dvfsrc", "mtk-dvfsrc-devfreq"), memory.map { it.name })
        assertEquals(listOf("112b0000.ufshci"), storage)
        assertTrue(memory.all { it.family == MemoryUfsHardwareBackend.Family.MEDIATEK })
        assertEquals("/sys/class/devfreq/dvfsrc-a", memory.first().path)
        assertTrue(discovery.scanned)
    }

    @Test
    fun `qualcomm bandwidth nodes keep their vendor family and are all listed`() {
        val io = fakeIo(
            "cpubw" to device(name = "soc:qcom,cpubw", frequencies = HZ_LADDER),
            "llccbw" to device(name = "soc:qcom,llccbw", frequencies = HZ_LADDER),
            "memlat0" to device(name = "memlat-cpu0", frequencies = HZ_LADDER),
            "memlat4" to device(name = "memlat-cpu4", frequencies = HZ_LADDER),
        )

        val memory = MemoryUfsHardwareBackend.discover(io).of(MemoryUfsHardwareBackend.Kind.MEMORY)

        // أربعة أجهزة ذاكرة على نواة واحدة **لا تُختصر إلى واحد**: كلٌّ يأخذ مقبضه، وإسقاط
        // اثنين كان يعني ترك عنقودين يُسحبان إلى أدنى حالاتهما بلا أن يظهر ذلك.
        assertEquals(4, memory.size)
        // والنسب يُقال حيث يُعرف: عقد عرض النطاق من كوالكوم. وأمّا `memlat` فاسمٌ **مشترك** بين
        // المصنّعين (`memlat-cpu0` عند كوالكوم و`mtk-memlat` عند ميدياتك)، ونسبتُه إلى أحدهما
        // تخمين يُعرَض كمعرفة — فالعائلة `GENERIC` تقول «الوظيفة معلومة والنسب مجهول».
        assertEquals(
            listOf("soc:qcom,cpubw", "soc:qcom,llccbw"),
            memory.filter { it.family == MemoryUfsHardwareBackend.Family.QUALCOMM }.map { it.name },
        )
        assertEquals(
            listOf("memlat-cpu0", "memlat-cpu4"),
            memory.filter { it.family == MemoryUfsHardwareBackend.Family.GENERIC }.map { it.name },
        )
        // وما يُقاس عليه فعلًا هو الصنف نفسه: الأربعة ذاكرة، فالمقبض الأربعة يوجد.
        assertTrue(memory.all { it.kind == MemoryUfsHardwareBackend.Kind.MEMORY })
    }

    @Test
    fun `exynos memory interface is recognised by its own tokens`() {
        val io = fakeIo(
            "devfreq_mif" to device(name = "17000010.devfreq_mif", frequencies = HZ_LADDER),
            "devfreq_int" to device(name = "17000020.devfreq_int", frequencies = HZ_LADDER),
        )

        val memory = MemoryUfsHardwareBackend.discover(io).of(MemoryUfsHardwareBackend.Kind.MEMORY)

        assertEquals(2, memory.size)
        assertTrue(memory.all { it.family == MemoryUfsHardwareBackend.Family.EXYNOS })
    }

    @Test
    fun `a graphics node is never claimed by this owner`() {
        val io = fakeIo(
            "13000000.mali" to device(name = "13000000.mali", frequencies = HZ_LADDER),
            "kgsl-3d0" to device(name = "kgsl-3d0", frequencies = HZ_LADDER),
            "dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER),
        )

        val discovery = MemoryUfsHardwareBackend.discover(io)

        // عقدة GPU لها مالكها (GpuHardwareBackend) ومفتاحها القياسي: مقبضٌ له كاتبان يعني
        // كتابتين تنجرف إحداهما عن الأخرى — وهو ما يمنعه التصنيف هنا لا مراجعةٌ بشريّة.
        assertEquals(listOf("dvfsrc"), discovery.devices.map { it.name })
    }

    @Test
    fun `short tokens match whole words only`() {
        val io = fakeIo(
            "interconnect" to device(name = "interconnect_devfreq", frequencies = HZ_LADDER),
            "printer" to device(name = "printer_devfreq", frequencies = HZ_LADDER),
        )

        // `int` رمز Exynos للذاكرة، لكن مطابقته داخل `interconnect` كانت ستضيف عقدةً بلا صلة.
        // فالمطابقة على حدود الرموز، وهذان الرمزان لا يحملان `int` ككلمة.
        assertTrue(MemoryUfsHardwareBackend.discover(io).devices.isEmpty())
    }

    @Test
    fun `an unreadable root proves no absence`() {
        // الفرق بين «عدّدتُ فلم أجد» و«لم أستطع العدّ» هو الفرق بين قدرة غائبة وقياس ناقص.
        val discovery = MemoryUfsHardwareBackend.discover(FakeIo(emptyMap(), rootUnreadable = true))

        assertFalse("غياب الأجهزة لا يُدَّعى من فشل قراءة", discovery.scanned)
        assertEquals(MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE, discovery.reason)
        assertTrue(discovery.devices.isEmpty())
    }

    @Test
    fun `a kernel with no devfreq class is a measured absence, not a read failure`() {
        // والشقّ الثاني من التفريق نفسه: نواة لا تنشر `/sys/class/devfreq` أصلًا **قيس** غيابها
        // (سؤال الوجود أجاب «لا»)، فلا يُقال لصاحبها «لم أستطع العدّ» ويُطلب منه جذرٌ لا يُغيّر
        // شيئًا. وكان الرمزان واحدًا، فكانت قدرةٌ غائبة تُقرأ «نقص قياس» فيُطلب لها ما لا ينفع.
        val discovery = MemoryUfsHardwareBackend.discover(fakeIo())

        assertTrue("الغياب المقيس يُعلَن مقيسًا", discovery.scanned)
        assertEquals(MemoryUfsHardwareBackend.REASON_ROOT_ABSENT, discovery.reason)
        assertTrue(discovery.devices.isEmpty())
        assertTrue(discovery.unreadable.isEmpty())
    }

    @Test
    fun `a listed node that answers nothing is a gap, not a node without a frequency interface`() {
        // عقدة ظهرت في التعديد ولم يُقرأ منها سطر: الحكم **نقص قياس يُعلَن**، ولا يُنسب إليها
        // «ليست واجهة تردّد» ولا تُعرَض للتعليم. وسببُ هذه الحالة في الواقع حجبُ عبور مجلّد أب —
        // وهو بعينه السبب الذي جعل قسمًا في المشروع المرجعيّ يبدو غير مدعوم بعد أن ظهر فارغًا.
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER), "opaque" to emptyMap())

        val discovery = MemoryUfsHardwareBackend.discover(io)

        assertEquals(listOf("opaque"), discovery.unreadable)
        assertEquals("المقروء وحده يُعدّ جهازًا", listOf("dvfsrc"), discovery.devices.map { it.name })
        assertTrue("وما لم يُقرأ لا يُصنَّف مجهولًا يُعلَّم", discovery.unclassified.isEmpty())
        // والفحص يقول الرمز نفسه: عقدة لم تُقرأ لا تصل إلى التعليم، وسببُ رفضها يُسمّى.
        val probe = MemoryUfsHardwareBackend.probe("opaque", MemoryUfsHardwareBackend.Kind.STORAGE, io, emptyList())
        assertEquals(
            MemoryUfsHardwareBackend.PROBE_UNREADABLE,
            (probe as MemoryUfsHardwareBackend.Probe.Refused).reason,
        )
    }

    // ── الوحدة والطلب ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `a kilohertz ladder infers kilohertz and keeps raw node values`() {
        val device = MemoryUfsHardwareBackend.discover(
            fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = KHZ_LADDER)),
        ).devices.single()

        assertEquals(GpuHardwareBackend.FrequencyUnit.KHZ, device.frequencyUnit)
        assertTrue(device.unitTrusted)
        assertEquals(1_350L, MemoryUfsHardwareBackend.megahertz(device, 1_350_000L))
    }

    @Test
    fun `an ambiguous unit makes the device read-only and refuses every write`() {
        // سلّم مختلط المقادير: لا وحدة واحدة تجمعه، فالأمان أن يُقرأ ولا يُكتب. وكتابة Hz في
        // عقدة تتكلّم kHz هي العطب الذي فسد به `min_freq` على كل جهاز لا يستعمل Hz.
        val device = MemoryUfsHardwareBackend.discover(
            fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = listOf(300_000L, 1_200_000_000L))),
        ).devices.single()

        assertFalse(device.unitTrusted)
        assertEquals(MemoryUfsHardwareBackend.Level.READ_ONLY, device.level)
        assertEquals(
            "frequency-unit-unproven",
            MemoryUfsHardwareBackend.validate(device, MemoryUfsHardwareBackend.Request(maxFreq = 300_000L)),
        )
    }

    @Test
    fun `a single-step ladder is not a range`() {
        val device = MemoryUfsHardwareBackend.discover(
            fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = listOf(1_000_000_000L))),
        ).devices.single()

        assertEquals(MemoryUfsHardwareBackend.Level.READ_ONLY, device.level)
        assertEquals(
            "no-published-ladder",
            MemoryUfsHardwareBackend.validate(device, MemoryUfsHardwareBackend.Request(maxFreq = 1_000_000_000L)),
        )
    }

    @Test
    fun `a fraction never invents a step that the device did not publish`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()

        // ٧٥٪ من ٢٠٠٠MHz = ١٥٠٠ وهي في السلّم؛ و٩٠٪ = ١٨٠٠ وليست فيه ⇒ ١٥٠٠ (أقرب درجة
        // معلنة لا تتجاوز الطلب)، وليست قيمةً مُخترعة بين الدرجتين.
        assertEquals(1_500_000_000L, MemoryUfsHardwareBackend.nodeForFraction(device, 0.75f))
        assertEquals(1_500_000_000L, MemoryUfsHardwareBackend.nodeForFraction(device, 0.90f))
        assertEquals(300_000_000L, MemoryUfsHardwareBackend.nodeForFraction(device, 0.01f))
    }

    @Test
    fun `a floor above the requested ceiling is clipped, not rejected blindly`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()

        val request = MemoryUfsHardwareBackend.requestForFraction(device, fraction = 0.2f, floorFraction = 0.9f)

        // الطلب يبقى، والأرضية تُقصّ إلى السقف بدل أن تُسقط الطلب كلّه.
        assertEquals(MemoryUfsHardwareBackend.nodeForFraction(device, 0.2f), request?.maxFreq)
        assertEquals("الأرضية تُقصّ إلى السقف", request?.maxFreq, request?.minFreq)
        // وطلبٌ بلا أرضية لا يُخترع له حدّ سفليّ.
        assertNull(MemoryUfsHardwareBackend.requestForFraction(device, fraction = 0.2f)?.minFreq)
    }

    // ── الكتابة والتحقق ────────────────────────────────────────────────────────────────────────

    @Test
    fun `raising the floor writes the ceiling first`() {
        // رفع الأرضية فوق السقف الحيّ يرفضه السائق (المدى يصير مقلوبًا لحظةً) — فالترتيب آمن
        // وليس تفصيلًا: بلا هذا السطر تفشل كتابة صحيحة على أجهزة لا تتسامح مع المدى المقلوب.
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER, min = 300_000_000L, max = 600_000_000L))
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()

        // والقيم **من السلّم المُعلن**: الطلب بقيمة لا وجود لها يُرفض قبل أي كتابة (وهو ما أظهره
        // تشغيل هذا الاختبار: قيمة 1GHz ليست في السلّم فسقط الاختبار على `validate` لا على الترتيب).
        val result = MemoryUfsHardwareBackend.applyValidated(
            device,
            MemoryUfsHardwareBackend.Request(minFreq = 1_200_000_000L, maxFreq = 2_000_000_000L),
            io,
        )

        assertTrue(result.verified)
        assertEquals(listOf("max", "min"), io.writeOrder)
        assertEquals(2_000_000_000L, io.maxNode("dvfsrc"))
        assertEquals(1_200_000_000L, io.minNode("dvfsrc"))
    }

    @Test
    fun `a kernel that ignores the write is reported unverified and the baseline is restored`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER, min = 300_000_000L, max = 2_000_000_000L))
        io.sticky = true
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()

        val result = MemoryUfsHardwareBackend.applyValidated(
            device,
            MemoryUfsHardwareBackend.Request(minFreq = 300_000_000L, maxFreq = 600_000_000L),
            io,
        )

        assertFalse(result.verified)
        assertTrue(result.rollbackAttempted)
        assertTrue("الاسترجاع يُقاس بقراءة لا يُعلن", result.rollbackVerified == true)
        assertEquals(2_000_000_000L, io.maxNode("dvfsrc"))
    }

    @Test
    fun `an unadvertised value is refused before any write`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()

        val result = MemoryUfsHardwareBackend.applyValidated(
            device,
            MemoryUfsHardwareBackend.Request(maxFreq = 1_234_567_890L),
            io,
        )

        assertEquals("max-not-advertised", result.error)
        assertFalse(result.writeSucceeded)
        assertTrue(io.writeOrder.isEmpty())
    }

    @Test
    fun `satisfied means the live range sits inside the request, and zero is never a reading`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))
        val device = MemoryUfsHardwareBackend.discover(io).devices.single()
        val requested = MemoryUfsHardwareBackend.Request(minFreq = 300_000_000L, maxFreq = 1_200_000_000L)

        // الاحتواء لا التساوي: السائق يضيّق المدى لأسباب مشروعة، ومدى أضيق تلبيةٌ لا فشل.
        assertTrue(
            MemoryUfsHardwareBackend.satisfied(requested, device.copy(minFreq = 300_000_000L, maxFreq = 1_200_000_000L)),
        )
        assertTrue(
            "سقفٌ أدنى من المطلوب يلبّي «لا تتجاوز»",
            MemoryUfsHardwareBackend.satisfied(requested, device.copy(minFreq = 600_000_000L, maxFreq = 800_000_000L)),
        )
        // وما تجاوز الطلب أو نزل تحت أرضيته ليس تلبية.
        assertFalse(MemoryUfsHardwareBackend.satisfied(requested, device.copy(maxFreq = 2_000_000_000L)))
        assertFalse(MemoryUfsHardwareBackend.satisfied(requested, device.copy(minFreq = 100_000_000L)))
        // ولا صفرٌ ولا قراءة غائبة: الصفر في عقدة تردّد إمّا «لا قياس» وإمّا «أُطفئ»، ولا يُدّعى أيّهما.
        assertFalse(MemoryUfsHardwareBackend.satisfied(requested, device.copy(minFreq = 0L)))
        assertFalse(MemoryUfsHardwareBackend.satisfied(requested, device.copy(maxFreq = null)))
        assertFalse(MemoryUfsHardwareBackend.satisfied(requested, null))
        // وطلبٌ فيه حاكمٌ وحده يُحكم بالحاكم وحده.
        assertTrue(
            MemoryUfsHardwareBackend.satisfied(
                MemoryUfsHardwareBackend.Request(governor = "simple_ondemand"),
                device,
            ),
        )
    }

    // ── الترميز والعقود ────────────────────────────────────────────────────────────────────────

    @Test
    fun `the encoded request round-trips through the shared decoder`() {
        val request = MemoryUfsHardwareBackend.Request(minFreq = 300_000_000L, maxFreq = 1_200_000_000L)
        val encoded = MemoryUfsHardwareBackend.encodeRequest(request)

        assertEquals("300000000:1200000000|-", encoded)
        val parsed = DevfreqEncoding.parseRequest(encoded)
        assertEquals(request, parsed)
        assertEquals(300_000_000L, DevfreqEncoding.floor(encoded))
        assertEquals(1_200_000_000L, DevfreqEncoding.ceiling(encoded))
        assertNull(DevfreqEncoding.governor(encoded))
    }

    @Test
    fun `an empty encoding is not a request`() {
        assertNull(DevfreqEncoding.parseRequest(":|--"))
        assertNull(DevfreqEncoding.parseRequest(":|"))
        assertNull(DevfreqEncoding.parseBaseline("relative/path", "1:2|-"))
    }

    @Test
    fun `contained accepts a ceiling below the request and refuses a governor mismatch`() {
        val desired = "300000000:1200000000|simple_ondemand"

        assertTrue(DevfreqCeilingContracts.contained(desired, "300000000:800000000|simple_ondemand"))
        assertFalse(DevfreqCeilingContracts.contained(desired, "300000000:1500000000|simple_ondemand"))
        assertFalse("حاكم آخر ليس تلبية", DevfreqCeilingContracts.contained(desired, "300000000:800000000|performance"))
        // ورفع المصنّع للأرضية فوق طلبنا ليس فشلًا (سياسة السائق تفعل ذلك)، وأرضيةٌ **أدنى** من
        // المطلوبة فشلٌ حقيقيّ: الطلب كان «لا تنزل تحت هذا».
        assertTrue(DevfreqCeilingContracts.contained(desired, "400000000:800000000|simple_ondemand"))
        assertFalse(DevfreqCeilingContracts.contained(desired, "100000000:800000000|simple_ondemand"))
    }

    @Test
    fun `reached is the write gate - a governor request is never skipped as already satisfied`() {
        val ceilingOnly = "300000000:1200000000|-"
        assertTrue(DevfreqCeilingContracts.reached(ceilingOnly, "300000000:1500000000|-"))
        assertFalse("يجب أن تُكتب قيمة أعلى: القيمة الحالية هي رقم كتبناه نحن", DevfreqCeilingContracts.reached(ceilingOnly, "300000000:800000000|-"))

        val withGovernor = "300000000:1200000000|performance"
        // تخطّي الكتابة هنا يترك الحاكم المطلوب بلا كتابة أبدًا — وهو العطب المُصرَّح به في
        // `HardwareVerification.ceilingReached`: حكم السقف متسامح فتُقرأ قيمتنا السابقة "مُلبّاة".
        assertFalse(DevfreqCeilingContracts.reached(withGovernor, "300000000:1500000000|simple_ondemand"))
        assertTrue(DevfreqCeilingContracts.reached(withGovernor, "300000000:1500000000|performance"))
    }

    @Test
    fun `a read only projection cannot write even through the mutable seam`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))

        val observed = MemoryUfsHardwareBackend.observe(io)

        assertEquals(1, observed.devices.size)
        assertTrue("الاكتشاف لا يكتب", io.writeOrder.isEmpty())
    }

    // ── تصنيف المستخدم (تعليم الأجهزة) ─────────────────────────────────────────────────────────

    @Test
    fun `a node whose name no rule knows is reported instead of dropped`() {
        val io = fakeIo("mtk-memnoc" to device(name = "mtk-memnoc", frequencies = HZ_LADDER))

        val discovery = MemoryUfsHardwareBackend.discover(io)

        assertTrue("لا جهاز مُصنَّف", discovery.devices.isEmpty())
        assertEquals(listOf("mtk-memnoc"), discovery.unclassified.map { it.directory })
        val entry = discovery.unclassified.single()
        assertEquals("mtk-memnoc", entry.label)
        assertEquals(HZ_LADDER.size, entry.ladderSize)
        assertTrue("الحدّان قابلان للكتابة: التعليم هنا مجدٍ", entry.writable)
    }

    @Test
    fun `a node with no frequency interface is not offered for teaching`() {
        // عقدة في المجلد بلا سلّم ولا حدود ولا ساعة: لا شيء يُعلَّم ولا يُعرض. وهي **مقروءة**
        // (`device_name` منها يجيب)، فالفرق بينها وبين العقدة المحجوبة في الاختبار الآخر مقيسٌ
        // لا مُدَّعى: هذه حكمٌ على محتواها، وتلك نقصُ قياسٍ فيها.
        val io = fakeIo("mystery" to mapOf("device_name" to "mystery"))

        val discovery = MemoryUfsHardwareBackend.discover(io)

        assertTrue(discovery.unclassified.isEmpty())
        assertTrue(discovery.devices.isEmpty())
        assertTrue("وما قُرئ لا يُقال عنه «غير مقروء»", discovery.unreadable.isEmpty())
    }

    @Test
    fun `a graphics node is owned elsewhere, and teaching cannot take it`() {
        val io = fakeIo("13000000.mali" to device(name = "13000000.mali", frequencies = HZ_LADDER))

        val discovery = MemoryUfsHardwareBackend.discover(io)

        assertEquals(listOf("13000000.mali"), discovery.graphicsOwned)
        assertTrue("عقدة GPU لا تُعرَض كأنها مجهولة", discovery.unclassified.isEmpty())

        // وحتى لو مرّر منادٍ سطرًا مبرمَجًا للاسم نفسه فالمانع داخل القراءة لا في الملفّ وحده.
        val forced = MemoryUfsHardwareBackend.discover(
            io,
            listOf(HardwareDeviceOverlay.Entry("13000000.mali", MemoryUfsHardwareBackend.Kind.MEMORY)),
        )
        assertTrue(forced.devices.isEmpty())
        assertEquals(listOf("13000000.mali"), forced.graphicsOwned)
    }

    @Test
    fun `a taught node becomes a first class memory device`() {
        val io = fakeIo("mtk-memnoc" to device(name = "mtk-memnoc", frequencies = HZ_LADDER))
        val taught = listOf(HardwareDeviceOverlay.Entry("mtk-memnoc", MemoryUfsHardwareBackend.Kind.MEMORY))

        val discovery = MemoryUfsHardwareBackend.discover(io, taught)

        val device = discovery.devices.single()
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, device.kind)
        assertTrue("مصدر التصنيف يُعلن", device.userTaught)
        assertTrue(device.evidence.contains("user-taught"))
        assertTrue(device.rangeWritable)
        assertTrue(discovery.unclassified.isEmpty())
        // والمفتاح القياسي نفسه: التعليم يوسّع التصنيف، ولا يخترع مقبضًا ثانيًا.
        assertEquals("mem_frequency:mtk-memnoc", HardwareControlKey.devfreqFrequency(device.kind, device.name))
        assertTrue(AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.devfreqFrequency(device.kind, device.name)))
    }

    @Test
    fun `a reviewed rule wins over a taught line`() {
        val io = fakeIo("dvfsrc" to device(name = "dvfsrc", frequencies = HZ_LADDER))

        val discovery = MemoryUfsHardwareBackend.discover(
            io,
            listOf(HardwareDeviceOverlay.Entry("dvfsrc", MemoryUfsHardwareBackend.Kind.STORAGE)),
        )

        val device = discovery.devices.single()
        assertEquals("القاعدة المبنية تفوز دائمًا", MemoryUfsHardwareBackend.Kind.MEMORY, device.kind)
        assertFalse(device.userTaught)
        assertFalse(device.evidence.contains("user-taught"))
    }

    @Test
    fun `the probe accepts a node only when a frequency ladder is read from it`() {
        val io = fakeIo("mtk-memnoc" to device(name = "mtk-memnoc", frequencies = HZ_LADDER))

        val ok = MemoryUfsHardwareBackend.probe(
            "mtk-memnoc",
            MemoryUfsHardwareBackend.Kind.MEMORY,
            io,
            emptyList(),
        )
        val missing = MemoryUfsHardwareBackend.probe(
            "mystery",
            MemoryUfsHardwareBackend.Kind.MEMORY,
            io,
            emptyList(),
        )

        assertTrue(ok is MemoryUfsHardwareBackend.Probe.Ok)
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, (ok as MemoryUfsHardwareBackend.Probe.Ok).device.kind)
        // واسمٌ لم يُقرأ منه شيء ليس «عقدة ليست واجهة تردّد» بل **نقص قياس**: كان الرمز واحدًا،
        // فيُقرأ العطب «العقدة ليست واجهة» وهي لم تُقرأ أصلًا — والرّمان مطلوبان مختلفان.
        assertEquals(
            MemoryUfsHardwareBackend.PROBE_UNREADABLE,
            (missing as MemoryUfsHardwareBackend.Probe.Refused).reason,
        )
        assertTrue("الفحص قراءة لا كتابة", io.writeOrder.isEmpty())
    }

    @Test
    fun `a readable node that publishes no frequency is refused as not a frequency interface`() {
        // والشقّ الآخر من التفريق: عقدة **أجابت** (نقرأ منها `device_name`) ولا تنشر تردّدًا ⇒ حكمٌ
        // مقيس بأنها ليست واجهة تردّد. وهذا وحده ما يجوز أن يُقال «ليست واجهة».
        val io = fakeIo("oddity" to mapOf("device_name" to "oddity"))

        val probe = MemoryUfsHardwareBackend.probe(
            "oddity",
            MemoryUfsHardwareBackend.Kind.MEMORY,
            io,
            emptyList(),
        )

        assertEquals(
            MemoryUfsHardwareBackend.PROBE_NOT_INTERFACE,
            (probe as MemoryUfsHardwareBackend.Probe.Refused).reason,
        )
    }

    @Test
    fun `the probe refuses a graphics name and a name that is not one segment`() {
        val io = fakeIo("13000000.mali" to device(name = "13000000.mali", frequencies = HZ_LADDER))

        val graphics = MemoryUfsHardwareBackend.probe(
            "13000000.mali",
            MemoryUfsHardwareBackend.Kind.MEMORY,
            io,
            emptyList(),
        )
        val malformed = MemoryUfsHardwareBackend.probe(
            "a/b",
            MemoryUfsHardwareBackend.Kind.MEMORY,
            io,
            emptyList(),
        )

        assertEquals(
            HardwareDeviceOverlay.REASON_GRAPHICS_OWNED,
            (graphics as MemoryUfsHardwareBackend.Probe.Refused).reason,
        )
        assertEquals(
            MemoryUfsHardwareBackend.PROBE_NAME_INVALID,
            (malformed as MemoryUfsHardwareBackend.Probe.Refused).reason,
        )
    }

    @Test
    fun `the installed classification is what every caller sees - discovery and refresh alike`() {
        val io = fakeIo("mtk-memnoc" to device(name = "mtk-memnoc", frequencies = HZ_LADDER))
        MemoryUfsHardwareBackend.installOverlay(
            listOf(HardwareDeviceOverlay.Entry("mtk-memnoc", MemoryUfsHardwareBackend.Kind.MEMORY)),
        )
        try {
            // الاكتشاف وخريطة القدرة والفحص يقرؤون التصنيف المُشغَّل بلا وسيط يُنسى في موضع.
            assertEquals(1, MemoryUfsHardwareBackend.discover(io).devices.size)
            // و`refresh` يقرأ التصنيف نفسه: لو تجاهله لصار الجهاز مُعلَّمًا في القائمة و«غير
            // موجود» عند الكتابة — وهذا هو العطب الذي قُصد منعُه بجعل الحالة واحدة.
            assertEquals(
                MemoryUfsHardwareBackend.Kind.MEMORY,
                MemoryUfsHardwareBackend.refresh("/sys/class/devfreq/mtk-memnoc", io)?.kind,
            )
        } finally {
            MemoryUfsHardwareBackend.installOverlay(emptyList())
        }
    }

    // ── أدوات الاختبار ─────────────────────────────────────────────────────────────────────────

    /** جهاز `devfreq` مصطنع: الملفّات التي يقرأها المالك، لا أكثر. */
    private fun device(
        name: String,
        frequencies: List<Long>,
        min: Long? = null,
        max: Long? = null,
        current: Long? = null,
        governor: String = "simple_ondemand",
        governors: String = "simple_ondemand performance",
    ): Map<String, String> = mapOf(
        "device_name" to name,
        "available_frequencies" to frequencies.joinToString(" "),
        "min_freq" to ((min ?: frequencies.firstOrNull() ?: 0L).toString()),
        "max_freq" to ((max ?: frequencies.lastOrNull() ?: 0L).toString()),
        "cur_freq" to ((current ?: frequencies.firstOrNull() ?: 0L).toString()),
        "governor" to governor,
        "available_governors" to governors,
    )

    private fun fakeIo(vararg devices: Pair<String, Map<String, String>>): FakeIo =
        FakeIo(devices.toMap())

    private class FakeIo(
        private val devices: Map<String, Map<String, String>>,
        /**
         * الجذر **موجود ولا يُعدَّد** — الحالة التي تفرّق بين «فارغ» و«محجوب».
         *
         * وهي تُصرَّح ولا تُستنتج من غياب العُقَد: `fakeIo()` بلا أجهزة يعني جذرًا **غير موجود**
         * (نواة بلا صنف devfreq)، وهو قياسٌ آخر وتقريره آخر — وهذا الفرق نفسه هو ما يقيسه
         * `a kernel with no devfreq class is a measured absence`.
         */
        private val rootUnreadable: Boolean = false,
    ) : MemoryUfsHardwareBackend.Io {

        /** الأوراق المكتوبة الآن، مفصولةً بالاسم — فالقراءة المرتجعة تقرأ ما كُتب فعلًا. */
        private val nodes = mutableMapOf<String, String>()

        /** ترتيب أسماء الحقول كما كُتبت: به يُقاس ترتيب الكتابة. */
        val writeOrder = mutableListOf<String>()

        /** `true` = النواة تتجاهل الكتابة (تقرأ ما كان قبلها) — لقياس مسار الفشل والاسترجاع. */
        var sticky: Boolean = false

        override fun exists(path: String): Boolean =
            if (path == "/sys/class/devfreq") rootUnreadable || devices.isNotEmpty() else valueOf(path) != null

        override fun read(path: String): String? = valueOf(path)

        override fun listDirectories(path: String): List<String> =
            if (path == "/sys/class/devfreq") devices.keys.sorted() else emptyList()

        override fun writable(path: String): Boolean = path.endsWith("_freq") || path.endsWith("governor")

        override fun write(path: String, value: String): Boolean {
            // يُسجَّل **اسم الحقل** لا مساره: به يُقاس الترتيب (سقف قبل أرضية حين يجب).
            writeOrder += path.substringAfterLast('/').removeSuffix("_freq")
            if (!sticky) nodes[path] = value
            return true
        }

        fun maxNode(directory: String): Long? = read("/sys/class/devfreq/$directory/max_freq")?.toLongOrNull()

        fun minNode(directory: String): Long? = read("/sys/class/devfreq/$directory/min_freq")?.toLongOrNull()

        private fun valueOf(path: String): String? {
            nodes[path]?.let { return it }
            val directory = path.substringAfterLast('/', missingDelimiterValue = "")
            val parent = path.substringBeforeLast('/').substringAfterLast('/')
            if (parent !in devices || directory.isEmpty()) return null
            return devices.getValue(parent)[directory]
        }
    }

    private companion object {
        // سلّم بالهرتز كما تنشره النواة العامة، وفيه ١٨٠٠ مفقودة عن قصد ليقيس القصّ.
        val HZ_LADDER = listOf(300_000_000L, 600_000_000L, 1_200_000_000L, 1_500_000_000L, 2_000_000_000L)

        // السلّم نفسه بالكيلوهرتز — للتأكد أن الوحدة تُستنتج ولا تُفترض.
        val KHZ_LADDER = listOf(300_000L, 600_000L, 1_200_000L, 1_350_000L, 2_000_000L)
    }
}
