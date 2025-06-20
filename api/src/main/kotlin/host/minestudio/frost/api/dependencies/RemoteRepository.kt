package host.minestudio.frost.api.dependencies

import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.jetbrains.annotations.ApiStatus

/**
 * Remote Repository wrapper, ease of use
 */
data class RemoteRepository(

    /**
     * The repository id, used for identification
     */
    val id: String,

    /**
     * The repository URL
     */
    val url: String,
) {

    /**
     * Used to build a new RemoteRepository
     * from the given parameters
     */
    @ApiStatus.Internal
    fun toRemoteRepository(): RemoteRepository {
        val repo = RemoteRepository.Builder(
            id,
            "default",
            url
        )
        val policy = RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_FAIL)
        repo.setSnapshotPolicy(policy)
        repo.setPolicy(policy)

        val built = repo.build()
        return built
    }
}