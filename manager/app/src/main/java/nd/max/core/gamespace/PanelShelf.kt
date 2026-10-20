/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

/** تطبيق يُعرض في قائمة التحرير: مثبّت وله شاشة تشغيل، ومعه اسمه للعرض. */
data class PanelAppChoice(val pkg: String, val label: String)

/**
 * منطق الترتيب الصافي للتطبيقات السريعة: بلا أندرويد، فيُقاس على JVM.
 *
 * الترتيب: المثبَّت أولًا بترتيبه، ثم الأكثر فتحًا من هذه اللوحة، ثم الافتراضيّ. وما ليس قابلًا
 * للإطلاق الآن لا يدخل أيًّا منها، فلا يظهر زرّ لتطبيق محذوف.
 */
object PanelShelfModel {
    const val MAX_PICKS = 6
    const val SHELF_SIZE = 6

    /** مرشّحات أولى قبل أن يتعلّم الترتيب شيئًا (كانت قائمة ثابتة في اللوحة). */
    val DEFAULTS: List<String> = listOf(
        "com.android.chrome", "com.google.android.gm", "com.whatsapp", "com.discord",
        "org.telegram.messenger", "com.google.android.youtube", "com.google.android.apps.messaging",
        "com.facebook.orca",
    )

    private val PKG = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    /** أسماء حزم صالحة فقط، بلا تكرار، وبسقف [MAX_PICKS]. */
    fun picksSanitized(raw: List<String>): List<String> =
        raw.map { it.trim() }.filter { PKG.matches(it) }.distinct().take(MAX_PICKS)

    fun compose(
        picks: List<String>,
        uses: Map<String, Int>,
        launchable: Set<String>,
        limit: Int = SHELF_SIZE,
    ): List<String> {
        val out = LinkedHashSet<String>()
        for (pkg in picksSanitized(picks)) if (pkg in launchable) out.add(pkg)
        uses.entries
            .filter { it.value > 0 && it.key in launchable }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .forEach { out.add(it.key) }
        for (pkg in DEFAULTS) if (pkg in launchable) out.add(pkg)
        return out.take(limit)
    }
}

/** اللوحة تقرأ التطبيقات من هنا: الأسماء المُختارة، وما يصلح للإطلاق فعلًا. */
object PanelShelf {

    /** كل ما يُفتح من اللوحة: مثبَّت وله مدخل تشغيل، مرتّبًا بالاسم. */
    fun launchable(context: Context): List<PanelAppChoice> = runCatching {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: pkg
                PanelAppChoice(pkg, label)
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    /** الشريط كما يُرسم: الترتيب مع الأيقونة والعنوان، وما لا يُحمَّل يُسقَط لا يُخترَع. */
    fun build(
        context: Context,
        picks: List<String>,
        uses: Map<String, Int>,
        limit: Int = PanelShelfModel.SHELF_SIZE,
    ): List<QuickApp> {
        val byPkg = launchable(context).associateBy { it.pkg }
        val pm = context.packageManager
        return PanelShelfModel.compose(picks, uses, byPkg.keys, limit).mapNotNull { pkg ->
            runCatching {
                QuickApp(pkg = pkg, label = byPkg[pkg]?.label ?: pkg, icon = pm.getApplicationIcon(pkg).toBitmap(96))
            }.getOrNull()
        }
    }

    private fun Drawable.toBitmap(px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, px, px)
        draw(Canvas(bmp))
        return bmp
    }
}
