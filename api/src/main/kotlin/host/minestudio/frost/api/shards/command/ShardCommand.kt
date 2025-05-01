package host.minestudio.frost.api.shards.command

import net.minestom.server.command.builder.CommandContext
import net.minestom.server.entity.Player

abstract class ShardCommand {

    abstract fun execute(executor: Player)

}