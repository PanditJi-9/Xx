/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import android.content.pm.PackageManager
import nd.max.core.platform.PackageFeatures

/**
 * يقرأ ما تطلبه حزمة من ميزات (`<uses-feature>`) وما يعلنه الجهاز، ويقارن بينهما.
 *
 * **ولماذا `GET_CONFIGURATIONS`:** `reqFeatures` لا يُملأ بلا هذا العلم، فقراءةٌ بدونه تعطي
 * قائمة فارغة تُقرأ خطأً «لا متطلبات» بدل «لم تُقرأ». وهو أرخص من `GET_META_DATA` بكثير.
 *
 * **وما لا يُقال:** غياب الميزة من إعلان الجهاز لا يعني استحالة العمل؛ بعض التطبيقات تطلب
 * `camera` بلا `required="true"`. الشاشة تعرض الفرق، والقرار يبقى للمستخدم.
 */
object PackageFeaturesReader {

    fun read(context: Context, pkg: String): PackageFeatures {
        val manager = context.packageManager
        val declared = runCatching {
            manager.getPackageInfo(pkg, PackageManager.GET_CONFIGURATIONS)
                .reqFeatures
                .orEmpty()
                .mapNotNull { it.name }
        }.getOrDefault(emptyList())
        val available = runCatching {
            manager.systemAvailableFeatures.orEmpty().mapNotNull { it.name }.toSet()
        }.getOrDefault(emptySet())
        return PackageFeatures.split(declared, available)
    }
}
