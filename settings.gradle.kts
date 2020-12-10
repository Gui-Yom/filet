pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    val kotlinVersion: String by settings
    val dokkaVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
    }
}

rootProject.name = "filet"
include("transport-ktor", "transport-udt", "ser-jackson")
