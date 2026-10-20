/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import nd.max.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * قياس ختم أصول الملكية على **مخرَج الخاتم نفسه** — لا على وصفه.
 *
 * ## ولماذا هذا القياس هو المهمّ دون غيره
 *
 * الخاتم في `build.gradle.kts` ([SealAssetsTask]) والفكّ في [AssetVault] **كودان مستقلّان
 * بمُحرّرين مختلفين**، والاتفاق بينهما (ترتيب الرأس، طول الملح، اشتقاق المفتاح، صيغة
 * القائمة) **عقد غير مكتوب في أي موضع**. وكل انزياح فيهما يُنتج إمّا تطبيقًا لا يقرأ
 * كتالوج الأجهزة أصلًا، وإمّا — وهو الأسوأ — ختمًا لا معنى له. فالقياس هنا يمرّ على
 * الطريقين معًا: تُقرأ البايتات المختومة، وتُفكّ بالبصمة المشتقّة من مخزن المفاتيح،
 * وتُقابَل **بالملفّ الأصلي** بايتًا ببايت.
 *
 * ## ووجهاه
 *
 * 1. **الاستخراج السهل يسقط:** الأصل لا يُشحن كنصّ JSON عاديّ — وهو الادّعاء الذي بُني
 *    عليه الملفّ كلّه (`unzip` واحد = قاعدة الأجهزة كاملة).
 * 2. **والفكّ يعود إلى الأصل:** البايتات المفكوكة **تساوي** الملفّ الأصلي، وقائمة الختوم
 *    تطابق بصمات محتواها.
 *
 * ## وحدّه المعلن
 *
 * يحتاج ثلاثة أشياء معًا: مخرَج خاتم مُولَّدًا، وبصمة مشتقّة ([BuildConfig.SEALED_ASSETS]
 * وحدها لا تكفي)، وحزمة مبنيّة للأصول المقاسة داخل الحزمة. وكلّ ما غاب منها **يُعلن
 * التخطّي** بـ`assumeTrue` ولا يُقال «مرّ» — لأن قياسًا لم يُجرَ ليس نجاحًا.
 */
class SealedAssetsTest {

    /**
     * جذر الأصول. والمسارات مرشّحة لأن الاختبار يُشغَّل من مجلّد الوحدة في Gradle
     * (`manager/app`) ومن جذر المستودع في `tools/test_security_jvm.py` — ومسار واحد
     * جامد يجعل القياس «تخطّيًا» في أحد الاثنين بلا سبب.
     */
    private val assetRoot = listOf(
        "src/main/assets-internal",
        "manager/app/src/main/assets-internal",
    ).map(::File).firstOrNull { it.isDirectory } ?: File("src/main/assets-internal")

    /** الأصول المشحونة — والأسماء هي المفاتيح التي تقرأ بها الشاشات، فلا تُغيَّر هنا. */
    private val sealedEntries = listOf("socs.json", "spoof/device_catalog.json", "spoof/cpu_catalog.json")

    /** مخرَج [SealAssetsTask]: يكتبه AGP في مجلّد مُشتقّ من اسم المهمّة. */
    private fun sealerOutput(): File? = listOf(
        "build/generated/assets/sealReleaseCreativeAssets",
        "app/build/generated/assets/sealReleaseCreativeAssets",
        "manager/app/build/generated/assets/sealReleaseCreativeAssets",
    ).map(::File).firstOrNull { it.isDirectory }

    private fun builtApk(): File? = listOf(
        "build/outputs/apk/release/app-release.apk",
        "app/build/outputs/apk/release/app-release.apk",
        "manager/app/build/outputs/apk/release/app-release.apk",
    ).map(::File).firstOrNull { it.isFile }

    private fun pinIsConfigured(): Boolean {
        assumeTrue("الأصول لا تُختم بلا بصمة مشتقّة (KS_PWD) ⇒ القياس غير مُتحقَّق", BuildConfig.SEALED_ASSETS)
        assumeTrue("البصمة لم تُشتقّ في هذا البناء ⇒ القياس غير مُتحقَّق", SecurityPin.expected != null)
        return true
    }

    /** الوجه الأول: ما يكتبه الخاتم **ليس** الأصل صريحًا، وبنية رأسه كما في المواصفة. */
    @Test
    fun `the sealer writes a vault header, not the plain json`() {
        val output = sealerOutput()
        assumeTrue("لا مخرَج خاتم في الشجرة ⇒ القياس غير مُتحقَّق في هذه البيئة", output != null)
        sealedEntries.forEach { name ->
            val sealed = File(output!!, name)
            assumeTrue("الأصل '$name' غير مختوم في هذه الشجرة", sealed.isFile)
            val bytes = sealed.readBytes()
            assertTrue("رأس الخزانة غائب عن '$name'", startsWithVaultMagic(bytes))
            val plain = File(assetRoot, name)
            assertTrue("الأصل المصدر '$name' غائب", plain.isFile)
            assertEquals(
                "حجم '$name' المختوم = الأصل + ٥٢ بايت بالضبط (٨ سحر · ١٦ ملح · ١٢ نونس · ١٦ وسم)",
                plain.length() + VAULT_HEADER_AND_TAG,
                sealed.length(),
            )
        }
    }

    /** الوجه الثاني: كل أصل مختوم يُفكّ إلى الملفّ الأصلي **بايتًا ببايت**، وبصمته تطابق القائمة. */
    @Test
    fun `every sealed asset decrypts back to its source and matches its manifest digest`() {
        val output = sealerOutput()
        assumeTrue("لا مخرَج خاتم في الشجرة ⇒ القياس غير مُتحقَّق في هذه البيئة", output != null)
        pinIsConfigured()
        val manifest = File(output!!, AssetVault.MANIFEST_ASSET)
        assumeTrue("لا قائمة ختوم في هذه الشجرة", manifest.isFile)
        val entries = AssetVault.parseManifest(manifest.readText())
        assertEquals("عدد الأصول في القائمة يجب أن يطابق الأصول المشحونة", sealedEntries.size, entries.size)
        // سطر قياس واحد: يجعل «قِيس» و«تُخطّي» قابلين للتمييز من مخرَج أي مُشغّل، بدل أن
        // يمرّ التخطّي في صمت ويُقرأ «٢٩ اختبارًا نجحت» وهو لم يقِس شيئًا.
        println("MEASURED sealed round-trip: " + entries.size + " assets from " + output.path)
        entries.forEach { (name, expectedDigest) ->
            val sealed = File(output, name).readBytes()
            assertEquals(
                "بصمة الأصل المختوم '$name' لا تطابق قائمة الختوم",
                expectedDigest,
                IntegrityDigest.encode(IntegrityDigest.sha256Of(sealed)),
            )
            val plain = AssetVault.unseal(sealed)
            assertTrue("فكّ ختم '$name' فشل ⇒ الخاتم والفكّ لا يتّفقان", plain != null)
            assertEquals(
                "المفكوك من '$name' لا يساوي الملفّ الأصلي",
                File(assetRoot, name).readBytes().toList(),
                plain!!.toList(),
            )
        }
    }

    /** وقياس ثالث حين تُبنى حزمة: ما يُشحن داخلها هو المختوم لا الصريح. */
    @Test
    fun `the shipped apk carries the sealed bytes, not readable json`() {
        val apk = builtApk()
        assumeTrue("لا حزمة إصدار مبنيّة ⇒ القياس غير مُتحقَّق في هذه البيئة", apk != null)
        ZipFile(apk!!).use { zip ->
            sealedEntries.forEach { name ->
                val bytes = zip.getEntry(name)?.let { zip.getInputStream(it).use { s -> s.readBytes() } }
                assumeTrue("الأصل '$name' غير موجود في الحزمة", bytes != null)
                assertTrue("الأصل '$name' يُقرأ في الحزمة نصًّا عاديًّا ⇒ الختم ساقط", startsWithVaultMagic(bytes!!))
            }
        }
    }

    private fun startsWithVaultMagic(bytes: ByteArray): Boolean {
        val magic = "MMVAULT1"
        if (bytes.size < magic.length) return false
        return magic.indices.all { bytes[it] == magic[it].code.toByte() }
    }

    private companion object {
        /** ٨ سحر + ١٦ ملح + ١٢ نونس + ١٦ وسم GCM. */
        const val VAULT_HEADER_AND_TAG = 52
    }
}
