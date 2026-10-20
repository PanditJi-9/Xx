import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ⚠️ **بلا `org.jetbrains.kotlin.android`**: AGP 9 يُدمج دعم Kotlin في نفسه، وإضافته تُسقط
// البناء برسالة صريحة («لم تعد مطلوبة منذ AGP 9.0»). و`manager/` نفسه لا يطبّقها — فالخطر
// هنا كان **نسخ عادة AGP 8** لا خطأ في الكود (قِيس بتشغيل حقيقي، لا بالقراءة).
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Supabase settings come from -P flags or environment variables (the CI passes repository secrets).
// Empty means "use the placeholders in Config.kt", which the app reports clearly on the login screen.
fun setting(name: String): String {
    val raw = providers.gradleProperty(name).orNull ?: System.getenv(name) ?: ""
    return raw.trim().replace("\\", "\\\\").replace("\"", "\\\"")
}

android {
    namespace = "com.maxmanager.control"
    // نفس سقف `manager/` (API 37 = Android 17) — مشروع واحد بمحرّك واحد، ولأنّ compileSdk 34
    // لم يكن منشورًا في أي SDK عندنا فكان البناء **غير قابل للتحقّق** لا «ناجحًا».
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maxmanager.control"
        // 26 يكفي لكل ما يقرأه هذا التطبيق: `AES-GCM` عبر Keystore و`SHA256withECDSA`
        // (قِيس في `MAX-CONTROL-01` §٥.٢: Ed25519 ليس مضمونًا عند هذا المستوى).
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "SUPABASE_URL", "\"${setting("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${setting("SUPABASE_ANON_KEY")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    lint {
        // The build must not fail on style warnings; real errors still stop it.
        abortOnError = false
    }
}

// **إصدارات صريحة مطابقة لما يُحلّ في `manager/` فعلًا** (قِيس بأمر `:app:dependencies`، لا
// منقولًا من الذاكرة): ui 1.11.0 · material3 1.5.0-alpha14 · activity-compose 1.13.0 · core-ktx 1.18.0.
// وmaterial3 ليس 1.4.0 لأن `manager/` نفسه يرتفع إلى 1.5.0-alpha14 في الرسم المحلول، وهو الذي
// يجلب `material-ripple` المتوفّر؛ وتثبيت 1.4.0 يطلب `material-ripple:1.8.1` **غير الموجود**
// فيُسقط التحلّل بلا شبكة (قِيس: رسالة `No cached version … for offline mode`).
dependencies {
    implementation("androidx.compose.ui:ui:1.11.0")
    implementation("androidx.compose.foundation:foundation:1.11.0")
    implementation("androidx.compose.material3:material3:1.5.0-alpha14")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ⚠️ **قائمة إلزامية لا ترفيه:** ستّة اعتماديات منقولة تطلب إصدارات **لا وجود لها في مخزن
    // البناء عندنا** (`tracing:1.2.0` · `savedstate:1.3.1` · `collection-ktx:1.5.0` ·
    // `dynamicanimation:1.0.0` · `material-ripple:1.8.1` · `serialization-core:1.7.3`)، وفي
    // `manager/` تُرفع إلى هذه الإصدارات بالضبط من اعتماديات أخرى. فبلا هذا القيد يفشل التحلّل
    // بـ«No cached version … for offline mode» — قِيس. وهو رفعٌ إلى نفس ما يُشحَن في `manager/`،
    // فلا يُضاف إصدار جديد إلى المستودع، ولا يُخفّض شيء.
    constraints {
        implementation("androidx.tracing:tracing:1.3.0")
        implementation("androidx.savedstate:savedstate:1.4.0")
        implementation("androidx.savedstate:savedstate-compose:1.4.0")
        implementation("androidx.collection:collection-ktx:1.6.0")
        implementation("androidx.transition:transition:1.7.1")
        implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")
        implementation("androidx.compose.material:material-ripple:1.11.0-alpha05")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    }

    testImplementation("junit:junit:4.13.2")
}
