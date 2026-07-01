plugins {
    `java-library`
}

version = "1.13.6"

description = "A resource-pack library for Minecraft: Java Edition."

dependencies {
    compileOnlyApi("org.jetbrains:annotations:26.0.2")
    api("net.kyori:adventure-key:4.24.0")
    api("net.kyori:adventure-api:4.24.0")
    api("com.google.code.gson:gson:2.13.1")
    runtimeOnly("net.kyori:adventure-text-serializer-legacy:4.24.0")
}