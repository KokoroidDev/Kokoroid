// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import dev.kokoroidkt.core.extension.ExtensionType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DependencyGraphTest {

    private fun descriptor(
        identifier: String,
        name: String = identifier,
        mainClass: String = identifier,
        type: ExtensionType = ExtensionType.PLUGIN,
        dependencyIdentifiers: List<String> = emptyList(),
    ): ExtensionDescriptor = ExtensionDescriptor(
        identifier = identifier,
        name = name,
        mainClass = mainClass,
        type = type,
        jarFile = File("/tmp/test.jar"),
        dependencyIdentifiers = dependencyIdentifiers,
    )

    @Test
    fun `simple chain sorts topologically`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER)
        val b = descriptor("B@b", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("A@a"))
        val c = descriptor("C@c", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("B@b"))

        val result = DependencyGraph()
            .addNode(a)
            .addNode(b)
            .addNode(c)
            .resolve()

        assertEquals(listOf(a, b, c), result.orderedExtensions)
    }

    @Test
    fun `diamond dependency resolves correctly`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER)
        val b = descriptor("B@b", type = ExtensionType.ADAPTER, dependencyIdentifiers = listOf("A@a"))
        val c = descriptor("C@c", type = ExtensionType.ADAPTER, dependencyIdentifiers = listOf("A@a"))
        val d = descriptor("D@d", type = ExtensionType.PLUGIN, dependencyIdentifiers = listOf("B@b", "C@c"))

        val result = DependencyGraph()
            .addNode(a)
            .addNode(b)
            .addNode(c)
            .addNode(d)
            .resolve()

        assertEquals(4, result.orderedExtensions.size)
        // A must be first
        assertEquals(a, result.orderedExtensions[0])
        // B and C must precede D
        assertTrue(result.orderedExtensions.indexOf(b) < result.orderedExtensions.indexOf(d))
        assertTrue(result.orderedExtensions.indexOf(c) < result.orderedExtensions.indexOf(d))
    }

    @Test
    fun `no dependencies allows any order`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER)
        val b = descriptor("B@b", type = ExtensionType.ADAPTER)
        val c = descriptor("C@c", type = ExtensionType.PLUGIN)

        val result = DependencyGraph()
            .addNode(a)
            .addNode(b)
            .addNode(c)
            .resolve()

        assertEquals(3, result.orderedExtensions.size)
        assertTrue(result.orderedExtensions.containsAll(listOf(a, b, c)))
    }

    @Test
    fun `cycle A to B to A throws circular dependency`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("B@b"))
        val b = descriptor("B@b", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("A@a"))

        val exception = assertFailsWith<DependencyGraphException> {
            DependencyGraph()
                .addNode(a)
                .addNode(b)
                .resolve()
        }

        assertEquals(DependencyErrorType.CIRCULAR_DEPENDENCY, exception.errorType)
    }

    @Test
    fun `self cycle throws circular dependency`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("A@a"))

        val exception = assertFailsWith<DependencyGraphException> {
            DependencyGraph()
                .addNode(a)
                .resolve()
        }

        assertEquals(DependencyErrorType.CIRCULAR_DEPENDENCY, exception.errorType)
    }

    @Test
    fun `missing dependency throws`() {
        val a = descriptor("A@a", type = ExtensionType.DRIVER, dependencyIdentifiers = listOf("nonexistent"))

        val exception = assertFailsWith<DependencyGraphException> {
            DependencyGraph()
                .addNode(a)
                .resolve()
        }

        assertEquals(DependencyErrorType.MISSING_DEPENDENCY, exception.errorType)
    }

    @Test
    fun `type violation adapter depends on plugin throws`() {
        val plugin = descriptor("P@p", type = ExtensionType.PLUGIN)
        val adapter = descriptor("A@a", type = ExtensionType.ADAPTER, dependencyIdentifiers = listOf("P@p"))

        val exception = assertFailsWith<DependencyGraphException> {
            DependencyGraph()
                .addNode(plugin)
                .addNode(adapter)
                .resolve()
        }

        assertEquals(DependencyErrorType.INVALID_DEPENDENCY_TYPE, exception.errorType)
    }

    @Test
    fun `duplicate identifier throws`() {
        val a1 = descriptor("same@id", type = ExtensionType.DRIVER)
        val a2 = descriptor("same@id", type = ExtensionType.DRIVER)

        val exception = assertFailsWith<DependencyGraphException> {
            DependencyGraph()
                .addNode(a1)
                .addNode(a2)
                .resolve()
        }

        assertEquals(DependencyErrorType.DUPLICATE_EXTENSION, exception.errorType)
    }
}
