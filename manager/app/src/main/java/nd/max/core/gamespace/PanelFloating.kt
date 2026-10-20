/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.topjohnwu.superuser.Shell
import nd.max.ui.util.PrivilegedShell

/**
 * أمر النافذة العائمة كما يُكتب، بلا تنفيذ: هذا الجزء صافٍ فيُقاس على JVM.
 *
 * **عطبان مقيسان في الأمر السابق جعلاه لا يفتح شيئًا:**
 *  ١. بلا `-W` لا يطبع `am start` سطر `Status: ok` أبدًا، فكان النجاح الفعلي يُعدّ «فشلًا» —
 *     وحين يُعدّ فشلًا لا تنطوي اللوحة، فتبقى ملء الشاشة **فوق** النافذة التي فُتحت تحتها فلا تُرى.
 *  ٢. `--bounds` ليس خيارًا في `am start` أصلًا (يرفضه الأمر بـ`Unknown option`)، فكان الأمر يسقط كله.
 * ومقاس النافذة وموضعها يتركهما النظام لسياسته الافتراضية للنوافذ الحرّة، وهو الأوثق عبر الإصدارات.
 */
object PanelFloatingCommand {
    /** `WindowConfiguration.WINDOWING_MODE_FREEFORM`: نافذة حرّة فوق اللعبة لا شاشة كاملة. */
    const val WINDOWING_MODE_FREEFORM = 5

    /** `-W` ينتظر الإطلاق فيطبع `Status: ok`. والمكوّن يُقتبس لأن أسماء الأصناف الداخلية تحمل `$`. */
    fun build(component: String): String =
        "am start -W --windowingMode $WINDOWING_MODE_FREEFORM -n ${quote(component)}"

    /** مفتاحا «النوافذ الحرّة» و«إجبار قابلية تغيير الحجم» في خيارات المطوّر، يُكتبان معًا. */
    fun enableFreeform(): List<String> = listOf(
        "settings put global enable_freeform_support 1",
        "settings put global force_resizable_activities 1"
    )

    /** `am start -W` يطبع `Status: ok` عند النجاح؛ خروجه وحده بلا `-W` لا يدلّ على فتح نافذة. */
    fun succeeded(output: List<String>?): Boolean = output?.any { it.trim() == "Status: ok" } == true

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}

/** فتح تطبيق من اللوحة كنافذة عائمة. لا يسقط إلى شاشة كاملة أبدًا: إن تعذّر، يُقال السبب بدقّة. */
object PanelFloatingLauncher {
    enum class Outcome { Opened, Unsupported, NoRoot, NotLaunchable, Failed }

    /** الجهاز يدعم النوافذ الحرّة إن أعلن الميزة، أو فُعّل خيار «النوافذ الحرّة» في خيارات المطوّر. */
    fun supported(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)) return true
        return runCatching {
            Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1
        }.getOrDefault(false)
    }

    private fun rootAvailable(): Boolean = runCatching { Shell.getShell().isRoot }.getOrDefault(false)

    /**
     * يفعّل النوافذ الحرّة بالجذر حين يطلب المستخدم فتح تطبيق عائم والجهاز لا يدعمها.
     * أثره عكسيّ (خيارا المطوّر نفسهما) ويُبلَّغ به في [launch] لا يُسكَت عنه.
     */
    private fun enableFreeform(context: Context): Boolean {
        PrivilegedShell.run(*PanelFloatingCommand.enableFreeform().toTypedArray())
        repeat(4) {
            if (supported(context)) return true
            Thread.sleep(120L)
        }
        return supported(context)
    }

    /** يُستدعى من خيط IO (فيه انتظار صدفة الجذر). */
    fun launch(context: Context, pkg: String): Outcome {
        val component = context.packageManager.getLaunchIntentForPackage(pkg)?.component
            ?: return Outcome.NotLaunchable
        if (!rootAvailable()) return Outcome.NoRoot
        if (!supported(context) && !enableFreeform(context)) return Outcome.Unsupported
        val output = PrivilegedShell.run(PanelFloatingCommand.build(component.flattenToShortString()))
        return if (PanelFloatingCommand.succeeded(output)) Outcome.Opened else Outcome.Failed
    }
}
