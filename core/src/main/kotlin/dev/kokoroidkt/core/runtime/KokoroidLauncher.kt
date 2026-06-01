// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.runtime

import ch.qos.logback.classic.Level
import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.core.adapter.AdapterLoader
import dev.kokoroidkt.core.adapter.AdapterManager
import dev.kokoroidkt.core.config.Config
import dev.kokoroidkt.core.constants.ExitStatus
import dev.kokoroidkt.core.constants.ExitStatus.DATABASE_TOO_OLD
import dev.kokoroidkt.core.di.allModules
import dev.kokoroidkt.core.driver.DriverLoader
import dev.kokoroidkt.core.driver.DriverManager
import dev.kokoroidkt.core.extension.ExtensionType
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import dev.kokoroidkt.core.loader.ExtensionPreloader
import dev.kokoroidkt.core.loader.preloader.AdapterPreloader
import dev.kokoroidkt.core.loader.preloader.DriverPreloader
import dev.kokoroidkt.core.loader.preloader.PluginPreloader
import dev.kokoroidkt.core.logger.getLogger
import dev.kokoroidkt.core.plugin.PluginLoader
import dev.kokoroidkt.core.plugin.PluginManager
import dev.kokoroidkt.core.runtime.crash.CrashRegistry
import dev.kokoroidkt.core.runtime.state.InternalState
import dev.kokoroidkt.core.runtime.state.RuntimeState
import dev.kokoroidkt.core.utils.KokoroidVersion
import dev.kokoroidkt.coreApi.database.DatabaseManager
import dev.kokoroidkt.coreApi.database.DatabaseType
import dev.kokoroidkt.coreApi.database.allTables
import dev.kokoroidkt.coreApi.database.migrations.MIGRATION_VERSION_KEY
import dev.kokoroidkt.coreApi.database.migrations.MigrationResult
import dev.kokoroidkt.coreApi.database.migrations.computeTableHash
import dev.kokoroidkt.coreApi.database.migrations.trySyncDB
import dev.kokoroidkt.coreApi.database.tables.MigrationTable
import dev.kokoroidkt.coreApi.exceptions.CriticalException
import dev.kokoroidkt.coreApi.logging.LogFiles
import dev.kokoroidkt.coreApi.logging.LogLevelManager
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.pluginApi.plugin.Plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.java.KoinJavaComponent
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

class KokoroidLauncher(
    private val pluginPreloader: PluginPreloader = PluginPreloader(),
    private val adapterPreloader: AdapterPreloader = AdapterPreloader(),
    private val driverPreloader: DriverPreloader = DriverPreloader(),
) : KoinComponent {
    private val pluginManager: PluginManager by inject()
    private val adapterManager: AdapterManager by inject()
    private val driverManager: DriverManager by inject()
    private val config: Config by inject()
    private val globalEventLoop: GlobalEventLoop by inject()
    private val crashRegistry: CrashRegistry by inject()
    private val runtimeState: RuntimeState by inject()
    private val databaseManager: DatabaseManager by inject()
    private val logger = getLogger("KokoroidLifecycle")

    private val shutdownThread =
        Thread {
            val logger = getLogger("Shutdown")
            logger.info { "Shutting down...\n" }
            runtimeState.state = InternalState.BeforeStopping()
            val ex = runCatching { stopAllExtensions() }.exceptionOrNull()
            logger.info { "All extensions stopped" }
            databaseManager.close()
            logger.info { "Kokoroid Database closed" }
            if (ex != null) {
                logger.error(ex) { "Error when shutdown: ${ex::class.qualifiedName}: ${ex.message}" }
            }
            if (crashRegistry.isCrashed) {
                logger.error { "###### Kokoroid Crash Report ######" }
                crashRegistry.logRecords()
                runtimeState.state =
                    InternalState.Stopped(crashRegistry.exitCode)
            } else {
                runtimeState.state = InternalState.Stopped(ExitStatus.SUCCESS_EXIT)
            }
            logger.info { "Kokoroid shutdown, bye" }
        }

    private fun initKoin() {
        startKoin {
            modules(allModules)
        }
    }

    private fun initDB(doMigration: Boolean) {
        Path("kokoroid/datas/dev.kokoroid.core").toFile().mkdirs()
        if (config.basic.database.type == DatabaseType.H2) {
            logger.warn { "H2 database is used for development! MAKE SURE WHEN YOU ARE IN PRODUCTION, USE A PROPER DATABASE" }
        }
        val db =
            when (config.basic.database.type) {
                DatabaseType.H2,
                DatabaseType.SQLITE,
                -> {
                    Database.connect(
                        config.basic.database.jdbc,
                        driver = config.basic.database.type.jdbcClassName,
                    )
                }

                DatabaseType.MYSQL,
                DatabaseType.POSTGRESQL,
                -> {
                    Database.connect(
                        config.basic.database.jdbc,
                        driver = config.basic.database.type.jdbcClassName,
                        user = config.basic.database.username,
                        password = config.basic.database.password,
                    )
                }
            }
        databaseManager.init(db)
        logger.debug { "jdbc url: ${db.url}" }
        when (val result = trySyncDB()) {
            MigrationResult.NoChange -> {
                logger.debug { "Database is up to date" }
            }

            is MigrationResult.NotMatch -> {
                if (!doMigration) {
                    val errorMessage =
                        """
                            
                            
                        Database too old! Please run migration.
                        Required: ${result.newHash}, Actual: ${result.oldHash}
                        Migration Script Location (for reference): ${result.migrationScriptFilename}
                        You can use --migration option to run migration automatically
                        or use the SQL script to do migration by yourself.


                        """.trimIndent()
                    logger.error { errorMessage }
                    crashRegistry.stopNow(DATABASE_TOO_OLD)
                } else {
                    logger.warn { "Trying to auto migration ${result.oldHash} --> ${result.newHash}" }
                    databaseManager.transaction {
                        val sqls =
                            MigrationUtils.statementsRequiredForDatabaseMigration(
                                *allTables,
                            )
                        sqls.forEach {
                            exec(it)
                            logger.info { "executing -> $it " }
                        }
                        MigrationTable.update({ MigrationTable.key eq MIGRATION_VERSION_KEY }) {
                            it[value] = computeTableHash()
                        }
                    }
                    logger.info { "Auto migration ${result.oldHash} -> ${result.newHash} completed successfully" }
                }
                // databaseManager.init(db)
            }

            MigrationResult.CreateNew -> {
                logger.info { "Kokoroid database has been created" }
            }
        }
    }

    /**
     * 启动Kokoroid
     */
    fun launch(
        validatingOnly: Boolean,
        isDebug: Boolean = false,
        doMigration: Boolean = false,
    ) {
        LogLevelManager.setLevel(Level.INFO)
        if (isDebug) {
            LogLevelManager.setLevel(Level.DEBUG)
        }

        println(
            """
                           _                         _      _ 
              /\ /\  ___  | | __  ___   _ __   ___  (_)  __| |
             / //_/ / _ \ | |/ / / _ \ | '__| / _ \ | | / _` |
            / __ \ | (_) ||   < | (_) || |   | (_) || || (_| |
            \/  \/  \___/ |_|\_\ \___/ |_|    \___/ |_| \__,_|                                                            
            """.trimIndent(),
        )
        LogFiles.archiveLatestLogOnStartup(Paths.get("./kokoroid/logs"))
        logger.info { "Kokoroid Version ${KokoroidVersion.version} (Build #${KokoroidVersion.gitHash})" }
        logger.info { "「ちょー高尚な理由で 目指すは　ひとりぼっち産業革命」" }
        logger.info { "Kokoroid Starting....." }
        initKoin()
        KoinJavaComponent.getKoin().get<RuntimeState>().state = InternalState.Initializing()
        Runtime.getRuntime().addShutdownHook(shutdownThread)
        initDB(doMigration)
        try {
            initAllExtensions()
            if (!validatingOnly) {
                runtimeState.state =
                    InternalState.Running()
                runBlocking {
                    globalEventLoop.start()
                }
            }
        } catch (e: CriticalException) {
            logger.error(e) { "CRITICAL Error occurred" }
            crashRegistry.recordAndRequestStop(e, null)
            shutdown()
        } catch (e: CancellationException) {
            logger.debug { "A CancellationException occurred: $e" }
            throw e
        } catch (e: Exception) {
            // 漏网异常视为框架不可恢复错误，按 Critical 上报并停机
            logger.error(e) { "occurred Uncatched Exception" }
            crashRegistry.recordAndRequestStop(
                CriticalException(cause = e),
                event = null,
            )
            shutdown()
        }
    }

    internal fun shutdown() {
        shutdownThread.start()
        shutdownThread.join()
        val exitCode =
            if (runtimeState.state is InternalState.Stopped) {
                (runtimeState.state as InternalState.Stopped).statusCode
            } else {
                logger.error { "Wrong exiting runtimeStatus.status: ${runtimeState.state::class.qualifiedName}" }
                ExitStatus.WRONG_EXIT_STATE
            }
        crashRegistry.stopNow(exitCode = exitCode)
    }

    fun stopAllExtensions() {
        runtimeState.state = InternalState.Stopping(InternalState.Stopping.StoppingStep.StoppingDrivers())
        stopDrivers()
        runtimeState.state = InternalState.Stopping(InternalState.Stopping.StoppingStep.StoppingAdapters())
        stopAdapters()
        runtimeState.state = InternalState.Stopping(InternalState.Stopping.StoppingStep.UnloadingPlugins())
        unloadPlugins()
        runtimeState.state = InternalState.Stopping(InternalState.Stopping.StoppingStep.UnloadingAdapters())
        unloadAdapters()
        runtimeState.state = InternalState.Stopping(InternalState.Stopping.StoppingStep.UnloadingDrivers())
        unloadDrivers()
    }

    fun stopDrivers() {
        val logger = getLogger("DriverStopper")
        logger.info { "Stopping Drivers" }
        driverManager.driverList.forEach {
            try {
                logger.debug { "Stopping Driver ${it.driverId}" }
                it.stop()
            } catch (e: Exception) {
                logger.error(e) { "error while stopping ${it.driverId}" }
            }
        }
    }

    fun stopAdapters() {
        val logger = getLogger("AdapterStopper")
        logger.info { "Stopping Adapters" }
        adapterManager.adapterList.forEach {
            try {
                logger.debug { "Stopping Adapter ${it.adapterId}" }
                it.stop()
            } catch (e: Exception) {
                logger.error(e) { "error while stopping ${it.adapterId}" }
            }
        }
    }

    fun unloadPlugins() {
        val logger = getLogger("PluginUnloader")
        logger.info { "Unloading Plugins" }
        pluginManager.pluginList.forEach {
            try {
                logger.debug { "unloading Plugin ${it.pluginId}" }
                it.disable()
                it.unload()
            } catch (e: Exception) {
                logger.error(e) { "error while unloading ${it.pluginId}" }
            }
        }
    }

    fun unloadAdapters() {
        val logger = getLogger("AdapterUnloader")
        logger.info { "Unloading Adapters" }
        adapterManager.adapterList.forEach { container ->
            try {
                logger.debug { "unloading Adapter ${container.adapterId}" }
                adapterManager.unloadAdapter(container)
            } catch (e: Exception) {
                logger.error(e) { "error while unloading ${container.adapterId}" }
            }
        }
    }

    fun unloadDrivers() {
        val logger = getLogger("DriverUnloader")
        logger.info { "Unloading Drivers" }
        driverManager.driverList.forEach { container ->
            try {
                logger.debug { "unloading ${container.driverId}" }
                driverManager.unloadDriver(container)
            } catch (e: Exception) {
                logger.error(e) { "error while unloading ${container.driverId}" }
            }
        }
    }

    /**
     * 初始化所有拓展
     * 顺序：
     * [Driver.onLoad] -> [Adapter.onLoad] -> [Plugin.onLoad] -> [Plugin.onEnable] -> [Adapter.onStart] -> [Driver.onStart]
     * 1. Driver首先加载，Adapter才能检查有没有自己需要的Driver
     * 2. Plugin才能发现有没有自己需要的Adapter
     * 3. Plugin可能向Adapter或者其他组件挂钩/发出请求，所以[Plugin.onEnable]后执行[Adapter.onStart]
     * 4. Driver最后启动，因为Driver需要处理Adapter的网络需求（轮询声明/开启API端点）
     * 5. 按照优先级顺序加载每个Plugin，Plugin启动成功后会立刻调用他的[Plugin.onEnable]方法
     */
    fun initAllExtensions() {
        // Phase 1: Scan directories, collect JAR paths into preloaders
        installDrivers()
        installAdapters()
        installPlugins()

        // Phase 2: Preload — read metadata, build dependency graph, create classloader chain
        val preloadResult =
            ExtensionPreloader(
                driverPreloader,
                adapterPreloader,
                pluginPreloader,
            ).preload()

        // Phase 3: Load extensions using dependency-aware classloaders
        runtimeState.state = InternalState.Starting(InternalState.Starting.StartingStep.LoadingDrivers())
        loadDrivers(preloadResult)
        runtimeState.state = InternalState.Starting(InternalState.Starting.StartingStep.LoadingAdapters())
        loadAdapters(preloadResult)
        runtimeState.state = InternalState.Starting(InternalState.Starting.StartingStep.LoadingPlugins())
        loadPlugins(preloadResult)

        runtimeState.state = InternalState.Starting(InternalState.Starting.StartingStep.StartingAdapters())
        startAdapters()
        runtimeState.state = InternalState.Starting(InternalState.Starting.StartingStep.StartingDrivers())
        startDrivers()
        runtimeState.state = InternalState.AfterStarting()
    }

    fun startAdapters() {
        val logger = getLogger("AdapterStarter")
        logger.info { "Starting Adapters" }
        adapterManager.adapterList.forEach {
            try {
                logger.debug { "starting Adapter ${it.adapterId}" }
                it.start()
            } catch (e: Exception) {
                logger.error(e) { "error while starting ${it.adapterId}" }
            }
        }
    }

    fun startDrivers() {
        val logger = getLogger("DriverStarter")
        logger.info { "Starting Drivers" }
        driverManager.driverList.forEach {
            try {
                logger.debug { "starting Driver ${it.driverId}" }
                it.start()
            } catch (e: Exception) {
                logger.error(e) {
                    "error while starting ${it.driverId}"
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Install phase — scan directories, collect JAR paths
    // ─────────────────────────────────────────────────────────────────────────

    private fun installDrivers() {
        driverPreloader.jarPaths.addAll(jarPathsFrom(config.basic.driverDirectory))
    }

    private fun installAdapters() {
        adapterPreloader.jarPaths.addAll(jarPathsFrom(config.basic.adapterDirectory))
    }

    private fun installPlugins() {
        pluginPreloader.jarPaths.addAll(jarPathsFrom(config.basic.pluginDirectory))
    }

    private fun jarPathsFrom(dir: Path): List<Path> = dir.walk().filter { it.isRegularFile() && it.extension == "jar" }.toList()

    // ─────────────────────────────────────────────────────────────────────────
    // Load phase — use dependency-aware classloaders from preload result
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadDrivers(preloadResult: ExtensionPreloader.PreloadResult) {
        val logger = getLogger("DriverLoader")
        logger.info { "loading drivers..." }
        logger.info { "find ${preloadResult.allDescriptors.count { it.type == ExtensionType.DRIVER }} drivers" }

        preloadResult.allDescriptors
            .filter { it.type == ExtensionType.DRIVER }
            .sortedBy { it.name }
            .forEach { desc ->
                val cl = preloadResult.classLoaderMap[desc.identifier] ?: return@forEach
                try {
                    logger.debug { "try to load ${desc.jarFile.absolutePath}" }
                    val (driver, metadata, _) =
                        DriverLoader(
                            desc.jarFile,
                            cl as DependencyAwareClassLoader,
                        ).loadDriver()
                    val container = driverManager.create(driver, metadata)
                    driverManager.register(container)
                    logger.debug { "${container.driverId} metadata: ${Json.encodeToString(metadata)}" }
                    logger.info { "Loading ${container.driverId}" }
                    driverManager.loadDriver(container)
                    logger.info { "Loaded ${container.driverId} successfully" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load driver: ${e.message}" }
                }
            }
        logger.info { "successfully loaded ${driverManager.length} drivers" }
    }

    private fun loadAdapters(preloadResult: ExtensionPreloader.PreloadResult) {
        val logger = getLogger("AdapterLoader")
        logger.info { "loading adapters..." }
        logger.info { "find ${preloadResult.allDescriptors.count { it.type == ExtensionType.ADAPTER }} adapters" }

        preloadResult.allDescriptors
            .filter { it.type == ExtensionType.ADAPTER }
            .sortedBy { it.name }
            .forEach { desc ->
                val cl = preloadResult.classLoaderMap[desc.identifier] ?: return@forEach
                try {
                    logger.debug { "try to load ${desc.jarFile.absolutePath}" }
                    val (adapter, meta, _) = AdapterLoader(desc.jarFile, cl as DependencyAwareClassLoader).loadAdapter()
                    val container = adapterManager.create(adapter, meta)
                    adapterManager.register(container)
                    logger.debug { "${container.adapterId} metadata: ${Json.encodeToString(meta)}" }
                    logger.info { "Loading ${container.adapterId}" }
                    adapterManager.loadAdapter(container)
                    logger.info { "Loaded ${container.adapterId} successfully" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load adapter: ${e.message}" }
                }
            }
        logger.info { "successfully loaded ${adapterManager.length} adapters" }
    }

    private fun loadPlugins(preloadResult: ExtensionPreloader.PreloadResult) {
        val logger = getLogger("PluginLoader")
        logger.info { "loading plugins..." }
        logger.info { "find ${preloadResult.allDescriptors.count { it.type == ExtensionType.PLUGIN }} plugins." }

        preloadResult.allDescriptors
            .filter { it.type == ExtensionType.PLUGIN }
            .sortedBy { it.name }
            .forEach { desc ->
                val cl = preloadResult.classLoaderMap[desc.identifier] ?: return@forEach
                try {
                    logger.debug { "Try to loading: ${desc.jarFile.absolutePath}" }
                    val (plugin, meta, _) = PluginLoader(desc.jarFile, cl as DependencyAwareClassLoader).loadPlugin()
                    val container = pluginManager.create(plugin, meta)
                    pluginManager.register(container)
                    logger.debug { "${container.pluginId} metadata: ${Json.encodeToString(meta)}" }
                    logger.info { "loading ${container.pluginId}" }
                    pluginManager.loadPlugin(container)
                    logger.debug { "enable ${container.pluginId} plugin" }
                    pluginManager.enablePlugin(container)
                    logger.info { "enable ${container.pluginId} plugin successfully" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load plugin: ${e.message}" }
                }
            }
        logger.info { "successfully loaded ${pluginManager.length} plugins" }
    }
}
