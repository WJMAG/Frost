package host.minestudio.frost.api.dependencies.classpath

import java.lang.AutoCloseable
import java.nio.file.Path

interface ClassPathAppender : AutoCloseable {
    fun addJarToClasspath(file: Path?)

    override fun close() {
    }
}