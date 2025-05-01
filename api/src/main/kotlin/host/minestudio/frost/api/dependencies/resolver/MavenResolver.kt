package host.minestudio.frost.api.dependencies.resolver

import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

interface MavenResolver {

    fun addDependency(owningClass: Class<*>?, dependency: Dependency)
    fun addRepository(owningClass: Class<*>?, repository: RemoteRepository)

}