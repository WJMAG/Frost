package host.minestudio.frost.api.dependencies.resolver.impl

import host.minestudio.frost.api.dependencies.resolver.lib.ClassPathLibrary
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryLoadingException
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryStore
import java.io.File
import java.util.ArrayList
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyFilter
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author PaperMC
 */
@Suppress("unused")
class MavenLibraryResolver : ClassPathLibrary {
    private val repository: RepositorySystem
    private val session: DefaultRepositorySystemSession
    private val repositories: MutableList<RemoteRepository> = ArrayList<RemoteRepository>()
    private val dependencies: MutableList<Dependency> = ArrayList<Dependency>()

    init {
        val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        this.repository = locator.getService(RepositorySystem::class.java)
        this.session = MavenRepositorySystemUtils.newSession()
        this.session.setSystemProperties(System.getProperties())
        this.session.checksumPolicy = "fail"
        this.session.localRepositoryManager = this.repository.newLocalRepositoryManager(
            this.session,
            LocalRepository("libraries")
        )
        this.session.setReadOnly()
    }

    fun addDependency(dependency: Dependency) {
        this.dependencies.add(dependency)
    }

    fun addRepository(remoteRepository: RemoteRepository) {
        this.repositories.add(remoteRepository)
    }

    @Throws(LibraryLoadingException::class)
    override fun register(var1: LibraryStore) {
        val repos: MutableList<RemoteRepository> =
            this.repository.newResolutionRepositories(this.session, this.repositories)

        val result: DependencyResult
        try {
            result = this.repository.resolveDependencies(
                this.session,
                DependencyRequest(
                    CollectRequest(null as Dependency?, this.dependencies, repos),
                    null as DependencyFilter?
                )
            )
        } catch (var7: DependencyResolutionException) {
            val ex: DependencyResolutionException = var7
            throw LibraryLoadingException("Error resolving libraries", ex)
        }

        val var8: MutableIterator<*> = result.artifactResults.iterator()

        while (var8.hasNext()) {
            val artifact: ArtifactResult = var8.next() as ArtifactResult
            val file: File = artifact.artifact.file
            var1.addLibrary(file.toPath())
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger("MavenLibraryResolver")
    }
}