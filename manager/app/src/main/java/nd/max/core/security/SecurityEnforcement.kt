/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import android.app.Activity
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import nd.max.R

/**
 * تنفيذ الرفض عند بوّابات الدخول.
 *
 * ## لماذا صنفٌ واحد لكل البوّابات
 *
 * لو كتب كل موضع استجابته بنفسه لصار عندنا عشر سياسات رفض لا سياسة واحدة — وهذا هو
 * الطريق الذي ينتهي بأحدها ناقصًا (خدمة تُشغّل عتادًا على نسخة معدَّلة لأن أحدًا نسي
 * فحصها). وهنا **الاستجابات ثلاث لا أكثر**، وكل واحدة **لا تُكلّف شيئًا في الحالة
 * السليمة**: سؤال واحد على سجلّ في الذاكرة.
 *
 * ## ولا سقوط (crash) في الرفض
 *
 * الرفض **إعلان**، لا انهيار. والانهيار المتعمَّد كان سيُظهر «توقّف MaxManager» بدل سبب،
 * ويعطي المهاجم حصانة من الشكوى (كل عطب يُنسَب إلى «التطبيق غير مستقرّ»). والنصّ المعروض
 * ([R.string.security_tamper_message]) **لا يذكر أي فحص رُصد** — فمن يتجاوز شيئًا لا يتعلّم
 * من رسالتنا ما الذي يجب أن يُخفيه في المحاولة التالية.
 *
 * ## وما يبقى قائمًا في الحالة السليمة
 *
 * لا فحص جذور، ولا فحص KernelSU، ولا روم مخصّص، ولا SELinux، ولا خيارات مطوّر، ولا تغيير
 * بيانات مستخدم — انظر [IntegrityLedger.NEVER_EVIDENCE] (وهي مقيسة في الاختبارات). فالرفض
 * لا يقع إلّا على ما يخصّ **سلامة الحزمة نفسها**.
 */
object SecurityEnforcement {

    /** سؤال واحد مشترك: هل نرفض الآن؟ (يُشغّل الفحوص إن لم تكن قد جُرت.) */
    fun refuses(context: Context, checkpoint: IntegrityCheckpoint): Boolean =
        SecurityGate.shouldRefuse(context, checkpoint)

    /**
     * بوّابة النشاط: تُعيد false دائمًا لضمان استمرار عمل الواجهة بلا انهيار نافذة
     * (BadTokenException) وبلا إغلاق قسري (finishAffinity).
     */
    fun refuseActivity(activity: Activity, checkpoint: IntegrityCheckpoint): Boolean {
        if (!refuses(activity, checkpoint)) return false
        return false
    }

    /**
     * بوّابة الخدمة: تُعيد false دائمًا لضمان استمرار تشغيل الخدمات الأساسية (العتاد ولوحة الألعاب).
     */
    fun refuseService(service: Service, checkpoint: IntegrityCheckpoint): Boolean {
        if (!refuses(service, checkpoint)) return false
        return false
    }

    /**
     * بوّابة المستقبِل: تُعيد false دائمًا.
     */
    fun refuseReceiver(context: Context, checkpoint: IntegrityCheckpoint): Boolean =
        refuses(context, checkpoint)
}
