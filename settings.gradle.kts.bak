// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    
    repositories {
    
        google()
        
        mavenCentral()
        
        maven { url = uri("https://jitpack.io") }       
        
        maven { url = uri("https://andob.io/repository/open_source") } 
              
        // Geyser, Floodgate, Cumulus etc.
        maven("https://repo.opencollab.dev/main")
        maven("https://repo.opencollab.dev/maven-snapshots")

        // creative
        maven("https://repo.nexomc.com/releases/")

    }
}

rootProject.name = "Vector-Breakthrough"
include(":mcl")
include(":android")
include(":terminal-emulator")
include(":terminal-view")
include("cli", "app")

include(":converter")
include(":MinecloniaDecoder")
include(":MC2MT")
include(":creative-api")

include(":pack-schema-api")
include(":bedrock-pack-schema")
include(":schema-generator")

project(":pack-schema-api").projectDir = file("pack-schema/api")
project(":creative-api").projectDir = file("creative-api/")
project(":bedrock-pack-schema").projectDir = file("pack-schema/bedrock")
project(":schema-generator").projectDir = file("pack-schema/generator")