package host.minestudio.serverimpl

import host.minestudio.serverimpl.impl.ConnSet
import host.minestudio.serverimpl.impl.SocketIO
import host.minestudio.serverimpl.socket.SocketAuth
import host.minestudio.serverimpl.socket.SocketRegister
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import kotlin.system.exitProcess

lateinit var SERVER: MinecraftServer
lateinit var socket: SocketIO
lateinit var logger: org.slf4j.Logger

lateinit var spawnWorld: InstanceContainer

fun main() {
    logger = org.slf4j.LoggerFactory.getLogger("MinestudioServer")
    SERVER = MinecraftServer.init()

    val gatewayHost = System.getenv("GATEWAY_HOST").replace("http://", "").replace("https://", "")
    socket = SocketIO.connect(
        ConnSet(
            gatewayHost.split(":")[0],
            // if there's a port, use it. Will be added to the url after a colon. Not an env variable
            if(gatewayHost.contains(":")) {
                gatewayHost.split(":")[1].toInt()
            } else {
                80
            }
        ),
        false
    )!!

    val auth = SocketAuth(socket).authenticate()
    if(!auth) {
        logger.error("Failed to authenticate with the API. Killing process.")
        exitProcess(1)
    } else {
        logger.info("Authenticated with the GatewayAPI.")
    }

    val register = SocketRegister(socket).register()
    if(!register) {
        logger.error("Failed to register with the API. Killing process.")
        exitProcess(1)
    } else {
        logger.info("Registered with the GatewayAPI.")
    }

    demoWorld()
    events()

    if (System.getenv("PAPER_VELOCITY_SECRET") !== null) {
        VelocityProxy.enable(System.getenv("PAPER_VELOCITY_SECRET"))
    }
    SERVER.start("0.0.0.0", 25565)
}

private fun demoWorld() {
    spawnWorld = MinecraftServer.getInstanceManager().createInstanceContainer()
    spawnWorld.setBlock(Pos(0.0, 64.0, 0.0), Block.STONE)
}

private fun events() {
    MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        val instance = spawnWorld
        event.spawningInstance = instance
        player.teleport(Pos(0.5, 66.0, 0.5))
    }
}