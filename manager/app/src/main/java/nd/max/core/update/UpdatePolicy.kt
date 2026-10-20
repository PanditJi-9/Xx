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

/** الحكم النهائي على النسخة الحالية في ضوء ما نُشر — خمس حالات، ولا سادسة ضمنية. */
enum class UpdateVerdict {
    /** لا شيء: النسخة الحالية هي المعروضة، أو لا مانيفست بعد. */
    NONE,

    /** إصدار أحدث موجود، ولم يُطلب فرضه. يُغلق ويُسجَّل حتى لا يزعج في كل فتحة. */
    OPTIONAL,

    /** إصدار أحدث موجود و`force = true`: النافذة لا تُغلق. */
    REQUIRED,

    /** النسخة الحالية تحت `minimum.code`: التطبيق محجوب حتى التحديث. */
    BLOCKED,

    /** `maintenance.active`: تعليق مؤقّت — يُغلق إن كان غير محجوب. */
    MAINTENANCE,
}

/**
 * كل ما تحتاجه الواجهة للحكم — والقرار **خالصٌ** لا Context ولا شبكة، فيُختبر بلا جهاز.
 *
 * [release] موجودة كلما وُجد إصدار أحدث يمكن تنزيله (حتى في [UpdateVerdict.BLOCKED]
 * و[UpdateVerdict.MAINTENANCE] — لأن الحجب بلا زرّ تحديث = طريق مسدود)، و`null` إن لم يوجد.
 * [message] ملاحظات المالك للنصّ المعروض: ملاحظات الإصدار، أو نصّ الحجب، أو نصّ التعليق.
 */
data class UpdateDecision(
    val verdict: UpdateVerdict,
    val localCode: Long,
    val release: UpdateRelease?,
    val floorCode: Long,
    val blocking: Boolean,
    val message: Map<String, String>,
) {
    /** هل يوجد إصلاحٌ معروض أكبر من المثبَّت؟ (الحجب يشترطه أيضًا، والتطبيق يتحقّق به) */
    val hasNewer: Boolean get() = release != null && release.code > localCode

    companion object {
        val NONE = UpdateDecision(UpdateVerdict.NONE, 0L, null, 0L, false, emptyMap())
    }
}

/**
 * القاعدة الواحدة التي تُترجم «ما نُشر» + «ما هو مثبَّت» إلى قرار.
 *
 * **ولماذا دالّة خالصة لا منطق مبعوث في الواجهة:** كل خطأٍ في هذا الملفّ ظاهرٌ للمستخدم —
 * حجبٌ لا يقع فيبقى القديم يعمل، أو حجبٌ يقع على أحدث نسخة فيُقفل التطبيق على المالك نفسه.
 * والاختبار الوحيد المتاح لهذه الحالات هنا هو اختبارٌ على JVM بلا جهاز (لا محاكي في هذه
 * البيئة)، فلا يمكن أن يسكن الحكم `remember{}` داخل شاشة.
 *
 * **والحدّ المُعلَن في [decide]:** رقم نسخة غير مقروء (`localCode <= 0`) **لا يُحجب أبدًا**.
 * لأن «غير معروف» ليست «قديم»؛ وحجبُ نسخةٍ لا نعرف رقمها كان سيُغلق التطبيق على مستخدمٍ
 * نظامه رفض قراءة الحزمة — وهو العطب الذي كان سيدفعنا لقراءة رقمٍ مخترع.
 */
object UpdatePolicy {

    fun decide(
        localCode: Long,
        manifest: UpdateManifest?,
        channel: String = UpdateManifests.STABLE,
        skippedCode: Long = 0L,
    ): UpdateDecision {
        if (manifest == null) return UpdateDecision.NONE

        val release = UpdateManifests.releaseFor(manifest, channel)
        val floor = manifest.minimum?.code ?: 0L
        val maintenance = manifest.maintenance

        if (maintenance?.active == true) {
            return UpdateDecision(
                verdict = UpdateVerdict.MAINTENANCE,
                localCode = localCode,
                // التعليق **ليس حكمًا على الإصدار**، فيُعرض حتى مع رقمٍ مجهول — وهو المخرج
                // الوحيد المتبقي للمالك إن كان رقمٌ ما يقرأ خطأً على أجهزةٍ بعينها.
                release = release.takeIf { localCode > 0L && hasNewer(localCode, it) },
                floorCode = floor,
                blocking = maintenance.blocking,
                message = maintenance.notes,
            )
        }

        // رقمٌ مجهول (‏-1) = **لا إعلان ولا حجب**: لا نقول «حدّث» لنسخةٍ لا نعرفها، ولا نقول
        // «أنت قديم». وهذا ما كشفه `UpdatePolicyTest`: كان الاختبار يفترض أن المجهول يُسقط
        // في مسار التحديث، وهو استنتاج من رقمٍ مفقود (نفس علّة ADR-07: لا تيليمتري مُصنَّع).
        if (localCode <= 0L) {
            return UpdateDecision(
                verdict = UpdateVerdict.NONE,
                localCode = localCode,
                release = null,
                floorCode = floor,
                blocking = false,
                message = emptyMap(),
            )
        }

        if (floor > 0L && localCode < floor) {
            return UpdateDecision(
                verdict = UpdateVerdict.BLOCKED,
                localCode = localCode,
                release = release,
                floorCode = floor,
                blocking = true,
                message = manifest.minimum?.notes ?: emptyMap(),
            )
        }

        if (release != null && hasNewer(localCode, release)) {
            // ★ التخطّي يُسجَّل **لكل إصدار** لا لكل مرة: من أغلق 2.0 لا يُزعج بها ثانيةً،
            // لكنه يرى 2.1 فورًا — **والإلزامي لا يُسقطه تخطٍّ أبدًا**. والقاعدة الأخيرة
            // كشفها `UpdatePolicyTest` بعد أن كانت النيّة مكتوبة في التعليق وغير مُنفَّذة
            // في المنطق: كان `force` يُسقطه التخطّي لأن الفرعين يشتركان في بوابة واحدة.
            val skipped = skippedCode == release.code
            if (release.force || !skipped) {
                return UpdateDecision(
                    verdict = if (release.force) UpdateVerdict.REQUIRED else UpdateVerdict.OPTIONAL,
                    localCode = localCode,
                    release = release,
                    floorCode = floor,
                    blocking = false,
                    message = release.notes,
                )
            }
        }

        return UpdateDecision(
            verdict = UpdateVerdict.NONE,
            localCode = localCode,
            release = release,
            floorCode = floor,
            blocking = false,
            message = emptyMap(),
        )
    }

    private fun hasNewer(localCode: Long, release: UpdateRelease?): Boolean =
        release != null && release.code > localCode
}
