// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.classloader

import dev.kokoroidkt.coreApi.classloader.ExtensionClassloader
import dev.kokoroidkt.coreApi.logging.KokoroidLogger
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

open class ExtensionClassLoaderImpl(
    jarFile: File,
    parent: ClassLoader? = Thread.currentThread().contextClassLoader,
) : ExtensionClassloader(parent) {
    private var _logger: KokoroidLogger? = null

    override var logger: KokoroidLogger
        get() = _logger ?: throw IllegalStateException("Logger is not initialized")
        set(value) {
            _logger = value
        }

    private val jarFile = JarFile(jarFile)

    internal fun isExtensionCorrect(): Boolean = true

    override fun findClass(className: String): Class<*> {
        try {
            val entryName = className.replace('.', '/') + ".class"
            val entry =
                this@ExtensionClassLoaderImpl.jarFile.getEntry(entryName) ?: return parent.loadClass(className)
            return this@ExtensionClassLoaderImpl.jarFile.getInputStream(entry).use { input ->
                val bytes = input.readBytes()
                defineClass(className, bytes, 0, bytes.size)
            }
        } catch (e: IOException) {
            throw ClassNotFoundException("Failed to load class $className from JAR", e)
        } catch (e: ClassNotFoundException) {
            // 重新抛出父类加载器的 ClassNotFoundException
            throw ClassNotFoundException(
                "Failed to load class $className from JAR " +
                    "because cannot find this class from both JAR and parent",
                e,
            )
        }
    }
}
