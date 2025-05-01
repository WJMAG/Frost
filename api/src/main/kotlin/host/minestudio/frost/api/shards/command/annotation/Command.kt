package host.minestudio.frost.api.shards.command.annotation

@Target(AnnotationTarget.CLASS)
annotation class Command(
    val name: String,
    val aliases: Array<String> = [],
    val description: String = "",
    val usage: String = "",
    val permission: String = "",
)
