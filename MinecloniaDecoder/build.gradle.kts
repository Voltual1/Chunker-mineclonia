plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "com.neteasedecryptor.sdk"
version = "1.0.0"


dependencies {
    implementation(libs.kotlinx.io)    
    implementation(kotlin("stdlib"))
}
