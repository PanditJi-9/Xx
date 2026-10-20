/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import android.content.Context
import nd.max.BuildConfig
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * خزانة الأصول الفارقة: كتالوجات الأجهزة والمعالجات وشريحة SoC — **ما يجعل MaxManager
 * MaxManager**، وما يُسرَق أوّلًا.
 *
 * ## ما كان يحدث قبل هذا الملفّ — بالقياس لا بالتقدير
 *
 * ```sh
 * unzip -p app-release.apk assets/spoof/device_catalog.json     # يُطبع نصًّا مقروءًا
 * unzip -p app-release.apk assets/socs.json                     # ويُطبع ألف مفتاح
 * ```
 *
 * أي أنّ كلفة أخذ قاعدة الأجهزة كاملة منافسًا كانت **سطرًا واحدًا**، لا هندسة عكسيّة.
 * والأصول بطبيعتها لا تُصرَّف، فلا يُساعد فيها R8 ولا التقليص ولا أي إعادة تسمية: تبقى
 * بايتات صريحة في ملفّ مضغوط.
 *
 * ## ولماذا **ختمٌ بمفتاح البناء** لا مفتاح مكتوب
 *
 * المفتاح مشتقّ من **بصمة موقّع الحزمة** ([SecurityPin]) مع ملح لكل ملفّ. ولهذا الاشتقاق
 * ثلاث نتائج مقيسة:
 *
 * 1. لا سرّ يُكتب في المستودع ولا في الكود: البصمة موجودة أصلًا في كل حزمة موقّعة.
 * 2. من يعيد توقيع الحزمة بمفتاحه **لا يستطيع فكّ أصولنا** — مفتاحه هو غير مفتاحنا،
 *    فيحصل على بايتات مشوّشة لا على قاعدة الأجهزة. أي أنّ إعادة التغليف تُثمر صفرًا.
 * 3. وdebug وrelease يُختمان بمفتاحين مختلفين تلقائيًّا، فلا ينزاح أحدهما عن الآخر يدويًّا.
 *
 * ## وحدودها المعلنة — بلا تهويل
 *
 * الختم هنا **يعطيل الاستخراج السهل، ولا يمنع استخراجًا متعمّدًا**: من يشغّل التطبيق
 * ويُنفّذ مسار الفكّ يستطيع أن يقرأ الناتج عند نقطة الوصول، ومن يفكّ المكتبة/DEX يجد
 * الاشتقاق. فالقيمة الحقيقيّة: **إسقاط كلفة النسخ من «unzip» إلى «هندسة عكسيّة»**، وهذا
 * هو أقصى ما يعطيه أي ختم محليّ. وليس ادّعاء منعٍ.
 */
object AssetVault {

    /** السحر: تمييز ملفّ مختوم من ملفّ نصّي يعود `null` بدل أن يُفسَّر خطأً. */
    private const val MAGIC = "MMVAULT1"

    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BYTES = 16

    /** اسم قائمة الختوم داخل الأصول. */
    const val MANIFEST_ASSET = "sealed-manifest.txt"

    /**
     * هل الحزمة تحمل أصولًا مختومة؟ تُقرأ من `BuildConfig` المُولَّد زمن البناء.
     * و`false` يعني أصولًا صريحة (بناء debug) — والخزانة تعمل عندها كمرور مباشر.
     */
    val sealed: Boolean get() = BuildConfig.SEALED_ASSETS

    /**
     * يقرأ أصلًا نصيًّا — مختومًا أو صريحًا.
     *
     * والعائد `null` يعني «لا نصّ»، ويُفسّره المتصل كما كان يُفسّر `IOException` قبله:
     * الشاشة تقول سببها ولا تسقط. **ولا يوجد سقوط صامت إلى نسخة صريحة:** لو بقيت نسخة صريحة
     * في الحزمة لسقط الختم كلّه من أجلها.
     */
    fun readText(context: Context, name: String): String? =
        openBytes(context, name)?.toString(Charsets.UTF_8)

    /** بايتات أصل، مختومًا كان أو صريحًا. */
    fun openBytes(context: Context, name: String): ByteArray? = runCatching {
        val raw = context.assets.open(name).use { it.readBytes() }
        if (!sealed || !looksSealed(raw)) raw else unseal(raw)
    }.getOrNull()

    /** تدفّق أصل — لمن يقرأ تدريجيًّا (قاعدة أسماء الأجهزة كبيرة). */
    fun openStream(context: Context, name: String): InputStream? =
        openBytes(context, name)?.let(::ByteArrayInputStream)

    /**
     * فكّ الختم: `MAGIC(8) || salt(16) || iv(12) || AES-GCM(...)`.
     *
     * والمفتاح = `SHA-256(pin ‖ salt)` مقصوصًا إلى ١٦ بايت. والملح لكل ملفّ يمنع أن يبدو
     * ملفّان متطابقين متماثلين في الحزمة، ويمنع إعادة استخدام المفتاح عينه بينها.
     *
     * **ولا يحتاج اسم الملفّ**: الملح والنونس مكتوبان في الرأس نفسه ([SealAssetsTask] في
     * `build.gradle.kts` يُشتقهما فيكتبهما)، فالفكّ دالّة على البايتات وحدها — واسمٌ يُمرَّر
     * ولا يُستعمل كان يوهم بأن الترتيب أو المسار عقدٌ إلزاميّ وهو ليس كذلك.
     *
     * و`internal` لنفس سبب [parseManifest]: يُقاس من الاختبار على حزمة مبنيّة فعلًا.
     */
    internal fun unseal(raw: ByteArray): ByteArray? {
        val pin = SecurityPin.expected ?: return null
        val header = MAGIC.length + SALT_BYTES + IV_BYTES
        if (raw.size <= header) return null
        val salt = raw.copyOfRange(MAGIC.length, MAGIC.length + SALT_BYTES)
        val iv = raw.copyOfRange(MAGIC.length + SALT_BYTES, header)
        val ciphertext = raw.copyOfRange(header, raw.size)
        val key = derivedKey(pin, salt) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        // الفشل هنا **لا يُفسَّر خطأً كتلاعب**: قد يكون الملح أو الرأس معطوبين لسبب آخر.
        // والحكم من [verifySealedAssets] وحده.
        return runCatching { cipher.doFinal(ciphertext) }.getOrNull()
    }

    private fun derivedKey(pin: ByteArray, salt: ByteArray): ByteArray? {
        if (pin.isEmpty()) return null
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(pin)
        digest.update(salt)
        return digest.digest().copyOf(KEY_BYTES)
    }

    /**
     * التطابق: `true` سليم · `false` مختوم بختم مختلف · `null` لا شيء يُقاس (بناء غير مختوم).
     *
     * وهذا فحص **مستقلّ عن فحص التوقيع**: من نجح في تعطيل فحص البصمة ثم استبدل أصلًا
     * مختومًا، يبقى مكشوفًا هنا — لأن هذا المسار لا يسأل عن الموقّع شيئًا، بل يقارن
     * بايتات الملفّ بما خُتم عليه زمن البناء.
     */
    fun verifySealedAssets(context: Context): Boolean? {
        if (!sealed) return null
        val manifest = runCatching {
            context.assets.open(MANIFEST_ASSET).use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull() ?: return null
        val entries = parseManifest(manifest)
        if (entries.isEmpty()) return null
        for ((name, expected) in entries) {
            val actual = runCatching {
                context.assets.open(name).use { it.readBytes() }
            }.getOrNull() ?: return false
            if (IntegrityDigest.encode(IntegrityDigest.sha256Of(actual)) != expected) return false
        }
        return true
    }

    /**
     * أسطر `sha256<مسافة>اسم` — والقائمة تُولَّد آليًّا في [SealAssetsTask] فلا يُشتقّ شكلها
     * هنا بالتخمين. والعائد `(اسم, بصمة)`: الترتيب مقصود ليطابق ترتيب الفكّ في
     * [verifySealedAssets] — ومَن عكس الربط هنا فتح ملفًّا اسمه بصمة سداسية.
     */
    internal fun parseManifest(text: String): List<Pair<String, String>> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split(' ', limit = 2)
            // parts[0] بصمة · parts[1] اسم الملفّ (المسار داخل الأصول)
            if (parts.size != 2) null else parts[1].trim() to parts[0].trim().lowercase()
        }
        .filter { (path, digest) -> path.isNotEmpty() && digest.length == 64 && HEX_ONLY.matches(digest) }
        .toList()

    /**
     * البصمة السداسية وحدها مقبولة: صفّ بطول ٦٤ من محارف غير سداسية (نصّ إنجليزيّ عاديّ
     * مثلًا) كان يمرّ من فحص الطول ثم يقارَن ببصمة حقيقية فيَرُد الرفض على أصل سليم.
     * والقائمة تُولَّد آليًّا، فالمقارنة هنا **تحقّق من الشكل لا تفسير للشكل**.
     */
    private val HEX_ONLY = Regex("[0-9a-f]{64}")

    private fun looksSealed(raw: ByteArray): Boolean {
        if (raw.size < MAGIC.length) return false
        for (i in MAGIC.indices) if (raw[i] != MAGIC[i].code.toByte()) return false
        return true
    }
}
