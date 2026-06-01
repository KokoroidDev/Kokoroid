// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.adapterApi.adapter.AdapterMeta
import dev.kokoroidkt.core.extension.ExtensionType
import dev.kokoroidkt.driverApi.driver.DriverMeta
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionDescriptorTest {
    @Test
    fun `fromDriverMeta creates descriptor with correct identifier`() {
        val meta =
            DriverMeta(
                name = "TestDriver",
                version = "1.0.0",
                mainClass = "com.example.Driver",
                authors = null,
                description = null,
                website = null,
            )
        val jarFile = File("/tmp/test-driver.jar")
        val descriptor = ExtensionDescriptor.fromDriverMeta(meta, jarFile)

        assertEquals("TestDriver@com.example.Driver", descriptor.identifier)
        assertEquals("TestDriver", descriptor.name)
        assertEquals("com.example.Driver", descriptor.mainClass)
        assertEquals(ExtensionType.DRIVER, descriptor.type)
        assertEquals(jarFile, descriptor.jarFile)
    }

    @Test
    fun `fromDriverMeta has empty dependencyIdentifiers`() {
        val meta =
            DriverMeta(
                name = "TestDriver",
                version = "1.0.0",
                mainClass = "com.example.Driver",
                authors = null,
                description = null,
                website = null,
            )
        val descriptor = ExtensionDescriptor.fromDriverMeta(meta, File("/tmp/test.jar"))

        assertEquals(emptyList<String>(), descriptor.dependencyIdentifiers)
    }

    @Test
    fun `fromAdapterMeta creates descriptor with correct identifier and type`() {
        val meta =
            AdapterMeta(
                name = "TestAdapter",
                version = "2.0.0",
                mainClass = "com.example.Adapter",
                authors = null,
                description = null,
                website = null,
            )
        val jarFile = File("/tmp/test-adapter.jar")
        val descriptor = ExtensionDescriptor.fromAdapterMeta(meta, jarFile)

        assertEquals("TestAdapter@com.example.Adapter", descriptor.identifier)
        assertEquals("TestAdapter", descriptor.name)
        assertEquals("com.example.Adapter", descriptor.mainClass)
        assertEquals(ExtensionType.ADAPTER, descriptor.type)
        assertEquals(jarFile, descriptor.jarFile)
    }

    @Test
    fun `fromAdapterMeta has empty dependencyIdentifiers`() {
        val meta =
            AdapterMeta(
                name = "TestAdapter",
                version = "2.0.0",
                mainClass = "com.example.Adapter",
                authors = null,
                description = null,
                website = null,
            )
        val descriptor = ExtensionDescriptor.fromAdapterMeta(meta, File("/tmp/test.jar"))

        assertEquals(emptyList<String>(), descriptor.dependencyIdentifiers)
    }

    @Test
    fun `fromPluginMeta creates descriptor with correct identifier and type`() {
        val meta =
            PluginMeta(
                name = "TestPlugin",
                author = null,
                description = null,
                version = "3.0.0",
                mainClass = "com.example.Plugin",
                website = null,
                dependedPlugins = null,
                loadBefore = null,
                loadAfter = null,
            )
        val jarFile = File("/tmp/test-plugin.jar")
        val descriptor = ExtensionDescriptor.fromPluginMeta(meta, jarFile)

        assertEquals("TestPlugin@com.example.Plugin", descriptor.identifier)
        assertEquals("TestPlugin", descriptor.name)
        assertEquals("com.example.Plugin", descriptor.mainClass)
        assertEquals(ExtensionType.PLUGIN, descriptor.type)
        assertEquals(jarFile, descriptor.jarFile)
    }

    @Test
    fun `fromPluginMeta with null dependedPlugins has empty dependencyIdentifiers`() {
        val meta =
            PluginMeta(
                name = "TestPlugin",
                author = null,
                description = null,
                version = "3.0.0",
                mainClass = "com.example.Plugin",
                website = null,
                dependedPlugins = null,
                loadBefore = null,
                loadAfter = null,
            )
        val descriptor = ExtensionDescriptor.fromPluginMeta(meta, File("/tmp/test.jar"))

        assertEquals(emptyList<String>(), descriptor.dependencyIdentifiers)
    }

    @Test
    fun `fromPluginMeta with dependedPlugins includes them in dependencyIdentifiers`() {
        val meta =
            PluginMeta(
                name = "TestPlugin",
                author = null,
                description = null,
                version = "3.0.0",
                mainClass = "com.example.Plugin",
                website = null,
                dependedPlugins = arrayOf("com.example.DepA", "com.example.DepB"),
                loadBefore = null,
                loadAfter = null,
            )
        val descriptor = ExtensionDescriptor.fromPluginMeta(meta, File("/tmp/test.jar"))

        assertEquals(listOf("com.example.DepA", "com.example.DepB"), descriptor.dependencyIdentifiers)
    }

    @Test
    fun `jarFile is preserved in descriptor`() {
        val jarFile = File("/some/path/extension.jar")
        val meta =
            DriverMeta(
                name = "Test",
                version = "1.0.0",
                mainClass = "com.example.Test",
                authors = null,
                description = null,
                website = null,
            )
        val descriptor = ExtensionDescriptor.fromDriverMeta(meta, jarFile)

        assertEquals(jarFile, descriptor.jarFile)
    }
}
