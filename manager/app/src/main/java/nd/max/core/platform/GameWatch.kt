/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.platform

import android.content.Context
import nd.max.core.gamespace.GameLibraryAccess

/**
 * هل الحزمة الأمامية لعبة؟ — سؤال واحد يقرأه مسجّل الجلسات كل دورة، فالإجابة تُكاش.
 *
 * **ولماذا كاش ٦٠ ثانية:** الاستعلام يمسح كل أنشطة المشغّل عند كل نداء، وحلقة العيّنات تعمل
 * كل ثانية. فالسؤال يُطرح كل ثانية والجواب يُعاد بناؤه كل دقيقة — والتعديل اليدوي في مكتبة
 * الألعاب يُبطل الكاش فورًا عبر [invalidate]، فلا ينتظر المستخدم دقيقة ليرى أثره.
 *
 * **ومصدر الحقيقة واحد:** [GameLibraryAccess] نفسه الذي تعرضه شاشة الألعاب. لا قائمة ثانية
 * تفترق عن الأولى يومًا (ADR-18).
 */
object GameWatch {

    private const val TTL_MS = 60_000L

    @Volatile private var loadedAtMs = 0L
    @Volatile private var games: Set<String> = emptySet()

    fun isGame(context: Context, packageName: String?): Boolean {
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: return false
        val now = System.currentTimeMillis()
        if (now - loadedAtMs >= TTL_MS) refresh(context, now)
        return pkg in games
    }

    /** يُعاد البناء فورًا — يُنادى بعد تعديل مكتبة الألعاب، أو من اختبار. */
    fun refresh(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val app = context.applicationContext
        val set = HashSet<String>()
        runCatching { GameLibraryAccess.apps(app) }
            .getOrDefault(emptyList())
            .forEach { if (it.detectedGame) set += it.packageName }
        runCatching { GameLibraryAccess.manual(app) }
            .getOrDefault(emptySet())
            .forEach { set += it }
        runCatching { GameLibraryAccess.excluded(app) }
            .getOrDefault(emptySet())
            .forEach { set -= it }
        games = set
        loadedAtMs = nowMs
    }

    fun invalidate() {
        loadedAtMs = 0L
    }
}
