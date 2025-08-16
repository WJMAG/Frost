package host.minestudio.frost.api.gaming

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.tag.Tag
import org.jetbrains.annotations.UnmodifiableView
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

/**
 * @author https://github.com/Minestom/game-jam-template/blob/main/src/main/java/net/minestom/jam/Game.java
 */
class Game(players: MutableSet<UUID>, private val lobbyInstance: Instance, private val lobbyPos: Pos) : PacketGroupingAudience {
    private val instance: InstanceContainer = createGameInstance()
    private val players: MutableList<Player> = ArrayList<Player>()
    private val ending = AtomicBoolean(false)

    fun onGameEnd() {
        ending.set(true)

        for (player in players) {
            player.setInstance(lobbyInstance, lobbyPos)
            player.removeTag(GAME)
        }

        players.clear()
        GAMES.remove(this)
    }

    @Suppress("unused")
    fun onDisconnect(player: Player) {
        players.remove(player)

        sendMessage(PLAYER_HAS_LEFT.apply(player.username)!!)

        // As an example, we end the game if there's one player left
        if (players.size == 1) {
            onGameEnd()
        }
    }

    init {

        for (uuid in players) {
            val player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)
            if (player == null) continue

            this.players.add(player)
            player.setTag<Game?>(GAME, this)

            player.setInstance(instance, SPAWN_POINT)
        }

        GAMES.add(this)
    }

    override fun getPlayers(): @UnmodifiableView MutableCollection<Player> {
        return Collections.unmodifiableCollection(players)
    }

    companion object {
        /**
         * The spawn point in the instance. Make sure to change this when changing the world!
         */
        val SPAWN_POINT: Pos = Pos(0.5, 65.0, 0.5, 0f, 0f)

        /**
         * The game that a player is in.
         */
        val GAME: Tag<Game?> = Tag.Transient<Game?>("Game")

        private val GAMES: MutableSet<Game?> = HashSet<Game?>()

        private fun createGameInstance(): InstanceContainer {
            val instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                AnvilLoader(Path.of("game"))
            )

            instance.setTimeRate(0)
            instance.setTime(6000) // Noon

            return instance
        }

        private val PLAYER_HAS_LEFT: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.GRAY),
                Component.text(username!!, NamedTextColor.GRAY),
                Component.text(" has left the game!", NamedTextColor.GRAY)
            )
        }
    }
}