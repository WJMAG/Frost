package host.minestudio.frost.api.shards

import host.minestudio.frost.api.dependencies.resolver.DirectMavenResolver
import host.minestudio.frost.api.dependencies.resolver.impl.SimpleLibraryStore
import host.minestudio.frost.api.shards.annotations.ShardConfig
import host.minestudio.frost.api.shards.command.ShardCommand
import host.minestudio.frost.api.shards.impl.ShardHelperImpl
import host.minestudio.frost.spawnWorld
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.arguments.Argument
import org.eclipse.aether.graph.Dependency
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.time.measureTimedValue

class ShardManager {
    private val logger: Logger = LoggerFactory.getLogger(ShardManager::class.java)
    val shards = ConcurrentHashMap<String, Shard>()
    private val loadMetrics = ConcurrentHashMap<String, ShardLoadResult>()

    fun setup(dataDir: File) {
        logBanner("Initializing Shard Manager")

        try {
            logStep("SETUP") { "Initializing shard loader" }
            val shardDir = prepareShardDirectory(dataDir.toPath())

            logStep("LOAD") { "Beginning shard loading process" }
            val discoveredShards = discoverShards(shardDir)
            processShards(discoveredShards, dataDir.toPath())
        } finally {
            logStep("COMPLETE") { "Shard loading finished" }
            printLoadMetrics()

            Runtime.getRuntime().addShutdownHook(Thread {
                logStep("SHUTDOWN") { "Cleaning up shards" }
                trashModules()
                logSubStep("All shards cleaned up")
                logDivider()
            })
        }
    }

    private fun prepareShardDirectory(dataDir: Path): File {
        return File(dataDir.toFile(), "shards").apply {
            if (!exists() && mkdirs()) {
                logSubStep("Created shard directory: $absolutePath")
            }
            require(isDirectory) { "Shard directory is not valid: $absolutePath" }
        }
    }

    private fun discoverShards(shardDir: File): List<File> {
        val jars = shardDir.listFiles().orEmpty()
        logSubStep("Discovered ${jars.size} potential shard(s)")
        jars.forEach { logSubStep("Found: ${it.name}", 2) }
        return jars.toList()
    }

    private fun processShards(shardFiles: List<File>, dataDir: Path) {
        val shardsAndLoaders: MutableList<Pair<Shard, ShardClassLoader>> = mutableListOf()
        for(file in shardFiles) {
            if (!file.name.endsWith(".jar")) {
                logger.warn("Skipping non-jar file: ${file.name}")
                continue
            }

            try {
                val loader = ShardClassLoader(arrayOf(file.toURI().toURL()), ShardManager::class.java.classLoader, dataDir.toFile())
                val shard = ServiceLoader.load(Shard::class.java, loader).firstOrNull()
                if (shard != null) {
                    shardsAndLoaders.add(Pair(shard, loader))
                    logSubStep("Loaded shard: ${shard.getInfo()?.name ?: "UNKNOWN"} from ${file.name}")
                } else {
                    logger.warn("No shard found in ${file.name}")
                }
            } catch (e: Exception) {
                logger.error("Failed to load shard from ${file.name}", e)
            }
        }

        logSubStep("All shards loaded - ${shardsAndLoaders.count()} shard(s) detected")
        logDivider()

        // Step 2: Process all shards in parallel
        val shardsWithDependencies = shardsAndLoaders.mapNotNull { (shard, loader) ->
            processShard(shard, loader, dataDir).also { result ->
                if (result != null) {
                    shards[result.shard.getInfo()!!.id] = result.shard
                }
            }
        }

        // Step 3: Report dependencies
        reportDependencies(shardsWithDependencies.flatMap { it.dependencies })

        // Step 4: Register libraries
        registerLibraries(shardsWithDependencies.map { it.libraryInfo })

        // Step 5: Install shards
        installShards(shardsWithDependencies.map { it.shard })
    }

    private data class ProcessedShardResult(
        val shard: Shard,
        val dependencies: List<Pair<Dependency, Class<*>>>,
        val libraryInfo: Pair<ShardClassLoader, Pair<SimpleLibraryStore, DirectMavenResolver>>
    )

    private fun processShard(shard: Shard, loader: ShardClassLoader, dataDir: Path): ProcessedShardResult? {
        val shardName = shard.getInfo()?.name!!
        var result: ProcessedShardResult?
        var durationMs: Long
        try {
            val timed = measureTimedValue {
                logStep("SHARD") { "Processing ${shard.jarLocation?.file?.substringAfterLast('/') ?: "UNKNOWN"}" }

                setupShardContext(shard, loader, dataDir)
                logSubStep("Metadata loaded: ${shard.getInfo()!!.name} v${shard.getInfo()!!.version}")

                val dependencyResult = loadDependenciesForShard(shard)
                if (dependencyResult != null) {
                    logSubStep("Dependencies resolved with ${shard.getClassLoader()!!.javaClass.simpleName}", 2)
                } else {
                    logSubStep("No dependencies to resolve", 2)
                }

                dependencyResult?.let {
                    ProcessedShardResult(
                        shard,
                        it.second.second.getDependencies()
                            .map { dep -> Pair(dep.key!!, dep.value!!) },
                        it
                    )
                }
            }
            result = timed.value
            durationMs = timed.duration.inWholeMilliseconds
        } catch (e: Exception) {
            logger.error("│   [!] FAILED TO PROCESS SHARD", e)
            loadMetrics[shardName] = ShardLoadResult(shardName, 0, false, e.message)
            return null
        } finally {
            logDivider()
        }
        loadMetrics[shardName] = ShardLoadResult(
            shardName,
            durationMs,
            result != null,
            if (result == null) "Failed to process" else null
        )
        return result
    }

    private fun reportDependencies(dependencies: List<Pair<Dependency, Class<*>>>) {
        logger.info("┌─ DEPENDENCY REPORT ──────────────────────")
        logger.info("│   Loading ${dependencies.size} dependencies for ${shards.size} shard(s)")
        dependencies.forEach { (dep, owningClass) ->
            logger.info("│     ${dep.artifact.groupId}:${dep.artifact.artifactId ?: "UNKNOWN"}:${dep.artifact.version ?: "UNKNOWN"} (for ${owningClass.name ?: "unknown"})")
        }
        logger.info("└──────────────────────────────────────")
    }

    private fun registerLibraries(libraryInfos: List<Pair<ShardClassLoader, Pair<SimpleLibraryStore, DirectMavenResolver>>>) {
        logger.info("┌─ REGISTERING LIBRARIES ──────────────────────")

        try {
            libraryInfos.forEach { (shardClassLoader, libraryPair) ->
                val (store, _) = libraryPair
                try {
                    logger.info("│   Registering libraries for $shardClassLoader")
                    store.paths.forEach { path ->
                        if (path == null || !path.exists()) {
                            logger.warn("│   [!] Library path does not exist: $path")
                            return@forEach
                        }
                        logger.info("│   ${path.fileName.toString().split("/").last()}")
                        try {
                            shardClassLoader.addDependencyURL(path.toUri().toURL())
                            logger.info("|     └─ ✅")
                        } catch (_: Exception) {
                            logger.error("|     └─ ❌")
                            return@forEach
                        }
                    }
                    logger.info("│   └─ Libraries added successfully")
                } catch (e: Exception) {
                    logger.error("│   [!] FAILED TO REGISTER LIBRARIES", e)
                }
            }
        } finally { }
        logger.info("└──────────────────────────────────────")
    }

    private fun installShards(shardsToInstall: List<Shard>) {
        shardsToInstall.forEach { shard ->
            try {
                val (_, duration) = measureTimedValue {
                    installShard(shard)
                }
                loadMetrics[shard.getInfo()?.name ?: "UNKNOWN"] = ShardLoadResult(
                    shard.getInfo()?.name ?: "UNKNOWN",
                    duration.inWholeMilliseconds,
                    true
                )
            } catch (e: Exception) {
                logger.error("│   [!] FAILED TO INSTALL SHARD", e)
                loadMetrics[shard.getInfo()?.name ?: "UNKNOWN"] = ShardLoadResult(
                    shard.getInfo()?.name ?: "UNKNOWN",
                    0,
                    false,
                    e.message
                )
            }
        }
    }

    private fun setupShardContext(shard: Shard, shardLoader: ShardClassLoader, dataDir: Path) {
        Thread.currentThread().contextClassLoader = shardLoader

        val shardInfo = shard.javaClass.getAnnotation(ShardConfig::class.java)?.let {
            ShardInfo(it.name, it.id, it.version, "unused", it.deps.toList())
        } ?: throw IllegalStateException("ShardConfig annotation missing on ${shard.javaClass.name}")

        shard.init(shardLoader, ShardHelperImpl(shardInfo.id, shardInfo.name), shardInfo, spawnWorld, dataDir.toFile())

        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        // Pre-setup phase
        logger.info("┌─ PRE-SETUP ───────────────────────────────")
        shard.isInPreSeup = true
        shard.presetup()
        shard.isInPreSeup = false
        shard.isPreSetup = true
        logger.info("└──────────────────────────────────────────")
    }

    private fun loadDependenciesForShard(shard: Shard): Pair<ShardClassLoader, Pair<SimpleLibraryStore, DirectMavenResolver>>? {
        return measureTimedValue {
            try {
                val resolver = DirectMavenResolver()
                val store = SimpleLibraryStore()

                val shardAppointedResolver = shard.getShardAppointedDependencyLoader()
                if (shardAppointedResolver == null) {
                    logger.warn("│     No appointed dependency loader found for ${shard.getInfo()?.name ?: "UNKNOWN"}")
                    return@measureTimedValue null
                }
                logSubStep("Loading dependencies with ${shardAppointedResolver.javaClass.simpleName}", 2)
                shardAppointedResolver.loadDependencies(resolver)

                resolver.register(store)
                Pair(shard.getClassLoader()!!, Pair(store, resolver))
            } catch (e: Exception) {
                logger.error("│     [!] DEPENDENCY RESOLUTION FAILED", e)
                throw e
            }
        }.also { (_, duration) ->
            logSubStep("Dependencies resolved in ${duration.inWholeMilliseconds}", 2)
        }.value
    }

    private fun installShard(shard: Shard) {
        // Check dependencies first
        shard.getInfo()?.deps.orEmpty().forEach { dep ->
            val depShard = shards[dep] ?: throw IllegalStateException("Missing dependency: $dep")

            if (depShard.getInfo()?.deps?.contains(shard.getInfo()!!.id) == true) {
                throw IllegalStateException("Circular dependency with $dep")
            }

            if (!depShard.isSetup) {
                logSubStep("Initializing dependency: $dep", 3)
                initializeShard(depShard)
            }
        }

        initializeShard(shard)
    }

    private fun initializeShard(shard: Shard) {
        // Main setup phase
        logger.info("┌─ SETUP ───────────────────────────────────")
        shard.isInSetup = true
        shard.create()
        shard.isInSetup = false
        shard.isSetup = true
        logger.info("└──────────────────────────────────────────")

        // Register commands
        registerShardCommands(shard)
    }

    private fun registerShardCommands(shard: Shard) {
        val commandLoader = ShardCommandLoader(shard, logger)
        val commands = commandLoader.loadCommands()

        if (commands.isNotEmpty()) {
            logger.info("┌─ Commands Registered ────────────")
            commands.forEach { cmd ->
                val minestomCmd = ShardCommandLoader.MinestomCommand(cmd)
                MinecraftServer.getCommandManager().register(minestomCmd)

                logger.info("├─ ${cmd.name.padEnd(15)} (${cmd.aliases.joinToString(", ")})")
                cmd.arguments?.forEach {
                    logger.info("│  └─ ${it.id.padEnd(15)} - ${it.javaClass.simpleName}")
                }
                cmd.subcommands?.values?.forEach { sub ->
                    logger.info("│  ◦ ${sub.path.padEnd(15)} - ${sub.description.take(80)}")
                    sub.arguments?.forEach {
                        logger.info("│  │  └─ ${it.id.padEnd(15)} - ${it.javaClass.simpleName}")
                    }
                }
            }
            logger.info("└─────────────────────────────────")
        }
    }

    fun trashModules() {
        logStep("CLEANUP") { "Unloading all shards" }
        shards.values.forEach { shard ->
            try {
                shard.delete()
                logSubStep("Unloaded ${shard.getInfo()?.name ?: "UNKNOWN"}")
            } catch (e: Exception) {
                logger.error("│   [!] FAILED TO UNLOAD SHARD", e)
            }
        }
        logDivider()
    }

    // Improved logging methods
    private fun logBanner(title: String) {
        logger.info("╔══════════════════════════════════════╗")
        logger.info("║        ${title.padEnd(30)} ║")
        logger.info("╚══════════════════════════════════════╝")
    }

    private fun logStep(phase: String, message: () -> String) {
        logger.info("┌─[$phase] ${message().uppercase()}")
    }

    private fun logSubStep(message: String, indentLevel: Int = 1) {
        logger.info("│ ${"  ".repeat(indentLevel)}$message")
    }

    private fun logDivider() {
        logger.info("└──────────────────────────────────────")
    }

    private fun printLoadMetrics() {
        logBanner("Shard Load Metrics")

        loadMetrics.forEach { (name, result) ->
            val status = if (result.success) "✓" else "✗"
            val time = if (result.durationMs > 0) "%.3fs".format(result.durationMs / 1000.0).padStart(7) else "N/A"
            val error = result.errorMessage?.let { " - $it" } ?: ""
            logger.info("║ ${name.padEnd(25)}: $time $status$error ║")
        }

        val totalTime = loadMetrics.values.sumOf { it.durationMs }
        val successCount = loadMetrics.values.count { it.success }

        logger.info("╠══════════════════════════════════════╣")
        logger.info("║ Total: ${"%.3fs".format(totalTime / 1000.0).padStart(16)} | $successCount/${loadMetrics.size} loaded ║")
        logger.info("╚══════════════════════════════════════╝")
    }

    private data class ShardLoadResult(
        val shardName: String,
        val durationMs: Long,
        val success: Boolean,
        val errorMessage: String? = null
    )

    data class CommandOpts(
        val name: String,
        val cmd: ShardCommand,
        val aliases: Array<String>,
        val description: String,
        val usage: String,
        val permission: String,
        val arguments: List<Argument<*>>? = null,
        val subcommands: Map<Method, SubcommandOpts>? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CommandOpts

            if (name != other.name) return false
            if (cmd != other.cmd) return false
            if (!aliases.contentEquals(other.aliases)) return false
            if (description != other.description) return false
            if (usage != other.usage) return false
            if (permission != other.permission) return false
            if (arguments != other.arguments) return false
            if (subcommands != other.subcommands) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + cmd.hashCode()
            result = 31 * result + aliases.contentHashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + usage.hashCode()
            result = 31 * result + permission.hashCode()
            result = 31 * result + (arguments?.hashCode() ?: 0)
            result = 31 * result + (subcommands?.hashCode() ?: 0)
            return result
        }
    }

    data class SubcommandOpts(
        val path: String,
        val description: String,
        val usage: String,
        val arguments: List<Argument<*>>? = null
    )
}