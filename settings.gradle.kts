pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version "$kotlinVersion.2"
        id("com.github.ben-manes.versions") version "0.33.0"
    }
}

rootProject.name = "filet"
include("transport-ktor", "ser-jackson")
