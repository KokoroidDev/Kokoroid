import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin


plugins {
    alias(libs.plugins.ktlint)

    id("com.gradleup.nmcp.aggregation").version("1.4.4")
    id("org.jetbrains.dokka") version "2.2.0"
}

repositories {
    // Required to download KtLint
    mavenCentral()
}
ktlint {
    version = "1.4.0"
}

val docVersion = "0.x"

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)

        // Output directory for additional files
        // Use this block instead of the standard when you
        // want to change the output directory and include extra files
        outputDirectory.set(rootDir.resolve("docs/api/$docVersion"))

        // Use fileTree to add multiple files
        includes.from(
            fileTree("docs") {
                include("**/*.md")
            },
        )
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:2.2.0")
    }
}

dependencies {
    dokka(project(":core"))
    dokka(project(":core-api"))
    dokka(project(":adapter-api"))
    dokka(project(":driver-api"))
    dokka(project(":plugin-api"))
    dokka(project(":transport-api"))
    nmcpAggregation(project(":core"))
    nmcpAggregation(project(":core-api"))
    nmcpAggregation(project(":adapter-api"))
    nmcpAggregation(project(":driver-api"))
    nmcpAggregation(project(":plugin-api"))
    nmcpAggregation(project(":transport-api"))
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin:2.2.0")
}

tasks.dokkaHtml {
    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = docVersion
        versionsOrdering = listOf(project.version.toString())
        renderVersionsNavigationOnAllPages = true
    }
}

allprojects {
    group = "dev.kokoroidkt"
    version = findProperty("version")?.toString()
        ?: System.getenv("VERSION")
        ?: "undefined"
}

nmcpAggregation {
    centralPortal {
        username = providers.environmentVariable("MAVEN_USERNAME").orNull
        password = providers.environmentVariable("MAVEN_PASSWORD").orNull

        // optional: publish manually from the portal
        publishingType = "USER_MANAGED"

        // optional: configure the name of your publication in the portal UI
        publicationName = "kokoroid:$version"
    }
}
