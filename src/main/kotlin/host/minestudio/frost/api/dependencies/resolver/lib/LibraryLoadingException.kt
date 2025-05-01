package host.minestudio.frost.api.dependencies.resolver.lib

/**
 * @author PaperMC
 */
class LibraryLoadingException : RuntimeException {
    constructor(s: String?) : super(s)

    constructor(s: String?, e: Exception?) : super(s, e)
}