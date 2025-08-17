package host.minestudio.frost.api.util

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files


object FileUtil {

    /**
     * Attempts to save a file to the specified location,
     * however, if the file already exists, it will not
     * overwrite it.
     *
     * @param stream InputStream to save
     * to the file
     * @param output File to save the InputStream to
     */
    fun trySave(stream: InputStream?, output: File) {
        if (output.exists()) return
        try {
            if (stream != null) {
                Files.copy(stream, output.toPath())
            }
        } catch (ignored: IOException) {
        } catch (ignored: NullPointerException) {
        }
    }

}