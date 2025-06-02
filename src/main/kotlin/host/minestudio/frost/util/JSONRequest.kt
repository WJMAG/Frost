package host.minestudio.frost.util

/*
 * Copyright (C) 2025 MineStudio Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import host.minestudio.frost.logger
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Sends an HTTP request and returns a JSON response.
 * @param url The URL to send the request to
 * @param requestMethod The HTTP request method to use
 * @param requestProperties The request headers
 * @param body The request body, if applicable
 * @return A [JsonObject] containing the response
 * @throws IOException If an error occurs while sending the request
 */
@Throws(IOException::class)
@Suppress("MagicNumber")
fun request(
    url: String,
    requestMethod: String,
    requestProperties: Map<String, String> = emptyMap(),
    body: ByteArray? = null
): JsonObject {
    try {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            this.requestMethod = requestMethod
            doInput = true
            doOutput = body != null
            useCaches = false
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            requestProperties.forEach { (key, value) -> setRequestProperty(key, value) }
            connect()
        }

        body?.let {
            connection.outputStream.use { os -> os.write(it) }
        }

        logger.info("Request: $url, ${connection.responseCode}, ${connection.responseMessage}")

        val responseStream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return responseStream.parseJson() ?: JsonObject()
    } catch (e: IOException) {
        throw IOException("Error during HTTP request: ${e.message}", e)
    }
}

/**
 * Parses an [InputStream] into a [JsonObject].
 */
private fun InputStream?.parseJson(): JsonObject? {
    if (this == null) return null
    return try {
        bufferedReader(StandardCharsets.UTF_8).use { reader ->
            JsonParser.parseString(reader.readText()).asJsonObject
        }
    } catch (e: IOException) {
        JsonParser.parseString("{\"success\": false, \"error\": \"${e.message}\"}").asJsonObject
    }
}
