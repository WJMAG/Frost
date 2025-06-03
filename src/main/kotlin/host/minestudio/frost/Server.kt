package host.minestudio.frost

import host.minestudio.frost.api.shards.ShardManager
import host.minestudio.frost.impl.ConnSet
import host.minestudio.frost.impl.SocketIO
import host.minestudio.frost.impl.SocketListener
import host.minestudio.frost.util.request
import io.sentry.Sentry
import io.socket.client.Ack
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.bungee.BungeeCordProxy
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

lateinit var SERVER: MinecraftServer
lateinit var socket: SocketIO
lateinit var logger: org.slf4j.Logger

lateinit var spawnWorld: InstanceContainer

const val onlineMode = true
val API_URL = "${System.getenv("API_HOST")}"

fun main() {
    if(API_URL.isEmpty() || System.getenv("API_HOST") == null) {
        throw IllegalArgumentException("API_HOST environment variable is not set. Please set it to the MineStudio API URL.")
    }
    logger = org.slf4j.LoggerFactory.getLogger("MinestudioServer")
    SERVER = MinecraftServer.init()

    logger.info("Setting up Sentry...")
    Sentry.init { options ->
        options.dsn = "https://bb8e8d5a1c3b49faa0d189ace27d4bd9@sentry.minestudio.dev/5"
    }

    if(onlineMode) {
        logger.info("Online Mode is enabled. Connecting to the MineStudio API.")
        val apiHost = API_URL.replace("http://", "").replace("https://", "")
        socket = SocketIO.connect(
            ConnSet(
                apiHost.split(":")[0],
                // if there's a port, use it. Will be added to the url after a colon. Not an env variable
                if (apiHost.contains(":")) {
                    apiHost.split(":")[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid port number")
                } else {
                    if (System.getenv("API_HOST").startsWith("https://")) {
                        443
                    } else {
                        80
                    }
                }
            ),
            API_URL.startsWith("https://")
        )!!
        socket.register(object: SocketListener {
            override fun listen(objects: Array<Any?>?, callback: Ack?) {
                val firstObject = objects?.firstOrNull() ?: throw IllegalArgumentException("First object cannot be null")
                if(firstObject is JSONObject) {
                    val json = firstObject
                    val sessionId = json.getJSONObject("payload").getString("session_id")
                    if(sessionId === null) throw IllegalArgumentException("Session ID cannot be null")
                    val req = request(
                        "$API_URL/api/v1/registration/sidecar",
                        "POST",
                        body = JSONObject().apply {
                            put("ws_id", sessionId)
                            put("server_id", System.getenv("SERVER_ID"))
                            put("to_register", System.getenv("TO_REGISTER")?.toBoolean() == true)
                            put("pod_ip", System.getenv("POD_IP"))
                            put("pod_id", System.getenv("HOSTNAME"))
                        }.toString().toByteArray()
                    )
                    logger.info("Registration response: $req")
                    if(req.get("success").asBoolean) {
                        logger.info("Successfully registered with the API.")
                    } else {
                        logger.error("Failed to register with the API. Killing process.")
                        exitProcess(1)
                    }
                }
            }

            override val name: String?
                get() = "welcome"
            override val channel: String?
                get() = "welcome"

        })

        if(System.getenv("BUNGEE") !== null) {
            logger.info("BungeeCord Proxy is enabled. Connecting to BungeeCord.")
            BungeeCordProxy.enable()
        } else if(System.getenv("VELOCITY") !== null) {
            logger.info("Velocity Proxy is enabled. Connecting to Velocity.")
            VelocityProxy.enable(System.getenv("VELOCITY"))
        } else {
            logger.info("No proxy is enabled. Using MojangAuth for authentication.")
            MojangAuth.init()
        }
    }

    logger.info("Setting up the Demo World...")
    demoWorld()

    logger.info("Setting up Events")
    events()

    logger.info("Setting up ShardManager...")
    ShardManager().setup(File(System.getProperty("user.dir")))

    MinecraftServer.setBrandName("§bFrost §7(§aMineStudio§7)§f")

    SERVER.start("0.0.0.0", 25565)
    println("Server started on port 25565")
}

private fun demoWorld() {
    spawnWorld = MinecraftServer.getInstanceManager().createInstanceContainer()
    spawnWorld.setBlock(Pos(0.0, 64.0, 0.0), Block.STONE)
}

private fun events() {
    MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        event.spawningInstance = spawnWorld
        event.player.respawnPoint = Pos(0.5, 66.0, 0.5)
    }
    MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        player.teleport(Pos(0.5, 66.0, 0.5))
        player.sendMessage("Welcome to a Minestudio SubServer!")
        player.sendMessage("This is a demo world.")
    }
}
