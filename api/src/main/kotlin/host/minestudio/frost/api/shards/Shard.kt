package host.minestudio.frost.api.shards

import host.minestudio.frost.api.config.ConfigSchema
import host.minestudio.frost.api.lang.Lang
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.enum.LogLevel
import host.minestudio.frost.api.shards.helper.LogEmitter
import host.minestudio.frost.api.shards.helper.ShardHelper
import host.minestudio.frost.api.shards.helper.StorageService
import net.minestom.server.instance.Instance
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.net.URL
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The base class for all shards.
 */
abstract class Shard() {

    private lateinit var loader: ShardClassLoader
    private lateinit var helper: ShardHelper
    private lateinit var info: ShardInfo

    private val isInitialized: AtomicBoolean = AtomicBoolean(false)
    fun init(loader: ShardClassLoader, helper: ShardHelper, info: ShardInfo, world: Instance, dataFolder: File) {
        if(!isInitialized.compareAndSet(false, true)) {
            throw IllegalStateException("Shard has already been initialized.")
        }
        this.loader = loader
        this.helper = helper
        this.info = info
        this.defaultWorld = world
        this.dataFolder = dataFolder
        this.hasContext = true
    }


    /**
     * The data folder for this shard.
     * You can use this folder to store
     * data for your shard, such as
     * configuration files.
     */
    lateinit var dataFolder: File

    /**
     * This is the base world loaded
     * by Frost. This is where players
     * will spawn when they join the server.
     */
    lateinit var defaultWorld: Instance

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
        this.helper.registerConfigSchema(schema)
    }

    protected fun getStorageService(): StorageService {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        return this.helper.getStorageService()
    }

    fun getLogEmitter(): LogEmitter {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        val logger: (LogLevel, String) -> Unit = { level, message ->
            this.helper.emitLog(level, message)
        }
        return LogEmitter(logger)
    }

    fun registerCommand(command: ShardCommand) {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        this.helper.registerCommand(command)
    }

    fun getLang(title: String, lang: String, variant: String): Lang {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        return this.helper.getLang(title, lang, variant, this.javaClass)
    }

    fun setDependencyloader(loader: ShardDependencyLoader) {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        this.helper.shardAppointedDependencyLoader = loader
    }

    fun getCommands(): List<ShardCommand> {
        if (!hasContext) {
            throw IllegalStateException("This shard does not have context assigned yet. Please ensure you're calling this after presetup or create, and not during a constructor.")
        }
        return Collections.unmodifiableList(this.helper.commands)
    }

    fun getInfo(): ShardInfo? {
        return if (this::info.isInitialized) this.info else null
    }

    fun getClassLoader(): ShardClassLoader? {
        return if (this::loader.isInitialized) this.loader else null
    }

    fun getShardAppointedDependencyLoader(): ShardDependencyLoader? {
        return if (this::helper.isInitialized) this.helper.shardAppointedDependencyLoader else null
    }

}