/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * **مشاركة ملف واحد — مسار واحد مهما كان الملفّ.**
 *
 * كان هذا المسار مكتوبًا داخل شاشة السجلّات وحدها، فلمّا احتاجه المختبر (تقرير نتائج
 * العتاد) صار أمامنا طريقان: نسخه، أو إخراجه. ونُسخه يعني أن يتباعد القناع أو سلطة
 * `FileProvider` في أحد الموضعين بصمت — وهو العطب الذي يُكتشف عند المستخدم لا عندنا.
 *
 * والقناع (`mimeType`) يُمرَّر من المستدعي لأنه يعرف ما كتبه: نصّ · zip · غيره.
 */
fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        setType(mimeType)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, file.name))
}

/**
 * يكتب نصًّا في `cacheDir` باسم ثابت ويشاركه كنصّ.
 *
 * **والاسم ثابت لا مُولَّد:** ملفّ واحد يُستبدل في كل مشاركة، فلا تتراكم نسخ في ذاكرة الجهاز
 * كلّما ضغط المستخدم الزرّ.
 */
fun shareText(context: Context, name: String, text: String) {
    val file = File(context.cacheDir, name)
    file.writeText(text)
    shareFile(context, file, "text/plain")
}
