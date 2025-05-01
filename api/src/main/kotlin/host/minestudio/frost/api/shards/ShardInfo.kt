package host.minestudio.frost.api.shards

interface ShardInfo {

    val name: String
    val id: String
    val description: String
    val version: String
    val deps: List<String>?

}