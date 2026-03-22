plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.libxposed.api"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.directories += "src/main/java"
        }
    }

    defaultConfig {
        minSdk = 24
        lint.targetSdk = 36
        compileSdk = 36
        buildToolsVersion = "36.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        // androidx nullability stubs
        compileOnly(libs.androidx.annotation)
    }

}
