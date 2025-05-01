package host.minestudio.frost.api.shards

/**
 * The information of a shard.
 */
data class ShardInfo(

    /**
     * The name of the shard.
     */
    val name: String,

    /**
     * The id of the shard.
     *
     * This must be unique across all shards.
     */
    val id: String,

    /**
     * The description of the shard.
     */
    val description: String,

    /**
     * The version your shard is currently on.
     */
    val version: String,

    /**
     * Dependency shards required to run this shard.
     */
    val deps: List<String>?
)