// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.driver

import dev.kokoroidkt.core.exceptions.LoadDriverFailedException
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.driver.DriverMeta
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.jar.JarFile

class DriverLoader(
    private val jarFile: File,
    private val classLoader: DependencyAwareClassLoader,
) {
    private val jar = JarFile(jarFile)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadDriver(): Triple<Driver, DriverMeta, DependencyAwareClassLoader> {
        val metadataEntry = jar.getEntry("driver-meta.json")
        try {
            val metadata: DriverMeta =
                jar.getInputStream(metadataEntry).use {
                    json.decodeFromStream<DriverMeta>(it)
                }
            val clazz = classLoader.loadClass(metadata.mainClass)
            val driver = clazz.getConstructor().newInstance() as Driver
            return Triple(driver, metadata, classLoader)
        } catch (e: Exception) {
            throw LoadDriverFailedException(
                msg = "Error while loading driver: ${e.message}",
                cause = e,
                jarFile = jarFile,
            )
        }
    }
}
