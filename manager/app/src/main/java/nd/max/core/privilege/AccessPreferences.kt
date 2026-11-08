/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.privilege

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ما يُعرض في قوائم القدرات: المدعوم في الوضع الحالي وحده، أو كل شيء موسومًا بما يحتاجه.
 *
 * **ولماذا تفضيل محفوظ لا استنتاج لحظيّ:** المستخدم قد يختار «كل شيء» ليرى ما يفوته قبل منح
 * الجذر؛ ولو اشتقّناه من الطبقة الحالية لعادت القائمة تقصر وحدها فور تغيّر الطبقة، فيفقد ما كان
 * ينظر إليه. الاختيار قراره، والطبقة حقيقة الجهاز — ولا يُخلطان.
 */
enum class AccessFilter(val id: String) {
    SUPPORTED_ONLY("supported"),
    EVERYTHING("everything");

    companion object {
        /** المجهول يعود إلى الأضيق لا إلى الأوسع: لا ندّعي دعمًا لم يُطلب. */
        fun fromId(id: String?): AccessFilter =
            entries.firstOrNull { it.id == id } ?: SUPPORTED_ONLY
    }
}

/** تخزين [AccessFilter] — النمط نفسه المستعمل في [ExecutionModeStore]. */
object AccessFilterStore {
    private const val PREFS = "access_filter"
    private const val KEY = "filter"

    private val selection = MutableStateFlow(AccessFilter.SUPPORTED_ONLY)
    val state = selection.asStateFlow()

    fun current(): AccessFilter = selection.value

    fun load(context: Context) {
        selection.value = runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            AccessFilter.fromId(prefs.getString(KEY, null))
        }.getOrDefault(AccessFilter.SUPPORTED_ONLY)
    }

    fun save(context: Context, filter: AccessFilter) {
        selection.value = filter
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, filter.id)
                .apply()
        }
    }
}

/**
 * من يدير الجذر فعلًا — لا «هل يوجد su» فقط.
 *
 * والفرق يهمّ المستخدم: Magisk وKernelSU وAPatch تختلف في مساراتها وفي طريقة منح الصلاحية،
 * وتشخيص «الجذر ممنوح لكن الأمر رُفض» يبدأ من هنا.
 */
enum class RootManager { NONE, MAGISK, KERNEL_SU, APATCH, OTHER, UNKNOWN }

/**
 * كشف مدير الجذر بمسارات إدارته المعلنة، وبأمر واحد فقط.
 *
 * **ولا يُنفَّذ إلا عند جذر ممنوح:** تشغيله بلا جذر يعني استدعاء صلاحية لم يطلبها المستخدم،
 * وهذا فعل صريح لا استطلاع خلفي (ADR-11).
 */
object RootManagerDetector {
    private const val PROBE = "ls -d /data/adb/magisk /data/adb/ksu /data/adb/ap 2>/dev/null"

    suspend fun detect(rootGranted: Boolean): RootManager {
        // بلا جذر ممنوح لا يُسأل الجهاز أصلًا، فلا يُقال «لا مدير» عن شيء لم يُفحص:
        // «مجهول» صادق، و«لا شيء» ادّعاء. (وفي وضع Shizuku يبقى الجذر خارج المعادلة.)
        if (!rootGranted) return RootManager.UNKNOWN
        val outcome = PrivilegedShell.exec(PROBE)
        val result = outcome as? PrivilegedShell.Outcome.Ran
        if (result?.lane != PrivilegedShellRules.Lane.ROOT) return RootManager.UNKNOWN
        val output = result.output
        return when {
            output.contains("/data/adb/magisk") -> RootManager.MAGISK
            output.contains("/data/adb/ksu") -> RootManager.KERNEL_SU
            output.contains("/data/adb/ap") -> RootManager.APATCH
            // جذر ممنوح بلا مسار إدارة معروف: صادقٌ أن نقول «أخرى» لا «مجهول».
            outcome is PrivilegedShell.Outcome.Ran && outcome.ok -> RootManager.OTHER
            else -> RootManager.UNKNOWN
        }
    }
}

/** حالة SELinux كما يعلنها النواة — قراءة فقط، بلا جذر. */
enum class SelinuxMode { ENFORCING, PERMISSIVE, UNKNOWN }

object SelinuxStatus {
    private val enforceFile = File("/sys/fs/selinux/enforce")

    fun read(file: File = enforceFile): SelinuxMode = runCatching {
        when (file.readText().trim()) {
            "1" -> SelinuxMode.ENFORCING
            "0" -> SelinuxMode.PERMISSIVE
            else -> SelinuxMode.UNKNOWN
        }
    }.getOrDefault(SelinuxMode.UNKNOWN)
}
