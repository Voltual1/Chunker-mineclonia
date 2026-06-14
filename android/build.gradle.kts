import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
//    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room3)    
//    alias(libs.plugins.shizuku.refine)
}

android {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    namespace = "me.voltual.vb"
    compileSdk = 37

    // AGP 9.0 新的基础包名设置方式
    base {
        archivesName.set("Vector-Breakthrough")
    }

    defaultConfig {
        applicationId = "me.voltual.vb"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"GPLv3\"")
        // 此处删除了 resourceConfigurations
    }

    // AGP 9.0 替代 resourceConfigurations 的新写法
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
            include("arm64-v8a")
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
            isMinifyEnabled = false
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
                implementation(libs.sqlite.bundled)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.components.resources)    
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.navigation3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation(libs.compose.navigation3.ui)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.adaptive.navigation)
    //MarkDown
    implementation(libs.markdown)

    // 图片与异步
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.kotlinx.coroutines.android)
    
    // FileKit
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(libs.filekit.dialogs.compose)    
    // 存储
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.core)
/*    
    //Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.shizuku.refine)
    compileOnly(libs.shizuku.hidden)
    
    //Libsu
    implementation(libs.libsu.core)
*/
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
    implementation(libs.work.runtime)
    implementation(libs.ktor.io)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    //termux
        implementation(project(":terminal-view"))
    implementation(project(":terminal-emulator"))

    // --- Neo Store 移植依赖 ---
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(project(":cli"))
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