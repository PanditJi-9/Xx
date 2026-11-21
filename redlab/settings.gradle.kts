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

// P1 (docs/MASTER_PLAN.md §8): `:apps:gamespace` is the first application module, built from the
// original materials staged by tools/port_materials.py. The other apps are added the same way,
// one at a time, each proof standing on its own.
include(":core")
include(":hardware")
include(":apps:gamespace")
include(":apps:gameassist")
