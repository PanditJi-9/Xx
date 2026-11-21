// Versions deliberately match the ones already proven to resolve in this repository's CI
// (manager/gradle/libs.versions.toml): Kotlin 2.3.10, Gradle 9.5.1.
//
// `:core` is a plain JVM library on purpose: the capability model must be testable with no
// device and no Android SDK, because a device is exactly what CI does not have and the
// safety rules are the part that must never go unverified.
plugins {
    kotlin("jvm") version "2.3.10" apply false
    id("com.android.library") version "9.2.0" apply false
    id("com.android.application") version "9.2.0" apply false
}
