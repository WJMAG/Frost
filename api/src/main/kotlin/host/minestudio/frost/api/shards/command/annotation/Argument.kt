package host.minestudio.frost.api.shards.command.annotation

import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentString
import kotlin.reflect.KClass

/**
 * Add an argument to a command.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Argument(

    /**
     * The type of argument to parse with. You
     * can specify anything that eventually extends
     * [net.minestom.server.command.builder.arguments.Argument].
     */
    val type: KClass<out Argument<*>> = ArgumentString::class
)
