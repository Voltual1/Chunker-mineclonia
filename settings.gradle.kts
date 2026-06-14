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
    }
}

rootProject.name = "Vector-Breakthrough"
include(":terminal-view")
include(":terminal-emulator")
include(":mcl")
include(":android")
include("cli", "app")