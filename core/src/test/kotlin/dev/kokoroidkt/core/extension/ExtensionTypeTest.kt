// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.extension

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionTypeTest {
    // ──────────────────────────────────────────────
    // DRIVER as dependant
    // ──────────────────────────────────────────────

    @Test
    fun `DRIVER can depend on DRIVER`() {
        assertTrue(ExtensionType.DRIVER.canDependOn(ExtensionType.DRIVER))
    }

    @Test
    fun `DRIVER cannot depend on ADAPTER`() {
        assertFalse(ExtensionType.DRIVER.canDependOn(ExtensionType.ADAPTER))
    }

    @Test
    fun `DRIVER cannot depend on PLUGIN`() {
        assertFalse(ExtensionType.DRIVER.canDependOn(ExtensionType.PLUGIN))
    }

    @Test
    fun `DRIVER cannot depend on EXTENSION`() {
        assertFalse(ExtensionType.DRIVER.canDependOn(ExtensionType.EXTENSION))
    }

    // ──────────────────────────────────────────────
    // ADAPTER as dependant
    // ──────────────────────────────────────────────

    @Test
    fun `ADAPTER can depend on DRIVER`() {
        assertTrue(ExtensionType.ADAPTER.canDependOn(ExtensionType.DRIVER))
    }

    @Test
    fun `ADAPTER can depend on ADAPTER`() {
        assertTrue(ExtensionType.ADAPTER.canDependOn(ExtensionType.ADAPTER))
    }

    @Test
    fun `ADAPTER cannot depend on PLUGIN`() {
        assertFalse(ExtensionType.ADAPTER.canDependOn(ExtensionType.PLUGIN))
    }

    @Test
    fun `ADAPTER cannot depend on EXTENSION`() {
        assertFalse(ExtensionType.ADAPTER.canDependOn(ExtensionType.EXTENSION))
    }

    // ──────────────────────────────────────────────
    // PLUGIN as dependant
    // ──────────────────────────────────────────────

    @Test
    fun `PLUGIN can depend on DRIVER`() {
        assertTrue(ExtensionType.PLUGIN.canDependOn(ExtensionType.DRIVER))
    }

    @Test
    fun `PLUGIN can depend on ADAPTER`() {
        assertTrue(ExtensionType.PLUGIN.canDependOn(ExtensionType.ADAPTER))
    }

    @Test
    fun `PLUGIN can depend on PLUGIN`() {
        assertTrue(ExtensionType.PLUGIN.canDependOn(ExtensionType.PLUGIN))
    }

    @Test
    fun `PLUGIN cannot depend on EXTENSION`() {
        assertFalse(ExtensionType.PLUGIN.canDependOn(ExtensionType.EXTENSION))
    }

    // ──────────────────────────────────────────────
    // EXTENSION as dependant
    // ──────────────────────────────────────────────

    @Test
    fun `EXTENSION cannot depend on DRIVER`() {
        assertFalse(ExtensionType.EXTENSION.canDependOn(ExtensionType.DRIVER))
    }

    @Test
    fun `EXTENSION cannot depend on ADAPTER`() {
        assertFalse(ExtensionType.EXTENSION.canDependOn(ExtensionType.ADAPTER))
    }

    @Test
    fun `EXTENSION cannot depend on PLUGIN`() {
        assertFalse(ExtensionType.EXTENSION.canDependOn(ExtensionType.PLUGIN))
    }

    @Test
    fun `EXTENSION cannot depend on EXTENSION`() {
        assertFalse(ExtensionType.EXTENSION.canDependOn(ExtensionType.EXTENSION))
    }
}
