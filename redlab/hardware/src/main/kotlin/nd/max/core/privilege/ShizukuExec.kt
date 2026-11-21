/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

/**
 * حكم تنفيذ أمر عبر خدمة المستخدم (`AR-20`) — **نقيّ بلا Android فيُختبر بلا جهاز**.
 *
 * والحالات ثلاث لا اثنتان، وهذا مقصود: «لم يُحاوَل أصلًا» ([Refused]) ليست «حاولنا فانكسر
 * الطريق» ([Failed]) ليست «نُفِّذ وخرج برمز» ([Ran]). وخلطُ الأولى بالثانية هو الذي يُنتج نصًّا
 * يقول «فشل الأمر» والحقيقة «لا امتياز» — فيظنّ المستخدم العطب في الجهاز لا في الإعداد، وهو
 * النفي نفسه الذي يحكمه `ADR-07`: لا قدرة مُبالَغ فيها ولا فشل مُبالَغ فيه.
 */
sealed interface ShizukuExec {

    /**
     * نُفِّذ فعلًا: [code] رمز خروج الأمر، و[output] خَرْجه **المدمج** (stdout + stderr معًا).
     * والدمج اختيار معلن: قارئ واحد لا ينحبس، والمستخدم يرى السبب الذي كتبه النظام نفسه.
     */
    data class Ran(val code: Int, val output: String) : ShizukuExec {
        val ok: Boolean get() = code == 0
    }

    /** رُفض **قبل** أن يصل الخدمة: لا جسر، أو لا إذن، أو أمر غير صالح. [reason] مفتاح تقنيّ. */
    data class Refused(val reason: String) : ShizukuExec

    /** كان الطريق متاحًا وفشل: binder ميت، أو مهلة ربط، أو ردّ غير صالح. */
    data class Failed(val reason: String) : ShizukuExec
}
