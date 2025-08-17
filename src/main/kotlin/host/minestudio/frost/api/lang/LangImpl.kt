package host.minestudio.frost.api.lang

import java.text.MessageFormat
import java.util.*


class LangImpl(private val bundle: ResourceBundle) : Lang {

    override fun lang(key: String, vararg args: Any?): String {
        checkNotNull(bundle) { "Bundle not yet loaded" }
        val pattern = bundle.getString(key)
        return MessageFormat.format(pattern, *args)
    }
    override fun lang(key: String): String {
        checkNotNull(bundle) { "Bundle not yet loaded" }
        return bundle.getString(key)
    }

}