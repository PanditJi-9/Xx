/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import nd.max.R
import nd.max.core.platform.HUD_OWNER_ISLAND
import nd.max.core.platform.HudField
import nd.max.core.platform.HudSampler
import nd.max.core.security.IntegrityCheckpoint
import nd.max.core.security.SecurityEnforcement
import nd.max.ui.component.IslandSurface
import nd.max.ui.overlay.DisplayCutouts
import nd.max.ui.overlay.OverlayGeometry
import nd.max.ui.overlay.OverlayWindow
import nd.max.ui.util.IslandContent
import nd.max.ui.util.IslandGeometry
import nd.max.ui.util.IslandLive
import nd.max.ui.util.IslandPrefs

/**
 * الجزيرة الديناميكية — **خدمة التراكب** التي تملك الكبسولة فوق التطبيقات (`DYNAMIC-ISLAND-01`).
 *
 * ### تقسيم المسؤوليات (نفس تقسيم لوحة الأداء، بلا استثناء)
 *
 * | المسؤولية | موضعها |
 * | --- | --- |
 * | النافذة العائمة وإدارتها | `ui/overlay/OverlayWindow` (مشترك، بجاذبية `TOP|CENTER` الجديدة) |
 * | إشعار الخدمة الأمامية | `service/OverlayForeground` (مشترك) |
 * | المصادر والقرار | `ui/util/IslandLive` + `ui/util/IslandModel` (النموذج نقيّ يُقاس) |
 * | الرسم | `ui/component/IslandSurface` |
 * | القيادة | هذه الخدمة وحدها |
 *
 * ### وما تفعله هذه الخدمة فوق التركيب
 *
 * **حراسة متكيّفة مع إيقاظ حدثيّ** تُزامن شيئين:
 *
 * 1. **الموضع** — اختيار المستخدم (أعلى الشاشة أو تحت شريط الحالة) وإزاحتاه (يمينًا/يسارًا
 *    وأعلى/أسفل) يُطبَّقون على النافذة بلا إعادة تشغيل، والإزاحة الأفقية تُحبس بعرض الكبسولة
 *    اللحظيّ لأنها تتغيّر مع المحتوى. وطلبٌ لا يغيّر شيئًا لا يلمس `WindowManager` (`setGravity`
 *    يقارن بنفسه).
 * 2. **قارئ اللوحة** — الجزيرة **لا تسبق أحدًا**: تُشغّل `HudSampler` فقط إن كان **بلا مالك**
 *    (فشاشة مفتوحة أو تراكب قائم لهما الأولوية)، وتُوقفه عند الخروج فقط إن كانت هي التي
 *    شغّلته. وبإطفاء المؤشرات تُوقف ما شغّلته بلا أن تمسّ قارئ غيرها.
 *
 * **والكبسولة لا تُسحب بالإصبع:** `dragEnabled = false` — ولمن أراد تحريكها فمن الشاشة
 * (منازل ومقبض سحب في المعاينة)، لأن السحب فوق لعبة يسرق لمسة من اللعبة نفسها.
 */
class IslandService : LifecycleService() {

    companion object {

        /** هل الكبسولة معروضة الآن؟ تقرأه الشاشة قبل أن تعرض «يعمل». */
        var isRunning = false
            private set

        const val CHANNEL_ID = "maxmanager_dynamic_island"
        const val NOTIF_ID = 9022
        const val ACTION_STOP = "nd.max.service.IslandService.STOP"

        /** نبضة الحراسة — ثانية واحدة ما دام هناك ما يُعرض. أقلّ من ذلك كلفة بلا فرق، وأكثر تأخير يُرى. */
        private const val WATCHDOG_MS = 1_000L

        /**
         * نبضة الحراسة والجزيرة هادئة — خمس ثوانٍ (FOOTPRINT-01).
         *
         * النبضة كانت ثانيةً واحدة **دائمًا**، وكلّها تستدعي قراءة حدود الشاشة والقصّة
         * وتعيد حساب الموضع: ٣٦٠٠ دورة مجدولة في الساعة. عدد معاملات Binder الفعليّ وكلفة
         * البطارية يحتاجان قياس جهاز، ولا يُستنتجان من عدد الاستدعاءات. أحداث الموضع:
         * تدوير الشاشة، أو تغيير
         * المستخدم للإعداد، أو تغيّر عرض المحتوى. فصار الإيقاع ٥ ثوانٍ عند السكون، و**ثانيةً
         * واحدة عند وجود محتوى حيّ** (نشاط · مؤقّت · مؤشّرات)، مع استيقاظ فوريّ عند تدوير
         * الشاشة (`onConfigurationChanged`) فلا يُرى موضع قديم بعد الدوران.
         */
        private const val WATCHDOG_IDLE_MS = 5_000L

        /** حقول الجزيرة الثلاثة: الإطارات والحمل والحرارة — أوسعها فائدةً بلا طلب ثانٍ. */
        private val ISLAND_FIELDS: Set<HudField> = setOf(HudField.Frames, HudField.Cpu, HudField.Heat)

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, IslandService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    /** `null` في مسار الرفض: خدمة مرفوضة لا نافذة لها، والإغلاق لا يفترض أنها بُنيت. */
    private var island: OverlayWindow? = null

    /** إشارة استيقاظ فوريّة للحلقة (تدوير الشاشة) — بلا استطلاع. */
    private val geometryWake = kotlinx.coroutines.channels.Channel<Unit>(
        kotlinx.coroutines.channels.Channel.CONFLATED,
    )

    // حدود الشاشة والقصّة لا تتغيّر إلا بتدوير أو تغيّر نافذة — فتُقرأ عند الحاجة فقط،
    // لا في كل نبضة. و`null` تعني «لم تُقرأ بعد»؛ والقائمة الفارغة قيمة صحيحة (جهاز بلا قصّة).
    private var boundsCache: Pair<Int, Int>? = null
    private var cutoutsCache: List<OverlayGeometry.Cutout>? = null
    private var geometryDirty = true
    private val layoutListener = View.OnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
        if (r - l != oldR - oldL || b - t != oldB - oldT) geometryWake.trySend(Unit)
    }

    override fun onCreate() {
        super.onCreate()
        // بوّابة الخدمة: خدمة تعمل على نسخة معدَّلة تُنفّذ طلب المعدِّل بلا واجهة أصلًا.
        if (SecurityEnforcement.refuseService(this, IntegrityCheckpoint.SERVICE)) return
        island = OverlayWindow(
            context = this,
            windowGravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            // الكبسولة لا فعل عند النقر: تمرّر اللمس ولا تسرق سحب شريط الإشعارات.
            touchable = false,
            // الجزيرة تُقاس من أعلى الشاشة لا من تحت شريط الحالة: بهذا وحده يصير «أسفل الكاميرا» موضعًا حقيقيًّا.
            screenCoordinates = true
        )
        startOverlayForeground(
            OverlayNotice(
                channelId = CHANNEL_ID,
                notificationId = NOTIF_ID,
                channelNameRes = R.string.dynamic_island_notif_channel,
                titleRes = R.string.dynamic_island_notif_title,
                // النصّ العامّ («إيقاف») يُعاد استعماله: هو نفسه في كل تراكب، وتكراره نصًّا
                // ثانيًا بنفس المعنى يزيد مفتاحًا بلا معنى.
                stopLabelRes = R.string.fps_overlay_notif_stop,
                stopAction = ACTION_STOP,
                serviceClass = IslandService::class.java
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val panel = island ?: return START_NOT_STICKY
        if (!panel.isShown) {
            val mounted = panel.mount(dragEnabled = false) { IslandSurface() }
            if (!mounted) {
                // إذن الرسم فوق التطبيقات سُحب أو النافذة رُفضت: لا كبسولة، ولا خدمة بلا عمل.
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
            isRunning = true
            panel.hostView()?.addOnLayoutChangeListener(layoutListener)
            IslandLive.start(
                context = applicationContext,
                listenerComponent = ComponentName(this, MaxNotificationListener::class.java)
            )
            watchdog()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        HudSampler.stop(HUD_OWNER_ISLAND)
        IslandLive.stop()
        island?.hostView()?.removeOnLayoutChangeListener(layoutListener)
        island?.unmount()
        isRunning = false
        super.onDestroy()
    }

    private fun watchdog() {
        // Reuse the existing preference StateFlow: no new preferences poll or disk reader.
        lifecycleScope.launch {
            IslandLive.config.collect { geometryWake.trySend(Unit) }
        }
        lifecycleScope.launch {
            while (isActive) {
                // قراءة واحدة للتفضيلات لكل نبضة: الموضع والمصادر يقرآن المفاتيح نفسها،
                // وقراءتان متتاليتان في الثانية ثمنٌ بلا مقابل (تكملة `PERF-AUDIT-01`).
                val prefs = IslandLive.config.value ?: IslandPrefs.load(applicationContext)
                syncPosition(prefs)
                syncSampler(prefs)
                // النبضة السريعة تُشترى فقط حين يكون هناك ما يتغيّر فعلًا: محتوى حيّ أو مؤشّرات.
                // التدوير والتفضيلات وقياس محتوى جديد توقظ الحلقة دون انتظار الإيقاع البطيء.
                val busy = prefs.showIndicators ||
                    IslandLive.state.value.content != IslandContent.Hidden
                withTimeoutOrNull(if (busy) WATCHDOG_MS else WATCHDOG_IDLE_MS) {
                    geometryWake.receive()
                }
            }
        }
    }

    /**
     * التدوير يُبطل الهندسة المخبّأة ويُوقظ الحلقة فورًا — لا انتظار خمس ثوانٍ لموضع جديد.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        geometryDirty = true
        geometryWake.trySend(Unit)
    }

    /**
     * الموضع كاملًا يُطبَّق: الأساس (أعلى الشاشة، أو تحت شريط الحالة، أو أسفل الكاميرا) + إزاحتا المستخدم.
     *
     * **والقراءة كل نبضة لا عند التغيير:** الأساس ثابت في الجهاز لكن الاتجاه يقلبه، وعرض الكبسولة
     * يتغيّر مع محتواها (نقطة هادئة ← إشعار بعنوان طويل)، وحدّ الإزاحة الأفقية يعتمد عليه — فإعادة
     * الحساب كل ثانية هي ما يمنع كبسولةً واسعة عند أقصى اليمين من الخروج بحافّتها. والحساب رخيص ودالّته نقيّة.
     */
    private fun syncPosition(prefs: IslandPrefs.State) {
        val panel = island ?: return
        // **القراءتان تُخبَّآن (FOOTPRINT-01):** الحدود وقراءة قصّة الشاشة تتغيّران بتدوير
        // أو تبدّل نافذة. وكانتا في كل نبضة (٣٦٠٠/ساعة) بلا فائدة. والقصّة الفارغة قيمة صحيحة
        // (جهاز بلا قصّة) فلا تُخلط بـ`null` التي تعني «لم تُقرأ بعد».
        val bounds = boundsCache?.takeIf { !geometryDirty } ?: panel.screenBounds().also { boundsCache = it }
        val (screenWidth, screenHeight) = bounds
        val cutouts = cutoutsCache?.takeIf { !geometryDirty } ?: run {
            val host = panel.hostView()
            if (host == null || !host.isAttachedToWindow || host.display == null) {
                emptyList()
            } else {
                DisplayCutouts.of(host).also { cutoutsCache = it }
            }
        }
        if (boundsCache != null && cutoutsCache != null) geometryDirty = false

        val size = panel.contentSize()
        val islandWidth = size?.first ?: 0
        val islandHeight = size?.second ?: 0
        val cameraBottom = IslandGeometry.cameraBottomPx(cutouts, screenWidth, screenHeight)
        val baseY = IslandGeometry.anchorYPx(
            prefs.position,
            DisplayCutouts.statusBarPx(resources),
            cameraBottom
        )
        val x = IslandGeometry.clampOffsetXPx(
            IslandGeometry.offsetXPx(prefs.offsetXf, screenWidth),
            screenWidth,
            islandWidth
        )
        val wanted = baseY + IslandGeometry.offsetYPx(prefs.offsetYf, screenHeight)
        // أسفل الشاشة لا تخرج منه الكبسولة: أقصى y يترك لها ارتفاعها كاملًا.
        val y = wanted.coerceIn(0, (screenHeight - islandHeight).coerceAtLeast(0))
        panel.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, x, y)
    }

    private fun syncSampler(prefs: IslandPrefs.State) {
        val wanted = prefs.showIndicators
        val owner = HudSampler.currentOwner
        when {
            wanted && owner == null -> HudSampler.start(
                context = applicationContext,
                owner = HUD_OWNER_ISLAND,
                fields = { ISLAND_FIELDS }
            )

            !wanted && owner == HUD_OWNER_ISLAND -> HudSampler.stop(HUD_OWNER_ISLAND)
        }
    }
}
