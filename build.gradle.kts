import java.net.URL

plugins {
    kotlin("jvm") version "1.4.10"
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.10"
    id("com.github.ben-manes.versions") version "0.33.0"
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java-library")
        plugin("maven-publish")
        plugin("org.jetbrains.dokka")
        plugin("com.github.ben-manes.versions")
    }
}

allprojects {

    group = "marais"
    version = "0.1.0"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven { url = uri("https://kotlin.bintray.com/ktor") }
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

        java {
            sourceCompatibility = JavaVersion.VERSION_11
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

        dokkaHtml {
            dokkaSourceSets.configureEach {
                skipEmptyPackages.set(true)
                platform.set(org.jetbrains.dokka.Platform.jvm)
                includes.from("src/main/doc/extras.md")
                jdkVersion.set(11)
                sourceLink {
                    remoteUrl.set(URL("https://github.com/Gui-Yom/filet/blob/master/src/main/kotlin"))
                    // For GitHub
                    remoteLineSuffix.set("#L")
                }
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
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.squareup.okio:okio:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
