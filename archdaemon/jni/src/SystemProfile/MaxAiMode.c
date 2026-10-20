/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#include <MaxManager.h>

/**
 * الوضع الواحد: قرارات نقيّة تُقاس على المضيف، وقراءة واحدة للملفّ/الخاصية.
 *
 * لماذا ملفّ جديد بدل تعديل `ProfileUtility.c`: `ProfileUtility.c` يملك كتابة
 * `current_profile` (الوضع الفاعل)، وهذا الملفّ يملك **نيّة المستخدم** فقط.
 * الفصل هو ما يمنع وضع اللعبة المؤقّت من أن يصير «اختيار المستخدم» عند الإقلاع
 * التالي. والدوال هنا بلا `fopen` في اختبارات القرار النقيّ.
 */

ProfileMode max_ai_mode_from_saved(int saved) {
    if (saved >= PERFORMANCE_PROFILE && saved <= ECO_MODE)
        return (ProfileMode)saved;
    return BALANCED_PROFILE;
}

ProfileMode max_ai_mode_from_objective(const char* objective) {
    if (objective != NULL && strcmp(objective, "performance") == 0)
        return PERFORMANCE_PROFILE;
    if (objective != NULL && strcmp(objective, "battery") == 0)
        return ECO_MODE;
    return BALANCED_PROFILE;
}

/**
 * @brief هل يُطبَّق وضع أساس عند هذه الحالة؟
 * @return الوضع المطلوب، أو `PERFCOMMON` حين يكون Max AI مطفأً (لا ملفّ إقلاع).
 *
 * `PERFCOMMON` هنا علامة «لا تُطبّق» لا ملفّ رابع؛ الخادم يستعمله أصلًا للتهيئة
 * (`run_profiler(0)`)، ولا يعني وضعًا للمستخدم.
 */
ProfileMode max_ai_mode_resolve(bool ai_enabled, int saved, const char* objective) {
    if (!ai_enabled)
        return PERFCOMMON;
    if (saved >= PERFORMANCE_PROFILE && saved <= ECO_MODE)
        return (ProfileMode)saved;
    return max_ai_mode_from_objective(objective);
}

/**
 * @brief يقرأ نيّة المستخدم: الملفّ أولًا، ثم `AIobjective` القديم، ثم Balanced.
 *
 * القراءة لا تحكم هل يُطبَّق؛ `System.c` يسأل `prev_ai_state` قبل التطبيق. فالملفّ
 * يبقى محفوظًا حتى لو كان AI مطفأً، ويُطبَّق عند تشغيله.
 */
ProfileMode max_ai_mode_read(void) {
    int saved = -1;
    FILE* fp = fopen(MAX_AI_MODE, "r");
    if (fp != NULL) {
        char value[16] = {0};
        if (fgets(value, sizeof(value), fp) != NULL)
            saved = atoi(value);
        fclose(fp);
    }

    if (saved >= PERFORMANCE_PROFILE && saved <= ECO_MODE)
        return (ProfileMode)saved;

    char objective[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.maxmanagerconf.AIobjective", objective);
    return max_ai_mode_from_objective(objective);
}

/** مرآة الهدف: تُكتب مع الوضع ليبقى الإشعار والخصائص القديمة متوافقة. */
const char* max_ai_objective_for_mode(ProfileMode mode) {
    switch (mode) {
        case PERFORMANCE_PROFILE:
            return "performance";
        case ECO_MODE:
            return "battery";
        case BALANCED_PROFILE:
        default:
            return "balanced";
    }
}
