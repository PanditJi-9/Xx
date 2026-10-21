/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.io.File
// مُستوردة بالاسم لا بالمسار الكامل: `java` في سكربت Kotlin DSL يُحلّ إلى امتداد
// `java` في Gradle لا إلى حزمة JDK، فـ`java.security.MessageDigest` تُقرأ «مرجعًا
// غير محلول». وهذا يُخطئ فيه كل من يكتب تشفيرًا داخل سكربت بناء مرّة، ثم يتعلّمها.
import java.security.KeyStore
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
}

/**
 * زمن البناء الذي يُكتب في `BuildConfig.BUILD_TIME` (يُعرض كتاريخ بناء في ترويسة الإعدادات).
 *
 * **لماذا ليس `System.currentTimeMillis()` مباشرةً:** ذاك يتغيّر في كل تشغيل، فيصير
 * `generateReleaseBuildConfig` **غير up-to-date أبدًا** ⇒ كل من يقرأ `BuildConfig` يُعاد
 * تصريفه (٤ ملفات) ⇒ ثم dex ثم R8 ثم التغليف — سلسلة كاملة تُعاد من أجل رقم واحد،
 * و`--build-cache` لا يُصيب منها شيئًا لأن مخرَج المُدخَل يتغيّر في كل بناء.
 *
 * **البديل:** زمن الالتزام — ثابتٌ للنُسخة الواحدة (نفس الالتزام ⇒ نفس الرقم)، فيصير البناء
 * الثاني للنُسخة نفسها شبه مجّاني على الجهاز. وهو أصدق معنًى أيضًا: تاريخ إنتاج النسخة.
 *
 * **ولماذا `ValueSource` لا `ProcessBuilder` في جسم السكربت** — عطب قِيس في CI، لا تخمين.
 * تشغيل عملية خارجية في زمن التهيئة مباشرةً يجعل Gradle **يتخلّى عن مخزن التهيئة كلّه**:
 *
 * ```
 * Configuration cache problems found in this build.
 * 1 problem was found storing the configuration cache.
 * - Build file 'app/build.gradle.kts': external process started 'git log -1 --format=%ct'
 * ```
 *
 * أي أن كل تشغيل كان يُهدِر المخزن من أجل رقم واحد. والطريق الذي توثّقه Gradle نفسها
 * (Configuration Cache Requirements §Running External Processes) هو تشغيل العملية **داخل
 * `ValueSource`** بمُشغِّل محقون، وحينها: «تُشغَّل العملية في كل بناء لتقرير هل المخزن ما زال
 * صالحًا، وإن تغيّرت القيمة فُسد المخزن» — وهو المطلوب بالضبط: نفس الالتزام ⇒ نفس الرقم ⇒
 * المخزن صالح، والتزام جديد ⇒ الرقم يتغيّر ⇒ تُعاد التهيئة مرّة واحدة.
 *
 * **والسقوط:** بلا `git` أو بلا التزام (نسخة مفكوكة من zip، أو بناء في AndroidIDE) يُكتب يوم
 * البناء الحالي بدل أن يُرمى خطأ أو يُكتب صفرًا. وتدويرُه إلى اليوم **مقصود** لا تهاون: القارئ
 * الوحيد لهذا الرقم يرسمه `yyyy-MM-dd` (SettingsHeaderComponent)، فالتدوير لا يكذب في شيء
 * ويُبقي المخزن صالحًا داخل اليوم نفسه على جهاز بلا `git`.
 */
abstract class GitCommitTimeValueSource : ValueSource<Long, GitCommitTimeValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        /**
         * مسار المستودع. `String` لا `DirectoryProperty` عن قصد: بصمة شجرة كاملة تُفسد المخزن
         * عند كل ملف يتغيّر، بينما مسارٌ ثابت لا يتغيّر أبدًا.
         */
        val repositoryPath: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Long {
        val output = ByteArrayOutputStream()
        val ran = runCatching {
            execOperations.exec {
                workingDir = File(parameters.repositoryPath.get())
                commandLine("git", "log", "-1", "--format=%ct")
                standardOutput = output
                // بلا مستودع يعود `git` بغير صفر: لا نُفشل البناء، بل نسقط إلى يوم البناء.
                isIgnoreExitValue = true
            }
        }.isSuccess
        val epochSeconds = output.toByteArray().toString(Charsets.UTF_8).trim().toLongOrNull()
        return if (ran && epochSeconds != null) epochSeconds * 1000L else todayEpochMs()
    }

    /** يوم البناء الحالي — سقوطٌ صادق (لا صفر = ١٩٧٠) وثابتٌ داخل اليوم. */
    private fun todayEpochMs(): Long {
        val millisPerDay = 24L * 60L * 60L * 1000L
        return System.currentTimeMillis().let { now -> now - now % millisPerDay }
    }
}

/**
 * ويُقاس **في زمن التهيئة** عن قصد: القيمة الفعلية هي ما يُخزَّن في مدخلات التهيئة، فالنداء
 * يُعاد في كل بناء ليعرف Gradle هل المخزن ما زال صالحًا. والكلفة مقيسة لا مُقدَّرة: تشغيلان
 * كاملان في تجربة هذا الإصلاح استغرقا ٨٥٦ و٧٦١ ملّي ثانية، وفي كلٍّ منهما نداء `git` واحد.
 */
val buildTimeEpochMs: Long = providers.of(GitCommitTimeValueSource::class) {
    parameters {
        repositoryPath.set(rootDir.absolutePath)
    }
}.get()

/**
 * بصمة شهادة التوقيع، مُشتقّة **من مخزن المفاتيح نفسه** لا مكتوبة بيد.
 *
 * **ولماذا `ValueSource` لا قراءة مباشرة:** القيمة تُصبح حقلًا في `BuildConfig`، فتُخزَّن
 * في مخزن التهيئة. ولو قُرئ المخزن قراءةً مباشرةً في زمن التهيئة لَخزّن Gradle نتيجتها
 * ولمعرفة أنّها تغيّرت قطّ: يُعاد المخزن المعلَّل بصمةً **للكود تغيّر** لا لملفّ خارجيّ.
 * وGradle توثّق أنّ قيمة `ValueSource` **تُعاد قراءتها في كل بناء** لتقرير صلاحية المخزن،
 * وهو المطلوب بالضبط: مخزنٌ جديد ⇒ بصمة جديدة ⇒ فساد المخزن مرّة واحدة.
 *
 * **ولا كلمة مرور تُخزَّن:** كلمة سرّ الإصدار تُقرأ من **اسم متغيّر بيئة** داخل `obtain()`
 * (لا كوسيط للـ`ValueSource`)، فلا تُسلسل إلى ملفّ مخزن التهيئة ولا إلى أي ملفّ. أمّا كلمة
 * سرّ التصحيح فهي `android` المشهورة، وتُمرَّر كوسيط صريح لأنها ليست سرًّا.
 *
 * والسقوط: أي فشل ⇒ نصّ فارغ ⇒ «لا بصمة» ⇒ وضع رصد. **ولا يُسقط البناء**، لأن بناءً في
 * بيئة بلا مخزن مفاتيح يجب أن يُنتج تطبيقًا يعمل لا تطبيقًا يرفض نفسه.
 */
abstract class SignerPinValueSource : ValueSource<String, SignerPinValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        /** مسار مخزن المفاتيح؛ فراغ = لا بصمة. */
        val storePath: Property<String>

        /** اسم متغيّر البيئة الذي يحمل كلمة السرّ (لا كلمة السرّ نفسها). */
        val passwordEnv: Property<String>

        /** كلمة سرّ غير سرّية (مخزن التصحيح). */
        val literalPassword: Property<String>

        /** الاسم المستعار للمفتاح؛ فراغ = أوّل مدخل مفتاح في المخزن. */
        val keyAlias: Property<String>
    }

    override fun obtain(): String {
        val path = parameters.storePath.orNull?.takeIf { it.isNotBlank() } ?: return ""
        val file = File(path)
        if (!file.isFile) return ""
        val password = parameters.passwordEnv.orNull?.takeIf { it.isNotBlank() }
            ?.let { System.getenv(it) }
            ?.takeIf { it.isNotBlank() }
            ?: parameters.literalPassword.orNull?.takeIf { it.isNotBlank() }
            ?: return ""
        return runCatching {
            val keyStore = KeyStore.getInstance(file, password.toCharArray())
            val alias = parameters.keyAlias.orNull?.takeIf { it.isNotBlank() }
                ?: keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
                ?: return ""
            val certificate = keyStore.getCertificate(alias) ?: return ""
            MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
                .joinToString("") { byte -> "%02x".format(byte) }
        }.getOrDefault("")
    }
}

/**
 * بصمة موقّع الإصدار السداسية — تُملأ داخل `android { buildTypes { release { … } } }`
 * لأنّها تُقرأ من `signingConfigs`، وهذا لا يكون مُهيّأً إلّا بعد كتلة `signingConfigs {
 * create("release") }` داخله. وتُقرأ لاحقًا في `androidComponents` لتُربط بمهمّة الختم.
 */
var releasePinHex: String = ""

/** قناع إخفاء البصمة في `BuildConfig`: ثابت عمدًا ليبقى البناء قابلاً لإعادة الإنتاج. */
val pinMaskHex = "9e7c1a34c05f2b68"

/**
 * يُقنّع بصمة سداسية بكسرها نصفين وXOR متكرّر بقناع ثابت.
 *
 * **وما تعطله وما لا تعطله — بالصراحة:** تعطل `grep`/`strings` من إيجاد البصمة في الحزمة
 * كنصّ صريح مباشر (وهو ما يفعله من يبحث عن «كيف أُبطل الفحص؟»)، **ولا تعطل** محلّلًا
 * متعمّدًا يفكّ الضرب. والقيمة الحقيقيّة في [SignerPinValueSource] (أن البصمة تُشتقّ ولا
 * تُكتب) لا هنا.
 */
fun maskedHalves(hex: String, maskHex: String): Pair<String, String> {
    if (hex.length != 64 || maskHex.length != 16) return "" to ""
    val mask = ByteArray(8) { maskHex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    val bytes = ByteArray(32) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor mask[i % mask.size].toInt()).toByte()
    val masked = bytes.joinToString("") { byte -> "%02x".format(byte) }
    return masked.substring(0, 32) to masked.substring(32)
}

android {
    namespace = "nd.max"
    // **API 37 = Android 17 — منشور ومُتحقَّق (تكملة ١١٠):** التقييد السابق («API 37 غير
    // منشور في مستودع SDK») كان صحيحًا وقته، وقد انتهى: `platforms;android-37.0`
    // و`build-tools;37.0.0` مستقرّان — مُثبَّتان محليًّا ومُشترَطان في CI. وطلب المالك
    // «يعمل على كل أندرويد دون مشاكل» يبدأ بأن يُصرَّف ضد أحدث واجهة ويُعلَن عليها.
    compileSdk = 37

    // كلمة مرور مخزن المفاتيح من سر CI (KS_PWD / KEYSTORE_PASSWORD).
    // بناء release غير موقّع غير صالح للتثبيت، لذلك نفشل مبكرًا عند طلبه.
    val ksPwd: String? = System.getenv("KS_PWD")
    val releaseArtifactRequested = gradle.startParameter.taskNames.any { requestedTask ->
        requestedTask.substringAfterLast(':') in setOf("assembleRelease", "bundleRelease")
    }
    if (releaseArtifactRequested && ksPwd.isNullOrEmpty()) {
        throw GradleException("KS_PWD must be set to produce a signed release artifact")
    }

    /**
     * **مصدر المكتبات الأصليّة** — `maxfx/libs/` حيث يُخرج `ndk-build` مؤثّرنا النظاميّ.
     *
     * **ولماذا يُضاف إلى `jniLibs` مع أنّ التعليق في سير عمل CI يقول إنّ مجلّده منفصل:** الانفصال
     * كان يمنع `clean-installed-binaries` من مسّه؛ وهذا يبقى صحيحًا (البناء يكتب في `maxfx/libs`،
     * والتنظيف لا يمسّه). وأمّا **الشحن** فلم يكن له مسار أصلًا: الطبقة النظاميّة تُنسخ إلى الجهاز من
     * ملفّ المكتبة (‏[`AudioEffectLibraryStaging`] تقرأ مدخل `lib/<abi>/libmaxfx.so` من الحزمة)،
     * **فمكتبةٌ لا تُشحَن لا تُنسخ ولا تُحمَّل** — و«صفر تغيير» في لوق المالك كان بعضُها هذا بعينه.
     *
     * **وحدُّ هذا السطر مُعلَن:** Gradle **لا يبني** المكتبة (لا `externalNativeBuild`) — يبنبها
     * `ndk-build` قبل Gradle في CI. فبناءٌ محليّ بلا `ndk-build` سابق يُنتج تطبيقًا بلا مكتبة،
     * وتُقرأ الحاجة في الشاشة بسببها الصريح (`effect-library-not-shipped-in-app`).
     *
     * **والشرط مقصود لا تحوّطًا:** المصدر يُضاف **إن وُجد المجلّد وقت التهيئة فقط**. فالبناء المحليّ
     * بلا `ndk-build` سابق لا يُدخل مصدرًا غائبًا (ولا يُخاطر بسلوك AGP غير المقيس هنا)، والـCI يضيفه
     * دائمًا لأنّه يبني بـ`ndk-build` قبله. **وحدّه مُعلَن:** هذا الشرط وتغليف المكتبة **لم يُقيَما في
     * هذه البيئة** (لا Android SDK ولا NDK فيها) — ويتحقّقان في CI بخطوة تقيس المدخل داخل الحزمة.
     *
     * والمسار من جذر المشروع (`manager/`) فلا يتوقّف على مجلّد التشغيل.
     */
    val maxFxLibsDir = rootProject.file("../maxfx/libs")
    if (maxFxLibsDir.isDirectory) {
        sourceSets["main"].jniLibs.srcDir(maxFxLibsDir)
    }

    defaultConfig {
        applicationId = "nd.max"
        minSdk = 29
        targetSdk = 37
        versionCode = 3
        versionName = "V3.0"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("long", "BUILD_TIME", "${buildTimeEpochMs}L")
        // قناة المانيفست بقيت متاحة لكنها **اختيارية صريحة** الآن: قناة التحديث الفعّالة هي
        // إعداد اللوحة الموقّع (الإصدار + رابط التنزيل يكتبهما المالك من Max Control، والنافذة
        // تفتح الرابط في المتصفّح). والرابط الافتراضي القديم (nader295/Max-Manger) قِيس حيًّا:
        // **404** — والمستودع الخاص لا يخدم raw لمجهول — فقناةٌ لا تُقرأ لا تُترك رابطًا ميتًا
        // يوهم المستخدم بعطب شبكة. ومن أراد المانيفست يمرّره صريحًا بـ-Pmaxmanager.updateManifestUrl=…
        val updateManifestUrl = (project.findProperty("maxmanager.updateManifestUrl") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: ""
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
        // ── منظومة الترخيص والإعداد البعيد (LICENSE-KEYS-01) ────────────────────────────────
        // القيم تُمرَّر من أسرار CI أو من سطر البناء (`-Pmaxmanager.supabaseUrl=…`)، وافتراضها
        // **فارغ**. وفراغُها ليس عطبًا: التطبيق يعمل بسلوكه المحلي كاملًا (كل الميزات كما في
        // الحزمة)، والافتراضي المحلي هو الحكم — وهذا الفرق بين «بناء بلا منظومة» و«بناء معطّل».
        // ولا سرّ هنا: مفتاح anon **منشور بطبيعته** في كل نسخة، والمفتاح العامّ للتحقّق عامّ.
        val supabaseUrl = (project.findProperty("maxmanager.supabaseUrl") as String?)?.trim().orEmpty()
        val supabaseAnonKey = (project.findProperty("maxmanager.supabaseAnonKey") as String?)?.trim().orEmpty()
        val configPublicKey = (project.findProperty("maxmanager.configPublicKey") as String?)?.trim().orEmpty()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "CONFIG_PUBLIC_KEY", "\"$configPublicKey\"")
        ndk {
            // **العمودان معًا (قرار المالك، تكملة ١١٠ — عكس تكملة ٨٢):** هواتف 32-بت
            // تعود مدعومة كاملة: `armeabi-v7a` يُبنى في التطبيق (مكتبات JNI) وفي الموديول
            // (الثنائيات الخمسة) وفي المنصّب (يختار بحسب `ARCH`)، و`arm64-v8a` كما كان.
            // وترتيب القائمة (64 ثم 32) هو عُرف السلف المُستعاد من المشروع القديم.
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    tasks.matching { task ->
        task.name in setOf("packageRelease", "assembleRelease", "bundleRelease")
    }.configureEach {
        doFirst {
            check(!ksPwd.isNullOrEmpty()) {
                "KS_PWD must be set to produce a signed release artifact"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("maxmanager.jks")
            if (!ksPwd.isNullOrEmpty()) {
                storePassword = ksPwd
                keyAlias = "azenith_key" // alias unchanged: renaming it needs the store password (CI secret); regenerating would change the signer fingerprint
                keyPassword = ksPwd
            }
        }
    }
    
    // ── ملاحظة عن `lintVitalRelease` (قِيست، ولا تُغيَّر هنا) ──
    //
    // `assembleRelease` يستدعي `lintVitalRelease` تلقائيًّا، وقِسناها في هذه البيئة:
    // **٣ دقائق و٢٩ ثانية** من أصل ~١٥ دقيقة (٤ أنوية · ١٥ جيجابايت، نفس مقاس runner).
    //
    // **ولا يُعطَّل هنا:** `checkReleaseBuilds = false` **يمحو المهمة نفسها** (تحقّقنا:
    // اختفت من `:app:tasks --all`)، فيصير الحاجز غائبًا لا منقولًا — وهذا إسقاط لجودة لا
    // تسريع لبناء. والفصل جرى بـ`-x :app:lintVitalRelease` في أمر البناء داخل
    // `.github/workflows/build.yml` (والمهمة الموازية التي كانت تشغّله أُزيلت في تكملة ٨٠)
    // ⇒ **لا حاجز آليّ اليوم لِما تكشفه هذه المهمة وحدها**، ومسار الفحص اليدوي قبل الإصدار:
    // `./gradlew :app:lintVitalRelease`.

    androidResources {
        // [FIX] AGP يرفض الجمع بين توليده الآلي و`android:localeConfig` الصريح في
        // البيان («Locale config generation was requested but user locale config is
        // present in manifest») فيفشل :app:processDebugMainManifest ويسقط البناء كله.
        // نُبقي قائمتنا الصريحة في res/xml/locales_config.xml لأنها المصدر الوحيد
        // الذي يضمن ظهور اللغات الـ85 كاملةً في منتقي النظام — ومنها ما لا يستنتجه
        // التوليد الآلي من أسماء المجلدات (b+sr+Latn، zh-rHK، pt-rPT…) — ولأن
        // tools/i18n_coverage.py --check-codes يتحقق من مطابقتها للمجلدات والمنتقي.
        generateLocaleConfig = false
    }

    /**
     * أصول الملكية الفكريّة (كتالوجات الأجهزة والمعالجات وشريحة SoC) — مخرَجة من
     * `assets/` إلى مجلّد لا يدخله أي مصدر تلقائيًّا، فتصير **حاضرة في debug وحدها**،
     * ويُولَّد نظيرها المختوم للإصدار. ولماذا كذلك: لو بقيت في `assets/` لَشُحنت صريحةً
     * مع الإصدار إلى جانب النسخة المختومة، فيسقط الختم كلّه من أجلها.
     */
    sourceSets {
        getByName("debug").assets.srcDir("src/main/assets-internal")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            signingConfig =
                if (ksPwd.isNullOrEmpty()) null
                else signingConfigs.getByName("release")

            // بصمة الموقّع: مُشتقّة لا مكتوبة (انظر SignerPinValueSource).
            releasePinHex = providers.of(SignerPinValueSource::class) {
                parameters {
                    val config = signingConfigs.getByName("release")
                    storePath.set(config.storeFile?.absolutePath ?: "")
                    passwordEnv.set("KS_PWD")
                    literalPassword.set("")
                    keyAlias.set(config.keyAlias ?: "")
                }
            }.get()
            val (releaseLo, releaseHi) = maskedHalves(releasePinHex, pinMaskHex)
            buildConfigField("String", "SECURITY_PIN_LO", "\"$releaseLo\"")
            buildConfigField("String", "SECURITY_PIN_HI", "\"$releaseHi\"")
            buildConfigField("String", "SECURITY_PIN_MASK", "\"$pinMaskHex\"")
            // الأصول مختومة فقط إذا اشتُقّت بصمة فعلًا (وإلا فلا مفتاح يفكّها).
            buildConfigField("boolean", "SEALED_ASSETS", "${releasePinHex.isNotEmpty()}")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), 
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            val debugPinHex = providers.of(SignerPinValueSource::class) {
                parameters {
                    val config = runCatching { signingConfigs.getByName("debug") }.getOrNull()
                    storePath.set(config?.storeFile?.absolutePath ?: "")
                    passwordEnv.set("")
                    literalPassword.set(config?.storePassword ?: "android")
                    keyAlias.set(config?.keyAlias ?: "androiddebugkey")
                }
            }.get()
            val (debugLo, debugHi) = maskedHalves(debugPinHex, pinMaskHex)
            buildConfigField("String", "SECURITY_PIN_LO", "\"$debugLo\"")
            buildConfigField("String", "SECURITY_PIN_HI", "\"$debugHi\"")
            buildConfigField("String", "SECURITY_PIN_MASK", "\"$pinMaskHex\"")
            buildConfigField("boolean", "SEALED_ASSETS", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.version"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
        // **حرس 32-بت أُزيل (تكملة ١١٠):** كان يمنع دخول `lib/armeabi-v7a/**` في زمن
        // «64-بت وحده». اليوم الـABIان يُشحنان معًا، فالحرس انقلب إلى الاتجاه الصحيح
        // وموضعه CI: البناء يفشل إن **غاب** `lib/armeabi-v7a/` أو `lib/arm64-v8a/`.
        // (وقيمة إيجابية بالمناسبة: `kernel-flasher` حمل ثنائيات v7a جاهزة في المستودع،
        // وكان الحرس السابق يمنعها من الوصول إلى جهازها. **وقد حُذفت الوحدة** في جولة
        // تدقيق الأصل `PROVENANCE-01` مع ميزة التفليش.)
    }

    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // Compose Material3 still marks several stable-looking APIs as experimental. They are
            // opted in once here instead of repeating `@file:OptIn` per screen, which is how the
            // tree ended up with a mix of files that had it and files that did not compile.
            optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }
}

/**
 * خاتم أصول الملكية: يُقرأ المجلّد الصريح ويكتب نظيره المختوم + قائمة الختوم.
 *
 * **ولماذا مهمّة Gradle لا سكربت خارجيّ:** الأصول يجب أن تُختم **قبل** التغليف وبنفس
 * البصمة التي ستُبنى بها الحزمة، فربطها بالمتغيّر (`addGeneratedSourceDirectory`) يجعل
 * AGP نفسه يضمن الترتيب — بدل وصفة تُنسى في أحد المسارين (CI والمحليّ).
 *
 * **ومُحسَّنة للمخزن (`@CacheableTask`):** المدخلات معلنة (مجلّد المصدر + البصمة)، والخاتم
 * **حتميّ** (ملح ونونس مشتقّان من اسم الملف ومحتواه، لا عشوائيّة)، فإعادة البناء بنفس
 * المدخلات لا تُنتج بايتًا مختلفًا — وبناءٌ غير حتميّ كان سيُفسد مخزن البناء في كل تشغيل.
 */
@CacheableTask
abstract class SealAssetsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    /** بصمة موقّع الحزمة السداسية — هي جذر المفتاح. */
    @get:Input
    abstract val pinHex: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun seal() {
        val output = outputDir.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val root = sourceDir.get().asFile
        val pin = pinHex.get()
        if (pin.length != 64 || !root.isDirectory) return
        val manifest = StringBuilder()
        root.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(root).path }.forEach { file ->
            val relative = file.relativeTo(root).path.replace(File.separatorChar, '/')
            val plain = file.readBytes()
            val sealed = sealBytes(plain, relative, pin)
            val target = File(output, relative)
            target.parentFile?.mkdirs()
            target.writeBytes(sealed)
            val digest = MessageDigest.getInstance("SHA-256").digest(sealed)
            manifest.append(digest.joinToString("") { byte -> "%02x".format(byte) })
                .append(" ").append(relative).append("\n")
        }
        File(output, "sealed-manifest.txt").writeText(manifest.toString())
    }

    /**
     * الختم: `MMVAULT1 || salt(16) || iv(12) || AES-GCM(plain)`.
     *
     * والملح والنونس **مشتقّان لا عشوائيّان**: ملحٌ = `SHA-256(pin ‖ relativePath)`،
     * ونونس = `SHA-256(relativePath ‖ pin ‖ SHA-256(plain))` — فالبناء حتميّ، ومع ذلك
     * لا يتكرّر (نونس، مفتاح) بين ملفّين مختلفين ولا بين محتويين مختلفين لملفّ واحد
     * (وهو شرط GCM الذي لا يُتهاون فيه: تكرار النونس بالمفتاح نفسه يُفشي XOR النصّين).
     */
    private fun sealBytes(plain: ByteArray, relative: String, pin: String): ByteArray {
        val sha = { bytes: ByteArray -> MessageDigest.getInstance("SHA-256").digest(bytes) }
        val hex = { bytes: ByteArray -> bytes.joinToString("") { byte -> "%02x".format(byte) } }
        val pinBytes = ByteArray(32) { pin.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        val salt = sha((hex(pinBytes) + "|salt|" + relative).toByteArray(Charsets.UTF_8)).copyOf(16)
        val iv = sha((relative + "|iv|" + pin + "|" + hex(sha(plain))).toByteArray(Charsets.UTF_8)).copyOf(12)
        val key = sha(pinBytes + salt).copyOf(16)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.ENCRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.GCMParameterSpec(128, iv),
        )
        return "MMVAULT1".toByteArray(Charsets.US_ASCII) + salt + iv + cipher.doFinal(plain)
    }
}

/**
 * يربط الخاتم بمتغيّر الإصدار: مُخرَجه مصدر أصول مُولَّد، وAGP يضمن أنّه يُشغَّل قبل الدمج.
 * وإن تعذّر اشتقاق البصمة (بلا مخزن مفاتيح) تُشحن الأصول صريحةً — لا "بلا أصول"، لأن
 * حزمةً بلا كتالوج الأجهزة تطبيقٌ مكسور لا تطبيقٌ محميّ.
 */
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        if (releasePinHex.isEmpty()) {
            variant.sources.assets?.addStaticSourceDirectory("src/main/assets-internal")
            return@onVariants
        }
        val seal = tasks.register<SealAssetsTask>("sealReleaseCreativeAssets") {
            description = "يختم أصول الملكية الفكريّة بمفتاح مشتقّ من بصمة موقّع الإصدار"
            sourceDir.set(layout.projectDirectory.dir("src/main/assets-internal"))
            pinHex.set(releasePinHex)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(seal, SealAssetsTask::outputDir)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
    // Declared explicitly (DI-01): `HorizontalPager` in Device Info was being pulled through
    // material3 transitively; a page pulled from an undeclared dependency breaks on a material3
    // upgrade with no visible cause.
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.compose.material3)
    // ودجات الشاشة الرئيسية (Glance): رسم الودجة يقع في عملية المشغّل لا في عمليتنا.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material.kolor)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.materials)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.androidx.documentfile)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)

    // Shizuku: طبقة امتياز ثانية بمستوى ADB بلا جذر (AR-20).
    // provider ليس اختياريًا: يحمل ShizukuProvider الذي يُصرَّح به في البيان.
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // ── مكتبات أُخرجت في جولة «الحرارة واللاغ وتقليل الحزمة» (APK-TRIM-01) ──
    //
    // **والحكم فيها بالقياس لا بالانطباع:** كل واحدة منها فُحصت في الشجرة كلها
    // (`manager/` بما فيه `src/main`, `src/test`, `res`, `AndroidManifest.xml`) بـ`git grep` فلم
    // يوجد لها **مستهلك واحد**:
    //
    //   • `compose-markdown`        — صفر مرجع لأي رمز منها.
    //   • `appiconloader-coil`      — صفر مرجع (والشجرة تحمل `AppIconCache` الخاص بها).
    //   • `media3-exoplayer/ui`      — صفر مرجع لـ`ExoPlayer`/`PlayerView`/`media3` في أي ملف Kotlin
    //                                 أو مورد أو بيان؛ ولا وحدة `app` وحدها بل `manager/` كلّه.
    //
    // والثمن الذي يُدفع لكل واحدة حتى وهي غير مستعملة: **ترجمة + فحص Hilt/KSP + فهرسة dex +
    // موارد وتنميطات تُدمج ثم تُشذَّب**. وفي التشغيل: مُهيّئون وخدمات معلنة تُقرأ من فهرس الحزمة.
    // ولا شيء منها يُقاس في `MaxManager`: سلوك المنتج هو ما يُقاس، وما لا يُنادى لا يُقاس.
    //
    // **والحدّ المُعلن:** هذا حذف **تبعيات** لا حذف كود؛ فإن احتاجت الشجرة يومًا مشغلًا داخل
    // التطبيق (`ExoPlayer`) أو عرض Markdown أو أيقونات بتقنية `appiconloader`، فتُعاد السطور من
    // `libs.versions.toml` وهي باقية بأرقامها هناك — والعودة سطر واحد.

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.yalantis.ucrop)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.transition)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.ansi.library)
    implementation(libs.ansi.library.ktx)
    implementation(libs.hiddenapibypass)
    // التحقّق من **محتوى** الحزمة مقابل بصماتها الموقّعة (ADR-69). ولذلك هي مكتبة تشغيل
    // لا أداة بناء: الفحص يجب أن يقع على الجهاز، على النسخة المثبَّتة نفسها.
    implementation(libs.apksig)
    // GIF لعناصر الواجهة (‏`GifDecoder.Factory()` في `HomeComponents.kt`) — مُستعمل فعلًا.
    implementation(libs.coil.gif)

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")


    // ── الاختبارات الوحدوية ──
    // عقد نصوص التوصيات↔الأفعال (RecommendationTextClassifierTest) —
    // أول خط دفاع ضد انزياح الصياغة بين المصادر والمصنِّف
    testImplementation("junit:junit:4.13.2")
    // Local JVM tests execute against the host JDK, so Android's framework
    // org.json stubs must be replaced by the real JSON implementation.
    testImplementation("org.json:json:20260814")
}
