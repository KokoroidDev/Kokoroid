// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.testdeps.driver

import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.testdeps.Tracker

class DriverA : Driver() {
    override fun onLoad() {
        Tracker.calls.add("DriverA.onLoad")
    }

    override fun onStart() {
        Tracker.calls.add("DriverA.onStart")
    }

    override fun onStop() {
        Tracker.calls.add("DriverA.onStop")
    }

    override fun onUnload() {
        Tracker.calls.add("DriverA.onUnload")
    }
}
