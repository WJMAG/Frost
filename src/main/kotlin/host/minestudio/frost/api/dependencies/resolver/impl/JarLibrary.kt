package host.minestudio.frost.api.dependencies.resolver.impl

import host.minestudio.frost.api.dependencies.resolver.lib.ClassPathLibrary
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryLoadingException
import host.minestudio.frost.api.dependencies.resolver.lib.LibraryStore
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author PaperMC
 */
class JarLibrary(private val path: Path) : ClassPathLibrary {
    @Throws(LibraryLoadingException::class)
    fun register(store: LibraryStore) {
        if (Files.notExists(this.path)) {
            throw LibraryLoadingException("Could not find library at " + this.path)
        } else {
            store.addLibrary(this.path)
        }
    }
}