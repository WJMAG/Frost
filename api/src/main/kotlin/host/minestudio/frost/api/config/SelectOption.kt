package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable

@Serializable
data class SelectOption(
    val value: String,
    val label: String,
    val badge: Badge? = null
)