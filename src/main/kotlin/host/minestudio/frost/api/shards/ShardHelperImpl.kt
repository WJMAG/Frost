package host.minestudio.frost.api.shards

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.socket

class ShardHelperImpl(
    private val shardId: String,
    private val shardName: String
) : ShardHelper {

    override fun registerConfigSchema(schema: ConfigSchema) {
        socket.emit("config:register", shardId, shardName, schema.toJson())
        println("Registered config schema for shard: $shardName: " + schema.toJson())
    }

    override fun emitLog(level: LogLevel, message: String) {
        socket.emit("log", shardName, level.name, message)
    }

}