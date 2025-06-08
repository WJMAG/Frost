package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.Polymorphic

@Serializable
data class SettingGroup(
    val id: String,
    val title: String,
    val description: String? = null,
    val settings: List<@Polymorphic SettingConfig>
)