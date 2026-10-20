/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import nd.max.core.update.UpdateManager

/**
 * غلافٌ رقيق حول [UpdateManager] — لا حالة ولا منطق هنا.
 *
 * وهذا مقصود: الحكم (حجب؟ تحديث؟ لا شيء؟) يعيش في الـsingleton لأن أول شاشة يجب ألّا
 * تُرسم قبل أن يُعرف، ولو كانت الحالة هنا لبَنى كلُّ مالكِ نطاقٍ (النشاط · شاشة الإعدادات ·
 * مستضيف النافذة) نسخةً وقرّر لنفسه — والفرق بين نسختين = شاشةٌ تقول «حدّث» وأخرى لا
 * تعرف، وهو أسوأ من الفحص نفسه.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager,
) : ViewModel() {

    val state: StateFlow<UpdateManager.State> = manager.state

    fun autoCheck() = manager.autoCheck()

    fun install() = manager.install()

    fun skip() = manager.skipCurrent()

    fun dismissNotice() = manager.dismissNotice()

    suspend fun checkNow(): UpdateManager.CheckResult = manager.checkNow()
}
