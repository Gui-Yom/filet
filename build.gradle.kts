import java.net.URL

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java-library")
        plugin("maven-publish")
        plugin("org.jetbrains.dokka")
    }
}

allprojects {

    val filetVersion: String by project

    group = "marais.filet"
    version = filetVersion

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
            withSourcesJar()
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
                includes.from("$projectDir/src/main/doc/extras.md")
                jdkVersion.set(11)
                sourceLink {
                    localDirectory.set(file("$projectDir/src/main/kotlin"))
                    remoteUrl.set(URL("https://github.com/Gui-Yom/filet/blob/master/${project.name.let { if (it == "filet") "." else this }}/src/main/kotlin"))
                    // For GitHub
                    remoteLineSuffix.set("#L")
                }
            }
        }

        val dokkaJar by creating(Jar::class) {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Assembles Kotlin docs with Dokka"
            classifier = "javadoc"
            from(project.tasks.dokkaHtml)
        }

        publishing {
            publications {
                create<MavenPublication>("Filet") {
                    from(project.components["java"])
                    artifact(dokkaJar)
                    pom {
                        name.set("Filet")
                        description.set("Networking made easy")
                        url.set("https://github.com/Gui-Yom/filet")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://github.com/Gui-Yom/filet/blob/master/LICENSE")
                            }
                        }
                        developers {
                            developer {
                                id.set("Gui-Yom")
                                name.set("Guillaume Anthouard")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/Gui-Yom/filet.git")
                            developerConnection.set("scm:git:ssh://github.com/Gui-Yom/filet.git")
                            url.set("https://github.com/Gui-Yom/filet/")
                        }
                    }
                }
            }
            /*
            repositories {
                mavenLocal()
                maven {
                    url = uri("${rootProject.buildDir}/repository")
                }
            }

             */
        }
    }
}

dependencies {
    val ktxCoroutinesVersion: String by project

    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$ktxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$ktxCoroutinesVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    val junitVersion: String by project
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}
