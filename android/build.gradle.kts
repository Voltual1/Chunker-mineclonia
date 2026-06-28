// [file name]: build.gradle.kts
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room3)    
}

android {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    namespace = "me.voltual.vb"
    compileSdk = 37

    base {
        archivesName.set("Vector-Breakthrough")
    }

    defaultConfig {
        applicationId = "me.voltual.vb"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "2.1"
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"AGPLv3\"")
    }

    androidResources {
        localeFilters += "zh"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: keystoreProperties.getProperty("storeFile") ?: "debug.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
            keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
            keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/INDEX.LIST")
            excludes.add("/META-INF/DEPENDENCIES")
            excludes.add("/google/protobuf/**")
            excludes.add("/src/google/protobuf/**")
            excludes.add("/java/core/java_features_proto-descriptor-set.proto.bin")
            excludes.add("/META-INF/LICENSE*")
            excludes.add("/META-INF/*.txt")
            excludes.add("/DebugProbesKt.bin")
            merges.add("/META-INF/services/**")
            excludes.add("/darwin/**")
            excludes.add("/org/sqlite/native/Mac/**")
            excludes.add("/org/sqlite/native/Windows/**")
            excludes.add("/sqlite-jdbc.properties")
            excludes.add("/org/iq80/leveldb/impl/version.txt")
            excludes.add("/kotlin/**")
//            excludes.add("/java/**") 
//这个"/java/"其实是Chunker需要的资源
        }
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.google.material)
    implementation(libs.okhttp)
    
    implementation(libs.room3.runtime)
    
    implementation(libs.ftpserver.core)
    implementation(libs.ftpserver.api)
    
    implementation(platform(libs.compose.bom))  
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.navigation3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation(libs.compose.navigation3.ui)
    implementation(libs.markdown)

    implementation(libs.kotlinx.coroutines.android)
    
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(libs.filekit.dialogs.compose)    
    implementation(libs.simple.storage)
    implementation(libs.simple.storage.compose)
    implementation(libs.kotlinx.io)    
    
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.tink)
    implementation(libs.tink.android)
    implementation(libs.datastore.core)

    implementation(libs.koin.android.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.startup)
    ksp(libs.room3.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.io)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    
    implementation("ro.andob.androidawt:androidawt:1.0.4")
    
    implementation("androidx.work:work-multiprocess:2.11.0")

    implementation(libs.kotlinx.datetime)
    
    // Add WorkManager dependency
    implementation(libs.work.runtime)
    
    implementation(project(":cli"))
    implementation(project(":mcl"))
    implementation(project(":terminal-emulator"))
    implementation(project(":terminal-view"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}