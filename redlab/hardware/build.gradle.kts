plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // MaxManager persists a little JSON (locks, route memory) with org.json; keeping the same
    // calls keeps the ported logic byte-identical to the measured original.
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test { useJUnit() }
