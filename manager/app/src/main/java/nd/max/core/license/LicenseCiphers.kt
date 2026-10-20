/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import nd.max.core.security.SecurityPin
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * اختيار طبقة تشفير ذاكرة الترخيص — **ثلاث طبقات بترتيب مُعلن**، وكلٌّ تُسمّي نفسها في السجلّ:
 *
 * | الطبقة | متى | ماذا تقاوم فعلًا |
 * | --- | --- | --- |
 * | `keystore` — مفتاح `AndroidKeyStore` غير قابل للتصدير | الجهاز الطبيعيّ | **نسخ ملفّ الترخيص إلى جهاز آخر**: بايتاته لا تُفكّ إلّا بمفتاح الجهاز الأول |
 * | `pin` — مفتاح مشتقّ من بصمة موقّع الحزمة | مخزن مفاتيح النظام تعذّر (نادر) | تشغيل الترخيص داخل **حزمة مُعاد توقيعها**: بصمة الموقّع مختلفة ⇒ بايتات لا تُفكّ |
 * | `none` — بلا تشفير (بناء بلا بصمة موقّع أصلًا) | debug/بناء بلا مفتاح | لا شيء — **ويُعلن باسمه**، وهذا فرقُ صدقٍ لا نقصُ تنفيذ |
 *
 * ولماذا لا تُبنى الطبقة الثالثة على «الملفّ غامض»: الغموض ليس تشفيرًا، ومن يقرأ هذا السجلّ يجب
 * أن يعرف أن ترخيصه غير مشفّر بدل أن يُترك يظنّ العكس.
 */
object LicenseCiphers {

    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    /** الطبقة الأولى المتاحة فعلًا على هذا الجهاز — والاختبار بعد كل محاولة لا قبلها. */
    fun primary(): LicenseCipher =
        KeystoreCipher().takeIf { it.available() }
            ?: PinCipher().takeIf { it.available() }
            ?: IdentityCipher

    /** ترويسة الناتج: ١٢ بايت IV ثم النصّ المشفّر (وGCM يختم السلامة في آخره). */
    private fun sealWith(key: SecretKey, plain: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        require(iv.size == IV_BYTES) { "unexpected IV size ${iv.size}" }
        iv + cipher.doFinal(plain)
    }.getOrNull()

    private fun openWith(key: SecretKey, sealed: ByteArray): ByteArray? = runCatching {
        if (sealed.size <= IV_BYTES) return null
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, sealed, 0, IV_BYTES))
        cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }.getOrNull()

    /** الطبقة الأولى: مفتاح AES-256 يُنشئه نظام المفاتيح ويبقى فيه ولا يُصدَّر. */
    private class KeystoreCipher : LicenseCipher {

        override val id: String = "keystore"

        private var cached: SecretKey? = null

        private fun key(): SecretKey? {
            cached?.let { return it }
            val key = runCatching {
                val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            }.getOrNull() ?: runCatching {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generator.generateKey()
            }.getOrNull()
            cached = key
            return key
        }

        fun available(): Boolean = key() != null

        override fun seal(plain: ByteArray): ByteArray? = key()?.let { sealWith(it, plain) }

        override fun open(sealed: ByteArray): ByteArray? = key()?.let { openWith(it, sealed) }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val ALIAS = "maxmanager_license_v1"
        }
    }

    /**
     * الطبقة الثانية: مفتاح يُشتقّ من بصمة موقّع الحزمة نفسها ([SecurityPin]) + ملح ثابت.
     * وبصمة الموقّع **ليست سرًّا** — لكن اشتقاقًا منها يجعل حزمةً معادة التوقيع لا تفكّ شيئًا.
     */
    private class PinCipher : LicenseCipher {

        override val id: String = "pin"

        private val key: SecretKey? by lazy {
            val pin = SecurityPin.expected ?: return@lazy null
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(pin)
            digest.update(SALT.toByteArray(Charsets.US_ASCII))
            SecretKeySpec(digest.digest(), "AES")
        }

        fun available(): Boolean = key != null

        override fun seal(plain: ByteArray): ByteArray? = key?.let { sealWith(it, plain) }

        override fun open(sealed: ByteArray): ByteArray? = key?.let { openWith(it, sealed) }

        private companion object {
            const val SALT = "maxmanager-license-v1"
        }
    }

    /** الطبقة الأخيرة: لا تشفير — واسمها يقول ذلك. */
    private object IdentityCipher : LicenseCipher {
        override val id: String = "none"
        override fun seal(plain: ByteArray): ByteArray = plain
        override fun open(sealed: ByteArray): ByteArray = sealed
    }
}
