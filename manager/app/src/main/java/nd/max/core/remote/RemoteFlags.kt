/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

/**
 * سِجلّ المفاتيح المعروفة — **والكود أوّلًا، اللوحة ثانيًا**.
 *
 * §٦.١ من `MAX-CONTROL-01` يمنع مفتاحًا يُخترع من الخادم: المفتاح الذي لا يعرفه التطبيق لا يظهر
 * في اللوحة ولا يُقرأ. فمصدر الحقيقة هنا، واللوحة تقرأ هذه القائمة لعرض ما يعنيه كل مفتاح.
 *
 * ## ومعنى المفتاح — «يقيّد ولا يوسّع» (§٧)
 *
 * كل مفتاح هنا يملك حالةً واحدة لها معنى واحد في التطبيق ([FeatureState]): `off` **تُخفي** السطح
 * أو **تُطفئ** ميزة قائمة، و`subscribe` تُبقي السطح ظاهرًا **مقفلًا بمفتاح ترخيص**، و`on` سلوك
 * اليوم. ولا مفتاح يمنح قدرة لم تكن في هذه النسخة من الحزمة، ولا يفتح مسار كتابة عتاد — فالكتابة
 * كلها خلف `HardwareControlArbiter` وحده (ADR-11)، والمفتاح البعيد لا يمسّها.
 *
 * ## والقيم الافتراضية = **سلوك اليوم**
 *
 * الافتراضي عند غياب الخادم ليس «كل شيء مطفأ» ولا «كل شيء مشغَّل» عشوائيًّا: هو ما يفعله
 * التطبيق اليوم بالضبط. فسقوط الخادم لا يُغيّر شيئًا للمستخدم — وهذا هو معنى §٧ «الافتراضي
 * المحلي عند غياب الخادم»: الغياب يعيد الحالة المعروفة، لا حالةً ثالثة لم يجرّبها أحد.
 */
object RemoteFlags {

    /**
     * @param id المعرّف في `app_config.features` (نفس مفردات المالك القائمة — لا مفردات جديدة).
     * @param enabledByDefault سلوك التطبيق اليوم عند غياب الخادم.
     * @param minVersionCode أقدم `versionCode` يفهم هذا المفتاح؛ وما دونه يتجاهله.
     * @param surface السطح الذي يحكمه — يُعرض في اللوحة، ويُحفظ هنا كي لا يُنسى المعنى.
     */
    data class Flag(
        val id: String,
        val enabledByDefault: Boolean,
        val minVersionCode: Int = 0,
        val surface: String,
        /**
         * هل يُعرض هذا المفتاح في جدول «المتاح الآن» داخل شاشة Max AI؟
         *
         * الافتراض `true` لكل مفتاح يحكم سطح ميزة. و[LICENSE] استثناءٌ مقصود: مفتاحُ
         * **ظهور صفّ الترخيص في الإعدادات**، لا سطحَ قدرة؛ فإدراجه في جدول القدرات يوهم
         * بأنه ميزةٌ تُشترى، والصفّ نفسه يبقى في الإعدادات وحدها.
         */
        val showInMaxAiIntro: Boolean = true,
    )

    /**
     * السبعة القائمة في `app_config` اليوم (قِيس: هي نفسها ما يكتبه المالك من لوحته)، ولكلٍّ
     * سطحٌ موجود فعلًا في الشجرة — ومفتاحٌ بلا سطحٍ يقابله «واجهة وهمية» بعينها.
     *
     * **والمعرّفات ثوابت لا حروف مكتوبة في موضعين**: بوّابة السطح (`FeatureGate`) تُنادى
     * بالثابت نفسه الذي في هذا السجلّ، فلو غُيّر معرّف يومًا تغيّر في الموضعين معًا. ولو كُتب
     * حرفيًّا في الرسم الملاحيّ لصار معرّفٌ منزاحٌ ⇒ `state` تُعيد `OFF` للمجهول ⇒ **اختفاء
     * السطح** بلا رسالة — وهو أسوأ من قفلٍ صريح.
     */
    const val GAMESPACE = "gamespace"
    const val AUDIO = "audio"
    const val ATLAS = "atlas"
    const val MAXAI = "maxai"
    const val SPOOF = "spoof"
    const val EMUHUB = "emuhub"
    const val BYPASS_CHARGE = "bypass_charge"

    /**
     * ظهور صفّ «الترخيص» في الإعدادات — **مطفأ افتراضيًّا** بأمر المالك: يُخفى الباب ما دام
     * هو لا يبيعه. وتشغيله من Max Control يُظهره؛ ولا يُغلق مسار التفعيل من شاشة القفل.
     */
    const val LICENSE = "license"

    val ALL: List<Flag> = listOf(
        Flag(GAMESPACE, enabledByDefault = true, surface = "ui/subscreens/GameLobbyScreen.kt"),
        Flag(AUDIO, enabledByDefault = true, surface = "core/audio"),
        Flag(ATLAS, enabledByDefault = true, surface = "core/atlas"),
        Flag(MAXAI, enabledByDefault = true, surface = "ui/mainscreens/MaxAiScreen.kt"),
        Flag(SPOOF, enabledByDefault = true, surface = "ui/subscreens/AppSpoofSection.kt"),
        Flag(EMUHUB, enabledByDefault = true, surface = "ui/subscreens/EmulatorHubScreen.kt"),
        Flag(BYPASS_CHARGE, enabledByDefault = true, surface = "ui/subscreens/BypassChargeScreen.kt"),
        Flag(
            LICENSE,
            enabledByDefault = false,
            surface = "ui/mainscreens/SettingsScreen.kt",
            showInMaxAiIntro = false,
        ),
    )

    private val byId: Map<String, Flag> = ALL.associateBy(Flag::id)

    fun isKnown(id: String): Boolean = byId.containsKey(id)

    fun flag(id: String): Flag? = byId[id]

    /**
     * الحكم الفعليّ لمفتاح: من الإعداد المقبول إن ذكره، وإلّا افتراضه المكتوب في الكود.
     * ويُشترط `minVersionCode` كذلك: مفتاحٌ لمن يفهمه فقط (وإلّا ظهر مفتاح في اللوحة لا معنى
     * له في نسخةٍ قديمة).
     *
     * وهذا هو السؤال الأول والأخير في التطبيق عن حالة مفتاح — فلا أحد يقرأ `RemoteFeature`
     * بنفسه في موضعٍ ثانٍ، ولا تتباعد قراءتان يومًا.
     */
    fun state(config: RemoteConfig?, id: String, versionCode: Int): FeatureState {
        val declared = byId[id] ?: return FeatureState.OFF
        val remote = config?.features?.get(id)
        val resolved = remote?.state() ?: if (declared.enabledByDefault) FeatureState.ON else FeatureState.OFF
        val floor = remote?.minVersionCode ?: declared.minVersionCode
        return if (versionCode >= floor) resolved else FeatureState.OFF
    }

    /**
     * «هل يُعرض السطح أصلًا؟» — `true` للعامل **وللمقفل بمفتاح** معًا: ميزة الاشتراك تبقى ظاهرة
     * (بشاشة «يلزم مفتاح») ولا تُخفى؛ فالاختفاء للمطفأ وحده. ومن أراد التمييز الكامل فليسأل
     * [state] مباشرةً — وهذه غلافٌ لا ثاني حكم.
     */
    fun isOn(config: RemoteConfig?, id: String, versionCode: Int): Boolean =
        state(config, id, versionCode) != FeatureState.OFF

    /**
     * الإعداد الافتراضي المحلي — **الحكم عند غياب الخادم**: يذكر كل مفتاح بقيمته المكتوبة.
     * `revision = 0` و`issuedAtMillis = 0` عن قصد: هذا ليس إعدادًا صادرًا من خادم، ولا يجوز أن
     * يُسجَّل كأنه كذلك (فيُقرأ في اللوحة «آخر نشر: ١٩٧٠» — ولهذا يُقرأ أصله من
     * [ConfigOrigin] لا من هذه الحقول).
     */
    fun defaultConfig(): RemoteConfig = RemoteConfig(
        revision = 0,
        issuedAtMillis = 0,
        expiresAtMillis = null,
        features = ALL.associate { it.id to RemoteFeature(it.enabledByDefault, it.minVersionCode) },
        announcement = null,
        latest = null,
        stats = null,
        unknownFeatureKeys = emptyList(),
    )
}
