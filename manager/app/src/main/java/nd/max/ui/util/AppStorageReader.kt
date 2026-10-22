/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * قارئ أحجام التطبيقات — قسم «أكبر التطبيقات» في شاشة العناية بالتخزين.
 *
 * **ودرجتان لا واحدة، والفرق يُعلَن في الشاشة:**
 *
 * 1. **بصلاحية «الوصول إلى بيانات الاستخدام»:** القياس من `StorageStatsManager` — حجم الحزمة
 *    + بياناتها + كاشها لكل تطبيق. وهذا هو الرقم الذي يجيب «من أكل مساحتي؟».
 * 2. **بلا الصلاحية:** حجم APK وحده من `sourceDir`. حدّ أدنى صادق (الرقم حقيقي) لا مجموع
 *    مُقدَّر — و`precise = false` في الصفّ يقوله، والشاشة تشرحه بسطرها.
 *
 * **ولا قياس بلا معنى:** تطبيق فشل قياسه كليًّا **يُسقط صفّه** ولا يُكتب له صفر — نفس قاعدة
 * `null` ليست صفرًا الحاكمة على عائلة التنظيف.
 *
 * **والقراءة ثقيلة** (استعلام لكل حزمة) فتُنادى من `Dispatchers.IO` وحدها، وهذا مكتوب في
 * عقد الدالّة لا في ذاكرة القارئ.
 *
 * **وحدّ معلن:** الحزم التي يراها التطبيق هي ما تسمح به رؤية الحزم المعلنة في البيان
 * (`<queries>`) — أي التطبيقات ذات الأيقونة أساسًا — وهذا **المرئيّ للمستخدم** لا كل ما في
 * النظام، فلا يُوعد بما لا يُرى. (وسرد التنظيف للبقايا لا يمرّ من هنا أصلًا: ذاك يقرأ
 * `pm list packages` بالجذر في `StorageCareEngine`، لأن الحكم هناك «أحذف أم لا» لا «أعرض».)
 */
package nd.max.ui.util

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import java.io.File

/** تطبيق واحد بمقاسه. */
data class AppStorageEntry(
    val packageName: String,
    val label: String,
    val bytes: Long,
    /** تطبيق نظام؟ يُوسم في الصفّ، ولا يُستثنى — حجمه يهمّ كما يهمّ غيره. */
    val system: Boolean,
    /** من `StorageStatsManager` (صلاحية الاستخدام ممنوحة) أم من حجم APK فقط. */
    val precise: Boolean,
    /**
     * تفصيل الرقم حين يكون من `StorageStatsManager`: الحزمة والبيانات والكاش.
     *
     * **وهو الجواب على «لماذا هذا التطبيق كبير؟»** — الرقم الواحد لا يقولها. والثلاثة
     * `null` معًا حين لا صلاحية (فالمعروض حجم الحزمة وحده)، ولا تُصطنع أصفار مكان مجهول.
     */
    val packageBytes: Long? = null,
    val dataBytes: Long? = null,
    val cacheBytes: Long? = null,
)

object AppStorageReader {

    /** عدد الصفوف المعروضة: اثنا عشر تكفي شاشة، وأكثر منها قائمة لا تُقرأ. */
    const val ROW_LIMIT = 12

    /** صلاحية «الوصول إلى بيانات الاستخدام» ممنوحة؟ — فحص صامت لا نافذة. */
    fun hasUsageAccess(context: Context): Boolean = runCatching {
        // مسار priv-app: ملفّ السماح يمنح PACKAGE_USAGE_STATS، و`StorageStatsManager` يقبل
        // الصلاحية نفسها بلا عملية appops. والفحص هنا يمنع شاشةً تقول «تحتاج منحًا» لتطبيق
        // مُنح فعلًا عبر XML — وهو عين الإصلاح الذي طلبه المالك.
        if (context.checkPermission(
                Manifest.permission.PACKAGE_USAGE_STATS,
                Process.myPid(),
                Process.myUid(),
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return@runCatching true
        }
        // مسار تطبيق المستخدم (الوحدة): العملية وحدها هي ما يمنحه `service.sh`.
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** نافذة منح الصلاحية — تُفتح من الشاشة (الطبقة التي تملك سياق الواجهة). */
    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /**
     * أكبر [limit] تطبيقًا بمقاسهم، تنازليًّا. **ثقيل: يُنادى من IO لا من الخيط الرئيسي.**
     */
    fun read(context: Context, limit: Int = ROW_LIMIT): List<AppStorageEntry> {
        val pm = context.packageManager
        val stats = statsService(context)
        val user = Process.myUserHandle()

        return installedPackages(pm)
            .asSequence()
            .mapNotNull { pkg -> entryOf(pm, stats, user, pkg) }
            .sortedByDescending { it.bytes }
            .take(limit)
            .toList()
    }

    /**
     * قراءة تطبيق واحد — **بعد مسح كاشه أو بياناته**: الصفّ في الشاشة يقول ما بقي لا ما كان.
     *
     * ويعيد نفس شكل [read] حرفيًّا، فالشاشة لا تعرف مصدر الرقم: صفٌّ واحد أو قائمة.
     */
    fun readOne(context: Context, packageName: String): AppStorageEntry? {
        val pm = context.packageManager
        return entryOf(pm, statsService(context), Process.myUserHandle(), packageName)
    }

    private fun statsService(context: Context): StorageStatsManager? =
        if (hasUsageAccess(context)) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else {
            null
        }

    /** صفّ واحد من حزمة: القياس الدقيق إن توفّر، وإلا حجم الحزمة — أو `null` إن فشل الكل. */
    private fun entryOf(
        pm: PackageManager,
        stats: StorageStatsManager?,
        user: UserHandle,
        pkg: String,
    ): AppStorageEntry? {
        val info = applicationInfo(pm, pkg) ?: return null
        val row = stats?.let { service ->
            runCatching {
                service.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, user)
            }.getOrNull()
        }
        val measured = row?.let { it.appBytes + it.dataBytes + it.cacheBytes }
        val bytes = measured
            ?: runCatching { File(info.sourceDir).length() }.getOrNull()
            ?: return null
        return AppStorageEntry(
            packageName = pkg,
            label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg),
            bytes = bytes,
            system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            precise = measured != null,
            packageBytes = row?.appBytes,
            dataBytes = row?.dataBytes,
            cacheBytes = row?.cacheBytes,
        )
    }

    private fun installedPackages(pm: PackageManager): List<String> = runCatching {
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        infos.map { it.packageName }.distinct()
    }.getOrDefault(emptyList())

    private fun applicationInfo(pm: PackageManager, pkg: String): ApplicationInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
    }.getOrNull()
}
