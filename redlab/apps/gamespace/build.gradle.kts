import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

// P1 (docs/MASTER_PLAN.md §8): the first module built out of the original materials.
//
// The resources, the assets and the native libraries are the original ones, staged by
// tools/port_materials.py into `materials/` — they are not tracked by git and not rewritten.
// What this module adds today is the manifest we can actually ship and one screen that reports
// what the port measured; each ported screen replaces part of that screen as its code compiles.
plugins {
    id("com.android.application")
}

// The dependencies the original bundled in dex live in `libraries.txt`. tools/port_slice.py reads
// the same file, so a unit it calls compilable is exactly a unit whose imports are on javac's
// classpath. compileOnly keeps javac honest without letting the AARs merge resources into a table
// that already carries the original copies; `portRuntime` below puts only their classes in the APK.

android {
    namespace = "dev.redlab.gamespace"
    compileSdk = 36

    defaultConfig {
        // A unique id, not cn.nubia.gamelauncher: the stock GameSpace is a system app, so an APK
        // carrying its id cannot be installed over it without root (signature mismatch). Coexisting
        // with it is what lets this build install on an ordinary phone. The namespace stays
        // dev.redlab.gamespace because the ported R classes were compiled against it.
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

    // The sealed code was decompiled with the original resource ids. AGP renumbers the table when
    // it links, so those constants pointed at the wrong resources (measured: id/tgk_case_modify_name
    // 0x7f0a0643 original vs 0x7f0a0641 built). The decoded `values/public.xml` is the original
    // id table; aapt2 can pin every id with `--stable-ids`. apktool keeps dots in style names and
    // `$` in the 41 renamed AVD files, while aapt2/R normalize them to `_` and `rl_`, so map the
    // names the same way before writing the mapping.
    val stableIdsFile = layout.buildDirectory.file("stable-ids.txt")
    val generateStableIds = tasks.register("generateStableIds") {
        val publicXml = file("materials/res/values/public.xml")
        inputs.file(publicXml)
        outputs.file(stableIdsFile)
        doLast {
            val out = stableIdsFile.get().asFile
            if (!publicXml.isFile) {
                out.writeText("")
                return@doLast
            }
            val pattern = Regex("<public type=\"([^\"]+)\" name=\"([^\"]+)\" id=\"(0x[0-9a-fA-F]+)\"")
            val lines = pattern.findAll(publicXml.readText()).map { match ->
                val type = match.groupValues[1]
                var name = match.groupValues[2]
                if (name.startsWith("$")) {
                    name = "rl_" + name.substring(1)
                }
                // aapt2 2.20 accepts stable ids only when the key is package-qualified
                // (`dev.redlab.gamespace:string/name = 0x...`). Without the package it silently
                // ignores the file and renumbers everything (measured in /tmp/opencode/aapttest).
                "dev.redlab.gamespace:$type/$name = ${match.groupValues[3]}"
            }.toList()
            out.writeText(lines.joinToString("\n", postfix = "\n"))
        }
    }
    androidResources {
        additionalParameters("--stable-ids", stableIdsFile.get().asFile.absolutePath)
    }
    tasks.matching { it.name == "processDebugResources" }.configureEach {
        dependsOn(generateStableIds)
        inputs.file(stableIdsFile)
    }

    // Libraries the original app bundled as dex and we therefore do not port from decompiled
    // source (measured: jadx output for them does not compile). The real artifact replaces it.
    useLibrary("org.apache.http.legacy")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// `<import prefix>. <maven coordinate>`, one per line; the prefix is what port_slice.py uses and
// the coordinate is what Gradle resolves. Duplicates are collapsed so media3 can share one prefix.
val portLibraries: List<String> = file("libraries.txt")
    .takeIf { it.isFile }
    ?.readLines()
    ?.map(String::trim)
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?.map { line ->
        val columns = line.split(Regex("\\s+"))
        require(columns.size == 2) { "libraries.txt: expected `<prefix>. <coordinate>`: $line" }
        columns[1]
    }
    ?.distinct()
    ?: emptyList()

// Runtime classes without runtime resources.
//
// `compileOnly` puts these libraries on javac's classpath but not in the APK, so sealed code that
// imports them would fail at class load. Adding the AARs themselves as `implementation` would merge
// their resources into the resource table, which already carries the original copies (measured:
// "Can not add resource ... to table" with androidx.preference). These two transforms take only
// `classes.jar` out of each AAR (or copy a plain JAR), so the APK gets the classes and nothing else.
abstract class ExtractClassesJar : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-classes.jar")
        ZipFile(input).use { zip ->
            val classes = zip.getEntry("classes.jar")
            if (classes == null) {
                // Resource-only AAR: contribute an empty jar instead of failing the build.
                JarOutputStream(output.outputStream()).use { it.putNextEntry(ZipEntry("META-INF/MANIFEST.MF")); it.closeEntry() }
            } else {
                zip.getInputStream(classes).use { source ->
                    output.outputStream().use { target -> source.copyTo(target) }
                }
            }
        }
    }
}

abstract class CopyPlainJar : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        input.copyTo(outputs.file(input.name), overwrite = true)
    }
}

val artifactType = Attribute.of("artifactType", String::class.java)

dependencies {
    portLibraries.forEach { add("compileOnly", it) }

    registerTransform(ExtractClassesJar::class) {
        from.attribute(artifactType, "aar")
        to.attribute(artifactType, "port-classes-jar")
    }
    registerTransform(CopyPlainJar::class) {
        from.attribute(artifactType, "jar")
        to.attribute(artifactType, "port-classes-jar")
    }
}

val portRuntime: Configuration = configurations.create("portRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes { attribute(artifactType, "port-classes-jar") }
}

dependencies {
    // Resolved transitively, so classes pulled by these libraries are packaged too; only the
    // `port-classes-jar` artifact type survives, which is why no AAR resource is merged.
    // The flattened file collection loses module identity, so Gradle's version conflict
    // resolution cannot deduplicate what these libraries pull transitively.
    // - org.jetbrains:annotations arrives twice; CLASS retention, not needed at runtime.
    // - kotlin-stdlib 1.8.10 arrives through androidx while AGP 9.2 already supplies 2.3.10;
    //   both contain kotlin/internal/internal.kotlin_builtins and fail mergeDebugJavaResource.
    portLibraries.forEach { coordinate ->
        add("portRuntime", coordinate) {
            exclude(group = "org.jetbrains", module = "annotations")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            // kotlin-stdlib 2.x absorbed the jdk7/jdk8/common artifacts; old 1.6.21 copies
            // pulled transitively duplicate their classes under the built-in 2.3.10 stdlib.
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        }
    }
    implementation(portRuntime.incoming.files)
}
