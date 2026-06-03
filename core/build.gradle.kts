plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "9.3.1"

    alias(libs.plugins.kotlinPluginSerialization)
    application
    id("com.gradleup.nmcp")
    `maven-publish`
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jetbrains.dokka-javadoc")
}

application {
    mainClass.set("dev.kokoroidkt.MainKt")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

val gitCommit = gitCommitShort().get()

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.1.0")

    api(libs.bundles.kotlinxEcosystem)
    api(libs.bundles.logging)
    api(libs.bundles.kotlinxSerialization)
    implementation(libs.bundles.exposedPlatform)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.koin.test)

    implementation(project(":transport-api"))
    implementation(project(":core-api"))
    implementation(project(":plugin-api"))
    implementation(project(":adapter-api"))
    implementation(project(":driver-api"))
    implementation(project(":transport-api"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testSuit)
    // testImplementation()
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Delete>("deleteTestConfigFolder") {
    group = "cleanup"
    description = "Delete test configuration folder for test"
    delete("kokoroid")
}

tasks.test {
    dependsOn(":test-extension-with-deps:jar")
    dependsOn(":test-extension:jar")

    dependsOn(":core:deleteTestConfigFolder")
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "dev.kokoroidkt.core.MainKt",
            "Implementation-Title" to "Kokoroid Core",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "dev.kokoroidkt",
            "Git-Hash" to gitCommit,
            "Add-Opens" to "java.base/java.lang java.base/jdk.internal.loader",
            "Add-Exports" to "java.base/jdk.internal.loader",
            "Enable-Native-Access" to "ALL-UNNAMED",
        )
        archiveFileName.set("kokoroidkt-core-$version-all.jar")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.kokoroidkt.core.MainKt",
            "Implementation-Title" to "Kokoroid Core",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "dev.kokoroidkt",
            "Git-Hash" to gitCommit,
            "Add-Opens" to "java.base/java.lang java.base/jdk.internal.loader",
            "Add-Exports" to "java.base/jdk.internal.loader",
            "Enable-Native-Access" to "ALL-UNNAMED",
        )
    }
    archiveFileName.set("kokoroidkt-core-$version.jar")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    archiveFileName.set("kokoroidkt-core-$version-sources.jar")
    from(sourceSets.main.get().allSource)
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc) // 依赖生成文档的任务
    archiveClassifier.set("javadoc")
    archiveFileName.set("kokoroidkt-core-$version-javadoc.jar")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

tasks.build {
    dependsOn(sourcesJar)
    dependsOn(dokkaJavadocJar)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJavadocJar)
            pom {
                name.set("Kokoroid Core")
                description.set("Core module of Kokoroid framework")
                artifactId = "kokoroidkt-core"
                url.set("https://github.com/kokoroidkt/kokoroid")
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, version 2.1")
                        url.set("https://www.gnu.org/licenses/lgpl-2.1.txt")
                    }
                }
                developers {
                    developer {
                        id.set("kokoroidkt")
                        name.set("Kokoroid Dev Team")
                    }
                    developer {
                        id.set("moran0710")
                        name.set("Moran0710")
                        email.set("moran0710@qq.com")
                    }
                }
                scm {
                    url.set("https://github.com/kokoroidkt/kokoroid.git")
                    connection.set("scm:git:git://github.com/kokoroidkt/kokoroid.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kokoroidkt/kokoroid.git")
                }
            }
        }
    }
}

autoConfigureSigning()
