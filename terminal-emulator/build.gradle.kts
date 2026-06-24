plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux.emulator"
    compileSdk = 37

    ndkVersion = System.getenv("JITPACK_NDK_VERSION") 
        ?: (project.findProperty("ndkVersion") as? String) 
        ?: "29.0.14206865" 

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            ndkBuild {
                cFlags(
                    "-std=c11", 
                    "-Wall", 
                    "-Wextra", 
                    "-Werror", 
                    "-Os", 
                    "-fno-stack-protector", 
                    "-Wl,--gc-sections"
                )
            }
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),                
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.0")
    testImplementation("junit:junit:4.13.2")
}