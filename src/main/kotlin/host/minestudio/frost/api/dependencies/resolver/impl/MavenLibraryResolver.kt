package host.minestudio.frost.api.dependencies.resolver.impl

import com.sun.tools.classfile.Dependency
import host.minestudio.frost.api.dependencies.resolver.lib.ClassPathLibrary
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryLoadingException
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryStore
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DependencyFilter
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author PaperMC
 */
class MavenLibraryResolver : ClassPathLibrary {
    private val repository: RepositorySystem
    private val session: DefaultRepositorySystemSession
    private val repositories: MutableList<RemoteRepository?> = ArrayList<Any?>()
    private val dependencies: MutableList<Dependency?> = ArrayList<Any?>()

    init {
        val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        this.repository = locator.getService(RepositorySystem::class.java)
        this.session = MavenRepositorySystemUtils.newSession()
        this.session.setSystemProperties(System.getProperties())
        this.session.setChecksumPolicy("fail")
        this.session.setLocalRepositoryManager(
            this.repository.newLocalRepositoryManager(
                this.session,
                LocalRepository("libraries")
            )
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
    fun register(store: LibraryStore) {
        val repos: MutableList<RemoteRepository?>? =
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
            val file: File = artifact.artifact.getFile()
            store.addLibrary(file.toPath())
        }
    }

    internal inner class Transfer() : AbstractTransferListener() {
        fun transferInitiated(event: TransferEvent) {
            val l = logger
            val repo: String? = event.resource.repositoryUrl
            l.info("Downloading {}", repo + event.resource.resourceName)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger("MavenLibraryResolver")
    }
}