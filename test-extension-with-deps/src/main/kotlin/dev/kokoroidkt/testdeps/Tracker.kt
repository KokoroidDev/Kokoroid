// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.testdeps

object Tracker {
    val calls = mutableListOf<String>()

    fun reset() {
        calls.clear()
    }
}
