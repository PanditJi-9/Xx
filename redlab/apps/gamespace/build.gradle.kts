// P1 (docs/MASTER_PLAN.md §8): the first module built out of the original materials.
//
// The resources, the assets and the native libraries are the original ones, staged by
// tools/port_materials.py into `materials/` — they are not tracked by git and not rewritten.
// What this module adds today is the manifest we can actually ship and one screen that reports
// what the port measured; each ported screen replaces part of that screen as its code compiles.
plugins {
    id("com.android.application")
}

dependencies {
    implementation("androidx.preference:preference:1.2.1")
}

android {
    namespace = "dev.redlab.gamespace"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.redlab.gamespace"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Extra source sets, so the original corpus compiles with the build instead of being copied
    // into a directory someone has to keep in sync by hand.
    sourceSets["main"].res.srcDir("materials/res")
    sourceSets["main"].assets.srcDir("materials/assets")
    sourceSets["main"].jniLibs.srcDir("materials/jniLibs")
    // Units ported from the original, staged by port_materials.py. Absent until the port stages
    // them, so a plain checkout still builds the app instead of failing on a missing directory.
    if (file("materials/sources").isDirectory) {
        sourceSets["main"].java.srcDir("materials/sources")
    }

    // Libraries the original app bundled as dex and we therefore do not port from decompiled
    // source (measured: jadx output for them does not compile). The real artifact replaces it.
    useLibrary("org.apache.http.legacy")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
