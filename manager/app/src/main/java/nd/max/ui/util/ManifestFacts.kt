/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

/**
 * ما يعلنه مانيفست حزمة مثبَّتة — كما يقرأه مدير الحزم، لا كما يُخمَّن.
 *
 * **ولماذا هنا لا داخل الشاشة:** الحساب كله قراءة واحدة من `PackageManager`؛ فصله يجعل
 * القواعد قابلة للاختبار ويمنع الشاشة من إعادة عدّ المكوّنات في كل إعادة رسم.
 *
 * **وما لا يُقال:** حقل لم يعدّه مدير الحزم يبقى `null` أو `-1` في المصدر، وتُترجمه الشاشة
 * إلى «غير معلوم» — ولا يُعرض صفرًا، لأن الصفر يعني «مقيس ولا يوجد» وهو جواب آخر.
 */
data class ManifestFacts(
    val versionName: String?,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val installLocation: Int,
    val activities: Int,
    val services: Int,
    val receivers: Int,
    val providers: Int,
    val exportedComponents: Int,
    val splitCount: Int,
    val debuggable: Boolean
) {
    val components: Int get() = activities + services + receivers + providers
}

object ManifestFactsReader {

    @Suppress("DEPRECATION")
    fun read(context: Context, pkg: String): ManifestFacts? {
        val manager = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS
        val info = runCatching { manager.getPackageInfo(pkg, flags) }.getOrNull() ?: return null

        val activities = info.activities.orEmpty()
        val services = info.services.orEmpty()
        val receivers = info.receivers.orEmpty()
        val providers = info.providers.orEmpty()
        val exported = activities.count { it.exported } +
            services.count { it.exported } +
            receivers.count { it.exported } +
            providers.count { it.exported }

        val app = info.applicationInfo
        return ManifestFacts(
            versionName = info.versionName,
            versionCode = info.longVersionCode,
            minSdk = app?.minSdkVersion ?: 0,
            targetSdk = app?.targetSdkVersion ?: 0,
            installLocation = app?.installLocation ?: PackageInfo.INSTALL_LOCATION_AUTO,
            activities = activities.size,
            services = services.size,
            receivers = receivers.size,
            providers = providers.size,
            exportedComponents = exported,
            splitCount = info.splitNames?.size ?: 0,
            debuggable = app != null && (app.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
    }
}
