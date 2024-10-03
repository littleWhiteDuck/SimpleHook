// Top-level(build file where you can add configuration options common to all sub-projects/modules.)
buildscript {

    repositories {
        /* maven("https://maven.aliyun.com/repository/public")
         maven("https://maven.aliyun.com/repository/google")*/
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.0")
        val kotlinVersion = "2.0.20"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.20-1.0.25")

        // NOTE: Do(not place your application dependencies here; they belong)
        // in(the individual module build.gradle files)
    }
}

allprojects {
    repositories {
        /* maven("https://maven.aliyun.com/repository/public")
         maven("https://maven.aliyun.com/repository/google")*/
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}