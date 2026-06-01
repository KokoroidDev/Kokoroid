// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.core.extension.ExtensionType
import dev.kokoroidkt.core.loader.preloader.AdapterPreloader
import dev.kokoroidkt.core.loader.preloader.DriverPreloader
import dev.kokoroidkt.core.loader.preloader.PluginPreloader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExtensionPreloaderTest {
    @Test
    fun `preload with empty preloaders returns empty result`() {
        val preloader =
            ExtensionPreloader(
                DriverPreloader(),
                AdapterPreloader(),
                PluginPreloader(),
            )

        val result = preloader.preload()

        assertEquals(0, result.allDescriptors.size)
        assertEquals(0, result.classLoaderMap.size)
        assertEquals(0, result.resolved.orderedExtensions.size)
        assertTrue(result.resolved.classLoaderMap.isEmpty())
    }

    @Test
    fun `buildClassLoaders with single extension returns single classloader`() {
        val preloader =
            ExtensionPreloader(
                DriverPreloader(),
                AdapterPreloader(),
                PluginPreloader(),
            )
        val descriptor =
            ExtensionDescriptor(
                identifier = "test-ext@com.example.Test",
                name = "test-ext",
                mainClass = "com.example.Test",
                type = ExtensionType.DRIVER,
                jarFile = File("/nonexistent/test.jar"),
                dependencyIdentifiers = emptyList(),
            )

        val classLoaderMap = preloader.buildClassLoaders(listOf(descriptor))

        assertEquals(1, classLoaderMap.size)
        assertTrue(classLoaderMap.containsKey("test-ext@com.example.Test"))
        assertTrue(
            classLoaderMap["test-ext@com.example.Test"] is DependencyAwareClassLoader,
            "classloader should be a DependencyAwareClassLoader",
        )
    }

    @Test
    fun `buildClassLoaders with dependency chain creates all classloaders`() {
        val preloader =
            ExtensionPreloader(
                DriverPreloader(),
                AdapterPreloader(),
                PluginPreloader(),
            )
        val driver =
            ExtensionDescriptor(
                identifier = "base-driver@com.example.BaseDriver",
                name = "base-driver",
                mainClass = "com.example.BaseDriver",
                type = ExtensionType.DRIVER,
                jarFile = File("/nonexistent/driver.jar"),
                dependencyIdentifiers = emptyList(),
            )
        val adapter =
            ExtensionDescriptor(
                identifier = "my-adapter@com.example.MyAdapter",
                name = "my-adapter",
                mainClass = "com.example.MyAdapter",
                type = ExtensionType.ADAPTER,
                jarFile = File("/nonexistent/adapter.jar"),
                dependencyIdentifiers = listOf("base-driver@com.example.BaseDriver"),
            )
        val plugin =
            ExtensionDescriptor(
                identifier = "my-plugin@com.example.MyPlugin",
                name = "my-plugin",
                mainClass = "com.example.MyPlugin",
                type = ExtensionType.PLUGIN,
                jarFile = File("/nonexistent/plugin.jar"),
                dependencyIdentifiers = listOf("my-adapter@com.example.MyAdapter"),
            )

        val classLoaderMap = preloader.buildClassLoaders(listOf(driver, adapter, plugin))

        assertEquals(3, classLoaderMap.size)
        assertTrue(classLoaderMap["base-driver@com.example.BaseDriver"] is DependencyAwareClassLoader)
        assertTrue(classLoaderMap["my-adapter@com.example.MyAdapter"] is DependencyAwareClassLoader)
        assertTrue(classLoaderMap["my-plugin@com.example.MyPlugin"] is DependencyAwareClassLoader)
    }

    @Test
    fun `buildClassLoaders with diamond dependency creates all classloaders`() {
        val preloader =
            ExtensionPreloader(
                DriverPreloader(),
                AdapterPreloader(),
                PluginPreloader(),
            )
        val driver =
            ExtensionDescriptor(
                identifier = "core-driver@com.example.CoreDriver",
                name = "core-driver",
                mainClass = "com.example.CoreDriver",
                type = ExtensionType.DRIVER,
                jarFile = File("/nonexistent/driver.jar"),
                dependencyIdentifiers = emptyList(),
            )
        val adapterA =
            ExtensionDescriptor(
                identifier = "adapter-a@com.example.AdapterA",
                name = "adapter-a",
                mainClass = "com.example.AdapterA",
                type = ExtensionType.ADAPTER,
                jarFile = File("/nonexistent/adapterA.jar"),
                dependencyIdentifiers = listOf("core-driver@com.example.CoreDriver"),
            )
        val adapterB =
            ExtensionDescriptor(
                identifier = "adapter-b@com.example.AdapterB",
                name = "adapter-b",
                mainClass = "com.example.AdapterB",
                type = ExtensionType.ADAPTER,
                jarFile = File("/nonexistent/adapterB.jar"),
                dependencyIdentifiers = listOf("core-driver@com.example.CoreDriver"),
            )
        val plugin =
            ExtensionDescriptor(
                identifier = "top-plugin@com.example.TopPlugin",
                name = "top-plugin",
                mainClass = "com.example.TopPlugin",
                type = ExtensionType.PLUGIN,
                jarFile = File("/nonexistent/plugin.jar"),
                dependencyIdentifiers =
                    listOf(
                        "adapter-a@com.example.AdapterA",
                        "adapter-b@com.example.AdapterB",
                    ),
            )

        val classLoaderMap = preloader.buildClassLoaders(listOf(driver, adapterA, adapterB, plugin))

        assertEquals(4, classLoaderMap.size)
        assertTrue(classLoaderMap["core-driver@com.example.CoreDriver"] is DependencyAwareClassLoader)
        assertTrue(classLoaderMap["adapter-a@com.example.AdapterA"] is DependencyAwareClassLoader)
        assertTrue(classLoaderMap["adapter-b@com.example.AdapterB"] is DependencyAwareClassLoader)
        assertTrue(classLoaderMap["top-plugin@com.example.TopPlugin"] is DependencyAwareClassLoader)
    }

    @Test
    fun `buildClassLoaders with missing dependency throws exception`() {
        val preloader =
            ExtensionPreloader(
                DriverPreloader(),
                AdapterPreloader(),
                PluginPreloader(),
            )
        val plugin =
            ExtensionDescriptor(
                identifier = "orphan@com.example.Orphan",
                name = "orphan",
                mainClass = "com.example.Orphan",
                type = ExtensionType.PLUGIN,
                jarFile = File("/nonexistent/plugin.jar"),
                dependencyIdentifiers = listOf("missing-dep@com.example.Missing"),
            )

        assertFailsWith<IllegalStateException> {
            preloader.buildClassLoaders(listOf(plugin))
        }
    }
}
