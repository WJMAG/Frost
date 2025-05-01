package host.minestudio.frost.api.dependencies.resolver.lib

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path


/**
 * @author PaperMC
 */
@ApiStatus.Internal
interface LibraryStore {
    fun addLibrary(var1: Path)
}