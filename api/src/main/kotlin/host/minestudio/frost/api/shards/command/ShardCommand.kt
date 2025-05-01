package host.minestudio.frost.api.shards.command

import net.minestom.server.entity.Player

/**
 * The base class for all shard commands.
 */
abstract class ShardCommand {

    /**
     * The default executor for this command.
     * This is the method that will be called
     * when no arguments are provided.
     *
     * @param executor The player that executed the command.
     */
    abstract fun execute(executor: Player)

}