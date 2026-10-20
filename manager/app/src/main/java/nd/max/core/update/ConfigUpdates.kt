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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nd.max.core.remote.RemoteLatest

/**
 * قناة التحديث التي **يملكها المالك من لوحته**: رقم الإصدار ورابط التنزيل في الإعداد الموقّع.
 *
 * ## لماذا قناة ثانية إلى جانب المانيفست
 *
 * قناة المانيفست (`UpdateManager`) تبقى كما هي للحجب والتعليق، لكنها تحتاج استضافة عامّة
 * لمستودع يحمل ملفًّا — وهذا ما تعذّر قياسه فعلًا: رابط المانيفست في البناء القديم يردّ **404**
 * (قِيس حيًّا: المستودع خاصّ، وGitHub لا يخدم raw لمجهول). أمّا الإعداد الموقّع فالوصول إليه
 * يعمل اليوم بلا حساب (`config_signed` يُقرأ بمفتاح publishable)، والمالك يكتب فيه ما يريد
 * من لوحة Max Control: أي إصدار وأي رابط.
 *
 * ## وطلب المالك هو العقد
 *
 * «أريد فقط أن أضع أي رابط أريده، وعندما يضغط المستخدم على تحديث يفتح له الرابط» — فالنافذة
 * تفتح الرابط في المتصفّح (`ACTION_VIEW`) ولا تنزّل شيئًا داخل التطبيق، ولا تلمس أي مسار
 * تثبيت. والقرار هنا **خالص** (بلا Context وبلا شبكة) فيُقاس على JVM، والواجهة ترسم فقط.
 *
 * ## حدود مُعلنة
 *
 * * النسخة المثبَّتة تُقرأ من نظام الحزم (`versionName`) وقد تحمل بادئة `V` — تُطبَّع هنا.
 * * نسخة التطبيق غير المقروءة (`UNKNOWN`) **لا تُخفي** تحديثًا منشورًا: العرض أفضل من الكتمان،
 *   و«لاحقًا» تُغلقه بلمسة. والكتمان هنا يعني تحديثًا لا يعلم به أحد أبدًا.
 * * ولا حجب من هذه القناة: هي إعلانٌ إرشاديّ. الحجب يبقى للمانيفست وحده (UpdateVerdict.BLOCKED).
 */
object ConfigUpdates {

    /**
     * حكم قناة اللوحة — أربع حالات بأسماء صريحة، ولا خامسة ضمنية.
     */
    enum class Verdict {
        /** إصدار منشور أحدث من المثبَّت، ورابطه موجود ⇒ تُعرض النافذة. */
        AVAILABLE,

        /** المنشور حديثٌ أو مساوٍ للمثبَّت ⇒ «أنت على أحدث إصدار». */
        UP_TO_DATE,

        /** لا إصدار منشور في الإعداد (أو لم يصل إعداد بعد) ⇒ لا شيء لأعرضه. */
        NOT_PUBLISHED,

        /** إصدار منشور لكن رابط تنزيله فارغ ⇒ عطب نشرٍ يُقال صراحةً ولا يُخمَّن له رابط. */
        LINK_MISSING,
    }

    /**
     * تطبيع رقم إصدار إلى مقاطع عددية: `V2.1` ⇒ `[2, 1]` و`2.10.0` ⇒ `[2, 10, 0]`.
     * ويقبل لاحقة غير رقمية في آخر مقطع (`2.6-beta` ⇒ `[2, 6]`) لأن لاحقة الوسم ليست رقمًا
     * يُقارن. وما لا يبدأ بعدد (`UNKNOWN`) ⇒ `null` بلا تخمين.
     */
    fun normalize(version: String): List<Int>? {
        val cleaned = version.trim().trimStart('v', 'V')
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 4) return null
        val numbers = ArrayList<Int>(parts.size)
        for (part in parts) {
            val digits = part.trim().takeWhile { it.isDigit() }
            if (digits.isEmpty()) return null
            numbers.add(digits.toIntOrNull() ?: return null)
        }
        return numbers
    }

    /**
     * مقارنة رقمية مقطعًا مقطعًا (`2.10 > 2.9`، و`2.1 == 2.1.0`) — و`null` إن تعذّر تطبيع
     * أحد الطرفين: لا حكم بلا رقمين مفهومين.
     */
    fun compare(a: String, b: String): Int? {
        val x = normalize(a) ?: return null
        val y = normalize(b) ?: return null
        for (i in 0 until maxOf(x.size, y.size)) {
            val order = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (order != 0) return order
        }
        return 0
    }

    /** الحكم الكامل: من الإصدار المنشور والرابط والنسخة المثبَّتة. */
    fun decide(latest: RemoteLatest?, installedVersionName: String): Verdict {
        val version = latest?.version?.trim().orEmpty()
        if (version.isEmpty()) return Verdict.NOT_PUBLISHED
        if (latest?.page?.trim().isNullOrEmpty()) return Verdict.LINK_MISSING
        val order = compare(version, installedVersionName)
        return if (order == null || order > 0) Verdict.AVAILABLE else Verdict.UP_TO_DATE
    }

    /**
     * هل يعود الإصدار الاختياري للظهور الآن؟
     *
     * * إصدار منشور أحدث ولم يُتخطَّ بعد ⇒ نعم.
     * * تخطّاه المستخدم ⇒ **ليس قبل الفاصل** الذي كتبه المالك في اللوحة.
     * * وتخطٍّ قديم بلا زمن (بيانات النسخة السابقة) يُعامَل كأنه وقع الآن، فلا تنقلب
     *   الترقية إلى نافذة تُفتح فورًا لكل من كان قد تخطّى الإصدار.
     */
    fun reminderDue(
        latest: RemoteLatest?,
        dismissedVersion: String,
        dismissedAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        val published = latest?.version?.trim().orEmpty()
        if (published.isEmpty()) return false
        if (published != dismissedVersion.trim()) return true
        if (dismissedAtMillis <= 0L) return false
        val interval = (latest?.effectiveReminderHours ?: RemoteLatest.DEFAULT_REMINDER_HOURS).toLong() *
            MILLIS_PER_HOUR
        return nowMillis - dismissedAtMillis >= interval
    }

    /**
     * حكم الظهور الموحّد: الإلزاميّ يظهر دائمًا ما دام منشورًا أحدث، والاختياري يتبع
     * [reminderDue]. ولا شيء هنا يقرأ التخزين — فيُقاس على JVM بلا Context.
     */
    fun shouldShow(
        latest: RemoteLatest?,
        verdict: Verdict,
        dismissedVersion: String,
        dismissedAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        if (verdict != Verdict.AVAILABLE) return false
        if (latest?.required == true) return true
        return reminderDue(latest, dismissedVersion, dismissedAtMillis, nowMillis)
    }

    private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L

    // ── الطلب اليدوي من «الإعدادات ← التحقّق من التحديثات» ─────────────────────────────────────

    private val _manualRequests = MutableStateFlow(0L)

    /** عدّاد الطلبات اليدوية: كل زيادة تعني «أرِني النافذة الآن» — والمستضيف في الجذر يراقبه. */
    val manualRequests: StateFlow<Long> = _manualRequests.asStateFlow()

    /**
     * طلب فحص يدوي. ولا يقع هنا أي فحص: الطلب إشارة، والحكم يُعاد اشتقاقه في الواجهة من الحالة
     * الحيّة — فلا نسختان من المنطق تتباعدان.
     */
    fun requestManualCheck() {
        _manualRequests.update { it + 1 }
    }
}

/**
 * ذاكرة «تخطّي هذا الإصدار» — بإعدادات صغيرة، وبلا أي كتابة خارج مساحة التطبيق.
 *
 * والفلسفة نفسها التي في `UpdateStore`: التخطّي لإصدارٍ **واحد**؛ وإصدارٌ أنشأه المالك بعده
 * يعود للظهور من نفسه. ومسار الفحص اليدوي يمسحه (`clear`) كما يمسح فحص المانيفست تخطّيه.
 */
class ConfigUpdateStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun dismissedVersion(): String = prefs.getString(KEY_DISMISSED, "").orEmpty()

    /**
     * زمن آخر تخطٍّ. وبيانات النسخة السابقة كانت تحفظ الإصدار وحده؛ عند أول قراءة نمنحها
     * «الآن» فلا ينقلب تحديث قديم متخطّى إلى نافذة تُفتح في وجه المستخدم بعد الترقية.
     */
    fun dismissedAtMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val stored = prefs.getLong(KEY_DISMISSED_AT, 0L)
        if (stored > 0L) return stored
        if (dismissedVersion().isNotEmpty()) {
            prefs.edit().putLong(KEY_DISMISSED_AT, nowMillis).apply()
            return nowMillis
        }
        return 0L
    }

    fun dismiss(version: String, atMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_DISMISSED, version.trim())
            .putLong(KEY_DISMISSED_AT, atMillis)
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_DISMISSED).remove(KEY_DISMISSED_AT).apply()
    }

    /** آخر طلب فحص يدوي عالجه مستضيف النافذة — يمنع إعادة الفتح عند كل تركيب جديد. */
    fun handledManualRequest(): Long = prefs.getLong(KEY_MANUAL_HANDLED, 0L)

    fun markManualRequestHandled(request: Long) {
        prefs.edit().putLong(KEY_MANUAL_HANDLED, request).apply()
    }

    private companion object {
        const val FILE_NAME = "config_update_notice"
        const val KEY_DISMISSED = "dismissed_version"
        const val KEY_DISMISSED_AT = "dismissed_at"
        const val KEY_MANUAL_HANDLED = "manual_handled"
    }
}
