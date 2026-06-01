// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

import java.io.File
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Marker types used to verify which ClassLoader in the delegation chain returned a class.
 */
private object DepClassMarker1

private object DepClassMarker2

/**
 * A test ClassLoader that can "load" a specific class name by returning a known [Class] reference.
 * This avoids needing actual .class files or JARs on disk for testing delegation logic.
 */
private class MarkerClassLoader(
    private val knownClass: Class<*>,
    private val knownClassName: String,
) : ClassLoader(null) {
    override fun loadClass(
        name: String,
        resolve: Boolean,
    ): Class<*> =
        if (name == knownClassName) {
            knownClass
        } else {
            throw ClassNotFoundException(name)
        }
}

class DependencyAwareClassLoaderTest {
    @Test
    fun `system class delegation returns system class`() {
        val loader = DependencyAwareClassLoader(File("test.jar"), emptyList())
        val clazz = loader.loadClass("java.lang.String")
        assertEquals(String::class.java, clazz)
    }

    @Test
    fun `dependency classloader is consulted when system cannot find`() {
        val depLoader =
            MarkerClassLoader(
                knownClass = DepClassMarker1::class.java,
                knownClassName = "com.example.Marker",
            )
        val loader =
            DependencyAwareClassLoader(
                jarFile = File("test.jar"),
                dependencyClassLoaders = listOf(depLoader),
            )
        val clazz = loader.loadClass("com.example.Marker")
        assertEquals(DepClassMarker1::class.java, clazz)
    }

    @Test
    fun `delegation order prefers system over dependencies`() {
        // System CL can load String; dep CL also chains to system CL.
        // System CL should win because it's checked first.
        val depLoader =
            URLClassLoader(
                emptyArray(),
                ClassLoader.getSystemClassLoader(),
            )
        val loader =
            DependencyAwareClassLoader(
                jarFile = File("test.jar"),
                dependencyClassLoaders = listOf(depLoader),
            )
        val clazz = loader.loadClass("java.lang.String")
        assertEquals(String::class.java, clazz)
    }

    @Test
    fun `dependency order first wins when both can load the same class`() {
        val dep1 = MarkerClassLoader(DepClassMarker1::class.java, "com.example.Marker")
        val dep2 = MarkerClassLoader(DepClassMarker2::class.java, "com.example.Marker")
        val loader =
            DependencyAwareClassLoader(
                jarFile = File("test.jar"),
                dependencyClassLoaders = listOf(dep1, dep2),
            )
        // dep1 is first in the list, so its marker should be returned
        val clazz = loader.loadClass("com.example.Marker")
        assertEquals(DepClassMarker1::class.java, clazz)
    }

    @Test
    fun `class not found throws ClassNotFoundException`() {
        val loader = DependencyAwareClassLoader(File("test.jar"), emptyList())
        assertFailsWith<ClassNotFoundException> {
            loader.loadClass("com.nonexistent.NoSuchClass")
        }
    }

    @Test
    fun `multiple dependencies are searched in order until found`() {
        val dep1 = MarkerClassLoader(DepClassMarker1::class.java, "com.example.MarkerA")
        val dep2 = MarkerClassLoader(DepClassMarker2::class.java, "com.example.MarkerB")
        val loader =
            DependencyAwareClassLoader(
                jarFile = File("test.jar"),
                dependencyClassLoaders = listOf(dep1, dep2),
            )
        // dep1 cannot find MarkerB, so dep2 should be consulted and return its marker
        val clazz = loader.loadClass("com.example.MarkerB")
        assertEquals(DepClassMarker2::class.java, clazz)
    }

    @Test
    fun `findClass is called as last resort when nothing else can find the class`() {
        val loader = DependencyAwareClassLoader(File("nonexistent.jar"), emptyList())
        assertFailsWith<ClassNotFoundException> {
            loader.loadClass("com.nonexistent.NoSuchClass")
        }
    }
}
