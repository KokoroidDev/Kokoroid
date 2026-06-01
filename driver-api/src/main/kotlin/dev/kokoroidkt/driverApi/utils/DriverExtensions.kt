// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.driverApi.utils

import dev.kokoroidkt.coreApi.config.decodeDataFromPath
import dev.kokoroidkt.coreApi.config.encodeDataToPath
import dev.kokoroidkt.coreApi.config.kokoroidConfigRoot
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.driver.DriverMeta
import dev.kokoroidkt.driverApi.driver.DriverRegistry
import org.koin.java.KoinJavaComponent.getKoin
import java.nio.file.Path
import java.nio.file.Paths

fun Driver.getId(): String? = getKoin().get<DriverRegistry>()[this::class.java]?.driverId

fun Driver.metadata(): DriverMeta? = getKoin().get<DriverRegistry>()[this::class.java]?.metadata

/**
 * 将配置保存到文件。
 *
 * @param config 要保存的配置对象。
 * @param path 配置文件相对于 driver/<driver_name> 的路径。默认为 /settings.conf。
 */
inline fun <reified T : Any> Driver.saveConfigToFile(
    config: T,
    path: Path = Paths.get("settings.conf"),
) {
    encodeDataToPath(config, Path.of("driver", metadata()!!.name).resolve(path))
}

/**
 * 从文件加载配置。
 *
 * @param defaultWhenNull 当配置文件不存在时返回的默认值
 * @param path 配置文件相对于 driver/<driver_name> 的路径。默认为 /settings.conf。
 * @param createWhenNull 当配置文件不存在时是否创建默认配置文件，默认为 true
 * @return 加载的配置对象。
 */
inline fun <reified T : Any> Driver.loadConfigFromFile(
    defaultWhenNull: T,
    path: Path = Paths.get("settings.conf"),
    createWhenNull: Boolean = true,
): T {
    val fullPath = Path.of("driver", metadata()!!.name).resolve(path)
    val configFile = kokoroidConfigRoot.resolve(fullPath).toFile()
    if (configFile.exists()) {
        return decodeDataFromPath<T>(fullPath)
    } else if (createWhenNull) {
        saveConfigToFile(defaultWhenNull, path)
    }
    return defaultWhenNull
}
