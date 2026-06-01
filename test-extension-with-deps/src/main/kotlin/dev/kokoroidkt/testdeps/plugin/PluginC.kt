// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.testdeps.plugin

import dev.kokoroidkt.pluginApi.plugin.KotlinPlugin
import dev.kokoroidkt.testdeps.Tracker

class PluginC : KotlinPlugin() {
    override fun onLoad() {
        Tracker.calls.add("PluginC.onLoad")
    }

    override fun onEnable() {
        Tracker.calls.add("PluginC.onEnable")
    }

    override fun onDisable() {
        Tracker.calls.add("PluginC.onDisable")
    }

    override fun onUnload() {
        Tracker.calls.add("PluginC.onUnload")
    }
}
