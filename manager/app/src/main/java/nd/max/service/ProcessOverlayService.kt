/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.service

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.core.platform.ProcessFeed
import nd.max.core.platform.ProcessWatch
import nd.max.core.platform.ProcessWatchSettings
import nd.max.ui.component.ProcessSurface
import nd.max.ui.component.baselineOf
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.overlay.OverlayWindow
import nd.max.ui.util.ProcessOverlayPrefs

/**
 * نافذة مراقب المهام العائمة — **قيادة فقط**.
 *
 * ### ما كانت وما صارت
 *
 * كانت تحمل أربع مسؤوليات في ملفّ واحد: عقد `WindowManager` والسحب، وإشعار الخدمة الأمامية،
 * وقراءة `top` وحلقتها، ورسم القائمة. ومنها كان العطب الحقيقي: **السحب بلا حدّ** (`FLAG_LAYOUT_NO_LIMITS`
 * يُجيز إخراج النافذة عن الشاشة فلا تُرجَع)، وثمانية صفوف وفاصل ثانيتين **ثوابت لا خيار**، و`emptyList()`
 * عند الفشل تُقرأ «لا توجد عمليات».
 *
 * | ما كان | أين صار |
 * | --- | --- |
 * | النافذة والسحب والالتصاق ومالك دورة الحياة | [OverlayWindow] (مشترك مع لوحة الأداء) |
 * | الإشعار الأمامي وزرّ الإيقاف | [startOverlayForeground] (مشترك) |
 * | قراءة الشجرة | [ProcessFeed] + [ProcessWatch] (قارئ واحد للشاشة والتراكب) |
 * | الرسم | [ProcessSurface] (يُرسم بها في الشاشة أيضًا، فلا فرق بين المعاينة والحقيقة) |
 *
 * **والتفضيلات تُقرأ كل دورة** ([ProcessOverlayPrefs]) لا عند الإقلاع: تغيير عدد الصفوف أو
 * الفاصل أو الترتيب من الشاشة يظهر في النافذة بلا إطفائها وتشغيلها.
 */
class ProcessOverlayService : LifecycleService() {

    companion object {
        /** هل التراكب معروض الآن؟ تقرأه الشاشة قبل أن تَعِد المستخدم بشيء. */
        var isRunning = false
            private set

        const val CHANNEL_ID = "maxmanager_process_overlay"
        const val NOTIF_ID = 9022
        const val ACTION_STOP = "nd.max.service.ProcessOverlayService.STOP"

        /** معرّف هذا الطالب عند القارئ المشترك — واحد لا يتكرّر. */
        private const val WATCH_OWNER = "process_overlay"
    }

    private lateinit var window: OverlayWindow

    /**
     * هل حقل البحث مفتوح الآن؟
     *
     * يقرأه طلب القراءة أدناه ليطلب **سقف القارئ** بدل صفوف اللوحة: البحث في ثلاثة صفوف
     * ليس بحثًا. ويُرفع من داخل `Panel` عبر `LaunchedEffect`، فلا تتغيّر دورة القراءة إلا
     * بعد أن يُركَّب الحقل فعلًا.
     */
    @Volatile
    private var searchOpen = false

    override fun onCreate() {
        super.onCreate()
        // بوّابة الخدمة: خدمة تعمل على نسخة معدَّلة تُنفّذ طلب المعدِّل بلا واجهة أصلًا.
        if (SecurityEnforcement.refuseService(this, IntegrityCheckpoint.SERVICE)) return
        window = OverlayWindow(this)
        startOverlayForeground(
            OverlayNotice(
                channelId = CHANNEL_ID,
                notificationId = NOTIF_ID,
                channelNameRes = R.string.processmgr_notif_channel,
                titleRes = R.string.processmgr_notif_title,
                stopLabelRes = R.string.processmgr_notif_stop,
                stopAction = ACTION_STOP,
                serviceClass = ProcessOverlayService::class.java
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!window.isShown) {
            val mounted = window.mount(
                dragEnabled = true,
                // يُقرأ لحظة الإفلات لا لحظة التركيب: إطفاء الالتصاق يُطبَّق بلا إعادة تشغيل.
                snapToEdges = { ProcessOverlayPrefs.load(applicationContext).snapEdges }
            ) {
                Panel()
            }
            if (!mounted) {
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
            isRunning = true
            ProcessWatch.start(
                context = applicationContext,
                owner = WATCH_OWNER,
                scope = lifecycleScope,
                settings = {
                    val prefs = ProcessOverlayPrefs.load(applicationContext)
                    // النافذة تطلب عدد صفوفها هي، والفاصل الذي اختاره المالك للتراكب.
                    // وحال البحث تطلب السقف، وإلا لبحث القارئ في خمسة صفوف فقط.
                    ProcessWatchSettings(
                        limit = if (searchOpen) ProcessWatch.MAX_ROWS else prefs.rows,
                        intervalSeconds = prefs.intervalSeconds
                    )
                }
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ProcessWatch.stop(WATCH_OWNER)
        window.unmount()
        isRunning = false
        super.onDestroy()
    }

    /** اللوحة: تُرتَّب من العيّنة نفسها التي تراها الشاشة، فالمعاينة والحقيقة مصدرهما واحد. */
    @Composable
    private fun Panel() {
        val sample by ProcessWatch.snapshot.collectAsState()
        val tick by ProcessWatch.samples.collectAsState()
        val prefs = remember(tick) { ProcessOverlayPrefs.load(applicationContext) }
        var typing by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }

        // التركيز يُرفع مع الحقل ويُعاد مع إغلاقه: نافذة غير قابلة للتركيز لا تُظهر لوحة مفاتيح،
        // وإبقاؤها قابلة للتركيز بعد البحث يسلب اللمس من اللعبة تحتها.
        LaunchedEffect(typing) {
            searchOpen = typing
            window.setFocusable(typing)
        }

        // أثناء البحث نُصفّي الشجرة كلها (سقف القارئ) لا صفوف اللوحة الخمسة.
        val rows = if (typing) ProcessWatch.MAX_ROWS else prefs.rows
        val shown = remember(sample, prefs, query, rows) {
            ProcessFeed.arrange(sample.readings, prefs.scope, query, prefs.sort, rows)
        }

        ProcessSurface(
            sample = sample.copy(readings = shown),
            sort = prefs.sort,
            baseline = baselineOf(shown, prefs.sort),
            textSizeSp = prefs.textSizeSp,
            backgroundAlpha = prefs.backgroundAlpha,
            markHeavy = prefs.markHeavy,
            showTally = prefs.showTally,
            onClose = { stopSelf() },
            headerAction = {
                if (!typing) {
                    IconButton(
                        onClick = { typing = true },
                        modifier = Modifier.size(MaxSize.iconGlyph)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.processmgr_search_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            searchRow = {
                if (typing) {
                    OverlaySearchRow(
                        query = query,
                        textSizeSp = prefs.textSizeSp,
                        onQuery = { query = it },
                        onDismiss = {
                            query = ""
                            typing = false
                        }
                    )
                }
            }
        )
    }

    /**
     * حقل البحث داخل النافذة العائمة — لا صفّ في الترويسة.
     *
     * [`BasicTextField`] لا [`OutlinedTextField`]: ارتفاع الأخير يبتلع ثلاثة صفوف من لوحة
     * صغيرة، ونحن نضع الحقل تحت العنوان لا في نموذج. والحدّ نفسه رمز قائم (`MaxRadius.chip`).
     */
    @Composable
    private fun OverlaySearchRow(
        query: String,
        textSizeSp: Float,
        onQuery: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val colors = MaterialTheme.colorScheme
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = remember { FocusRequester() }

        // النافذة تصير قابلة للتركيز بعد التركيب بلحظة، والطلب الفوري يُهمَل بلا صوت.
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }

        Surface(
            color = colors.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    start = MaxSpace.sm,
                    end = MaxSpace.xs,
                    top = MaxSpace.xs,
                    bottom = MaxSpace.xs
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(MaxSize.iconGlyph)
                )
                Spacer(Modifier.width(MaxSpace.xs))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.processmgr_search_hint),
                            fontSize = textSizeSp.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(color = colors.onSurface, fontSize = textSizeSp.sp),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                IconButton(
                    onClick = {
                        keyboard?.hide()
                        onDismiss()
                    },
                    modifier = Modifier.size(MaxSize.iconGlyph)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.processmgr_search_close_cd),
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
