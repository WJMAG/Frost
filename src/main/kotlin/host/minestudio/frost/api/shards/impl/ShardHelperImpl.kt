package host.minestudio.frost.api.shards.impl

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.lang.Lang
import host.minestudio.frost.api.lang.LangImpl
import host.minestudio.frost.api.lang.LangLoader
import host.minestudio.frost.api.shards.Shard
import host.minestudio.frost.api.shards.ShardDependencyLoader
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.api.shards.helper.ShardHelper
import host.minestudio.frost.api.shards.helper.StorageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ShardHelperImpl(
    shardName: String
) : ShardHelper {
    val logger: Logger = LoggerFactory.getLogger(shardName)
    override val commands: MutableList<ShardCommand> = mutableListOf()
    override var shardAppointedDependencyLoader: ShardDependencyLoader? = null

    private val storageServiceImpl = StorageServiceImpl()

    override fun registerConfigSchema(schema: ConfigSchema) {
        logger.warn("WARN: Cannot register config without connection to MineStudio.")
    }

    override fun emitLog(level: LogLevel, message: String) {
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
        return storageServiceImpl
    }

    override fun registerCommand(command: ShardCommand) {
        commands.add(command)
    }

    override fun getLang(title: String, lang: String, variant: String, clazz: Class<out Shard>): Lang {
        val loader = LangLoader(clazz, title, lang, variant)
        return LangImpl(loader.getBundle())
    }

}