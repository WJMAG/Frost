package host.minestudio.frost.api.shards

import host.minestudio.frost.api.dependencies.resolver.DirectMavenResolver
import host.minestudio.frost.api.shards.annotations.ShardConfig
import host.minestudio.frost.api.shards.command.ShardCommand
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.arguments.Argument
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.ServiceLoader

val PLUGIN_REGEX = Regex("[0-9a-z-]+")

@Suppress("duplicate")
class ShardManager {

    private val logger = LoggerFactory.getLogger(ShardManager::class.java)
    private val shards = HashMap<String, Shard>()
    private val loadMetrics = LinkedHashMap<String, Pair<Long, Boolean>>() // <ShardName, <LoadTimeMS, Success>>

    private lateinit var shardLoader: URLClassLoader

    fun setup(dataDir: File) {
        logger.info("╔══════════════════════════════════════╗")
        logger.info("║        Initializing Shard Manager    ║")
        logger.info("╚══════════════════════════════════════╝")

        try {
            logStep("SETUP", "Initializing shard loader")
            setupShardLoader(dataDir.toPath())

            logStep("LOAD", "Beginning shard loading process")
            loadAndInstallShards(dataDir.toPath())
        } finally {
            logStep("COMPLETE", "Shard loading finished")
            printLoadMetrics()
        }
    }

    private fun logStep(phase: String, message: String) {
        logger.info("┌─[$phase] ${message.uppercase()}")
    }

    private fun logSubStep(message: String, indentLevel: Int = 1) {
        val indent = "│ ${"  ".repeat(indentLevel)}"
        logger.info("$indent$message")
    }

    private fun printLoadMetrics() {
        logger.info("╔══════════════════════════════════════╗")
        logger.info("║          Shard Load Metrics           ║")
        logger.info("╠══════════════════════════════════════╣")

        loadMetrics.forEach { (name, metrics) ->
            val status = if (metrics.second) "✓" else "✗"
            logger.info("║ ${name.padEnd(25)}: ${"%.3fs".format(metrics.first/1000.0).padStart(7)} $status ║")
        }

        val totalTime = loadMetrics.values.sumOf { it.first }
        val successCount = loadMetrics.values.count { it.second }
        logger.info("╠══════════════════════════════════════╣")
        logger.info("║ Total: ${"%.3fs".format(totalTime/1000.0).padStart(25)} | $successCount/${loadMetrics.size} loaded ║")
        logger.info("╚══════════════════════════════════════╝")
    }

    fun setupShardLoader(dataDir: Path) {
        try {
            val shardDir = File(dataDir.toFile(), "shards").apply {
                if (!exists() || !isDirectory) {
                    if (mkdirs()) {
                        logSubStep("Created shard directory: $absolutePath")
                    } else {
                        throw IllegalStateException("Failed to create shard directory: $absolutePath")
                    }
                }
            }

            shardDir.listFiles()?.let { jars ->
                logSubStep("Discovered ${jars.size} potential shard(s)")
                jars.forEach { jar ->
                    logSubStep("Found: ${jar.name}", 2)
                }

                shardLoader = ShardClassLoader(
                    jars.map { it.toURI().toURL() }.toTypedArray(),
                    this::class.java.classLoader,
                    dataDir.toFile()
                )
            } ?: run {
                logSubStep("No shards found in directory", 1)
            }
        } catch (e: Exception) {
            logger.error("│   [!] FAILED TO INITIALIZE SHARD LOADER", e)
        }
    }

    fun loadAndInstallShards(dataDir: Path) {
        try {
            val serviceLoader = ServiceLoader.load(Shard::class.java, shardLoader)
            logSubStep("Service loader prepared - ${serviceLoader.count()} shard(s) detected")

            serviceLoader.forEach { shard ->
                val timerStart = System.currentTimeMillis()
                var shardName = shard.info?.name ?: "UNKNOWN"

                try {
                    logStep("SHARD", "Processing ${shard.jarLocation?.file?.split("/")?.last() ?: "UNKNOWN"}")

                    // Load core shard
                    logSubStep("Loading shard classes")
                    Thread.currentThread().contextClassLoader = shardLoader
                    shard.apply {
                        loader = ShardClassLoader(arrayOf(jarLocation), ShardManager::class.java.classLoader, dataDir.toFile())
                        info = try {
                            val config = shard.javaClass.getAnnotation<ShardConfig>(ShardConfig::class.java)
                            ShardInfo(
                                id = config.id,
                                name = config.name,
                                version = config.version,
                                description = "unused",
                                deps = config.deps.toList()
                            )
                        } catch (e: Exception) {
                            logger.error("│   [!] FAILED TO LOAD SHARD INFO", e)
                            throw e
                        }
                        dataFolder = dataDir.toFile()
                    }
                    shardName = shard.info?.name ?: "UNKNOWN"
                    Thread.currentThread().contextClassLoader = this::class.java.classLoader

                    if (shard.info == null) {
                        throw IllegalStateException("Shard info not found (invalid/missing ShardConfig annotation?)")
                    }

                    logSubStep("Metadata loaded: ${shard.info!!.name} v${shard.info!!.version}")

                    // Load dependencies
                    logSubStep("Resolving dependencies")
                    loadDependenciesForShard(shard, dataDir)

                    // Install shard
                    logSubStep("Installing shard components")
                    installShard(shard)

                    // Record success
                    val loadTime = System.currentTimeMillis() - timerStart
                    loadMetrics[shardName] = Pair(loadTime, true)
                    logSubStep("✓ Load completed in ${"%.3f".format(loadTime/1000.0)}s")

                    shards[shard.info!!.id] = shard
                } catch (e: Exception) {
                    val loadTime = System.currentTimeMillis() - timerStart
                    loadMetrics[shardName] = Pair(loadTime, false)
                    logger.error("│   [!] FAILED TO LOAD SHARD (after ${"%.3f".format(loadTime/1000.0)}s)", e)
                } finally {
                    logger.info("└──────────────────────────────────────")
                }
            }
        } catch (e: Exception) {
            logger.error("│   [!] CRITICAL ERROR IN SHARD LOADER", e)
        }
    }

    private fun loadDependenciesForShard(shard: Shard, dataDir: Path) {
        val timerStart = System.currentTimeMillis()
        try {
            val resolver = DirectMavenResolver()
            val depLoader = try {
                ServiceLoader.load(ShardDependencyLoader::class.java, shard.loader)
            } catch (e: ClassNotFoundException) {
                logSubStep("No dependency loader specified - skipping", 2)
                return
            }

            if(depLoader.count() == 0) {
                logSubStep("No dependency loader found - skipping", 2)
                return
            }
            if(depLoader.count() > 1) {
                logSubStep("Multiple dependency loaders found - using the first one", 2)
            }
            logSubStep("Using dependency loader: ${depLoader.first().javaClass.name}", 2)
            depLoader.first().loadDependencies(resolver)

            val loadTime = System.currentTimeMillis() - timerStart
            logSubStep("Dependencies resolved in ${"%.3f".format(loadTime/1000.0)}s", 2)
        } catch (e: Exception) {
            logger.error("│     [!] DEPENDENCY RESOLUTION FAILED", e)
            throw e // Re-throw to mark shard as failed
        }
    }

    private fun installShard(shard: Shard) {
        val timerStart = System.currentTimeMillis()
        try {
            val info = shard.info!!

            // Handle dependencies
            if (!info.deps.isNullOrEmpty()) {
                val deps = info.deps!!
                logSubStep("Checking ${deps.size} dependencies", 2)
                deps.forEach { dep ->
                    when {
                        shards[dep] == null -> {
                            logSubStep("Missing dependency: $dep", 3)
                            throw IllegalStateException("Dependency $dep not found")
                        }
                        shards[dep]!!.info!!.deps?.contains(info.id) == true -> {
                            logSubStep("Circular dependency: $dep", 3)
                            throw IllegalStateException("Circular dependency with $dep")
                        }
                        !shards[dep]!!.isSetup -> {
                            logSubStep("Initializing dependency: $dep", 3)
                            initializeShard(shards[dep]!!)
                        }
                    }
                }
            }

            // Initialize main shard
            initializeShard(shard)

            val loadTime = System.currentTimeMillis() - timerStart
            logSubStep("Installation completed in ${"%.3f".format(loadTime/1000.0)}s", 2)
        } catch (e: Exception) {
            logger.error("│     [!] INSTALLATION FAILED", e)
            throw e // Re-throw to mark shard as failed
        }
    }

    private fun initializeShard(shard: Shard) {
        shard.create()
        logger.info("│   Loading commands...")
        val commandLoader = ShardCommandLoader(shard.loader, logger)
        val commands = commandLoader.loadCommands()

        if (commands.isNotEmpty()) {
            logger.info("│   ┌─ Commands Registered ────────────")
            commands.forEach { cmd ->
                val minestomCommand = ShardCommandLoader.MinestomCommand(cmd)
                MinecraftServer.getCommandManager().register(minestomCommand)
                logger.info("│   ├─ ${cmd.name.padEnd(15)} (${cmd.aliases.joinToString(", ")})")
                cmd.arguments?.forEach { arg ->
                    logger.info("│   │  └─ ${arg.id.padEnd(15)} - ${arg.javaClass.name.split(".").last()}")
                }
                cmd.subcommands?.values?.forEach { sub ->
                    logger.info("│   │  ◦ ${sub.path.padEnd(15)} - ${sub.description.take(80)}")
                    sub.arguments?.forEach { arg ->
                        logger.info("│   │  │  └─ ${arg.id.padEnd(15)} - ${arg.javaClass.name.split(".").last()}")
                    }
                }
            }
            logger.info("│   └─────────────────────────────────")
        }
        shard.isSetup = true
    }

    fun trashModules() {
        logStep("CLEANUP", "Unloading all shards")
        shards.values.forEach { shard ->
            try {
                shard.delete()
                logSubStep("Unloaded ${shard.info?.name ?: "UNKNOWN"}")
            } catch (e: Exception) {
                logger.error("│   [!] FAILED TO UNLOAD SHARD", e)
            }
        }
        logger.info("└──────────────────────────────────────")
    }

    data class CommandOpts(
        val name: String,
        val cmd: ShardCommand,
        val aliases: Array<String>,
        val description: String,
        val usage: String,
        val permission: String,
        val arguments: List<Argument<*>>? = null,
        val subcommands: Map<Method, SubcommandOpts>? = null
    )

    data class SubcommandOpts(
        val path: String,
        val description: String,
        val usage: String,
        val arguments: List<Argument<*>>? = null
    )
}