package host.minestudio.frost.api.shards.command.annotation


@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(
    val path: String,
    val description: String = "",
    val usage: String = ""
)
