package host.minestudio.serverimpl.socket

import host.minestudio.serverimpl.impl.SocketIO
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

class SocketAuth(
    private val socket: SocketIO
) {

    fun authenticate(): Boolean {
        var isAuthenticated = false
        val latch = CountDownLatch(1)

        socket.emit("auth", object : SocketIO.SocketCallback {
            override fun call(response: JSONObject?) {
                isAuthenticated = response?.get("success") == true
                println("Socket auth response: $response")
                latch.countDown()
            }
        }, JSONObject()
            .put("pod_ip", System.getenv("POD_IP"))
            .put("server_id", System.getenv("SERVER_ID"))
            .put("token", System.getenv("EXCHANGE_TOKEN") ?: "null")
            .put("type", "SUBSERVER")
        )
        latch.await()
        return isAuthenticated
    }
}