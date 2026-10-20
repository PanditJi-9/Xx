/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * الجزيرة الديناميكية — **الطبقة البيانية**: تُقرأ منها الكبسولة وتُغذّى من ثلاثة مصادر،
 * ولا واجهة في هذا الملف.
 *
 * ### المصادر الثلاثة ومن يملكها
 *
 * 1. **البطارية** — لاصق نظام (`ACTION_BATTERY_CHANGED`) يُسلَّم عند التسجيل نفسه، فلا استطلاع
 *    ولا إذن: الشحن حالة يعلنها النظام لا قياس نطلبه.
 * 2. **الوسائط** — جلسات النظام عبر `MediaSessionManager`، وشرطها الوحيد أن يكون مستمع
 *    الإشعارات مُفعَّلًا (المنصّة تشترطه على `getActiveSessions`). ولذلك الشاشة تقول ذلك
 *    صريحًا بدل أن تعرض صفًّا لا يمتلئ أبدًا.
 * 3. **المؤشرات** — من قارئ اللوحة المشترك (`HudLive`) لا من قارئ ثانٍ: قراءتان متوازيتان
 *    على جهاز يُقاس أداؤه ثمن بلا مقابل. ومالك القارئ يبقى واحدًا (`HUD_OWNER_ISLAND` لا
 *    يسبق أحدًا: لا يبدأ إلا إن كان القارئ بلا مالك — والقرار في `IslandService`).
 *
 * **والعابر لا مصدر له هنا:** يدفعه مستمع الإشعارات (`MaxNotificationListener`) بـ[pushNotice]،
 * ويُقرأ بعمره في `IslandModel` — فالجزيرة لا تحتفظ بإشعارات بل بآخر إشعار، وهذا هو الفرق بين
 * كبسولة تُلاحَظ وسجلّ إشعارات مصغّر.
 *
 * **والحلقة الواحدة (٥٠٠ms) مقصودة:** العابر يسقط بزمن لا بحدث، فبلا نبضة دورية يبقى إشعار
 * انتهت مدّته على الشاشة. والنبضة **لا تقرأ شيئًا** — تُعيد القرار على ما هو محفوظ (كائن
 * صغير في الذاكرة)، فالكلّفة جمعُ قيمة لا استدعاء نظام.
 */
package nd.max.ui.util

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nd.max.core.platform.HudLive
import kotlin.math.roundToInt

object IslandLive {

    /** نبضة إسقاط العابر: أسرع من أقصر مدّة عرض (٣ ثوانٍ) بهامش مريح. */
    private const val TICK_MS = 500L

    private val mutable = MutableStateFlow(IslandState())

    /** ما تعرضه الجزيرة الآن — تُقرأ من الكبسولة، وتُستبدل كل نبضة أو حدث. */
    val state: StateFlow<IslandState> = mutable.asStateFlow()

    private val configMutable = MutableStateFlow<IslandPrefs.State?>(null)

    /**
     * آخر مفاتيح مقروءة — تنشرها هذه الطبقة ليقرأها الرسم.
     *
     * **ولماذا لا يقرأ الرسم الملفّ بنفسه:** قراءة الملفّ داخل `remember(state)` كانت ستُجمّد
     * اللون والمقاس حتى أوّل تغيّر محتوى (والحالة لا تتغيّر وهي ثابتة، فلا إعادة تركيب).
     * فالنشر هنا يجعل تغيير اللون من الشاشة يظهر خلال النبضة التالية (٥٠٠ms) — والنبضة تقرأ
     * الملفّ أصلًا لتصفية المصادر، فلا قراءة زائدة.
     */
    val config: StateFlow<IslandPrefs.State?> = configMutable.asStateFlow()

    private var appContext: Context? = null
    private var sources = IslandInputs()
    private var ticker: Job? = null
    private var hudWatch: Job? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var mediaListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var mediaManager: MediaSessionManager? = null
    private var owner: CoroutineScope? = null

    /**
     * عدّاد المالكين — والسبب مقيس لا احتياطيّ.
     *
     * للجزيرة **مالكان**: الخدمة (الكبسولة فوق التطبيقات) وشاشة الإعداد (معاينة حيّة). ولو كان
     * `start`/`stop` ثنائيًّا لكان إيقاف أحدهما يُسقط مصادر الآخر: من يفتح الشاشة ثم يُشغّل
     * الجزيرة ثم يُوقفها كان يفقد المعاينة، والعكس كذلك. فالعدّاد يجعل التسجيل مرّة واحدة
     * والتسليم مرّة واحدة: أوّل مالك يُسجّل، وآخر مالك يُنظّف.
     */
    private var starts = 0

    /**
     * نطاق خاصّ لا يُستعار: التكّة (ticker) تعيش أطول من الشاشة التي بدّلت المصادر. ولو مرّرت
     * الشاشة نطاقها لتوقّفت النبضة عند خروجها بينما الخدمة ما زالت تعرض.
     */
    private var scope: CoroutineScope? = null

    /**
     * يُسجّل المصادر (مرّة واحدة، والعدّاد يمنع الإغلاق المبكر).
     *
     * @param listenerComponent مكوّن مستمع الإشعارات — يُمرَّر من المستدعي لا يُبنى هنا، فلا
     *   يعرف هذا الملف صنفًا في طبقة الخدمة، ولا يقرأ جلسات الوسائط من لا يملك الشرط.
     */
    fun start(context: Context, listenerComponent: ComponentName) {
        starts += 1
        if (starts > 1) return
        val app = context.applicationContext
        appContext = app
        sources = IslandInputs()
        val fresh = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = fresh
        startBattery(app)
        startMedia(app, listenerComponent)
        startIndicators(fresh)
        ticker = fresh.launch {
            while (isActive) {
                recompute()
                delay(TICK_MS)
            }
        }
        recompute()
    }

    /** يُسلّم ملكًا واحدًا — والنطاق يُلغى ويرجع القرار إلى «مخفي» عند رحيل آخر مالك. */
    fun stop() {
        if (starts == 0) return
        starts -= 1
        if (starts > 0) return
        scope?.cancel()
        scope = null
        ticker = null
        hudWatch = null
        batteryReceiver?.let { receiver ->
            runCatching { appContext?.unregisterReceiver(receiver) }
        }
        batteryReceiver = null
        mediaListener?.let { listener ->
            runCatching { mediaManager?.removeOnActiveSessionsChangedListener(listener) }
        }
        mediaListener = null
        mediaManager = null
        appContext = null
        sources = IslandInputs()
        configMutable.value = null
        mutable.value = IslandState()
    }

    /**
     * يُعيد تسجيل جلسات الوسائط — بعد منح صلاحية الإشعارات **والجزيرة تعمل**.
     *
     * والسبب أنّ شرط المنصّة يُفحص لحظة التسجيل: من شغّل الجزيرة قبل المنح بقي بلا وسائط حتى
     * إعادة تشغيلها. وهذا النداء يجعل المنح يُطبَّق فورًا. ولا يفعل شيئًا إن لم يكن للجزيرة مالك.
     */
    fun refreshMedia(context: Context, listenerComponent: ComponentName) {
        if (starts == 0) return
        mediaListener?.let { listener ->
            runCatching { mediaManager?.removeOnActiveSessionsChangedListener(listener) }
        }
        mediaListener = null
        mediaManager = null
        startMedia(context.applicationContext, listenerComponent)
    }

    /** آخر إشعار عابر — يستبدل سابقه، ولا يُرتَّب معه (كبسولة لا سجلّ). */
    fun pushNotice(notice: IslandNotice) {
        sources = sources.copy(notice = notice)
        recompute()
    }

    // ── المصادر ───────────────────────────────────────────────────────────

    private fun startBattery(context: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
                // «الشحن» يشمل المكتمل: الجهاز على الشاحن — وهي الحالة التي تهمّ من ينظر.
                val charging = plugged != 0 ||
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                sources = sources.copy(
                    charging = if (charging && level >= 0 && scale > 0) {
                        IslandContent.Charging(level * 100 / scale)
                    } else {
                        null
                    }
                )
                recompute()
            }
        }
        batteryReceiver = receiver
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun startMedia(context: Context, listenerComponent: ComponentName) {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            publishMedia(controllers)
        }
        mediaManager = manager
        mediaListener = listener
        // بلا مستمع إشعارات مُفعَّل يرمي النظام `SecurityException` — وهو ليس عطبًا بل شرطًا
        // معلنًا، فتسقط القراءة إلى «لا وسائط» بلا ضجيج.
        runCatching { manager.addOnActiveSessionsChangedListener(listener, listenerComponent) }
        publishMedia(runCatching { manager.getActiveSessions(listenerComponent) }.getOrNull())
    }

    private fun publishMedia(controllers: List<MediaController>?) {
        // **الفعّالة وحدها:** جلسة موقوفة تحمل عنوانًا وتُعرض أبدًا كانت ستُبقي الكبسولة معلّقة
        // على شيء لا يعمل. الجزيرة تقول ما يُشغَّل الآن، والموقوف لا يُشغَّل.
        val playing = controllers?.firstOrNull { controller ->
            controller.playbackState?.isActive == true &&
                !controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
        }
        sources = sources.copy(
            media = playing?.let { controller ->
                IslandContent.Media(
                    title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                    artist = controller.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() }
                )
            }
        )
        recompute()
    }

    private fun startIndicators(scope: CoroutineScope) {
        hudWatch = scope.launch {
            HudLive.snapshot.collect { snapshot ->
                val reading = snapshot.reading
                // بلا مالك للقارئ لا أرقام حيّة: آخر قراءة تبقى في اللقطة بعد التوقّف، وعرضها
                // كأنها الآن هو الرقم الكاذب الذي يمنعه هذا المشروع في كل طبقاته.
                val indicators = if (snapshot.owner == null || reading == null) {
                    null
                } else {
                    IslandContent.Indicators(
                        frames = reading.frames?.roundToInt(),
                        cpuPercent = reading.cpu,
                        heatC = reading.heat
                    )
                }
                sources = sources.copy(indicators = indicators)
                recompute()
            }
        }
    }

    /** القرار: تُقرأ المفاتيح (أيّ مصادر تُعرض) ثم يقرّر النموذج النقيّ الأولوية. */
    private fun recompute() {
        val prefs = appContext?.let { IslandPrefs.load(it) }
        if (prefs != null) configMutable.value = prefs
        val gated = if (prefs == null) {
            sources
        } else {
            IslandInputs(
                notice = sources.notice.takeIf { prefs.showNotices },
                media = sources.media.takeIf { prefs.showMedia },
                charging = sources.charging.takeIf { prefs.showCharging },
                indicators = sources.indicators.takeIf { prefs.showIndicators }
            )
        }
        mutable.value = IslandModel.decide(gated, System.currentTimeMillis(), prefs?.duration?.scale ?: 1f)
    }
}
