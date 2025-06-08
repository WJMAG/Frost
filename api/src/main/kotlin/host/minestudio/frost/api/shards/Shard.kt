package host.minestudio.frost.api.shards

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.api.shards.helper.LogEmitter
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.lang.reflect.Method
import java.net.URL

/**
 * The base class for all shards.
 */
abstract class Shard {

    /**
     * The classloader for this shard.
     * Generally safe to never touch this.
     */
    @ApiStatus.Internal
    lateinit var loader: ShardClassLoader

    /**
     * The shard helper for this shard.
     * You should never need to touch
     * this, as it is used by methods
     * within this class.
     */
    @ApiStatus.Internal
    lateinit var shardHelper: ShardHelper

    /**
     * The data folder for this shard.
     * You can use this folder to store
     * data for your shard, such as
     * configuration files.
     */
    lateinit var dataFolder: File

    /**
     * The shard info for this shard.
     * This contains information
     * about the shard, such as
     * the name, version, and author.
     *
     * Do not modify this during runtime.
     */
    @ApiStatus.Internal
    var info: ShardInfo? = null

    /**
     * Whether this shard
     * has been through the setup
     * process.
     *
     * You do not need to set this
     * manually.
     */
    @ApiStatus.Internal
    var isSetup = false

    /**
     * This is called prior to the
     * [Shard.create] method. Allowing
     * you to perform any pre-setup actions.
     *
     * This is called prior to commands
     * being registered to the server.
     */
    open fun presetup() {}

    /**
     * The setup method for this shard.
     * This method will be called when
     * the shard is loaded.
     *
     * You can use this method to
     * initialize your shard, such as
     * loading configuration files,
     * registering commands, etc.
     */
    abstract fun create()

    /**
     * The destroy method for this shard.
     * This method will be called when
     * the shard is unloaded.
     *
     * You can use this method to
     * clean up your shard, such as
     * saving configuration files,
     * unregistering commands, etc.
     */
    abstract fun delete()

    /**
     * This is where the jar file
     * is located.
     */
    @get:ApiStatus.Internal
    val jarLocation: URL?
        get() = this.javaClass.protectionDomain.codeSource.location

    /**
     * Publish a configuration schema to
     * the dashboard. This allows users
     * to configure your shard without
     * requiring a file.
     *
     * This should be called on [Shard.create]
     * every time the shard boots. This
     * does support updates, so you should
     * run this method every time the shard
     * boots to ensure the latest schema is
     * applied.
     *
     * @param schema The configuration schema to publish
     */
    protected fun publishConfigSchema(schema: ConfigSchema) {
        if(!isSetup) {
            throw IllegalStateException("Shard is not setup yet.")
        }
        this.shardHelper.registerConfigSchema(schema)
    }

    fun getLogEmitter(): LogEmitter {
        if (!isSetup) {
            throw IllegalStateException("Shard is not setup yet.")
        }
        val logger: (LogLevel, String) -> Unit = { level, message ->
            this.shardHelper.emitLog(level, message)
        }
        return LogEmitter(logger)
    }

}