package host.minestudio.frost.impl

import org.eclipse.collections.api.factory.Maps

class EclipseStore<K, V> : Registry<K, V> {
    protected val map: MutableMap<K, V> = Maps.mutable.empty()
    override fun getRegistry(): MutableMap<K, V> {
        return this.map
    }
}