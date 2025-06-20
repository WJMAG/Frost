package host.minestudio.frost.api.shards

import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader

@ApiStatus.Internal
class ShardClassLoader(
    urls: Array<URL?>,
    parentClassLoader: ClassLoader,
    private val dataDir: File?
) : URLClassLoader(urls.filterNotNull().toTypedArray(), parentClassLoader) {

    private val logger = LoggerFactory.getLogger(ShardClassLoader::class.java)
    private val classCache = mutableMapOf<String, Class<*>>()

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            logger.debug("Attempting to load class: $name through ${urLs.size} loaders")

            var loadedClass = findLoadedClass(name)
            if (loadedClass != null) return loadedClass

            loadedClass = classCache[name]
            if (loadedClass != null) return loadedClass

            try {
                loadedClass = findClass(name).also {
                    logger.debug("Found class in shard: $name")
                    if (resolve) resolveClass(it)
                }
                classCache[name] = loadedClass
                return loadedClass
            } catch (e: ClassNotFoundException) {
                // Not found in this classloader, continue
            }

            try {
                loadedClass = super.loadClass(name, resolve).also {
                    logger.debug("Loaded class from parent: $name")
                }
                classCache[name] = loadedClass
                return loadedClass
            } catch (e: ClassNotFoundException) {
                logger.error("Failed to load class: $name", e)
                throw e
            }
        }
    }

    override fun findClass(name: String): Class<*> {
        logger.trace("Finding class: $name")
        return try {
            super.findClass(name).also {
                logger.debug("Found class in shard: $name")
            }
        } catch (e: ClassNotFoundException) {
            logger.trace("Class not found in shard: $name")
            throw e
        }
    }

    /**
     * Helper method to ensure proper classloader context for operations
     */
    fun <T> withClassLoaderContext(block: () -> T): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = this
            return block()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    fun addDependencyURL(url: URL) {
        logger.debug("Adding dependency URL: {}", url)
        addURL(url)
        logger.debug("All URLs after addition: {}", urLs.joinToString(", ") { it.toString() })
    }
}