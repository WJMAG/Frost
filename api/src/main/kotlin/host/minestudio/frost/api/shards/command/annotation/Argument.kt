package host.minestudio.frost.api.shards.command.annotation

import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentString
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Argument(
    val type: KClass<out Argument<*>> = ArgumentString::class
)
