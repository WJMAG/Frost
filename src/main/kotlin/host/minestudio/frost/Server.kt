package host.minestudio.frost

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import host.minestudio.frost.api.shards.ShardManager
import host.minestudio.frost.impl.ConnSet
import host.minestudio.frost.impl.SocketIO
import host.minestudio.frost.impl.SocketListener
import host.minestudio.frost.util.request
import io.socket.client.Ack
import me.lucko.spark.minestom.SparkMinestom
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.bungee.BungeeCordProxy
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.block.Block
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.system.exitProcess


lateinit var SERVER: MinecraftServer
var socket: SocketIO? = null
lateinit var logger: org.slf4j.Logger

lateinit var spawnWorld: InstanceContainer

val shardManager = ShardManager()

fun main() {
    val isDebug = System.getProperty("frost.debug") == "true"
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    if(isDebug) {
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).level = Level.DEBUG
        loggerContext.getLogger("host.minestudio.frost").level = Level.DEBUG
    }

    logger = LoggerFactory.getLogger("host.minestudio.frost.MinestudioServer")
    SERVER = MinecraftServer.init()
    val directory = Path.of("spark")
    SparkMinestom.builder(directory)
        .commands(true) // enables registration of Spark commands
        .permissionHandler { sender, permission -> true } // allows all command senders to execute all commands
        .enable()

    if(false) {
        logger.info("Online Mode is enabled. Connecting to the MineStudio API.")
        val apiHost = "".replace("http://", "").replace("https://", "")
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
            "".startsWith("https://")
        )!!
        socket!!.register(object: SocketListener {
            override fun listen(objects: Array<Any?>?, callback: Ack?) {
                val firstObject = objects?.firstOrNull() ?: throw IllegalArgumentException("First object cannot be null")
                if(firstObject is JSONObject) {
                    val json = firstObject
                    val sessionId = json.getJSONObject("payload").getString("session_id")
                    if(sessionId === null) throw IllegalArgumentException("Session ID cannot be null")
                    val req = request(
                        "/api/v1/registration/sidecar",
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

        socket!!.io!!.on("heartbeat") { args ->
            val ack = args.lastOrNull() as? Ack

        }
    } else {
        logger.info("Online Mode is disabled. Not authenticating with MineStudio API. (Forcefully disabled)")
    }

    if(System.getenv("BUNGEE") !== null) {
        logger.info("BungeeCord Proxy is enabled. Connecting to BungeeCord.")
        BungeeCordProxy.enable()
    } else if(System.getenv("VELOCITY") !== null) {
        logger.info("Velocity Proxy is enabled. Connecting to Velocity.")
        VelocityProxy.enable(System.getenv("VELOCITY"))
    } else if(System.getenv("OFFLINE") !== null) {
        logger.warn("Running in offline mode. This is not recommended for any servers, let alone production servers.")
    } else {
        logger.info("No proxy is enabled. Using MojangAuth for authentication.")
        MojangAuth.init()
    }

    logger.info("Setting up the Demo World...")
    demoWorld()

    logger.info("Setting up Events")
    events()

    logger.info("Setting up ShardManager...")
    shardManager.setup(File(System.getProperty("user.dir")))

    MinecraftServer.setBrandName("§bFrost §7(§aMineStudio§7)§f")

    SERVER.start("0.0.0.0", System.getenv("PORT")?.toIntOrNull() ?: 25565)
    println("Server started on port ${System.getenv("POST")?.toIntOrNull() ?: 25565}")

    println("")
    println("    __________  ____  ___________")
    println("   / ____/ __ \\/ __ \\/ ___/_  __/")
    println("  / /_  / /_/ / / / /\\__ \\ / /   ")
    println(" / __/ / _, _/ /_/ /___/ // /    ")
    println("/_/   /_/ |_|\\____//____//_/     ")
    println("     -- By MineStudio --  ")
}

private fun demoWorld() {
    spawnWorld = MinecraftServer.getInstanceManager().createInstanceContainer()
    val worldDir = Path("world").toFile()
    if(!worldDir.exists()) {
        spawnWorld.setBlock(0, 64, 0, Block.GRASS_BLOCK)
        spawnWorld.setChunkSupplier(::LightingChunk)
    } else {
        val loader = AnvilLoader(worldDir.toPath())
        spawnWorld.chunkLoader = loader
        spawnWorld.setChunkSupplier(::LightingChunk)
    }
}

private fun events() {
    val spawnPointFile = File("points/spawn.json")
    var spawnPoint = Pos(0.5, 66.0, 0.5)
    if(spawnPointFile.exists()) {
        val json = JSONObject(spawnPointFile.readText())
        val x = json.getDouble("x")
        val y = json.getDouble("y")
        val z = json.getDouble("z")
        val yaw = json.getDouble("yaw").toFloat()
        val pitch = json.getDouble("pitch").toFloat()
        spawnPoint = Pos(x, y, z, yaw, pitch)
    }
    MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        event.spawningInstance = spawnWorld
        event.player.respawnPoint = spawnPoint
    }
    spawnWorld.eventNode().addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        player.teleport(spawnPoint)
    }
}
