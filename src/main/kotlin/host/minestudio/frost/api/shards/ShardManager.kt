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
    private val loadMetrics = LinkedHashMap<String, Pair<Long, Boolean>>()

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

    private fun setupShardLoader(dataDir: Path) {
        try {
            val shardDir = File(dataDir.toFile(), "shards").apply {
                if (!exists() && mkdirs()) logSubStep("Created shard directory: $absolutePath")
                require(isDirectory) { "Shard directory is not valid: $absolutePath" }
            }

            val jars = shardDir.listFiles().orEmpty()
            logSubStep("Discovered ${jars.size} potential shard(s)")
            jars.forEach { logSubStep("Found: ${it.name}", 2) }

            shardLoader = ShardClassLoader(
                jars.map { it.toURI().toURL() }.toTypedArray(),
                this::class.java.classLoader,
                dataDir.toFile()
            )
        } catch (e: Exception) {
            logger.error("│   [!] FAILED TO INITIALIZE SHARD LOADER", e)
        }
    }

    private fun loadAndInstallShards(dataDir: Path) {
        try {
            val serviceLoader = ServiceLoader.load(Shard::class.java, shardLoader)
            logSubStep("Service loader prepared - ${serviceLoader.count()} shard(s) detected")

            serviceLoader.forEach { shard ->
                val timerStart = System.currentTimeMillis()
                var shardName = shard.info?.name ?: "UNKNOWN"

                try {
                    logStep("SHARD", "Processing ${shard.jarLocation?.file?.substringAfterLast('/') ?: "UNKNOWN"}")
                    setupShardContext(shard, dataDir)
                    shardName = shard.info?.name ?: "UNKNOWN"

                    logSubStep("Metadata loaded: ${shard.info!!.name} v${shard.info!!.version}")
                    loadDependenciesForShard(shard, dataDir)
                    installShard(shard)

                    val loadTime = System.currentTimeMillis() - timerStart
                    loadMetrics[shardName] = loadTime to true
                    logSubStep("✓ Load completed in ${"%.3f".format(loadTime / 1000.0)}s")

                    shards[shard.info!!.id] = shard
                } catch (e: Exception) {
                    val loadTime = System.currentTimeMillis() - timerStart
                    loadMetrics[shardName] = loadTime to false
                    logger.error("│   [!] FAILED TO LOAD SHARD (after ${"%.3f".format(loadTime / 1000.0)}s)", e)
                } finally {
                    logger.info("└──────────────────────────────────────")
                }
            }
        } catch (e: Exception) {
            logger.error("│   [!] CRITICAL ERROR IN SHARD LOADER", e)
        }
    }

    private fun setupShardContext(shard: Shard, dataDir: Path) {
        Thread.currentThread().contextClassLoader = shardLoader

        val shardInfo = shard.javaClass.getAnnotation(ShardConfig::class.java)?.let {
            ShardInfo(it.name, it.id, it.version, "unused", it.deps.toList())
        } ?: throw IllegalStateException("ShardConfig annotation missing on ${javaClass.name}")

        shard.apply {
            loader = ShardClassLoader(arrayOf(jarLocation), ShardManager::class.java.classLoader, dataDir.toFile())
            info = shardInfo
            dataFolder = dataDir.toFile()
            shardHelper = ShardHelperImpl(shardInfo.id, shardInfo.name)
        }

        Thread.currentThread().contextClassLoader = this::class.java.classLoader
    }

    private fun loadDependenciesForShard(shard: Shard, dataDir: Path) {
        val timerStart = System.currentTimeMillis()
        try {
            val resolver = DirectMavenResolver()
            val loaders = runCatching {
                ServiceLoader.load(ShardDependencyLoader::class.java, shard.loader)
            }.getOrElse {
                logSubStep("No dependency loader specified - skipping", 2)
                return
            }

            if (loaders.count() == 0) {
                logSubStep("No dependency loader found - skipping", 2)
                return
            }

            logSubStep("Using dependency loader: ${loaders.first().javaClass.name}", 2)
            loaders.first().loadDependencies(resolver)

            val loadTime = System.currentTimeMillis() - timerStart
            logSubStep("Dependencies resolved in ${"%.3f".format(loadTime / 1000.0)}s", 2)
        } catch (e: Exception) {
            logger.error("│     [!] DEPENDENCY RESOLUTION FAILED", e)
            throw e
        }
    }

    private fun installShard(shard: Shard) {
        val timerStart = System.currentTimeMillis()
        try {
            shard.info?.deps.orEmpty().forEach { dep ->
                val depShard = shards[dep]
                requireNotNull(depShard) { "Missing dependency: $dep" }

                if (depShard.info?.deps?.contains(shard.info!!.id) == true)
                    throw IllegalStateException("Circular dependency with $dep")

                if (!depShard.isSetup) {
                    logSubStep("Initializing dependency: $dep", 3)
                    initializeShard(depShard)
                }
            }

            initializeShard(shard)

            val loadTime = System.currentTimeMillis() - timerStart
            logSubStep("Installation completed in ${"%.3f".format(loadTime / 1000.0)}s", 2)
        } catch (e: Exception) {
            logger.error("│     [!] INSTALLATION FAILED", e)
            throw e
        }
    }

    private fun initializeShard(shard: Shard) {
        shard.presetup()
        logger.info("│   Loading commands...")

        val commandLoader = ShardCommandLoader(shard.loader, logger)
        val commands = commandLoader.loadCommands()

        if (commands.isNotEmpty()) {
            logger.info("│   ┌─ Commands Registered ────────────")
            commands.forEach { cmd ->
                val minestomCmd = ShardCommandLoader.MinestomCommand(cmd)
                MinecraftServer.getCommandManager().register(minestomCmd)

                logger.info("│   ├─ ${cmd.name.padEnd(15)} (${cmd.aliases.joinToString(", ")})")
                cmd.arguments?.forEach {
                    logger.info("│   │  └─ ${it.id.padEnd(15)} - ${it.javaClass.simpleName}")
                }
                cmd.subcommands?.values?.forEach { sub ->
                    logger.info("│   │  ◦ ${sub.path.padEnd(15)} - ${sub.description.take(80)}")
                    sub.arguments?.forEach {
                        logger.info("│   │  │  └─ ${it.id.padEnd(15)} - ${it.javaClass.simpleName}")
                    }
                }
            }
            logger.info("│   └─────────────────────────────────")
        }

        shard.isSetup = true
        shard.create()
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

    private fun printLoadMetrics() {
        logger.info("╔══════════════════════════════════════╗")
        logger.info("║          Shard Load Metrics           ║")
        logger.info("╠══════════════════════════════════════╣")

        loadMetrics.forEach { (name, metrics) ->
            val status = if (metrics.second) "✓" else "✗"
            logger.info("║ ${name.padEnd(25)}: ${"%.3fs".format(metrics.first / 1000.0).padStart(7)} $status ║")
        }

        val totalTime = loadMetrics.values.sumOf { it.first }
        val successCount = loadMetrics.values.count { it.second }

        logger.info("╠══════════════════════════════════════╣")
        logger.info("║ Total: ${"%.3fs".format(totalTime / 1000.0).padStart(25)} | $successCount/${loadMetrics.size} loaded ║")
        logger.info("╚══════════════════════════════════════╝")
    }

    private fun logStep(phase: String, message: String) {
        logger.info("┌─[$phase] ${message.uppercase()}")
    }

    private fun logSubStep(message: String, indentLevel: Int = 1) {
        logger.info("│ ${"  ".repeat(indentLevel)}$message")
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
