import org.gradle.api.initialization.resolve.RepositoriesMode

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://jitpack.io")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                google()
            }
            filter {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
            }
        }
        mavenCentral()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "SimpleHook"
include(
    ":app",
    ":loader:hookapi",
    ":libs:ezxhelper",
    ":libs:arsclib-wrapper"
)
