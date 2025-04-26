plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "host.minestudio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("net.minestom:minestom-snapshots:87f6524aeb")             // MINESTOM

    implementation("org.eclipse.collections:eclipse-collections:12.0.0.M3")  // ECLIPSE COLLECTIONS
    implementation("io.socket:socket.io-client:2.1.2")                       // SOCKET.IO
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "host.minestudio.serverimpl.ServerKt"
        }
    }
    build {
        dependsOn("shadowJar")
    }
    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("")
        archiveFileName.set("${project.name}-${project.version}.jar")
        doLast {
            val targetDir = file("$rootDir/.run/").apply { mkdirs() }

            archiveFile.get().asFile.apply {
                copyTo(
                    target = file("$targetDir/$name"),
                    overwrite = true
                )
            }
        }
    }
}