package host.minestudio.frost.api.dependencies.resolver

import host.minestudio.frost.api.dependencies.RemoteRepository
import org.eclipse.aether.graph.Dependency

interface MavenResolver {

    /**
     * Add a dependency to the resolver. This will
     * load it into the classpath, meaning you don't
     * have to shade it into your shard.
     *
     * @param owningClass Your Shards main class
     * @param dependency The dependency to add
     * @see [Dependency]
     */
    fun addDependency(owningClass: Class<*>?, dependency: Dependency)

    /**
     * Add a repository to the resolver. This will
     * allow you to load dependencies from remote
     * repositories, such as Maven Central or JitPack.
     *
     * @param owningClass Your Shards main class
     * @param repository The repository to add
     * @see [RemoteRepository]
     */
    fun addRepository(owningClass: Class<*>?, repository: RemoteRepository)

    /**
     * Get all repositories that have been added to the resolver.
     * This is useful for debugging purposes.
     *
     * @return A map of repository names to their URLs
     */
    fun getRepositories(): Map<org.eclipse.aether.repository.RemoteRepository?, Class<*>?>

    /**
     * Get all dependencies that have been added to the resolver.
     * This is useful for debugging purposes.
     *
     * @return A map of dependency names to their versions
     */
    fun getDependencies(): Map<Dependency?, Class<*>?>
}