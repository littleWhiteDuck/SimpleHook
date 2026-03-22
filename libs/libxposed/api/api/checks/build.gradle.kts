plugins {
    java
    alias(libs.plugins.kotlin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    compileOnly(libs.kotlin.stdlib)
}

