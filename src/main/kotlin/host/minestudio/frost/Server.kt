package host.minestudio.frost

import host.minestudio.frost.api.shards.ShardManager
import host.minestudio.frost.impl.ConnSet
import host.minestudio.frost.impl.SocketIO
import host.minestudio.frost.socket.SocketAuth
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.bungee.BungeeCordProxy
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

lateinit var SERVER: MinecraftServer
lateinit var socket: SocketIO
lateinit var logger: org.slf4j.Logger

lateinit var spawnWorld: InstanceContainer

const val onlineMode = false

fun main() {
    logger = org.slf4j.LoggerFactory.getLogger("MinestudioServer")
    SERVER = MinecraftServer.init()

    if(onlineMode) {
        val gatewayHost = System.getenv("GATEWAY_HOST").replace("http://", "").replace("https://", "")
        socket = SocketIO.connect(
            ConnSet(
                gatewayHost.split(":")[0],
                // if there's a port, use it. Will be added to the url after a colon. Not an env variable
                if (gatewayHost.contains(":")) {
                    gatewayHost.split(":")[1].toInt()
                } else {
                    80
                }
            ),
            false
        )!!
        socket.io!!.on("ready") { args ->
            Thread.ofVirtual().name("Setup Socket").start {
                val auth = SocketAuth(socket).authenticate()
                if (!auth) {
                    logger.error("Failed to authenticate with the API. Killing process.")
                    exitProcess(1)
                } else {
                    logger.info("Authenticated with the GatewayAPI.")
                }
            }
        }

        if(System.getenv("BUNGEE") !== null) {
            BungeeCordProxy.enable()
        } else if(System.getenv("VELOCITY") !== null) {
            VelocityProxy.enable(System.getenv("VELOCITY"))
        } else {
            MojangAuth.init()
        }
    }



    demoWorld()
    events()

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