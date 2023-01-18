import java.text.SimpleDateFormat
import java.util.*


plugins {
    id("com.android.application")
    id("kotlin-android")
    id("c")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    val verCode = run {
        val sdf = SimpleDateFormat("yyMMddHH")
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        sdf.format(Date()).toInt()
    }
    val verName = "1.2.8"

    signingConfigs {
        create("keyStore") {
            keyAlias = "littlewhite"
            keyPassword = "littleWhite"
            storeFile = file("D:\\littleWhiteDuck\\sign\\littleWhiteDuck.jks")
            storePassword = "littlesimpleVip"
        }
    }
    compileSdk = 33
    defaultConfig {
        resourceConfigurations += setOf("zh_CN", "en", "zh_TW")
        applicationId = "me.simpleHook"
        minSdk = 22
        targetSdk = 33
        versionCode = verCode
        versionName = verName
        flavorDimensions += "default"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val signConfig = signingConfigs.getByName("keyStore")
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signConfig
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signConfig
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_11)
        targetCompatibility(JavaVersion.VERSION_11)
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "31.0.0"

    productFlavors {
        create("beta") {
            versionName = verName + "_beta"
        }
        create("normal") {
            versionName = verName
        }
    }
    namespace = "me.simpleHook"

    androidComponents.onVariants { v ->
        val variant = v as com.android.build.api.variant.impl.ApplicationVariantImpl
        variant.outputs.forEach {
            it.outputFileName.set("SimpleHook-${verName}-${verCode}.apk")
        }
    }

}


dependencies {

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.0-rc01")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // room
    val room_version = "2.5.0-beta02"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // xposed
    compileOnly("de.robv.android.xposed:api:82")
    //compileOnly("de.robv.android.xposed:api:82:sources")
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    /* def(nav_version = "2.4.2")
     implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
     implementation("androidx.navigation:navigation-ui-ktx:$nav_version")*/

    //悬浮窗
    implementation("com.github.princekin-f:EasyFloat:2.0.3")

    //json
    implementation("com.google.code.gson:gson:2.9.1")

    //splashScreen
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")

    //shell
    implementation("com.github.d4rken:rxshell:3.0.0")

    //paging3
    val pagingVersion = "3.1.1"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.room:room-paging:$room_version")

    //glide
    val glideVersion = "4.14.2"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")
}