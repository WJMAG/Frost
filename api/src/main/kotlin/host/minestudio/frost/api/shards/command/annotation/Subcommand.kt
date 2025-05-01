package host.minestudio.frost.api.shards.command.annotation

/**
 * Mark this function as a subcommand.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Subcommand(

    /**
     * The name of the subcommand. Each value
     * placed here, players will need to use
     * as an argument to get to this method.
     * eg. "frost start", would require the player
     * to run "/command frost start <args?>".
     */
    val path: String,

    /**
     * The description of the subcommand. This
     * currently has no effect.
     */
    val description: String = "",

    /**
     * The usage of the subcommand. This
     * currently has no effect.
     */
    val usage: String = ""
)
