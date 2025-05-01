package host.minestudio.frost.api.shards

import java.io.File
import java.net.URL

abstract class Shard {

    lateinit var loader: ShardClassLoader
    lateinit var dataFolder: File
    var info: ShardInfo? = null

    var isSetup = false

    abstract fun create()
    abstract fun delete()

    val jarLocation: URL?
        get() = this.javaClass.protectionDomain.codeSource.location

}