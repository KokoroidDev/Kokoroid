// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.core.adapter.AdapterLoader
import dev.kokoroidkt.core.driver.DriverLoader
import dev.kokoroidkt.core.extension.ExtensionType
import dev.kokoroidkt.core.loader.preloader.AdapterPreloader
import dev.kokoroidkt.core.loader.preloader.DriverPreloader
import dev.kokoroidkt.core.loader.preloader.PluginPreloader
import dev.kokoroidkt.core.plugin.PluginLoader
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.pluginApi.plugin.Plugin
import org.junit.jupiter.api.BeforeAll
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end integration test for the extension loading pipeline.
 *
 * Exercises the COMPLETE pipeline from metadata discovery through dependency
 * graph resolution to classloader construction and extension instance creation,
 * using the actual [test-extension-with-deps] JAR that contains three extensions:
 * - DriverA (no dependencies)
 * - AdapterB (declared driverDependencies: DriverA)
 * - PluginC (declared adapterDependencies: AdapterB, driverDependencies: DriverA)
 *
 * NOTE on current dependency wiring: The [ExtensionDescriptor] factory methods
 * for [dev.kokoroidkt.driverApi.driver.DriverMeta] and
 * [dev.kokoroidkt.adapterApi.adapter.AdapterMeta] currently set
 * `dependencyIdentifiers` to [emptyList] because those metadata classes do not
 * yet have dependency fields. Similarly, [dev.kokoroidkt.pluginApi.plugin.PluginMeta]
 * only tracks `dependedPlugins` (plugin-to-plugin deps), not adapter/driver deps.
 * As a result, all three extensions resolve as independent nodes in the graph.
 * Once dependency fields are added to the metadata classes and wired in the
 * factory methods, this test should be updated to verify delegation chains.
 */
class DependencyExtensionIntegrationTest {

    // -----------------------------------------------------------------------
    // Test scenarios
    // -----------------------------------------------------------------------

    @Test
    fun `full pipeline discovers all 3 extensions from test JAR`() {
        val preloader = createPreloader()
        val result = preloader.preload()

        assertEquals(3, result.allDescriptors.size, "Preloader should discover 3 extensions")
        assertEquals(3, result.resolved.orderedExtensions.size)
        assertEquals(3, result.classLoaderMap.size)

        // ── Verify DriverA descriptor ──────────────────────────────────────
        val driverA = descriptorById(result, "DriverA@dev.kokoroidkt.testdeps.driver.DriverA")
        assertNotNull(driverA, "DriverA descriptor must be present")
        assertEquals("DriverA@dev.kokoroidkt.testdeps.driver.DriverA", driverA.identifier)
        assertEquals("DriverA", driverA.name)
        assertEquals("dev.kokoroidkt.testdeps.driver.DriverA", driverA.mainClass)
        assertEquals(ExtensionType.DRIVER, driverA.type)

        // ── Verify AdapterB descriptor ─────────────────────────────────────
        val adapterB = descriptorById(result, "AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB")
        assertNotNull(adapterB, "AdapterB descriptor must be present")
        assertEquals("AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB", adapterB.identifier)
        assertEquals("AdapterB", adapterB.name)
        assertEquals("dev.kokoroidkt.testdeps.adapter.AdapterB", adapterB.mainClass)
        assertEquals(ExtensionType.ADAPTER, adapterB.type)

        // ── Verify PluginC descriptor ──────────────────────────────────────
        val pluginC = descriptorById(result, "PluginC@dev.kokoroidkt.testdeps.plugin.PluginC")
        assertNotNull(pluginC, "PluginC descriptor must be present")
        assertEquals("PluginC@dev.kokoroidkt.testdeps.plugin.PluginC", pluginC.identifier)
        assertEquals("PluginC", pluginC.name)
        assertEquals("dev.kokoroidkt.testdeps.plugin.PluginC", pluginC.mainClass)
        assertEquals(ExtensionType.PLUGIN, pluginC.type)
    }

    @Test
    fun `classLoaderMap contains DependencyAwareClassLoader for each extension`() {
        val result = createPreloader().preload()

        listOf(
            "DriverA@dev.kokoroidkt.testdeps.driver.DriverA",
            "AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB",
            "PluginC@dev.kokoroidkt.testdeps.plugin.PluginC",
        ).forEach { id ->
            val cl = result.classLoaderMap[id]
            assertNotNull(cl, "ClassLoader for '$id' must be present")
            assertTrue(cl is DependencyAwareClassLoader, "ClassLoader for '$id' must be a DependencyAwareClassLoader")
        }
    }

    @Test
    fun `load DriverA via DriverLoader returns valid driver instance`() {
        val jarFile = jarFile()
        val result = createPreloader().preload()
        val cl = result.classLoaderMap["DriverA@dev.kokoroidkt.testdeps.driver.DriverA"] as DependencyAwareClassLoader

        val (driver, meta, returnedCl) = DriverLoader(jarFile, cl).loadDriver()

        assertNotNull(driver, "DriverA instance must not be null")
        assertEquals("DriverA", meta.name)
        assertEquals(cl, returnedCl, "Returned classLoader must be the same instance")
    }

    @Test
    fun `load AdapterB via AdapterLoader returns valid adapter instance`() {
        val jarFile = jarFile()
        val result = createPreloader().preload()
        val cl = result.classLoaderMap["AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB"] as DependencyAwareClassLoader

        val (adapter, meta, returnedCl) = AdapterLoader(jarFile, cl).loadAdapter()

        assertNotNull(adapter, "AdapterB instance must not be null")
        assertEquals("AdapterB", meta.name)
        assertEquals(cl, returnedCl, "Returned classLoader must be the same instance")
    }

    @Test
    fun `load PluginC via PluginLoader returns valid plugin instance`() {
        val jarFile = jarFile()
        val result = createPreloader().preload()
        val cl = result.classLoaderMap["PluginC@dev.kokoroidkt.testdeps.plugin.PluginC"] as DependencyAwareClassLoader

        val (plugin, meta, returnedCl) = PluginLoader(jarFile, cl).loadPlugin()

        assertNotNull(plugin, "PluginC instance must not be null")
        assertEquals("PluginC", meta.name)
        assertEquals(cl, returnedCl, "Returned classLoader must be the same instance")
    }

    @Test
    fun `topological order has all 3 extensions with correct sequence`() {
        val result = createPreloader().preload()
        val ordered = result.resolved.orderedExtensions

        assertEquals(3, ordered.size)

        // With current code, all extensions have empty dependencyIdentifiers,
        // so topological sort follows insertion order: driver → adapter → plugin.
        val ids = ordered.map { it.identifier }
        assertEquals("DriverA@dev.kokoroidkt.testdeps.driver.DriverA", ids[0])
        assertEquals("AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB", ids[1])
        assertEquals("PluginC@dev.kokoroidkt.testdeps.plugin.PluginC", ids[2])
    }

    @Test
    fun `all descriptors reference the same JAR file`() {
        val result = createPreloader().preload()
        // Normalize path so that "core\..\" is resolved for comparison
        val expectedPath = Paths.get(jarFile().absolutePath).normalize().toString()

        result.allDescriptors.forEach { descriptor ->
            assertEquals(
                expectedPath, descriptor.jarFile.absolutePath,
                "Descriptor '${descriptor.identifier}' should reference the shared test JAR",
            )
        }
    }

    @Test
    fun `each classloader can load its own extension class from the JAR`() {
        val result = createPreloader().preload()

        // DriverA classloader loads DriverA class
        val driverCl = result.classLoaderMap["DriverA@dev.kokoroidkt.testdeps.driver.DriverA"]
        val driverClass = driverCl!!.loadClass("dev.kokoroidkt.testdeps.driver.DriverA")
        assertNotNull(driverClass)
        assertEquals("dev.kokoroidkt.testdeps.driver.DriverA", driverClass.name)
        val driverInstance = driverClass.getConstructor().newInstance()
        assertTrue(driverInstance is Driver)

        // AdapterB classloader loads AdapterB class
        val adapterCl = result.classLoaderMap["AdapterB@dev.kokoroidkt.testdeps.adapter.AdapterB"]
        val adapterClass = adapterCl!!.loadClass("dev.kokoroidkt.testdeps.adapter.AdapterB")
        assertNotNull(adapterClass)
        assertEquals("dev.kokoroidkt.testdeps.adapter.AdapterB", adapterClass.name)
        val adapterInstance = adapterClass.getConstructor().newInstance()
        assertTrue(adapterInstance is Adapter)

        // PluginC classloader loads PluginC class
        val pluginCl = result.classLoaderMap["PluginC@dev.kokoroidkt.testdeps.plugin.PluginC"]
        val pluginClass = pluginCl!!.loadClass("dev.kokoroidkt.testdeps.plugin.PluginC")
        assertNotNull(pluginClass)
        assertEquals("dev.kokoroidkt.testdeps.plugin.PluginC", pluginClass.name)
        val pluginInstance = pluginClass.getConstructor().newInstance()
        assertTrue(pluginInstance is Plugin)
    }

    @Test
    fun `all 3 extension types contribute to resolved graph`() {
        val result = createPreloader().preload()

        val ordered = result.resolved.orderedExtensions
        val types = ordered.map { it.type }.toSet()

        assertTrue(types.contains(ExtensionType.DRIVER), "Graph must contain a DRIVER extension")
        assertTrue(types.contains(ExtensionType.ADAPTER), "Graph must contain an ADAPTER extension")
        assertTrue(types.contains(ExtensionType.PLUGIN), "Graph must contain a PLUGIN extension")
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a pre-configured [ExtensionPreloader] with all three preloaders
     * pointing at the shared test JAR.
     */
    private fun createPreloader(): ExtensionPreloader {
        val jarPath = Paths.get(jarFile().absolutePath).normalize()
        return ExtensionPreloader(
            DriverPreloader().apply { addJar(jarPath) },
            AdapterPreloader().apply { addJar(jarPath) },
            PluginPreloader().apply { addJar(jarPath) },
        )
    }

    private fun descriptorById(
        result: ExtensionPreloader.PreloadResult,
        identifier: String,
    ): ExtensionDescriptor? = result.allDescriptors.find { it.identifier == identifier }

    // -----------------------------------------------------------------------
    // Companion — one-time JAR build
    // -----------------------------------------------------------------------

    companion object {
        private lateinit var builtJarFile: File

        @JvmStatic
        @BeforeAll
        fun buildTestJars() {
            // Determine the correct Gradle wrapper for the platform.
            // The test runs with working directory = <project>/core/,
            // so we reference gradlew.bat or gradlew from the project root.
            val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
            val gradlew = if (isWindows) {
                File("../gradlew.bat").absolutePath
            } else {
                File("../gradlew").absolutePath
            }

            val process = ProcessBuilder(
                gradlew, ":test-extension-with-deps:jar", "-q",
            )
                .inheritIO()
                .start()

            val exitCode = process.waitFor()
            require(exitCode == 0) {
                "test-extension-with-deps JAR build failed (exit code $exitCode)"
            }

            val jarPath = Paths.get("../test-extension-with-deps/build/libs/test-extension-with-deps.jar")
                .normalize()
            builtJarFile = jarPath.toFile()

            require(builtJarFile.exists()) {
                "Test JAR not found at expected path: ${builtJarFile.absolutePath}"
            }
        }

        private fun jarFile(): File = builtJarFile
    }
}
