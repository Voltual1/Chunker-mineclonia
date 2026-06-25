plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

version = "1.1"

kotlin {
    jvm()
        android {
        namespace = "me.voltual.mcl"
        compileSdk = 37
        minSdk = 24
        
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
        
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib")) 
                implementation(project(":cli"))
            }
        }
        
                val androidMain by getting {
            dependencies {
            }
        }
       
        val jvmMain by getting {
            dependencies {
                implementation("org.xerial:sqlite-jdbc:3.45.1.0") 
            }
        }
    }
}