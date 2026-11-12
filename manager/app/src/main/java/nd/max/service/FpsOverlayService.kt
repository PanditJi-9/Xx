/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
 * خدمة لوحة الأداء: تُشغّل دورة القراءة الواحدة وتملك النوافذ العائمة، وتُمرّر الرسم إلى
 * `HudSurface`. ولا تقرأ عتادًا بنفسها ولا تكتب عليه — القارئ هو `HudSampler`.
 * */

package nd.max.service

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import nd.max.R
import nd.max.core.platform.HUD_OWNER_OVERLAY
import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudSampler
import nd.max.ui.component.HudRestoreTab
import nd.max.ui.component.HudSurface
import nd.max.ui.overlay.OverlayWindow
import nd.max.ui.util.FpsOverlayPrefs
import nd.max.ui.util.HudFilter
import nd.max.ui.util.MonitorGate
import nd.max.ui.util.MonitorPrefs
import nd.max.ui.util.MonitorSpec

/**
 * النوافذ العائمة التي تُظهر أداء الجهاز فوق أيّ تطبيق آخر.
 *
 * **وما تغيّر في إعادة البناء:** هذه الخدمة كانت تحمل ثلاث مسؤوليات معًا — عقد النافذة
 * العائمة، ورسم اللوحة بثلاث شيفرات منفصلة، وقراءة المقاييس. فصارت تحمل **واحدة**: القيادة.
 *
 * | ما كان | أين صار |
 * | --- | --- |
 * | إنشاء النافذة وسحبها ومالك دورة الحياة | `ui/overlay/OverlayWindow.kt` (مشترك مع مراقب المهام) |
 * | إشعار الخدمة الأمامية | `service/OverlayForeground.kt` (مشترك) |
 * | قراءة المقاييس وحلقة التحديث | `core/platform/HudSession.kt` (`HudSampler`) |
 * | رسم اللوحة بثلاث صور | `ui/component/HudSurface.kt` (مُصيِّر واحد يستعمله التراكب والمعاينة) |
 *
 * **وتعدّد المراقبات لا يعني تعدّد الخدمات:** خدمة واحدة، و[OverlayWindow] واحدة لكل مراقب،
 * وقارئ واحد ([HudSampler]) يضمّ حقول المالكين. فإضافة مراقب ثالث لا تفتح حلقة قراءة ثالثة
 * ولا تُكرّر المراقبة التي يمنعها العقد.
 *
 * **والمراقب الأوّل يبقى على مفاتيحه القديمة** (`FpsOverlayPrefs`): لاحظه `PersistRestorer`
 * وتقرؤه الاختبارات، فتحويله إلى المخزن الجديد يهجر إعدادًا محفوظًا بلا مقابل.
 */
class FpsOverlayService : LifecycleService() {

    companion object {
        private const val PRIMARY_ID = "primary"

        var isRunning = false
            private set

        private const val CHANNEL_ID = "maxmanager_fps_overlay"
        private const val NOTIF_ID = 9021
        const val ACTION_STOP = "nd.max.service.FpsOverlayService.STOP"
        private const val PANEL_MIN_WIDTH_DP = 90f

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FpsOverlayService::class.java)
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, FpsOverlayService::class.java).setAction(ACTION_STOP)
                )
            }
        }
    }

    /** نوافذ المراقبات المفتوحة، بمعرّف المراقب. */
    private val panels = LinkedHashMap<String, OverlayWindow>()

    /**
     * طيّ كل مراقب على حِدة.
     *
     * **ولماذا خريطة لا بوليًا واحدًا:** كانت اللوحة الواحدة تُطوى وتُسترجع معًا. ومع ثلاثة
     * مراقبات، طيّ واحد كان سيُخفي الثلاثة بينما العنوان يقول إنه طوى واحدًا.
     */
    private val hidden = mutableStateMapOf<String, Boolean>()

    private var refused = false

    override fun onCreate() {
        super.onCreate()
        if (SecurityEnforcement.refuseService(this, IntegrityCheckpoint.SERVICE)) {
            refused = true
            return
        }
        hidden.clear()
        startOverlayForeground(
            OverlayNotice(
                channelId = CHANNEL_ID,
                notificationId = NOTIF_ID,
                channelNameRes = R.string.fps_overlay_notif_channel,
                titleRes = R.string.fps_overlay_notif_title,
                stopLabelRes = R.string.fps_overlay_notif_stop,
                stopAction = ACTION_STOP,
                serviceClass = FpsOverlayService::class.java
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (refused) {
            stopSelf()
            return START_NOT_STICKY
        }

        // كل نداء يزامن القائمة المحفوظة مع النوافذ القائمة: إضافة مراقب أو حذفه تظهر بلا
        // إعادة تشغيل الخدمة، وفشل نافذة واحدة لا يُسقط الباقيات.
        syncPanels()
        isRunning = panels.isNotEmpty()
        return if (isRunning) {
            START_STICKY
        } else {
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        panels.keys.toList().forEach { id ->
            panels.remove(id)?.unmount()
            HudSampler.stop(ownerOf(id))
            hidden.remove(id)
        }
        isRunning = false
        super.onDestroy()
    }

    /** مالك القارئ لهذا المراقب: الأوّل باسمه القديم، والإضافات باسمها الخاص. */
    private fun ownerOf(id: String): String =
        if (id == PRIMARY_ID) HUD_OWNER_OVERLAY else "monitor:$id"

    private fun wantedMonitors(): List<MonitorView> {
        val monitors = ArrayList<MonitorView>(MonitorPrefs.MAX_MONITORS)
        monitors.add(MonitorView(id = PRIMARY_ID, owner = HUD_OWNER_OVERLAY, primary = true))
        MonitorPrefs.load(applicationContext).forEach { spec ->
            monitors.add(MonitorView(id = spec.id, owner = ownerOf(spec.id), primary = false))
        }
        return monitors
    }

    /**
     * يُطابق النوافذ القائمة مع المطلوب: يُنشئ الناقص، ويُسقط الزائد.
     *
     * **ورفضُ التركيب يُعلن نفسه:** نافذة لم تُركَّب لا تدخل الخريطة، فإن فشلت كلها صارت
     * `isRunning = false` وتوقّفت الخدمة بدل إشعار أماميّ بلا نافذة.
     */
    private fun syncPanels() {
        val wanted = wantedMonitors()
        val wantedIds = wanted.mapTo(HashSet()) { it.id }

        panels.keys.filter { it !in wantedIds }.forEach { id ->
            panels.remove(id)?.unmount()
            HudSampler.stop(ownerOf(id))
            hidden.remove(id)
        }

        wanted.forEach { view ->
            if (panels.containsKey(view.id)) return@forEach

            val window = OverlayWindow(
                this,
                onMoved = { x, y -> savePosition(view, x, y) },
                avoidCutout = true
            )
            val mounted = runCatching {
                window.mount(
                    dragEnabled = true,
                    snapToEdges = { loadView(view).snapEdges }
                ) { Panel(view) }
            }.getOrDefault(false)

            if (!mounted) {
                runCatching { window.unmount() }
                return@forEach
            }

            panels[view.id] = window
            savedPosition(view)?.let { (x, y) -> window.restorePosition(x, y) }
            HudSampler.start(
                context = applicationContext,
                owner = view.owner,
                fields = { loadView(view).fields.toSet() },
                // الإيقاع من التفضيلات المشتركة: حلقة واحدة تخدم كل المراقبات، فلا معنى
                // لإيقاعين متزامنين — والقيمة تُقرأ كل دورة فتُطبَّق بلا إعادة تشغيل.
                intervalMs = FpsOverlayPrefs.load(applicationContext).intervalMs.toLong(),
            )
        }
    }

    private fun savePosition(view: MonitorView, x: Int, y: Int) {
        if (view.primary) {
            FpsOverlayPrefs.savePosition(applicationContext, x, y)
        } else {
            MonitorPrefs.savePosition(applicationContext, view.id, x, y)
        }
    }

    private fun savedPosition(view: MonitorView): Pair<Int, Int>? {
        if (view.primary) return FpsOverlayPrefs.loadPosition(applicationContext)
        val spec = MonitorPrefs.load(applicationContext).firstOrNull { it.id == view.id }
            ?: return null
        val x = spec.posX ?: return null
        val y = spec.posY ?: return null
        return x to y
    }

    /**
     * إعدادات الرسم لهذا المراقب، مقروءة الآن.
     *
     * والمراقب المحذوف من التخزين يعود إلى الافتراضي بدل أن يختفي فجأة: النافذة تُسحب في
     * المزامنة التالية، وما بينهما يرسم إعدادًا صالحًا لا فراغًا.
     */
    private fun loadView(view: MonitorView): HudView {
        if (view.primary) {
            val state = FpsOverlayPrefs.load(applicationContext)
            return HudView(
                fields = state.orderedFields,
                form = state.form,
                arrangement = state.arrangement,
                colorHex = state.colorHex,
                textSizeSp = state.textSizeSp,
                backgroundAlpha = state.backgroundAlpha,
                widthScale = state.widthScale,
                showGraph = state.showGraph,
                graphSpan = state.graphSpan,
                snapEdges = state.snapEdges,
                intervalMs = state.intervalMs,
                filter = state.filter
            )
        }

        val spec = MonitorPrefs.load(applicationContext).firstOrNull { it.id == view.id }
            ?: MonitorSpec(id = view.id)
        return HudView(
            fields = spec.orderedFields,
            form = spec.form,
            arrangement = spec.arrangement,
            colorHex = spec.colorHex,
            textSizeSp = spec.textSizeSp,
            backgroundAlpha = spec.backgroundAlpha,
            widthScale = spec.widthScale,
            showGraph = spec.showGraph,
            graphSpan = spec.graphSpan,
            snapEdges = spec.snapEdges,
            intervalMs = spec.intervalMs,
            filter = spec.filter
        )
    }

    /** إغلاق مراقب: الأوّل يُوقف الخدمة، والإضافي يُحذف ويُسقط وحده. */
    private fun closeMonitor(view: MonitorView) {
        if (view.primary) {
            stopSelf()
            return
        }
        MonitorPrefs.remove(applicationContext, view.id)
        panels.remove(view.id)?.unmount()
        HudSampler.stop(view.owner)
        hidden.remove(view.id)
        isRunning = panels.isNotEmpty()
    }

    @Composable
    private fun Panel(view: MonitorView) {
        val snapshot by HudLive.snapshot.collectAsState()
        // التفضيلات تُقرأ كل دورة: تغيير الحقول أو الشكل يظهر خلال ثانية بلا إعادة تشغيل.
        val hud = remember(view.id, snapshot.sampledAtMs) { loadView(view) }
        /*
         * بوّابة الإيقاع: العيّنة تصل كل ثانية، والمراقب قد يطلب ثلاثًا. واللقطة المعروضة
         * تُثبَّت بين القبولين — فبدون التثبيت يُعاد الرسم لكل عيّنة ويصير الفاصل زينة.
         */
        val gate = remember(view.id) { MonitorGate() }
        var shown by remember(view.id) { mutableStateOf(snapshot) }
        LaunchedEffect(snapshot.sampledAtMs, hud.intervalMs) {
            gate.updateInterval(hud.intervalMs)
            if (gate.accept(snapshot.sampledAtMs)) shown = snapshot
        }
        val accent = remember(hud.colorHex) {
            runCatching { Color(android.graphics.Color.parseColor(hud.colorHex)) }
                .getOrDefault(Color(0xFF00E676))
        }

        if (hidden[view.id] == true) {
            HudRestoreTab(onShow = { hidden[view.id] = false })
            return
        }

        HudSurface(
            form = hud.form,
            arrangement = hud.arrangement,
            reading = shown.reading,
            fields = hud.fields,
            accent = accent,
            textSizeSp = hud.textSizeSp,
            backgroundAlpha = hud.backgroundAlpha,
            framesHistory = shown.framesHistory.takeLast(hud.graphSpan),
            showGraph = hud.showGraph,
            recording = shown.recording,
            onToggleRecording = {
                if (HudRecorder.isLive) HudRecorder.stop(this) else HudRecorder.start(this)
            },
            onHide = { hidden[view.id] = true },
            onClose = { closeMonitor(view) },
            modifier = Modifier.widthIn(min = (PANEL_MIN_WIDTH_DP * hud.widthScale).dp),
            filter = hud.filter
        )
    }
}

/** مراقب واحد كما تراه الخدمة: معرّفه ومالكه وهل هو الأوّل. */
private data class MonitorView(
    val id: String,
    val owner: String,
    val primary: Boolean
)

/** لقطة إعدادات الرسم — تُوحّد مصدرين (القديم والجديد) في شكل واحد. */
private data class HudView(
    val fields: List<HudField>,
    val form: HudForm,
    val arrangement: HudArrangement,
    val colorHex: String,
    val textSizeSp: Float,
    val backgroundAlpha: Float,
    val widthScale: Float,
    val showGraph: Boolean,
    val graphSpan: Int,
    val snapEdges: Boolean,
    val intervalMs: Int,
    val filter: HudFilter
)
