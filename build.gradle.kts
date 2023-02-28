// Top-level(build file where you can add configuration options common to all sub-projects/modules.)
buildscript {
    extra["kotlin_version"] = "1.5.30"

    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        val kotlinVersion = "1.8.10"
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20-Beta")
        classpath(kotlin("serialization", version = kotlinVersion))
        val nav_version = "2.5.3"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
        // NOTE: Do(not place your application dependencies here; they belong)
        // in(the individual module build.gradle files)
    }
}

allprojects {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}