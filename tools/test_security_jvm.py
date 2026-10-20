#!/usr/bin/env python3
"""قياس نواة الأمان (`nd.max.core.security`) على JVM عادية — بلا Gradle وبلا جهاز.

## لماذا أداة مستقلّة — والسبب مُقاس لا مُفترض

بوابة `:app:testReleaseUnitTest` تحتاج خطّ تغليف أندرويد كاملًا (‏`aapt2` من AGP)، وذلك
الخطّ **لا يعمل على مضيف aarch64**: AGP 9.2.0 يُنزّل `aapt2` لبِنيّة x86‑64 وحدها، فيفشل
تشغيله عند `mergeReleaseResources`/`processReleaseResources`. (قِيس هنا: الإصدار الموجود في
`build-tools` لهذه البيئة لبِنيّة aarch64 لكنه `2.19-debian`، أي أقدم من أن يستوعب واجهات
AGP 9). فالنتيجة أن اختبارات المنطق — وهي أصلًا **منطق خالص لا يمسّ Android** — تصير رهينة
عطب بيئة لا علاقة له بها. وهذه الأداة تقطع ذلك: تُصرّف الملفّات وتُشغّل `JUnitCore` مباشرة.

وهي على نمط `tools/test_maxai_jvm.py` نفسه: **لا تنزّل شيئًا**، بل تستعمل ما في مخزن
Gradle من مُصرّف ومكتبات، و`android.jar` من الـSDK المثبّت.

## وما تقيسه بالضبط — ثلاثة مستويات

1. **المنطق الخالص:** السجلّ الثابت، والسياسة، وتشفير/فكّ السداسي، وقارئ كتلة التوقيع.
2. **الحزمة المعدَّلة (محاكاة حقيقية):** تُبنى حزمة APK مصنوعة، وتُوقّع بمخزن مفاتيح
   مُولَّد في هذه اللحظة بالمُوقِّع الحقيقي نفسه (`apksig`)، فتُقبل. ثم يُقلَب **بايت واحد**
   في محتوى مدخل فتُرفض. وهذا هو الادّعاء الوحيد الذي لا يثبته اختبار على بنية مصنوعة:
   أن الكاشف يرى تعديلًا حقيقيًّا في حزمة موقّعة حقيقيًّا.
3. **انحراف المنصّة:** يُقاس الوجهان — الفحص بمنصّة التثبيت، والفحص بمنصّة قديمة تطلب
   توقيع `v1` — ليُعرف أن اختيار المنصّة ليس تفصيلًا صامتًا.

## حدودها المُعلنة

* **ليست بديلًا عن بناء CI:** لا تُنتج APK للتطبيق ولا تُصرّف الموارد. هي **بوابة منطق**.
* ومَخزَن التشخيص (`DiagnosticCenter`) و`R` **مُبدَّلان ببذرة مصغّرة** هنا: الأداة تقيس نواة
  الأمان لا مخزن التشخيص. وتطابق التوقيع مع الإنتاج مضمون بأن `:app:compileReleaseKotlin`
  هو البوابة الحقيقيّة للأنواع (وقد مُرّرت في هذه الجولة).
* وبلا مخزن مفاتيح (`KS_PWD`) تُقاس الحزم المصنوعة بمخزن تولّده الأداة نفسها؛ وأصول الختم
  تبقى **غير مقيسة** وتُعلن كذلك (لا تُدّعى).

## الاستعمال

```sh
python3 tools/test_security_jvm.py                       # المنطق + حزمة مصنوعة موقّعة
KS_PWD=... python3 tools/test_security_jvm.py            # ومعها قياس أصول الختم ببصمة حقيقيّة
python3 tools/test_security_jvm.py --json /tmp/out.json  # تقرير مقروء آليًّا
```
"""

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max"
TEST = ROOT / "manager/app/src/test/java/nd/max"
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"

# الإصدارات مطابقة لـ`manager/gradle/libs.versions.toml` — وقراءتها من المخزن، لا تنزيلها.
JARS = {
    "stdlib": ("org.jetbrains.kotlin", "kotlin-stdlib", "2.3.10"),
    "compiler": ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.3.10"),
    "reflect": ("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
    "annotations": ("org.jetbrains", "annotations", "13.0"),
    "coroutines": ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.2"),
    "junit": ("junit", "junit", "4.13.2"),
    "hamcrest": ("org.hamcrest", "hamcrest-core", "1.3"),
    "apksig": ("com.android.tools.build", "apksig", "9.2.0"),
}

# اختبارات نواة الأمان — الملفّات التي تقيس ما في `core/security`.
SECURITY_TESTS = [
    "core/security/IntegrityLedgerTest.kt",
    "core/security/ApkSigningBlockTest.kt",
    "core/security/ApkContentVerificationTest.kt",
    "core/security/SealedAssetsTest.kt",
]

# مَخزَن التشخيص و`R` مُبدَّلان بذرة: نطاق الأداة نواة الأمان، لا مخزن التشخيص.
STUB_BUILD_CONFIG = """package nd.max

/**
 * بديل مصغّر لـ`BuildConfig` المُولَّد — و**قيمه هنا ليست قيم الإنتاج**: البصمة تُشتقّ من
 * مخزن المفاتيح في `build.gradle.kts`. ومن هنا جاءت [pinHex] لتُمرَّر من الأداة.
 */
object BuildConfig {
    const val DEBUG: Boolean = false
    const val SEALED_ASSETS: Boolean = @SEALED@
    const val SECURITY_PIN_LO: String = "@LO@"
    const val SECURITY_PIN_HI: String = "@HI@"
    const val SECURITY_PIN_MASK: String = "@MASK@"
}
"""

STUB_SUPPORT = """package nd.max

/** معرّفات نصوص الرفض — الأداة تقيس المنطق لا الموارد. */
object R {
    object string {
        const val security_tamper_title: Int = 1
        const val security_tamper_message: Int = 2
        const val security_tamper_close: Int = 3
    }
}
"""

STUB_DIAGNOSTICS = """package nd.max.core.diagnostics

/** مَخزَن تشخيص لا يفعل شيئًا: الأداة تقيس نواة الأمان لا المخزن. */
object DiagnosticCenter {
    enum class Level { INFO, WARN, ERROR }

    fun record(
        component: String,
        message: String,
        throwable: Throwable? = null,
        level: Level = Level.ERROR,
    ) {
        recorded.add(component + " | " + message)
    }

    val recorded = mutableListOf<String>()
}
"""

# قياس يُولَّد هنا ولا يعيش في المستودع: يحتاج مخزن مفاتيح ومُوقِّعًا، ولا يصحّ أن يحمل
# المستودع مفتاحًا ولا حزمة موقّعة. والتوقيع الحقيقي بالمُوقِّع الحقيقي، لا ببنية مصنوعة.
HARNESS_TEST = """package nd.max.core.security

import com.android.apksig.ApkSigner
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SignedApkHarnessTest {

    private fun prop(name: String): String = System.getProperty("harness." + name)!!

    private fun unsignedApk(): File {
        val apk = File.createTempFile("harness-unsigned", ".apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            // STORED: الحجم المضغوط = الحجم الصريح (وبلا ضغط يكون حساب الإزاحة في القلب
            // واضحًا)، و`resources.arsc` مخزَّن في الحزم الحقيقية كذلك.
            fun put(name: String, bytes: ByteArray) {
                val entry = ZipEntry(name).apply { method = ZipEntry.STORED }
                val crc = CRC32().apply { update(bytes) }
                entry.size = bytes.size.toLong()
                entry.compressedSize = bytes.size.toLong()
                entry.crc = crc.value
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            put("AndroidManifest.xml", ByteArray(64) { 0x03 })
            put("resources.arsc", ByteArray(4096) { (it % 251).toByte() })
            put("classes.dex", ByteArray(2048) { (it * 7 % 253).toByte() })
        }
        return apk
    }

    private fun sign(unsigned: File, target: File) {
        val store = KeyStore.getInstance(prop("storetype"))
        store.load(File(prop("keystore")).inputStream(), prop("storepass").toCharArray())
        val alias = prop("alias")
        val key = store.getKey(alias, prop("storepass").toCharArray()) as PrivateKey
        val chain = store.getCertificateChain(alias).map { it as X509Certificate }
        // **٢٨ لا ٢٤**: في apksig 9 صار التوقيع المُوجَّه نحو منصّة (`SignerConfig` بهذه
        // الدالة) مشروطًا بـv3 وهو لا يوجد قبل ٢٨، فيرفضه المُوقِّع تحت ذلك. و٢٨ تعطي
        // v2+v3 بلا v1 — وهو بعينه ما يُنتجه AGP للتطبيق (‏`minSdk 29`).
        val config = ApkSigner.SignerConfig.Builder("harness", key, chain)
            .setMinSdkVersion(28)
            .build()
        ApkSigner.Builder(listOf(config))
            .setInputApk(unsigned)
            .setOutputApk(target)
            // المنصّة مُعلنة هنا كذلك: بدونها يقرأ المُوقِّع `AndroidManifest.xml` الثنائي
            // لاستنباطها، وهذا الملفّ مصنوع في هذه الحزمة فلا هو بنفسها ولا هيكله كامل.
            .setMinSdkVersion(28)
            .build()
            .sign()
    }

    /**
     * `uint16` صغير النهاية في إزاحة ما — للطولَين المُخزَّنين في ٢٦ و٢٨ من رأس السجلّ.
     *
     * وفيها قصة عطب مقيس: كانت تُقرأ بـ`uint32` فيُركَّب من البايتين التاليين (بيانات
     * التوقيع) عددًا بمنطق ٤٥٨٧٧١، فيتوقف المشي عند أوّل سجلّ رغم أن السجلّ سليم تمامًا.
     */
    private fun readAt(path: File, offset: Int): Long {
        val bytes = path.readBytes()
        return (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
    }

    /**
     * `uint16` صغير النهاية في إزاحة ما — للطولَين المُخزَّنين في ٢٦ و٢٨ من رأس السجلّ.
     *
     * وفيها قصة عطب مقيس: كانت تُقرأ بـ`uint32` فيُركَّب من البايتين التاليين (بيانات
     * التوقيع) عددًا بمنطق ٤٥٨٧٧١، فيتوقف المشي عند أوّل سجلّ رغم أن السجلّ سليم تمامًا.
     */
    private fun readNameLength(path: File, offset: Int): Int {
        val bytes = path.readBytes()
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    /**
     * يقلب بايتًا واحدًا **داخل محتوى** مدخل، بمشي على سجلات ZIP المحلية.
     *
     * والحدّ الأدنى المتحقّق منه `>= 2` لا `>= 8`: مدخل صغير مشروع (بيان مضغوط إلى ستّة
     * بايتات) كان يُوقف المشي كله حين اشترطنا حجماً أكبر — وهو عطب أُوقف المشي قبله.
     */
    private fun flipOneByte(source: File, target: File, entry: String): Int {
        val bytes = source.readBytes()
        var cursor = 0
        while (cursor + 30 <= bytes.size) {
            val sig = readAt(source, cursor)
            if (sig != 0x04034b50L) {
                println("HARNESS walk stop: sig=" + java.lang.Long.toHexString(sig) + " at " + cursor)
                break
            }
            val compressed = readAt(source, cursor + 18)
            val nameLength = readNameLength(source, cursor + 26)
            val extraLength = readNameLength(source, cursor + 28)
            val nameEnd = cursor + 30 + nameLength
            val dataStart = nameEnd + extraLength
            // حاجز المشي: رأس تعسفيّ (أو فهرس مركزي) ليس سجلًّا محليًّا، وأطواله بيانات
            // ثنائيّة تُقرأ أطوالًا — والمطالبة بأن يكون السجلّ كله داخل الملفّ (لا أن يكون
            // المدخل كله) تُوقف المشي هنا بدل أن تُقرأ خارج الملفّ. **والفحص قبل القراءة:
            // سطر تشخيص يقرأ الاسم قبل أن يتأكّد أن الاسم داخل الملفّ كان يُقرأ خارجها.**
            val nameFits = nameLength <= bytes.size - (cursor + 30)
            val extraFits = extraLength <= bytes.size - nameEnd
            val dataStartFits = dataStart in (cursor + 30)..bytes.size
            val dataFits = compressed >= 2L && compressed <= (bytes.size - dataStart).toLong() &&
                dataStart + compressed <= bytes.size
            if (!nameFits || !extraFits || !dataStartFits || !dataFits) {
                println("HARNESS walk stop: not a local record with data inside the file at " + cursor +
                    " (nameLen=" + nameLength + " extraLen=" + extraLength + " compressed=" + compressed + ")")
                break
            }
            val name = String(bytes, cursor + 30, nameLength, Charsets.UTF_8)
            println("HARNESS walk name=" + name +
                " compressed=" + compressed + " nameLen=" + nameLength + " extraLen=" + extraLength)
            if (name == entry) {
                val copy = source.readBytes()
                val at = dataStart + (compressed / 2).toInt()
                copy[at] = (copy[at].toInt() xor 0x01).toByte()
                target.writeBytes(copy)
                return at
            }
            cursor = dataStart + compressed.toInt()
        }
        return -1
    }

    @Test
    fun `a freshly signed apk verifies, its signer matches, and one flipped byte is rejected`() {
        val signed = File.createTempFile("harness-signed", ".apk")
        sign(unsignedApk(), signed)
        assertTrue("الحزمة الموقّعة تُكتب في ملفّ", signed.length() > 0)

        val started = System.nanoTime()
        val outcome = ApkContentVerification.verifyFile(signed)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        println("HARNESS signed-apk outcome=" + outcome + " elapsedMs=" + elapsedMs)
        assertEquals("حزمة موقّعة سليمة يجب أن تُقبل", ApkContentVerification.Outcome.VERIFIED, outcome)

        // شهادة الموقّع كما يقرؤها قارئنا من كتلة التوقيع = الشهادة التي وُقّعت بها فعلًا.
        val fromBlock = ApkSigningBlock.signerCertificate(signed)
        assertTrue("قراءة الشهادة من كتلة التوقيع", fromBlock != null)
        assertArrayEquals(
            "شهادة كتلة التوقيع يجب أن تساوي شهادة الموقّع نفسها",
            File(prop("certder")).readBytes(),
            fromBlock,
        )

        val tampered = File.createTempFile("harness-tampered", ".apk")
        val at = flipOneByte(signed, tampered, "resources.arsc")
        assertTrue("موضع القلب يجب أن يوجد", at > 0)
        val original = signed.readBytes()
        val modified = tampered.readBytes()
        assertEquals("الحجم لا يتغيّر بالقلب", original.size, modified.size)
        assertEquals(
            "الفرق بايت واحد بالضبط",
            1,
            original.indices.count { original[it] != modified[it] },
        )
        val startedTamper = System.nanoTime()
        val tamperedOutcome = ApkContentVerification.verifyFile(tampered)
        val tamperMs = (System.nanoTime() - startedTamper) / 1_000_000
        println("HARNESS tampered-apk outcome=" + tamperedOutcome + " elapsedMs=" + tamperMs)
        assertEquals(
            "نسخة عُدّل فيها بايت واحد يجب أن تُرفض",
            ApkContentVerification.Outcome.REJECTED,
            tamperedOutcome,
        )

        // انحراف المنصّة: نفس الحزمة تُفحص كأنها لمنصّة قديمة تطلب v1 ⇒ تُرفض لغياب v1،
        // وهو إثبات أن تمرير منصّة التثبيت ليس تفصيلًا صامتًا.
        val oldPlatform = ApkContentVerification.verifyFile(signed, 1)
        println("HARNESS minPlatform=1 outcome=" + oldPlatform + " (توقّع REJECTED لغياب v1)")
        assertEquals(
            "فحص حزمة بلا v1 لمنصّة تطلب v1 يجب أن يُرفض",
            ApkContentVerification.Outcome.REJECTED,
            oldPlatform,
        )
    }
}
"""


def cached_jar(coords) -> Path:
    group, artifact, version = coords
    matches = sorted((CACHE / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        sys.exit(f"missing cached {group}:{artifact}:{version} — لا تنزيل في هذه الأداة")
    return matches[0]


def find_android_jar() -> Path | None:
    candidates = []
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        root = os.environ.get(env)
        if root:
            candidates += sorted(Path(root).glob("platforms/*/android.jar"))
    candidates += sorted(Path("/usr/lib/android-sdk").glob("platforms/*/android.jar"))
    candidates += sorted((Path.home() / "android-sdk").glob("platforms/*/android.jar"))
    return candidates[-1] if candidates else None


def keystore_cert(keystore: Path, storepass: str, alias: str, cert: Path, java_home: str | None) -> bytes:
    """يُصدّر شهادة الموقّع (DER) من مخزن مفاتيح ويعيد بايتاتها."""
    keytool = str(Path(java_home) / "bin/keytool") if java_home else "keytool"
    subprocess.run(
        [keytool, "-exportcert", "-keystore", str(keystore), "-alias", alias,
         "-storepass", storepass, "-file", str(cert)],
        check=True, capture_output=True,
    )
    return cert.read_bytes()


def masked_halves(pin_hex: str, mask_hex: str) -> tuple[str, str]:
    """نفس تحويل `maskedHalves` في `build.gradle.kts` — ليُقاس المنطق على بصمة حقيقيّة."""
    mask = bytes.fromhex(mask_hex)
    raw = bytes.fromhex(pin_hex)
    masked = bytes(b ^ mask[i % len(mask)] for i, b in enumerate(raw))
    out = masked.hex()
    return out[:32], out[32:]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--work-dir", type=Path, default=Path(tempfile.gettempdir()) / "maxmanager-security-jvm")
    parser.add_argument(
        "--project-keystore",
        type=Path,
        default=ROOT / "manager/app/maxmanager.jks",
        help="مخزن الإصدار الذي تُشتقّ منه البصمة (وكلمة مروره من KS_PWD)",
    )
    parser.add_argument("--json", type=Path, help="يُكتب تقرير النتيجة هنا")
    args = parser.parse_args()

    android_jar = find_android_jar()
    if android_jar is None:
        sys.exit("بلا android.jar: نواة الأمان تلمس Context/Debug ⇒ القياس غير مُتحقَّق في هذه البيئة")

    work = args.work_dir.resolve()
    (work / "src" / "nd" / "max" / "core" / "diagnostics").mkdir(parents=True, exist_ok=True)
    output = work / "classes"
    output.mkdir(parents=True, exist_ok=True)

    jars = {name: cached_jar(coords) for name, coords in JARS.items()}
    compiler_cp = os.pathsep.join(str(jars[k]) for k in ("compiler", "stdlib", "coroutines", "annotations", "reflect"))
    runtime_cp = os.pathsep.join(
        str(jars[k]) for k in ("stdlib", "coroutines", "annotations", "junit", "hamcrest", "apksig")
    ) + os.pathsep + str(android_jar)

    # ── مخزن مفاتيح مؤقّت لهذه الجولة: لا يُكتب في المستودع ولا يُحفظ له سرّ.
    keystore = work / "harness.jks"
    storepass = "harness-store"
    alias = "harness"
    java_home = os.environ.get("JAVA_HOME")
    keytool = str(Path(java_home) / "bin/keytool") if java_home else "keytool"
    if not keystore.exists():
        subprocess.run(
            [keytool, "-genkeypair", "-keystore", str(keystore), "-storetype", "pkcs12",
             "-storepass", storepass, "-keypass", storepass, "-alias", alias,
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
             "-dname", "CN=MaxManager Security Harness"],
            check=True, capture_output=True,
        )
    harness_cert = work / "harness-signer-cert.der"
    keystore_cert(keystore, storepass, alias, harness_cert, java_home)

    # ── بصمة الإصدار: تُشتقّ من مخزن المشروع إن أُعطيَت كلمته (KS_PWD)، وإلا تُعلن غير مُقيسة.
    # ولا تُشتقّ من المخزن المؤقّت أعلاه: ذاك للمُوقِّع وحده، وبصمته غير بصمة الإصدار —
    # وخلط الاثنين يجعل قياس الختم يقارن مفتاحين مختلفين فيَرُد الرفضَ على ختمٍ سليم.
    mask_hex = "9e7c1a34c05f2b68"          # نفس قناع الإنتاج في `build.gradle.kts`
    pin_hex = ""
    storepass_env = os.environ.get("KS_PWD", "")
    if storepass_env and args.project_keystore.is_file():
        project_cert = keystore_cert(
            args.project_keystore, storepass_env, "azenith_key",
            work / "project-signer-cert.der", java_home,
        )
        pin_hex = hashlib.sha256(project_cert).hexdigest()
    elif storepass_env:
        print("! مخزن الإصدار غير موجود: أصول الختم تبقى غير مقيسة")
    else:
        print("! KS_PWD غير مضبوطة: أصول الختم تبقى غير مقيسة (تُعلَن ولا تُدّعى)")
    lo, hi = masked_halves(pin_hex, mask_hex) if pin_hex else ("", "")

    (work / "src" / "nd" / "max" / "BuildConfig.kt").write_text(
        STUB_BUILD_CONFIG.replace("@SEALED@", "true" if pin_hex else "false")
        .replace("@LO@", lo).replace("@HI@", hi).replace("@MASK@", mask_hex),
        encoding="utf-8",
    )
    (work / "src" / "nd" / "max" / "R.kt").write_text(STUB_SUPPORT, encoding="utf-8")
    (work / "src" / "nd" / "max" / "core" / "diagnostics" / "DiagnosticCenter.kt").write_text(
        STUB_DIAGNOSTICS, encoding="utf-8"
    )
    harness_test = work / "src" / "nd" / "max" / "core" / "security" / "SignedApkHarnessTest.kt"
    harness_test.parent.mkdir(parents=True, exist_ok=True)
    harness_test.write_text(HARNESS_TEST, encoding="utf-8")

    sources = sorted(str(p) for p in (SOURCE / "core/security").glob("*.kt"))
    sources += [str(work / "src" / "nd" / "max" / "BuildConfig.kt"),
                str(work / "src" / "nd" / "max" / "R.kt"),
                str(work / "src" / "nd" / "max" / "core" / "diagnostics" / "DiagnosticCenter.kt")]
    tests = sorted(str(TEST / rel) for rel in SECURITY_TESTS)
    tests.append(str(harness_test))

    print("== نواة الأمان: تصرّف وتُقاس على JVM ==")
    print(f"   android.jar: {android_jar}")
    print("   بصمة الإصدار: " + (pin_hex[:16] + "… (كاملة في التقرير)" if pin_hex else "غير مضبوطة"))
    compile_cmd = [
        "java", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", runtime_cp,
        "-d", str(output), *sources, *tests,
    ]
    compiled = subprocess.run(compile_cmd, cwd=ROOT, capture_output=True, text=True)
    if compiled.returncode != 0:
        print(compiled.stdout[-4000:])
        print(compiled.stderr[-4000:], file=sys.stderr)
        return 2

    test_classes = [
        "nd.max.core.security.IntegrityLedgerTest",
        "nd.max.core.security.IntegrityPolicyTest",
        "nd.max.core.security.IntegrityDigestTest",
        "nd.max.core.security.ApkSigningBlockTest",
        "nd.max.core.security.SealedManifestParsingTest",
        "nd.max.core.security.ApkContentVerificationTest",
        "nd.max.core.security.SealedAssetsTest",
        "nd.max.core.security.SignedApkHarnessTest",
    ]
    run_cmd = ["java", "-cp", str(output) + os.pathsep + runtime_cp,
               f"-Dharness.keystore={keystore}", f"-Dharness.storepass={storepass}",
               f"-Dharness.storetype=pkcs12", f"-Dharness.alias={alias}",
               f"-Dharness.certder={harness_cert}",
               "org.junit.runner.JUnitCore", *test_classes]
    ran = subprocess.run(run_cmd, cwd=ROOT, capture_output=True, text=True)
    print(ran.stdout[-8000:])
    if ran.stderr.strip():
        print(ran.stderr[-4000:], file=sys.stderr)

    ok = ran.returncode == 0
    if args.json:
        args.json.write_text(json.dumps({
            "ok": ok,
            "pinConfigured": bool(pin_hex),
            "pinHex": pin_hex,
            "maskedLo": lo,
            "maskedHi": hi,
            "androidJar": str(android_jar),
            "junitReturnCode": ran.returncode,
            "stdout": ran.stdout[-4000:],
        }, ensure_ascii=False, indent=2), encoding="utf-8")
    print("النتيجة:", "نجحت كل اختبارات نواة الأمان" if ok else "فشل — انظر الأعطال أعلاه")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
