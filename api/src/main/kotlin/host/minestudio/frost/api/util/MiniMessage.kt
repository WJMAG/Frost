import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/** Global [MiniMessage] instance. */
private val miniMessage = MiniMessage.builder()
    .tags(TagResolver.resolver(
        TagResolver.standard(), // Default tags

        TagResolver.resolver("green", Tag.styling(TextColor.fromHexString("#28A745")!!)),
        TagResolver.resolver("g", Tag.styling(TextColor.fromHexString("#28A745")!!)),
        TagResolver.resolver("description", Tag.styling(TextColor.fromHexString("#A0A0A0")!!)),
        TagResolver.resolver("d", Tag.styling(TextColor.fromHexString("#A0A0A0")!!)),
        TagResolver.resolver("warning", Tag.styling(TextColor.fromHexString("#FF5733")!!)),
        TagResolver.resolver("warn", Tag.styling(TextColor.fromHexString("#FF5733")!!)),

        Placeholder.parsed("error", "<warn>⚠</warn> <color:#FF7F6E>"),
        Placeholder.parsed("success", "<g>✔</g> <color:#6F8F6D>"),
        Placeholder.parsed("info", "<color:#66CCCC>ℹ</color> <d>"),
    ))
    .build()

@Suppress("unused")
fun String.mm(
    andOld: Boolean = true,
    vararg resolvers: TagResolver,
): Component {
    val mm = miniMessage.deserialize(this, *resolvers)
        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    return if (!andOld) mm else mm.mmOld()
}

fun String.mmOld(): Component {
    return LegacyComponentSerializer.legacyAmpersand().deserialize(this)
        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
}

fun Component.mmOld(): Component {
    return miniMessage.serialize(this).mmOld()
}

val Component.string: String
    get() = miniMessage.serialize(this)