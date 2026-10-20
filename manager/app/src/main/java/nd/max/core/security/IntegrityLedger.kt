/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

/**
 * سجلّ الأدلّة — **منطق خالص** يُقاس في JVM، ولا يعرف شيئًا عن Android.
 *
 * وهو **ثابت (sticky) عن قصد**: من رصد التلاعب مرّةً لا يعود إلى «سليم» إن اختفت الإشارة
 * بعدها. وسبب ذلك مقيس لا نظريّ: أشهر طريقة لتجاوز فحص محلي هي **إخفاء ما يراه الفحص**
 * (إزالة ملفّ، إخفاء خريطة ذاكرة، إخفاء اسم حزمة) لا إلغاء الفحص نفسه — فلو كان الحكم
 * يُعاد حسابه من الصفر كل مرّة، لكفى المهاجم أن يُخفي الأثر أثناء الفحص الثاني فيعود
 * التطبيق «سليمًا». والسجلّ الثابت يقطع هذا الطريق: الإشارة تُسجَّل لحظة رؤيتها وتبقى.
 *
 * **فيه لا تُحسب الإشارات الودّية أبدًا** (جذور · KernelSU · روم مخصّص · SELinux · خيارات
 * المطوّر · بيانات المستخدم). وهذا مُثبَّت في [NEVER_EVIDENCE] كي **يُقاس** كما يُقاس أي
 * عقد آخر — لا كي يُوصف في تعليق.
 */
class IntegrityLedger(private val softThreshold: Int = DEFAULT_SOFT_THRESHOLD) {

    private val findings = LinkedHashSet<IntegrityFinding>()

    @Volatile
    private var stickyVerdict: IntegrityVerdict = IntegrityVerdict.CLEAN

    /** كم دليلًا **مختلفًا** (أو إشارة متكرّرة في موضع مختلف) يلزم للحكم بالتلاعب. */
    val threshold: Int get() = softThreshold

    /**
     * يُسجّل دليلًا ويُعيد الحكم الجاري.
     *
     * **ووحدة العدّ هي الإشارة لا الموضع:** `TRACER_ATTACHED` مع `HOOK_FRAMEWORK_MAPPED`
     * دليلان (إشارتان مختلفتان)، و`TRACER_ATTACHED` في أربعة مواضع **دليل واحد**
     * (إشارة واحدة رُصدت أربع مرّات). والتكرار في [findings] يُزال بالمفتاح
     * [IntegrityFinding.key] فلا يُخزَّن الدليل نفسه مرّتين — وإزالة التكرار شيء،
     * والعدّ شيء آخر، وقد اختلطا هنا قبل أن يُفصلا (انظر [evaluate]).
     */
    @Synchronized
    fun record(finding: IntegrityFinding): IntegrityVerdict {
        findings.add(finding)
        val verdict = evaluate()
        // الثبات: لا يعود الحكم إلى ما هو أهون أبدًا.
        if (verdict.ordinal > stickyVerdict.ordinal) stickyVerdict = verdict
        return stickyVerdict
    }

    /** كل الأدلّة المرصودة، مرتّبة بترتيب رصدها. */
    @Synchronized
    fun all(): List<IntegrityFinding> = findings.toList()

    /** الحكم الجاري — أكبر ما بلغه السجلّ، لا ما تُنتجه آخر إشارة. */
    @Synchronized
    fun verdict(): IntegrityVerdict {
        val fresh = evaluate()
        if (fresh.ordinal > stickyVerdict.ordinal) stickyVerdict = fresh
        return stickyVerdict
    }

    /** يُعيد السجلّ إلى حاله الأوّل — يُستعمل في الاختبارات وحدها، ولا يُنادى في الإنتاج. */
    @Synchronized
    fun reset() {
        findings.clear()
        stickyVerdict = IntegrityVerdict.CLEAN
    }

    private fun evaluate(): IntegrityVerdict {
        if (findings.any { it.signal.hard }) return IntegrityVerdict.TAMPERED
        // والعدّ على **الإشارات المختلفة** لا على المواضع: إشارةٌ واحدة تُرى في أربعة
        // مواضع ليست أربعة أدلّة، بل دليل واحد رُصد أربع مرّات. ولو عُدّت المواضع لأصبح
        // مستخدمٌ سطحه يحمل `libsubstrate` لتطبيق آخر مرفوضًا بلا ذنب — وهو الثمن الذي
        // ترفضه هذه الطبقة صراحةً (انظر NEVER_EVIDENCE وما حولها).
        val distinctSignals = findings.map { it.signal }.distinct().size
        return when {
            distinctSignals >= softThreshold -> IntegrityVerdict.TAMPERED
            distinctSignals > 0 -> IntegrityVerdict.SUSPECTED
            else -> IntegrityVerdict.CLEAN
        }
    }

    companion object {
        /**
         * العتبة الافتراضية = ٢. وليست ١ لسببين مقيسين: مصحّح تجريبيّ على جهاز يخصّ
         * مستخدمنا يفتح خيارات المطوّر، وسطح هوك مثبَّت لتطبيق آخر يُظهر اسمه في خريطتنا.
         * وليست ٣ لأن ذلك يُهدر تصديقًا مشروعًا واحدًا في المقابل بلا مقابل.
         */
        const val DEFAULT_SOFT_THRESHOLD = 2

        /**
         * حالات مشروعة **لا تُنتج دليلًا مهما كانت**.
         *
         * وهذه قائمة أسماء مقروءة تُستعمل في التوثيق والاختبار معًا: الاختبار
         * (`IntegrityLedgerTest`) يمرّ عليها كلّها ويثبت أن السجلّ يبقى `CLEAN`. فالقاعدة
         * التي لا يُقيسها اختبار تتحوّل بمرور الجولات إلى تعليق لا يحمي شيئًا — وهذا
         * صنف العطب الذي وقع في هذا المستودع قبل ذلك (مسبار يقول «٧١ موضعًا» وكله زائف).
         */
        val NEVER_EVIDENCE: List<String> = listOf(
            "root",              // وجود su/جذور مقصود ومطلوب للمنتج نفسه
            "kernelsu",          // منصّة جذور بديلة — المنتج يدعمها
            "magisk",            // منصّة جذور — المنتج يدعمها
            "custom-rom",        // روم مخصّص: كل أجهزة المالك المستهدفة تقريبًا
            "selinux-permissive", // حالة نواة، لا تعديل على تطبيقنا
            "developer-options", // خيارات المطوّر مفتوحة
            "adb-enabled",       // تصحيح USB مفعّل
            "user-data",         // بيانات المستخدم وإعداداته: تتغيّر بحكم الاستعمال
            "device-identity",   // ما يزيّفه المستخدم من هوية/خصائص على جهازه
            "battery-or-thermal", // حالات الجهاز الطبيعيّة (شحن، سخونة، توفير طاقة)
        )

        /** أسماء الأصناف التي تُعدّ جسر هوك على مستوى Java — تُقرأ بالاسم لا بالنمط العشوائي. */
        val JAVA_HOOK_CLASSES: List<String> = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook",
            "org.lsposed.lspd.core.Main",
            "com.saurik.substrate.MS",
            "re.frida.server.FridaServer",
        )

        /** أسماء تظهر في `/proc/self/maps` — مكتبات ريثر وإطارات هوك معروفة. */
        val HOOK_LIBRARY_MARKERS: List<String> = listOf(
            "frida-agent",
            "frida-gadget",
            "libsubstrate",
            "libxposed",
            "liblspd",
            "libepic",
            "libsandhook",
            "libdobby",
            "libwhale",
            "libriru",
            "libpine",
        )
    }
}
