package host.minestudio.frost

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import host.minestudio.frost.api.shards.ShardManager
import me.lucko.spark.minestom.SparkMinestom
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerMoveEvent
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


lateinit var SERVER: MinecraftServer
lateinit var logger: Logger

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

    if(System.getenv("BUNGEE") !== null) {
        logger.info("BungeeCord Proxy is enabled. Connecting to BungeeCord.")
        BungeeCordProxy.enable()
    } else if(System.getenv("VELOCITY") !== null) {
        logger.info("Velocity Proxy is enabled. Connecting to Velocity.")
        VelocityProxy.enable(System.getenv("VELOCITY"))
    } else if(System.getenv("OFFLINE") == "I KNOW WHAT IM DOING I SWEAR LET ME USE OFFLINE MODE THANKS OK BYE NOW") {
        logger.warn("Running in offline mode. This is not recommended for any servers, let alone production servers.")
    } else {
        logger.info("No proxy is enabled. Using MojangAuth for authentication.")
        MojangAuth.init()
    }

    logger.info("Setting up the World...")
    setupWorld()

    logger.info("Setting up Events")
    events()

    logger.info("Setting up ShardManager...")
    shardManager.setup(File(System.getProperty("user.dir")))

    MinecraftServer.setBrandName("§bFrost §7(§aMineStudio §efb WJMAG§7)§f")

    SERVER.start("0.0.0.0", System.getenv("PORT")?.toIntOrNull() ?: 25565)
    println("Server started on port ${System.getenv("POST")?.toIntOrNull() ?: 25565}")

    println("")
    println("    __________  ____  ___________")
    println("   / ____/ __ \\/ __ \\/ ___/_  __/")
    println("  / /_  / /_/ / / / /\\__ \\ / /   ")
    println(" / __/ / _, _/ /_/ /___/ // /    ")
    println("/_/   /_/ |_|\\____//____//_/     ")
    println("     -- By MineStudio --  ")
    println("    -- Forked by WJMAG --  ")
}

private fun setupWorld() {
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
    spawnWorld.timeRate = 0
    spawnWorld.time = 6000 //noon
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
        event.spawningInstance = spawnWorld
        event.player.respawnPoint = spawnPoint
    }
    @Suppress("UnstableApiUsage")
    spawnWorld.eventNode().addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        player.teleport(spawnPoint)
    }
    @Suppress("UnstableApiUsage")
    spawnWorld.eventNode().addListener(PlayerMoveEvent::class.java) { event ->
        if (event.player.position.y < 0) {
            event.player.teleport(spawnPoint)
        }
    }
}
