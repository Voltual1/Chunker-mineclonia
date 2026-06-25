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
        versionCode = 2
        versionName = "2.0"
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"GPLv3\"")
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
        excludes.add("/darwin/**")                            // 移除 Mac 专属的 liblz4-java.dylib 等
        excludes.add("/org/sqlite/native/Mac/**")             // 移除 sqlite-jdbc 带来的 Mac 动态库
        excludes.add("/org/sqlite/native/Windows/**")         // 移除 sqlite-jdbc 带来的 Windows 动态库
        excludes.add("/sqlite-jdbc.properties")               // 移除 sqlite-jdbc 的配置文件
        excludes.add("/org/iq80/leveldb/impl/version.txt")    // 移除 leveldb 的无用文本
        excludes.add("/kotlin/**")
        excludes.add("/java/**") 
    }
}

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // 基础
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.google.material)
    implementation(libs.okhttp)
    
    implementation(libs.room3.runtime)
    
    implementation(libs.ftpserver.core)
    implementation(libs.ftpserver.api)
//    implementation(libs.mina.core)
    
    // Compose
    implementation(platform(libs.compose.bom))  
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.navigation3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation(libs.compose.navigation3.ui)    //MarkDown
    implementation(libs.markdown)

    // 图片与异步
//    implementation(libs.coil.compose)
//    implementation(libs.coil.network.ktor)
    implementation(libs.kotlinx.coroutines.android)
    
    // File
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(libs.filekit.dialogs.compose)    
    implementation(libs.simple.storage)
    implementation(libs.simple.storage.compose)
    implementation(libs.kotlinx.io)    
    
    // 持久化
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.tink)
    implementation(libs.tink.android)
    implementation(libs.datastore.core)

    // Koin 注入
    implementation(libs.koin.android.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.startup)
    ksp(libs.room3.compiler)

    // Ktor 与 序列化
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.io)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    //termux    
    
    implementation("ro.andob.androidawt:androidawt:1.0.4")

    implementation(libs.kotlinx.datetime)
    
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