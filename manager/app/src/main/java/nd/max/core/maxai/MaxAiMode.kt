/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import nd.max.MaxManagerPaths
import nd.max.MaxManagerProps
import nd.max.core.hardware.ProfileApplier
import nd.max.core.hardware.RootFileAccess
import nd.max.core.platform.PropertyUtils

/**
 * الوضع الواحد الذي يراه المستخدم: ملفّ النظام وأوزان Max AI قرارٌ واحد.
 *
 * كان في التطبيق مبدّلان: «الملف العام» (performance/balanced/eco) ومبدّل «هدف»
 * Max AI (performance/balanced/battery). الأول يملكه الخادم ويُطبَّق عند الإقلاع
 * وعند أحداث اللعب/البطارية، والثاني يملكه المحرك — فيكتبان العتاد نفسه ويتراجع
 * أحدهما عن الآخر. وبعد الدمج صار **وضع Max AI** هو الملفّ العام نفسه:
 *
 * - `performance` ⇒ الملفّ `1` ووزن الأداء.
 * - `balanced`    ⇒ الملفّ `2` والأوزان المتوازنة.
 * - `battery`     ⇒ الملفّ `3` ووزن البطارية.
 *
 * ويُحفظ الاختيار في `API/max_ai_mode` لا في `current_profile`: الثاني **الوضع
 * الفاعل الآن** يكتبه الخادم عند كل تبديل مؤقّت (لعب/بطارية منخفضة)، والأول
 * **نيّة المستخدم** التي يعود إليها الخادم بعد كل تبديل. فقراءة `current_profile`
 * على أنها الأساس كانت تجعل وضع اللعب هو «اختيار المستخدم» عند الإقلاع التالي.
 *
 * وهذا الملفّ يقرؤه الخادم عند الإقلاع وعند تغييره، ويكتبه التطبيق عند اختيار
 * الوضع. و`AIobjective` يبقى **مرآة مشتقة** للتوافق مع الإشعارات والخصائص القديمة،
 * لا خيارًا ثانيًا.
 */
object MaxAiMode {

    const val PERFORMANCE = ProfileApplier.PROFILE_PERFORMANCE
    const val BALANCED = ProfileApplier.PROFILE_BALANCED
    const val BATTERY = ProfileApplier.PROFILE_ECO

    private val VALID = setOf(PERFORMANCE, BALANCED, BATTERY)

    fun isValid(profileId: String?): Boolean = profileId != null && profileId in VALID

    /** مفتاح Max AI المحفوظ ⇒ رقم الملفّ الذي يفهمه الخادم. */
    fun profileIdForPreference(preference: String?): String = when (preference) {
        "performance" -> PERFORMANCE
        "battery" -> BATTERY
        else -> BALANCED
    }

    /** رقم الملفّ ⇒ مفتاح Max AI الذي تقرؤه بقية الطبقات. */
    fun preferenceForProfile(profileId: String?): String = when (profileId) {
        PERFORMANCE -> "performance"
        BATTERY -> "battery"
        else -> "balanced"
    }

    /** نيّة المستخدم المحفوظة، أو null إن لم تُكتب بعد. */
    fun currentProfileId(): String? =
        RootFileAccess.read(MaxManagerPaths.MAX_AI_MODE)?.trim()?.takeIf { isValid(it) }

    /** يكتب النيّة وحدها — بلا لمس العتاد، عبر بوابة طبقة العتاد. */
    fun write(profileId: String): Boolean {
        if (!isValid(profileId)) return false
        return ProfileApplier.persistMaxAiMode(profileId)
    }

    /**
     * ترحيل لمرّة واحدة: قبل الدمج كان `AIobjective` هو التعبير الوحيد عن نيّة
     * المستخدم، بينما `current_profile` قد يكون وضعًا مؤقّتًا (لعب/بطارية).
     * فحين يغيب ملفّ الوضع الجديد تُؤخذ النيّة من الهدف القديم، لا من الوضع الفاعل.
     */
    fun migrateIfMissing(): String {
        currentProfileId()?.let { return it }
        val legacy = PropertyUtils.get(MaxManagerProps.Conf.AI_OBJECTIVE)
        val migrated = profileIdForPreference(legacy)
        write(migrated)
        PropertyUtils.set(MaxManagerProps.Conf.AI_OBJECTIVE, preferenceForProfile(migrated))
        return migrated
    }

    /**
     * يطبّق الوضع عبر خدمة الوحدة: يكتب النيّة ويحدّث مرآة الهدف ثم ينفّذ الملفّ.
     * `fromAi=true` يمرّ من بوابة `--from-ai` حين يكون Max AI مفعّلًا.
     */
    fun applyProfile(profileId: String, fromAi: Boolean): Boolean {
        if (!isValid(profileId)) return false
        write(profileId)
        PropertyUtils.set(MaxManagerProps.Conf.AI_OBJECTIVE, preferenceForProfile(profileId))
        return if (fromAi) ProfileApplier.applyFromAi(profileId) else ProfileApplier.apply(profileId)
    }
}
