/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nd.max.core.display.NightLightSchedule

/**
 * ينفّذ تبديلة الوضع الليلي ويعيد جدولة التالية.
 *
 * **ولماذا يعالج أربعة أحداث إضافية غير منبّهه:** تغيّر الساعة أو المنطقة الزمنية
 * يُزيح النافذة عن وقتها المحسوب، وإعادة التشغيل تُلغي كل المنبّهات، وتحديث الحزمة
 * كذلك. في الأربعة تُقرأ الحالة من جديد وتُضبط النافذة وتُجدول التالية — لا يمكن
 * أن يبقى الضوء على نافذة الأمس صامتًا.
 *
 * **ولا عمل ثقيل هنا:** قراءة تفضيل، وكتابة مفتاح واحد، وضبط منبّه واحد.
 */
class NightLightScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            NightLightSchedule.ACTION_APPLY,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                if (!NightLightSchedule.isEnabled(app)) return
                NightLightSchedule.applyNow(app)
                NightLightSchedule.ensureScheduled(app)
            }
        }
    }
}
