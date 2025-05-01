package host.minestudio.frost.api.shards

import host.minestudio.frost.api.dependencies.resolver.MavenResolver
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.ServiceLoader

val PLUGIN_REGEX = Regex("[0-9a-z-]+")

class ShardManager {

    private val logger = LoggerFactory.getLogger(ShardManager::class.java)
    private val shards = HashMap<String, Shard>()


    protected lateinit var shardLoader: URLClassLoader

    fun setup(dataDir: File) {
        try {
            setupShardLoader(dataDir.toPath())
            loadModules(dataDir.toPath())
        } finally {
            installModules()
        }
    }

    fun setupShardLoader(dataDir: Path) {
        try {
            val shardDir = File(dataDir.toFile(), "shards")
            if(!shardDir.exists() || !shardDir.isDirectory) {
                val success = shardDir.mkdirs()
                if(!success) {
                    throw IllegalStateException("Failed to create shard directory: ${shardDir.absolutePath}")
                }
            }
            if(shardDir.listFiles() == null) return
            val jars = requireNotNull(shardDir.listFiles())
            val urls = arrayOfNulls<URL>(jars.size)
            for ((index, jar) in jars.withIndex()) {
                urls[index] = jar.toURI().toURL()
            }
            shardLoader = ShardClassLoader(urls, this::class.java.classLoader, dataDir.toFile())
        } catch (e: Exception) {
            logger.error("Failed to setup shard loader", e)
        }
    }


    fun loadModules(dataDir: Path) {
        try {
            val currentLoader = Thread.currentThread().contextClassLoader
            val shardClass = Shard::class.java

            val serviceLoader: ServiceLoader<Shard> = ServiceLoader.load(shardClass, shardLoader)

            for(shard in serviceLoader) {
                Thread.currentThread().contextClassLoader = shardLoader
                shard.loader = ShardClassLoader(arrayOf(shard.jarLocation), ShardManager::class.java.classLoader, dataDir.toFile())
                shard.info = shard.loader.shardInfo
                shard.dataFolder = dataDir.toFile()
                Thread.currentThread().contextClassLoader = currentLoader
                if(shard.info == null) {
                    logger.warn("Shard info is null, please check your module.toml file")
                } else {
                    logger.info("Loading shard ${shard.info?.name} v${shard.info?.version}")
                    shards.put(shard.info?.id!!, shard)
                }

            }
        } catch (e: Exception) {
            logger.error("Failed to load shards", e)
        }
    }


    fun loadDependencies(res: MavenResolver, dir: Path) {
        try {
            val currentLoader = Thread.currentThread().contextClassLoader
            val depClass = ShardDependencyLoader::class.java

            val serviceLoader: ServiceLoader<ShardDependencyLoader> = ServiceLoader.load(depClass, shardLoader)

            for (loader in serviceLoader) {
                Thread.currentThread().contextClassLoader = shardLoader
                loader.loadDependencies(res)
                Thread.currentThread().contextClassLoader = currentLoader
            }
        } catch (e: Exception) {
            logger.error("Failed to load shard dependencies", e)
        }
    }


    fun installModules() {
        for(shard in shards.values) {
            try {
                val info = shard.info!!
                logger.info("Installing shard ${info.name} v${info.version}")
                try {
                    if(info.deps != null && !info.deps!!.isEmpty()) {
                        for(dep in info.deps!!) {
                            val depend = shards[dep]
                            if(depend == null) {
                                logger.error("Failed to install shard ${info.name}, dependency $dep not found")
                                continue
                            }
                            if(depend.info!!.deps?.contains(info.id) == true) {
                                logger.error("Failed to install shard ${info.name}, circular dependency detected with $dep")
                                continue
                            }
                            if(!depend.isSetup) {
                                depend.create()
                                depend.isSetup = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to install shard ${info.name}, dependency check failed", e)
                }

                try {
                    shard.create()
                    shard.isSetup = true
                } catch (e: Exception) {
                    logger.error("Failed to install shard ${info.name}", e)
                }

                logger.info("Shard ${info.name} v${info.version} installed")
            } catch (e: Exception) {
                logger.error("Failed to install shard ${shard.info?.name}", e)
            }
        }
    }


    fun trashModules() {
        for(shard in shards.values) {
            try {
                shard.delete()
            } catch (e: Exception) {
                logger.error("Failed to trash shard ${shard.info?.name}", e)
            }
        }
    }

}