// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.driverApi.utils

import dev.kokoroidkt.coreApi.config.kokoroidConfigRoot
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.driver.DriverContainer
import dev.kokoroidkt.driverApi.driver.DriverMeta
import dev.kokoroidkt.driverApi.driver.DriverRegistry
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Serializable
data class TestDriverConfig(
    val foo: String,
    val bar: Int,
)

class MockDriver : Driver() {
    override fun onLoad() {}

    override fun onStart() {}

    override fun onStop() {}

    override fun onUnload() {}
}

class MockDriverRegistry(
    private val driver: Driver,
    private val meta: DriverMeta,
) : DriverRegistry {
    private val container = DriverContainer(meta, driver)

    override fun get(driverId: String): DriverContainer? = if (driverId == container.driverId) container else null

    override fun getDriverId(driverClass: Class<*>): String? = if (driverClass == driver::class.java) container.driverId else null

    override fun get(driverClass: Class<*>): DriverContainer? = if (driverClass == driver::class.java) container else null
}

class TestDriverExtensions {
    private val testMeta =
        DriverMeta(
            name = "TestDriver",
            version = "1.0.0",
            mainClass = "dev.kokoroidkt.driverApi.utils.MockDriver",
            authors = listOf("test"),
            description = "test",
            website = "test",
        )
    private val driver = MockDriver()

    @BeforeEach
    fun setup() {
        startKoin {
            modules(
                module {
                    single<DriverRegistry> { MockDriverRegistry(driver, testMeta) }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        kokoroidConfigRoot.resolve("driver/TestDriver/settings.conf").deleteIfExists()
    }

    @Test
    fun testDriverSaveAndLoadConfig() {
        val config = TestDriverConfig("hello", 123)
        driver.saveConfigToFile(config)

        val loadedConfig = driver.loadConfigFromFile(TestDriverConfig("default", 0))
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testDriverLoadConfigWhenFileExists() {
        val config = TestDriverConfig("test", 456)
        driver.saveConfigToFile(config)

        val loadedConfig = driver.loadConfigFromFile(TestDriverConfig("default", 0))
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testDriverLoadConfigWhenFileNotExistsAndCreateWhenNullTrue() {
        val defaultConfig = TestDriverConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("driver/TestDriver/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            driver.loadConfigFromFile(
                defaultConfig,
                createWhenNull = true,
            )

        assertEquals(defaultConfig, loadedConfig)
        assertTrue(configPath.exists())

        val fileContent = Files.readString(configPath)
        assertTrue(fileContent.contains("foo") && fileContent.contains("default"))
        assertTrue(fileContent.contains("bar") && fileContent.contains("999"))
    }

    @Test
    fun testDriverLoadConfigWhenFileNotExistsAndCreateWhenNullFalse() {
        val defaultConfig = TestDriverConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("driver/TestDriver/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            driver.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )

        assertEquals(defaultConfig, loadedConfig)
        assertFalse(configPath.exists())
    }

    @Test
    fun testDriverLoadConfigWhenFileHasInvalidFormat() {
        val defaultConfig = TestDriverConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("driver/TestDriver/settings.conf")

        // 创建格式错误的配置文件
        configPath.parent.toFile().mkdirs()
        Files.writeString(configPath, "invalid json content")

        // 当配置文件格式错误时，应该抛出异常
        assertThrows(Exception::class.java) {
            driver.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )
        }
    }
}
