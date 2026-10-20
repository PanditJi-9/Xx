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

package nd.max.core.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nd.max.BuildConfig
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.ui.util.VersionIdentity

/**
 * منسّق التحديث الوحيد: يقرأ الحكم من الخليلين ([UpdatePolicy] + [UpdateStore])، ويجلب
 * ([UpdateTransport])، ويُنزّل، ويثبّت ([UpdateInstaller]) — **والواجهة تُراقب فقط**.
 *
 * لماذا singleton لا ViewModel: الحكم يجب أن يكون **جاهزًا قبل أول شاشة** (الحجب يعني أن
 * المستخدم لا يجوز أن يرى الواجهة أصلًا)، والجلب يجب أن يقع **مرّة** للأشتراك كله — لا مرّة
 * لكل شاشة تُفتح. والـViewModel بعدها غلافٌ رقيق يعرض `state` ويُمرّر الأفعال.
 *
 * **والفصل المقصود:** لا شيء من هذا الملفّ يُخزَّن في `state` إلّا ما تحتاجه الواجهة كي
 * ترسم؛ والشبكة والملفّات في `Dispatchers.IO` دائمًا. وأخطاء **الفحص** لا تدخل الحالة
 * أصلًا — لأن فحصًا فشل في الخلفية يجب ألّا يرسم خطأً أحمر في نافذة تحديث صحيحة جاءت من
 * الذاكرة؛ ومن فحص بنفسه ([checkNow]) يجيب على فعله بنفسه.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    enum class Phase { IDLE, CHECKING, DOWNLOADING, INSTALLING }

    /** نتيجة فحصٍ طلبه المستخدم — تترجمها الشاشة إلى كلمة واحدة، لا إلى نصّ مبعوث. */
    enum class CheckResult { UPDATED, UP_TO_DATE, FAILED, NOT_CONFIGURED }

    data class State(
        val decision: UpdateDecision = UpdateDecision.NONE,
        val phase: Phase = Phase.IDLE,
        val progress: Float = 0f,
        val error: UpdateError? = null,
        val detail: String? = null,
        val noticeDismissed: Boolean = false,
    )

    private val store = UpdateStore(context)
    private val transport = UpdateTransport()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        // أوّل حكم يُبنى من **الذاكرة وحدها** — بلا شبكة وبلا تأجيل. وهذا هو ما يجعل الحجب
        // قائمًا بلا إنترنت لمن رآه مرّة (نقطة «منع الرجوع» في [UpdateStore]).
        recompute()
    }

    fun localVersionCode(): Long = VersionIdentity.readApp(context).versionCode

    /** فحص خلفيّ عند الدخول: لا يُنتظر، ولا يُزعج، ولا يرسم فشله. */
    fun autoCheck() {
        scope.launch { refresh(force = false) }
    }

    /**
     * فحص بطلب صريح من المستخدم. يمسح التخطّي أولًا: من طلب الفحص بنفسه يريد أن يرى ما
     * تخطّاه — وإلّا لكان الفحص اليدويّ يقول «لا جديد» وهو يعلم أن هناك جديدًا تخطّاه.
     */
    suspend fun checkNow(): CheckResult {
        store.clearSkipped()
        return refresh(force = true)
    }

    /** «لاحقًا» — تُسجَّل لرقم هذا الإصدار وحده، والإصدار التالي يعود للظهور. */
    fun skipCurrent() {
        val release = _state.value.decision.release ?: return
        store.markSkipped(release.code)
        recompute()
    }

    /** إغلاق إعلان التعليق غير المُقيِّد (التعليق الحاجب لا يُغلق — الحاجب حاجب). */
    fun dismissNotice() {
        if (_state.value.decision.verdict == UpdateVerdict.MAINTENANCE && !_state.value.decision.blocking) {
            _state.update { it.copy(noticeDismissed = true) }
        }
    }

    fun setChannel(name: String) {
        store.setChannel(name)
        recompute()
    }

    /** تنزيل حزمة ZIP والتحقق من بصمتها؛ لا تُمرَّر إلى مثبت APK. */
    fun install() {
        val release = _state.value.decision.release ?: return
        if (_state.value.phase == Phase.DOWNLOADING || _state.value.phase == Phase.INSTALLING) return
        scope.launch {
            val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, UPDATES_DIR)
            val zip = File(directory, "MaxManager-${release.name}.zip")
            try {
                directory.mkdirs()
                _state.update { it.copy(phase = Phase.DOWNLOADING, progress = 0f, error = null, detail = null) }
                transport.download(release.url, zip, release.sha256) { done, total ->
                    if (total > 0L) {
                        val ratio = (done.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
                        _state.update { it.copy(progress = ratio) }
                    }
                }
                // ZIP محفوظ في مساحة ملفات التطبيق؛ لا نحاول تثبيته كتطبيق Android.
                _state.update {
                    it.copy(phase = Phase.IDLE, progress = 1f, error = null,
                        detail = "ZIP_DOWNLOADED:${zip.absolutePath}")
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(phase = Phase.IDLE) }
                throw cancelled
            } catch (fault: UpdateFault) {
                fail(fault.error, fault.message, record = fault.error == UpdateError.FORMAT)
            } catch (error: Exception) {
                fail(UpdateError.INSTALL, error.message ?: error.javaClass.simpleName, record = true)
            }
        }
    }

    private suspend fun refresh(force: Boolean): CheckResult {
        val url = BuildConfig.UPDATE_MANIFEST_URL
        if (url.isBlank()) return CheckResult.NOT_CONFIGURED

        val age = System.currentTimeMillis() - store.cachedAt()
        val fresh = store.cachedManifest() != null && age in 0 until CACHE_TTL_MS
        if (!force && fresh) {
            recompute()
            return if (_state.value.decision.hasNewer) CheckResult.UPDATED else CheckResult.UP_TO_DATE
        }

        _state.update { it.copy(phase = Phase.CHECKING) }
        val raw = try {
            transport.fetchManifest(url)
        } catch (fault: UpdateFault) {
            // الفحص الفاشل **لا يُسقط حكمًا قائمًا**: الذاكرة هي المرجع، والشبكة تجدّده فقط.
            _state.update { it.copy(phase = Phase.IDLE) }
            recompute()
            return CheckResult.FAILED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // عطبٌ غير متوقّع (لا عطب شبكة): يُهمَل للواجهة ويُنهي الحالة نظيفة — تركُ
            // الحالة على CHECKING كان يُجمّد زرّ الفحص إلى الأبد.
            _state.update { it.copy(phase = Phase.IDLE) }
            recompute()
            return CheckResult.FAILED
        }
        val parsed = UpdateManifests.parse(raw)
        if (parsed == null) {
            // مانيفست منشور لا يُقرأ = عطب نشرٍ حقيقي يُسجَّل ليراه المالك في التشخيص.
            DiagnosticCenter.record(component = "update", message = "published manifest did not parse")
            _state.update { it.copy(phase = Phase.IDLE) }
            recompute()
            return CheckResult.FAILED
        }
        store.saveFetched(raw, System.currentTimeMillis())
        _state.update { it.copy(phase = Phase.IDLE) }
        recompute()
        return if (_state.value.decision.hasNewer) CheckResult.UPDATED else CheckResult.UP_TO_DATE
    }

    /** إعادة اشتقاق القرار من الذاكرة — تتغيّر معها: التخطّي، القناة، أو مانيفست جديد. */
    private fun recompute() {
        val decision = UpdatePolicy.decide(
            localCode = localVersionCode(),
            manifest = store.cachedManifest(),
            channel = store.channel(),
            skippedCode = store.skippedCode(),
        )
        _state.update { it.copy(decision = decision) }
    }

    private fun fail(error: UpdateError, detail: String?, record: Boolean) {
        _state.update { it.copy(phase = Phase.IDLE, error = error, detail = detail) }
        if (record) {
            DiagnosticCenter.record(
                component = "update",
                message = "update failed: $error${detail?.let { " :: $it" } ?: ""}",
            )
        }
    }

    private companion object {
        const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L
        const val UPDATES_DIR = "updates"
    }
}
