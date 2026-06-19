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
    }
}

rootProject.name = "Vector-Breakthrough"
include(":mcl")
include(":android")
include(":terminal-emulator")
include("cli", "app")