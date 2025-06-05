/**
 * MIT License
 *
 * Copyright (c) 2024 Cam
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package host.minestudio.frost.impl

import io.socket.client.Ack
import io.socket.client.AckWithTimeout
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.jetbrains.annotations.ApiStatus
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.exitProcess


/**
 * SocketIO is a program that allows for
 * transferring data between servers with
 * a callback.
 */
@ApiStatus.Experimental
@Suppress("unused")
object SocketIO {
    val l: Logger = LoggerFactory.getLogger(SocketIO::class.java)

    var io: Socket? = null
    private val pendingRegistration: MutableList<SocketListener> = ArrayList<SocketListener>()

    private val registry: Registry<String, Emitter.Listener> = EclipseStore<String, Emitter.Listener>()
    private var isApiRebooting = false
    private var connectionAttempts = 0
    private const val RECONNECT_JITTER_MAX_MS = 5000
    private const val MAX_RETRIES = 3

    /**
     * Connects to a socket server.
     *
     * @param connSet The connection data.
     * @param secure  Whether the connection is secure.
     * @return [Socket] The socket.
     */
    @Suppress("unused")
    fun connect(connSet: ConnSet, secure: Boolean): SocketIO? {

        try {
            val opts = IO.Options().apply {
                reconnection = false
                timeout = 1000 // single second
                transports = arrayOf("websocket")
                query = "pod_id=${System.getenv("POD_ID")}"
            }

            println("Connecting to socket server: ${if (secure) "https" else "http"}://${connSet.ip}:${connSet.port}")
            io = IO.socket(if (secure) {
                "https://${connSet.ip}:${connSet.port}/server"
            } else {
                "http://${connSet.ip}:${connSet.port}/server"
            }, opts)
            io!!.connect()
            Thread.ofVirtual().name("SocketIO").start(Runnable {
                for (listener in pendingRegistration) {
                    registry.register(listener.channel!!, Emitter.Listener { obj: Array<Any?>? ->
                        if (Arrays.stream<Any?>(obj).toList().last() is Ack) {
                            listener.listen(obj, obj?.lastOrNull() as Ack)
                        } else {
                            listener.listen(obj, null)
                        }
                    })
                }
            })
            io!!.onAnyIncoming { args ->
                val event = args[0] as String
                println("[-----] Received event: $event")
                val handler = registry[event]
                if(handler.isPresent) {
                    // all but first argument
                    handler.get().call(*args.drop(1).toTypedArray())
                }
            }
            io!!.onAnyOutgoing { args ->
                val event = args[0] as String
                println("[-----] Emitting event: $event")
            }
            socketEvents(io!!)
            return this
        } catch (e: Exception) {
            throw Error("Failed to connect to socket server: " + e.message)
        }
    }

    /**
     * Registers a listener to the socket.
     *
     * @param listener [SocketListener] The listener to register.
     */
    @Suppress("unused")
    fun register(listener: SocketListener) {
        if (io != null) {
            println("Registering listener ${listener.name}")
            registry.register(listener.channel!!, Emitter.Listener { obj: Array<Any?>? ->
                if (Arrays.stream<Any?>(obj).toList().last() is Ack) {
                    listener.listen(obj, obj?.lastOrNull() as? Ack)
                } else {
                    listener.listen(obj, null)
                }
            })
        } else {
            println("SocketIO is not connected. Adding listener ${listener.name} to pending registration")
            pendingRegistration.add(listener)
        }
    }

    /**
     * Emits a socket event.
     *
     * @param channel The channel to emit to.
     * @param args    The arguments to emit.
     *
     */
    @Suppress("unused")
    fun emit(channel: String, vararg args: Any?) {
        if (io != null) {
            io!!.emit(channel, *args)
        } else {
            l.error("SocketIO is not connected.")
        }
    }

    /**
     * Emits a socket event with a callback.
     *
     * @param channel The channel to emit to.
     * @param args    The arguments to emit.
     * @param callback The callback to call.
     *
     */
    @Suppress("unused")
    fun emit(channel: String, callback: SocketCallback, vararg args: Any?) {
        if (io != null) {
            io!!.emit(channel, args, SocketAcknowledgement(callback))
        } else {
            l.error("SocketIO is not connected.")
        }
    }

    @FunctionalInterface
    interface SocketCallback {
        fun call(response: JSONObject?)
    }

    class SocketAcknowledgement(private val cb: SocketCallback) : AckWithTimeout(10000) {
        override fun onTimeout() {
            println("[DEBUG] Socket acknowledgement timed out")
            cb.call(JSONObject().put("success", false))
        }

        override fun onSuccess(vararg args: Any?) {
            val data = args.firstOrNull()
            val response = when (data) {
                is Boolean -> JSONObject().put("success", data)
                is JSONObject -> data.put("success", true)
                is String -> JSONObject(data).put("success", true)
                else -> null
            }
            cb.call(response)
        }
    }

    private fun socketEvents(io: Socket) {
        io.on("connect") { _ ->
            connectionAttempts = 0
            isApiRebooting = false
            l.info("[SOCKET] Connected to API")
        }

        io.on("api-rebooting") { _ ->
            isApiRebooting = true
            l.info("[SOCKET] API reboot notification received")
            io.disconnect() // Will trigger disconnect handler
        }

        io.on("disconnect") { reason ->
            if (isApiRebooting) handleApiReboot()
            else handleUnexpectedDisconnect(reason.firstOrNull()?.toString())
        }

        // Keep existing handlers
        io.on("error") { args -> l.error("[SOCKET] Error: ${args.firstOrNull()}") }
        io.on("reconnect") { attempt -> l.info("[SOCKET] Reconnected (attempt $attempt)") }
    }

    private fun handleApiReboot() {
        val delay = 5000L + (0..RECONNECT_JITTER_MAX_MS).random()
        l.info("[SOCKET] API reboot detected - reconnecting in ${delay}ms")

        Thread.ofVirtual().start {
            Thread.sleep(delay)
            io?.connect() // Fresh connection attempt
        }
    }

    private fun handleUnexpectedDisconnect(reason: String?) {
        if (++connectionAttempts > MAX_RETRIES) {
            l.error("[SOCKET] Max retries reached ($MAX_RETRIES) - terminating pod")
            exitProcess(1)
        }

        val delay = connectionAttempts * 1000L + (0..RECONNECT_JITTER_MAX_MS).random()
        l.warn("[SOCKET] Disconnected (attempt $connectionAttempts/$MAX_RETRIES) - retrying in ${delay}ms")

        Thread.ofVirtual().start {
            Thread.sleep(delay)
            io?.connect()
        }
    }
}