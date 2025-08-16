package host.minestudio.frost.api.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class ConfigSchema(
    val title: String,
    val description: String? = null,
    val sections: List<ConfigSection>
) {
    @Suppress("unused")
    fun toJson(): String {
        val json = Json {
            prettyPrint = true
            classDiscriminator = "type"
            serializersModule = serializers
        }
        return json.encodeToString(this)
    }

    companion object {
        val serializers = SerializersModule {
            polymorphic(SettingConfig::class) {
                subclass(SwitchConfig::class)
                subclass(SelectConfig::class)
                subclass(InputConfig::class)
                subclass(SliderConfig::class)
            }
        }
    }
}