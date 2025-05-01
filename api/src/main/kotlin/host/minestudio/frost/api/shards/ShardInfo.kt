package host.minestudio.frost.api.shards

data class ShardInfo(
    val name: String,
    val id: String,
    val description: String,
    val version: String,
    val deps: List<String>?
)