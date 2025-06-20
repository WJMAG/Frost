package host.minestudio.frost.api.shards.command

import host.minestudio.frost.api.shards.Shard
import host.minestudio.frost.api.shards.helper.LogEmitter
import net.minestom.server.entity.Player

/**
 * The base class for all shard commands.
 */
abstract class ShardCommand {

    /**
     * The shard this command belongs to.
     * This is automatically initialized
     * by the shard loader.
     */
    lateinit var shard: Shard

    /**
     * Returns the log emitter for this shard.
     * This is used for logging messages to
     * the console.
     */
    fun getLogger(): LogEmitter = shard.getLogEmitter()

    /**
     * The default executor for this command.
     * This is the method that will be called
     * when no arguments are provided.
     *
     * @param executor The player that executed the command.
     */
    abstract fun execute(executor: Player)

}