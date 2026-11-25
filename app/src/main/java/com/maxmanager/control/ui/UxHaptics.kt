/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package com.maxmanager.control.ui

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * نقرة لمسية خفيفة عند تغيير التبويب.
 *
 * **ولماذا `performHapticFeedback` لا `Vibrator`:** الأولى يمرّرها النظام بوصفها تغذية راجعة
 * للواجهة ولا تحتاج إذنًا، بينما الثانية تحتاج `android.permission.VIBRATE` — والبيان لا يحمله
 * (فقط `INTERNET`)، فكان مسار `Vibrator` يفشل بصمت داخل `runCatching` ويُوهم بأن الاهتزاز يعمل.
 * والاختيار هنا يقاس: `CLOCK_TICK` هو أهدأ ما يعرّفه النظام لهذا الغرض، وإن رفضه المُصنّع
 * (يعيد `false`) ننتقل إلى `VIRTUAL_KEY` الأدنى.
 */
object UxHaptics {
    fun tap(view: View?) {
        val target = view ?: return
        if (!target.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) {
            target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
