// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.adapter

import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.adapterApi.adapter.AdapterMeta
import dev.kokoroidkt.core.exceptions.LoadAdapterFailedException
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.jar.JarFile

class AdapterLoader(
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
    fun loadAdapter(): Triple<Adapter, AdapterMeta, DependencyAwareClassLoader> {
        val metadataEntry = jar.getEntry("adapter-meta.json")
        try {
            val metadata: AdapterMeta =
                jar.getInputStream(metadataEntry).use {
                    json.decodeFromStream<AdapterMeta>(it)
                }
            val clazz = classLoader.loadClass(metadata.mainClass)
            val adapter = clazz.getConstructor().newInstance() as Adapter
            return Triple(adapter, metadata, classLoader)
        } catch (e: Exception) {
            throw LoadAdapterFailedException(
                msg = "Error while loading driver: ${e.message}",
                cause = e,
                jarFile = jarFile,
            )
        }
    }
}
