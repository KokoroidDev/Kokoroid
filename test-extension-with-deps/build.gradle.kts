plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    implementation(project(":core-api"))
    implementation(project(":plugin-api"))
    implementation(project(":adapter-api"))
    implementation(project(":driver-api"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveBaseName.set("test-extension-with-deps")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("test-extension-with-deps")
}

tasks.jar {
    doLast {
        copy {
            from(archiveFile)
            into(file("../core/build/resources/test"))
        }
    }
}
