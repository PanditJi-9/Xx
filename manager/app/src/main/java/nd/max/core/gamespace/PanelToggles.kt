/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nd.max.AppMonitorLogger
import nd.max.MaxManagerProps
import nd.max.core.privilege.PrivilegedShell
import nd.max.core.hardware.TouchControlBackend
import nd.max.core.platform.PropertyUtils
import java.util.Locale

/**
 * مقابض لوحة «المكعّب» التي تُكتب بامتياز فعلًا — عبر المسار الواحد: جذر، أو Shizuku، أو رفض معلن.
 * كل دالة `set*` تُستدعى من خيط IO فقط، وكل `*On` قراءة رخيصة؛ واللوحة تعيد القراءة بعد الكتابة
 * فلا تعرض حالة لم تحدث. أمّا التحويل الجانبي: لا أداة تُعرض هنا إن لم يكن لها أثر حقيقي على الجهاز.
 */
/** إخفاق كتابة مقبض: [key] مفتاح المقبض (`wifi` · `data` · `dnd`)، و[refused] = لم يُنفَّذ أصلًا. */
data class PanelWriteFailure(val key: String, val refused: Boolean)

object PanelToggles {
    /** نطاق للكتابة يعيش أطول من اللوحة: طيّ اللوحة بعد الضغط لا يلغي الكتابة. */
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** يستقبل الكتل المعلّقة أيضًا ([PrivilegedShell.exec]) فلا يحجب خيطًا بانتظار ربط أو مهلة. */
    fun async(block: suspend () -> Unit) {
        io.launch { runCatching { block() } }
    }

    private fun global(ctx: Context, key: String, fallback: Int): Int =
        runCatching { Settings.Global.getInt(ctx.contentResolver, key, fallback) }.getOrDefault(fallback)

    fun wifiOn(ctx: Context): Boolean = global(ctx, "wifi_on", 0) != 0
    fun setWifi(on: Boolean) = write("wifi", "svc wifi ${if (on) "enable" else "disable"}")

    fun hasCellular(ctx: Context): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun dataOn(ctx: Context): Boolean = global(ctx, "mobile_data", 1) != 0
    fun setData(on: Boolean) = write("data", "svc data ${if (on) "enable" else "disable"}")

    /** نفس مفتاح `zen_mode` الذي يكتبه `AppMonitor` عند «عدم الإزعاج أثناء اللعب». */
    fun dndOn(ctx: Context): Boolean = global(ctx, "zen_mode", 0) != 0
    fun setDnd(on: Boolean) = write("dnd", "settings put global zen_mode ${if (on) 1 else 0}")

    /**
     * المسار الواحد للكتابة المميّزة: الجذر إن وُجد، وإلا Shizuku، وإلا رفض مُعلن.
     *
     * وقبل هذه الجولة كانت الثلاثة تُنفَّذ بـ`Shell.cmd` مباشرةً: بجذر تعمل، وبلا جذر تُنفَّذ
     * بصلاحية التطبيق فترجع «permission denied» بينما اللوحة تعيد القراءة كأنها كتبت. الآن
     * النتيجة تُسجَّل بمفتاحها ورمزها وطبقته — واللوحة تعيد القراءة فلا تعرض حالة لم تحدث.
     */
    private fun write(key: String, command: String) = async {
        when (val outcome = PrivilegedShell.exec(command)) {
            is PrivilegedShell.Outcome.Ran -> if (!outcome.ok) {
                AppMonitorLogger.w(
                    "EVENT=PANEL_WRITE_FAILED key=$key lane=${outcome.lane} code=${outcome.code}",
                )
                _failures.tryEmit(PanelWriteFailure(key, refused = false))
            }

            is PrivilegedShell.Outcome.Refused -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_REFUSED key=$key reason=${outcome.reason}")
                _failures.tryEmit(PanelWriteFailure(key, refused = true))
            }

            is PrivilegedShell.Outcome.Failed -> {
                AppMonitorLogger.w("EVENT=PANEL_WRITE_FAILED key=$key reason=${outcome.reason}")
                _failures.tryEmit(PanelWriteFailure(key, refused = false))
            }
        }
    }

    /**
     * إخفاق الكتابة يُقال للمستخدم لا للّوج وحده: كان الضغط على مقبض يفشل فيعود المقبض بعد إعادة القراءة
     * ولا يعرف السبب. [PanelWriteFailure.refused] = رُفض قبل التنفيذ (الوضع/الامتياز)، وغيره = نُفِّذ وفشل.
     * بلا تراكم: الأحداث القديمة تُسقَط، فلا تتأخّر رسالة عن ضغطتها.
     */
    private val _failures = MutableSharedFlow<PanelWriteFailure>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val failures: SharedFlow<PanelWriteFailure> = _failures.asSharedFlow()

    /**
     * لا تظهر الأداة إلا إن وُجد عقدة لمس **أو** محوّل مصنّع معلن على هذا الجهاز.
     *
     * وكانت القائمة هنا **نسخة ثانية** من `TouchBoostBackend.GAME_MODE_NODES`، والنسختان تفترقان
     * بأول تعديل — فيظهر المفتاح في اللوحة ويغيب عن شاشة اللمس (أو العكس) على الجهاز نفسه.
     * والاكتشاف الآن من المالك الواحد: [TouchControlBackend.discover]، وهو الذي يكتب أيضًا.
     */
    fun touchAvailable(): Boolean {
        if (!GameRootGate.allowed()) return false
        val found = TouchControlBackend.discover()
        return found.hasBoostNode || found.vendorTransportPresent
    }

    fun touchOn(): Boolean = PropertyUtils.get(MaxManagerProps.Touch.BOOST) == "1"

    /**
     * يطبّق عبر مالك اللمس الواحد، ولا يحفظ التفضيل إلا إن لم تُرفض الكتابة ولم يغب المزوّد.
     *
     * والحفظ مشروط بـ[TouchControlBackend.Attempt.persistable] — أي «نجحت» أو «لا شيء يُفعَل
     * الآن» (`DEFERRED` عند عقدة تحمل المطلوب أصلًا)، لا «رُفضت» ولا «لا مزوّد».
     */
    fun setTouch(on: Boolean) {
        if (!GameRootGate.allowed()) return
        val attempt = TouchControlBackend.reconcile(on)
        if (attempt.persistable) PropertyUtils.set(MaxManagerProps.Touch.BOOST, if (on) "1" else "0")
    }
}

/** خطوات المنبّه بالدقائق، و`0` = متوقف. نقيّة فتُختبر بلا جهاز. */
val REMINDER_STEPS: List<Int> = listOf(30, 60, 90, 120)

fun nextReminderMinutes(current: Int): Int {
    if (current <= 0) return REMINDER_STEPS.first()
    val index = REMINDER_STEPS.indexOf(current)
    // قيمة غير معروفة (مثل 45) تبدأ من أول خطوة؛ والصفر حصرًا بعد آخر خطوة.
    if (index < 0) return REMINDER_STEPS.first()
    return if (index == REMINDER_STEPS.lastIndex) 0 else REMINDER_STEPS[index + 1]
}

/** منبّه وقت اللعب: مؤقّت في الذاكرة ينبّه بـToast + نغمة إشعار، ولا يحتاج إذنًا إضافيًّا. */
object PanelReminder {
    private val main = Handler(Looper.getMainLooper())
    private var task: Runnable? = null

    @Volatile private var minutes = 0
    @Volatile private var endsAt = 0L

    fun remainingMin(): Int =
        if (endsAt == 0L) 0 else ((endsAt - SystemClock.elapsedRealtime() + 59_999L) / 60_000L).toInt().coerceAtLeast(0)

    /** يدوّر الخطوة التالية ويعيدها (`0` = أُوقف). */
    fun cycle(ctx: Context, doneMessage: String): Int {
        cancel()
        val next = nextReminderMinutes(minutes)
        minutes = next
        if (next == 0) return 0
        val app = ctx.applicationContext
        val delayMs = next * 60_000L
        endsAt = SystemClock.elapsedRealtime() + delayMs
        val r = Runnable {
            endsAt = 0L
            task = null
            runCatching { Toast.makeText(app, doneMessage, Toast.LENGTH_LONG).show() }
            runCatching {
                RingtoneManager.getRingtone(app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play()
            }
        }
        task = r
        main.postDelayed(r, delayMs)
        return next
    }

    private fun cancel() {
        task?.let { main.removeCallbacks(it) }
        task = null
        endsAt = 0L
    }
}

/**
 * تسجيل الشاشة عبر `screenrecord` (مقاطع ≤ 3 دقائق متتابعة حتى الإيقاف) إلى `Movies/MaxManager`،
 * **بالمسار الموحَّد** ([PrivilegedShell]): جذر، أو Shizuku (uid الـshell يملك `screenrecord` والكتابة إلى
 * `/sdcard`)، أو رفض معلَن. العلم ملفّ في `/data/local/tmp` فيعيش خارج عمر اللوحة، ولو فشل
 * `screenrecord` توقّفت الحلقة بدل أن تدور بلا نهاية.
 *
 * **غير مقيس على جهاز:** استمرار الحلقة الخلفية (`&`) بعد عودة الأمر تحت خدمة Shizuku. والتحقّق من
 * الحالة يبقى على ملفّ العلم نفسه، فإن لم تستمرّ تُعلن اللوحة فشل البدء بدل «بدأ التسجيل».
 */
object PanelScreenRecorder {
    private const val DIR = "/sdcard/Movies/MaxManager"
    private const val FLAG = "/data/local/tmp/.max_screenrec"

    /** الحالة من ملفّ العلم نفسه: لو مات `screenrecord` وحذف العلم، تتصحّح اللوحة وحدها. */
    suspend fun isRecording(): Boolean =
        PanelShellOutput.lastLine(PrivilegedShell.exec("[ -f $FLAG ] && echo 1 || echo 0")) == "1"

    suspend fun start() {
        PrivilegedShell.exec(
            "mkdir -p $DIR && touch $FLAG && " +
                "((while [ -f $FLAG ]; do screenrecord --time-limit 180 $DIR/Max_\$(date +%Y%m%d_%H%M%S).mp4 " +
                "|| { rm -f $FLAG; break; }; done) >/dev/null 2>&1 &)"
        )
    }

    suspend fun stop() {
        PrivilegedShell.exec(
            "rm -f $FLAG; pkill -2 screenrecord || killall -2 screenrecord; sleep 1; " +
                "for f in $DIR/*.mp4; do am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE " +
                "-d file://\$f >/dev/null 2>&1; done"
        )
    }
}

/**
 * لقطة شاشة إلى `Pictures/MaxManager` — **والتحقق قبل الإعلان**، وبالمسار الموحَّد ([PrivilegedShell]).
 *
 * `screencap` قد يُنهي أمره رمزيًّا ويكتب ملفًا صفريًّا (محتوى محمي، أو تنفيذ بلا صلاحية
 * رسومية)، والملفّ الفارغ يُقرأ «نجاحًا» في الواجهة وهو لا شيء. فالقاعدة نفسها التي تحكم
 * القياس تحكم الالتقاط: **لا يُعلن ما لم يُقَس** — لا يُعاد مسار إلا بعد أن يقول الملفّ إنه
 * صورة (مقاسه > صفر)، وما دونه يُحذف فورًا فلا يبقى أثر يوهم بنجاح (§9.3 من الطلب).
 * ورفضُ المسار (لا امتياز في الوضع المختار) يُعامَل كأي فشل: الحجم صفر ⇒ `null`.
 */
object PanelScreenshot {
    private const val DIR = "/sdcard/Pictures/MaxManager"

    /** يلتقط ويعيد مسار الصورة، أو `null` إن فشل الالتقاط أو الكتابة. من أي سياق (يتحوّل إلى IO داخليًا). */
    suspend fun capture(): String? {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val path = "$DIR/Max_$stamp.png"
        PrivilegedShell.exec("mkdir -p $DIR && screencap -p $path")
        val size = PanelShellOutput.lastLine(PrivilegedShell.exec("stat -c %s $path 2>/dev/null || echo 0"))
            ?.toLongOrNull() ?: 0L
        if (size <= 0L) {
            PrivilegedShell.exec("rm -f $path")
            return null
        }
        PrivilegedShell.exec(
            "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$path >/dev/null 2>&1"
        )
        return path
    }
}

/** قراءة خَرْج أمر المسار الموحَّد: آخر سطر غير فارغ إن نُفِّذ فعلًا، و`null` إن رُفض أو فشل الطريق. */
internal object PanelShellOutput {
    fun lastLine(outcome: PrivilegedShell.Outcome): String? =
        (outcome as? PrivilegedShell.Outcome.Ran)?.output
            ?.lineSequence()
            ?.map { it.trim() }
            ?.lastOrNull { it.isNotEmpty() }
}

/** ساعة الجلسة: منذ متى اللعبة الحالية أمامية (تُصفَّر بتبدّل اللعبة). */
object PanelSessionClock {
    @Volatile private var pkg: String? = null
    @Volatile private var since = 0L

    fun touch(packageName: String) {
        if (packageName != pkg) {
            pkg = packageName
            since = SystemClock.elapsedRealtime()
        }
    }

    fun hoursText(): String {
        val hours = if (since == 0L) 0f else (SystemClock.elapsedRealtime() - since) / 3_600_000f
        return "%.1fh".format(Locale.US, hours)
    }
}

/** تطبيق مختصر يظهر أعلى اللوحة: يُعرض فقط إن كان مثبّتًا فعلًا وله شاشة تشغيل. */
class QuickApp(val pkg: String, val label: String, val icon: Bitmap)
