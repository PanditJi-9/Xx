/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import nd.max.core.persist.PersistRestorer
import nd.max.core.security.IntegrityCheckpoint
import nd.max.core.security.SecurityEnforcement

/**
 * **باب الاستعادة بعد الإقلاع** — «حفظ خياراتك وتطبيقها بعد إعادة التشغيل» (§3 من الطلب).
 *
 * ### لماذا مستقبِل ثانٍ ولا يُدمج في `ModuleWatchReceiver`
 *
 * وذاك باب شفاء **الوحدة** (الرفيق والخادم)، هذا باب استعادة **اختيارات المستخدم**. دمجهما
 * يعني أن عطبًا في أحدهما يُسقط الآخر، وأن تعديل شروط أحدهما يمسّ مسارًا لا يخصّه. والاثنان
 * يتقاسمان الأحداث نفسها عن قصد — فالاستعادة أيضًا تنتظر فتح تخزين المستخدم (‏DE لا CE):
 * `BOOT_COMPLETED` و`USER_UNLOCKED` و`MY_PACKAGE_REPLACED`، **ولا `LOCKED_BOOT_COMPLETED`**
 * للسبب نفسه المكتوب هناك: التخزين المحميّ لم يُفت بعد.
 *
 * ### والحدود الثلاثة نفسها
 *
 * 1. **لا عمل على الخيط الرئيسيّ:** إقلاع خدمات وقراءة sysfs ومقارنات — كله على خيط دايمون.
 * 2. **`goAsync()` مع إغلاق مضمون:** بدونه يعود النظام من البثّ وقد قُتلت العملية قبل الفعل.
 * 3. **بلا إعلان نجاح هنا:** الأحكام تُجمع سطرًا واحدًا في `MaxManager.log`
 *    (`EVENT=PERSIST_RESTORE`)، وهذا الملفّ لا يدّعي.
 *
 * ### والحرس قبل الأذن
 *
 * `SecurityEnforcement.refuseReceiver` نفسه الذي يحرس مستقبِل الوحدة، وقصرٌ على المستخدم
 * الأول كما فيه — فلا مسار ثانٍ لمستخدم آخر يكتب في السجلّ نفسه.
 */
class PersistRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (SecurityEnforcement.refuseReceiver(context, IntegrityCheckpoint.RECEIVER)) return
        val action = intent.action ?: return
        if (action !in WATCHED_ACTIONS) return
        if (!isPrimaryUser()) return

        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                runBlocking { PersistRestorer.restoreAll(app) }
            } finally {
                pending.finish()
            }
        }.apply { isDaemon = true }.start()
    }

    /** المستخدم الأول يُقاس بمقارنة المُقدَّين المُعلَنَين وحدهما — كما في مستقبِل الوحدة. */
    private fun isPrimaryUser(): Boolean = runCatching {
        android.os.Process.myUserHandle() == android.os.UserHandle.getUserHandleForUid(0)
    }.getOrDefault(true)

    private companion object {
        val WATCHED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
