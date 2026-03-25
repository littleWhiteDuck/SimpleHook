plugins {
    id("com.android.library")
}

val arsclibDir = rootDir.resolve("third_party/ARSCLib")

android {
    namespace = "me.simpleHook.thirdparty.arsclib"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("main") {
            java.setSrcDirs(listOf(arsclibDir.resolve("src/main/java/com")))
            resources.setSrcDirs(listOf("src/main/resources"))
        }
    }
}
