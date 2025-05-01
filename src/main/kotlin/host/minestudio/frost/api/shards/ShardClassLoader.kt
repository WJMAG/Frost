package host.minestudio.frost.api.shards

import com.moandjiezana.toml.Toml
import jdk.internal.module.ModuleInfo
import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * ClassLoader for loading modules
 */
@ApiStatus.Internal
class ShardClassLoader
/**
 * Constructor
 * @param urls URLs to load
 * @param dataDir Data directory
 * @param parentClassLoader Parent classloader
 */(
    urls: Array<URL?>,
    /**
     * Parent classloader
     */
    private val parentClassLoader: ClassLoader, private val dataDir: File?
) : URLClassLoader(urls, null) {
    /**
     * Load a class
     * @param name
     * The [binary name](#binary-name) of the class
     *
     * @param resolve
     * If `true` then resolve the class
     *
     * @return The [Class] object
     */
    public override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        try {
            var loadedClass = findLoadedClass(name)
            if (loadedClass == null) {
                loadedClass = super.loadClass(name, resolve)
            }
            if (resolve) super.resolveClass(loadedClass)
            return loadedClass
        } catch (ex: ClassNotFoundException) {
            LoggerFactory.getLogger(ShardClassLoader::class.java)
                .error("Failed to load class $name", ex)

            return null
        }
    }

    val shardInfo: ShardInfo?
        /**
         * Get the module info
         * @return A [ModuleInfo] object
         */
        get() {
            val `is` = getResourceAsStream("module.toml")
            if (`is` == null) {
                LoggerFactory.getLogger(ShardClassLoader::class.java)
                    .warn("Module info not found in ${dataDir?.absolutePath}")
                return null
            }
            return Toml().read(`is`).to(ShardInfo::class.java)
        }
}