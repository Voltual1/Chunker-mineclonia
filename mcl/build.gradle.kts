plugins {
    kotlin("jvm") version "2.3.21"
}

version = "1.0.0"

dependencies {
    implementation(project(":cli"))
    implementation("org.xerial:sqlite-jdbc:3.45.1.0") 
}

tasks.register<JavaExec>("runConverter") {
    mainClass.set("me.voltual.mcl.MclMain")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("/path/to/input", "/path/to/output") // 替换为实际路径
}