package host.minestudio.frost.api.shards

import host.minestudio.frost.api.dependencies.resolver.MavenResolver

/**
 * Represents a module dependency loader.
 */
abstract class ShardDependencyLoader {

    /**
     * Loads the dependencies for the module.
     * You should use the [MavenResolver] to add
     * dependencies and repositories to the classpath.
     *
     * You should never need to shade dependencies into
     * your shard jarfile. Use this whenever possible to
     * keep your shard lightweight.
     *
     * @param mavenResolver The maven resolver to use.
     */
    abstract fun loadDependencies(mavenResolver: MavenResolver?)
}