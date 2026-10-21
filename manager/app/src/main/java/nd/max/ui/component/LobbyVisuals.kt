/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.component

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Date
import kotlinx.coroutines.delay

/**
 * لغة اللوبي البصرية — **داكنة دائمًا**: اللوبي سطح لعب لا صفحة إعدادات، فلا يتبع ثيم النظام
 * الفاتح (نفس سابقة `GamePanelSurface`). الألوان هنا ثوابت السطح وحده؛ أمّا الفراغات والأنصاف فتأتي
 * من `MaxSpace`/`MaxRadius` حتى لا يتفرّع لها نظام ثانٍ.
 *
 * ### الشكل الحالي: حلقة 3D + مركز تحكم
 *
 * مسرح بحلقة بطاقات ثلاثية الأبعاد (ميل وعمق وتقارب) يمين عمود معلومات اللعبة وزرّ «ابدأ»، وسحبة
 * رأسية تنقل المسرح إلى «مركز التحكم» الذي يضمّ كل الإعدادات في سطح واحد. التخطيط والأشكال والألوان
 * مرسومة هنا بالكود، **ولم يُنقل أيّ أصل** — والصور هي أيقونات التطبيقات المثبَّتة على الجهاز.
 *
 * ### أحمر العلامة ثابت هنا عن قصد
 *
 * `DESIGN.md` يجعل الـaccent مستعارًا من ثيم المستخدم. اللوبي سطح لعب بهوية ثابتة، فيُثبَّت
 * أحمره (استثناء معلن لهذه الشاشة وللّوحة الجانبية). وبما أنّ الأحمر هو العلامة فهو **لا يعني
 * خطرًا أبدًا**: حالات التحذير تأخذ [Caution]/[Positive] الثابتتين مع أيقونة، لا أحمرًا وحده.
 */
object LobbyPalette {
    val Black = Color(0xFF050608)
    val Surface = Color(0xFF0E1015)
    val Panel = Color(0xFF1B1E2C)
    val PanelRaised = Color(0xFF282D44)
    val PanelSelected = Color(0xFF454B78)
    val Red = Color(0xFFE5262B)
    val RedBright = Color(0xFFFF5257)
    val RedDeep = Color(0xFF5A0A10)
    val Ink = Color(0xFFF2F3F8)
    val Muted = Color(0xFF8F96AB)
    val Hairline = Color(0x33FFFFFF)

    /** سيان — للبيانات الحيّة فقط (حافة البطاقة، قراءات الجاهزية)، ولا يُستعمل لزرّ إجراء. */
    val Cyan = Color(0xFF3DD9FF)

    /**
     * ألوان مسرح اللوبي (الشكل العرضيّ ذو الأجنحة): كحليّ داكن للأرضية، ووردي-أحمر نيون وأزرق ملكيّ
     * للحافة المتوهّجة والأزرار، وفولاذيّ باهت للأجنحة والخلايا السداسية الزخرفية. ثوابت السطح وحده.
     */
    val Navy = Color(0xFF060B1A)
    val NavyGlow = Color(0xFF14337F)
    val Neon = Color(0xFFFF2D55)
    val Blue = Color(0xFF2F7BFF)
    val BlueBright = Color(0xFF6FA8FF)
    val BlueDeep = Color(0xFF0B1B45)
    val Steel = Color(0xFF9AA6C0)

    /** نفس `Positive` في `DESIGN.md` على الداكن (تباين ١٠٫٢٣). */
    val Positive = Color(0xFF5FD9AC)

    /** نفس `Caution` في `DESIGN.md` على الداكن (تباين ١٠٫٥٠). */
    val Caution = Color(0xFFFFB86B)
}

/**
 * هل الحركة مسموحة؟ — مقياس النظام `animator_duration_scale`؛ إذا صفّره المستخدم لا تتنفّس الحافة.
 * (دعم الحركة المخفَّضة يمرّ من هذا الباب الواحد، لا من فحص في كل مكوّن.)
 */
@Composable
fun rememberLobbyAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}

/**
 * خلفية اللوبي: أسود بتوهّج بلون اللعبة المحدَّدة خلف البطاقة المركزية، وقاعدة حمراء خافتة من
 * الأسفل. اللون يأتي من أيقونة اللعبة نفسها فتتبدّل الخلفية معها بلا أي أصل مرافق.
 */
@Composable
fun LobbyBackdrop(tone: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(LobbyPalette.Black)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(tone.copy(alpha = 0.30f), tone.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.46f),
                        radius = size.width * 0.55f
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, LobbyPalette.RedDeep.copy(alpha = 0.40f)),
                        startY = size.height * 0.55f,
                        endY = size.height
                    )
                )
            }
    )
}

// ───────────────────────────── نافذة الشاشة: عرضي + ملء الشاشة ─────────────────────────────

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** الاتجاه الذي يُعلنه المانيفست لهذا النشاط، أو «غير محدَّد» إن تعذّرت قراءته. */
private fun Activity.manifestOrientation(): Int = runCatching {
    packageManager.getActivityInfo(componentName, 0).screenOrientation
}.getOrDefault(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

/**
 * يقفل الاتجاه عرضيًّا ويُخفي أشرطة النظام ما دام اللوبي ظاهرًا، ويُعيد ما كان عند الخروج.
 * الأشرطة تظهر بسحبة من الحافة (سلوك النظام)، فلا يُحبس المستخدم خارج التنقّل.
 *
 * ### لماذا يُستعاد الاتجاه المُعلَن في المانيفست لا قيمته الحيّة
 * القيمة الحيّة تُقرأ من سجلّ النظام، والسجلّ **يبقى بعد قتل العملية في الخلفية**، ولا يُستدعى الخروج
 * (`onDispose`) حينئذٍ. فإذا أُعيد اللوبي من حالته المحفوظة قرأ القفل العرضي القديم كأنه «الأصل»،
 * فعاد التطبيق عرضيًّا بعد كل خروج. وهذا يفسّر أن العطب يظهر أحيانًا لا دائمًا.
 */
@Composable
fun LobbyWindowEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        val restoreOrientation = activity?.manifestOrientation() ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = restoreOrientation
        }
    }
}

/**
 * يُبقي الشاشة مضاءة ما دام اللوبي ظاهرًا و[enabled] صحيح، ويُزيل العلَم عند الخروج أو الإيقاف.
 * لا أحد في التطبيق يضع هذا العلَم غير اللوبي، فإزالته آمنة ولا تمسّ شاشة أخرى.
 */
@Composable
fun LobbyKeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val window = if (enabled) context.findActivity()?.window else null
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

// ───────────────────────────── قراءات الجاهزية ─────────────────────────────

/**
 * @param tempTenthsC حرارة البطارية بعُشر الدرجة كما يُبلّغها النظام، و`null` إن لم تُبلَّغ.
 *   هي حرارة **البطارية** لا المعالج: المعالج يحتاج مسارًا لا يملكه اللوبي، فلا يُدَّعى.
 */
data class LobbyBattery(val percent: Int, val charging: Boolean, val tempTenthsC: Int? = null)

/** بطارية الجهاز: قراءة لاصقة (sticky) بلا تسجيل مستقبِل، تُجدَّد كل نصف دقيقة. `null` قبل أول قراءة. */
@Composable
fun rememberLobbyBattery(): State<LobbyBattery?> {
    val context = LocalContext.current
    return produceState<LobbyBattery?>(null, context) {
        while (true) {
            value = readBattery(context)
            delay(30_000L)
        }
    }
}

private fun readBattery(context: Context): LobbyBattery? = runCatching {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    LobbyBattery(percent = (level * 100 / scale).coerceIn(0, 100), charging = charging, tempTenthsC = temp)
}.getOrNull()

data class LobbyMemory(val availBytes: Long, val totalBytes: Long)

/** ذاكرة النظام الحرّة (`ActivityManager.MemoryInfo`، بلا إذن)، تُجدَّد كل عشر ثوانٍ. `null` قبل أول قراءة. */
@Composable
fun rememberLobbyMemory(): State<LobbyMemory?> {
    val context = LocalContext.current
    return produceState<LobbyMemory?>(null, context) {
        while (true) {
            value = readMemory(context)
            delay(10_000L)
        }
    }
}

private fun readMemory(context: Context): LobbyMemory? = runCatching {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    LobbyMemory(availBytes = info.availMem, totalBytes = info.totalMem)
}.getOrNull()

/** ساعة الجهاز بتنسيق النظام (12/24)، تتحدّث عند بداية كل دقيقة. */
@Composable
fun rememberLobbyClock(): State<String> {
    val context = LocalContext.current
    return produceState("", context) {
        val format = DateFormat.getTimeFormat(context)
        while (true) {
            val now = System.currentTimeMillis()
            value = format.format(Date(now))
            delay(60_000L - now % 60_000L + 50L)
        }
    }
}
