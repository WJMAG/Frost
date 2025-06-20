package host.minestudio.frost.api.shards

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.api.shards.helper.LogEmitter
import host.minestudio.frost.api.shards.helper.ShardHelper
import host.minestudio.frost.api.shards.helper.StorageService
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
     * Never touch this. It will break
     * your entire shard if modified.
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
     * has been assigned a
     *
     *
     * This is set AFTER the shard
     * is assigned context.
     *
     * You do not need to set this
     * manually.
     */
    @ApiStatus.Internal
    var hasContext = false

    /**
     * Whether this shard
     * has been through the
     * pre-setup process.
     *
     * This is set AFTER the presetup
     * method is called.
     *
     * You do not need to set this
     * manually.
     *
     * @see [Shard.presetup]
     */
    var isPreSetup = false

    /**
     * Whether this shard
     * is currently in the setup
     * process.
     *
     * This is set to true when the
     * [Shard.presetup] method is started,
     * and set to false when the
     * [Shard.presetup] method is finished.
     *
     * You do not need to set this
     * manually.
     */
    var isInPreSeup = false

    /**
     * Whether this shard
     * has been through the setup
     *
     *
     * This is set AFTER the setup
     * method is called.
     *
     * You do not need to set this
     * manually.
     *
     * @see [Shard.create]
     */
    @ApiStatus.Internal
    var isSetup = false

    /**
     * Whether this shard
     * is currently in the setup
     * process.
     *
     * This is set to true when the
     * [Shard.create] method is started,
     * and set to false when the
     * [Shard.create] method is finished.
     *
     * You do not need to set this
     * manually.
     */
    var isInSetup = false

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
        if(!isInSetup) {
            throw IllegalStateException("You can only register Config Schemas during the SETUP phase of a shard.")
        }
        this.shardHelper.registerConfigSchema(schema)
    }

    protected fun getStorageService(): StorageService {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have contex assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        return this.shardHelper.getStorageService()
    }

    fun getLogEmitter(): LogEmitter {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        val logger: (LogLevel, String) -> Unit = { level, message ->
            this.shardHelper.emitLog(level, message)
        }
        return LogEmitter(logger)
    }

    fun registerCommand(command: ShardCommand) {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        this.shardHelper.registerCommand(command)
    }

    fun setDependencyloader(loader: ShardDependencyLoader) {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        this.shardHelper.shardAppointedDependencyLoader = loader
    }

}