import java.net.URI

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
    id("maven-publish")
    signing
    kotlin("plugin.serialization") version "2.2.0"
}

group = "gg.wjmag"
version = System.getenv("VERSION") ?: "dev"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))

    // Internal dependencies
    compileOnly(libs.maven.resolver.provider)
    compileOnly(libs.maven.resolver.connector)
    compileOnly(libs.maven.resolver.file)
    compileOnly(libs.maven.resolver.http)
    compileOnly(libs.maven.resolver.classpath)
    compileOnly(libs.ktor.client.cio)
    compileOnly(libs.ktor.client.content.negotiation)
    compileOnly(libs.ktor.serialization.kotlinx.json)
    compileOnly(libs.kotlinx.coroutines.core)

    // Exposed APIs
    api(libs.maven.resolver.impl)
    api(libs.minestom) // MINESTOM SNAPSHOTS
    api(libs.kotlinx.serialization) // JSON SERIALIZER
    api(libs.auto.service) // AUTO SERVICE
    api(libs.boosted.yaml) // YAML PARSER
    api("net.kyori:adventure-text-minimessage:4.24.0")
}


publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/wjmag/frost")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: error("GITHUB_ACTOR environment variable is not set.")
                password = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN environment variable is not set.")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "frost-api"
            version = project.version.toString()

            pom {
                name.set("Frost API")
                description.set("The API for Frost, a Minecraft server software.")
                url.set("https://github.com/wjmag/frost")
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
