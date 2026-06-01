// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.pluginApi.utils

import dev.kokoroidkt.coreApi.config.decodeDataFromPath
import dev.kokoroidkt.coreApi.config.encodeDataToPath
import dev.kokoroidkt.coreApi.config.kokoroidConfigRoot
import dev.kokoroidkt.coreApi.exceptions.CriticalException
import dev.kokoroidkt.pluginApi.conversation.ConversationOrchestrator
import dev.kokoroidkt.pluginApi.conversation.Processable
import dev.kokoroidkt.pluginApi.exceptions.ConversationRegisterFailedException
import dev.kokoroidkt.pluginApi.factory.ConversationOrchestratorFactory
import dev.kokoroidkt.pluginApi.plugin.Plugin
import dev.kokoroidkt.pluginApi.plugin.PluginMeta
import dev.kokoroidkt.pluginApi.plugin.PluginRegistry
import org.koin.java.KoinJavaComponent.getKoin
import java.nio.file.Path
import java.nio.file.Paths

fun getRegistry(): PluginRegistry = getKoin().get<PluginRegistry>()

fun Plugin.getId(): String {
    val registry = getRegistry()
    try {
        return registry.getPluginId(this::class.java)!!
    } catch (e: NullPointerException) {
        throw CriticalException("Plugin Id found: ${this::class.qualifiedName}", cause = e)
    }
}

internal fun Plugin.getContainer() =
    try {
        this.getId().let { getRegistry()[it] }!!
    } catch (e: NullPointerException) {
        throw CriticalException("Plugin Container Not found: ${this::class.qualifiedName}", cause = e)
    }

fun Plugin.metadata(): PluginMeta = getContainer().metadata

fun Plugin.addConversation(processor: Processable) {
    val orchestrator: ConversationOrchestrator
    val container = getContainer()
    try {
        orchestrator = getKoin().get<ConversationOrchestratorFactory>().create(processor)
        container.registerOrchestrator(orchestrator)
    } catch (e: Exception) {
        throw ConversationRegisterFailedException(
            "Register Conversation ${processor.name()} failed",
            container,
            e,
        )
    }
}

/**
 * 将配置保存到文件。
 *
 * @param config 要保存的配置对象。
 * @param path 配置文件相对于 plugin/<plugin_name> 的路径。默认为 /settings.conf。
 */
inline fun <reified T : Any> Plugin.saveConfigToFile(
    config: T,
    path: Path = Paths.get("settings.conf"),
) {
    encodeDataToPath(config, Path.of("plugin", metadata().name).resolve(path))
}

/**
 * 从文件加载配置。
 *
 * @param defaultWhenNull 当配置文件不存在时返回的默认值
 * @param path 配置文件相对于 plugin/<plugin_name> 的路径。默认为 /settings.conf。
 * @param createWhenNull 当配置文件不存在时是否创建默认配置文件，默认为 true
 * @return 加载的配置对象。
 */
inline fun <reified T : Any> Plugin.loadConfigFromFile(
    defaultWhenNull: T,
    path: Path = Paths.get("settings.conf"),
    createWhenNull: Boolean = true,
): T {
    val fullPath = Path.of("plugin", metadata().name).resolve(path)
    val configFile = kokoroidConfigRoot.resolve(fullPath).toFile()
    if (configFile.exists()) {
        return decodeDataFromPath<T>(fullPath)
    } else if (createWhenNull) {
        saveConfigToFile(defaultWhenNull, path)
    }
    return defaultWhenNull
}
