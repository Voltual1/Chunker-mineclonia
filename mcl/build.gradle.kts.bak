plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

version = "1.1"

kotlin {
    jvm()
        
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib")) 
                implementation(project(":cli"))
            }
        }
       
        val jvmMain by getting {
            dependencies {
                implementation("org.xerial:sqlite-jdbc:3.45.1.0") 
            }
        }
    }
}