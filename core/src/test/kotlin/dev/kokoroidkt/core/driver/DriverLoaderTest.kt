// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.driver

import dev.kokoroidkt.core.exceptions.LoadDriverFailedException
import dev.kokoroidkt.core.loader.DependencyAwareClassLoader
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.driver.DriverMeta
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A concrete Driver subclass used to verify that DriverLoader
 * can instantiate a driver loaded via the provided classloader.
 */
class TestDriver : Driver() {
    override fun onLoad() {}
    override fun onStart() {}
    override fun onStop() {}
    override fun onUnload() {}
}

/**
 * A mock [DependencyAwareClassLoader] that returns [TestDriver] for any class name.
 * This avoids needing real .class files or JARs on disk for testing DriverLoader.
 */
private class MockDependencyAwareClassLoader(
    jarFile: File,
) : DependencyAwareClassLoader(jarFile, emptyList()) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return TestDriver::class.java
    }
}

class DriverLoaderTest {

    @Test
    fun `loadDriver returns Triple with driver meta and classLoader`() {
        val tempJar = createTempJarWithMeta()
        val classLoader = MockDependencyAwareClassLoader(tempJar)
        val loader = DriverLoader(jarFile = tempJar, classLoader = classLoader)

        val (driver, meta, returnedCl) = loader.loadDriver()

        assertNotNull(driver)
        assertTrue(driver is TestDriver, "driver should be TestDriver")
        assertEquals("TestDriver", meta.name)
        assertEquals(classLoader, returnedCl)
    }

    @Test
    fun `loadDriver returns correct metadata fields from JSON`() {
        val tempJar = createTempJarWithMeta()
        val classLoader = MockDependencyAwareClassLoader(tempJar)
        val loader = DriverLoader(jarFile = tempJar, classLoader = classLoader)

        val (_, meta, _) = loader.loadDriver()

        assertEquals("TestDriver", meta.name)
        assertEquals("1.0.0", meta.version)
        assertEquals("dev.kokoroidkt.core.driver.TestDriver", meta.mainClass)
        assertEquals(listOf("TestAuthor"), meta.authors)
        assertEquals("A test driver for DriverLoader", meta.description)
        assertEquals(500, meta.priority)
    }

    @Test
    fun `loadDriver uses provided classLoader to load main class`() {
        var wasCalled = false
        var capturedClassName: String? = null
        val tempJar = createTempJarWithMeta()
        val trackingLoader = object : DependencyAwareClassLoader(tempJar, emptyList()) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                wasCalled = true
                capturedClassName = name
                return TestDriver::class.java
            }
        }
        val loader = DriverLoader(jarFile = tempJar, classLoader = trackingLoader)

        loader.loadDriver()

        assertTrue(wasCalled, "classLoader.loadClass should have been called")
        assertEquals("dev.kokoroidkt.core.driver.TestDriver", capturedClassName)
    }

    @Test
    fun `loadDriver throws LoadDriverFailedException when driver-meta JSON is missing`() {
        val emptyJar = createEmptyJar()
        val classLoader = MockDependencyAwareClassLoader(emptyJar)
        val loader = DriverLoader(jarFile = emptyJar, classLoader = classLoader)

        assertFailsWith<LoadDriverFailedException> {
            loader.loadDriver()
        }
    }

    @Test
    fun `loadDriver throws LoadDriverFailedException when classLoader fails to load class`() {
        val tempJar = createTempJarWithMeta()
        val failingLoader = object : DependencyAwareClassLoader(tempJar, emptyList()) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                throw ClassNotFoundException("Simulated failure: $name")
            }
        }
        val loader = DriverLoader(jarFile = tempJar, classLoader = failingLoader)

        assertFailsWith<LoadDriverFailedException> {
            loader.loadDriver()
        }
    }

    @Test
    fun `loadDriver throws LoadDriverFailedException when mainClass does not have no-arg constructor`() {
        val tempJar = createTempJarWithMeta()
        // CL returns a class without a no-arg constructor
        val noConstructorLoader = object : DependencyAwareClassLoader(tempJar, emptyList()) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                return ClassWithNoNoArgConstructor::class.java
            }
        }
        val loader = DriverLoader(jarFile = tempJar, classLoader = noConstructorLoader)

        assertFailsWith<LoadDriverFailedException> {
            loader.loadDriver()
        }
    }

    companion object {
        /**
         * Creates a temporary JAR file containing a valid driver-meta.json entry.
         * The file is scheduled for deletion on JVM exit.
         */
        fun createTempJarWithMeta(): File {
            val tempFile = File.createTempFile("test-driver-", ".jar")
            tempFile.deleteOnExit()

            JarOutputStream(tempFile.outputStream()).use { jos ->
                jos.putNextEntry(JarEntry("driver-meta.json"))
                val metaJson = """
                    {
                        "name": "TestDriver",
                        "version": "1.0.0",
                        "mainClass": "dev.kokoroidkt.core.driver.TestDriver",
                        "authors": ["TestAuthor"],
                        "description": "A test driver for DriverLoader",
                        "priority": 500
                    }
                """.trimIndent()
                jos.write(metaJson.toByteArray(Charsets.UTF_8))
                jos.closeEntry()
            }

            return tempFile
        }

        /**
         * Creates a temporary empty JAR file (no entries).
         * The file is scheduled for deletion on JVM exit.
         */
        fun createEmptyJar(): File {
            val tempFile = File.createTempFile("empty-", ".jar")
            tempFile.deleteOnExit()
            JarOutputStream(tempFile.outputStream()).use { }
            return tempFile
        }
    }
}

/**
 * A class without a no-arg constructor, used to test error handling
 * when the loaded class cannot be instantiated via reflection.
 */
private class ClassWithNoNoArgConstructor(name: String)
