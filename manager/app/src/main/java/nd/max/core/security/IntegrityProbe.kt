/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File
import java.security.MessageDigest

/**
 * المسبار: **يقيس فقط، ولا يحكم**. الحكم في [IntegrityLedger] وهو منطق خالص يُقاس في JVM.
 *
 * والفصل مقصود: ما يحتاج Android لا يمكن قياسه إلا على جهاز، فلو حُشي المنطق هنا لصار
 * الحكم كله رهينة جهازٍ لا نملكه. أمّا هنا فمسارات قراءة قصيرة، كل واحد منها يُعيد قيمة
 * خامًا (بصمة، أو PID، أو اسم) ولا يعرف شيئًا عن العتبات.
 *
 * **ولا يُسجَّل سرّ هنا ولا يُطبع:** ما يُعاد هو بصمة (هاش) أو اسم ملفّ، لا مفاتيح.
 */
object IntegrityProbe {

    /** مسار المكتبة الأصلية داخل الحزمة، لكل بنية. */
    private const val NATIVE_LIB_ENTRY_PREFIX = "lib/"

    /** اسم ملفّ المكتبة الأصلية كما تُحمّله [nd.max.core.jni.ProbeBridge]. */
    const val NATIVE_LIBRARY_NAME = "libmaxmanager_native.so"

    /**
     * بصمة شهادة التوقيع للحزمة المُثبَّتة (SHA-256 على شهادة DER).
     *
     * و`null` تعني «تعذّرت القراءة» أو «أكثر من موقّع»، وليست «مطابقة» ولا «مختلفة» — فلا
     * يُبنى عليها دليل، ويُترك الحكم للسياسة ([SecurityGate]) التي تُعلن الوضع.
     *
     * **وهذه أشهر نقطة يُهاجَمها الفحص بالهوك** (استبدال `getPackageInfo`). ومن هنا جاءت
     * المرساة الأصلية الثانية في [SecurityBridge] التي تقرأ شهادة الحزمة من ملفّ الـAPK
     * مباشرةً بلا `PackageManager` — فمن أراد التعمية عليه أن يُبطل المسارين معًا.
     */
    fun signerDigest(context: Context): ByteArray? = runCatching {
        val info = packageInfo(context, context.packageName)
        val signing = info.signingInfo ?: return null
        if (signing.hasMultipleSigners()) return null
        val certificate = signing.apkContentsSigners.firstOrNull() ?: return null
        sha256(certificate.toByteArray())
    }.getOrNull()

    /**
     * بصمة شهادة التوقيع مقروءة **من ملفّ الحزمة نفسه** بحساب مباشر على ملفّ التوقيع.
     *
     * وهذه مرساة ثانية **مستقلّة عن `PackageManager` بالكامل**: لا تسأل النظام شيئًا، بل
     * تُفتح الحزمة من `sourceDir` وتُقرأ شهادة `META-INF/` (ملفات `.RSA` أو `.DSA`/`.EC`) منها.
     * ومن أراد تجاوز [signerDigest] بالهوك وحده يبقى مكشوفًا هنا.
     *
     * وحدّها المعلن: تُقرأ الشهادة من مخطّط التوقيع القديم (v1/JAR) — وهو موجود في كل حزمة
     * يوقّعها AGP افتراضيًّا إلى جانب v2/v3. فإن غاب، عادت `null` (لا «مطابقة»).
     */
    fun signerDigestFromApkFile(context: Context): ByteArray? = runCatching {
        val apk = File(context.applicationInfo.sourceDir)
        java.util.zip.ZipInputStream(apk.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.startsWith("META-INF/") && SIGNATURE_SUFFIXES.any { name.endsWith(it) }) {
                    val bytes = zip.readBytes()
                    val certificates = java.security.cert.CertificateFactory
                        .getInstance("X.509")
                        .generateCertificates(bytes.inputStream())
                    val first = certificates.firstOrNull() ?: return null
                    return sha256(first.encoded)
                }
                entry = zip.nextEntry
            }
            null
        }
    }.getOrNull()

    /** هل مصحّح مرتبط بهذه العملية؟ ونسخة `release` غير قابلة للتنقيح أصلًا. */
    fun debuggerAttached(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    /**
     * `TracerPid` من `/proc/self/status` — صفر تعني «لا متتبّع».
     * و`null` تعني «لم تُقرأ»، ولا تُفسَّر صفرًا.
     */
    fun tracerPid(): Int? = runCatching {
        File("/proc/self/status").useLines { lines ->
            lines.firstOrNull { it.startsWith(TRACER_PID_PREFIX) }
                ?.removePrefix(TRACER_PID_PREFIX)?.trim()?.toIntOrNull()
        }
    }.getOrNull()

    /**
     * اسم أوّل مكتبة هوك ظاهرة في خريطة ذاكرة العملية، أو `null`.
     *
     * **والرصد بالاسم لا بالنمط العشوائي:** مسحٌ يبحث عن «frida» في أي سلسلة يعطي بلاغات
     * كاذبة (اسم حزمة، مسار مشروع، نصّ). وهنا يُقارَن كل سطر من `/proc/self/maps` بقائمة
     * أسماء معلنة في [IntegrityLedger.HOOK_LIBRARY_MARKERS]، ويُعاد الاسم نفسه الذي طابق
     * ليُكتب في التشخيص — فيستطيع المستخدم أن يرى **ما** رُصد لا أن يُخبَر أن شيئًا رُصد.
     */
    fun hookLibraryMapped(): String? = runCatching {
        File("/proc/self/maps").useLines { lines ->
            lines.firstOrNull { line ->
                IntegrityLedger.HOOK_LIBRARY_MARKERS.any { marker -> line.contains(marker, ignoreCase = true) }
            }?.substringAfterLast('/')?.trim()?.take(64)
        }
    }.getOrNull()

    /** أوّل صنف جسر هوك موجود فعلًا في مسار الأصناف، أو `null`. */
    fun javaHookBridge(): String? = runCatching {
        IntegrityLedger.JAVA_HOOK_CLASSES.firstOrNull { name ->
            runCatching { Class.forName(name, false, javaClass.classLoader) }.isSuccess
        }
    }.getOrNull()

    /**
     * بصمة المكتبة الأصلية داخل الحزمة (كما وُقّعت) — تُقارَن بالملفّ المستخرج على القرص.
     *
     * **ولماذا هذا دليل قاطع:** المكتبات الأصلية تُستخرج إلى `nativeLibraryDir` عند التثبيت،
     * وهذا المجلّد **ليس محميًّا من الكتابة على الأنظمة المتجذّرة**. فاستبدال
     * `libmaxmanager_native.so` هناك لا يغيّر بايتًا واحدًا في الحزمة الموقّعة (فلا يراه فحص
     * التوقيع أبدًا) ويُحمَّل مكان مكتبتنا. واختبار غيابه كان ثغرة صامتة.
     *
     * والعائد `null` إن تعذّرت المقارنة (لا مكتبة في الحزمة أصلًا — وهو حال بناء محليّ بلا
     * NDK). و`true` تعني «مختلفة»: دليل قاطع.
     */
    fun nativeImageMismatch(context: Context): Boolean? = runCatching {
        val apk = File(context.applicationInfo.sourceDir)
        val abi = primaryAbi()
        val entryName = NATIVE_LIB_ENTRY_PREFIX + abi + "/" + NATIVE_LIBRARY_NAME
        val inApk = zipEntryBytes(apk, entryName) ?: return null
        val extracted = File(context.applicationInfo.nativeLibraryDir, NATIVE_LIBRARY_NAME)
        if (!extracted.isFile) return null
        !IntegrityDigest.constantTimeEquals(sha256(inApk), sha256(extracted.readBytes()))
    }.getOrNull()

    /** بصمة مدخل داخل الحزمة، أو `null`. تُستخدم لاختبارات الختم وللمقارنات المعلنة. */
    fun apkEntryDigest(context: Context, entry: String): ByteArray? = runCatching {
        zipEntryBytes(File(context.applicationInfo.sourceDir), entry)?.let(::sha256)
    }.getOrNull()

    /** بصمة ملفّ على القرص، أو `null`. */
    fun fileDigest(file: File): ByteArray? = runCatching {
        if (!file.isFile) null else sha256(file.readBytes())
    }.getOrNull()

    fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    /**
     * بنية الجهاز الأساسية كما يراها النظام (فهرس `SUPPORTED_ABIS`)، مع التحفّظ إلى
     * بنيتنا المعلنة — فلا يُطلب مدخل `lib/x86_64` في حزمة لا تُشحنه.
     */
    private fun primaryAbi(): String {
        val supported = Build.SUPPORTED_ABIS
        return supported.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" } ?: supported.firstOrNull().orEmpty()
    }

    private fun packageInfo(context: Context, pkg: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
            context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        }

    private fun zipEntryBytes(apk: File, name: String): ByteArray? {
        java.util.zip.ZipFile(apk).use { zip ->
            val entry = zip.getEntry(name) ?: return null
            return zip.getInputStream(entry).use { it.readBytes() }
        }
    }

    private const val TRACER_PID_PREFIX = "TracerPid:"

    private val SIGNATURE_SUFFIXES = listOf(".RSA", ".DSA", ".EC", ".rsa", ".dsa", ".ec")
}
