package host.minestudio.frost.api.shards.command.annotation

/**
 * Mark this class as a command
 */
@Target(AnnotationTarget.CLASS)
annotation class Command(

    /**
     * The name of the command. This is the
     * name that players will use when
     * executing the command.
     * eg. `/frost`
     */
    val name: String,

    /**
     * The aliases of the command. This is
     * the other names that players can
     * use to execute the same command.
     */
    val aliases: Array<String> = [],

    /**
     * The description of the command. This
     * currently has no effect.
     */
    val description: String = "",

    /**
     * The usage of the command. This
     * currently has no effect.
     */
    val usage: String = "",

    /**
     * The permission required to execute
     * the command.
     */
    val permission: String = "",
)
