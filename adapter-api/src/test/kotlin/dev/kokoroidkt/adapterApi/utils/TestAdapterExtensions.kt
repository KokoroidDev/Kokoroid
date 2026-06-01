// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.adapterApi.utils

import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.adapterApi.adapter.AdapterContainer
import dev.kokoroidkt.adapterApi.adapter.AdapterMeta
import dev.kokoroidkt.adapterApi.adapter.AdapterRegistry
import dev.kokoroidkt.coreApi.bot.Bot
import dev.kokoroidkt.coreApi.config.kokoroidConfigRoot
import dev.kokoroidkt.coreApi.user.UserContainer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Serializable
data class TestConfigData(
    val foo: String,
    val bar: Int,
)

class MockAdapter : Adapter {
    override fun onLoad() {}

    override fun onStart() {}

    override fun onStop() {}

    override fun onUnload() {}

    override fun getBot(botId: String): Bot = throw NotImplementedError()

    override fun getBotList(): List<Bot> = emptyList()

    override fun getUserContainer(): UserContainer = throw NotImplementedError()
}

class MockAdapterRegistry(
    private val adapter: Adapter,
    private val meta: AdapterMeta,
) : AdapterRegistry {
    private val container = AdapterContainer(adapter, meta)

    override fun get(adapterId: String): AdapterContainer? = if (adapterId == container.adapterId) container else null

    override fun getAdapterId(adapterClass: Class<*>): String? = if (adapterClass == adapter::class.java) container.adapterId else null
}

class TestAdapterExtensions {
    private val testMeta =
        AdapterMeta(
            name = "TestAdapter",
            version = "1.0.0",
            mainClass = "dev.kokoroidkt.adapterApi.utils.MockAdapter",
            authors = listOf("test"),
            description = "test",
            website = "test",
        )
    private val adapter = MockAdapter()

    @BeforeEach
    fun setup() {
        startKoin {
            modules(
                module {
                    single<AdapterRegistry> { MockAdapterRegistry(adapter, testMeta) }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        val configPath = kokoroidConfigRoot.resolve("adapter/TestAdapter/settings.conf")
        configPath.deleteIfExists()
    }

    @Test
    fun testAdapterGetId() {
        val adapterId = adapter.getId()
        assertNotNull(adapterId)
        println("Adapter ID: $adapterId")

        // 检查MockAdapterRegistry中的container.adapterId
        val registry = getKoin().get<AdapterRegistry>() as MockAdapterRegistry
        val container = registry.get(adapterId!!)
        assertNotNull(container)
        println("Container adapterId: ${container?.adapterId}")

        assertEquals(adapterId, container?.adapterId)
    }

    @Test
    fun testAdapterGetMetadata() {
        val metadata = adapter.getMetadata()
        assertNotNull(metadata)
        println("Metadata: $metadata")
        assertEquals("TestAdapter", metadata?.name)
    }

    @Test
    fun testAdapterSaveAndLoadConfig() {
        val config = TestConfigData("hello", 123)

        // 先检查metadata
        val metadata = adapter.getMetadata()
        println("Metadata: $metadata")
        println("Metadata name: ${metadata?.name}")

        adapter.saveConfigToFile(config)

        // 检查文件是否被创建
        val configPath = kokoroidConfigRoot.resolve("adapter/TestAdapter/settings.conf")
        println("Config file exists: ${configPath.exists()}")
        println("Config file path: ${configPath.toAbsolutePath()}")

        // 列出目录内容
        val dir = kokoroidConfigRoot.resolve("adapter/TestAdapter").toFile()
        if (dir.exists()) {
            println("Directory exists: ${dir.absolutePath}")
            println("Files in directory: ${dir.listFiles()?.joinToString { it.name }}")
        } else {
            println("Directory does not exist: ${dir.absolutePath}")
        }

        // 检查loadConfigFromFile中使用的路径
        val fullPath = Path.of("adapter", metadata!!.name).resolve(Paths.get("settings.conf"))
        println("Full path in loadConfigFromFile: $fullPath")
        println("Full path exists: ${fullPath.toFile().exists()}")
        println("Full path absolute: ${fullPath.toAbsolutePath()}")

        // 检查decodeDataFromPath使用的路径
        val decodePath = kokoroidConfigRoot.resolve(fullPath)
        println("Decode path: $decodePath")
        println("Decode path exists: ${decodePath.toFile().exists()}")

        val loadedConfig = adapter.loadConfigFromFile(TestConfigData("default", 0))
        println("Loaded config: $loadedConfig")
        println("Expected config: $config")
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testAdapterLoadConfigWhenFileExists() {
        val config = TestConfigData("test", 456)
        adapter.saveConfigToFile(config)

        val loadedConfig = adapter.loadConfigFromFile(TestConfigData("default", 0))
        assertEquals(config, loadedConfig)
    }

    @Test
    fun testAdapterLoadConfigWhenFileNotExistsAndCreateWhenNullTrue() {
        val defaultConfig = TestConfigData("default", 999)
        val configPath = kokoroidConfigRoot.resolve("adapter/TestAdapter/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            adapter.loadConfigFromFile(
                defaultConfig,
                createWhenNull = true,
            )

        assertEquals(defaultConfig, loadedConfig)
        assertTrue(configPath.exists())

        val fileContent = Files.readString(configPath)
        println("File content: $fileContent")
        assertTrue(fileContent.contains("foo") && fileContent.contains("default"))
        assertTrue(fileContent.contains("bar") && fileContent.contains("999"))
    }

    @Test
    fun testAdapterLoadConfigWhenFileNotExistsAndCreateWhenNullFalse() {
        val defaultConfig = TestConfigData("default", 999)
        val configPath = kokoroidConfigRoot.resolve("adapter/TestAdapter/settings.conf")

        configPath.deleteIfExists()

        val loadedConfig =
            adapter.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )

        assertEquals(defaultConfig, loadedConfig)
        assertFalse(configPath.exists())
    }

    @Test
    fun testAdapterLoadConfigWhenFileHasInvalidFormat() {
        val defaultConfig = TestConfigData("default", 999)
        val configPath = kokoroidConfigRoot.resolve("adapter/TestAdapter/settings.conf")

        // 创建格式错误的配置文件
        configPath.parent.toFile().mkdirs()
        Files.writeString(configPath, "invalid json content")

        // 当配置文件格式错误时，应该抛出异常
        assertThrows(Exception::class.java) {
            adapter.loadConfigFromFile(
                defaultConfig,
                createWhenNull = false,
            )
        }
    }
}
