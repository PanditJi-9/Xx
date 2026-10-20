/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.persist

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.topjohnwu.superuser.Shell
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nd.max.AppMonitorLogger
import nd.max.MaxManagerProps
import nd.max.core.audio.AudioSoundPreset
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.DevfreqCeilingContracts
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.GpuTweakPersistence
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.hardware.MemoryUfsTuningStore
import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.SystemDevfreqCeilingAccess
import nd.max.core.hardware.TouchControlBackend
import nd.max.core.hardware.ZramHardwareBackend
import nd.max.core.platform.PropertyUtils
import nd.max.service.AudioEffectService
import nd.max.service.FpsOverlayService
import nd.max.service.GamePanelService
import nd.max.service.IslandService
import nd.max.service.ProcessOverlayService
import nd.max.ui.util.AudioEffectPrefs
import nd.max.ui.util.FpsOverlayPrefs
import nd.max.ui.util.GamePanelPrefs
import nd.max.ui.util.IslandPrefs
import nd.max.ui.util.ProcessOverlayPrefs

/**
 * مُشغّل الاستعادة — **جولة واحدة، وفيها يُقاس كل بند على حدة**.
 *
 * ### ما تغيّر: من ثلاثة بنود إلى تغطية كل سطح يملكه التطبيق
 *
 * كان المُشغّل يعيد ثلاثة أشياء (لوحة FPS · الجزيرة · تحقّق GPU Studio)، فكان القسم يَعِد بما
 * لا يفعله: الإعدادات التي يحفظها التطبيق **في مصدرها** تبقى بلا استعادة، لأن حفظها لا يعني
 * تطبيقها على الجهاز من جديد. والبنود الآن ثلاث عائلات، ولا يُخلط بينها:
 *
 * 1. **خدمات التطبيق وتراكباته** — تُقلع فعلًا هنا: لوحة FPS، الجزيرة، مراقب المهامّ، لوحة
 *    الألعاب، ومحرّك الصوت. ولا يُقلع ما لم يُطلب: تفضيله المطفأ أو مجموعته الفارغة = `Skipped`
 *    بسبب مكتوب، لا خدمة أمامية بلا سبب.
 * 2. **مقابض يكتبها التطبيق** — تُعاد عبر مسارها الإنتاجيّ نفسه: بروتوكولات الشبكة، ومعاملات
 *    المجدول، واللمس، ودقّة/مقاس الشاشة، وحجم zram، ومدى الذاكرة/UFS. والكتابة تُقاس بقراءة
 *    بعدها، وما لم يُقرأ يُعلَن `Failed` بسببه — لا نجاحًا بلا دليل.
 * 3. **ما يعيده المحرّك بالجذر** — GPU Studio وخصائص الأداء العامة: يُقرأ الحيّ ويُقارَن
 *    بالمحفوظ، فإن طابقه فبرهانُ قراءة (`Applied`)، وإلا `Pending` بمعنى «المحرّك سيعيده» —
 *    ولا يُكتب من هنا بلا سبب، لأن للمقبض الواحد كاتبًا واحدًا.
 *
 * ### والخطر له حارس
 *
 * بنودٌ تُعلَن `dangerous`: zram، والذاكرة/UFS، ومقاس/دقّة الشاشة، واللمس. تُستعاد تلقائيًّا
 * ما دام [PersistStore.dangerousAutoEnabled] مفتوحًا، ويُغلقه [BootLoopGuard] بعد إقلاعين
 * سريعين متتاليين. زرّ «استعادة الآن» يبقى يتجاوز الحارس: **قرارٌ صريح من صاحب الجهاز ليس
 * استعادةً تلقائية**. وهذا نصّ الطلب: «إن سبّب إقلاعًا متكرّرًا تُعطَّل التلقائية».
 *
 * ### والبوابتان الزمنيتان
 *
 * [restoreAll] تحرسها المفتاح المركزي وبوابة تكرار (فـ`BOOT_COMPLETED` و`USER_UNLOCKED` يصلان
 * في الإقلاع نفسه)، وبطءٌ متعمّد بين بند وبند حتى لا تتصادم محاولات الإقلاع. و[restoreNow]
 * يتجاوز بوابة التكرار وحدها — لأن المستخدم طلبها الآن — ولا يتجاوز المفتاح.
 */
object PersistRestorer {

    /** مصدر الجولة: يُكتب في السجلّ ليفرّق القارئ بين استعادة الإقلاع وطلب المستخدم. */
    const val SOURCE_AUTO = "auto"
    const val SOURCE_MANUAL = "manual"

    /**
     * مهلة «جولة واحدة في الإقلاع»: `BOOT_COMPLETED` و`USER_UNLOCKED` قد يفصل بينهما ثوانٍ،
     * والجولة الثانية على بقايا الأولى ليست استعادة بل ضجيج — فدقيقتان كافية لفصلهما، ولا
     * تمنع جولة عمدية تالية (زرّ «استعادة الآن» يتجاوزها بقصد).
     */
    private const val REPEAT_GUARD_MS = 120_000L

    /** فاصلٌ بين بند وبند: إقلاع خمس خدمات في اللحظة نفسها يُنتج ضغطًا لا استعادة. */
    private const val STAGE_GAP_MS = 350L

    /** بند واحد: معرّف ثابت + دالّة تُعيد حكمه ورِمزه + هل هو من عائلة الخطر. */
    private data class Item(
        val id: String,
        val dangerous: Boolean = false,
        val restore: suspend (Context) -> Pair<PersistStatus, String>,
    )

    /**
     * ترتيب التنفيذ مقصود: الأسطح الخفيفة أولًا، ثم مقابض النظام، ثم الخطر في الآخر — فلو
     * أوقف النظامُ الجولة عند بند خطر لا تكون الخدمات قد فُقدت.
     */
    private val ITEMS = listOf(
        Item("fps_panel") { app ->
            val prefs = FpsOverlayPrefs.load(app)
            if (!prefs.enabled) {
                PersistStatus.Skipped to "disabled-by-user"
            } else {
                awaitService(app, needsOverlay = true, start = FpsOverlayService::start) {
                    FpsOverlayService.isRunning
                }
            }
        },
        Item("island") { app ->
            val prefs = IslandPrefs.load(app)
            if (!prefs.enabled) {
                PersistStatus.Skipped to "disabled-by-user"
            } else {
                awaitService(app, needsOverlay = true, start = IslandService::start) {
                    IslandService.isRunning
                }
            }
        },
        Item("process_overlay") { app ->
            val prefs = ProcessOverlayPrefs.load(app)
            if (!prefs.enabled) {
                PersistStatus.Skipped to "disabled-by-user"
            } else {
                awaitService(app, needsOverlay = true, start = ::startProcessOverlay) {
                    ProcessOverlayService.isRunning
                }
            }
        },
        Item("game_panel") { app ->
            val prefs = GamePanelPrefs.load(app)
            if (prefs.enabledPackages.isEmpty()) {
                PersistStatus.Skipped to "nothing-saved"
            } else {
                awaitService(app, needsOverlay = true, start = GamePanelService::ensureRunning) {
                    GamePanelService.isRunning
                }
            }
        },
        Item("audio_effects") { app ->
            val saved = AudioEffectPrefs.load(app)
            if (!saved.enabled || saved.preset == AudioSoundPreset.OFF) {
                PersistStatus.Skipped to "disabled-by-user"
            } else {
                awaitService(app, needsOverlay = false, start = { AudioEffectService.start(it, saved.preset, saved.intensity) }) {
                    AudioEffectService.isRunning
                }
            }
        },
        Item("gpu_studio") { verifyGpuStudio() },
        Item("cpu_io") { restoreCpuAndIo() },
        Item("network") { restoreNetwork() },
        Item("scheduler") { restoreScheduler() },
        Item("touch", dangerous = true) { restoreTouch() },
        Item("display", dangerous = true) { restoreDisplay() },
        Item("zram", dangerous = true) { restoreZram() },
        Item("memory_ufs", dangerous = true) { app -> restoreMemoryUfs(app) },
    )

    /**
     * دخول الجولة **التلقائية** (بعد الإقلاع): يقيس الإقلاع أولًا، ثم يحترم المفتاح وبوابة التكرار.
     */
    suspend fun restoreAll(context: Context): List<PersistEntry> {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        // **الحارس قبل المفتاح:** الإقلاع يُحسب ولو كانت الميزة مطفأة، وإلا لما كان لمن أطفأها
        // ثم أعاد فتحها بعد حلقة أيُّ تاريخ يقيس عليه.
        PersistStore.observeBoot(app, now)
        if (!PersistStore.enabled(app)) return emptyList()
        if (now - PersistStore.lastRunAtMs(app) < REPEAT_GUARD_MS) return emptyList()
        // **الحجز قبل العمل لا بعده:** بثّان متلاصقان كانا يمرّان كلاهما من البوابة لو كُتب
        // الزمن في النهاية — الجولة تُحجز أولًا.
        PersistStore.markRun(app, now)
        return withContext(Dispatchers.IO) { run(app, automatic = true) }
    }

    /** الجولة اليدوية من زرّ «استعادة الآن»: تتجاوز بوابة التكرار والحارس، وتحترم المفتاح. */
    suspend fun restoreNow(context: Context): List<PersistEntry> {
        val app = context.applicationContext
        if (!PersistStore.enabled(app)) return emptyList()
        // قراءة عقد وكتابتها وصفتها: على خيط دايمون لا على الخيط الذي يرسم الزرّ.
        return withContext(Dispatchers.IO) { run(app, automatic = false) }
    }

    private suspend fun run(app: Context, automatic: Boolean): List<PersistEntry> {
        val holdDangerous = automatic && !PersistStore.dangerousAutoEnabled(app)
        val entries = mutableListOf<PersistEntry>()
        for (item in ITEMS) {
            val (status, detail) = if (item.dangerous && holdDangerous) {
                PersistStatus.Skipped to "bootloop-guard"
            } else {
                runCatching { item.restore(app) }
                    .getOrElse { error ->
                        PersistStatus.Failed to "exception:${error.javaClass.simpleName}"
                    }
            }
            entries += PersistEntry(item.id, status, detail, System.currentTimeMillis())
            delay(STAGE_GAP_MS)
        }
        log(entries, automatic, holdDangerous)
        return entries
    }

    /**
     * سطر واحد في `MaxManager.log` — **لا سجلًّا في الواجهة**. ويُسجَّل كل فشل برمزه وحده،
     * لأن الفشل الذي لا يظهر في أيّ مكان هو فشل مُنكَر.
     */
    private fun log(entries: List<PersistEntry>, automatic: Boolean, holdDangerous: Boolean) {
        val count = { status: PersistStatus -> entries.count { it.status == status } }
        AppMonitorLogger.i(
            "EVENT=PERSIST_RESTORE source=${if (automatic) SOURCE_AUTO else SOURCE_MANUAL} " +
                "items=${entries.size} applied=${count(PersistStatus.Applied)} " +
                "pending=${count(PersistStatus.Pending)} skipped=${count(PersistStatus.Skipped)} " +
                "failed=${count(PersistStatus.Failed)} dangerous=${if (holdDangerous) "held" else "on"}"
        )
        entries.filter { it.status == PersistStatus.Failed }
            .forEach { AppMonitorLogger.w("EVENT=PERSIST_RESTORE_ITEM id=${it.id} detail=${it.detail}") }
    }

    // ── الخدمات ───────────────────────────────────────────────────────────────────────────────

    private fun startProcessOverlay(context: Context) {
        val intent = Intent(context, ProcessOverlayService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /**
     * إقلاع خدمة مع تحقّق محدود: تُفحص الحاجة (الصلاحية)، ثم تُقلع، ثم تُسأل الخدمة نفسها
     * مرّتين بحدّ أعلى — وبعدها حكم صريح. ولا حلقة لا نهائية: محاولتان ثم تسجيل.
     */
    private suspend fun awaitService(
        app: Context,
        needsOverlay: Boolean,
        start: (Context) -> Unit,
        isRunning: () -> Boolean,
    ): Pair<PersistStatus, String> {
        if (needsOverlay && !Settings.canDrawOverlays(app)) {
            return PersistStatus.Skipped to "no-overlay-permission"
        }
        if (isRunning()) return PersistStatus.Applied to "already-running"
        start(app)
        // الأولى بعد مهلة إقلاع الخدمة الفعلية، والثانية مضاعفة لتغطية إقلاع بطيء — ثم الحكم.
        for (wait in longArrayOf(900L, 2_100L)) {
            delay(wait)
            if (isRunning()) return PersistStatus.Applied to "service-started"
        }
        return PersistStatus.Failed to "service-did-not-start"
    }

    // ── مقابض يكتبها التطبيق ──────────────────────────────────────────────────────────────────

    /**
     * معالج CPU ومجدول الإدخال/الإخراج — **مقبضان محفوظان في خصائص المحرّك، ويعيدهما**
     * [`binprofiles::init_cpu_governor`] **عند الإقلاع.** ويُقاسان هنا من التطبيق كما بقيّة
     * البنود: طابق الحيّ المحفوظَ فبرهان قراءة، وإلا كُتب المحفوظ ثم قُرئ — فانحرافٌ لم
     * يلحقه المحرّك يُصلَح في الجولة نفسها لا يُترك حتى فتح شاشة.
     *
     * ولا يُكتب حاكم لا تعرضه النواة الآن: سلّم معلن لا يحتويه المحفوظ يعود `Skipped`، وهو
     * فرق «قدرة تغيّرت» عن «كتابة رُفضت».
     */
    private fun restoreCpuAndIo(): Pair<PersistStatus, String> {
        val tally = Tally()
        val governor = PropertyUtils.get(MaxManagerProps.Governor.CPU_CUSTOM_DEFAULT).trim()
            .ifEmpty { PropertyUtils.get(MaxManagerProps.Governor.CPU_DEFAULT).trim() }
        if (governor.isNotEmpty()) {
            RootFileAccess.globDirectories("/sys/devices/system/cpu/cpufreq/*").forEach { policy ->
                val available = RootFileAccess.read("$policy/scaling_available_governors")
                    .orEmpty().split(Regex("\\s+")).filter(String::isNotBlank)
                if (available.isEmpty() || governor in available) {
                    tally.write("$policy/scaling_governor", governor)
                }
            }
        }
        val scheduler = PropertyUtils.get(MaxManagerProps.Governor.IO_CUSTOM_DEFAULT).trim()
            .ifEmpty { PropertyUtils.get(MaxManagerProps.Governor.IO_DEFAULT).trim() }
        if (scheduler.isNotEmpty()) {
            RootFileAccess.globDirectories("/sys/block/*")
                .firstOrNull { RootFileAccess.exists("$it/queue/scheduler") }
                ?.let { block ->
                    val options = RootFileAccess.read("$block/queue/scheduler").orEmpty()
                        .split(Regex("[\\[\\]\\s]+")).filter(String::isNotBlank)
                    if (options.isEmpty() || scheduler in options) {
                        tally.write("$block/queue/scheduler", scheduler)
                    }
                }
        }
        return tally.verdict()
    }

    /**
     * مقابض الشبكة — تُكتب من محفوظها كما تفعل شاشتها عند الفتح (`applySavedBool`)، لكن هنا
     * **عند الإقلاع** لا عند فتح شاشة: من أعاد تشغيل جهازه ولم يفتح الشاشة كان يفقدها.
     */
    private fun restoreNetwork(): Pair<PersistStatus, String> {
        val tally = Tally()
        listOf(
            MaxManagerProps.Network.TCP_SYNCOOKIES to "/proc/sys/net/ipv4/tcp_syncookies",
            MaxManagerProps.Network.TCP_REUSE to "/proc/sys/net/ipv4/tcp_tw_reuse",
            MaxManagerProps.Network.TCP_FASTOPEN to "/proc/sys/net/ipv4/tcp_fastopen",
            MaxManagerProps.Network.TCP_SACK to "/proc/sys/net/ipv4/tcp_sack",
            MaxManagerProps.Network.TCP_ECN to "/proc/sys/net/ipv4/tcp_ecn",
        ).forEach { (prop, path) ->
            val saved = PropertyUtils.get(prop)
            if (saved == "0" || saved == "1") tally.write(path, saved)
        }
        // خوارزمية الازدحام نصّ لا علَم: تُكتب كما حُفظت، ولا تُكتب إن لم تكن النواة تعرضها —
        // فخوارزمية اختفت بعد تحديث نواة تعود `Skipped` لا فشلًا يُسجَّل كل إقلاع.
        val congestion = PropertyUtils.get(MaxManagerProps.Network.TCP_CONGESTION).trim()
        if (congestion.isNotEmpty()) {
            val available = RootFileAccess.read("/proc/sys/net/ipv4/tcp_available_congestion_control")
                .orEmpty().split(Regex("\\s+"))
            if (available.isEmpty() || congestion in available) {
                tally.write("/proc/sys/net/ipv4/tcp_congestion_control", congestion)
            }
        }
        return tally.verdict()
    }

    /**
     * معاملات المجدول: الأعلام الثنائية، ووضع التقريب، والمعاملات العامة المحفوظة بمسارها.
     * ولا يُكتب معامل لم تُقرأ عقدته أصلًا — الغياب يعني «نواة لا تنشره»، لا «قيمة صفر».
     */
    private fun restoreScheduler(): Pair<PersistStatus, String> {
        val tally = Tally()
        listOf(
            MaxManagerProps.Scheduler.BORE to "/proc/sys/kernel/sched_bore",
            MaxManagerProps.Scheduler.AUTOGROUP to "/proc/sys/kernel/sched_autogroup_enabled",
            MaxManagerProps.Scheduler.CHILD_RUNS_FIRST to "/proc/sys/kernel/sched_child_runs_first",
            MaxManagerProps.Scheduler.SCHEDSTATS to "/proc/sys/kernel/sched_schedstats",
            MaxManagerProps.Scheduler.CSTATE_AWARE to "/proc/sys/kernel/sched_cstate_aware",
        ).forEach { (prop, path) ->
            val saved = PropertyUtils.get(prop)
            if (saved == "0" || saved == "1") tally.write(path, saved)
        }
        listOf(
            MaxManagerProps.Scheduler.TUNABLE_SCALING to "/proc/sys/kernel/sched_tunable_scaling",
            MaxManagerProps.Scheduler.UCLAMP_MAX to "/proc/sys/kernel/sched_util_clamp_max",
            MaxManagerProps.Scheduler.UCLAMP_MIN to "/proc/sys/kernel/sched_util_clamp_min",
            MaxManagerProps.Scheduler.PRINTK to "/proc/sys/kernel/printk",
        ).forEach { (prop, path) ->
            val saved = PropertyUtils.get(prop).trim()
            if (saved.isNotEmpty()) tally.write(path, saved)
        }
        // المعاملات العامة: `path=value;path=value` كما حفظتها شاشتها.
        PropertyUtils.get(MaxManagerProps.Scheduler.GENERIC_OVERRIDES)
            .split(';')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { override ->
                val path = override.substringBefore('=').trim()
                val value = override.substringAfter('=', "").trim()
                if (path.startsWith("/") && value.isNotEmpty()) tally.write(path, value)
            }
        return tally.verdict()
    }

    /**
     * عدّاد حكم واحد لكل عائلة: ما قُرئ، وما ثبت، وما رُفض.
     *
     * وعقدة غائبة **لا تُحسب** حاجةً ولا فشلًا: نواة لا تنشر العقدة ليست نواة رفضت الكتابة،
     * وخلط الاثنين يجعل جهازًا سليمًا يبدو معطوبًا.
     */
    private class Tally {
        private var seen = 0
        private var applied = 0
        private var failed = 0

        fun write(path: String, value: String) {
            when (writeAndRead(path, value)) {
                WriteOutcome.OK -> {
                    seen++
                    applied++
                }
                WriteOutcome.MISMATCH -> {
                    seen++
                    failed++
                }
                WriteOutcome.NO_NODE -> Unit
            }
        }

        fun verdict(): Pair<PersistStatus, String> = when {
            seen == 0 -> PersistStatus.Skipped to "nothing-saved"
            failed > 0 -> PersistStatus.Failed to "node-write-refused"
            applied > 0 -> PersistStatus.Applied to "read-back-ok"
            else -> PersistStatus.Skipped to "no-node"
        }
    }

    /**
     * اللمس: يُعاد عبر المالك الواحد ([TouchControlBackend]) لا بكتابة عقدة من هنا — فمعنى
     * `1`/`0` يختلف باختلاف العقدة، والمالك هو الذي يعرف أيّها قُرئت.
     */
    private fun restoreTouch(): Pair<PersistStatus, String> {
        val boost = PropertyUtils.get(MaxManagerProps.Touch.BOOST) == "1"
        val doubleTap = PropertyUtils.get(MaxManagerProps.Touch.DT2W) == "1"
        if (!boost && !doubleTap) return PersistStatus.Skipped to "disabled-by-user"
        val found = TouchControlBackend.discover()
        val boostAvailable = found.hasBoostNode || found.vendorHasWritePath
        val doubleTapAvailable = found.doubleTap != null
        var attempted = 0
        var ok = true
        if (boost && boostAvailable) {
            attempted++
            val attempt = TouchControlBackend.reconcile(true)
            // «لا مزوّد» ليست «رُفضت»: الأولى قدرة غائبة تُترك، والثانية محاولة فشلت تُعلَن.
            if (attempt.result == TouchControlBackend.Result.REFUSED) return PersistStatus.Failed to "touch-refused"
            if (!attempt.persistable) ok = false
        }
        if (doubleTap && doubleTapAvailable) {
            attempted++
            if (!TouchControlBackend.reconciler.reconcileDoubleTap(true)) ok = false
        }
        return when {
            attempted == 0 -> PersistStatus.Skipped to "no-node"
            ok -> PersistStatus.Applied to "read-back-ok"
            else -> PersistStatus.Failed to "touch-refused"
        }
    }

    /**
     * المقاس والدقّة: يُعادان بأمر `wm` نفسه الذي تستعمله الشاشة، ويُتحقّق بقراءة الإخراج لا
     * بنجاح الأمر. وهذا بند خطر: مقاس خاطئ يُفقد الواجهة، ولذلك يُطفأ تلقائيًّا عند الحلقة.
     */
    private fun restoreDisplay(): Pair<PersistStatus, String> {
        val width = PropertyUtils.get(MaxManagerProps.Display.RES_WIDTH).trim()
        val height = PropertyUtils.get(MaxManagerProps.Display.RES_HEIGHT).trim()
        val dpi = PropertyUtils.get(MaxManagerProps.Display.RES_DPI).trim()
        if (width.isEmpty() || height.isEmpty() || dpi.isEmpty()) {
            return PersistStatus.Skipped to "nothing-saved"
        }
        val size = "${width}x$height"
        var ok = true
        if (!readsOverride("wm size", "Override size: $size")) {
            ok = Shell.cmd("wm size $size").exec().isSuccess && readsOverride("wm size", "Override size: $size") && ok
        }
        if (!readsOverride("wm density", "Override density: $dpi")) {
            ok = Shell.cmd("wm density $dpi").exec().isSuccess && readsOverride("wm density", "Override density: $dpi") && ok
        }
        return if (ok) PersistStatus.Applied to "read-back-ok" else PersistStatus.Failed to "display-write-refused"
    }

    private fun readsOverride(command: String, needle: String): Boolean = runCatching {
        Shell.cmd(command).exec().out.joinToString("\n").contains(needle)
    }.getOrDefault(false)

    /**
     * zram: الحجم والسوافبنس والمُرمِّز — من الخصائص التي كتبتها شاشتها **بعد** قراءة تحقّق.
     * وإعادة الحجم تُعيد بناء المبادلة (`swapoff`/`reset`/`mkswap`/`swapon`) داخل
     * [ZramHardwareBackend] نفسه، فلا مسار ثانٍ يختلف.
     */
    private fun restoreZram(): Pair<PersistStatus, String> {
        val sizeMb = PropertyUtils.get(MaxManagerProps.Storage.ZRAM_SIZE_MB).trim().toIntOrNull()
        val swappiness = PropertyUtils.get(MaxManagerProps.Storage.SWAPPINESS).trim().toIntOrNull()
        val algorithm = PropertyUtils.get(MaxManagerProps.Storage.ZRAM_COMP_ALGORITHM).takeIf(String::isNotBlank)
        if (sizeMb == null && swappiness == null) return PersistStatus.Skipped to "nothing-saved"
        val state = ZramHardwareBackend.readState()
        if (!state.exists) return PersistStatus.Skipped to "no-node"

        var ok = true
        if (sizeMb != null && sizeMb >= 0) {
            val target = sizeMb.toLong() * 1024L * 1024L
            val already = state.sizeBytes == target
            if (!already) {
                ok = ZramHardwareBackend.setSizeBytes(target, algorithm).verified && ok
            }
        }
        if (swappiness != null) {
            val clamped = swappiness.coerceIn(0, 200).toString()
            when (writeAndRead("/proc/sys/vm/swappiness", clamped)) {
                WriteOutcome.MISMATCH -> ok = false
                else -> Unit
            }
        }
        return if (ok) PersistStatus.Applied to "read-back-ok" else PersistStatus.Failed to "zram-write-refused"
    }

    /**
     * الذاكرة/UFS: يُعاد المدى المحفوظ على **العقدة نفسها**، بعد التحقّق من سلّمها الحالي،
     * ومن خلال المُحكِّم بمفتاحه القياسيّ — فلا كاتب ثانٍ لعقدة التردّد.
     *
     * ومسارات اختفت بعد تحديث نواة تُعدّ `Skipped` برمزها (لا تُحذف من الحفظ): قد تعود بعد
     * إقلاع تالٍ، والحذف الصامت يُفقد قرار المستخدم بلا سبب.
     */
    private suspend fun restoreMemoryUfs(app: Context): Pair<PersistStatus, String> {
        val saved = MemoryUfsTuningStore.load(app)
        if (saved.isEmpty()) return PersistStatus.Skipped to "nothing-saved"
        // المثيل **نفسه** الذي يستعمله التطبيق كلّه من بوابة Hilt — لا بناءٌ ثانٍ يفلق جدول
        // الطلبات؛ وهذا ما يمنعه الحرس بنصّه (`ControlPlaneArchitectureTest.oneArbiterInstancePerProcess`).
        val arbiter = EntryPointAccessors
            .fromApplication(app, PersistArbiterEntryPoint::class.java)
            .arbiter()
        var applied = 0
        var failed = 0
        var refused = 0
        var gone = 0
        for (entry in saved) {
            val device = MemoryUfsHardwareBackend.refresh(entry.devicePath)
            if (device == null) {
                gone++
                continue
            }
            val request = entry.request
            if (MemoryUfsHardwareBackend.validate(device, request) != null) {
                refused++
                continue
            }
            if (MemoryUfsHardwareBackend.satisfied(request, device)) {
                applied++
                continue
            }
            val key = HardwareControlKey.devfreqFrequency(device.kind, device.name)
            val result = runCatching {
                arbiter.submit(
                    key = key,
                    owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                    token = "persist-memufs-${entry.devicePath.hashCode()}",
                    desired = MemoryUfsHardwareBackend.encodeRequest(request),
                    apply = { value -> SystemDevfreqCeilingAccess.writeRange(device.path, value) },
                    read = { SystemDevfreqCeilingAccess.readBaseline(device.path) },
                    baseline = MemoryUfsHardwareBackend.encodeBaseline(device),
                    restore = { value -> SystemDevfreqCeilingAccess.restoreBaseline(device.path, value) },
                    verify = DevfreqCeilingContracts::contained,
                    realized = DevfreqCeilingContracts::reached,
                )
            }.getOrNull()
            if (result?.verified == true) applied++ else failed++
            delay(STAGE_GAP_MS)
        }
        return when {
            failed > 0 -> PersistStatus.Failed to "memory-ufs-write-refused"
            applied > 0 -> PersistStatus.Applied to "read-back-ok"
            refused > 0 -> PersistStatus.Skipped to "range-no-longer-offered"
            gone > 0 -> PersistStatus.Skipped to "device-gone"
            else -> PersistStatus.Skipped to "nothing-saved"
        }
    }

    /**
     * بند GPU Studio — **قراءةً من هنا، لا كتابة**.
     *
     * الطلب نفسه محفوظ في خصائص النظام ويُتحقَّق من صلاحيته على السائق الحالي
     * (`loadValidated` يعيد null إن لم يعد السائق يعرضه). فإن لبّت الحالة الحيّة الطلب
     * فبرهان القراءة `Applied`، وإلا فالكتابة من عمل المحرّك بالجذر عند قيامه: `Pending`.
     */
    private fun verifyGpuStudio(): Pair<PersistStatus, String> = runCatching {
        val device = GpuHardwareBackend.selection().device
            ?: return@runCatching PersistStatus.Skipped to "no-gpu-device"
        val request = GpuTweakPersistence.loadValidated(device)
            ?: return@runCatching PersistStatus.Skipped to "nothing-saved"
        val satisfied = GpuHardwareBackend.requestSatisfied(
            desired = GpuHardwareBackend.encodeRequest(request),
            actual = GpuHardwareBackend.encodeLive(device, request),
        )
        if (satisfied) PersistStatus.Applied to "read-back-ok"
        else PersistStatus.Pending to "engine-will-apply"
    }.getOrElse { error -> PersistStatus.Failed to "exception:${error.javaClass.simpleName}" }
}

/**
 * باب الوصول من `object` لا يُحقن إلى **مُحكِّم واحد في العملية**.
 *
 * [PersistRestorer] كائنٌ مفرد خارج Hilt (يناديه مُستقبِل إقلاع وعنصر واجهة)، و`HardwareControlArbiter`
 * `@Singleton` في `SingletonComponent`؛ فبدل بناء مثيل ثانٍ يفلق جدول الطلبات، يُسحب المثيل نفسه
 * من هذا الباب. ولا يُنادى إلا من عملية التطبيق — وHilt فيها (`@HiltAndroidApp`) مهيّأٌ قبل أول مُستقبِل.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PersistArbiterEntryPoint {
    fun arbiter(): HardwareControlArbiter
}

private enum class WriteOutcome { OK, NO_NODE, MISMATCH }

/**
 * يكتب ثم **يقرأ**: النجاح حكمٌ على القيمة لا على الأمر. وعقدة غائبة ليست فشلًا — هي قدرة
 * غير منشورة، ورمزها مختلف عن «كتبتُ فلم يثبت».
 */
private fun writeAndRead(path: String, value: String): WriteOutcome {
    if (!RootFileAccess.exists(path)) return WriteOutcome.NO_NODE
    if (RootFileAccess.read(path)?.trim() == value) return WriteOutcome.OK
    RootFileAccess.write(path, value)
    return if (RootFileAccess.read(path)?.trim() == value) WriteOutcome.OK else WriteOutcome.MISMATCH
}
