package host.minestudio.frost.api.shards

import host.minestudio.frost.shardManager
import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader

@ApiStatus.Internal
class ShardClassLoader(
    urls: Array<URL?>,
    parentClassLoader: ClassLoader,
    manager: ShardManager
) : URLClassLoader(urls.filterNotNull().toTypedArray(), parentClassLoader) {

    var shard: Shard? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("Shard is already set for this classloader")
            }
            field = value
        }
        get() {
            return field ?: throw IllegalStateException("Shard is not set for this classloader")
        }
    private val logger = LoggerFactory.getLogger(ShardClassLoader::class.java)
    private val cachedClasses = mutableMapOf<String, Class<*>>()

    override fun findClass(name: String): Class<*>? {
        return findClass(name, true)
    }

    private fun findClass(name: String, resolve: Boolean): Class<*> {
        if (cachedClasses.containsKey(name)) return cachedClasses[name]!!

        if (resolve) {
            if (cachedParentClasses.containsKey(name)) return cachedParentClasses[name]!!
            for (shard in shardManager.shards.values) {
                try {
                    val clazz = shard.getClassLoader()!!.findClass(name, false)
                    val loader = clazz.classLoader
                    if (loader is ShardClassLoader) {
                        loader.shard!!.getInfo()?.let {
                            if (!(shard.getInfo()?.deps?.contains(it.id) ?: false) && (it.id != (shard.getInfo()?.id
                                    ?: false))
                            ) {
                                logger.warn("Class {} is loaded by shard {}, but it is not a dependency of the current shard {}", name, it.id, this.shard?.getInfo()?.id ?: "UNKNOWN")
                            }
                        }
                    }
                    cachedClasses[name] = clazz
                    return clazz
                } catch (_: ClassNotFoundException) {}
            }
        }

        val clazz = super.findClass(name) ?: throw ClassNotFoundException("Class $name not found in any shard or parent classloader")
        cachedParentClasses[name] = clazz
        return clazz
    }

    fun addDependencyURL(url: URL) {
        logger.debug("Adding dependency URL: {}", url)
        addURL(url)
        logger.debug("All URLs after addition: {}", urLs.joinToString(", ") { it.toString() })
    }

    private companion object {
        val cachedParentClasses = HashMap<String, Class<*>>()
    }
}