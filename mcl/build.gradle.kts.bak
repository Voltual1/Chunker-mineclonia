import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.android.multiplatform.library)    
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)    
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

version = "1.1"

kotlin {
    jvm()
    
        android {
        namespace = "me.voltual.mcl"
        compileSdk = 37
        minSdk = 24
        
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
        
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib")) 
                implementation(project(":cli"))
                implementation(libs.kotlinx.serialization.json)    
            }
        }
        
        androidMain {
            dependencies {
            }
        }
       
        val jvmMain by getting {
            dependencies {
            }
        }
    }
}