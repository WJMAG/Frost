package host.minestudio.frost.api.shards

import host.minestudio.frost.api.dependencies.resolver.MavenResolver

/**
 * Represents a module dependency loader.
 */
@Suppress("unused")
abstract class ShardDependencyLoader {
    /**
     * Loads the dependencies for the module.
     * @param mavenResolver The maven resolver to use.
     */
    abstract fun loadDependencies(mavenResolver: MavenResolver?)
}