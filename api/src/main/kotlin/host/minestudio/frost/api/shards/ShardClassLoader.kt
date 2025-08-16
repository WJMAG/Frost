package host.minestudio.frost.api.shards

import org.jetbrains.annotations.ApiStatus

/**
 * The classloader for a shard.
 */
@ApiStatus.Internal
interface ShardClassLoader {

    /**
     * This is an override method from the
     * [java.net.URLClassLoader], which is used to
     * load classes from the shard.
     */
    @Suppress("unused")
    fun loadClass(name: String, resolve: Boolean): Class<*>?

}