dependencies {
    val ktxCoroutinesVersion: String by project
    val udtVersion: String by project

    api(project(":"))
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$ktxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$ktxCoroutinesVersion")
    implementation("marais:udt-java:$udtVersion")
}
