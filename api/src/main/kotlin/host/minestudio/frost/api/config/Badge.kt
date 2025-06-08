package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable

@Serializable
data class Badge(
    val text: String,
    val variant: String? = null
)