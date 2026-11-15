/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

/**
 * نافذة شفافة لا واجهة لها: تطلب إقرار التقاط الشاشة من النظام ثم تُنهي نفسها.
 *
 * **ولماذا صنف مستقلّ:** إذن `MediaProjection` لا يُمنح إلا لنتيجة `Activity`، والمسجّل يُستدعى من
 * اللوحة العائمة ومن خدمة الشريط — أيّ من موضعين لا يملكان دورة حياة نشاط. فبقيت هذه النافذة
 * المسؤول الوحيد عن الحوار، وصار كل مستدعٍ يمرّ بنفس الطريق (§0.5: مسار واحد).
 */
class ScreenRecordRequest : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // إعادة إنشاء بعد دوران/موت العملية: الحوار لا يُعاد تلقائيًّا ولا معنى لإعادته.
        if (savedInstanceState != null) {
            finish()
            return
        }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (manager == null) {
            finish()
            return
        }
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST && resultCode == RESULT_OK && data != null) {
            ScreenRecordService.start(this, resultCode, data)
        }
        finish()
    }

    companion object {
        private const val REQUEST = 4211

        fun launch(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(context, ScreenRecordRequest::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                )
            }
        }
    }
}
