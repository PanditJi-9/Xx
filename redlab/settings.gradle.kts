pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RedLabEcosystem"

// Phase 5 — foundation only. Application modules (`:gamespace`, `:gameassist`, …) are
// added in phase 6+ per docs/ECOSYSTEM_MODULES.md §6, one at a time.
include(":core")
include(":hardware")
