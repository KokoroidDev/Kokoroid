// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.testdeps.adapter

import dev.kokoroidkt.adapterApi.adapter.Adapter
import dev.kokoroidkt.coreApi.bot.Bot
import dev.kokoroidkt.coreApi.user.UserContainer
import dev.kokoroidkt.testdeps.Tracker

class AdapterB : Adapter {
    override fun onLoad() {
        Tracker.calls.add("AdapterB.onLoad")
    }

    override fun onStart() {
        Tracker.calls.add("AdapterB.onStart")
    }

    override fun onStop() {
        Tracker.calls.add("AdapterB.onStop")
    }

    override fun onUnload() {
        Tracker.calls.add("AdapterB.onUnload")
    }

    override fun getBot(botId: String): Bot = TODO("Not needed for testing")
    override fun getBotList(): List<Bot> = emptyList()
    override fun getUserContainer(): UserContainer = TODO("Not needed for testing")
}
