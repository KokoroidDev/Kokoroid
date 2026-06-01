// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.di

import dev.kokoroidkt.adapterApi.adapter.AdapterRegistry
import dev.kokoroidkt.core.adapter.AdapterManager
import dev.kokoroidkt.core.adapter.AdapterRegistryImpl
import dev.kokoroidkt.core.config.Config
import dev.kokoroidkt.core.database.DatabaseManagerImpl
import dev.kokoroidkt.core.driver.DriverManager
import dev.kokoroidkt.core.driver.DriverRegistryImpl
import dev.kokoroidkt.core.factory.ConversationOrchestratorFactoryImpl
import dev.kokoroidkt.core.factory.SessionContainerFactoryImpl
import dev.kokoroidkt.core.factory.SessionFactoryImpl
import dev.kokoroidkt.core.logger.DefaultKokoroidLogger
import dev.kokoroidkt.core.plugin.PluginManager
import dev.kokoroidkt.core.plugin.PluginRegistryImpl
import dev.kokoroidkt.core.runtime.GlobalEventLoop
import dev.kokoroidkt.core.runtime.KokoroidLauncher
import dev.kokoroidkt.core.runtime.crash.CrashRegistry
import dev.kokoroidkt.core.runtime.crash.CrashRegistryImpl
import dev.kokoroidkt.core.runtime.state.RuntimeState
import dev.kokoroidkt.core.utils.binds
import dev.kokoroidkt.coreApi.database.DatabaseManager
import dev.kokoroidkt.coreApi.logging.LoggerFactory
import dev.kokoroidkt.driverApi.driver.DriverRegistry
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.pluginApi.factory.ConversationOrchestratorFactory
import dev.kokoroidkt.pluginApi.plugin.PluginRegistry
import dev.kokoroidkt.pluginApi.session.container.SessionContainer
import dev.kokoroidkt.pluginApi.session.container.SessionFactoty
import org.koin.dsl.module
import kotlin.reflect.KClass

val pluginModules =
    module {
        single { PluginRegistryImpl() }
            .binds(PluginRegistry::class, PluginManager::class)
    }

val adapterModules =
    module {
        single { AdapterRegistryImpl() }
            .binds(AdapterManager::class, AdapterRegistry::class)
    }

val driverModules =
    module {
        single { DriverRegistryImpl() }
            .binds(DriverManager::class, DriverRegistry::class)
    }

val loggerModules =
    module {
        factory<LoggerFactory> {
            { prefix: String, clazz: KClass<*> ->
                DefaultKokoroidLogger(
                    name = clazz.simpleName ?: "Unknown",
                    prefix = prefix,
                )
            }
        }
    }

val utils =
    module {
        // single<ConfigHelper> { ConfigHelperImpl() }
    }

val basicModules =
    module {
        single<Config> { Config() }
        single { GlobalEventLoop() }.binds(GlobalLoopEmitter::class, GlobalEventLoop::class)
        single<KokoroidLauncher> { KokoroidLauncher() }
        single<CrashRegistry> { CrashRegistryImpl() }
        single<DatabaseManager> { DatabaseManagerImpl }
    }

val runtimeModules =
    module {
        single<RuntimeState> { RuntimeState() }
        factory<SessionContainer> {
            SessionContainerFactoryImpl().createSessionContainer()
        }
        single<SessionFactoty> { SessionFactoryImpl() }
        single<ConversationOrchestratorFactory> { ConversationOrchestratorFactoryImpl() }
    }

val allModules =
    listOf(
        basicModules,
        pluginModules,
        adapterModules,
        driverModules,
        loggerModules,
        runtimeModules,
        utils,
    )
