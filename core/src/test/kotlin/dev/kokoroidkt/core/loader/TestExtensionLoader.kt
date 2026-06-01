// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.core.config.Config
import dev.kokoroidkt.core.database.DatabaseManagerImpl
import dev.kokoroidkt.core.di.*
import dev.kokoroidkt.core.runtime.GlobalEventLoop
import dev.kokoroidkt.core.runtime.KokoroidLauncher
import dev.kokoroidkt.core.runtime.crash.CrashRegistry
import dev.kokoroidkt.core.runtime.crash.CrashRegistryImpl
import dev.kokoroidkt.coreApi.database.DatabaseManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.test.Test

fun copyDir(
    source: Path,
    target: Path,
) {
    if (!source.exists()) return

    Files.walk(source).use { stream ->
        stream.forEach { src ->
            val dest = target.resolve(source.relativize(src).toString())
            if (src.isDirectory()) {
                dest.createDirectories()
            } else {
                src.copyTo(dest, overwrite = true)
            }
        }
    }
}

class TestExtensionLoader {
    val kokoroidLauncher = KokoroidLauncher()

    @Test
    fun `test extension loader`() {
        kokoroidLauncher.initAllExtensions()
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun `set test configuration`() {
            val adaptersPath = Paths.get("./kokoroid/adapters")
            val pluginsPath = Paths.get("./kokoroid/plugins")
            val driversPath = Paths.get("./kokoroid/drivers")
            val kokoroidPath = Paths.get("./kokoroid")
            kokoroidPath.createDirectories()
            val testExtensionPath = Paths.get("../test-extension/build/libs")

            copyDir(testExtensionPath, adaptersPath)
            copyDir(testExtensionPath, pluginsPath)
            copyDir(testExtensionPath, driversPath)
            try {
                val config = Config()
                startKoin {
                    modules(
                        listOf(
                            pluginModules,
                            adapterModules,
                            driverModules,
                            loggerModules,
                            runtimeModules,
                            utils,
                            module {
                                single<Config> { config }
                                single<GlobalEventLoop> { GlobalEventLoop() }
                                single<KokoroidLauncher> { KokoroidLauncher() }
                                single<CrashRegistry> { CrashRegistryImpl() }
                                single<DatabaseManager> { DatabaseManagerImpl }
                            },
                        ),
                    )
                }
            } catch (_: KoinApplicationAlreadyStartedException) {
            }
        }

        @JvmStatic
        @AfterAll
        fun `tear down koin`() {
            stopKoin()
        }
    }
}
