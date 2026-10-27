/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.island.core

import android.content.Context
import android.os.Build

/**
 * قراءة ROM الفعلية **مرّة واحدة عند فتح الشاشة**، وبلا أثر: لا كتابة ولا تعطيل.
 *
 * والحزم المُفحوصة أربع معروفة فقط، بـ`getPackageInfo` لا بجرد كل التطبيقات — فحص أربع
 * استعلامات أرخص من تعداد جهاز كامل، ولا يُبطئ فتح الشاشة.
 */
object NativeIslandProbe {
    private val candidates = setOf(
        "com.miui.systemui",
        "com.oplus.systemui",
        "com.coloros.systemui",
        "com.vivo.systemui",
        "com.hihonor.systemui",
        "com.huawei.systemui",
    )

    fun read(ctx: Context): NativeIslandVerdict {
        val present = candidates.filterTo(mutableSetOf()) { pkg ->
            runCatching { ctx.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }
        return NativeIslandRules.classify(
            NativeIslandFacts(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                sdk = Build.VERSION.SDK_INT,
                systemPackages = present,
            )
        )
    }
}
