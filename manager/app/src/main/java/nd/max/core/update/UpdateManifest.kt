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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * عقد التحديث عن بُعد — الملفّ الذي ينشره المالك على رابط ثابت، فيقرؤه التطبيق.
 *
 * **ولماذا هذا موجود أصلًا:** التحديث القديم في هذا التطبيق كان يعرف **الوحدة المثبَّتة على
 * الجهاز وحده** (`MaxManagerPaths.MODULE_APK` مقابل `BuildConfig.VERSION_CODE`) — أي أنه لا
 * يعرف شيئًا عن إصدارٍ نُشر بعد اليوم الذي ثُبِّت فيه. فمن يبقى على نسخةٍ قديمة لا يسمع عن
 * الجديد إلا إن حدّث الوحدة بنفسه. وهذا العقد يجعل «هناك إصدار أحدث» و«هذه النسخة لم تعد
 * مسموحة» **حكمًا يُنشر من عند المالك**، لا استنتاجًا محليًّا.
 *
 * **الشكل (schema = 1):**
 * ```json
 * {
 *   "schema": 1,
 *   "latest":   { "code": 200, "name": "2.0", "url": "https://…/MaxManager-2.0.apk",
 *                 "sha256": "…", "size": 12345678, "notes": { "en": "…", "ar": "…" },
 *                 "force": false, "page": "https://…/releases" },
 *   "channels": { "beta": { …نفس شكل latest… } },
 *   "minimum":  { "code": 150, "notes": { "en": "…", "ar": "…" } },
 *   "maintenance": { "active": false, "blocking": false, "notes": { "en": "…", "ar": "…" } }
 * }
 * ```
 *
 * **قواعد الحكم:**

 * * `channels[القناة]` يُقدَّم على `latest`؛ والقناة الافتراضية `stable`، ويُختار `latest`
 *   إن لم تُذكر القناة — فالملفّ الأدنى الذي يمكن نشره لا يحتاج أكثر من `latest`.
 * * `minimum.code` أرضيّة: من كان تحتها **مُحجوب** حتى يحدّث. وغيابها = لا حجب.
 * * `maintenance` يعلّق الاستعمال (وإن كان `blocking=false` فنافذة تُغلق، لا حجب).
 * * `notes` خريطة لغة ← نصّ (`en`, `ar` …)؛ والمفتاحان `en`/`ar` هما ما نكتبه يدويًّا
 *   (ADR-14/§0.2)، وبقية اللغات تسقط إلى `en` ثم إلى أول قيمة — ولا يُخترع نصّ.
 *
 * **والقراءة صارمة عمدًا:** ملفّ مكسور أو نوعٌ خاطئ ⇒ `null` لا نموذجٌ نصفه أصفار. لأن
 * القيمة الافتراضية الخاطئة أخطر من الغياب: `code = 0` يقرأ «لا تحديث» بحقّ، لكن `url = ""`
 * يقرأ «رابطٌ موجود» ويكسر زرّ التحديث. فالسقوط إلى «لا شيء» يُبقي النسخة تعمل كما كانت.
 */
@Serializable
data class UpdateRelease(
    val code: Long = 0,
    val name: String = "",
    val url: String = "",
    val sha256: String = "",
    val size: Long = 0,
    val notes: Map<String, String> = emptyMap(),
    val force: Boolean = false,
    val page: String = "",
) {
    /** إصدارٌ بلا رقم أو بلا رابط لا يُعرض أصلًا — فلا زرّ يقود إلى لا شيء. */
    val usable: Boolean get() = code > 0 && url.startsWith("https://")
}

/** أرضيّة الإصدار المسموح — ما تحت [code] يُحجب عن الاستعمال حتى التحديث. */
@Serializable
data class UpdateFloor(
    val code: Long = 0,
    val notes: Map<String, String> = emptyMap(),
)

/** تعليق مؤقت. `blocking=true` يحجب كل شيء، وإلّا فهو إعلانٌ يمكن إغلاقه. */
@Serializable
data class UpdateMaintenance(
    val active: Boolean = false,
    val blocking: Boolean = false,
    val notes: Map<String, String> = emptyMap(),
)

@Serializable
data class UpdateManifest(
    val schema: Int = 1,
    val latest: UpdateRelease? = null,
    val channels: Map<String, UpdateRelease> = emptyMap(),
    val minimum: UpdateFloor? = null,
    val maintenance: UpdateMaintenance? = null,
)

object UpdateManifests {

    /** القناة الافتراضية — ومن يريد `beta` يكتبها في تفضيلاته، لا في الكود. */
    const val STABLE = "stable"

    /**
     * `ignoreUnknownKeys` مقصود: الملفّ يُنشر مستقلًّا عن دورات إصدار التطبيق، فحقلٌ جديد
     * يُضاف غدًا (قناة، ملاحظة، سعر) يجب ألّا يكسر النسخ المثبَّتة اليوم. هذا هو ثمن
     * «حرية التحكّم مستقبلًا» وهو ثمنٌ زهيد: لا نقرأ ما لا نعرف، ولا نسقط عليه.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
    }

    /** تحويل نصّ الملفّ إلى نموذج — أو `null` إذا كان مكسورًا (لا نموذج نصفه أصفار). */
    fun parse(raw: String): UpdateManifest? =
        runCatching { json.decodeFromString<UpdateManifest>(raw) }.getOrNull()

    /**
     * الإصدار المعروض لقناة: قناتها إن وُجدت، ثم `stable` كسقوط، ثم `latest`.
     * والسبب في السقوطين أن الملفّ قد يُنشر بلا `channels` إطلاقًا (حالة المالك الأولى).
     */
    fun releaseFor(manifest: UpdateManifest, channel: String): UpdateRelease? {
        val direct = manifest.channels[channel]
        val stable = manifest.channels[STABLE]
        return (direct ?: stable ?: manifest.latest)?.takeIf { it.usable }
    }

    /**
     * نصّ الملاحظات بلغة المستخدم: المفتاح الكامل (`zh-CN`) ثم الأساس (`zh`) ثم `en` ثم
     * أول قيمة موجودة. و«أول قيمة» ليست عشوائية هنا: المالك هو من يكتب الملفّ، ومن كتب
     * نصًّا واحدًا قصد أن يُقرأ. وأولوية `en` قبلها لأنها اللغة المصدر (ADR-14).
     */
    fun localized(notes: Map<String, String>, language: String): String? {
        if (notes.isEmpty()) return null
        val complete = language.lowercase().replace('_', '-')
        val base = complete.substringBefore('-')
        fun key(predicate: (String) -> Boolean): String? =
            notes.entries.firstOrNull { predicate(it.key.lowercase().replace('_', '-')) }?.value
        // والاختبار كشف عطب حالةٍ حقيقيًّا: طلب `zh` وحدات `zh-CN` لا يتقابلان بمطابقة
        // تساوٍ — وهما زوج العمل الطبيعي في هذا المستودع (`values-zh-rCN`). فالبادئة
        // (`$base-`) تُجرَّب بعد التساوي وقبله لا قبل `en`؟ بل بعدهما وقبل الإنجليزية:
        // الكلّي `zh-CN` أدقّ من اللغة المصدر، والإنجليزية أدقّ من «أول قيمة».
        return key { it == complete }
            ?: key { it == base }
            ?: key { it.startsWith("$base-") }
            ?: key { it == "en" }
            ?: notes.entries.firstOrNull()?.value
    }
}
