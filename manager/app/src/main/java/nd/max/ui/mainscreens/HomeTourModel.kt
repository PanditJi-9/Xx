/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * جولة أول فتح — **شرحٌ خطوةً خطوة فوق عناصر الشاشة الحقيقية**، بترتيب القراءة نفسه (طلب المالك).
 *
 * الترتيب هنا هو ترتيب الشاشة من أعلى إلى أسفل، فالجولة تنزل مع القارئ ولا تقفز بين أطراف الصفحة.
 * الخطوات تشير إلى مراسي (`HomeTourTarget`) تعلّمها كل شاشة بـ`HomeTourAnchor(...)`، ويحرس
 * `HomeShapeContractTest` أن لكل هدف علامةً في شاشته.
 *
 * **والجولة تعبر التبويبات الأربعة (طلب المالك، ٢٠٢٦-١٠-١٣):** بعد خطوات الرئيسية تنتقل إلى
 * التحكم فالتطبيقات فالإعدادات، ثم **تعود إلى الرئيسية عند الإتمام** — فلا يبقى المستخدم في قسم
 * لم يقصده. ولهذا يحمل كل هدف **قسمه** (`HomeTourSection`): منه تعرف الجولة أيّ تبويب تُنقل إليه
 * قبل إضاءة العنصر، وبه تُمسح مراسي الأقسام الأخرى فيتبع الثقبُ عنصرًا واحدًا لا عنصرًا قديمًا
 * من شاشة لم تعد معروضة.
 *
 * **ومرساة كل تبويب في أعلى صفحته:** التمرير المُدار للرئيسية وحدها (قائمتها في متناولها)، فخطوةٌ
 * تشير إلى كتلة تحت الطيّة في التحكم أو الإعدادات تصل بعد مهلة بلا ثقب — أي إلى شرح بلا مُشار
 * إليه. ولذلك تُذكر بقية الشاشة في نصّ الخطوة (زرّ البحث · تبديل العرض · الأدوات) بدل إشارة لا تُرى.
 *
 * والمرساة **قسمٌ لا رقم شاشة**: لو نُقل تبويبٌ أو صار قسمه تبويبًا آخر، يتغيّر سطر واحد هنا.
 */
package nd.max.ui.mainscreens

import androidx.annotation.StringRes
import nd.max.R

/**
 * التبويب الذي تعيش فيه مراسي الخطوة — وهو **الشاشة التي تُفتح قبل إضاءة العنصر**.
 *
 * `Home` بلا مسار لأنها الشاشة التي تبدأ منها الجولة أصلًا؛ وبقية الأقسام تحمل مسار الوجهة
 * الحقيقيّ (`MaxDestination`) لا نصًّا مكتوبًا بيد، فلا ينفصل المسار عن تعريف الوجهة يومًا.
 */
internal enum class HomeTourSection(val route: String) {
    Home(nd.max.ui.navigation.MaxDestination.Now.route),
    Control(nd.max.ui.navigation.MaxDestination.Control.route),
    Apps(nd.max.ui.navigation.MaxDestination.Apps.route),
    Settings(nd.max.ui.navigation.MaxDestination.Settings.route),
}

/** مواضع الشاشات التي تستطيع الجولة إضاءتها — كلٌّ منها كتلة واحدة في شاشته. */
internal enum class HomeTourTarget(val section: HomeTourSection) {
    // الرئيسية: نفس الكتل الستّ وترتيبها (لا تتغيّر — الخطوات تعرضها بالترتيب نفسه).
    Hero(HomeTourSection.Home),
    Vitals(HomeTourSection.Home),
    Actions(HomeTourSection.Home),
    Pulse(HomeTourSection.Home),
    Memory(HomeTourSection.Home),
    Deck(HomeTourSection.Home),

    /**
     * التحكم: مجموعات المجالات — **وهي أول ما يُرى في الصفحة**، فلا تحتاج تمريرًا ولا شاشةً
     * أخرى لتوضيحها.
     */
    ControlDomains(HomeTourSection.Control),

    /** التطبيقات: حقل البحث، ثم شرائح التصفية التي تحمل الأعداد — الاثنان في أعلى الصفحة. */
    AppsSearch(HomeTourSection.Apps),
    AppsFilters(HomeTourSection.Apps),

    /**
     * الإعدادات: أوّل مجموعة في الصفحة (هوية التطبيق · السمة · اللغة) — هي المرساة **المعروضة**
     * دائمًا. وصفّ الوصول ونسخة الضبط تحتها بيُخبر بها نصّ الخطوة بدل إشارة لا تُرى.
     */
    SettingsTop(HomeTourSection.Settings),
}

/** خطوات الجولة بترتيب الشاشة: البطاقة الأولى، ثم CPU/GPU، ثم الفعل، ثم القراءات الحيّة، ثم البقية. */
internal enum class HomeTourStep(
    val target: HomeTourTarget,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
) {
    /** أوّلًا: البطاقة الأولى — ما هذا الجهاز وأي صلاحية يحملها Max عليه. */
    Identity(HomeTourTarget.Hero, R.string.home_tour_identity_title, R.string.home_tour_identity_body),

    /** ثم CPU وGPU: تاريخ التردد كما يظهر تحتهما. */
    Pulse(HomeTourTarget.Pulse, R.string.home_tour_pulse_title, R.string.home_tour_pulse_body),

    /** ثم زرّا التعزيز والتحكم. */
    Actions(HomeTourTarget.Actions, R.string.home_tour_actions_title, R.string.home_tour_actions_body),

    /** ثم القراءات الحيّة التي لا تتكرر في مكان آخر. */
    Vitals(HomeTourTarget.Vitals, R.string.home_tour_vitals_title, R.string.home_tour_vitals_body),

    /** السعات. */
    Memory(HomeTourTarget.Memory, R.string.home_tour_memory_title, R.string.home_tour_memory_body),

    /** منصة التحكم: الاختصارات الشخصية. */
    Deck(HomeTourTarget.Deck, R.string.home_tour_deck_title, R.string.home_tour_deck_body),

    /** التحكم: تسعة مجالات، كلٌّ بابٌ إلى شاشة. */
    ControlDomains(HomeTourTarget.ControlDomains, R.string.home_tour_control_domains_title, R.string.home_tour_control_domains_body),

    /** التطبيقات: البحث. */
    AppsSearch(HomeTourTarget.AppsSearch, R.string.home_tour_apps_search_title, R.string.home_tour_apps_search_body),

    /** التطبيقات: شرائح التصفية وأعدادها. */
    AppsFilters(HomeTourTarget.AppsFilters, R.string.home_tour_apps_filters_title, R.string.home_tour_apps_filters_body),

    /** الإعدادات: المظهر واللغة — أوّل ما يُقرأ في الصفحة، وأبواب البقية تحته. */
    SettingsTop(HomeTourTarget.SettingsTop, R.string.home_tour_settings_top_title, R.string.home_tour_settings_top_body),
}

internal object HomeTourModel {
    val steps: List<HomeTourStep> = HomeTourStep.entries.toList()

    val count: Int get() = steps.size

    /** الفهرس بعد خطوة إلى الأمام، ويثبت عند الأخيرة (الإتمام قرار الواجهة لا الموديل). */
    fun next(index: Int): Int = (index + 1).coerceAtMost(steps.lastIndex)

    /** الفهرس بعد خطوة إلى الخلف، ويثبت عند الأولى. */
    fun previous(index: Int): Int = (index - 1).coerceAtLeast(0)

    fun isLast(index: Int): Boolean = index >= steps.lastIndex

    /**
     * الأقسام بترتيب عرضها: كل قسم متّصل لا يتكرّر ولا يعود إلى قسم سبق.
     *
     * تُشتقّ من الخطوات لا تُكتب بيد، فيستحيل أن تقول الجولة «الخطوة ٧ من ١٢» في قسم وتُضيء
     * عنصرًا في قسم آخر. ويحرس التسلسل `HomeShapeContractTest` عبر هذه الدالّة نفسها.
     */
    fun sections(): List<HomeTourSection> {
        val out = mutableListOf<HomeTourSection>()
        steps.forEach { step ->
            if (out.lastOrNull() != step.target.section) out += step.target.section
        }
        return out
    }
}
