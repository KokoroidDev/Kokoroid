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

/**
 * Unified metadata descriptor for all extension types (drivers, adapters, plugins).
 *
 * Provides a normalized representation that the extension loader can use
 * regardless of the original metadata class.
 *
 * @property identifier Composite key in "name@mainClass" format.
 * @property name Human-readable extension name.
 * @property mainClass Fully qualified main class name.
 * @property type Type of extension (DRIVER, ADAPTER, or PLUGIN).
 * @property jarFile The JAR file from which this extension was loaded.
 * @property dependencyIdentifiers Flat list of dependency identifiers (mainClass names).
 */
data class ExtensionDescriptor(
    val identifier: String,
    val name: String,
    val mainClass: String,
    val type: ExtensionType,
    val jarFile: File,
    val dependencyIdentifiers: List<String>,
) {
    companion object {
        /**
         * Creates an [ExtensionDescriptor] from a [DriverMeta].
         */
        fun fromDriverMeta(
            meta: DriverMeta,
            jarFile: File,
        ): ExtensionDescriptor =
            ExtensionDescriptor(
                identifier = "${meta.name}@${meta.mainClass}",
                name = meta.name,
                mainClass = meta.mainClass,
                type = ExtensionType.DRIVER,
                jarFile = jarFile,
                dependencyIdentifiers = meta.dependencies,
            )

        /**
         * Creates an [ExtensionDescriptor] from an [AdapterMeta].
         */
        fun fromAdapterMeta(
            meta: AdapterMeta,
            jarFile: File,
        ): ExtensionDescriptor =
            ExtensionDescriptor(
                identifier = "${meta.name}@${meta.mainClass}",
                name = meta.name,
                mainClass = meta.mainClass,
                type = ExtensionType.ADAPTER,
                jarFile = jarFile,
                dependencyIdentifiers = meta.driverDependencies + meta.adapterDependencies,
            )

        /**
         * Creates an [ExtensionDescriptor] from a [PluginMeta].
         */
        fun fromPluginMeta(
            meta: PluginMeta,
            jarFile: File,
        ): ExtensionDescriptor =
            ExtensionDescriptor(
                identifier = "${meta.name}@${meta.mainClass}",
                name = meta.name,
                mainClass = meta.mainClass,
                type = ExtensionType.PLUGIN,
                jarFile = jarFile,
                dependencyIdentifiers =
                    (meta.dependedPlugins?.toList() ?: emptyList()) +
                        meta.adapterDependencies + meta.driverDependencies,
            )
    }
}
