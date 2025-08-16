package host.minestudio.frost.api.util

import kotlinx.serialization.json.*

fun JsonElement?.path(vararg segments: Any, deliminator: Char = '.', literal: Boolean = false): JsonElement? {
    var element = this
    for (segment in segments) {
        when (segment) {
            is String -> {
                for (part in segment.split(deliminator)) {
                    val index = part.toIntOrNull()
                    element = if (!literal && index != null) {
                        (element as? JsonArray)?.get(index) ?: return null
                    } else {
                        (element as? JsonObject)?.get(part) ?: return null
                    }
                }
            }
            is Int -> {
                element = (element as? JsonArray)?.getOrNull(segment) ?: return null
            }
            else -> return null
        }
    }
    return element
}

operator fun JsonElement?.get(key: String): JsonElement? = path(key)
operator fun JsonElement?.get(index: Int): JsonElement? = path(index, literal = false)

@Suppress("unused")
val JsonElement?.string: String?
    get() = (this as? JsonPrimitive)?.contentOrNull

@Suppress("unused")
val JsonElement?.int: Int?
    get() = (this as? JsonPrimitive)?.intOrNull

@Suppress("unused")
val JsonElement?.long: Long?
    get() = (this as? JsonPrimitive)?.longOrNull

@Suppress("unused")
val JsonElement?.double: Double?
    get() = (this as? JsonPrimitive)?.doubleOrNull

@Suppress("unused")
val JsonElement?.boolean: Boolean?
    get() = (this as? JsonPrimitive)?.booleanOrNull

@Suppress("unused")
val JsonElement?.array: JsonArray?
    get() = this as? JsonArray