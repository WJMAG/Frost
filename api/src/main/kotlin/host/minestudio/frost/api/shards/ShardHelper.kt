package host.minestudio.frost.api.shards

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.enum.LogLevel

interface ShardHelper {
    fun registerConfigSchema(schema: ConfigSchema)
    fun emitLog(level: LogLevel, message: String)
}