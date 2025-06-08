package host.minestudio.frost.api.shards.helper

import host.minestudio.frost.api.shards.enum.LogLevel

class LogEmitter(private val logger: ((level: LogLevel, message: String) -> Unit)) {

    /**
     * Emits a log message with the specified log level.
     *
     * @param level The log level of the message.
     * @param message The log message to emit.
     */
    fun emit(level: LogLevel, message: String) {
        logger(level, message)
    }

    /**
     * Emits a TRACE level log message.
     *
     * @param message The log message to emit.
     */
    fun trace(message: String) {
        emit(LogLevel.TRACE, message)
    }

    /**
     * Emits a DEBUG level log message.
     *
     * @param message The log message to emit.
     */
    fun debug(message: String) {
        emit(LogLevel.DEBUG, message)
    }

    /**
     * Emits an INFO level log message.
     *
     * @param message The log message to emit.
     */
    fun info(message: String) {
        emit(LogLevel.INFO, message)
    }

    /**
     * Emits a WARN level log message.
     *
     * @param message The log message to emit.
     */
    fun warn(message: String) {
        emit(LogLevel.WARN, message)
    }

    /**
     * Emits an ERROR level log message.
     *
     * @param message The log message to emit.
     */
    fun error(message: String) {
        emit(LogLevel.ERROR, message)
    }

    /**
     * Emits a FATAL level log message.
     *
     * @param message The log message to emit.
     */
    fun fatal(message: String) {
        emit(LogLevel.FATAL, message)
    }

}