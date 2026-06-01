// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader.preloader

import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.driver.DriverContainer
import dev.kokoroidkt.driverApi.driver.DriverMeta
import java.nio.file.Path

class DriverPreloader {
    val jarPaths: MutableList<Path> = mutableListOf()
    val instants: MutableList<DriverContainer> = mutableListOf()

    fun addJar(path: Path) {
        jarPaths.add(path)
    }

    fun install(
        driver: Driver,
        meta: DriverMeta,
    ) {
        instants.add(DriverContainer(meta, driver))
    }

    fun install(driver: DriverContainer) {
        instants.add(driver)
    }
}
