dependencies {
    val coroutinesVersion: String by project
    val ktorVersion: String by project

    api(project(":"))
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("io.ktor:ktor-network:$ktorVersion")
}
