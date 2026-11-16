/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * سلوك التدخل — «كيف يتدخل Max AI»، لا «ماذا يريد».
 *
 * الوضع ([Objective]) يقول ما الذي يوازنه المحرك: أداء، بطارية، حرارة. والسلوك
 * يقول كم يتساهل في تعريف «الجهاز بخير»، وكم يسمح لنفسه أن يجرّب مقبضًا مجهولًا.
 * الفصل مقصود: تغيير السلوك **لا يكتب ملف نظام، ولا خاصية عتاد، ولا يغيّر ملكية
 * أي مقبض** — كل ما يفعله أنه يضبط عتبة الرضا وسقف الاستكشاف، ويبقى القرار
 * نفسه مقيدًا بالمخطط والسلامة والمُحكِّم.
 *
 * وهذا ليس مبدّل ملف ثانٍ (ADR-78): الملفّ والوضع قرار واحد، والسلوك تفضيل
 * عن الكيفية لا عن الوجهة. ومَن اختار «أداء» + «هادئ» يطلب أعلى أولوية أداء
 * بأقل تدخل ممكن؛ ومَن اختار «أداء» + «حاسم» يطلب نفس الأولوية لكن بمعالجة
 * أبكر للفجوة. لا يتغيّر أي منهما إلى ملف آخر.
 */
enum class MaxAiBehavior(val id: String) {
    /** لا يتدخل إلا عند فجوة واضحة، ولا يجرّب مقبضًا مجهولًا. */
    CALM("calm"),

    /** الافتراضي: عتبة المحرك القائمة ونفس سقف الاستكشاف المقيس. */
    BALANCED("balanced"),

    /** يرى الفجوة أبكر ويسمح بتجربة أوسع، وتبقى قابلة للاسترجاع دائمًا. */
    DECISIVE("decisive");

    companion object {
        /** ما لم يختر المستخدم شيئًا: نفس سلوك المحرك قبل وجود هذا المفهوم. */
        val DEFAULT = BALANCED

        /** قراءة صريحة: null لمعرّف مجهول أو غائب — لا تُحوَّل إلى الافتراضي. */
        fun fromIdOrNull(id: String?): MaxAiBehavior? = entries.firstOrNull { it.id == id }

        /** قراءة محفوظة قد تكون قديمة أو مجهولة ⇒ الافتراضي لا استثناء. */
        fun fromId(id: String?): MaxAiBehavior = fromIdOrNull(id) ?: DEFAULT

        /**
         * السلوك الفعّال: تجاوز التطبيق إن كان معرّفًا صحيحًا، وإلا الاختيار العام.
         * `"default"` و`null` والمجهول كلها تعني «ورّث العام» لا «متوازن».
         */
        fun resolve(global: MaxAiBehavior, perAppId: String?): MaxAiBehavior =
            fromIdOrNull(perAppId) ?: global
    }
}
