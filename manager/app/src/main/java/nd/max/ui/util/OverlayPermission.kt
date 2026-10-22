/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * صلاحية «الظهور فوق التطبيقات الأخرى» — الفحص وفتح نافذة المنح.
 *
 * **ولماذا موضع مشترك:** كانت `Settings.canDrawOverlays` تُفحص في شاشات الجزيرة وتُترك في
 * Game Space، فتُفعَّل اللوحة الجانبية ويُقال «مفعّلة» بينما `WindowManager.addView` يرفض
 * النافذة ويُغلق الخدمة صامتة — فيظنّ المستخدم أن الميزة لا تعمل أصلًا. الفحص هنا واحد
 * ليستعمله الفحص والبند الإرشادي والخدمة.
 *
 * **والفتح بثلاث درجات:** النيّة المباشرة بحزمة التطبيق هي الصحيحة، وبعض الرومات لا تحلّها
 * فتُفتح قائمة «الظهور فوق التطبيقات» العامة، وإن تعذّر الاثنان تُفتح صفحة التطبيق نفسها —
 * بدل إسقاط النقر بلا أثر. وكل نداء محاط بـ`runCatching` لأن `ActivityNotFoundException`
 * عطب روم لا عطب تطبيق، ويجب ألّا يُسقط الشاشة.
 */
object OverlayPermission {

    /** هل تسمح المنصّة فعلًا برسم نافذة تراكب الآن؟ `false` عند تعذّر القراءة. */
    fun granted(context: Context): Boolean =
        runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    /** يفتح نافذة المنح، أو أقرب بديل متاح على هذا الروم. */
    fun openSettings(context: Context) {
        val packageUri = Uri.parse("package:${context.packageName}")
        val direct = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(direct) }.isSuccess) return

        val list = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(list) }.isSuccess) return

        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
