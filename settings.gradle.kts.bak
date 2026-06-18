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
        maven { url "https://andob.io/repository/open_source" }
    }
}

rootProject.name = "Vector-Breakthrough"
include(":mcl")
include(":android")
include("cli", "app")