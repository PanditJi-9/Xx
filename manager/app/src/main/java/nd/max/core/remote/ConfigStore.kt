/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * عمر الإعداد المخزَّن — والأربعة كلها **تُعلن**، ولا واحدة منها تُقرأ كأخرى.
 *
 * والمهمّ هنا [UNMEASURABLE]: إذا رجعت ساعة الجهاز للوراء (`now < storedAt`) فالعمر **ليس صفرًا
 * بل غير قابل للقياس**، ويُعلن كذلك بدل أن يُقال «طازج». (نمط `AtlasFreshness` نفسه: المجهول
 * يُعلن مجهولًا ولا يُشبه المعروف — ADR-07.)
 */
enum class ConfigFreshness {
    /** العمر معروف وداخل الـTTL. */
    FRESH,

    /** العمر معروف وتجاوز الـTTL: الإعداد صالح لكنه قديم، ويُعلن عمره. */
    STALE,

    /** ساعة الجهاز رجعت للوراء: لا عمر يُقاس. */
    UNMEASURABLE,

    /** لا إعداد مخزَّن أصلًا (تركيب جديد، أو مسح بيانات). */
    ABSENT,
}

/**
 * ما في الذاكرة المحلية: الغلاف **كما وصل** (يُعاد التحقّق من توقيعه في كل قراءة)، ووقت الحفظ،
 * وآخر محاولة ونتيجتها.
 *
 * ولماذا يُخزَّن الغلاف لا الحمولة وحدها: §٥.١ تنصّ أن «تبديل ملفّ محلي» يجب أن يُمنع، والمنع
 * لا يقع إن كانت الذاكرة كائنًا محلولًا. فيُخزَّن **ما وُقّع** ويُتحقَّق منه مرّة أخرى عند كل
 * إقلاع — فذاكرة مفسودة (أو معدَّلة بجذر) لا تصير إعدادًا مطبَّقًا.
 */
data class StoredConfig(
    val envelope: SignedEnvelope,
    val storedAtMillis: Long,
    val lastAttemptMillis: Long,
    val lastOutcome: String,
    /**
     * وسم `ETag` كما أعاده الخادم — يُرسل في الطلب التالي `If-None-Match`، فإعدادٌ لم يتغيّر
     * يكلّف **304 بلا جسم**: صفر egress. وغيابه (بناء قديم لا يخزّنه) يعني طلبًا كاملًا — لا عطبًا.
     */
    val etag: String = "",
) {
    /** العمر بالميلي ثانية، أو `null` إذا كانت الساعة قد رجعت للوراء. */
    fun ageMillis(nowMillis: Long): Long? = (nowMillis - storedAtMillis).takeIf { it >= 0L }

    fun freshness(nowMillis: Long, ttlHours: Int): ConfigFreshness {
        val age = ageMillis(nowMillis) ?: return ConfigFreshness.UNMEASURABLE
        return if (age <= ttlHours.coerceIn(1, 24) * 3_600_000L) {
            ConfigFreshness.FRESH
        } else {
            ConfigFreshness.STALE
        }
    }
}

/**
 * الذاكرة المحلية على ملفّ واحد — بلا أندرويد (‏`java.io.File` وحده) ليقاس سلوكها كاملًا على JVM:
 * التلف، والقطع، والكتابة الذرّية، ورجوع الساعة. وهي بعينها الحالات ٥ · ٦ · ١٢ من §٥.٤.
 *
 * ## لماذا ملفّ لا `SharedPreferences`
 *
 * ثلاثة أسباب، أوّلها مُقاس: ① **الكتابة الذرّية** المطلوبة في §٩ تُنجز بـ«اكتب المؤقّت ثم
 * `renameTo`» — وهذا غير قابل للتنفيذ على تفضيلات أندرويد من خارجها؛ ② سقوط الجهد في منتصفه
 * (بطارية، قتل عملية) يجب أن يترك **الملفّ القديم صحيحًا** لا ملفًّا نصفه JSON؛ ③ ومع `File`
 * تصير كل الحالات أعلاه دعاوى في `ConfigStoreTest` بلا جهاز ولا محاكاة أندرويد.
 *
 * ## والقواعد الأربع
 *
 * 1. **التلف = الغياب**: ملفّ لا يُقرأ أو بصيغة قديمة ⇒ `null`، فيُعاد الطلب من الخادم، والافتراضي
 *    المحلي هو الحكم مؤقّتًا. ولا «إصلاح جزئيّ» لملفّ مكسور.
 * 2. **الكتابة ذرّية**: مؤقّتٌ في نفس المجلّد ثم `renameTo` (نفس نظام الملفّات ⇒ إعادة تسمية ذرّية).
 * 3. **الفشل لا يُسقط شيئًا**: إن تعذّرت الكتابة، الملفّ السابق يبقى كما هو، والنتيجة `false`
 *    لتُسجَّل — ولا تُبتلع.
 * 4. **لا حذف إلا بطلب**: [clear] تُنادى عند «سحب الموافقة» أو خروج المالك، لا في مسار عاديّ.
 */
class ConfigStore(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** الغلاف المخزَّن، أو `null` إن لم يوجد شيء صالح — والتلف يُعامل كغياب. */
    fun read(): StoredConfig? {
        if (!file.isFile) return null
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        val document = runCatching { json.decodeFromString<Document>(text) }.getOrNull() ?: return null
        if (document.format != FORMAT) return null
        if (document.payload.isBlank() || document.signature.isBlank()) return null
        return StoredConfig(
            envelope = SignedEnvelope(
                schema = document.schema,
                revision = document.revision,
                payload = document.payload,
                signature = document.signature,
            ),
            storedAtMillis = document.storedAt,
            lastAttemptMillis = document.lastAttempt,
            lastOutcome = document.lastCode,
            etag = document.etag,
        )
    }

    /** كتابة ذرّية. `false` = لم تُكتب، وللملفّ السابق أن يبقى. */
    fun write(stored: StoredConfig): Boolean {
        val body = json.encodeToString(
            Document(
                format = FORMAT,
                schema = stored.envelope.schema,
                revision = stored.envelope.revision,
                payload = stored.envelope.payload,
                signature = stored.envelope.signature,
                storedAt = stored.storedAtMillis,
                lastAttempt = stored.lastAttemptMillis,
                lastCode = stored.lastOutcome,
                etag = stored.etag,
            ),
        )
        val temporary = File(file.parentFile, "${file.name}.tmp")
        return runCatching {
            file.parentFile?.mkdirs()
            temporary.writeText(body, Charsets.UTF_8)
            // `renameTo` في نفس المجلّد: استبدال ذرّيّ، والقارئ لا يرى ملفًّا نصفه مكتوب.
            if (temporary.renameTo(file)) return@runCatching true
            // بعض أنظمة الملفّات ترفض إعادة التسمية على موجود: يُحذف الهدف ويُعاد المحاولة،
            // والفشل النهائيّ يُعلن `false` (لا يُبتلع).
            if (!file.delete()) return@runCatching false
            temporary.renameTo(file)
        }.getOrDefault(false)
    }

    /**
     * يسجّل محاولةً لم تُقبل (شبكة · توقيع · صيغة) **دون أن يمسح الغلاف القديم**: الإعداد الأخير
     * الصالح يبقى هو الحكم، ولذلك تُنفصل المحاولة عن الحفظ. وغياب ملفّ سابق ⇒ لا كتابة (لا إشارة
     * لمحاولةٍ في ذاكرة فارغة).
     */
    fun noteAttempt(nowMillis: Long, outcome: String): Boolean {
        val current = read() ?: return false
        return write(current.copy(lastAttemptMillis = nowMillis, lastOutcome = outcome))
    }

    /** حذف الذاكرة — عند سحب الموافقة أو الخروج. وغياب الملفّ نجاحٌ لا عطب. */
    fun clear(): Boolean = !file.exists() || file.delete()

    @Serializable
    private data class Document(
        val format: Int = FORMAT,
        val schema: Int = 0,
        val revision: Long = 0,
        val payload: String = "",
        val signature: String = "",
        @SerialName("stored_at") val storedAt: Long = 0,
        @SerialName("last_attempt") val lastAttempt: Long = 0,
        @SerialName("last_code") val lastCode: String = "",
        val etag: String = "",
    )

    companion object {
        /** صيغة الملفّ المحلي. تُرفع عند تغيّر الشكل، وما دونها يُعامل كغياب لا كخطأ. */
        const val FORMAT = 1

        /** اسم الملفّ داخل `filesDir` — مسارٌ واحد يُكتب في موضعٍ واحد. */
        const val FILE_NAME = "remote-config.json"
    }
}
