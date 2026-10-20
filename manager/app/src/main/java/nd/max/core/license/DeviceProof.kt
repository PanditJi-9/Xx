/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * مفتاح الجهاز الحقيقيّ: زوج RSA-2048 يُولّده Android Keystore، والخاصّ منه **غير قابل
 * للتصدير** (StrongBox إن توفّر، وإلّا TEE/برنامج). ومعرّف الجهاز = أوّل 32 محرفًا من
 * SHA-256 للمفتاح العامّ (SPKI) — رقم شبه عشوائي لا يكشف أي معرّف عتاد، لكنه **لا يُقلَّد**
 * بلا المفتاح الخاصّ.
 *
 * ## لماذا توقيع لا معرّف فقط
 *
 * لو أرسل التطبيق المعرّف وحده لكان نسخ ملفٍّ صغيرًا كافيًا لتقليد التثبيت. فكل نداء ترخيص
 * يحمل توقيعًا على نصّ قياسيّ يحوي الوجهة والموضوع والزمن، والخادم يتحقّق من التوقيع بمفتاح
 * الجهاز العامّ المحفوظ. نسخ المعرّف بلا المفتاح الخاصّ لا ينتج توقيعًا صالحًا.
 *
 * ## الحدّ الصادق
 *
 * إعادة تثبيت نظيفة أو مسح بيانات التطبيق يحذف مفتاح Keystore (سلوك أندرويد)، فتصير هوية
 * جديدة تحتاج مقعدًا — تمامًا كالسلوك السابق. وعلى جهاز مروّت بالكامل لا توجد ضمانة مطلقة،
 * لكن هذا أقوى من IMEI (القابل للتزوير) ومن ملفّ عشوائي (القابل للنسخ)، ولا يكشف معرّفًا
 * يعرفه أحد خارج التطبيق.
 */
internal class DeviceProof(
    val publicKeyBase64: String,
    private val privateKey: PrivateKey,
) {
    fun sign(payload: String): String? = runCatching {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(payload.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(signer.sign())
    }.getOrNull()

    companion object {
        private const val ALIAS = "maxmanager.license.device.v2"

        fun load(): LoadedProof? = runCatching {
            val store = keystore() ?: return null
            val entry = (store.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry)
                ?: generate(store)
                ?: return null
            val certificate = entry.certificate as X509Certificate
            val spki = certificate.publicKey.encoded ?: return null
            val digest = MessageDigest.getInstance("SHA-256").digest(spki)
            val id = digest.take(16).joinToString("") { "%02x".format(it) }
            val publicKey = Base64.getEncoder().encodeToString(spki)
            LoadedProof(id, DeviceProof(publicKey, entry.privateKey))
        }.getOrNull()

        private fun keystore(): KeyStore? = runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }.getOrNull()

        private fun generate(store: KeyStore): KeyStore.PrivateKeyEntry? {
            val strongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            if (strongBox && create(strongBox = true)) {
                return store.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
            }
            deleteAlias()
            if (create(strongBox = false)) {
                return store.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
            }
            return null
        }

        private fun create(strongBox: Boolean): Boolean = runCatching {
            val builder = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(false)
            if (strongBox) {
                runCatching { builder.setIsStrongBoxBacked(true) }
            }
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                "AndroidKeyStore",
            )
            generator.initialize(builder.build())
            generator.generateKeyPair()
        }.isSuccess

        private fun deleteAlias() {
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS)
            }
        }
    }
}

internal data class LoadedProof(val id: String, val proof: DeviceProof)

/** طبقة صغيرة قابلة للاستبدال في الاختبار: `null` تعني «Keystore غير متاح» بلا استثناء. */
internal object DeviceProofs {
    fun load(): LoadedProof? = runCatching { DeviceProof.load() }.getOrNull()
}

/** النصّ القياسيّ الذي يوقّعه التطبيق ويتحقّق منه الخادم — نسخة واحدة لا تنزاح. */
object LicenseProof {
    const val PREFIX = "MAXMANAGER-LICENSE-V1"

    fun payload(action: String, device: String, subject: String, atMillis: Long): String =
        listOf(PREFIX, action, device, subject, atMillis.toString()).joinToString("\n")
}
