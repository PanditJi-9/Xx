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

tasks.test {
    useJUnit()
    // Print each result: a task that passes because it discovered no tests is a silent lie,
    // and this project has already been bitten by gates that measured nothing.
    testLogging { events("passed", "failed", "skipped") }
}
