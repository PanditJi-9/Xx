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

/**
 * ذاكرة التحديث على الجهاز — أربعة أشياء فقط، وكلٌّ له سبب لا يستقيم بدونه:
 *
 * 1. **آخر مانيفست مقروء** — وهو **قلب منع الرجوع**: من قرأ مرّة «الأرضيّة 150» وبقي على
 *    نسخةٍ تحتها، يبقى الحكم قائمًا حتى بلا شبكة. لو لم يُخزَّن المانيفست لكان إطفاء
 *    الراوتر أول خطوةٍ في التحايل — وهذا مذكور صراحةً في الحدود (لا حماية كاملة بلا خادم).
 * 2. **وقت آخر قراءة** — لحساب عمر الملفّ وعدم سؤال الخادم في كل فتحة.
 * 3. **الإصدار المُتخطّى** ([markSkipped]) — «لاحقًا» تعني لاحقًا **لهذا الإصدار**؛ تُسجَّل
 *    برقمها لا بعلامة، فإصدارٌ أحدث يعود للظهور فورًا (وهو ما يثبّته `UpdatePolicyTest`).
 * 4. **القناة** — `stable` افتراضًا، ومن أراد `beta` يغيّرها (لا واجهة لها بعد؛ المفتاح في
 *    الملفّ نفسه، والمانيفست يدعم القنوات أصلًا).
 *
 * ولا يقرأ هذا الملفّ شيئًا من الشبكة ولا يفسّر شيئًا: يُخزّن ويُعيد، والحكم في
 * [UpdatePolicy]. وملفّ التفضيلات منفصل عن `settings` عن قصد: حالة تحديث تُصفَّر وحدها
 * بلا لمس تفضيلات المستخدم.
 */
class UpdateStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun cachedManifest(): UpdateManifest? =
        prefs.getString(KEY_MANIFEST, null)?.let(UpdateManifests::parse)

    fun cachedAt(): Long = prefs.getLong(KEY_FETCHED_AT, 0L)

    fun channel(): String =
        prefs.getString(KEY_CHANNEL, null)?.takeIf { it.isNotBlank() } ?: UpdateManifests.STABLE

    fun skippedCode(): Long = prefs.getLong(KEY_SKIPPED, 0L)

    fun saveFetched(raw: String, fetchedAt: Long) {
        prefs.edit().putString(KEY_MANIFEST, raw).putLong(KEY_FETCHED_AT, fetchedAt).apply()
    }

    fun markSkipped(code: Long) {
        prefs.edit().putLong(KEY_SKIPPED, code).apply()
    }

    fun clearSkipped() {
        prefs.edit().remove(KEY_SKIPPED).apply()
    }

    fun setChannel(name: String) {
        prefs.edit().putString(KEY_CHANNEL, name).apply()
    }

    private companion object {
        const val NAME = "update_state"
        const val KEY_MANIFEST = "manifest_json"
        const val KEY_FETCHED_AT = "fetched_at"
        const val KEY_SKIPPED = "skipped_code"
        const val KEY_CHANNEL = "channel"
    }
}
