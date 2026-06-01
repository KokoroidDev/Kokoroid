// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.pluginApi.utils

import dev.kokoroidkt.coreApi.config.kokoroidConfigRoot
import dev.kokoroidkt.pluginApi.plugin.Plugin
import dev.kokoroidkt.pluginApi.plugin.PluginContainer
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import dev.kokoroidkt.pluginApi.plugin.PluginRegistry
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
data class TestPluginConfig(
    val foo: String,
    val bar: Int,
)

class MockPlugin : Plugin {
    override fun onLoad() {}

    override fun onEnable() {}

    override fun onDisable() {}

    override fun onUnload() {}
}

class MockPluginRegistry(
    private val plugin: Plugin,
    private val meta: PluginMeta,
) : PluginRegistry {
    private val container = PluginContainer(plugin, meta)

    override fun get(pluginId: String): PluginContainer? = if (pluginId == container.pluginId) container else null

    override fun getPluginId(pluginClass: Class<*>): String? = if (pluginClass == plugin::class.java) container.pluginId else null
}

class TestPluginExtensions {
    private val testMeta =
        PluginMeta(
            name = "TestPlugin",
            version = "1.0.0",
            mainClass = "dev.kokoroidkt.pluginApi.utils.MockPlugin",
            author = arrayOf("test"),
            description = "test",
            website = "test",
            dependedPlugins = emptyArray(),
            loadBefore = emptyArray(),
            loadAfter = emptyArray(),
        )
    private val plugin = MockPlugin()

    @BeforeEach
    fun setup() {
        startKoin {
            modules(
                module {
                    single<PluginRegistry> { MockPluginRegistry(plugin, testMeta) }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        kokoroidConfigRoot.resolve("plugin/TestPlugin/settings.conf").deleteIfExists()
    }

    @Test
    fun testPluginSaveAndLoadConfig() {
        val config = TestPluginConfig("hello", 123)
        plugin.saveConfigToFile(config)

        val loadedConfig = plugin.loadConfigFromFile(TestPluginConfig("default", 0))
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testPluginLoadConfigWhenFileExists() {
        val config = TestPluginConfig("test", 456)
        plugin.saveConfigToFile(config)

        val loadedConfig = plugin.loadConfigFromFile(TestPluginConfig("default", 0))
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testPluginLoadConfigWhenFileNotExistsAndCreateWhenNullTrue() {
        val defaultConfig = TestPluginConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("plugin/TestPlugin/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            plugin.loadConfigFromFile(
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
    fun testPluginLoadConfigWhenFileNotExistsAndCreateWhenNullFalse() {
        val defaultConfig = TestPluginConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("plugin/TestPlugin/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            plugin.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )

        assertEquals(defaultConfig, loadedConfig)
        assertFalse(configPath.exists())
    }

    @Test
    fun testPluginLoadConfigWhenFileHasInvalidFormat() {
        val defaultConfig = TestPluginConfig("default", 999)
        val configPath = kokoroidConfigRoot.resolve("plugin/TestPlugin/settings.conf")

        // 创建格式错误的配置文件
        configPath.parent.toFile().mkdirs()
        Files.writeString(configPath, "invalid json content")

        // 当配置文件格式错误时，应该抛出异常
        assertThrows(Exception::class.java) {
            plugin.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )
        }
    }
}
