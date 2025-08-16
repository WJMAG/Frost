package host.minestudio.frost.api.gaming

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockManager
import net.minestom.server.tag.Tag
import java.util.function.Supplier

/**
 * Block handlers for signs, hanging signs, player heads, and banners.
 * <br></br>
 * These don't handle any logic; they just let the server know which tags need to be sent to the client.
 *
 * @author https://github.com/Minestom/game-jam-template/blob/main/src/main/java/net/minestom/jam/instance/BlockHandlers.java
 */
@Suppress("unused")
object BlockHandlers {
    /**
     * Registers every handler to a given block manager.
     * This should probably be `MinecraftServer.getBlockManager()`.
     */
    fun register(manager: BlockManager) {
        manager.registerHandler(Sign.Companion.KEY, Supplier { Sign() })
        manager.registerHandler(HangingSign.Companion.KEY, Supplier { HangingSign() })
        manager.registerHandler(PlayerHead.Companion.KEY, Supplier { PlayerHead() })
        manager.registerHandler(Banner.Companion.KEY, Supplier { Banner() })
    }

    class Sign : BlockHandler {
        override fun getKey(): Key {
            return KEY
        }

        override fun getBlockEntityTags(): MutableCollection<Tag<*>?> {
            return TAGS
        }

        companion object {
            val KEY = Key.key("sign")

            private val TAGS: MutableList<Tag<*>?> = mutableListOf<Tag<*>?>(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text")
            )
        }
    }

    class HangingSign : BlockHandler {
        override fun getKey(): Key {
            return KEY
        }

        override fun getBlockEntityTags(): MutableCollection<Tag<*>?> {
            return TAGS
        }

        companion object {
            val KEY = Key.key("hanging_sign")

            private val TAGS: MutableList<Tag<*>?> = mutableListOf<Tag<*>?>(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text")
            )
        }
    }

    class PlayerHead : BlockHandler {
        override fun getKey(): Key {
            return KEY
        }

        override fun getBlockEntityTags(): MutableCollection<Tag<*>?> {
            return TAGS
        }

        companion object {
            val KEY = Key.key("skull")

            private val TAGS: MutableList<Tag<*>?> = mutableListOf<Tag<*>?>(
                Tag.NBT("profile")
            )
        }
    }

    class Banner : BlockHandler {
        override fun getKey(): Key {
            return KEY
        }

        override fun getBlockEntityTags(): MutableCollection<Tag<*>?> {
            return TAGS
        }

        companion object {
            val KEY = Key.key("banner")

            private val TAGS: MutableList<Tag<*>?> = mutableListOf<Tag<*>?>(
                Tag.NBT("patterns")
            )
        }
    }
}