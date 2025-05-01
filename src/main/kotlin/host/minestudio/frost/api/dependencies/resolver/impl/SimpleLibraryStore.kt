package host.minestudio.frost.api.dependencies.resolver.impl

import host.minestudio.frost.api.dependencies.resolver.lib.LibraryStore
import java.nio.file.Path

/**
 * @author PaperMC
 */
class SimpleLibraryStore : LibraryStore {
    val paths: MutableList<Path?> = ArrayList<Path?>()

    override fun addLibrary(library: Path) {
        this.paths.add(library)
    }
}