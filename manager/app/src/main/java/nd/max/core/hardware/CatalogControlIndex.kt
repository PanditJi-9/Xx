/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.hardware

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * مدخل كتالوج واحد كما يُقرأ من `assets/control_catalog.json` (M1 · ADR-87).
 *
 * **ولماذا قارئ لا مولّد Kotlin:** الكتالوج بيانات، والقراءة منه في وقت التشغيل تجعل مدخلًا جديدًا
 * إضافةَ صفٍّ لا إعادةَ بناء صنف. والمولَّد كان سيضيف مرآةً ثانية تنزاح — وهو العطب الذي يمنعه ADR-87.
 */
internal data class CatalogControl(
    val key: String,
    val transport: String,
    val candidates: List<String>,
    val writeTiers: List<String>,
    val danger: String,
    val readOnlyReason: String? = null,
    /**
     * نوع القيمة المعلَن (`INT` · `PERCENT` · `FREQUENCY_RANGE` …) — يقرؤه محرّك الوصفات ليرفض
     * قيمةً لا تناسب عقدتها قبل الكتابة. افتراضيّ فارغ كي لا ينكسر بناءُ مدخلٍ قديم بلا هذا الحقل.
     */
    val kind: String = "",
) {
    /** `PROTECTED` لا يُكتب بأيّ طبقة — يُقرأ ولا يُمسّ (خطّة M1 §3). */
    val isProtected: Boolean get() = danger == DANGER_PROTECTED

    /** هل يُعلن المدخل كتابةً مشروعة؟ (قائمة طبقات غير فارغة، وغير محميّ) */
    val writeDeclared: Boolean get() = writeTiers.isNotEmpty() && !isProtected

    companion object {
        const val DANGER_PROTECTED = "PROTECTED"
    }
}

/**
 * فهرس الكتالوج في الذاكرة: المطابقة بين ميزة عتاد ومدخلاتها، والمسارات التي تُقاس فعلًا.
 *
 * **الحدّ المعلن:** هذا الفهرس لا يعرف جهازًا ولا يكتب شيئًا. يقرأ بيانات ويجيب عن سؤالين:
 * أيّ مدخل يخصّ هذه الميزة؟ وأيّ مرشّح منها **عقدة حقيقيّة قابلة للقياس**؟
 */
internal class CatalogControlIndex private constructor(val entries: List<CatalogControl>) {

    /** مدخلات ميزة واحدة — بترتيب الكتالوج، فلا يتغيّر الحكم بتغيّر ترتيب `HashMap`. */
    fun controlsFor(feature: HardwareFeature): List<CatalogControl> {
        val tokens = TOKENS[feature] ?: return emptyList()
        return entries.filter { entry -> tokens.any { entry.matches(it) } }
    }

    /**
     * المرشّحات التي **تُقاس كعقدة** — لا قالب ولا مجلّد ولا نمط.
     *
     * **والقاعدة مقيسة لا مُقدَّرة:** البذرة تحتوي قوالب (`{policy}` · `$cpu`) ومجلّدات
     * (`/sys/class/devfreq`). وقياسُ وجود مجلّد على أنه «قدرة» يمنح الجهاز قدرةً لا يملكها —
     * وهو بعينه ما يمنعه ADR-07. فما لا يُفتح كملفّ لا يُدَّعى.
     */
    fun literalNodes(entry: CatalogControl): List<String> = entry.candidates.filter(::isLiteralNode)

    private fun CatalogControl.matches(token: String): Boolean =
        key == token || ((token.endsWith(":") || token.endsWith(".")) && key.startsWith(token))

    companion object {
        /** اسم الأصل في `assets/` — موضع واحد، فلا يقرأه اثنان بمسارين. */
        const val ASSET = "control_catalog.json"

        private const val VERSION = 1

        /** ميزة عتاد ← رموز مفاتيحها في الكتالوج (بادئة منتهية بـ`:` أو `.`، أو مفتاح تامّ). */
        private val TOKENS: Map<HardwareFeature, List<String>> = mapOf(
            HardwareFeature.CPU_FREQUENCY to listOf(HardwareControlKey.CPU_LIMITS_PREFIX),
            HardwareFeature.CPU_CORE_CONTROL to listOf(HardwareControlKey.CPU_CORE_ONLINE_PREFIX),
            HardwareFeature.CPU_BOOST to listOf(HardwareControlKey.CPU_BOOST),
            HardwareFeature.GPU_FREQUENCY to listOf(HardwareControlKey.GPU_FREQUENCY_PREFIX),
            HardwareFeature.GPU_GOVERNOR to listOf(HardwareControlKey.GPU_GOVERNOR_PREFIX),
            HardwareFeature.MEMORY_FREQUENCY to listOf(HardwareControlKey.MEMORY_FREQUENCY_PREFIX),
            HardwareFeature.UFS_FREQUENCY to listOf(HardwareControlKey.STORAGE_FREQUENCY_PREFIX),
            HardwareFeature.ZRAM to listOf(HardwareControlKey.ZRAM_KEY_PREFIX),
            HardwareFeature.TOUCH_CONTROL to listOf(
                HardwareControlKey.TOUCH_BOOST_PREFIX,
                HardwareControlKey.TOUCH_GESTURE_PREFIX,
            ),
            HardwareFeature.BATTERY_TELEMETRY to listOf("battery_health"),
            HardwareFeature.THERMAL_ZONES to listOf("thermal_zone"),
            HardwareFeature.KERNEL_VM to listOf(HardwareControlKey.VM_TUNING_PREFIX),
        )

        fun empty(): CatalogControlIndex = CatalogControlIndex(emptyList())

        /**
         * يقرأ الكتالوج من نصّ JSON. أي خطأ بنيويّ يُرجع فهرسًا فارغًا بدل استثناء يُسقط شاشة:
         * الكتالوج **إضافة** إلى القياس القائم، فلا يجوز أن يصير نقطة فشل له.
         */
        fun parse(json: String): CatalogControlIndex {
            val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull()
                ?: return empty()
            if (root["version"]?.jsonPrimitive?.content?.toIntOrNull() != VERSION) return empty()
            val entries = runCatching {
                root["entries"]?.jsonArray.orEmpty().mapNotNull { element ->
                    val entry = element.jsonObject
                    val key = entry["key"]?.jsonPrimitive?.content.orEmpty()
                    if (key.isEmpty()) return@mapNotNull null
                    CatalogControl(
                        key = key,
                        transport = entry["transport"]?.jsonPrimitive?.content.orEmpty(),
                        candidates = entry["candidates"]?.jsonArray.orEmpty().mapNotNull { candidate ->
                            candidate.jsonObject["path"]?.jsonPrimitive?.content?.takeIf(String::isNotEmpty)
                        },
                        writeTiers = entry["privilege"]?.jsonObject?.get("write")?.jsonArray.orEmpty()
                            .map { it.jsonPrimitive.content },
                        danger = entry["danger"]?.jsonPrimitive?.content.orEmpty(),
                        readOnlyReason = entry["read_only_reason"]?.jsonPrimitive?.content,
                        kind = entry["kind"]?.jsonPrimitive?.content.orEmpty(),
                    )
                }
            }.getOrDefault(emptyList())
            return CatalogControlIndex(entries)
        }

        /**
         * مسار يُقاس كعقدة: مطلق، وبلا قالب `{}`/`$`، وبلا نمط `*`.
         * (القالب يجب أن يُحلّ من الاكتشاف أوّلًا؛ والمجلّد يُثبته `isFile` عند القياس.)
         */
        fun isLiteralNode(path: String): Boolean =
            path.startsWith("/") && path.none { it == '{' || it == '}' || it == '$' || it == '*' }
    }
}

/**
 * دمج الكتالوج في قياس القدرة القائم — **إضافة لا استبدال** (M1 §6 · ADR-87 §4).
 *
 * ثلاث قواعد:
 * 1. **لا خفض أبدًا:** الكتالوج يرفع `NONE → READ_ONLY → READ_WRITE` فقط. ما قاسه المحلّل بالملفّ
 *    الحقيقيّ لا يُنقض بمرشّح بيانات.
 * 2. **`PROTECTED` لا يرفع إلى كتابة** ولو كانت العقدة قابلة للكتابة فعلًا.
 * 3. **القالب والمجلّد لا يُقاسان** — `literalNodes` تُرشِّح، و`exists` عند المحلّل تتحقّق `isFile`.
 *
 * والدالّة نقيّة: كل لمسٍ للنظام يمرّ عبر [Probe]، فتٌقاس بالقيم في اختبار بلا أندرويد.
 */
internal object CatalogCapabilityOverlay {

    /** كل ما يُسأل عن النظام — مُمرَّر، فلا تقرأ هذه الدالّة ملفًّا بنفسها. */
    data class Probe(
        val exists: (String) -> Boolean,
        val writable: (String) -> Boolean,
    )

    fun apply(base: FeatureCapability, index: CatalogControlIndex, probe: Probe): FeatureCapability {
        val controls = index.controlsFor(base.feature)
        if (controls.isEmpty()) return base
        val declared = controls
            .flatMap { control -> index.literalNodes(control).map { control to it } }
            .distinctBy { (_, path) -> path }
        val present = declared.filter { (_, path) -> probe.exists(path) }
        if (present.isEmpty()) return base

        val writableNode = present.firstOrNull { (control, path) ->
            control.writeDeclared && probe.writable(path)
        }
        val access = if (writableNode != null) AccessLevel.READ_WRITE else AccessLevel.READ_ONLY
        val escalated = access.ordinal > base.access.ordinal
        return base.copy(
            access = if (escalated) access else base.access,
            evidence = (base.evidence + present.map { (_, path) -> path }).distinct(),
            catalogKeys = controls.map { it.key }.distinct(),
        )
    }
}
