/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.platform

/**
 * حالة عرض القراءة — أربعة أخبار مختلفة كانت خبرًا واحدًا.
 *
 * | الحالة | ما تقوله للقارئ |
 * | --- | --- |
 * | [Waiting] | لم تصل عيّنة جديدة بعد؛ ما في اليد قيمة محفوظة لا تُعرض كأنها الآن |
 * | [Live] | آخر عيّنة جديدة نجحت، والأرقام هي اللحظة |
 * | [Stale] | الجدول المعروض من آخر نجاح، والمحاولات الأخيرة لم تصل |
 * | [Failed] | لا جدول أصلًا بعد محاولتين فاشلتين — هنا فقط يُقال «تعذّرت القراءة» |
 */
enum class ProcessReadState { Waiting, Live, Stale, Failed }

/**
 * حكم واحد على **تتابع** عيّنات [ProcessWatch] لا على عيّنة واحدة معزولة.
 *
 * ### العطب الذي وُلد منه (٢٠٢٦-١٠-٢١)
 *
 * `ProcessWatch.snapshot` يحفظ آخر قيمة، والمستمع الجديد يستقبلها فورًا عند الاشتراك. وكانت
 * [nd.max.ui.viewmodel.ProcessManagerViewModel] تعدّ **كل** بثّ «عيّنة وصلت» وتضع `loaded = true`،
 * والقيمة المحفوظة قبل أوّل قراءة هي [ProcessSample.noAnswer] — فتظهر بطاقة «لا جواب من الجهاز»
 * الحمراء ثوانيَ قبل أن يجمع القارئ أوّل جدول، ويظنّ المستخدم أنّ عطلًا وقع (وهو ما أبلغ عنه
 * المالك بلقطة). هذا الحكم يفصل ثلاثة أشياء كانت واحدة:
 *
 * 1. **بثّ محفوظ** (`tick` لم يتقدّم) ليس عيّنة جديدة ولا يُعلن شيئًا.
 * 2. **فشل عابر** واحد ليس عطبًا؛ ويُحكم بعد [failuresBeforeError] محاولتين متتاليتين.
 * 3. **فشل بعد نجاح** لا يمسح الجدول المعروض ولا يقلب الشاشة حمراء؛ يقول «محفوظة».
 *
 * والحدّ المعلن: العدّاد يتقدّم لكل **محاولة** قراءة لا لكل نجاح — وهذا ما يسمح بالتمييز بين
 * «لم تُقرأ بعد» و«قُرئت وفشلت» بلا حقل جديد في [ProcessSample]، وبلا الاعتماد على نصّ عيّنة.
 */
class ProcessReadGuard(
    private val failuresBeforeError: Int = 2,
    private val failuresBeforeStale: Int = 2,
) {

    var state: ProcessReadState = ProcessReadState.Waiting
        private set

    /** آخر عدّاد قُرئ؛ ما دونه قيمة محفوظة من جلسة سابقة. */
    private var lastTick = 0

    /** محاولات فاشلة متتالية بعد آخر نجاح. */
    private var misses = 0

    /** هل وصلت قراءة ناجحة واحدة؟ الفشل بعدها خبرٌ مختلف عن الفشل قبلها. */
    private var answered = false

    /** يبدأ جلسة جديدة عند العدّاد [tick]؛ ما قبله لا يُحتسب عيّنة جديدة. */
    fun begin(tick: Int) {
        lastTick = tick
    }

    /**
     * عيّنة من القارئ المشترك.
     *
     * @param tick عدّاد العيّنات بعدها (يزيد لكل محاولة، ناجحة كانت أو فاشلة).
     * @param failed هل فشلت هذه المحاولة.
     * @return هل تُعرض هذه العيّنة؟ `false` للبثّ المحفوظ، وللفشل بعد نجاح (فالمعروض أحسن منه).
     */
    fun offer(tick: Int, failed: Boolean): Boolean {
        if (tick <= lastTick) return false
        lastTick = tick

        if (!failed) {
            answered = true
            misses = 0
            state = ProcessReadState.Live
            return true
        }

        misses += 1
        if (answered) {
            // الجدول السابق يبقى معروضًا: عملية انتهت للتوّ لا تعني اختفاء الجدول كلّه.
            if (misses >= failuresBeforeStale) state = ProcessReadState.Stale
            return false
        }

        state = if (misses >= failuresBeforeError) ProcessReadState.Failed else ProcessReadState.Waiting
        return true
    }
}
