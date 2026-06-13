plugins {
    kotlin("jvm") version "2.3.21"
}

version = "1.0.0"

repositories {
    mavenCentral() 
}

dependencies {
    implementation(project(":cli"))
    implementation("org.xerial:sqlite-jdbc:3.45.1.0") 
}