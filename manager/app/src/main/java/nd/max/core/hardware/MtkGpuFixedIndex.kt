/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * قفل فهرس OPP على MediaTek: **التثبيت** و**التحرير** — ولا شيء غيرهما.
 *
 * وفُصل هذا المسار في ملفه لأنه أخطر ما في طبقة GPU وأصعب ما يُكتشف خطؤه:
 *
 * - التثبيت يكتب فهرسًا واحدًا في `fix_target_opp_index`، والتردد الذي يجري عليه الجهاز بعدها
 *   يحدّده **جدول النواة** لا فهرستنا. فخطأ في الفهرسة لا يُخطئ الهدف فقط، بل يُعلن نجاحًا لأن
 *   التحقّق من صدى الفهرس لا من التردد (انظر `GpuCeilingPolicy.PinVerdict` الذي يقيس التردد).
 * - والتثبيت يوقف DVFS، فبقاؤه بلا تحرير يجمّد التردد على درجة واحدة: «عالق على ٦٥٠» — وهو أثر
 *   لا يظهر في أي شاشة ولا في أي قراءة تردد.
 *
 * وما لا يفعله هذا الملف: لا يقرّر إن كان الطلب سقفًا أم تثبيتًا (ذلك `GpuCeilingPolicy`)، ولا
 * يقرأ جدول OPP (ذلك `MtkGpuOppTable`)، ولا يختار الجهاز (ذلك `GpuHardwareBackend`).
 */
package nd.max.core.hardware

internal object MtkGpuFixedIndex {

    /**
     * يُعيد التوسّع الديناميكي بعد تحرير قفل OPP — والتثبيت هو ما يوقف DVFS، فزواله يُعيده.
     *
     * و`release = false`: هذه ليست إعادة سقف المصنّع بل إعادة تشغيل الحاكم. ولو مرّرنا `true` لصار
     * كل خروج من تثبيت يرفع حماية حراريّة وَضعها المصنّع بلا سبب يخصّها.
     */
    fun restoreDynamicScaling(io: GpuHardwareBackend.Io): Boolean {
        if (io.permitVendorCeiling(lock = false, release = false)) return true
        // `permitVendorCeiling` هنا يمرّ بـ`PlatformCeilingAuthority.permitGpu`، و`release = false`
        // لا يدخل في `Report.anyChannel` — فالعائد `false` لا يعني أن DVFS ما زال موقوفًا. الحكم
        // الأخير من العقدة نفسها: `1` = يعمل. وإن غابت العقدة فلا شيء نُثبته، ولا نُعلن فشلًا وهميًّا.
        val node = PlatformCeilingAuthority.GED_DVFS_ENABLE
        if (!io.exists(node)) return true
        return io.read(node)?.trim() == "1"
    }

    /** يحرّر قفل الفهرس الثابت (يكتب `-1`)، ويُرجع خط الأساس عند الفشل. */
    fun release(live: GpuHardwareBackend.Device, request: GpuHardwareBackend.Request, io: GpuHardwareBackend.Io): GpuHardwareBackend.TransactionResult {
        val path = live.mtkFixedIndexPath
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "fixed-lock-unavailable")
        val baselineIndex = io.read(path)?.let(MtkGpuOppTable::parseIndex)
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "baseline-unreadable")
        if (baselineIndex == "-1") {
            // القفل محرَّر أصلًا، لكنّ `release` هي فعل «أعِد التوسّع» لا «اكتب -1 فقط»:
            // قد يكون DVFS موقوفًا من جلسة سابقة، والنجاح بلا هذه الخطوة يُبقي التردد مجمّدًا.
            val scaling = restoreDynamicScaling(io)
            return GpuHardwareBackend.TransactionResult(
                request, GpuHardwareBackend.refresh(live.path, io), true, scaling,
                error = if (scaling) null else "gpu-scaling-not-restored",
            )
        }
        val wrote = io.write(path, "-1")
        val verified = wrote && io.read(path)?.let(MtkGpuOppTable::parseIndex) == "-1"
        if (verified) {
            val scaling = restoreDynamicScaling(io)
            return GpuHardwareBackend.TransactionResult(
                request, GpuHardwareBackend.refresh(live.path, io), true, scaling,
                error = if (scaling) null else "gpu-scaling-not-restored",
            )
        }
        val rollbackWrote = io.write(path, baselineIndex)
        val rollbackVerified = rollbackWrote && io.read(path)?.let(MtkGpuOppTable::parseIndex) == baselineIndex
        return GpuHardwareBackend.TransactionResult(
            request, GpuHardwareBackend.refresh(live.path, io), wrote, false, true, rollbackVerified,
            if (rollbackVerified) "apply-not-verified-baseline-restored" else "apply-and-rollback-failed",
        )
    }

    /**
     * يثبّت درجة واحدة بفهرسها — التثبيت يعني «ثبّت هذه القيمة».
     *
     * وفيه يوقف DVFS ويُرفع سقف المصنّع **إن طلبه المنادي** (`releaseVendorCeiling`): تثبيت درجة
     * أعلى من قمع المصنّع لا يُثبّت شيئًا بلا هذا الرفع. ومن كتب سقفًا أدنى من قدرة الجهاز (بروفايل
     * تبريد: `release = false`) لا يُلمس سقف المصنّع ولا يوقف DVFS — يكفيه أن الفهرس يقصّ.
     */
    fun pin(live: GpuHardwareBackend.Device, request: GpuHardwareBackend.Request, io: GpuHardwareBackend.Io): GpuHardwareBackend.TransactionResult {
        val path = live.mtkFixedIndexPath
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "fixed-lock-unavailable")
        val frequency = request.minFreq
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "incomplete-range")
        val targetIndex = live.mtkOppIndexByFrequency[frequency]
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "unsupported-frequency")
        val baselineIndex = io.read(path)?.let(MtkGpuOppTable::parseIndex)
            ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "baseline-unreadable")
        // لقطة السقف قبل رفعه: التثبيت قد يُطفئ DVFS ويرفع قمعًا حراريًّا/منصّيًّا، وتراجعٌ
        // يُعيد الفهرس وحده يترك المنصّة مكشوفة بلا القمع الذي كان قائمًا قبل الطلب.
        val ceilingCapture = if (request.releaseVendorCeiling) io.captureVendorCeiling() else null
        if (request.releaseVendorCeiling) {
            io.permitVendorCeiling(lock = true, release = true)
        }
        val touchesGovernor = request.governor != null
        val baselineGovernor = if (touchesGovernor) {
            live.governor
                ?: return GpuHardwareBackend.TransactionResult(request, live, false, false, error = "baseline-unreadable")
        } else null
        val wroteLock = io.write(path, targetIndex)
        val wroteGovernor = !touchesGovernor || (wroteLock && io.write("${live.path}/governor", request.governor))
        val actual = GpuHardwareBackend.refresh(live.path, io)
        // **صدى الفهرس ليس التردد.** هذا القياس هو ما يفصل «كتبنا الرقم» عن «الجهاز يعمل عليه»:
        // على هذا الجهاز بقي الفهرس على ٨٣٢ بينما الساعة الحقيقية ٢٦/٧٥٤، وكان التطبيق يعلن
        // النجاح لأن قراءة الفهرس تطابق ما كتبناه. وحين لا يُقاس التردد يبقى الحكم `BY_INDEX`
        // كما هو مصرَّح به، ولا يُدَّعى قياس لم يقع.
        val measuredHz = actual?.let { GpuHardwareBackend.currentFrequencyHz(it, io) }
        val clockMismatch = measuredHz != null &&
            GpuCeilingPolicy.pinVerdict(frequency, measuredHz) == GpuCeilingPolicy.PinVerdict.CLOCK_MISMATCH
        val verified = wroteLock && wroteGovernor && !clockMismatch &&
            io.read(path)?.let(MtkGpuOppTable::parseIndex) == MtkGpuOppTable.parseIndex(targetIndex) &&
            (request.governor == null || actual?.governor.equals(request.governor, true))
        if (verified) return GpuHardwareBackend.TransactionResult(request, actual, true, true)
        val rollbackLock = io.write(path, baselineIndex) && io.read(path)?.let(MtkGpuOppTable::parseIndex) == baselineIndex
        val rollbackGovernor = baselineGovernor == null ||
            (io.write("${live.path}/governor", baselineGovernor) &&
                GpuHardwareBackend.refresh(live.path, io)?.governor == baselineGovernor)
        // وإعادة التوسّع الديناميكي **جزء من التراجع** لا زينة بعده.
        //
        // فالتثبيت أوقف DVFS (إن كان قد طلب رفع سقف المصنّع)، وتراجعٌ يُعيد الفهرس ويُبقي DVFS
        // موقوفًا لا يُعيد شيئًا: الجهاز يبقى على الدرجة التي صادف وجودها وقت الطلب — أي يُنتج
        // العطب الذي جاء التراجع لإلغائه. و`applyDevfreq` يفعل هذا في تراجعه، فيتبع مسار القفل
        // القاعدة نفسها بدل أن يكون استثناءً لها.
        //
        // والترتيب مقصود: تمكين DVFS أولًا، ثم لقطة السقف **أخيرًا** لأنها تحمل `dvfsEnable`
        // الأصلي؛ ولو سبقتها كتابةُ تمكين لغلبها. والتراجع لا يُعلَن ناجحًا إلا إذا رجع
        // الفهرس والحاكم والتوسّع والسقف كلهم.
        val scalingRestored = restoreDynamicScaling(io)
        val ceilingRestored = ceilingCapture == null || io.restoreVendorCeiling(ceilingCapture)
        val rollbackVerified = rollbackLock && rollbackGovernor && scalingRestored && ceilingRestored
        return GpuHardwareBackend.TransactionResult(
            request, GpuHardwareBackend.refresh(live.path, io), wroteLock && wroteGovernor, false, true, rollbackVerified,
            when {
                clockMismatch && rollbackVerified -> GpuCeilingPolicy.PinVerdict.CLOCK_MISMATCH.token
                rollbackVerified -> "apply-not-verified-baseline-restored"
                else -> "apply-and-rollback-failed"
            },
        )
    }
}
