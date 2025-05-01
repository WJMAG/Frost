package host.minestudio.frost.api.shards

import org.jetbrains.annotations.ApiStatus
import java.io.File
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
     * Whether or not this shard
     * has been through the setup
     * process.
     *
     * You do not need to set this
     * manually.
     */
    @ApiStatus.Internal
    var isSetup = false

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

}