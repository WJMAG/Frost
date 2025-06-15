package host.minestudio.frost.api.dependencies

import org.eclipse.aether.repository.RemoteRepository
import org.jetbrains.annotations.ApiStatus

/**
 * Remote Repository wrapper, ease of use
 */
data class RemoteRepository(

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
        return RemoteRepository.Builder(url.hashCode().toString(), null, url).build()    }
}