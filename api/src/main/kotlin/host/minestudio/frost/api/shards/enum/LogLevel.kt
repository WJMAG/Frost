package host.minestudio.frost.api.shards.enum

enum class LogLevel {

    /**
     * Trace level for finer-grained informational events than the DEBUG level.
     */
    TRACE,

    /**
     * Debug level for detailed information, typically used for diagnosing issues.
     */
    DEBUG,

    /**
     * Info level for general informational messages that highlight the progress of the shard.
     */
    INFO,

    /**
     * Warn level for potentially harmful situations.
     */
    WARN,

    /**
     * Error level for error events that might still allow the shard to continue running.
     */
    ERROR,

    /**
     * Fatal level for very severe error events that will presumably lead the shard to abort.
     */
    FATAL,

}