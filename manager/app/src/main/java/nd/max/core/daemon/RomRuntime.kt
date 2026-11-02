/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.daemon

import java.util.concurrent.atomic.AtomicLong
import nd.max.MaxManagerPaths

/**
 * تجهيز بيئة التشغيل حين يكون MaxManager **مدمجًا في الروم** (حزمة المطوّرين) لا موديولًا.
 *
 * **العطب (سجلّ جهاز POCO X6 Pro، ٢٠٢٦-١١-٠١):** بعد تفليش الروم ومنح الجذر بقيت الشارة `IDLE` لأنّ
 * الخادم لم يعمل قطّ، وللأسباب الثلاثة معًا:
 *
 * 1. `init: cannot setexeccon('u:r:maxmanager:s0') … Invalid argument` ثم `SIGABRT` — الروم المجمَّع
 *    بنسخ الملفّات إلى الصور لا يحمل نطاق `maxmanager` في سياسته، فيُجهَض الخادم عند أول إقلاع.
 * 2. التطبيق يقرأ مسار الخادم من `/data/adb/modules/MaxManager` (غير موجود في الروم).
 * 3. ما كان يُجهّزه `customize.sh`/`service.sh` للموديول (مجلّد الإعداد، قائمة الألعاب، رفيق Java)
 *    لا يُنفّذه أحد في الروم.
 *
 * فالتشغيل في الروم يقوم به **التطبيق بالجذر الذي مُنح له** (`ModuleWatchman`) — بنطاق الجذر نفسه الذي
 * يعمل به الموديول — فلا يُشترط تعديل SELinux في الروم. وهذا الملفّ يحمل الخطوة الأولى: المجلّدات.
 */
object RomRuntime {

    /** الحارس يمنع الروم من تشغيل مسار الموديول: الثنائيّ **ليس** تحت `/data/adb/modules/MaxManager`. */
    fun isRom(serviceBin: String): Boolean = !serviceBin.startsWith("${MaxManagerPaths.MODULE_DIR}/")

    private val CONFIG = MaxManagerPaths.MODULE_CONFIG

    /**
     * ينشئ شجرة الإعداد نفسها التي ينشئها `mainfiles/customize.sh` (الأسطر 91–96)، ثم قائمة ألعاب
     * **فارغة** إن غابت — `verify_system_integrity()` في الخادم يخرج حين لا يجد الملفّ، والتطبيق يملأه
     * لاحقًا (`ApplistViewmodel`). ولا يُمَسّ ملفّ موجود (`[ -s … ]`).
     */
    val PREPARE: String =
        "mkdir -p '$CONFIG' '$CONFIG/debug' '$CONFIG/API' '$CONFIG/preload' '$CONFIG/bypasschgconfig' " +
            "'$CONFIG/gamelist' '$CONFIG/runtime' && " +
            "{ [ -s '${MaxManagerPaths.APPLIST_JSON}' ] || echo '{}' > '${MaxManagerPaths.APPLIST_JSON}'; } && " +
            "chmod 0755 '$CONFIG' 2>/dev/null; true"
}

/**
 * **إعادة المحاولة لحظة منح الجذر.**
 *
 * حارس الوحدة ([ModuleWatchman]) يُشغَّل عند قيام العملية وعند الإقلاع — وكلاهما قد يسبق منح الجذر
 * (المستخدم يفلّش الروم ثم يمنح الإذن من مدير الجذر **بعد** فتح التطبيق). فلو لم يُعَد النظر عند المنح
 * بقي الخادم متوقّفًا والشارة `IDLE` حتى يُقتل التطبيق ويُفتح من جديد.
 *
 * وهو **لا يعمل إلا في وضع الروم** (في الموديول `service.sh` هو المشغّل)، ولا أكثر من مرّة كل دقيقة
 * (`isRootGranted()` يُنادى كثيرًا)، وعلى خيط مستقلّ لأنّ الحارس ينفّذ صدفة ويقيس وينتظر.
 */
object RomRuntimeKick {
    private const val MIN_GAP_MS = 60_000L
    private val lastKickMs = AtomicLong(0L)

    fun onRootGranted() {
        if (!MaxManagerPaths.isRomInstall()) return
        val now = System.nanoTime() / 1_000_000L
        val last = lastKickMs.get()
        if (last != 0L && now - last < MIN_GAP_MS) return
        if (!lastKickMs.compareAndSet(last, now)) return
        Thread { runCatching { ModuleWatchman().watch() } }
            .apply { isDaemon = true }
            .start()
    }
}
