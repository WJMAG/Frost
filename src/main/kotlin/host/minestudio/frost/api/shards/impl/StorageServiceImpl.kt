package host.minestudio.frost.api.shards.impl

import host.minestudio.frost.api.shards.helper.StorageService
import host.minestudio.frost.impl.SocketIO
import host.minestudio.frost.logger
import host.minestudio.frost.onlineMode
import host.minestudio.frost.socket
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml
import java.util.concurrent.CompletableFuture

class StorageServiceImpl : StorageService {
    val localCache: MutableMap<String, MutableMap<String, Any?>> = mutableMapOf()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            if(localCache.isNotEmpty()) {
                logger.warn("Local cache is not empty on shutdown. Saving to file.")
                localCache.forEach { (storeName, data) ->
                    val file = java.io.File("store/$storeName.yaml")
                    file.parentFile.mkdirs()
                    val yaml = Yaml()
                    file.writeText(yaml.dump(data))
                }
            }
        })

        if(!onlineMode) {
            // Load local cache from files
            val storeDir = java.io.File("store")
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
    }

    override fun deleteData(storeName: String, key: String) {
        if(onlineMode)
            socket!!.emit("data_service:delete", storeName, key)
        else
            localCache[storeName]?.remove(key)
    }

    override fun getData(storeName: String, key: String): CompletableFuture<Any?> {
        val future = CompletableFuture<Any?>()
        if (onlineMode) {
            Thread.ofVirtual().run {
                val obj = object : SocketIO.SocketCallback {
                    override fun call(response: JSONObject?) {
                        future.complete(response?.getString("value"))
                    }
                }
                socket!!.emit("data_service:get", obj, storeName, key)
            }
        } else {
            future.complete(localCache[storeName]?.get(key))
        }
        return future
    }

    override fun getDataSync(storeName: String, key: String): Any? {
        return if (onlineMode) {
            getData(storeName, key).get()
        } else {
            localCache[storeName]?.get(key)
        }
    }

    override fun saveData(storeName: String, key: String, value: Any) {
        if(onlineMode) {
            socket!!.emit("data_service:set", storeName, key, value)
        } else {
            localCache.computeIfAbsent(storeName) { mutableMapOf() }[key] = value
        }
    }
}