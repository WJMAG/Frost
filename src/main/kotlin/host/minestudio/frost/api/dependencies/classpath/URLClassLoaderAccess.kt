package host.minestudio.frost.api.dependencies.classpath

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

/*
* This file is part of LuckPerms, licensed under the MIT License.
*
*  Copyright (c) lucko (Luck) <luck@lucko.me>
*  Copyright (c) contributors
*
*  Permission is hereby granted, free of charge, to any person obtaining a copy
*  of this software and associated documentation files (the "Software"), to deal
*  in the Software without restriction, including without limitation the rights
*  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*  copies of the Software, and to permit persons to whom the Software is
*  furnished to do so, subject to the following conditions:
*
*  The above copyright notice and this permission notice shall be included in all
*  copies or substantial portions of the Software.
*
*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
*  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
*  SOFTWARE.
*/

/**
 * Provides access to [URLClassLoader]#addURL.
 */
abstract class URLClassLoaderAccess protected constructor(private val classLoader: URLClassLoader?) {
    /**
     * Adds the given URL to the class loader.
     *
     * @param url the URL to add
     */
    abstract fun addURL(url: URL)

    /**
     * Accesses using reflection, not supported on Java 9+.
     */
    private class Reflection(classLoader: URLClassLoader?) : URLClassLoaderAccess(classLoader) {
        override fun addURL(url: URL) {
            try {
                ADD_URL_METHOD!!.invoke(super.classLoader, url)
            } catch (e: ReflectiveOperationException) {
                throwError(e)
            }
        }

        companion object {
            private val ADD_URL_METHOD: Method?

            init {
                var addUrlMethod: Method?
                try {
                    addUrlMethod = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
                    addUrlMethod.setAccessible(true)
                } catch (e: Exception) {
                    addUrlMethod = null
                }
                ADD_URL_METHOD = addUrlMethod
            }

            val isSupported: Boolean
                get() = ADD_URL_METHOD != null
        }
    }

    /**
     * Accesses using sun.misc.Unsafe, supported on Java 9+.
     *
     * @author Vaishnav Anil (https://github.com/slimjar/slimjar)
     */
    private class Unsafe(classLoader: URLClassLoader?) : URLClassLoaderAccess(classLoader) {
        private val unopenedURLs: MutableCollection<URL?>?
        private val pathURLs: MutableCollection<URL?>?

        init {
            var unopenedURLs: MutableCollection<URL?>?
            var pathURLs: MutableCollection<URL?>?
            try {
                val ucp = fetchField(URLClassLoader::class.java, classLoader, "ucp")
                unopenedURLs = fetchField(ucp.javaClass, ucp, "unopenedUrls") as MutableCollection<URL?>?
                pathURLs = fetchField(ucp.javaClass, ucp, "path") as MutableCollection<URL?>?
            } catch (e: Throwable) {
                unopenedURLs = null
                pathURLs = null
            }

            this.unopenedURLs = unopenedURLs
            this.pathURLs = pathURLs
        }

        override fun addURL(url: URL) {
            if (this.unopenedURLs == null || this.pathURLs == null) {
                throwError(NullPointerException("unopenedURLs or pathURLs"))
            }

            synchronized(this.unopenedURLs!!) {
                this.unopenedURLs.add(url)
                this.pathURLs!!.add(url)
            }
        }

        companion object {
            private val UNSAFE: sun.misc.Unsafe?

            init {
                var unsafe: sun.misc.Unsafe?
                try {
                    val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
                    unsafeField.setAccessible(true)
                    unsafe = unsafeField.get(null) as sun.misc.Unsafe?
                } catch (t: Throwable) {
                    unsafe = null
                }
                UNSAFE = unsafe
            }

            val isSupported: Boolean
                get() = UNSAFE != null

            @Throws(NoSuchFieldException::class)
            private fun fetchField(clazz: Class<*>, `object`: Any?, name: String): Any {
                val field = clazz.getDeclaredField(name)
                val offset = UNSAFE!!.objectFieldOffset(field)
                return UNSAFE.getObject(`object`, offset)
            }
        }
    }

    private class Noop : URLClassLoaderAccess(null) {
        override fun addURL(url: URL) {
            throwError(null)
        }

        companion object {
            val INSTANCE = Noop()
        }
    }

    companion object {
        /**
         * Creates a [URLClassLoaderAccess] for the given class loader.
         *
         * @param classLoader the class loader
         * @return the access object
         */
        fun create(classLoader: URLClassLoader?): URLClassLoaderAccess {
            if (Reflection.Companion.isSupported) {
                return Reflection(classLoader)
            } else if (Unsafe.Companion.isSupported) {
                return Unsafe(classLoader)
            } else {
                return Noop.Companion.INSTANCE
            }
        }

        @Throws(UnsupportedOperationException::class)
        private fun throwError(cause: Throwable?) {
            throw UnsupportedOperationException(
                "Control Center is unable to inject into the plugin URLClassLoader.\n" +
                        "You may be able to fix this problem by adding the following command-line argument " +
                        "directly after the 'java' command in your start script: \n'--add-opens java.base/java.lang=ALL-UNNAMED'",
                cause
            )
        }
    }
}