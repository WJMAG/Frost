package host.minestudio.frost.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed class SettingConfig {
    abstract val id: String
    abstract val label: String
    abstract val description: String?
    abstract val defaultValue: JsonElement?
    abstract val disabled: Boolean?
    abstract val info: JsonElement?
}

@Serializable
@SerialName("switch")
data class SwitchConfig(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val defaultValue: JsonElement? = null,
    override val disabled: Boolean? = null,
    override val info: JsonElement? = null
) : SettingConfig()
@Serializable
@SerialName("select")
data class SelectConfig(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val options: List<SelectOption>,
    override val defaultValue: JsonElement? = null,
    override val disabled: Boolean? = null,
    override val info: JsonElement? = null,
) : SettingConfig()

@Serializable
@SerialName("input")
data class InputConfig(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val inputType: String? = null,
    val placeholder: String? = null,
    override val defaultValue: JsonElement? = null,
    val suffix: InputSuffix? = null,
    override val disabled: Boolean? = null,
    override val info: JsonElement? = null,
) : SettingConfig()

@Serializable
@SerialName("slider")
data class SliderConfig(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null,
    override val defaultValue: JsonElement? = null,
    val labels: List<String>? = null,
    override val disabled: Boolean? = null,
    override val info: JsonElement? = null,
) : SettingConfig()
