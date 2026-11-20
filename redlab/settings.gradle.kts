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
// :hardware — MaxManager port, waiting on its dependency closure (docs/MAXMANAGER_TRANSFER.md §2).
// Not included on purpose: a module that does not compile is not progress, and it would break CI.
