package host.minestudio.frost.impl

import java.util.*
import java.util.function.BiConsumer

/**
 * Represents a registry.
 * @param K Key
 * @param V Value
 */
@Suppress("unused")
interface Registry<K, V> : Iterable<V> {

    /**
     * Returns the registry core - a [Map].
     *
     * @return The registry
     */
    fun getRegistry(): MutableMap<K, V>

    /**
     * Register an entry
     *
     * @param key - The key
     * @param value - The value
     */
    fun register(key: K, value: V) {
        getRegistry()[key] = value
    }

    /**
     * Get a value from the key
     *
     * @param key - The key of the entry
     * @return Returns a [Optional] value.
     */
    operator fun get(key: K): Optional<V & Any> {
        return Optional.ofNullable(getRegistry()[key])
    }

    /**
     * Unregister an entry
     *
     * @param key - The key of the entry you want to unregister
     */
    fun unregister(key: K) {
        getRegistry().toMutableMap().remove(key)
    }

    /**
     * Iterate over the entries
     *
     * @param consumer - The [BiConsumer] / the action
     */
    fun iterate(consumer: BiConsumer<K, V>) {
        for (entry in getRegistry().entries) {
            consumer.accept(entry.key, entry.value)
        }
    }

    /**
     * Check if an entry contains the specified key
     *
     * @param key The key of the entry
     * @return Returns a [Boolean], telling you if the registry contains the specified key
     */
    fun containsKey(key: K): Boolean {
        return getRegistry().containsKey(key)
    }

    /**
     * Check if an entry contains the specified value
     *
     * @param value The value of the entry
     * @return Returns a [Boolean], telling you if the registry contains the specified value
     */
    fun containsValue(value: V): Boolean {
        return getRegistry().containsValue(value)
    }

    /**
     * Get all the keys in the registry.
     *
     * @return A [Set] containing all the keys in the registry
     */
    fun keySet(): Set<K> {
        return getRegistry().keys
    }

    /**
     * Get all the entries in the registry
     *
     * @return A [Set] containing all the entries in the registry
     */
    fun entrySet(): Set<Map.Entry<K, V>> {
        return getRegistry().entries
    }

    /**
     * Get all the values in the registry
     *
     * @return A [Collection] containing all the values in the registry
     */
    fun values(): Collection<V> {
        return getRegistry().values
    }

    override fun iterator(): Iterator<V> {
        return values().iterator()
    }
}