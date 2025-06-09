package host.minestudio.frost.api.shards.impl

import host.minestudio.frost.api.shards.helper.StorageService
import host.minestudio.frost.impl.SocketIO
import host.minestudio.frost.socket
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class StorageServiceImpl : StorageService {

    override fun deleteData(keyId: String, keyName: String) {
        socket.emit("data_service:delete", keyId, keyName)
    }

    override fun getData(keyId: String, keyName: String): CompletableFuture<Any?> {
        val future = CompletableFuture<Any?>()
        Thread.ofVirtual().run {
            val obj = object : SocketIO.SocketCallback {
                override fun call(response: JSONObject?) {
                    future.complete(response?.getString("value")) // Complete the future with the response
                }
            }
            socket.emit("data_service:get", obj, keyId, keyName)
        }
        return future
    }

    override fun saveData(keyId: String, keyName: String, value: Any) {
        socket.emit("data_service:set", keyId, keyName, value)
    }

}