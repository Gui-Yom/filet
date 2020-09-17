plugins {
    kotlin("jvm") version "1.4.10"
    `java-library`
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.33.0"
}

group = "marais"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))


    implementation("com.squareup.okio:okio:2.8.0")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            //javaParameters = true
            //freeCompilerArgs = listOf("-Xemit-jvm-type-annotations")
        }
    }

    withType(JavaCompile::class).configureEach {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }

    jar {
        manifest {
            attributes(
                    "Automatic-Module-Name" to "marais.filet"
            )
        }
    }

    dependencyUpdates {
        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                        reject("Release candidate")
                    }
                }
            }
        }
        checkConstraints = true
        gradleReleaseChannel = "current"
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
