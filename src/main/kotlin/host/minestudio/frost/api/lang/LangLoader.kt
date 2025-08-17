package host.minestudio.frost.api.lang

import host.minestudio.frost.api.util.FileUtil
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*

class LangLoader(clazz: Class<*>, title: String, lang: String, variant: String) {

    private val locale: Locale = Locale.of(lang, variant)
    private val bundle: ResourceBundle

    init {
        try {
            val file = File("lang")
            if (!file.exists()) file.mkdirs()
            FileUtil.trySave(
                clazz.getClassLoader().getResourceAsStream(title + "_" + lang + "_" + variant + ".properties"),
                File("lang/" + title + "_" + lang + "_" + variant + ".properties")
            )
            val urls = listOf(file.toURI().toURL()).toTypedArray()
            URLClassLoader(urls).use {
                bundle = ResourceBundle.getBundle(title, locale, it)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load language file for $title", e)
        }
    }

    fun getBundle(): ResourceBundle {
        return bundle
    }

}