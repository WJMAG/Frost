package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable

@Serializable
data class InputSuffix(
    val type: String = "select",
    val options: List<SelectOption>,
    val defaultValue: String? = null
)