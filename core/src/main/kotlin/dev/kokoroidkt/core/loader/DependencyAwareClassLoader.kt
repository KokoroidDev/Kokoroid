// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.core.logger.getLogger
import dev.kokoroidkt.coreApi.classloader.ExtensionClassloader
import dev.kokoroidkt.coreApi.logging.KokoroidLogger
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

/**
 * A custom ClassLoader with delegation order:
 * JVM Application ClassLoader → Dependency ClassLoaders → Self (JAR).
 *
 * Unlike standard parent-first delegation, this classloader:
 * 1. Checks if the class is already loaded
 * 2. Delegates to the JVM system/application ClassLoader
 * 3. Tries each dependency ClassLoader in order
 * 4. Falls back to loading from the self JAR
 *
 * @param jarFile the extension JAR file to load classes from
 * @param dependencyClassLoaders ordered list of dependency ClassLoaders
 * @param extensionName human-readable extension name used as logger prefix
 */
open class DependencyAwareClassLoader(
    private val jarFile: File,
    private val dependencyClassLoaders: List<ClassLoader>,
    private val extensionName: String? = null,
) : ExtensionClassloader(parent = null) {
    override var logger: KokoroidLogger =
        getLogger(extensionName ?: "ExtensionClassLoader-${jarFile.name}")
        internal set

    private val jar by lazy { JarFile(jarFile) }

    override fun loadClass(
        name: String,
        resolve: Boolean,
    ): Class<*> {
        synchronized(this) {
            // 1. Check already loaded
            var c = findLoadedClass(name)
            if (c != null) return c

            // 2. Try JVM Application ClassLoader
            c =
                try {
                    ClassLoader.getSystemClassLoader().loadClass(name)
                } catch (_: ClassNotFoundException) {
                    null
                }

            // 3. Try dependency classloaders in order
            if (c == null) {
                for (depCl in dependencyClassLoaders) {
                    c =
                        try {
                            depCl.loadClass(name)
                        } catch (_: ClassNotFoundException) {
                            null
                        }
                    if (c != null) break
                }
            }

            // 4. Try self (JAR)
            if (c == null) {
                c =
                    try {
                        findClass(name)
                    } catch (_: ClassNotFoundException) {
                        null
                    }
            }

            if (resolve && c != null) resolveClass(c)
            return c ?: throw ClassNotFoundException(name)
        }
    }

    override fun findClass(className: String): Class<*> {
        try {
            val entryName = className.replace('.', '/') + ".class"
            val entry =
                jar.getEntry(entryName)
                    ?: throw ClassNotFoundException(className)
            return jar.getInputStream(entry).use { input ->
                val bytes = input.readBytes()
                defineClass(className, bytes, 0, bytes.size)
            }
        } catch (e: IOException) {
            throw ClassNotFoundException("Failed to load class $className from JAR", e)
        }
    }
}
