plugins {
    `java-library`
}

version = "1.14.4"

java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    api(project(":creative-api"))
    implementation("net.kyori:adventure-text-serializer-legacy:4.24.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.24.0")
}