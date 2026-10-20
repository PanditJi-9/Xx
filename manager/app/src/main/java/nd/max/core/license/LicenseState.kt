/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ما يُخزَّن على الجهاز: **الغلاف الموقّع أولًا**، والأرقام المخزَّنة نسخة عرضٍ سريعة يُعاد
 * اشتقاقها من الغلاف في كل إقلاع. فمن عدّل ملفّ الذاكرة بجذر يعدّل مرآةً لا مصدرًا: البايتات
 * الموقّعة هي التي تُقرأ ([LicenseCertificates.admit])، وما دونها يُهمَل ويُعاد بناؤه.
 */
@Serializable
data class LicenseRecord(
    val format: Int = FORMAT,
    val licenseId: String = "",
    val device: String = "",
    val plan: String = "",
    val issuedAtMillis: Long = 0,
    val serverMillis: Long = 0,
    val startsAtMillis: Long = 0,
    val expiresAtMillis: Long = 0,
    val pausedMillis: Long = 0,
    val deviceLimit: Int = 0,
    val devicesUsed: Int = 0,
    val certificate: LicenseEnvelope = LicenseEnvelope(),
    @SerialName("stored_at") val storedAtMillis: Long = 0,
    @SerialName("last_attempt") val lastAttemptMillis: Long = 0,
    @SerialName("last_code") val lastOutcome: String = "",
) {
    companion object {
        /** صيغة الملفّ المحلي. تُرفع عند تغيّر الشكل، وما دونها يُعامل كغياب لا كخطأ. */
        const val FORMAT = 1
    }
}

/** ثلاثة أحكام لا رابع: لا مفتاح · مفتاح سارٍ · مفتاح انتهى (ويُجدَّد أو يُشترى). */
enum class LicenseStatus {
    NONE,
    ACTIVE,
    EXPIRED,
}

/** عرض الحالة المحلول للمستخدم والواجهة — أرقامٌ صريحة، ولا قرار عرضٍ مخفيّ. */
data class LicenseState(
    val status: LicenseStatus = LicenseStatus.NONE,
    val plan: String = "",
    val licenseId: String = "",
    val expiresAtMillis: Long? = null,
    val remainingMillis: Long? = null,
    val pausedMillis: Long = 0,
    val deviceLimit: Int = 0,
    val devicesUsed: Int = 0,
    val serverMillis: Long = 0,
) {
    val active: Boolean get() = status == LicenseStatus.ACTIVE
}

object LicenseStates {

    /**
     * «الآن الموثوق» = ساعة الجهاز أو **آخر زمن شهده الخادم**، أيهما أبعد.
     *
     * وهو مانع رجوع الساعة: من أرجع ساعة هاتفه ليعيد الحياة إلى مفتاح منتهٍ لا يتحرّك حكمُه
     * خطوةً واحدة — لأن الزمن المرجعي محفوظٌ من ردّ الخادم الموقّع. ومن قدّم الساعة إلى الأمام
     * يُنهي مفتاحه بيده (ضررٌ على نفسه لا مكسب)، وهو أسوأ ما يفعله هذا المسار — وهذا صواب.
     */
    fun trustedNow(record: LicenseRecord?, nowMillis: Long): Long =
        maxOf(nowMillis, record?.serverMillis ?: 0L)

    /** الحكم: انتهى المفتاح حين يبلغ الزمن الموثوق حدّه (والحدّ مُوقّع من الخادم). */
    fun evaluate(record: LicenseRecord?, nowMillis: Long): LicenseState {
        if (record == null || record.licenseId.isBlank() || record.certificate.payload.isBlank()) {
            return LicenseState()
        }
        val now = trustedNow(record, nowMillis)
        val expired = now >= record.expiresAtMillis
        return LicenseState(
            status = if (expired) LicenseStatus.EXPIRED else LicenseStatus.ACTIVE,
            plan = record.plan,
            licenseId = record.licenseId,
            expiresAtMillis = record.expiresAtMillis,
            remainingMillis = (record.expiresAtMillis - now).coerceAtLeast(0L),
            pausedMillis = record.pausedMillis,
            deviceLimit = record.deviceLimit,
            devicesUsed = record.devicesUsed,
            serverMillis = record.serverMillis,
        )
    }

    /**
     * بناء سجلّ من شهادة مقبولة — **والأرقام كلها من الحمولة الموقّعة**، ولا شيء من الطلب.
     * وتبقى آخر محاولة ونتيجتها من السجلّ السابق إن وُجد (أثرٌ تشخيصيّ لا يُمسح مع كل تجديد).
     */
    fun recordOf(
        payload: LicensePayload,
        envelope: LicenseEnvelope,
        storedAtMillis: Long,
        previous: LicenseRecord? = null,
    ): LicenseRecord = LicenseRecord(
        format = LicenseRecord.FORMAT,
        licenseId = payload.licenseId,
        device = payload.device,
        plan = payload.plan,
        issuedAtMillis = LicenseTime.instantMillis(payload.issuedAt) ?: 0L,
        serverMillis = maxOf(
            LicenseTime.instantMillis(payload.serverTime) ?: 0L,
            LicenseTime.instantMillis(payload.issuedAt) ?: 0L,
        ),
        startsAtMillis = LicenseTime.instantMillis(payload.startsAt) ?: 0L,
        expiresAtMillis = LicenseTime.instantMillis(payload.expiresAt) ?: 0L,
        pausedMillis = payload.pausedMs.coerceAtLeast(0L),
        deviceLimit = payload.deviceLimit.coerceAtLeast(0),
        devicesUsed = payload.devicesUsed.coerceAtLeast(0),
        certificate = envelope,
        storedAtMillis = storedAtMillis,
        lastAttemptMillis = previous?.lastAttemptMillis ?: 0L,
        lastOutcome = previous?.lastOutcome ?: "",
    )

    /**
     * تثبيت مرساة الزمن من ردّ الخادم **دون إرجاعها للوراء أبدًا**.
     *
     * ويُستعمل عند القبول وعند الرفض معًا، وهذا هو بيت القصيد: ردُّ الرفض يحمل `server_time`
     * أيضًا، فجهازٌ انتهى مفتاحه ثم رجع ساعته لا يُنقذه الرجوع بعد أول اتصال — لأن المرساة صارت
     * بعد لحظة الانتهاء. أما جهازٌ **لم يتصل أبدًا** بعد الانتهاء فحدُّه الطبيعي هو ما بُني في
     * [trustedNow]: لا يتحرّك الزمن الموثوق خطوةً إلى الوراء، ويبقى مدى المراوغة محصورًا بين آخر
     * اتصال ولحظة الانتهاء — وهو ما يوصفه `docs/ai/LICENSE-KEYS-01-spec.md` §5 بلا مبالغة.
     */
    fun anchored(record: LicenseRecord, serverMillis: Long, nowMillis: Long): LicenseRecord =
        record.copy(
            serverMillis = maxOf(record.serverMillis, serverMillis),
            lastAttemptMillis = nowMillis,
        )

    /** أيامٌ متبقية بترقيم البشر (يومٌ يتبقّى = «يوم» لا صفر). */
    fun remainingDays(state: LicenseState): Long? =
        state.remainingMillis?.let { (it + DAY - 1) / DAY }
}

/** ثابت يومٍ واحد بالميلي ثانية — نسخة محلية صغيرة كي لا تعتمد الحزمة على غيرها. */
internal const val DAY = 24L * 60 * 60 * 1000
