plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "host.minestudio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly("net.minestom:minestom-snapshots:87f6524aeb") // MINESTOM
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.build {
    dependsOn("dokkaHtml")
}