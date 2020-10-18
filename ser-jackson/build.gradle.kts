dependencies {
    val jacksonVersion: String by project

    api(project(":"))
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}
