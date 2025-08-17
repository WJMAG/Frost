package host.minestudio.frost.api.lang

interface Lang {

    fun lang(key: String, vararg args: Any?): String
    fun lang(key: String): String

}