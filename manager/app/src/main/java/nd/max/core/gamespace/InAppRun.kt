/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

/**
 * **ما جرى في آخر محاولة تشغيل لبروفايل تطبيق — كما يراه المستخدم لا كما يراه السجلّ.**
 *
 * `InAppProfileRules.plan` يُرجع `skipped: Map<InAppKnob, String>` يحمل سبب كل مفتاح لم يُطبَّق
 * (`zen-mode-already-on`، `snapshot-unreadable`، `already-off`…). وكانت هذه الأسباب تُكتب في
 * `AppMonitorLogger` وحده — أي في سجلّ لا يفتحه إلا من يبحث عن عطب، **وصفر مستهلك لها في
 * الواجهة**. فالمستخدم يرى «البروفايل يعمل» ولا يعلم أن معدّل التحديث سُقط لأن اللقطة غير
 * مقروءة، أو أن «عدم الإزعاج» لم يُخفَّض لأن نمطًا أقوى كان مفعّلًا. وهذا عين الفرق بين ميزة
 * تعمل وميزة تبدو أنها تعمل.
 *
 * **والصفر غيابٌ لا صفر:** `writes = 0` تعني «لا مفاتيح مطلوبة أو كلّها رُفضت»، لا «طُبِّق
 * صفر بنجاح». ولذلك لا تُقرأ `allVerified` إلّا عند وجود كتابة واحدة على الأقل.
 */
data class InAppRun(
    val pkg: String,
    val writes: Int,
    val written: Int,
    val verified: Int,
    val skipped: Map<InAppKnob, String> = emptyMap(),
    val blocked: String? = null,
) {
    /** كل ما طُلب كُتب وقُرئ مرتجًعًا، ولا مفتاح سقط. */
    val allVerified: Boolean
        get() = writes > 0 && verified == writes && skipped.isEmpty() && blocked == null

    /** مفتاح لم يُطبَّق — والسبب مذكور لا مُقدَّر. */
    val refused: Boolean get() = skipped.isNotEmpty() || blocked != null
}

/**
 * رموز التخطّي التي تُنتجها [InAppProfileRules.plan] و[InAppProfileApplier] — **مُعدَّدة هنا**
 * كي لا تُقارَن النصوص في الواجهة (مقارنة نصّ خامّ تنكسر بصمت حين يتغيّر الرمز في مكانه).
 *
 * و[UNKNOWN] ليست احتياطًا كسولًا: تخزين قديم قد يحمل سببًا كُتب قبل هذه النسخة، وعرض «سبب
 * غير معروف» أصدق من إخفاء السطر أو من ادّعاء سبب لم يُقرأ.
 */
enum class InAppRunReason {
    /** تعذّرت قراءة `zen_mode` قبل الكتابة، فلا تُخمَّن قيمته السابقة. */
    ZEN_UNREADABLE,

    /** نمط عدم الإزعاج مفعّل أصلًا (أو نمط أقوى اختاره المستخدم): لا يُخفَّض ما اختاره. */
    ZEN_ALREADY_ON,

    /** القيمة السابقة قد لا تُستعاد كما كانت (محارف غير متوقّعة) ⇒ لا نكتب ثم نعجز عن الرجوع. */
    SNAPSHOT_UNSAFE,

    /** تعذّرت قراءة اللقطة السابقة ⇒ لا كتابة بلا طريق رجوع. */
    SNAPSHOT_UNREADABLE,

    /** المفتاح مُطفأ أصلًا، فلا كتابة ولا داعٍ لإعلان نجاح. */
    ALREADY_OFF,

    /** مسار الكتابة (الجذر/شل) غير جاهز الآن — يُعاد بعد لحظات. */
    LANE_UNAVAILABLE,

    /** سبب محفوظ من نسخة أخرى أو رمز لم تعد هذه النسخة تعرفه. */
    UNKNOWN;

    companion object {
        fun of(token: String?): InAppRunReason = when (token) {
            "zen-unreadable" -> ZEN_UNREADABLE
            "zen-mode-already-on" -> ZEN_ALREADY_ON
            "snapshot-unsafe" -> SNAPSHOT_UNSAFE
            "snapshot-unreadable" -> SNAPSHOT_UNREADABLE
            "already-off" -> ALREADY_OFF
            "lane-unavailable" -> LANE_UNAVAILABLE
            else -> UNKNOWN
        }
    }
}

/**
 * **تخزين آخر تشغيل في تفضيلات البروفايل نفسها** (`inapp_profile`، التي تحمل اللقطة).
 *
 * والفكّ **صارم**: نصّ فاسد أو نصف مكتوب أو من نسخة أخرى يُرجع `null`، فتُعرض «لا نتيجة مسجّلة»
 * بدلًا من عرض سبب مشبوه. والسبب في الصرامة أن هذا النصّ يُقرأ ليُعرض للمستخدم كحقيقة عن
 * جهازه — وما لا يُتحقّق منه لا يُعرض.
 */
object InAppRunLog {

    const val PREFS = "inapp_profile"
    const val KEY = "last_run"

    private const val VERSION = "v1"
    private const val MAX_LINES = 8
    private val SAFE_PKG = Regex("^[A-Za-z0-9_.]{1,128}$")
    private val SAFE_REASON = Regex("^[a-z0-9-]{1,32}$")

    fun encode(run: InAppRun): String = buildString {
        append("run\t").append(VERSION).append('\t').append(run.pkg).append('\n')
        append("n\t").append(run.writes).append('\t').append(run.written).append('\t')
            .append(run.verified).append('\n')
        run.blocked?.let { append("b\t").append(it).append('\n') }
        // ترتيب ثابت: الخريطة قد تُبنى بترتيب مختلف، والنصّ واحد للقراءة والاختبار.
        InAppKnob.entries.forEach { knob ->
            val reason = run.skipped[knob] ?: return@forEach
            append("k\t").append(knob.name).append('\t').append(reason).append('\n')
        }
    }

    fun decode(raw: String?): InAppRun? {
        val text = raw?.takeIf { it.isNotBlank() } ?: return null
        val lines = text.trimEnd('\n').split('\n')
        if (lines.size > MAX_LINES) return null
        val head = lines.first().split('\t')
        if (head.size != 3 || head[0] != "run" || head[1] != VERSION) return null
        val pkg = head[2]
        if (!SAFE_PKG.matches(pkg)) return null

        var numbers: List<String>? = null
        var blocked: String? = null
        val skipped = LinkedHashMap<InAppKnob, String>()
        for (line in lines.drop(1)) {
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                "n" -> {
                    if (numbers != null || parts.size != 4) return null
                    numbers = parts.drop(1)
                }
                "b" -> {
                    if (blocked != null || parts.size != 2) return null
                    val token = parts[1]
                    if (!SAFE_REASON.matches(token)) return null
                    blocked = token
                }
                "k" -> {
                    if (parts.size != 3) return null
                    val knob = InAppKnob.entries.firstOrNull { it.name == parts[1] } ?: return null
                    val reason = parts[2]
                    if (!SAFE_REASON.matches(reason)) return null
                    // تكرار المفتاح يعني نصًّا كُتب مرّتين — لا نختار أحدهما بالحدس.
                    if (skipped.put(knob, reason) != null) return null
                }
                else -> return null
            }
        }
        val counts = numbers ?: return null
        val writes = counts[0].toIntOrNull() ?: return null
        val written = counts[1].toIntOrNull() ?: return null
        val verified = counts[2].toIntOrNull() ?: return null
        if (writes < 0 || written < 0 || verified < 0) return null
        if (written > writes || verified > writes) return null
        if (skipped.size > InAppKnob.entries.size) return null
        return InAppRun(
            pkg = pkg,
            writes = writes,
            written = written,
            verified = verified,
            skipped = skipped,
            blocked = blocked,
        )
    }
}
