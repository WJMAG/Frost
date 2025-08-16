package host.minestudio.frost.api.shards.impl

import host.minestudio.frost.api.shards.helper.StorageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.concurrent.CompletableFuture

class StorageServiceImpl : StorageService {
    val localCache: MutableMap<String, MutableMap<String, Any?>> = mutableMapOf()
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            if(localCache.isNotEmpty()) {
                logger.warn("Local cache is not empty on shutdown. Saving to file.")
                localCache.forEach { (storeName, data) ->
                    val file = File("store/$storeName.yaml")
                    file.parentFile.mkdirs()
                    val yaml = Yaml()
                    file.writeText(yaml.dump(data))
                }
            }
        })

        // Load local cache from files
        val storeDir = File("store")
        if (storeDir.exists() && storeDir.isDirectory) {
            storeDir.listFiles()?.forEach { file ->
                if (file.extension == "yaml") {
                    val yaml = Yaml()
                    val data: MutableMap<String, Any?> = yaml.load(file.inputStream()) ?: mutableMapOf()
                    localCache[file.nameWithoutExtension] = data
                }
            }
        }
    }

    override fun deleteData(storeName: String, key: String) {
        localCache[storeName]?.remove(key)
    }

    override fun getData(storeName: String, key: String): CompletableFuture<Any?> {
        val future = CompletableFuture<Any?>()
        future.complete(localCache[storeName]?.get(key))
        return future
    }

    override fun getDataSync(storeName: String, key: String): Any? {
        return localCache[storeName]?.get(key)
    }

    override fun saveData(storeName: String, key: String, value: Any) {
        localCache.computeIfAbsent(storeName) { mutableMapOf() }[key] = value
    }
}