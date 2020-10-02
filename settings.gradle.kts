pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version kotlinVersion
        id("com.github.ben-manes.versions") version "0.33.0"
    }
}

rootProject.name = "filet"
include("filet-ktor")
