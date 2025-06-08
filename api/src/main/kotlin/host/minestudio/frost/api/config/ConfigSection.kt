package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSection(
    val id: String,
    val title: String,
    val icon: String,
    val groups: List<SettingGroup>
)