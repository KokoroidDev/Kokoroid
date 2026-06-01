// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.adapter

import dev.kokoroidkt.core.exceptions.LoadAdapterFailedException
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdapterLoaderTest {
    @Test
    fun `loadAdapter returns Triple with adapter meta and classloader`() {
        val tempDir =
            kotlin.io.path
                .createTempDirectory("adapterLoaderTest")
                .toFile()
        try {
            val jarFile = File(tempDir, "test-adapter.jar")
            createJarWithMeta(
                jarFile,
                """
                {
                    "name": "TestAdapter",
                    "version": "1.0.0",
                    "mainClass": "dev.kokoroidkt.core.adapter.SimpleTestAdapter",
                    "authors": ["TestAuthor"],
                    "description": "Test adapter for unit tests",
                    "website": null,
                    "priority": 500
                }
                """.trimIndent(),
            )

            val classLoader =
                DependencyAwareClassLoader(
                    jarFile = jarFile,
                    dependencyClassLoaders = listOf(SimpleTestAdapter::class.java.classLoader),
                )

            val loader = AdapterLoader(jarFile, classLoader)
            val (adapter, meta, returnedClassLoader) = loader.loadAdapter()

            assertNotNull(adapter)
            assertTrue(adapter is SimpleTestAdapter)
            assertEquals("TestAdapter", meta.name)
            assertEquals("1.0.0", meta.version)
            assertEquals(500, meta.priority)
            assertEquals("TestAuthor", meta.authors?.firstOrNull())
            assertEquals("Test adapter for unit tests", meta.description)
            assertEquals(classLoader, returnedClassLoader)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loadAdapter reuses provided classLoader instance`() {
        val tempDir =
            kotlin.io.path
                .createTempDirectory("adapterLoaderTest")
                .toFile()
        try {
            val jarFile = File(tempDir, "test-adapter.jar")
            createJarWithMeta(
                jarFile,
                """
                {
                    "name": "TestAdapter",
                    "version": "1.0.0",
                    "mainClass": "dev.kokoroidkt.core.adapter.SimpleTestAdapter",
                    "authors": ["TestAuthor"],
                    "description": "Test adapter for unit tests",
                    "website": null,
                    "priority": 500
                }
                """.trimIndent(),
            )

            val classLoader =
                DependencyAwareClassLoader(
                    jarFile = jarFile,
                    dependencyClassLoaders = listOf(SimpleTestAdapter::class.java.classLoader),
                )

            val loader = AdapterLoader(jarFile, classLoader)
            val result = loader.loadAdapter()

            // The Triple must contain the exact same classLoader instance
            assertTrue(result.third === classLoader, "classLoader must be the same instance")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loadAdapter throws LoadAdapterFailedException when JAR has no meta entry`() {
        val tempDir =
            kotlin.io.path
                .createTempDirectory("adapterLoaderTest")
                .toFile()
        try {
            val jarFile = File(tempDir, "empty.jar")
            JarOutputStream(FileOutputStream(jarFile)).use { /* empty jar */ }

            val classLoader =
                DependencyAwareClassLoader(
                    jarFile = jarFile,
                    dependencyClassLoaders = emptyList(),
                )

            val loader = AdapterLoader(jarFile, classLoader)

            assertFailsWith<LoadAdapterFailedException> {
                loader.loadAdapter()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `loadAdapter throws LoadAdapterFailedException when main class not found`() {
        val tempDir =
            kotlin.io.path
                .createTempDirectory("adapterLoaderTest")
                .toFile()
        try {
            val jarFile = File(tempDir, "broken-adapter.jar")
            createJarWithMeta(
                jarFile,
                """
                {
                    "name": "BrokenAdapter",
                    "version": "0.0.1",
                    "mainClass": "dev.kokoroidkt.core.adapter.NonExistentAdapter",
                    "authors": ["TestAuthor"],
                    "description": "This adapter class does not exist",
                    "website": null,
                    "priority": 100
                }
                """.trimIndent(),
            )

            val classLoader =
                DependencyAwareClassLoader(
                    jarFile = jarFile,
                    dependencyClassLoaders = emptyList(),
                )

            val loader = AdapterLoader(jarFile, classLoader)

            assertFailsWith<LoadAdapterFailedException> {
                loader.loadAdapter()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun createJarWithMeta(
        jarFile: File,
        metaJson: String,
    ) {
        JarOutputStream(FileOutputStream(jarFile)).use { jos ->
            jos.putNextEntry(ZipEntry("adapter-meta.json"))
            jos.write(metaJson.toByteArray())
            jos.closeEntry()
        }
    }
}
