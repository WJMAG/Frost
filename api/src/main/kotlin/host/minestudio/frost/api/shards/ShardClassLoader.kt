package host.minestudio.frost.api.shards

interface ShardClassLoader {

    fun loadClass(name: String, resolve: Boolean): Class<*>?

    val shardInfo: ShardInfo?

}