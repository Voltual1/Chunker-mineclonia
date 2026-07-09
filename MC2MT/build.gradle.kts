plugins {
    id("com.android.library")
}

android {
    namespace = "me.voltual.mc2mt"
    compileSdk = 37

    ndkVersion = System.getenv("JITPACK_NDK_VERSION") 
        ?: (project.findProperty("ndkVersion") as? String) 
        ?: "29.0.14206865"

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-O3", "-fexceptions", "-frtti")
                // 传入必要的链接参数，并确保使用共享 C++ 运行库
                arguments("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"/*, "armeabi-v7a"*/))
        }
    }

    externalNativeBuild {
    cmake {
        path = file("CMakeLists.txt")
        version = "3.22.1"
    }
}

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.0")
    testImplementation("junit:junit:4.13.2")
}