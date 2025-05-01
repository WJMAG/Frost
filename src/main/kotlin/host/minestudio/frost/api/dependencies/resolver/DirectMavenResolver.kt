package host.minestudio.frost.api.dependencies.resolver

import host.minestudio.frost.api.dependencies.resolver.lib.ClassPathLibrary
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryLoadingException
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryStore
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.graph.DefaultDependencyNode
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Direct Maven Resolver
 *
 * @author someone on stackoverflow
 */
@Suppress("unused", "deprecation")
class DirectMavenResolver : ClassPathLibrary, MavenResolver {
    private val repository: RepositorySystem
    private val session: DefaultRepositorySystemSession

    private val repositories = HashMap<RemoteRepository?, Class<*>?>()
    private val dependencies = HashMap<Dependency?, Class<*>?>()

    /**
     * Creates a new Direct Maven Resolver
     */
    init {
        val locator: DefaultServiceLocator  = MavenRepositorySystemUtils.newServiceLocator()
        try {
            locator.addService(
                RepositoryConnectorFactory::class.java,
                Class.forName("org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory") as Class<out RepositoryConnectorFactory?>
            )
            locator.addService(
                TransporterFactory::class.java,
                Class.forName("org.eclipse.aether.transport.http.HttpTransporterFactory") as Class<out TransporterFactory?>
            )
        } catch (exception: ClassNotFoundException) {
            throw RuntimeException(exception)
        }

        this.repository = locator.getService(RepositorySystem::class.java)
        this.session = MavenRepositorySystemUtils.newSession()

        this.session.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        this.session.localRepositoryManager = this.repository.newLocalRepositoryManager(
            this.session,
            LocalRepository("libraries")
        )
        this.session.transferListener = object : AbstractTransferListener() {
            override fun transferInitiated(event: TransferEvent) {
                LOGGER.info(
                    "Downloading {}{}...",
                    event.resource.repositoryUrl,
                    event.resource.resourceName
                )
            }
        }
        this.session.setReadOnly()
    }

    /**
     * Register the libraries
     * @param store Library store
     * @throws LibraryLoadingException If an error occurs
     */
    @Throws(LibraryLoadingException::class)
    override fun register(store: LibraryStore) {
        val repos = this.repository.newResolutionRepositories(this.session, this.repositories.keys.stream().toList())

        try {
            for (dependency in this.dependencies.keys) {
                val node = DefaultDependencyNode(dependency)
                node.repositories = repos

                val result = this.repository.resolveDependencies(
                    this.session, DependencyRequest(node, null)
                )

                for (artifact in result.artifactResults) {
                    val file = artifact.artifact.file
                    store.addLibrary(file.toPath())
                }
            }
        } catch (exception: DependencyResolutionException) {
            throw LibraryLoadingException(exception.message)
        }
    }

    /**
     * Add a dependency to be loaded
     * at runtime
     * @param owningClass Owning class
     * @param dependency Dependency
     */
    override fun addDependency(owningClass: Class<*>?, dependency: Dependency) {
        this.dependencies.put(dependency, owningClass)
        addSharedPackage(dependency.artifact.groupId)
    }

    /**
     * Add a repository to be used
     * at runtime to load the dependencies
     * @param owningClass Owning class
     * @param remoteRepository Remote repository
     */
    override fun addRepository(owningClass: Class<*>?, remoteRepository: RemoteRepository) {
        this.repositories.put(remoteRepository, owningClass)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MavenResolver::class.java)

        /**
         * A list of packages that should be
         * loaded from the Parent Classloader
         * instead of the Module Classloader
         */
        val SHARED_PACKAGES: MutableList<String?> = ArrayList<String?>()

        /**
         * Add a shared package
         * @param pkg Package
         */
        fun addSharedPackage(pkg: String?) {
            SHARED_PACKAGES.add(pkg)
        }
    }
}