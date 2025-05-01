package host.minestudio.frost.api.shards

import com.moandjiezana.toml.Toml
import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * ClassLoader for loading modules
 */
@ApiStatus.Internal
class ShardClassLoader(
    urls: Array<URL?>,
    parentClassLoader: ClassLoader,
    private val dataDir: File?
) : URLClassLoader(urls, parentClassLoader) {

    private val logger = LoggerFactory.getLogger(ShardClassLoader::class.java)

    public override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        try {
            logger.debug("Loading class $name from shard classloader")
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