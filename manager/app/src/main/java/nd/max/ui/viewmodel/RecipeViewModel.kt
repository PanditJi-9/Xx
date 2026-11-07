/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.RecipeBridge
import nd.max.core.hardware.RecipeCatalog
import nd.max.core.hardware.RecipePlan
import nd.max.core.hardware.RecipeRun

/**
 * حالة سطح الوصفة: النصّ، والخطة المحسوبة عنه، وتقرير التنفيذ.
 *
 * **ولماذا ViewModel لا حالة داخل الـcomposable:** نصّ الوصفة قد يكون عشرة أسطر يكتبها المستخدم
 * بيده، و`remember` يُسقطها عند تدوير الشاشة. وأيضًا القراءة من الأصل والتنفيذ عملٌ ممتدّ يجب
 * أن يبقى في نطاق معلوم لا في نطاق تركيب.
 *
 * **وثلاث قواعد مقصودة:**
 * 1. **الخطة تُسقَط مع أي تعديل.** خطةٌ عن نصٍّ آخر أسوأ من لا خطة: قد تُطبَّق أسطر لم تُراجَع.
 * 2. **التنفيذ `NonCancellable`.** لو أُلغيت الشاشة في منتصف وصفة لتُرك نصفها مكتوبًا بلا استرجاع؛
 *    فالتنفيذ يكمل أو يتراجع كاملًا، ثم لا يهمّ إن مات الـViewModel.
 * 3. **القراءة الفاشلة لا تُخلط بالمفتاح المجهول.** الكتالوج غير المقروء يُعلَن بوصفه (شاشة تقول
 *    ذلك) لا بأن كل مفتاح «مجهول» — وهذا فرق بين عطب بناء وعطب وصفة.
 */
@HiltViewModel
class RecipeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val arbiter: HardwareControlArbiter,
) : ViewModel() {

    /** نصّ الوصفة كما كتبه المستخدم. */
    var text by mutableStateOf("")
        private set

    /** مدخلات المحرّك المقروءة من الكتالوج؛ فارغةً تعني «لم تُقرأ» أو «لم يوجد شيء». */
    var catalog by mutableStateOf<List<RecipePlan.Entry>>(emptyList())
        private set

    /** هل انتهت محاولة القراءة؟ يفرّق بين «لم تُقرأ بعد» و«قُرئت فكانت فارغة». */
    var catalogLoaded by mutableStateOf(false)
        private set

    /** آخر خطة (نتيجة «مراجعة»)، أو `null` إن لم تُراجَع بعد. */
    var plan by mutableStateOf<RecipePlan.Plan?>(null)
        private set

    /** تقرير التنفيذ، أو `null` إن لم يُطبَّق شيء بعد. */
    var report by mutableStateOf<RecipeRun.Report?>(null)
        private set

    var running by mutableStateOf(false)
        private set

    /** قراءة الكتالوج مرة واحدة؛ فشلها يعود فارغًا ويُعلَن في الشاشة، لا استثناءً يُسقطها. */
    fun load() {
        if (catalogLoaded) return
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { RecipeCatalog.load(context) }
            catalog = loaded
            catalogLoaded = true
        }
    }

    /** تعديل النصّ يُبطل الخطة والتقرير معًا — لا يُعرض حكمٌ عن نصّ لم يُراجَع. */
    fun edit(value: String) {
        if (value == text) return
        text = value
        plan = null
        report = null
    }

    fun review() {
        plan = RecipePlan.plan(text, catalog)
        report = null
    }

    /**
     * ينفّذ الخطة الحالية إن كانت قابلة للتطبيق بالكامل.
     *
     * ولا يُعاد التخطيط هنا: ما يُطبَّق هو ما عُرض في المراجعة حرفيًّا. ومن عدّل النصّ بعد المراجعة
     * فقدت خطته (‏[edit]) فلم يبقَ ما يُطبَّق.
     */
    fun apply() {
        val decided = plan ?: return
        if (running || !decided.applicable) return
        running = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO + NonCancellable) {
                RecipeRun.run(decided, RecipeBridge.nodeIo(arbiter, TOKEN))
            }
            report = result
            running = false
        }
    }

    private companion object {
        /** معرّف الكاتب في دفتر الـarbiter: ظاهر في السجل، فيُعرف أن الوصفة كتبت. */
        const val TOKEN = "recipe"
    }
}
