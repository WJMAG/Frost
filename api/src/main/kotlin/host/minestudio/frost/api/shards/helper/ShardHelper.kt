package host.minestudio.frost.api.shards.helper

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.ShardDependencyLoader
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.enum.LogLevel

interface ShardHelper {
    
    val commands: MutableList<ShardCommand>
    var shardAppointedDependencyLoader: ShardDependencyLoader?
    
    /**
     * Registers a configuration schema for the shard.
     *
     * @param schema The configuration schema to be registered.
     */
    fun registerConfigSchema(schema: ConfigSchema)
    
    /**
     * Emits a log message with the specified level.
     *
     * @param level The log level of the message.
     * @param message The message to be logged.
     */
    fun emitLog(level: LogLevel, message: String)
    
    /**
     * Retrieves the storage service for the shard.
     *
     * @return The storage service instance.
     */
    fun getStorageService(): StorageService

    /**
     * Registers a command with the shard.
     *
     * @param command The command to be registered.
     */
    fun registerCommand(command: ShardCommand)
}