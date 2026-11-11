/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.diagnostics

import android.content.Context
import android.os.Build

/**
 * **قراءة هويّة الجهاز من موضع واحد.**
 *
 * كانت تُبنى داخل `AppMonitor.logDeviceFacts()` وحدها — عملية الخادم التي تكتب ترويسة السجل.
 * فلمّا احتاجها تقرير المختبر (يُشارَك خارج الجهاز، فيجب أن يقول **عن أي جهاز** يتحدّث) صار
 * أمامنا طريقان: نسخ الحقول، أو إخراج القارئ. ونسخُها يعني أن يتباعد الحقلان بصمت: يُضاف حقل
 * إلى أحدهما فيغيب عن الآخر، فيبدو التقريران عن جهازين مختلفين.
 *
 * **وحدّ الصدق (ADR-07):** كل حقل لا يُقرأ يبقى `null`، ويُعرض بـ[DeviceFacts.render] كـ`-`.
 * لا `"unknown"` ولا قيمة بديلة تشبه معرفة.
 *
 * @param moduleVersion يُمرَّر من المستدعي لأن قراءته تختلف بحسب العملية: الخادم يقرأه بـ`grep`
 *   على ملفّ الوحدة، والتطبيق يقرأه بما يملك من صلاحية. وما لا يُقرأ يبقى `null` — لا يُخمَّن.
 */
object DeviceFactsReader {

    fun read(context: Context?, moduleVersion: () -> String? = { null }): DeviceFacts {
        val appVersion = runCatching {
            val manager = context?.packageManager ?: return@runCatching null
            manager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        val sdk = Build.VERSION.SDK_INT
        return DeviceFacts(
            appVersion = appVersion,
            moduleVersion = moduleVersion(),
            // `Build.SOC_*` أُضيف في S؛ وما قبلها لا يُقرأ ⇒ `null` لا سلسلة فارغة تُقرأ كقيمة.
            socManufacturer = if (sdk >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER.ifBlank { null } else null,
            socModel = if (sdk >= Build.VERSION_CODES.S) Build.SOC_MODEL.ifBlank { null } else null,
            hardware = Build.HARDWARE.ifBlank { null },
            apiLevel = sdk,
            kernel = System.getProperty("os.version"),
            // قياس لا ادّعاء: هذه العملية يبدأها `service.sh` بجذر، و"جذر" هنا هو هويّة العملية.
            rooted = runCatching { android.os.Process.myUid() == 0 }.getOrDefault(false),
            board = Build.BOARD.ifBlank { null },
            abi = Build.SUPPORTED_ABIS.firstOrNull(),
            manufacturer = Build.MANUFACTURER.ifBlank { null },
            model = Build.MODEL.ifBlank { null },
        )
    }
}
