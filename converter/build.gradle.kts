plugins {
    `java-library`
    id("io.freefair.lombok") version "9.5.0"
}

version = "Based PackConverter-1ae5b8f048de39d6074132f44d58824c305eb295"

sourceSets {
    main {
        resources {
            srcDir("src/main/java/resources")
        }
    }
}

dependencies {
    api(project(":pack-schema-api"))
    compileOnly(project(":bedrock-pack-schema")) // Is provided by pack-schema-api for consumers, but not for us during compile time
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tga:3.9.4") {
    exclude(group = "com.twelvemonkeys.imageio", module = "imageio-core")
}
    implementation("it.unimi.dsi:fastutil:8.5.18")
    api("net.kyori:adventure-api:4.14.0")
    api("net.kyori:adventure-text-serializer-gson:4.14.0")
    api("net.kyori:adventure-text-serializer-legacy:4.14.0")
    api(project(":creative-api"))
    api("team.unnamed:creative-serializer-minecraft:1.14.4") {
        exclude(group = "team.unnamed", module = "creative-api")
    }

    compileOnly("com.google.auto.service:auto-service:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
}

java {
    withSourcesJar()
}
