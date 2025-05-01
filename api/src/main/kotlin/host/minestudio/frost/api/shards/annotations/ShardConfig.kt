package host.minestudio.frost.api.shards.annotations

annotation class ShardConfig(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val authors: Array<String> = [],
    val deps: Array<String> = [],
)
