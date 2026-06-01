// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import dev.kokoroidkt.core.loader.preloader.AdapterPreloader
import dev.kokoroidkt.core.loader.preloader.DriverPreloader
import dev.kokoroidkt.core.loader.preloader.PluginPreloader
import dev.kokoroidkt.core.logger.getLogger
import dev.kokoroidkt.core.runtime.KokoroidLauncher
import dev.kokoroidkt.core.utils.KokoroidVersion
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

class KokoroidBootstrap :
    CliktCommand(
        name = "Kokoroid",
    ) {
    val isPrintVersion by option("-v", "--version", help = "Print kokoroid version.").flag()
    val isDebug by option("-d", "--debug", help = "Print kokoroid version.").flag()
    val isValidationOnly by option(
        "-V",
        "--validation-only",
        help = "Only validate configuration & extension avalliable, do not start Kokoroid.",
    ).flag()

    val withDrivers by option(
        "--with-driver",
        help = "Add drivers to Kokoroid.",
    ).path().multiple()

    val withAdapters by option(
        "--with-adapter",
        help = "Add adapters to Kokoroid.",
    ).path().multiple()

    val withPlugins by option(
        "--with-plugin",
        help = "Add plugins to Kokoroid.",
    ).path().multiple()

    val withAdapterPath by option(
        "--with-adapter-path",
        help = "Add adapter path to Kokoroid.",
    ).path()

    val withDriverPath by option(
        "--with-driver-path",
        help = "Add driver path to Kokoroid.",
    ).path()

    val withPluginPath by option(
        "--with-plugin-path",
        help = "Add plugin path to Kokoroid.",
    ).path()

    val doMigration by option(
        "-m",
        "--migration",
        help = "Run database migration.",
    ).flag()

    val logger = getLogger("KokoroidBootstrap")

    override fun run() {
        if (isPrintVersion) {
            println("Kokoroid Version ${KokoroidVersion.version} (Build #${KokoroidVersion.gitHash})")
            return
        }

        var isConfirm = false
        if (doMigration) {
            logger.warn {
                "WARNING: You enabled migration flag, it will execute database migration, " +
                    "please BACKUP your database before proceeding."
            }
            println("Confirm? [Y]es/[N]o")
            val confirm = readlnOrNull()
            if (confirm == null) {
                isConfirm = false
                logger.warn { "stdin not exist, set NO automatically" }
            } else if (confirm.lowercase().startsWith("y")) {
                isConfirm = true
                logger.warn { "Kokoroid database will be automatically migrated" }
            } else {
                isConfirm = false
                logger.warn { "Migration option has been set to false" }
            }
        }

        val pluginPreloader = PluginPreloader()
        val driverPreloader = DriverPreloader()
        val adapterPreloader = AdapterPreloader()
        withPlugins.forEach {
            pluginPreloader.addJar(it)
        }
        withDrivers.forEach {
            driverPreloader.addJar(it)
        }
        withAdapters.forEach {
            adapterPreloader.addJar(it)
        }

        withPluginPath?.let {
            it.walk().filter { paths -> paths.isRegularFile() && paths.extension == "jar" }.forEach { jar ->
                pluginPreloader.addJar(jar)
            }
        }

        withDriverPath?.let {
            it.walk().filter { paths -> paths.isRegularFile() && paths.extension == "jar" }.forEach { jar ->
                driverPreloader.addJar(jar)
            }
        }
        withAdapterPath?.let {
            it.walk().filter { paths -> paths.isRegularFile() && paths.extension == "jar" }.forEach { jar ->
                adapterPreloader.addJar(jar)
            }
        }

        KokoroidLauncher(
            pluginPreloader = pluginPreloader,
            adapterPreloader = adapterPreloader,
            driverPreloader = driverPreloader,
        ).launch(isValidationOnly, isDebug, doMigration && isConfirm)
    }
}

fun main(args: Array<String>) {
    KokoroidBootstrap().main(args)
}
