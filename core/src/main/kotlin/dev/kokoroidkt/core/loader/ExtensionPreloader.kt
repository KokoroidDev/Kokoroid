// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.adapterApi.adapter.AdapterMeta
import dev.kokoroidkt.core.loader.preloader.AdapterPreloader
import dev.kokoroidkt.core.loader.preloader.DriverPreloader
import dev.kokoroidkt.core.loader.preloader.PluginPreloader
import dev.kokoroidkt.driverApi.driver.DriverMeta
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * Orchestrator that discovers extension metadata from JARs,
 * builds the dependency graph, and constructs classloader chains.
 *
 * @param driverPreloader preloader that collected driver JAR paths
 * @param adapterPreloader preloader that collected adapter JAR paths
 * @param pluginPreloader preloader that collected plugin JAR paths
 */
class ExtensionPreloader(
    private val driverPreloader: DriverPreloader,
    private val adapterPreloader: AdapterPreloader,
    private val pluginPreloader: PluginPreloader,
) {
    /**
     * Result of the preload process.
     *
     * @property resolved the resolved dependency graph with topological ordering
     * @property allDescriptors all discovered extension descriptors
     * @property classLoaderMap map from extension identifier to its [ClassLoader]
     */
    data class PreloadResult(
        val resolved: DependencyGraph.ResolvedDependencyGraph,
        val allDescriptors: List<ExtensionDescriptor>,
        val classLoaderMap: Map<String, ClassLoader>,
    )

    /**
     * Executes the full preload pipeline:
     * 1. Scans all JARs from the three preloaders and reads metadata
     * 2. Builds and resolves the dependency graph
     * 3. Creates [DependencyAwareClassLoader] instances in topological order
     */
    fun preload(): PreloadResult {
        val descriptors = mutableListOf<ExtensionDescriptor>()

        // Phase 1: Scan all JARs from preloaders, read metadata
        scanDriverJars(descriptors)
        scanAdapterJars(descriptors)
        scanPluginJars(descriptors)

        // Phase 2: Build and resolve dependency graph
        val graph = DependencyGraph()
        for (descriptor in descriptors) {
            graph.addNode(descriptor)
        }
        val resolved = graph.resolve()

        // Print dependency graph for debugging
        println(graph.dumpGraph())
        println(resolved.topologicalOrderString())

        // Phase 3: Create DependencyAwareClassLoaders in topological order.
        // Dependencies' classloaders are already in the map because we
        // process in topological order.
        val classLoaderMap = buildClassLoaders(resolved.orderedExtensions)

        return PreloadResult(
            resolved = resolved.copy(classLoaderMap = classLoaderMap),
            allDescriptors = descriptors.toList(),
            classLoaderMap = classLoaderMap,
        )
    }

    /**
     * Creates [DependencyAwareClassLoader] instances in topological order.
     *
     * For each extension, its dependency classloaders are looked up from
     * previously created entries in the accumulating map. This relies on
     * [orderedExtensions] being in topological order so that dependencies
     * are always created before dependents.
     *
     * Visible for testing — testers can pass synthetic descriptors without
     * requiring real JAR files on disk.
     */
    internal fun buildClassLoaders(orderedExtensions: List<ExtensionDescriptor>): Map<String, ClassLoader> {
        val classLoaderMap = mutableMapOf<String, ClassLoader>()
        for (descriptor in orderedExtensions) {
            val depClassLoaders = descriptor.dependencyIdentifiers.map { depId ->
                classLoaderMap[depId] ?: error(
                    "Internal error: dependency '$depId' for '${descriptor.identifier}' not yet loaded",
                )
            }
            classLoaderMap[descriptor.identifier] = DependencyAwareClassLoader(
                jarFile = descriptor.jarFile,
                dependencyClassLoaders = depClassLoaders,
                extensionName = descriptor.name,
            )
        }
        return classLoaderMap
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun scanDriverJars(descriptors: MutableList<ExtensionDescriptor>) {
        for (path in driverPreloader.jarPaths) {
            scanJarSafely(path) { file, jar ->
                val entry = jar.getJarEntry(DRIVER_META_FILE)
                if (entry != null) {
                    val meta = jar.getInputStream(entry).use { stream ->
                        json.decodeFromStream<DriverMeta>(stream)
                    }
                    descriptors.add(ExtensionDescriptor.fromDriverMeta(meta, file))
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun scanAdapterJars(descriptors: MutableList<ExtensionDescriptor>) {
        for (path in adapterPreloader.jarPaths) {
            scanJarSafely(path) { file, jar ->
                val entry = jar.getJarEntry(ADAPTER_META_FILE)
                if (entry != null) {
                    val meta = jar.getInputStream(entry).use { stream ->
                        json.decodeFromStream<AdapterMeta>(stream)
                    }
                    descriptors.add(ExtensionDescriptor.fromAdapterMeta(meta, file))
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun scanPluginJars(descriptors: MutableList<ExtensionDescriptor>) {
        for (path in pluginPreloader.jarPaths) {
            scanJarSafely(path) { file, jar ->
                val entry = jar.getJarEntry(PLUGIN_META_FILE)
                if (entry != null) {
                    val meta = jar.getInputStream(entry).use { stream ->
                        json.decodeFromStream<PluginMeta>(stream)
                    }
                    descriptors.add(ExtensionDescriptor.fromPluginMeta(meta, file))
                }
            }
        }
    }

    /**
     * Safely opens a JAR file and executes [action] with the file and JarFile.
     * Skips the JAR if it doesn't exist, isn't a regular file, or is unreadable.
     */
    private fun scanJarSafely(
        path: Path,
        action: (File, JarFile) -> Unit,
    ) {
        val file = path.toFile()
        if (!file.exists() || !file.isFile) return
        try {
            JarFile(file).use { jar ->
                action(file, jar)
            }
        } catch (_: Exception) {
            // Skip invalid or unreadable JARs
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        private const val DRIVER_META_FILE = "driver-meta.json"
        private const val ADAPTER_META_FILE = "adapter-meta.json"
        private const val PLUGIN_META_FILE = "plugin-meta.json"
    }
}
