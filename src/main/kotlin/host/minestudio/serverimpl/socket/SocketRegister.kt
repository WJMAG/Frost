package host.minestudio.serverimpl.socket

import host.minestudio.serverimpl.impl.SocketIO
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

class SocketRegister(
    private val socket: SocketIO
) {

    fun register(): Boolean {
        var isAuthenticated = false
        val latch = CountDownLatch(1)

        socket.emit("register subserver", object : SocketIO.SocketCallback {
            override fun call(response: JSONObject?) {
                isAuthenticated = response?.get("success") == true
                println("Socket register response: $response")
                latch.countDown()
            }
        }, JSONObject()
            .put("pod_hostname", System.getenv("POD_IP"))
        )
        latch.await()
        return isAuthenticated
    }

}