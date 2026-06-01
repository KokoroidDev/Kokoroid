// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.adapterApi.adapter

import kotlinx.serialization.Serializable

/**
 * 适配器元信息
 * Adapter metadata
 */
@Serializable
data class AdapterMeta(
    /**
     * 适配器名称
     * adapter name
     */
    val name: String,
    /**
     * 版本
     * version
     */
    val version: String,
    /**
     * 主类完整包名
     * The main class of this adapter.
     */
    val mainClass: String,
    /**
     * 适配器作者，适配器作者是一个数组，如果只有一个作者，就在数组里面写一个名字即可
     * The author of this adapter, it is an array, if there is only one author, just write one name in the array
     */
    val authors: List<String>?,
    /**
     * 适配器简介
     * description
     */
    val description: String?,
    /**
     * 支持网站
     * support website
     */
    val website: String?,
    /**
     * 优先级
     * The priority of this adapter.
     */
    val priority: Int = 1000,
    /**
     * 该适配器依赖的驱动列表，格式为 "name@mainClass"
     * The list of drivers that this adapter depends on, formatted as "name@mainClass".
     */
    val driverDependencies: List<String> = emptyList(),
    /**
     * 该适配器依赖的适配器列表，格式为 "name@mainClass"
     * The list of adapters that this adapter depends on, formatted as "name@mainClass".
     */
    val adapterDependencies: List<String> = emptyList(),
)
