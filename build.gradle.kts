// Top-level(build file where you can add configuration options common to all sub-projects/modules.)
buildscript {
    extra["kotlin_version"] = "1.5.30"

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.1")
        val kotlinVersion = "1.8.10"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        // NOTE: Do(not place your application dependencies here; they belong)
        // in(the individual module build.gradle files)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}