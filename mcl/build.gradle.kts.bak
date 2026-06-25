plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)    
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

version = "1.1"

kotlin {
    jvm()
    androidTarget()
    
        android {
        namespace = "me.voltual.pyrolysis.mcl"
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
            }
        }
        
        androidMain {
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