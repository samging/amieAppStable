rootProject.name = "amieAppStable"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("amieApplication/gradle/libs.versions.toml"))
        }
    }
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://packages.jetbrains.team/maven/p/kpm/public/") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/snapshots") }
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
project(":androidApp").projectDir = file("amieApplication/androidApp")

include(":desktopApp")
project(":desktopApp").projectDir = file("amieApplication/desktopApp")

include(":shared")
project(":shared").projectDir = file("amieApplication/shared")
