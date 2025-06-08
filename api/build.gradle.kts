import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost


plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.vanniktech.maven.publish") version "0.32.0"
    signing
    kotlin("plugin.serialization") version "1.9.0"
}

group = "host.minestudio"
version = System.getenv("VERSION") ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly("net.minestom:minestom-snapshots:87f6524aeb") // MINESTOM

    compileOnly("org.apache.maven:maven-resolver-provider:3.9.10")
    compileOnly("org.apache.maven.resolver:maven-resolver-impl:1.9.23")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-file:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.23")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-classpath:1.9.22")

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // JSON SERIALIZER
}

mavenPublishing {
    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true
    ))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    coordinates("host.minestudio", "frost-snapshots", project.version.toString())

    signAllPublications()

    pom {
        name.set("Frost API")
        description.set("A library for creating and managing Minestom servers.")
        inceptionYear.set("2025")
        url.set("https://www.minestudio.host")

        developers {
            developer {
                id.set("cammyzed")
                name.set("Cam M")
                url.set("https://expx.dev")
                email.set("cam@expx.dev")
            }
            developer {
                id.set("thecodingduck")
                name.set("Colton H")
                email.set("zcoltonhirsch@gmail.com")
            }
        }
        licenses {
            license {
                name.set("No License")
                url.set("https://www.minestudio.host/")
            }
        }
        scm {
            url.set("scm:git:github.com/MineStudio-Host/Frost.git")
            developerConnection.set("scm:git:ssh://github.com/MineStudio-Host/Frost.git")
            connection.set("scm:git:github.com/MineStudio-Host/Frost.git")
        }
    }
}

signing {
    val privateKey = System.getenv("GPG_PRIVATE_KEY")
    val keyPassphrase = System.getenv("GPG_PRIVATE_KEY_PASSPHRASE")
    if (privateKey != null && keyPassphrase != null) {
        useInMemoryPgpKeys(privateKey, keyPassphrase)
    } else {
        logger.warn("GPG keys not found in environment variables. Signing will be skipped.")
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