/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.core.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * التثبيت نفسه — مساران، ويُقدَّم الجذر لأنه «أسهل طريقة» بلا وسيط:
 *
 * 1. **بالجذر (`pm install -r`)**: نفس ثلاث الخطوات التي يستعملها التطبيق اليوم في تحديث
 *    الوحدة ([nd.max.MainActivity]): نسخ إلى `/data/local/tmp` ثم `pm install -r` ثم حذف
 *    القيادة. ولماذا النسخ إلى هناك تحديدًا: `installd` يقرأ الحزمة بصلاحياته هو، وملفٌّ
 *    في مجلّد التطبيق الخاصّ لا يضمن الوصول إليه — والنسخ إلى مساحة القراءة العامة يزيل
 *    هذا الشكّ كلّه. والتنفيذ في صدفة الجذر يُكمل حتى لو قُتل التطبيق أثناءه (وهو يُقتل
 *    فعلًا عند تثبيت نسخةٍ فوق نفسه، وهذا متوقَّع لا عطب).
 * 2. **بلا جذر**: يُسلَّم الملفّ إلى **مثبّت النظام** عبر `FileProvider` (المزوّد والقيد
 *    موجودان في البيان أصلًا: `nd.max.provider` و`REQUEST_INSTALL_PACKAGES`). الشاشة
 *    النظامية تظهر والمستخدم يُكمل بزرّ واحد — وهذا أقصى ما تسمح به أندرويد بلا امتياز.
 *
 * **والأمانة في المكان:** القيمة المعادة من مسار الجذر هي **نصّ الخطأ** أو `null` للنجاح؛
 * ولا يُعاد `Boolean` لأن «فشل» بلا سبب يُترجم في الواجهة إلى «فشل التثبيت» وقد يكون
 * السبب «PM رفض لأن الحزمة أقدم» أو «لا مساحة». والنصّ يُعرض ويُسجَّل.
 */
object UpdateInstaller {

    /** مساحة القراءة العامة التي يقرأ منها `installd` بلا شكّ. */
    const val STAGING_PATH = "/data/local/tmp/MaxManager_update.apk"

    private const val MIME_APK = "application/vnd.android.package-archive"

    /** هل التطبيق يملك جذرًا **مُمنوحًا** الآن؟ سؤالٌ قبل أي صدفة حتى لا تُفتح نافذة SU بلا داع. */
    fun rootAvailable(): Boolean = runCatching { Shell.isAppGrantedRoot() == true }.getOrDefault(false)

    /** تثبيت صامت بالجذر. تُعاد رسالة الفشل، أو `null` عند النجاح. */
    fun installWithRoot(apk: File): String? {
        val path = apk.absolutePath
        if (!isSafeForShell(path)) return "unsafe apk path: $path"
        val result = Shell.cmd(
            "cp $path $STAGING_PATH",
            "pm install -r $STAGING_PATH",
            "rm -f $STAGING_PATH",
        ).exec()
        if (result.isSuccess) return null
        return result.out.joinToString("\n").ifBlank { "pm install failed" }
    }

    /** فتح مثبّت النظام على الملفّ. `false` إن لم يوجد نشاطٌ يستقبل الطلب (نظام ناقص). */
    fun installWithSystem(context: Context, apk: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MIME_APK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /**
     * حروف المسار المسموح بها. والمسارات التي نبنيها نحن (`cacheDir/updates/MaxManager-<رقم>.apk`)
     * تقع فيها دائمًا؛ والفحص موجود لأن سطر الأوامر يُبنى بـ`String` و`Shell` لا يهرّب شيئًا:
     * مسارٌ فيه مسافة أو فاصلة علياكان يقسم الأمر إلى قسمين بلا أن يشتكي أحد.
     */
    private fun isSafeForShell(path: String): Boolean =
        path.isNotEmpty() && path.all { it.isLetterOrDigit() || it in "/._-" }
}
