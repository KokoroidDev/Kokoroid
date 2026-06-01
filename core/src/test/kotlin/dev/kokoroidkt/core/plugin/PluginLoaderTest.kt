// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.plugin

import dev.kokoroidkt.core.exceptions.LoadPluginFailedException
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import dev.kokoroidkt.pluginApi.plugin.Plugin
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Test implementation of [Plugin] for PluginLoader tests.
 */
class TestPluginImpl : Plugin {
    override fun onLoad() {}
    override fun onEnable() {}
    override fun onDisable() {}
    override fun onUnload() {}
}

class PluginLoaderTest {

    @Test
    fun `loadPlugin returns plugin loaded from provided classloader`() {
        val tempDir = kotlin.io.path.createTempDirectory("pluginLoaderTest").toFile()
        try {
            val jarFile = File(tempDir, "test-plugin.jar")
            createPluginJar(jarFile, "dev.kokoroidkt.core.plugin.TestPluginImpl")
            val classLoader = DependencyAwareClassLoader(jarFile, emptyList())
            val pluginLoader = PluginLoader(jarFile, classLoader)

            val result = pluginLoader.loadPlugin()

            val plugin = result.first
            assertTrue(plugin is TestPluginImpl, "Plugin should be an instance of TestPluginImpl")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `Triple includes meta and classloader`() {
        val tempDir = kotlin.io.path.createTempDirectory("pluginLoaderTest").toFile()
        try {
            val jarFile = File(tempDir, "test-plugin.jar")
            createPluginJar(jarFile, "dev.kokoroidkt.core.plugin.TestPluginImpl")
            val classLoader = DependencyAwareClassLoader(jarFile, emptyList())
            val pluginLoader = PluginLoader(jarFile, classLoader)

            val (plugin, meta, cl) = pluginLoader.loadPlugin()

            assertTrue(plugin is TestPluginImpl)
            assertNotNull(meta)
            assertSame(classLoader, cl)
            assertTrue(meta.name == "TestPlugin")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loadPlugin throws LoadPluginFailedException when main class not found`() {
        val tempDir = kotlin.io.path.createTempDirectory("pluginLoaderTest").toFile()
        try {
            val jarFile = File(tempDir, "test-plugin.jar")
            createPluginJar(jarFile, "com.nonexistent.NoSuchPlugin")
            val classLoader = DependencyAwareClassLoader(jarFile, emptyList())
            val pluginLoader = PluginLoader(jarFile, classLoader)

            val exception = assertThrows<LoadPluginFailedException> {
                pluginLoader.loadPlugin()
            }
            assertTrue(exception.cause is ClassNotFoundException)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loadPlugin throws LoadPluginFailedException when jar has no plugin-meta json`() {
        val tempDir = kotlin.io.path.createTempDirectory("pluginLoaderTest").toFile()
        try {
            val jarFile = File(tempDir, "empty.jar")
            JarOutputStream(FileOutputStream(jarFile)).use { }

            val classLoader = DependencyAwareClassLoader(jarFile, emptyList())
            val pluginLoader = PluginLoader(jarFile, classLoader)

            assertThrows<LoadPluginFailedException> {
                pluginLoader.loadPlugin()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    companion object {
        private fun createPluginJar(
            jarFile: File,
            mainClass: String,
        ) {
            JarOutputStream(FileOutputStream(jarFile)).use { jos ->
                jos.putNextEntry(JarEntry("plugin-meta.json"))
                val metaJson =
                    """
                    {
                        "name": "TestPlugin",
                        "author": ["TestAuthor"],
                        "version": "1.0.0",
                        "mainClass": "$mainClass"
                    }
                    """.trimIndent()
                jos.write(metaJson.toByteArray(Charsets.UTF_8))
                jos.closeEntry()
            }
        }
    }
}
