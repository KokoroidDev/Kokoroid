// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.pluginApi.plugin

import kotlinx.serialization.Serializable

/**
 * 插件元数据
 * 可为hocon格式(plugin.meta.conf)和json格式(plugin.meta.json)
 * 此处推荐使用json
 */
@Serializable
class PluginMeta(
    /**
     * 插件名称.
     * The name of this plugin.
     */
    val name: String,
    /**
     * 插件作者，插件作者是一个数组，如果只有一个作者，就在数组里面写一个名字即可
     * The author of this plugin, it is an array, if there is only one author, just write one name in the array
     */
    val author: Array<String>?,
    /**
     * 插件描述
     * The description of this plugin.
     */
    val description: String?,
    /**
     * 插件版本
     * The version of this plugin.
     */
    val version: String,
    /**
     * 插件主类
     * The main class of this plugin.
     */
    val mainClass: String,
    /**
     * 插件支持网站
     * The website of this plugin.
     */
    val website: String?,
    /**
     * 插件依赖的插件
     * The plugins that this plugin depends on.
     */
    val dependedPlugins: Array<String>?,
    /**
     * 插件应当在哪些插件前加载
     * The plugins that this plugin should load before.
     */
    val loadBefore: Array<String>?,
    /**
     * 插件应当应当哪些插件后加载
     * The plugins that this plugin should load after.
     */
    val loadAfter: Array<String>?,
    /**
     * 插件的优先级
     * The priority of this plugin.
     */
    val priority: Int = 1000,
    /**
     * 该插件依赖的适配器列表，格式为 "name@mainClass"
     * The list of adapters that this plugin depends on, formatted as "name@mainClass".
     */
    val adapterDependencies: List<String> = emptyList(),
    /**
     * 该插件依赖的驱动列表，格式为 "name@mainClass"
     * The list of drivers that this plugin depends on, formatted as "name@mainClass".
     */
    val driverDependencies: List<String> = emptyList(),
)
