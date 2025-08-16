package host.minestudio.frost.api.shards

import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.command.annotation.Command
import host.minestudio.frost.api.shards.command.annotation.Subcommand
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import org.slf4j.Logger
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class ShardCommandLoader(
    private val shard: Shard,
    private val logger: Logger
) {

    fun loadCommands(): List<ShardManager.CommandOpts> {
        logger.debug("│     Scanning for commands...")

        return try {
            shard.getCommands()
                .onEach { it.shard = shard }
                .mapNotNull {
                    processCommand(it).also { cmd ->
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
            val subcommands = logSubcommandProcessing {
                processSubcommands(command)
            }

            createCommandOpts(cmdAnnotation, command, subcommands).also {
                logger.debug("│   Created command options for ${it.name}")
            }
        } ?: run {
            logger.warn("│   Command ${command.javaClass.name} lacks @Command annotation")
            null
        }
    }

    private inline fun <T> logSubcommandProcessing(block: () -> T): T {
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
        val annotation = param.getAnnotation(
            host.minestudio.frost.api.shards.command.annotation.Argument::class.java
        ) ?: return ArgumentType.String(param.name)

        val argumentType = annotation.type.java
        if (!Argument::class.java.isAssignableFrom(argumentType)) {
            throw IllegalArgumentException("Parameter ${param.name} has unsupported type ${param.type.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return argumentType.getDeclaredConstructor(String::class.java).newInstance(
            param.name
        ) as Argument<Any>
    }

    private fun createCommandOpts(
        annotation: Command,
        cmd: ShardCommand,
        subcommands: Map<Method, ShardManager.SubcommandOpts>
    ): ShardManager.CommandOpts {
        return ShardManager.CommandOpts(
            name = annotation.name,
            aliases = annotation.aliases,
            description = annotation.description,
            usage = annotation.usage,
            permission = annotation.permission,
            cmd = cmd,
            subcommands = subcommands.takeIf { it.isNotEmpty() }
        )
    }


    class MinestomCommand(
        private val command: ShardManager.CommandOpts,
    ) : net.minestom.server.command.builder.Command(command.name) {

        init {
            setDefaultExecutor { sender, context ->
                command.cmd.execute(sender as Player)
            }

            command.subcommands?.forEach { (method, opts) ->
                val literals = opts.path.split(" ")
                val literalArgs = literals.mapNotNull { ArgumentType.Literal(it) }

                addSyntax({ executor, ctx ->
                    if (opts.arguments != null) {
                        val methodParams = method.parameters
                        if (methodParams.size != opts.arguments.size + 1) { // +1 for executor
                            throw IllegalArgumentException("Parameter count mismatch")
                        }
                        val argsMap = ctx.map
                        val args = Array<Any?>(methodParams.size) { index ->
                            when (index) {
                                0 -> executor
                                else -> {
                                   val arg = methodParams[index]
                                    val argName = arg.name
                                    val argType = arg.type
                                    val argValue = argsMap[argName]
                                    if (argValue == null) {
                                        throw IllegalArgumentException("Missing argument: $argName")
                                    }

                                    if (argType.isAssignableFrom(argValue.javaClass)) {
                                        argValue
                                    } else {
                                        throw IllegalArgumentException("Argument type mismatch for $argName. Expected ${argType.name.split(".").last()}, got ${argValue.javaClass.name.split(".").last()}")
                                    }
                                }
                            }
                        }
                        val middleware = command.cmd.middleware(executor as Player,literalArgs.joinToString(" "), args)
                        if (!middleware) return@addSyntax
                        method.invoke(command.cmd, *args)
                    } else {
                        val middleware = command.cmd.middleware(executor as Player, literalArgs.joinToString(" "), null)
                        if (!middleware) return@addSyntax
                        command.cmd.execute(executor)
                    }
                }, *(literalArgs.toTypedArray()), *(opts.arguments?.toTypedArray() ?: emptyArray()))            }
        }

    }
}