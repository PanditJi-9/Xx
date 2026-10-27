/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.viewmodel

import nd.max.MaxManagerProps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import nd.max.core.hardware.RootFileAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nd.max.core.platform.PropertyUtils
import nd.max.core.hardware.ZramHardwareBackend
import nd.max.core.privilege.PrivilegeManager

data class ZramSizePreset(
    val id: String,
    val labelRes: String,
    val mb: Int
)

/** Which zram write an operation notice refers to. */
enum class ZramOpKind { Size, Swappiness, Algorithm, Compact, Reset }

/**
 * سبب الفشل — لأنّ «رفضت النواة» ليست سببًا.
 *
 * الجهاز الفعليّ (POCO · Android): المبادلة مشغولة بـ~4GB، وكانت السلسلة تنادي
 * `swapoff /sys/block/zram0` (مجلّد sysfs لا عقدة كتلة) فيفشل الفصل، ويبقى الجهاز مشغولًا
 * فترفض النواة `reset` و`disksize` — ويظهر للمستخدم «النواة رفضت» بلا سبب. والتمييز هنا
 * يجعل الشاشة تقول ما حدث فعلًا: جذرٌ غير ممنوح؟ عقدة كتلة غائبة؟ مبادلة لا تُفصل الآن؟
 */
enum class ZramFailureReason { KernelRefused, NoRoot, SwapBusy, DeviceNode, Unverified }

/**
 * State of the last zram write. Nothing is reported as applied until the value has been read back
 * from the kernel, so the UI can tell "asked for" apart from "accepted by the kernel".
 */
sealed class ZramOperation {
    abstract val kind: ZramOpKind

    data class Applying(override val kind: ZramOpKind) : ZramOperation()

    data class Applied(override val kind: ZramOpKind, val value: String? = null) : ZramOperation()

    data class Failed(
        override val kind: ZramOpKind,
        val requested: String? = null,
        val actual: String? = null,
        val reason: ZramFailureReason = ZramFailureReason.KernelRefused,
        /** نصّ النواة نفسه والخطوة التي رفضتها — تقنيّ، لا يُختصر إلى «فشل». */
        val detail: String? = null,
    ) : ZramOperation()
}

class ZramViewModel : ViewModel() {

    companion object {
        private const val ZRAM_DEV = "/sys/block/zram0"
        private const val PROP_SIZE_MB = MaxManagerProps.Storage.ZRAM_SIZE_MB
        private const val PROP_SWAPPINESS = MaxManagerProps.Storage.SWAPPINESS
        private const val PROP_COMP_ALGO = MaxManagerProps.Storage.ZRAM_COMP_ALGORITHM
        const val MAX_ZRAM_MB = 25600 // 25.0 GB ceiling

        val SIZE_PRESETS = listOf(
            ZramSizePreset("disabled", "zram_preset_disabled", 0),
            ZramSizePreset("light", "zram_preset_light", 4096),
            ZramSizePreset("stock", "zram_preset_stock", 8192),
            ZramSizePreset("power", "zram_preset_power", 12288)
        )
    }

    var isAvailable by mutableStateOf<Boolean?>(null)
        private set
    var compAlgorithm by mutableStateOf("lz4")
        @JvmName("setCompAlgorithmState") private set
    var currentDiskSizeMb by mutableStateOf(0)
        private set
    var usedSwapMb by mutableStateOf(0)
        private set
    var totalSwapMb by mutableStateOf(0)
        private set
    var origDataMb by mutableStateOf(0)
        private set
    var compDataMb by mutableStateOf(0)
        private set
    var swappiness by mutableStateOf(60)
        private set
    var selectedPresetId by mutableStateOf("stock")
        private set

    var availableCompAlgorithms by mutableStateOf<List<String>>(emptyList())
        private set

    // Hardware Architecture panel — static capability facts detected once,
    // reusing the same /proc/swaps parse refreshStats() already does instead
    // of a second parallel detector.
    var blockDeviceNodeExists by mutableStateOf(false)
        private set
    var swapPriority by mutableStateOf<Int?>(null)
        private set
    var kernelCompactionSupported by mutableStateOf(false)
        private set
    var multiStreamCount by mutableStateOf<Int?>(null)
        private set

    val efficiencyRatio: Float
        get() = if (compDataMb > 0) origDataMb.toFloat() / compDataMb.toFloat() else 1f

    val ramSavedMb: Int
        get() = (origDataMb - compDataMb).coerceAtLeast(0)

    /** False when /proc/sys/vm/swappiness could not be read, so the UI can mark it unreadable. */
    var swappinessKnown by mutableStateOf(true)
        private set

    /**
     * هل الجذر ممنوح؟ `null` = لم يُسأل بعد.
     *
     * ولا يكفي وجود صدفة: libsu تعطي صدفةً بصلاحية التطبيق إن رُفض الجذر، فتُنفَّذ الأوامر
     * وتفشل الكتابة صامتةً — وهو أوّل ما يجب أن تقوله الشاشة بدل «رفضت النواة».
     */
    var rootGranted by mutableStateOf<Boolean?>(null)
        private set

    /** In-flight or finished state of the last write; the UI never claims unverified success. */
    var operation by mutableStateOf<ZramOperation?>(null)
        private set

    private var pollJob: kotlinx.coroutines.Job? = null

    fun loadState() {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = Shell.cmd("test -e $ZRAM_DEV/disksize && echo 1 || echo 0")
                .exec().out.joinToString("").trim() == "1"

            if (!exists) {
                isAvailable = false
                rootGranted = PrivilegeManager.cachedRootGranted()
                return@launch
            }
            isAvailable = true
            // بعد أوّل أمر صدفةً صار الحكم معلومًا: صدفة بصلاحية تطبيق = لا كتابة على العقد.
            rootGranted = PrivilegeManager.cachedRootGranted() || ZramHardwareBackend.isRoot()

            // دورة كل ٣ ثوانٍ: كل `cat` عبر صدفة كان رحلة كاملة — والقراءة الآن عبر الطبقة
            // الموحّدة (أصلي ← IPC ← ملف ← صدفة).
            val compAlgoRaw = RootFileAccess.read("$ZRAM_DEV/comp_algorithm").orEmpty()
            compAlgorithm = Regex("\\[(.*?)]").find(compAlgoRaw)?.groupValues?.get(1)
                ?.ifEmpty { null } ?: compAlgoRaw.trim().substringBefore(' ').ifEmpty { "lz4" }
            availableCompAlgorithms = compAlgoRaw.replace("[", "").replace("]", "")
                .trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            val liveSwappiness = readSwappiness()
            swappinessKnown = liveSwappiness != null
            if (liveSwappiness != null) swappiness = liveSwappiness

            val savedPreset = PropertyUtils.get(MaxManagerProps.Storage.ZRAM_PRESET)
            if (savedPreset.isNotEmpty()) selectedPresetId = savedPreset

            blockDeviceNodeExists = Shell.cmd("test -e /dev/block/zram0 && echo 1 || echo 0")
                .exec().out.joinToString("").trim() == "1"
            kernelCompactionSupported = Shell.cmd("test -e /proc/sys/vm/compact_memory && echo 1 || echo 0")
                .exec().out.joinToString("").trim() == "1"
            multiStreamCount = RootFileAccess.read("$ZRAM_DEV/max_comp_streams")?.toIntOrNull()

            refreshStats()
            startPolling()
        }
    }

    private fun refreshStats() {
        val disksizeBytes = RootFileAccess.read("$ZRAM_DEV/disksize")?.toLongOrNull() ?: 0L
        currentDiskSizeMb = (disksizeBytes / (1024 * 1024)).toInt()

        // mm_stat: orig_data_size compr_data_size mem_used_total ...
        val mmStat = RootFileAccess.read("$ZRAM_DEV/mm_stat").orEmpty().split(Regex("\\s+"))
        origDataMb = (mmStat.getOrNull(0)?.toLongOrNull() ?: 0L).let { it / (1024 * 1024) }.toInt()
        compDataMb = (mmStat.getOrNull(1)?.toLongOrNull() ?: 0L).let { it / (1024 * 1024) }.toInt()

        // /proc/swaps: Filename Type Size Used Priority (Size/Used are in KB)
        val swapsOut = RootFileAccess.read("/proc/swaps")?.lines().orEmpty()
        val zramSwapLine = swapsOut.drop(1).firstOrNull { it.contains("zram") }
        val fields = zramSwapLine?.trim()?.split(Regex("\\s+"))
        totalSwapMb = ((fields?.getOrNull(2)?.toLongOrNull() ?: 0L) / 1024).toInt()
        usedSwapMb = ((fields?.getOrNull(3)?.toLongOrNull() ?: 0L) / 1024).toInt()
        swapPriority = fields?.getOrNull(4)?.toIntOrNull()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshStats()
                delay(3000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    /** Lets the screen stop the 3s sysfs poll while it is not resumed. */
    fun pausePolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Resumes the poll when the screen returns to the foreground. */
    fun resumePolling() {
        if (isAvailable == true && pollJob == null) startPolling()
    }

    /** Clears the last operation notice so a stale outcome never sticks to a new action. */
    fun clearOperation() {
        operation = null
    }

    private fun readSwappiness(): Int? =
        RootFileAccess.read("/proc/sys/vm/swappiness")?.toIntOrNull()

    fun applyPreset(preset: ZramSizePreset) {
        applySizeMb(preset.mb, ZramOpKind.Size, preset.id)
    }

    fun applyCustomSizeMb(mb: Int) {
        applySizeMb(mb.coerceIn(0, MAX_ZRAM_MB), ZramOpKind.Size, "custom")
    }

    /** Restores the stock 8GB preset through the one size-setting path instead of a second copy. */
    fun resetToDefault() {
        val stock = SIZE_PRESETS.first { it.id == "stock" }
        applySizeMb(stock.mb, ZramOpKind.Reset, stock.id)
    }

    /**
     * Resizing a live zram device requires it to be swapped off and reset first (the kernel refuses
     * to change disksize on an active device), then swapped back on at the new size.
     *
     * Nothing is marked applied until disksize is read back from sysfs: a vendor kernel can refuse
     * the write silently, and the UI has to show what the device actually reports.
     */
    private fun applySizeMb(mb: Int, kind: ZramOpKind, presetId: String) {
        operation = ZramOperation.Applying(kind)
        viewModelScope.launch(Dispatchers.IO) {
            // السلسلة كلها في [ZramHardwareBackend.apply] — بعقدة الكتلة لا بمسار sysfs،
            // وبتحقّق بعد كل خطوة، وبإعادة المبادلة إن فشل ما بعد فصلها. وكانت هنا نسخة
            // ثانية من الأوامر (نفس عطب المسار) فتتباعد عن الأولى بلا قياس.
            val result = ZramHardwareBackend.apply(sizeBytes = mb.toLong() * 1024 * 1024)
            refreshStats()
            if (result.notRoot) rootGranted = false
            when {
                result.notRoot -> operation = ZramOperation.Failed(
                    kind, mb.toString(), currentDiskSizeMb.toString(), ZramFailureReason.NoRoot,
                )

                result.failedStage != null -> operation = ZramOperation.Failed(
                    kind = kind,
                    requested = mb.toString(),
                    actual = currentDiskSizeMb.toString(),
                    reason = when (result.failedStage) {
                        ZramHardwareBackend.Stage.Detach ->
                            if (result.node == null) ZramFailureReason.DeviceNode else ZramFailureReason.SwapBusy
                        else -> ZramFailureReason.KernelRefused
                    },
                    detail = "${result.failedStage.name.lowercase()}: ${result.message.orEmpty()}",
                )

                else -> {
                    selectedPresetId = presetId
                    PropertyUtils.set(PROP_SIZE_MB, mb.toString())
                    PropertyUtils.set(MaxManagerProps.Storage.ZRAM_PRESET, presetId)
                    operation = ZramOperation.Applied(kind, mb.toString())
                }
            }
        }
    }

    /**
     * فعلٌ صريح من المستخدم: يطلب الجذر (قد تُظهر نافذة الصلاحية) ثم يعيد القراءة.
     * ولا يُطلب الجذر تلقائيًّا عند فتح الشاشة — القراءة لا تحتاجه، والطلب فعل المستخدم.
     */
    fun requestRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            val granted = PrivilegeManager.requestRoot()
            rootGranted = granted
            if (granted) loadState()
        }
    }

    fun applySwappiness(value: Int) {
        val clamped = value.coerceIn(0, 200)
        operation = ZramOperation.Applying(ZramOpKind.Swappiness)
        viewModelScope.launch(Dispatchers.IO) {
            val step = ZramHardwareBackend.writeSwappiness(clamped)
            val live = readSwappiness()
            swappinessKnown = live != null
            if (live != null) swappiness = live
            if (live == clamped) {
                PropertyUtils.set(PROP_SWAPPINESS, clamped.toString())
                operation = ZramOperation.Applied(ZramOpKind.Swappiness, clamped.toString())
            } else {
                operation = ZramOperation.Failed(
                    kind = ZramOpKind.Swappiness,
                    requested = clamped.toString(),
                    actual = live?.toString(),
                    reason = when {
                        // «لا جذر» يُسأل عنه صراحةً: رمز الخروج وحده لا يميّز رفض الكتابة
                        // بسبب الصلاحية عن رفضها بسبب القيمة.
                        !ZramHardwareBackend.isRoot() -> {
                            rootGranted = false
                            ZramFailureReason.NoRoot
                        }

                        else -> ZramFailureReason.KernelRefused
                    },
                    detail = "swappiness: ${step.message.ifBlank { "exit=${step.code}" }}",
                )
            }
        }
    }

    /**
     * Changing the compression codec hits the same kernel constraint as resizing (the device must be
     * swapped off + reset first), so this goes through the same single sequence in
     * [ZramHardwareBackend.apply] rather than a second, easy-to-drift copy. The codec is only
     * reported as applied when the driver echoes it back.
     */
    fun setCompAlgorithm(algo: String) {
        if (algo == compAlgorithm) return
        val previous = compAlgorithm
        operation = ZramOperation.Applying(ZramOpKind.Algorithm)
        viewModelScope.launch(Dispatchers.IO) {
            val result = ZramHardwareBackend.apply(algorithm = algo)
            val live = ZramHardwareBackend.readState()
            if (result.notRoot) rootGranted = false
            when {
                result.notRoot -> {
                    compAlgorithm = live.algorithm ?: previous
                    operation = ZramOperation.Failed(
                        ZramOpKind.Algorithm, algo, live.algorithm, ZramFailureReason.NoRoot,
                    )
                }

                result.failedStage != null -> {
                    compAlgorithm = live.algorithm ?: previous
                    operation = ZramOperation.Failed(
                        kind = ZramOpKind.Algorithm,
                        requested = algo,
                        actual = live.algorithm,
                        reason = when (result.failedStage) {
                            ZramHardwareBackend.Stage.Detach ->
                                if (result.node == null) ZramFailureReason.DeviceNode else ZramFailureReason.SwapBusy
                            else -> ZramFailureReason.KernelRefused
                        },
                        detail = "${result.failedStage.name.lowercase()}: ${result.message.orEmpty()}",
                    )
                }

                live.algorithm == algo -> {
                    compAlgorithm = algo
                    PropertyUtils.set(PROP_COMP_ALGO, algo)
                    operation = ZramOperation.Applied(ZramOpKind.Algorithm, algo)
                }

                else -> {
                    compAlgorithm = live.algorithm ?: previous
                    operation = ZramOperation.Failed(
                        ZramOpKind.Algorithm, algo, live.algorithm, ZramFailureReason.Unverified,
                        detail = "readback: ${live.algorithm}",
                    )
                }
            }
            refreshStats()
        }
    }

    /**
     * Writes the kernel's real memory-compaction trigger. The node is write-only, so the outcome
     * comes from the write command's exit status; there is nothing to read back.
     *
     * ولماذا `writeFireAndForget` لا `write`: العقدة **لا تُقرأ أبدًا**، فكل كتابة متحقَّقة
     * عليها تُسجَّل `unreadable` — دليل ناقص يُقرأ عطبًا. الطبقة تُسجّل `write_only_sent` وتُبقي
     * السبب ظاهرًا (`NT-13`: الصدفة الخام كانت تُخفي `2>/dev/null` كل فشل).
     */
    fun compactZram() {
        operation = ZramOperation.Applying(ZramOpKind.Compact)
        viewModelScope.launch(Dispatchers.IO) {
            val ok = RootFileAccess.writeFireAndForget("/proc/sys/vm/compact_memory", "1")
            refreshStats()
            operation = if (ok) {
                ZramOperation.Applied(ZramOpKind.Compact, null)
            } else {
                ZramOperation.Failed(ZramOpKind.Compact, null, null)
            }
        }
    }
}
