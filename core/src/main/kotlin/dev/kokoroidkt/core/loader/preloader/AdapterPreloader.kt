// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader.preloader

import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.adapterApi.adapter.AdapterContainer
import dev.kokoroidkt.adapterApi.adapter.AdapterMeta
import java.nio.file.Path

class AdapterPreloader {
    val jarPaths: MutableList<Path> = mutableListOf()
    val instants: MutableList<AdapterContainer> = mutableListOf()

    fun addJar(path: Path) {
        jarPaths.add(path)
    }

    fun install(
        adapter: Adapter,
        meta: AdapterMeta,
    ) {
        instants.add(AdapterContainer(adapter, meta))
    }

    fun install(adapter: AdapterContainer) {
        instants.add(adapter)
    }
}
