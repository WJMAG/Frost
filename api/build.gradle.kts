import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import java.time.Duration


plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.vanniktech.maven.publish") version "0.32.0"
    signing
}

group = "host.minestudio"
version = System.getenv("VERSION") ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly("net.minestom:minestom-snapshots:87f6524aeb") // MINESTOM

    compileOnly("org.apache.maven:maven-resolver-provider:3.9.9")
    compileOnly("org.apache.maven.resolver:maven-resolver-impl:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-file:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-classpath:1.9.22")
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

mavenPublishing {
    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true
    ))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    coordinates("host.minestudio", "frost-snapshots", project.version.toString())



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
    }
}