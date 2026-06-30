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
        versionCode = 6
        versionName = "3.2"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

    buildTypes.forEach {
        it.matchingFallbacks.add("release")
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
            excludes.add("/assets/PublicSuffixDatabase.list")
            excludes.add("/kotlin/**")
        }
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)
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
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    
    implementation("ro.andob.androidawt:androidawt:1.0.4")
    
    implementation("androidx.work:work-multiprocess:2.11.0")

    implementation(libs.kotlinx.datetime)
    
    implementation(libs.work.runtime)
    
    implementation(project(":cli"))
    implementation(project(":converter"))
    implementation(project(":mcl"))
    implementation(project(":terminal-emulator"))
    implementation(project(":terminal-view"))
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// 针对 :converter 模块的 String.formatted JVM 字节码自动脱糖构建逻辑
project(":converter") {
    afterEvaluate {
        val compileJava = tasks.findByName("compileJava") as? org.gradle.api.tasks.compile.JavaCompile
        compileJava?.let { task ->
            task.doLast {
                val dir = task.destinationDirectory.get().asFile
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile && file.extension == "class") {
                            val bytes = file.readBytes()
                            val reader = org.objectweb.asm.ClassReader(bytes)
                            val writer = org.objectweb.asm.ClassWriter(reader, org.objectweb.asm.ClassWriter.COMPUTE_MAXS)
                            var modified = false
                            val visitor = object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
                                override fun visitMethod(
                                    access: Int,
                                    name: String?,
                                    descriptor: String?,
                                    signature: String?,
                                    exceptions: Array<out String>?
                                ): org.objectweb.asm.MethodVisitor {
                                    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                                    return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, mv) {
                                        override fun visitMethodInsn(
                                            opcode: Int,
                                            owner: String?,
                                            methodName: String?,
                                            methodDesc: String?,
                                            isInterface: Boolean
                                        ) {
                                            if (opcode == org.objectweb.asm.Opcodes.INVOKEVIRTUAL &&
                                                owner == "java/lang/String" &&
                                                methodName == "formatted" &&
                                                methodDesc == "([Ljava/lang/Object;)Ljava/lang/String;"
                                            ) {
                                                modified = true
                                                super.visitMethodInsn(
                                                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                                                    "org/geysermc/pack/converter/util/StringDesugar",
                                                    "formatted",
                                                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                                                    false
                                                )
                                            } else {
                                                super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface)
                                            }
                                        }
                                    }
                                }
                            }
                            reader.accept(visitor, 0)
                            if (modified) {
                                file.writeBytes(writer.toByteArray())
                                logger.lifecycle("Desugared String.formatted in class: ${file.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}