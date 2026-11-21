// P1 (docs/MASTER_PLAN.md §8): the first module built out of the original materials.
//
// The resources, the assets and the native libraries are the original ones, staged by
// tools/port_materials.py into `materials/` — they are not tracked by git and not rewritten.
// What this module adds today is the manifest we can actually ship and one screen that reports
// what the port measured; each ported screen replaces part of that screen as its code compiles.
plugins {
    id("com.android.application")
}

// No `androidx.preference` dependency on purpose: the exploited materials already carry that
// library's resources (apktool kept them), and adding the AAR made mergeDebugResources fail with
// "Can not add resource ... to table" — the same resource name declared twice. Units that need it
// are reported as a dependency by port_slice.py and stay out of the frontier.

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
    // Units the compiler accepted and we sealed: they are tracked by git, read and edited here.
    // A later port run stages only what is not sealed yet, so no unit lands in two source dirs.
    if (file("src/ported/java").isDirectory) {
        sourceSets["main"].java.srcDir("src/ported/java")
    }

    // Libraries the original app bundled as dex and we therefore do not port from decompiled
    // source (measured: jadx output for them does not compile). The real artifact replaces it.
    useLibrary("org.apache.http.legacy")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
