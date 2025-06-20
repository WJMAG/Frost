package host.minestudio.frost.api.shards.impl

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.ShardDependencyLoader
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.api.shards.helper.ShardHelper
import host.minestudio.frost.api.shards.helper.StorageService
import host.minestudio.frost.onlineMode
import host.minestudio.frost.socket
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ShardHelperImpl(
    private val shardId: String,
    private val shardName: String
) : ShardHelper {
    val logger: Logger = LoggerFactory.getLogger(shardName)
    override val commands: MutableList<ShardCommand> = mutableListOf()
    override var shardAppointedDependencyLoader: ShardDependencyLoader? = null

    override fun registerConfigSchema(schema: ConfigSchema) {
        if(onlineMode) {
            socket!!.emit("config:register", shardId, shardName, schema.toJson())
            println("Registered config schema for shard: $shardName: " + schema.toJson())
        } else {
            println("WARN: Cannot register config without connection to MineStudio.")
        }
    }

    override fun emitLog(level: LogLevel, message: String) {
        if(onlineMode)
            socket!!.emit("log", shardName, level.name, message)
        else
            when (level) {
                LogLevel.INFO -> logger.info(message)
                LogLevel.WARN -> logger.warn(message)
                LogLevel.ERROR -> logger.error(message)
                LogLevel.DEBUG -> logger.debug(message)
                LogLevel.TRACE -> logger.trace(message)
                LogLevel.FATAL -> logger.error("FATAL: $message")
            }
    }

    override fun getStorageService(): StorageService {
        return StorageServiceImpl()
    }

    override fun registerCommand(command: ShardCommand) {
        commands.add(command)
    }

}