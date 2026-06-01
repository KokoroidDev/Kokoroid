// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader.preloader

import dev.kokoroidkt.pluginApi.plugin.Plugin
import dev.kokoroidkt.pluginApi.plugin.PluginContainer
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import java.nio.file.Path

class PluginPreloader {
    val jarPaths: MutableList<Path> = mutableListOf()
    val instants: MutableList<PluginContainer> = mutableListOf()

    fun addJar(path: Path) {
        jarPaths.add(path)
    }

    fun install(
        plugin: Plugin,
        meta: PluginMeta,
    ) {
        instants.add(PluginContainer(plugin, meta))
    }

    fun install(plugin: PluginContainer) {
        instants.add(plugin)
    }
}
