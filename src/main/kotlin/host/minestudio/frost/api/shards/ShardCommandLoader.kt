package host.minestudio.frost.api.shards

import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.command.annotation.Command
import host.minestudio.frost.api.shards.command.annotation.Subcommand
import net.minestom.server.command.builder.arguments.Argument
import org.slf4j.Logger
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.Base64
import java.util.ServiceLoader

class ShardCommandLoader(
    private val shardLoader: ShardClassLoader,
    private val logger: Logger
) {
    fun loadCommands(): List<ShardManager.CommandOpts> {
        logger.debug("│     Scanning for commands...")

        return try {
            ServiceLoader.load(ShardCommand::class.java, shardLoader)
                .mapNotNull { command ->
                    processCommand(command).also { cmd ->
                        logger.debug("│     Found command: ${cmd?.name ?: "INVALID"}")
                    }
                }
        } catch (e: Exception) {
            logger.error("│     [!] Command loading failed", e)
            emptyList()
        }
    }

    private fun processCommand(command: ShardCommand): ShardManager.CommandOpts? {
        return command.javaClass.getAnnotation(Command::class.java)?.let { cmdAnnotation ->
            val subcommands = logSubcommandProcessing(command) {
                processSubcommands(command)
            }

            createCommandOpts(cmdAnnotation, subcommands).also {
                logger.debug("│   Created command options for ${it.name}")
            }
        } ?: run {
            logger.warn("│   Command ${command.javaClass.name} lacks @Command annotation")
            null
        }
    }

    private inline fun <T> logSubcommandProcessing(command: ShardCommand, block: () -> T): T {
        logger.debug("│   Scanning for subcommands...")
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime

        logger.debug("│   Found ${(result as? Map<*, *>)?.size ?: 0} subcommands (${duration}ms)")
        return result
    }

    private fun processSubcommands(command: ShardCommand): Map<Method, ShardManager.SubcommandOpts> {
        return command.javaClass.declaredMethods
            .asSequence()
            .filter { it.isAnnotationPresent(Subcommand::class.java) }
            .mapNotNull { method ->
                logSubcommandCreation(method) {
                    method to createSubcommandOpts(method)
                }
            }
            .toMap()
    }

    private inline fun <T> logSubcommandCreation(method: Method, block: () -> T): T? {
        return try {
            block().also {
                logger.trace("│     Processed subcommand: ${method.name}")
            }
        } catch (e: Exception) {
            logger.warn("│     Failed to process subcommand ${method.name}: ${e.message}")
            null
        }
    }

    private fun createSubcommandOpts(method: Method): ShardManager.SubcommandOpts {
        val subcommand = method.getAnnotation(Subcommand::class.java)
        return ShardManager.SubcommandOpts(
            path = subcommand.path,
            description = subcommand.description,
            usage = subcommand.usage,
            arguments = processArguments(method)
        )
    }

    private fun processArguments(method: Method): List<Argument<*>> {
        return method.parameters
            .asSequence()
            .filter { it.isAnnotationPresent(
                host.minestudio.frost.api.shards.command.annotation.Argument::class.java
            )}
            .mapNotNull { param ->
                logArgumentProcessing(param) {
                    createArgumentInstance(param)
                }
            }
            .toList()
    }

    private inline fun logArgumentProcessing(param: Parameter, block: () -> Argument<*>): Argument<*>? {
        return try {
            block().also { arg ->
                logger.trace("│       Created argument: ${param.name} (${arg.javaClass.simpleName})")
            }
        } catch (e: Exception) {
            logger.warn("│       Failed to create argument for ${param.name}: ${e.message}")
            logger.warn("│       Stack trace: ", e)
            null
        }
    }

    private fun createArgumentInstance(param: Parameter): Argument<*> {
        if (!Argument::class.java.isAssignableFrom(param.type)) {
            throw IllegalArgumentException("Parameter ${param.name} has unsupported type ${param.type.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return param.type.getDeclaredConstructor(String::class.java).newInstance(
            param.name
        ) as Argument<Any>
    }

    private fun createCommandOpts(
        annotation: Command,
        subcommands: Map<Method, ShardManager.SubcommandOpts>
    ): ShardManager.CommandOpts {
        return ShardManager.CommandOpts(
            name = annotation.name,
            aliases = annotation.aliases,
            description = annotation.description,
            usage = annotation.usage,
            permission = annotation.permission,
            subcommands = subcommands.takeIf { it.isNotEmpty() }
        )
    }
}