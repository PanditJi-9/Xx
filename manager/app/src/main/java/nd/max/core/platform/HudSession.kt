/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.PowerManager
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import nd.max.AppMonitor
import nd.max.service.SessionSummaryNotifier

/**
 * لوحة الأداء — الطبقة البيانية. **لا واجهة في هذا الملف**: تُقرأ منه اللوحة العائمة داخل
 * الخدمة وشاشة الإعدادات معًا.
 *
 * ### القاعدة الحاكمة: الغائب `null` لا صفرًا
 *
 * قارئ الإطارات (`FpsMonitorUtil`) يُعيد `0f` حين **لا يجيب** المصدر — وقيمة صفر إطار/ثانية
 * ليست قراءة ممكنة على جهاز شاشته مضاءة. فالصفر هنا يُترجم **غيابًا** لا رقمًا، ويصل الواجهة
 * شرطةً (`MAX_VALUE_UNAVAILABLE`) لا صفرًا معقولًا. وهذه هي نفس القاعدة التي تحكم `status_unknown`
 * في طبقة البيانات وألوان التنبيه في طبقة التصميم: **الأداة لا تدّعي قياسًا لم تقسه.**
 *
 * ### قارئ واحد لا قارئان
 *
 * اللوحة العائمة وشاشة الإعدادات تحتاجان الأرقام نفسها في الوقت نفسه (معاينة حيّة + تراكب)،
 * وقراءتان متوازيتان تعنيان استدعاءَي shell كل ثانية لنفس القيمة — وهو ثمن على جهاز يُقاس
 * أداؤه أصلًا. فالمُشغِّل واحد ([HudSampler]) وللمالك واحد ([HudSampler.start] `owner`): من
 * يبدأ آخرًا يملكه، ومن ينتهي لا يوقف إلا ما يملكه.
 */
/**
 * شكل اللوحة العائمة. أربعة أشكال، ولكلّ واحد موضعه:
 *
 * - [Strip] شريط رقيق في سطر واحد — للعب الأفقيّ حيث العرض ثمين والطول مفقود.
 * - [Pane] لوح بصفوف: التسمية يسارًا والقيمة يمينًا — للقراءة المتأنّية والمقارنة.
 * - [Ring] حلقة إطارات: رقم كبير داخل قوس — للإطارات وحدها حين تكون هي السؤال.
 * - [Badge] رقاقة بمبدأ تراكبات الحاسوب: رقم واحد كبير ووحدته، **وزمن الإطار** تحته، وبقيّة
 *   الحقول سطرًا مضغوطًا. وهو الفرق بين «قياس يُقرأ» و«قياس يُلاحَظ»: من عاش على MangoHud
 *   أو Fraps لا يقرأ الرقم فقط بل زمن الإطار، لأن ٨٧ إطارًا و٥٨ إطارًا قد يبدوان متقاربين في
 *   العدّ وهما متباعدان في الإحساس — وزمن الإطار هو المقياس الذي يكشف تلك الفجوة.
 *
 * **والترتيب هنا هو ترتيب الاختيار في الشاشة** (`ordinal`)، فإضافة شكل جديد **تُلحَق بآخره**
 * ولا تُدسّ في وسطه: الشكل المحفوظ في تفضيلات المستخدم يُقرأ بالاسم لا بالرقم، لكن الترتيب
 * الظاهر في المنتقي يبقى ما رآه سابقًا فلا يتحرّك تحت إصبعه.
 *
 * وهو في طبقة البيانات لا في ملف الرسم، كما [FpsReadMode] بالضبط: يقرأه المخزن ويكتبه،
 * ويرسمه المُصيِّر، فلا هو تفصيل رسم ولا هو نصّ مُخزَّن.
 */
enum class HudForm { Strip, Pane, Ring, Badge }

/** ترتيب الحقول. و[Stack] هو أصل كل شيء: هو ما يقرأه المستخدم رأسيًّا في العربية. */
enum class HudArrangement { Line, Stack }

/** الحلقة **مفردة** فلا صفّ ولا عمود لها — يُجاب هنا مرة واحدة بدل تحكّم صامت في الشاشة. */
fun HudForm.acceptsArrangement(): Boolean = this != HudForm.Ring

/**
 * حقول اللوحة **بترتيب أهمّيّتها كما تُقرأ** (§2 من الطلب)، وترتيب النوع هنا هو ترتيب العرض في
 * كل سطح: شبكة الاختيار في الإعدادات، و`orderedFields` في اللوحة، وسطر الاقتطاف. قرارٌ واحد في
 * موضع واحد، فلا يُرتَّب حقل في شاشة على خلاف أخرى.
 *
 * الترتيب: الإطارات أولًا (وجود اللوحة لأجلها)، ثم حِمل المعالج (أقرب رقم إلى سبب التقطيع)، ثم
 * الحرارة (السقف الحراري هو ما يُقرأ خلف الأداء)، ثم الذاكرة، فالقدرة، فمستوى البطارية، فالشبكة،
 * فالرسّام (ثابت لا يتغيّر جريانه، فهو آخر ما يُراقَب). الحقول تُخزَّن **بالاسم** لا بالرقم، فإعادة الترتيب تُغيّر القراءة
 * لا تُفسد محفوظًا.
 *
 * **والحدّ المعلن — ولا حقل GPU هنا:** قراءة الرسّام الحيّة بين أيدينا (`GpuLiveRead`) تحتاج
 * نموذج الجهاز المُكتشف (`GpuHardwareBackend.Device`)، واكتشافه الكامل ثقيل عن قصد (سرد `devfreq`
 * وجدول OPP وقراءات عبر صدفة الجذر) ويخزّنه `PanelClocks` في لوحة Game Space لمدّة ٦٠ ثانية لأن
 * نبضته أبطأ. ووضعُ مثله في حلقة اللوحة (نبضة كل ثانية، وتعمل **مع** لوحة الألعاب في آنٍ واحد حين
 * يكون الاختصار مفتوحًا §1.2) يعني إمّا تكرار مراقبة GPU — وهو ما يمنعه §10.4 صراحةً — أو رقمًا
 * يومض غيابًا حتى تُفتح لوحة الألعاب مرّة واحدة. فبقي حِمل الرسّام حيث يُقاس صحيحًا: لوحة Game
 * Space. وهذا قرارٌ مقيس لا إغفال.
 */
enum class HudField {
    /** إطارات/ثانية من المصدر المُختار. */
    Frames,

    /**
     * ثبات الإطارات: حصة العيّنات القريبة من هدف الشاشة، **من نافذة متدحرجة لا من لحظة**.
     *
     * والفرق جوهريّ: الإطارات اللحظية قد تُقرأ في هبّة فتبدو اللعبة متعثّرة، أو بين هبّتين فتبدو
     * مثالية — والرقم نفسه. فالثبات يجيب سؤال اللاعب («هل هي سلسة؟») بما لا تجيبه اللحظة.
     * ويُحسب في [FrameWindow] من معدّل الإطارات المُعايَن، وهدفه معدّل تحديث الشاشة نفسه.
     */
    Stability,

    /**
     * تقطيع الإطارات المقيس من `gfxinfo framestats`: نسبة الإطارات التي تجاوزت ميزانية
     * إطار واحد، ومعها المئين ٩٩ لزمن الإطار.
     *
     * **وحدّه:** يخصّ الحزمة الأمامية ومسار HWUI؛ وغياب `---PROFILEDATA---` يعني `null`
     * لا صفرًا. وهو مختلف عن [Stability] عمدًا: ذاك حصة عيّنات معدّل الإطارات، وهذا زمن
     * إطار من حلقة الرسم نفسها.
     */
    Jank,

    /** حِمل المعالج %. */
    Cpu,

    /** حرارة البطارية. */
    Heat,

    /** الذاكرة المستعملة. */
    Ram,

    /** قدرة البطارية الواطية. */
    Power,

    /** نسبة البطارية كما يعلنها النظام (٠–١٠٠). */
    Battery,

    /**
     * حركة الشبكة اللحظية: نزول ورفع بالبايت/ثانية، من فروق عدّادات النظام.
     *
     * **ولماذا ليست هنا قراءة لكل تطبيق:** هذه لوحة أداء كلية، وتفصيل كل تطبيق يخصّ شاشة
     * الاستخدام. والفرق بين العدّادين هو القياس؛ وما لا يُقاس يبقى `null` لا صفرًا.
     */
    Network,

    /** مُصيّر الرسوم للتطبيق الأمامي (GL/Vulkan). */
    Renderer,

    /** أعلى تردّد جارٍ بين عناقيد المعالج، من `scaling_cur_freq`. */
    CpuFreq,

    /** حرارة الشريحة من المناطق الحراريّة — لا حرارة البطارية ([Heat]). */
    SocTemp,

    /** تردّد الرسّام الجاري من عقدة `kgsl`/`devfreq`/Mali. */
    GpuFreq,

    /** انشغال الرسّام % من عقدة الانشغال. */
    GpuLoad,

    /** تردّد الذاكرة المشتركة من `devfreq` بالاسم. */
    DdrFreq,

    /** حركة القرص: قراءة وكتابة بالبايت/ثانية من `/proc/diskstats`. */
    DiskIo,

    /** جهد البطارية بالميلي فولت. */
    BatteryVoltage,
}

/**
 * §2 — **اختصاران جاهزان** لحالتَي الاستعمال المطلوبتين: «وضع مصغّر يعرض المعلومات الأساسية
 * فقط، ووضع موسّع للتفاصيل».
 *
 * وهما **لقطة إعداد لا نمط مفروض**: اللمس يضبط الشكل والحقول والحجم والمنحنى دفعةً متناسقة،
 * ويبقى كلٌّ منها قابلًا للتعديل الفردي كما كان. ولهذا لا يُخزَّن «النمط» في التفضيلات — الذي
 * يُخزَّن هو الخيارات نفسها، والنمط يُستنتج من مطابقتها عند العرض، فلا يُدَّعى نمطٌ غير قائم.
 */
enum class HudPreset(
    val form: HudForm,
    val fields: Set<HudField>,
    val textSizeSp: Float,
    val showGraph: Boolean
) {
    /** مصغّر للعب: شريط سطر واحد بالأساسيات الثلاثة (إطارات · معالج · حرارة)، بخطّ ١٢ وبلا منحنى. */
    Compact(HudForm.Strip, setOf(HudField.Frames, HudField.Cpu, HudField.Heat), 12f, false),

    /** موسّع للتفاصيل: لوح بصفوف، وكل الحقول، وخطّ أكبر، ومنحنى الإطارات. */
    Expanded(HudForm.Pane, HudField.entries.toSet(), 16f, true)
}

/**
 * قراءة واحدة. كل حقل قابل للغياب على حِدة: قد يجيب مصدر الإطارات ولا يجيب مصدر القدرة.
 */
data class HudReading(
    val frames: Float? = null,
    val cpu: Int? = null,
    val ramMb: Int? = null,
    val watt: Float? = null,
    val heat: Float? = null,
    /** نسبة البطارية المئوية؛ `null` حين لا يعلنها النظام. */
    val battery: Int? = null,
    /** نزول الشبكة بالبايت/ثانية — `null` قبل أوّل فرق مقيس. */
    val rxBps: Float? = null,
    /** رفع الشبكة بالبايت/ثانية — `null` قبل أوّل فرق مقيس. */
    val txBps: Float? = null,
    val renderer: String? = null,
    /** ثبات الإطارات ٠..١٠٠ من نافذة متدحرجة؛ `null` قبل أن تكتمل عيّنات كافية. */
    val stability: Int? = null,
    /** نسبة الإطارات المتقطّعة من `gfxinfo`؛ `null` حين لا يُعيد المصدر توزيعًا. */
    val jankPercent: Int? = null,
    /** المئين ٩٩ لزمن الإطار بالميلي ثانية؛ `null` مع غياب المصدر نفسه. */
    val frameP99Ms: Float? = null,
    /** أعلى تردّد جارٍ بين عناقيد المعالج. */
    val cpuFreqMhz: Int? = null,
    /** حرارة الشريحة — مستشعر مستقلّ عن حرارة البطارية. */
    val socTempC: Float? = null,
    /** تردّد الرسّام. */
    val gpuFreqMhz: Int? = null,
    /** انشغال الرسّام %. */
    val gpuLoadPercent: Int? = null,
    /** تردّد الذاكرة المشتركة. */
    val ddrFreqMhz: Int? = null,
    /** قراءة القرص بالبايت/ثانية. */
    val diskReadBps: Float? = null,
    /** كتابة القرص بالبايت/ثانية. */
    val diskWriteBps: Float? = null,
    /** جهد البطارية بالميلي فولت. */
    val batteryVoltageMv: Int? = null,
    /**
     * الحزمة الأمامية في لحظة العيّنة. تُقرأ أثناء التسجيل فقط، وهي ما يقصّ الانتقالات
     * في نهاية الجلسة: فترة تحميلٍ أو قائمةٍ ليست لعبًا، وإحصاؤها يخفض المتوسّط كذبًا.
     */
    val foregroundPackage: String? = null
) {
    /** هل أجاب مصدر الإطارات؟ هو المصدر الوحيد الذي يفشل بحسب الوضع المُختار. */
    val framesAnswered: Boolean get() = frames != null

    fun valueOf(field: HudField): Any? = when (field) {
        HudField.Frames -> frames
        HudField.Cpu -> cpu
        HudField.Ram -> ramMb
        HudField.Power -> watt
        HudField.Heat -> heat
        HudField.Battery -> battery
        // زوج الشبكة يُصاغ في `hudFieldText`؛ وهذه الدالة تعطي اتجاه النزول وحده
        // لمن يريد رقمًا واحدًا (الفرز/التصدير)، لا عرضًا.
        HudField.Network -> rxBps
        HudField.Renderer -> renderer
        HudField.Stability -> stability
        HudField.Jank -> jankPercent
        HudField.CpuFreq -> cpuFreqMhz
        HudField.SocTemp -> socTempC
        HudField.GpuFreq -> gpuFreqMhz
        HudField.GpuLoad -> gpuLoadPercent
        HudField.DdrFreq -> ddrFreqMhz
        HudField.DiskIo -> diskReadBps
        HudField.BatteryVoltage -> batteryVoltageMv
    }
}

/**
 * ما تعرفه الواجهة الآن: آخر قراءة، ومن قرأها، وهل الجلسة تُسجّل.
 *
 * @param reading `null` قبل أوّل قراءة — لا مصفوفة أصفار.
 * @param owner من يقرأ الآن (`shopfront` للخدمة و`screen` للشاشة) — يُعرض للمستخدم لأنّ
 *   «لماذا الأرقام تتوقّف حين أُغلق الشاشة؟» سؤال حقيقي، وجوابه أنّ القارئ كان الشاشة.
 */
data class HudSnapshot(
    val reading: HudReading? = null,
    val sampledAtMs: Long = 0L,
    val owner: String? = null,
    val recording: Boolean = false,
    val samples: Int = 0,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
    /** آخر الإطارات بترتيب الزمن — منحنى اللوحة يُقصّ منها على المدى المُختار. */
    val framesHistory: List<Float> = emptyList(),
    /** تحذيرات تجاوزت عتبتها ومدّتها — تُعرض في اللوحة، وتُصفَّر عند زوال السبب. */
    val warnings: List<HudWarning> = emptyList()
) {
    companion object {
        const val DEFAULT_INTERVAL_MS = 1000L
        val Idle = HudSnapshot()
    }
}

/**
 * تحذير عتبة: الحقل، والقيمة التي تجاوزت، والعتبة، ومنذ متى.
 *
 * **ولماذا المدّة جزء من التحذير لا زينة:** عتبة تُلمس لحظةً ليست عتبة؛ الحرارة تعلو
 * وتهبط مع نبضة واحدة، والتحذير يجب أن يعني «هذه الحالة مستمرّة» لا «رأيت رقمًا».
 */
data class HudWarning(
    val field: HudField,
    val value: Float,
    val threshold: Float,
    val sinceMs: Long,
    val nowMs: Long
) {
    val durationMs: Long get() = (nowMs - sinceMs).coerceAtLeast(0L)
}

/**
 * قواعد جلسة اللعب: متى تُوقف نفسها، ومتى تُحذّر.
 *
 * وهي **تفضيلات مستخدم** لا ثوابت: جهاز يبلغ ٨٠°C على SoC آخر قد يكون طبيعيًّا، ومن
 * يلعب على شاحن يريد عتبة بطارية مختلفة. القيم الافتراضيّة محافظة، وكل واحدة قابلة للضبط.
 */
data class HudSessionRules(
    val stopOnForegroundChange: Boolean = true,
    val stopOnPowerChange: Boolean = true,
    val stopOnScreenOff: Boolean = true,
    val clipTransitions: Boolean = true,
    /**
     * يبدأ التسجيل وحده حين تصير لعبةٌ من مكتبة الألعاب في المقدّمة.
     *
     * **السبب:** التسجيل اليدوي يُنسى. ومن لا يضغط «تسجيل» لا يملك جلسةً واحدة، فيبقى
     * السجلّ فارغًا والمقارنة بلا أساس — الميزة كلها بلا مستخدم.
     */
    val autoStart: Boolean = false,
    val socTempThresholdC: Float = 80f,
    val batteryTempThresholdC: Float = 42f,
    val gpuLoadThresholdPercent: Float = 90f,
    val warningSeconds: Int = 10
)

/** مخزن القواعد في تفضيلات التطبيق نفسها — لا ملفّ ثانٍ ولا مصدر حقيقة مزدوج. */
object HudSessionSettings {
    private const val PREFS = "settings"
    private const val KEY_STOP_FOREGROUND = "hud_session_stop_foreground"
    private const val KEY_STOP_POWER = "hud_session_stop_power"
    private const val KEY_STOP_SCREEN = "hud_session_stop_screen"
    private const val KEY_CLIP = "hud_session_clip"
    private const val KEY_AUTO_START = "hud_session_auto_start"
    private const val KEY_SOC = "hud_session_warn_soc"
    private const val KEY_BATTERY = "hud_session_warn_battery"
    private const val KEY_GPU = "hud_session_warn_gpu"
    private const val KEY_SECONDS = "hud_session_warn_seconds"

    fun load(context: Context): HudSessionRules {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = HudSessionRules()
        return HudSessionRules(
            stopOnForegroundChange = prefs.getBoolean(KEY_STOP_FOREGROUND, defaults.stopOnForegroundChange),
            stopOnPowerChange = prefs.getBoolean(KEY_STOP_POWER, defaults.stopOnPowerChange),
            stopOnScreenOff = prefs.getBoolean(KEY_STOP_SCREEN, defaults.stopOnScreenOff),
            clipTransitions = prefs.getBoolean(KEY_CLIP, defaults.clipTransitions),
            autoStart = prefs.getBoolean(KEY_AUTO_START, defaults.autoStart),
            socTempThresholdC = prefs.getFloat(KEY_SOC, defaults.socTempThresholdC),
            batteryTempThresholdC = prefs.getFloat(KEY_BATTERY, defaults.batteryTempThresholdC),
            gpuLoadThresholdPercent = prefs.getFloat(KEY_GPU, defaults.gpuLoadThresholdPercent),
            warningSeconds = prefs.getInt(KEY_SECONDS, defaults.warningSeconds)
        )
    }

    fun save(context: Context, rules: HudSessionRules) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_STOP_FOREGROUND, rules.stopOnForegroundChange)
            .putBoolean(KEY_STOP_POWER, rules.stopOnPowerChange)
            .putBoolean(KEY_STOP_SCREEN, rules.stopOnScreenOff)
            .putBoolean(KEY_CLIP, rules.clipTransitions)
            .putBoolean(KEY_AUTO_START, rules.autoStart)
            .putFloat(KEY_SOC, rules.socTempThresholdC)
            .putFloat(KEY_BATTERY, rules.batteryTempThresholdC)
            .putFloat(KEY_GPU, rules.gpuLoadThresholdPercent)
            .putInt(KEY_SECONDS, rules.warningSeconds)
            .apply()
    }
}

/**
 * متتبّع العتبات: يفتح نافذة لكل حقل عند أوّل تجاوز، ويغلقها عند العودة، ولا يُطلق
 * تحذيرًا قبل انقضاء المدّة المطلوبة.
 *
 * والفصل عن المُشغِّل مقصود: هذه سياسة قابلة للاختبار بلا أندرويد ولا مؤقّتات.
 */
class HudWarningTracker {

    private val since = HashMap<HudField, Long>()

    fun update(reading: HudReading, rules: HudSessionRules, nowMs: Long): List<HudWarning> {
        val breaches = buildList {
            reading.socTempC?.let { add(Triple(HudField.SocTemp, it, rules.socTempThresholdC)) }
            reading.heat?.let { add(Triple(HudField.Heat, it, rules.batteryTempThresholdC)) }
            reading.gpuLoadPercent?.let { add(Triple(HudField.GpuLoad, it.toFloat(), rules.gpuLoadThresholdPercent)) }
        }
        val minDuration = (rules.warningSeconds.coerceAtLeast(0) * 1000L)
        val active = HashSet<HudField>()
        val warnings = ArrayList<HudWarning>(breaches.size)
        for ((field, value, threshold) in breaches) {
            if (threshold <= 0f) continue
            if (value >= threshold) {
                active += field
                val started = since.getOrPut(field) { nowMs }
                if (nowMs - started >= minDuration) {
                    warnings += HudWarning(field, value, threshold, started, nowMs)
                }
            }
        }
        since.keys.retainAll(active)
        return warnings
    }

    fun reset() {
        since.clear()
    }
}

/** الاسم الذي يُسجّله قارئ اللوحة العائمة، والمقابل في شاشة الإعدادات. */
const val HUD_OWNER_OVERLAY = "overlay"
const val HUD_OWNER_SCREEN = "screen"

/**
 * **مالك رابع: لوحة مساحة الألعاب** (`GAME-PANEL`).
 *
 * مستقِلّ عن [HUD_OWNER_OVERLAY] لأنّ الخدمتين تعملان **معًا** الآن: اختصار FPS داخل تراكب
 * اللعبة (§1.2 من الطلب) يُشغّل لوحة الأداء ولوحة الألعاب مفتوحة. ولولا فصل الاسـمين لكان
 * خروج إحداهما يُسقط قارئ الأخرى (كان المالك الواحد يجعل «من ينتهي» و«من يملك» يلتبسان).
 */
const val HUD_OWNER_PANEL = "game_panel"

/**
 * **مالك ثالث: الجزيرة الديناميكية** (`DYNAMIC-ISLAND-01`).
 *
 * والفرق عنهما أنّ الجزيرة **لا تسبق أحدًا**: لا تشغّل القارئ إلا إن كان **بلا مالك**،
 * وتُوقفه عند خروجها فقط إن كانت هي التي شغّلته. فلو فُتحت شاشة الإعدادات أو عمل التراكب
 * فالقارئ لهما، والجزيرة تعرض أرقامهما الحيّة بلا قراءة ثانية على جهاز يُقاس أداؤه أصلًا.
 */
const val HUD_OWNER_ISLAND = "island"

/**
 * القناة بين القارئ والواجهتين. صغيرة عن قصد: قيمة واحدة تُستبدل كل دورة، لا سجلّ أحداث.
 */
object HudLive {

    /** أطول مدى يعرضه المُصيِّر ([HudSnapshot.framesHistory] يحمل هذا القدر والباقي يُسقَط). */
    const val HISTORY_CAPACITY = 40

    private val mutable = MutableStateFlow(HudSnapshot.Idle)
    private val history = ArrayDeque<Float>()
    val snapshot: StateFlow<HudSnapshot> = mutable.asStateFlow()

    internal fun publish(
        reading: HudReading,
        owner: String,
        intervalMs: Long,
        warnings: List<HudWarning> = emptyList()
    ) {
        reading.frames?.let { value ->
            history.addLast(value)
            while (history.size > HISTORY_CAPACITY) history.removeFirst()
        }
        val previous = mutable.value
        mutable.value = previous.copy(
            reading = reading,
            sampledAtMs = System.currentTimeMillis(),
            owner = owner,
            recording = HudRecorder.isLive,
            samples = previous.samples + 1,
            intervalMs = intervalMs,
            framesHistory = history.toList(),
            warnings = warnings
        )
    }

    internal fun markOwner(owner: String?) {
        mutable.value = mutable.value.copy(owner = owner)
    }

    internal fun markRecording(live: Boolean) {
        mutable.value = mutable.value.copy(recording = live)
    }

    internal fun reset() {
        history.clear()
        mutable.value = HudSnapshot.Idle
    }
}

/**
 * جلسة قياس: حلقة مقيّدة السعة من القراءات.
 *
 * والسقف مقصود — تسجيل ساعة بفاصل ثانية = ٣٦٠٠ عيّنة، وفي الذاكرة لا على القرص. وما يسقط
 * بالسقف هو **الأقدم**، ويُقال للمستخدم في الإحصاء أنّ ما يراه على المدى الأخير لا الجلسة كلها.
 */
class HudJournal(private val capacity: Int = 900) {

    data class Sample(val atMs: Long, val reading: HudReading)

    private val ring = ArrayDeque<Sample>()
    private var firstAtMs = 0L
    private var lastAtMs = 0L
    private var dropped = 0

    val size: Int get() = ring.size

    /** كم عيّنة سقطت بالسقف — يُعرض ولا يُسكَت عنه. */
    val droppedSamples: Int get() = dropped

    fun record(reading: HudReading, atMs: Long) {
        if (ring.isEmpty()) firstAtMs = atMs
        lastAtMs = atMs
        ring.addLast(Sample(atMs, reading))
        while (ring.size > capacity) {
            ring.removeFirst()
            dropped++
            firstAtMs = ring.first().atMs
        }
    }

    fun samples(): List<Sample> = ring.toList()

    fun clear() {
        ring.clear()
        dropped = 0
        firstAtMs = 0L
        lastAtMs = 0L
    }

    /**
     * الإحصاء. كل حقل يبقى `null` حتى تصل أوّل قراءة به — فمتوسط بلا عيّنات ليس صفرًا.
     *
     * والمدى يُحسب من **العيّنات الباقية** لا من زمن الجلسة، فما سقط لم يُحسب.
     */
    fun tally(): HudTally = tallyPoints(
        points = points(),
        droppedSamples = dropped,
        renderer = ring.firstNotNullOfOrNull { it.reading.renderer }
    )

    /** نقاط الجلسة بصيغة التخزين — تُستخدم للإحصاء وللرسم وللتصدير. */
    fun points(): List<SessionPoint> = ring.map { it.toPoint() }

    /** أوّل مُصيّر أُعلن في الجلسة — يُحفظ مرّة في الترويسة لا في كل نقطة. */
    fun renderer(): String? = ring.firstNotNullOfOrNull { it.reading.renderer }
}

/** عيّنة الذاكرة ← نقطة التخزين: تحويل واحد، فلا تختلف أعمدة CSV عن المنحنى. */
fun HudJournal.Sample.toPoint(): SessionPoint = SessionPoint(
    atMs = atMs,
    frames = reading.frames,
    cpu = reading.cpu,
    ramMb = reading.ramMb,
    watt = reading.watt,
    battery = reading.battery,
    heat = reading.heat,
    jankPercent = reading.jankPercent,
    frameP99Ms = reading.frameP99Ms,
    cpuFreqMhz = reading.cpuFreqMhz,
    socTempC = reading.socTempC,
    gpuLoadPercent = reading.gpuLoadPercent,
    batteryVoltageMv = reading.batteryVoltageMv,
    foreground = reading.foregroundPackage
)

/**
 * إحصاء مجموعة نقاط — الدالّة الواحدة التي تُغذّي العرض الحيّ والسجلّ المقصوص.
 *
 * **وكان الإحصاء يُحسب داخل الحلقة وحدها، فلمّا احتاج القصّ إحصاءً بعد حذف الانتقالات
 * كان سيُكرَّر حسابُه.** والتكرار هنا ليس جمالًا: نسختان تعنيان أن الجلسة الحيّة
 * والمحفوظة قد تعطيان رقمين مختلفين لنفس العيّنات.
 */
fun tallyPoints(
    points: List<SessionPoint>,
    droppedSamples: Int = 0,
    renderer: String? = null
): HudTally {
    val frames = points.mapNotNull { it.frames?.takeIf { v -> v > 0f } }
    val heats = points.mapNotNull { it.heat?.takeIf { v -> v > 0f } }
    val watts = points.mapNotNull { it.watt?.takeIf { v -> v > 0f } }
    val batteries = points.mapNotNull { it.battery }
    val socTemps = points.mapNotNull { it.socTempC?.takeIf { v -> v > 0f } }
    val gpuLoads = points.mapNotNull { it.gpuLoadPercent }
    val cpuFreqs = points.mapNotNull { it.cpuFreqMhz?.takeIf { v -> v > 0 } }
    val voltages = points.mapNotNull { it.batteryVoltageMv?.takeIf { v -> v > 0 } }
    val first = points.firstOrNull()
    val last = points.lastOrNull()
    return HudTally(
        samples = points.size,
        droppedSamples = droppedSamples,
        spanMs = if (points.size > 1 && first != null && last != null) {
            (last.atMs - first.atMs).coerceAtLeast(0L)
        } else {
            0L
        },
        framesAverage = frames.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        framesLow = frames.minOrNull(),
        framesHigh = frames.maxOrNull(),
        heatPeak = heats.maxOrNull(),
        powerAverage = watts.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        jankAverage = sessionJankAverage(points),
        frameP99AverageMs = sessionP99Average(points),
        energyWh = sessionEnergyWh(points),
        batteryStart = batteries.firstOrNull(),
        batteryEnd = batteries.lastOrNull(),
        renderer = renderer,
        socTempPeak = socTemps.maxOrNull(),
        gpuLoadAverage = gpuLoads.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        cpuFreqAverageMhz = cpuFreqs.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        batteryVoltageMinMv = voltages.minOrNull()
    )
}

/**
 * نتيجة الجلسة. القيم الغائبة `null` — لا أصفار تُقرأ أرقامًا.
 *
 * @param droppedSamples يوضّح أنّ المدى المعروض حدّه سقف الذاكرة لا بداية الجلسة.
 */
data class HudTally(
    val samples: Int = 0,
    val droppedSamples: Int = 0,
    val spanMs: Long = 0L,
    val framesAverage: Float? = null,
    val framesLow: Float? = null,
    val framesHigh: Float? = null,
    val heatPeak: Float? = null,
    val powerAverage: Float? = null,
    val jankAverage: Int? = null,
    val frameP99AverageMs: Float? = null,
    val energyWh: Float? = null,
    val batteryStart: Int? = null,
    val batteryEnd: Int? = null,
    val renderer: String? = null,
    /** أعلى حرارة شريحة في الجلسة. */
    val socTempPeak: Float? = null,
    /** متوسّط انشغال الرسّام %. */
    val gpuLoadAverage: Float? = null,
    /** متوسّط تردّد المعالج — «على أي سرعة جرى هذا اللعب». */
    val cpuFreqAverageMhz: Int? = null,
    /** أدنى جهد بطارية مقيس — يقيس الهبوط تحت الحمل لا النسبة وحدها. */
    val batteryVoltageMinMv: Int? = null
) {
    val isEmpty: Boolean get() = samples == 0
}

/** أعمدة CSV — ترتيب واحد يُكتب في الترويسة ويُقرأ آليًّا. */
private const val CSV_HEADER = SESSION_CSV_HEADER

/**
 * الجلسة نصًّا. والخلايا الغائبة **فارغة** لا `0`: جدول يُقرأ بجدول بيانات يجب أن يفرّق
 * بين «لم يُقَس» و«قِيس فكان صفرًا».
 */
fun hudSessionCsv(samples: List<HudJournal.Sample>): String {
    if (samples.isEmpty()) return CSV_HEADER + "\n"
    val origin = samples.first().atMs
    val body = samples.joinToString(separator = "\n") { (atMs, r) ->
        listOf(
            (atMs - origin).toString(),
            r.frames?.let { "%.1f".format(it) } ?: "",
            r.cpu?.toString() ?: "",
            r.ramMb?.toString() ?: "",
            r.watt?.let { "%.2f".format(it) } ?: "",
            r.heat?.let { "%.1f".format(it) } ?: "",
            r.jankPercent?.toString() ?: "",
            r.frameP99Ms?.let { "%.1f".format(it) } ?: "",
            r.renderer?.replace(',', ' ') ?: "",
            r.cpuFreqMhz?.toString() ?: "",
            r.socTempC?.let { "%.1f".format(it) } ?: "",
            r.gpuLoadPercent?.toString() ?: "",
            r.batteryVoltageMv?.toString() ?: ""
        ).joinToString(",")
    }
    return CSV_HEADER + "\n" + body + "\n"
}

/**
 * حائز الجلسة: واحد للتطبيق كلّه، يكتب فيه المُشغِّل ويقرأ منه القارئان.
 *
 * و`stop()` **لا يُفرغ**: الجلسة تنتهي ونتيجتها تبقى حتى تُصدَّر أو تُحذف. وإفراغها عند
 * الإيقاف كان يُتلف ما سُجّل من أجل قراءته.
 */
/** سبب انتهاء الجلسة — يُحفظ معها، فتُقرأ «لماذا توقّفت» لا «توقّفت» فقط. */
enum class SessionStopReason(val id: String) {
    Manual("manual"),
    ForegroundChanged("foreground"),
    PowerChanged("power"),
    ScreenOff("screen"),
    Cleared("cleared");

    companion object {
        fun of(id: String?): SessionStopReason = entries.firstOrNull { it.id == id } ?: Manual
    }
}

object HudRecorder {
    private const val CAPACITY = 900

    private var journal: HudJournal? = null
    private var live = false

    /** حارس الحفظ: `stop()` قد تُنادى مرّتين (خدمة وشاشة) فلا تُكتب الجلسة مرّتين. */
    @Volatile private var persisted = false

    @Volatile var sessionPackage: String? = null
        private set

    @Volatile var sessionLabel: String? = null
        private set

    @Volatile var sessionStartedAtMs: Long = 0L
        private set

    /** إصدار التطبيق المسجَّل — يجعل الجلسة القديمة قابلة للتفسير بعد تحديث اللعبة. */
    @Volatile var sessionAppVersion: String? = null
        private set

    /** دقّة العرض وتردّدها في لحظة البدء: «1080x2400@120». */
    @Volatile var sessionViewSize: String? = null
        private set

    @Volatile var sessionRefreshHz: Int? = null
        private set

    /** هل كان توفير البطارية مفعّلًا؟ وضعٌ حقيقي يُقاس لا نمطٌ يُدَّعى. */
    @Volatile var sessionPowerSave: Boolean = false
        private set

    /** ملاحظة المستخدم على الجلسة — تُحفظ معها لا في ملفّ منفصل. */
    @Volatile var remark: String? = null
        private set

    @Volatile var lastStopReason: SessionStopReason? = null
        private set

    /**
     * آخر جلسة حُفظت — للشاشة والإشعار بلا قراءة الملفّ من جديد.
     * تُضبط قبل الحفظ، فمن رآها ثم قرأ السجلّ وجدها فيه.
     */
    @Volatile var lastSaved: StoredGameSession? = null
        private set

    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isLive: Boolean get() = live
    val hasSession: Boolean get() = journal != null

    /**
     * بدء جلسة. الـ[context] ليس زينة: منه تُقرأ الحزمة الأمامية واسم اللعبة وإصدارها
     * ودقّة العرض، فيُعرَف **ما** سُجّل لا فقط **كم**. وغيابه يُبقي السلوك القديم
     * (جلسة بلا بيانات وصفية).
     */
    fun start(context: Context? = null) = start(context, null)

    /**
     * بدء جلسة بحزمة معلومة — يستدعيه البدء التلقائي لأن الحزمة جاءت من العيّنة نفسها،
     * و`AppMonitor` قد يكون متأخّرًا عنها بدورة.
     */
    fun start(context: Context?, packageOverride: String?) {
        journal = HudJournal(CAPACITY)
        live = true
        persisted = false
        lastSaved = null
        sessionPackage = packageOverride?.takeIf { it.isNotBlank() }
            ?: context?.let { AppMonitor.currentForegroundPackage() }
        sessionLabel = sessionPackage?.let { pkg -> appLabelOf(context, pkg) }
        sessionStartedAtMs = System.currentTimeMillis()
        sessionAppVersion = context?.let { ctx ->
            sessionPackage?.let { pkg -> versionOf(ctx, pkg) }
        }
        sessionViewSize = context?.let { viewSizeOf(it) }
        sessionRefreshHz = context?.let { refreshOf(it) }
        sessionPowerSave = context?.let { powerSaveOf(it) } ?: false
        remark = null
        lastStopReason = null
        HudLive.markRecording(true)
    }

    /** إيقاف يدوي — الغلاف القديم، ويُسجّل السبب صريحًا في السجلّ. */
    fun stop(context: Context? = null) = stop(context, SessionStopReason.Manual)

    /**
     * إيقاف بسبب مسمّى. مع [context] تُحفظ الجلسة في السجلّ الدائم؛ وبدونه يبقى السلوك
     * القديم (النتيجة في الذاكرة حتى تُصدَّر). والحفظ مرّة واحدة لكل جلسة.
     *
     * **والقصّ يقع هنا لا في العرض:** الانتقالات تُحذف قبل الإحصاء، فالمتوسّط المحفوظ
     * هو متوسط اللعب لا متوسط اللعب والتحميل معًا.
     */
    fun stop(context: Context?, reason: SessionStopReason) {
        val wasLive = live
        live = false
        HudLive.markRecording(false)
        if (!wasLive) return
        lastStopReason = reason
        if (context == null || persisted) return
        val finished = journal?.takeIf { it.size > 0 } ?: return
        val app = context.applicationContext
        val rules = HudSessionSettings.load(app)
        val allPoints = finished.points()
        val clip = if (rules.clipTransitions) clipTransitions(allPoints) else ClipResult(allPoints, 0, false)
        val tally = tallyPoints(clip.points, finished.droppedSamples, renderer = finished.renderer())
        val endedAtMs = System.currentTimeMillis()
        val startedAtMs = sessionStartedAtMs.takeIf { it > 0L } ?: endedAtMs
        val record = StoredGameSession(
            id = "$startedAtMs-${sessionPackage ?: "unknown"}",
            schemaVersion = SESSION_SCHEMA_VERSION,
            packageName = sessionPackage,
            appLabel = sessionLabel,
            appVersion = sessionAppVersion,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            spanMs = tally.spanMs,
            samples = tally.samples,
            droppedSamples = tally.droppedSamples,
            framesAverage = tally.framesAverage,
            framesLow = tally.framesLow,
            framesHigh = tally.framesHigh,
            heatPeak = tally.heatPeak,
            powerAverage = tally.powerAverage,
            energyWh = tally.energyWh,
            jankAverage = tally.jankAverage,
            frameP99AverageMs = tally.frameP99AverageMs,
            batteryStart = tally.batteryStart,
            batteryEnd = tally.batteryEnd,
            renderer = tally.renderer,
            socTempPeak = tally.socTempPeak,
            gpuLoadAverage = tally.gpuLoadAverage,
            cpuFreqAverageMhz = tally.cpuFreqAverageMhz,
            batteryVoltageMinMv = tally.batteryVoltageMinMv,
            viewSize = sessionViewSize,
            refreshHz = sessionRefreshHz,
            powerSave = sessionPowerSave,
            stopReason = reason.id,
            clipRemoved = clip.removed,
            clipRejected = clip.rejected,
            remark = remark,
            series = downsampleSamples(clip.points)
        )
        persisted = true
        lastSaved = record
        saveScope.launch {
            if (!GameSessionStore.append(app, record)) persisted = false
            // الإشعار بعد الحفظ لا قبله: من ضغطه وجد الجلسة في السجلّ، ولم يفتح شاشةً فارغة.
            runCatching {
                val history = GameSessionStore.load(app)
                SessionSummaryNotifier.notify(app, record, sessionInsight(record, history))
            }
        }
    }

    fun clear() {
        journal = null
        live = false
        persisted = false
        sessionPackage = null
        sessionLabel = null
        sessionStartedAtMs = 0L
        sessionAppVersion = null
        sessionViewSize = null
        sessionRefreshHz = null
        sessionPowerSave = false
        remark = null
        lastStopReason = null
        lastSaved = null
        HudLive.markRecording(false)
    }

    /** ملاحظة الجلسة الحيّة؛ `null` أو فراغ يمسحها. */
    fun setRemark(value: String?) {
        remark = value?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun feed(reading: HudReading, atMs: Long) {
        if (live) journal?.record(reading, atMs)
    }

    fun tally(): HudTally? = journal?.takeIf { it.size > 0 }?.tally()

    /** نقاط الجلسة الحيّة مصغّرة — لمنحنى الشاشة. */
    fun series(): List<SessionPoint> = journal?.takeIf { it.size > 0 }
        ?.let { downsampleSamples(it.points()) }
        ?: emptyList()

    fun csv(): String? = journal?.takeIf { it.size > 0 }?.let { hudSessionCsv(it.samples()) }

    /** اسم التطبيق كما يراه المستخدم؛ وفشل الاستعلام يعني الحزمة نفسها لا اسمًا مخترعًا. */
    private fun appLabelOf(context: Context?, pkg: String): String? = runCatching {
        val manager = context?.packageManager ?: return@runCatching null
        val info = manager.getApplicationInfo(pkg, 0)
        manager.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun versionOf(context: Context, pkg: String): String? = runCatching {
        context.packageManager.getPackageInfo(pkg, 0).versionName?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun viewSizeOf(context: Context): String? = runCatching {
        val metrics = context.resources.displayMetrics
        val hz = refreshOf(context)
        val size = "${metrics.widthPixels}x${metrics.heightPixels}"
        if (hz != null) "$size@$hz" else size
    }.getOrNull()

    private fun refreshOf(context: Context): Int? = runCatching {
        context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate?.roundToInt()?.takeIf { it > 0 }
    }.getOrNull()

    private fun powerSaveOf(context: Context): Boolean = runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
    }.getOrDefault(false)
}

/**
 * المُشغِّل الواحد.
 *
 * **والفاصل زمنيّ لا وعدي:** يُقاس النوم بطرح زمن القراءة من الثانية، فلا تتراكم القراءات
 * حين تبطؤ قراءة واحدة (وهو ما كان يُطيل الدورة صامتًا في النسخة السابقة).
 */
object HudSampler {

    /** إيقاع الاستطلاع والشاشة مطفأة — خمس ثوانٍ: يُبقي الجلسة حيّة بلا كلفة ثانية بثانية. */
    private const val SCREEN_OFF_SAMPLE_MS = 5_000L

    /**
     * أدنى فاصل **وتوفير البطارية مُفعَّل** — ثانيتان.
     *
     * **ولماذا هنا:** كل عيّنة تمرّ بمسار الصدفة وقراءة عقود (‏FPS · تردّدات · حمل) — وهذا أثقل
     * عمل اختياريّ في التطبيق. وكان `isPowerSaveMode` لا يقرِّر شيئًا في هذا المسار: يُفعّل
     * المستخدم توفير البطارية فلا يتبدّل إيقاع واحد. والتراكب لا يفقد قراءة صالحة بفاصل ثانيتين،
     * ومن يفتحه يرى الأرقام تتحرّك.
     */
    private const val POWER_SAVE_SAMPLE_MS = 2_000L

    /** عمر مخبأ حالة توفير البطارية — عشر ثوانٍ (قراءتها رحلة Binder). */
    private const val POWER_SAVE_CACHE_MS = 10_000L

    private var job: Job? = null
    private var appContext: Context? = null

    /**
     * المالكون النشطون وحقول كل منهم، بترتيب الإدخال.
     *
     * **وكانوا مالكًا واحدًا («من يبدأ آخرًا يملك»)، وصاروا خريطة:** لوحة الأداء تُطلَب الآن
     * من تراكبها المستقلّ **ومن لوحة الألعاب** معًا (اختصار FPS داخل تراكب اللعبة)، فإن كان
     * المالك واحدًا أسقط خروج أحدهما قارئ الآخر. والعضويّة هنا **لا تُضاعف القراءة**: الحلقة
     * واحدة، والحقول المطلوبة تُوسَّع إلى اتحاد حقول المالكين.
     */
    private val owners = LinkedHashMap<String, () -> Set<HudField>>()
    private val lock = Any()

    /**
     * إيقاع الحلقة الحاليّ — يُقرأ في كل دورة، فيسري تغييره على حلقة تعمل بالفعل.
     *
     * وكان الإيقاع يُقرأ مرّة عند الإطلاق من أوّل مالك: تغييره من الشاشة لا يظهر حتى تُطفأ
     * اللوحة وتُعاد. والقراءة كل دورة تُغيّر ذلك بلا إعادة تشغيل خدمة ولا فقدان عيّنة.
     */
    @Volatile
    private var cadenceMs: Long = HudSnapshot.DEFAULT_INTERVAL_MS

    /**
     * نطاق الحلقة يملكه القارئ لا مُشغّله.
     *
     * **وهذا شرط صحّة التعدّد:** لو كانت الحلقة في `lifecycleScope` لخدمةٍ ما لماتت بموت تلك
     * الخدمة وهي لا تزال تخدم مالكًا آخر — عطب صامت يُقرأ «الأرقام تجمّدت». وكان حملها على نطاق
     * المُشغّل صحيحًا حين كان المالك واحدًا فمات معه كما يليق.
     */
    private val loop = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * نافذة الثبات: تُغذّى من كل عيّنة إطارات، وتُقرأ فقط حين يطلبها أحد الحقول.
     *
     * **ولماذا هي هنا لا في القارئ:** `FpsMonitorUtil` يعطي اللحظة، والنافذة تجمعها؛ ووضعها في
     * الحلقة يعني أنها تُعاد مع كل مالك (تراكب + شاشة) على حدة — وهو تكرارٌ لا يُقاس عليه.
     * و[FrameWindow] آمنة للنداء من عدّة خيوط، فالقراءة من حلقة والكتابة من أخرى لا تتزاحم.
     */
    private val frameWindow = FrameWindow()

    /** عدّاد القرص: حالة سابقة بين عيّنتين، لا قراءة معزولة. */
    private val diskIo = DiskIoSampler()

    /** نافذة كل عتبة: التحذير يُطلق بعد استمرار لا بعد لمسة. */
    private val warningTracker = HudWarningTracker()

    /** حالة الشاحن السابقة: التغيّر هو الحدث، لا الحالة. */
    private var lastPlugged: Boolean? = null

    /** تأكيد مغادرة اللعبة بعيّنتين: تبديل عابر (لوحة إشعارات) ليس خروجًا. */
    private var foregroundMisses = 0

    /**
     * أوّل مالك أو `null`.
     *
     * **و`null` تعني «بلا قارئ»:** الجزيرة تقرأها فلا تسبق مالكًا قائمًا (عقدها في [HUD_OWNER_ISLAND]).
     */
    val currentOwner: String? get() = synchronized(lock) { owners.keys.firstOrNull() }

    /**
     * يضيف مالكًا (أو يحدّث حقوله إن كان مسجّلًا)، ويُشغّل الحلقة إن لم تكن تعمل.
     *
     * @param intervalMs إيقاع الحلقة — يُقرأ من أوّل مالك يُشغّلها؛ اللاحقون يشاركونها.
     */
    fun start(
        context: Context,
        owner: String,
        fields: () -> Set<HudField>,
        intervalMs: Long = HudSnapshot.DEFAULT_INTERVAL_MS
    ) {
        appContext = context.applicationContext
        FpsMonitorUtil.init(context)
        synchronized(lock) {
            owners[owner] = fields
            cadenceMs = intervalMs
            if (job?.isActive != true) {
                val ctx = appContext!!
                val power = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                job = loop.launch {
                    var powerSave = false
                    var powerSaveReadAtMs = 0L
                    // عدّادات الشبكة السابقة: الفرق بين عيّنتين هو معدّل البايت/ثانية.
                    // `UNSUPPORTED` تعني «لا يُعلنها النظام» فتبقى القراءة غيابًا لا صفرًا.
                    var lastRx = TrafficStats.UNSUPPORTED.toLong()
                    var lastTx = TrafficStats.UNSUPPORTED.toLong()
                    var lastTrafficAt = 0L
                    while (isActive) {
                        val began = System.currentTimeMillis()
                        if (power != null && began - powerSaveReadAtMs >= POWER_SAVE_CACHE_MS) {
                            powerSaveReadAtMs = began
                            powerSave = runCatching { power.isPowerSaveMode }.getOrDefault(false)
                        }
                        // **الشاشة مطفأة: لا استطلاع** (جولة الحرارة واللاغ). الطلب كان يبقى قائمًا بلا
                        // نهاية — وكل عينة تحمل صدفة المصدر وقراءات عقد — بينما المستهلك (التراكب
                        // والشاشة) لا يُعرض أحدهما والشاشة مطفأة. فالنوم يطول (`SCREEN_OFF_SAMPLE_MS`) بدل
                        // أن يُوقَف الطلب (فيفقد من يعود من الإطفاء أوّل قراءة) أو يُترك بلا حدّ.
                        if (power?.isInteractive == false) {
                            if (HudRecorder.isLive && HudSessionSettings.load(ctx).stopOnScreenOff) {
                                HudRecorder.stop(ctx, SessionStopReason.ScreenOff)
                            }
                            delay(SCREEN_OFF_SAMPLE_MS)
                            continue
                        }
                        val fields = unionFields()
                        // القواعد تُقرأ كل دورة وقبل القراءة: البدء التلقائي يحتاجها ليعرف
                        // أن عليه قراءة الحزمة الأمامية حتى وهو خامل.
                        val rules = HudSessionSettings.load(ctx)
                        var rxBps: Float? = null
                        var txBps: Float? = null
                        val rx = TrafficStats.getTotalRxBytes()
                        val tx = TrafficStats.getTotalTxBytes()
                        if (HudField.Network in fields &&
                            rx != TrafficStats.UNSUPPORTED.toLong() &&
                            tx != TrafficStats.UNSUPPORTED.toLong()
                        ) {
                            if (lastTrafficAt > 0L && began > lastTrafficAt) {
                                val seconds = (began - lastTrafficAt) / 1000f
                                val rxDelta = rx - lastRx
                                val txDelta = tx - lastTx
                                if (seconds > 0f) {
                                    // العدّاد يُصفَّر عند تبدّل الواجهة أو إعادة التشغيل: سالبٌ يعني
                                    // «بدأ العدّ من جديد»، فتُعلن صفرًا مقيسًا لا معدّلًا وهميًّا.
                                    rxBps = if (rxDelta < 0L) 0f else rxDelta / seconds
                                    txBps = if (txDelta < 0L) 0f else txDelta / seconds
                                }
                            }
                            lastRx = rx
                            lastTx = tx
                            lastTrafficAt = began
                        }
                        val reading = read(ctx, fields, rxBps, txBps, watchForeground = rules.autoStart)
                        val plugged = pluggedIn(ctx)
                        if (HudRecorder.isLive) {
                            val previousPlug = lastPlugged
                            if (rules.stopOnPowerChange && previousPlug != null && plugged != previousPlug) {
                                HudRecorder.stop(ctx, SessionStopReason.PowerChanged)
                            }
                            val sessionPkg = HudRecorder.sessionPackage
                            val foreground = reading.foregroundPackage
                            val leftGame = rules.stopOnForegroundChange &&
                                sessionPkg != null &&
                                foreground != null &&
                                foreground != sessionPkg &&
                                foreground !in IGNORED_FOREGROUND
                            if (leftGame) {
                                // عيّنتان متتاليتان: لوحة إشعارات أو نافذة نظام عابرة ليست خروجًا.
                                foregroundMisses++
                                if (foregroundMisses >= 2) {
                                    HudRecorder.stop(ctx, SessionStopReason.ForegroundChanged)
                                }
                            } else {
                                foregroundMisses = 0
                            }
                        } else {
                            foregroundMisses = 0
                            // البدء التلقائي: لعبة من مكتبتنا صارت في المقدّمة ولم يكن هناك تسجيل.
                            // وحزمتنا مستثناة كي لا تُسجَّل الشاشة نفسها لحظة فتحها.
                            val foreground = reading.foregroundPackage
                            if (rules.autoStart &&
                                foreground != null &&
                                foreground !in IGNORED_FOREGROUND &&
                                foreground != ctx.packageName &&
                                GameWatch.isGame(ctx, foreground)
                            ) {
                                HudRecorder.start(ctx, foreground)
                            }
                        }
                        lastPlugged = plugged
                        val warnings = warningTracker.update(reading, rules, System.currentTimeMillis())
                        // الإيقاع يُقرأ هنا لا عند الإطلاق: تغييره من الشاشة يسري على الدورة التالية.
                        val interval = cadenceMs
                        if (isActive) {
                            HudLive.publish(reading, joinedOwner(), interval, warnings)
                            HudRecorder.feed(reading, System.currentTimeMillis())
                        }
                        val cadence = if (powerSave) maxOf(interval, POWER_SAVE_SAMPLE_MS) else interval
                        delay((cadence - (System.currentTimeMillis() - began)).coerceAtLeast(150L))
                    }
                }
            }
            HudLive.markOwner(joinedOwner())
        }
    }

    /**
     * يُسقط مالكًا واحدًا. **ولا تُوقف الحلقة إلا إذا خلا المالكون** — وهو الفرق الذي يجعل
     * خروج معاينة الشاشة لا يُجمّد تراكبًا يعمل، وخروج تراكب لا يمسّ لوحة الألعاب.
     * ومالك غير مسجَّل لا يفعل شيئًا (شاشة تُغلق ولم تُشغّل شيئًا).
     */
    fun stop(owner: String) {
        synchronized(lock) {
            if (owners.remove(owner) == null) return
            if (owners.isNotEmpty()) {
                HudLive.markOwner(joinedOwner())
                return
            }
            job?.cancel()
            job = null
            HudLive.markOwner(null)
        }
    }

    /** حقول المالكين مجتمعةً — عضوان لا يعنيان قراءتين، بل حقولًا أوسع لقراءة واحدة. */
    private fun unionFields(): Set<HudField> = synchronized(lock) {
        owners.values.flatMap { it() }.toSet()
    }

    /** أسماء المالكين كما تُعرض: اسم واحد، أو مشترك بـ`+` حين يقرأ أكثر من واحد. */
    private fun joinedOwner(): String = synchronized(lock) {
        owners.keys.joinToString("+")
    }

    /** أسماء حزم نظامية عابرة لا تُعدّ خروجًا من اللعبة. */
    private val IGNORED_FOREGROUND = setOf(
        "com.android.systemui",
        "com.android.permissioncontroller",
        "android"
    )

    /** هل الشاحن موصول؟ الحالة تُقرأ من البثّ اللاصق لا من تفضيل محفوظ. */
    private fun pluggedIn(context: Context): Boolean = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }.getOrDefault(false)

    private fun read(
        context: Context,
        fields: Set<HudField>,
        rxBps: Float? = null,
        txBps: Float? = null,
        watchForeground: Boolean = false
    ): HudReading {
        if (fields.isEmpty()) return HudReading()
        val wantsFrames = HudField.Frames in fields
        // الثبات يحتاج العيّنات أيضًا: من أظهر الثبات وحده دون الإطارات يجب أن تُغذّى نافذته،
        // وإلا صار الحقل شرطةً دائمةً لأنه لم يُسجَّل شيء.
        val wantsSamples = wantsFrames || HudField.Stability in fields
        val framesNow = if (wantsSamples) {
            runCatching { FpsMonitorUtil.getFps() }.getOrNull().positive()
        } else {
            null
        }
        if (wantsSamples) frameWindow.record(framesNow)
        val foreground = if (HudField.Renderer in fields || HudField.Jank in fields || HudRecorder.isLive || watchForeground) {
            runCatching { FpsMonitorUtil.getForegroundPackage() }.getOrNull().orEmpty()
        } else {
            ""
        }
        val renderer = if (HudField.Renderer in fields && foreground.isNotEmpty()) {
            runCatching { FpsMonitorUtil.getCurrentRenderer(foreground) }
                .getOrNull()?.takeIf { it.isNotBlank() && it != "FPS" }
        } else {
            null
        }
        val refreshHz = if (HudField.Jank in fields || HudField.Stability in fields) {
            refreshRateHz(context)
        } else {
            null
        }
        val frameStats = if (HudField.Jank in fields && foreground.isNotEmpty() && refreshHz != null) {
            runCatching { FpsMonitorUtil.getFrameStats(foreground, refreshHz) }.getOrNull()
        } else {
            null
        }
        val disk = if (HudField.DiskIo in fields) diskIo.sample() else null
        return HudReading(
            // `0` من القارئ تعني «لم يُجِب» لا «صفر إطار» — فتُترجم غيابًا.
            frames = if (wantsFrames) framesNow else null,
            cpu = if (HudField.Cpu in fields) {
                runCatching { FpsMonitorUtil.getCpuLoad() }.getOrNull()?.takeIf { it > 0 }
            } else {
                null
            },
            ramMb = if (HudField.Ram in fields) {
                runCatching { FpsMonitorUtil.getRamInfo(context).usedMb }.getOrNull()?.takeIf { it > 0 }
            } else {
                null
            },
            watt = if (HudField.Power in fields) {
                runCatching { FpsMonitorUtil.getPowerWatt() }.getOrNull().positive()
            } else {
                null
            },
            heat = if (HudField.Heat in fields) {
                runCatching { FpsMonitorUtil.getBatteryTemp(context) }.getOrNull().positive()
            } else {
                null
            },
            battery = if (HudField.Battery in fields) {
                runCatching {
                    context.getSystemService(BatteryManager::class.java)
                        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }.getOrNull()?.takeIf { it in 0..100 }
            } else {
                null
            },
            rxBps = if (HudField.Network in fields) rxBps else null,
            txBps = if (HudField.Network in fields) txBps else null,
            renderer = renderer,
            stability = if (HudField.Stability in fields) stabilityPercent(refreshHz) else null,
            jankPercent = frameStats?.jankPercent,
            frameP99Ms = frameStats?.p99Ms,
            cpuFreqMhz = if (HudField.CpuFreq in fields) HudDeepReaders.cpuFreqMhz() else null,
            socTempC = if (HudField.SocTemp in fields) HudDeepReaders.socTempC() else null,
            gpuFreqMhz = if (HudField.GpuFreq in fields) HudDeepReaders.gpuFreqMhz() else null,
            gpuLoadPercent = if (HudField.GpuLoad in fields) HudDeepReaders.gpuLoadPercent() else null,
            ddrFreqMhz = if (HudField.DdrFreq in fields) HudDeepReaders.ddrFreqMhz() else null,
            diskReadBps = disk?.first,
            diskWriteBps = disk?.second,
            batteryVoltageMv = if (HudField.BatteryVoltage in fields) {
                HudDeepReaders.batteryVoltageMv(context)
            } else {
                null
            },
            foregroundPackage = if (HudRecorder.isLive || watchForeground) {
                foreground.takeIf { it.isNotBlank() }
            } else {
                null
            }
        )
    }

    /**
     * معدّل تحديث الشاشة الفعلي، أو `null` حين لا يعلنه النظام — والهدف يُقرأ ولا يُفترض.
     */
    private fun refreshRateHz(context: Context): Float? = runCatching {
        context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate
    }.getOrNull()?.takeIf { it.isFinite() && it > 0f }

    /**
     * نسبة الثبات ٠..١٠٠ أو `null`.
     *
     * **والهدف يُقرأ من الشاشة لا يُفترض:** افتراض ٦٠ على شاشة ١٢٠ يجعل الجهاز «غير ثابت» أبدًا،
     * وافتراض ١٢٠ على شاشة ٦٠ يجعل الرقم مستحيلًا. وحين لا يُعلن النظام معدّله يبقى `null` — لا
     * رقمٌ مبنيّ على تخمين.
     */
    private fun stabilityPercent(refreshHz: Float?): Int? {
        val hz = refreshHz ?: return null
        return frameWindow.stableShare(hz)?.let { (it * 100f).roundToInt() }
    }
}

private fun Float?.positive(): Float? = this?.takeIf { it.isFinite() && it > 0f }
