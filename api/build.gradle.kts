import java.net.URI

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
    id("maven-publish")
    signing
    kotlin("plugin.serialization") version "2.2.0"
}

group = "host.minestudio"
version = System.getenv("VERSION") ?: "dev"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Internal dependencies
    compileOnly(libs.maven.resolver.provider)
    compileOnly(libs.maven.resolver.connector)
    compileOnly(libs.maven.resolver.file)
    compileOnly(libs.maven.resolver.http)
    compileOnly(libs.maven.resolver.classpath)

    // Exposed APIs
    api(libs.maven.resolver.impl)
    api(libs.minestom) // MINESTOM SNAPSHOTS
    api(libs.kotlinx.serialization) // JSON SERIALIZER
    api(libs.auto.service) // AUTO SERVICE
}


publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/wjmag/frost")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "wjmag"
                password = System.getenv("GITHUB_TOKEN") ?: "null"
            }
        }
    }
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
