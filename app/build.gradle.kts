import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.FileInputStream
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

val keyFile = rootProject.file("sign.properties")
val prop = Properties()
prop.load(FileInputStream(keyFile))

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("org.jetbrains.kotlin.android")
}

val beta = true

android {
    val verCode = run {
        val sdf = SimpleDateFormat("yyMMddHH")
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        sdf.format(Date()).toInt()
    }
    val verName = "1.3.4"
    signingConfigs {
        create("keyStore") {
            keyAlias = prop.getProperty("alias")
            keyPassword = prop.getProperty("keyPassword")
            storeFile = File(prop.getProperty("file"))
            storePassword = prop.getProperty("password")
            enableV3Signing = true
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signConfig
            versionNameSuffix = Random().nextInt(1000).toString()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "33.0.1"
    namespace = "me.simpleHook"
    productFlavors {
        create("root") {
            manifestPlaceholders["PROVIDER"] = "me.simplehook.provider.root"
            manifestPlaceholders["FLAVOR"] = "SimpleHookR"
            versionName = verName + if (beta) "_beta" else ""
            buildConfigField("java.lang.String", "APP_NAME", "\"SimpleHookR\"")
        }
        create("normal") {
            manifestPlaceholders["PROVIDER"] = "me.simplehook.provider.normal"
            manifestPlaceholders["FLAVOR"] = "SimpleHook"
            applicationId = "me.simplehook.normal"
            versionName = verName + if (beta) "_beta" else ""
            buildConfigField("java.lang.String", "APP_NAME", "\"SimpleHook\"")
        }
        create("lite") {
            minSdk = 27
            versionName = verName + if (beta) "_beta" else ""
            applicationId = "me.simplehook.lite"
            manifestPlaceholders["PROVIDER"] = "me.simplehook.provider.lite"
            manifestPlaceholders["FLAVOR"] = "xposedsharedprefs"
            buildConfigField(String::class.java.name, "APP_NAME", "\"SimpleHookL\"")
        }
        this.forEach {
            it.buildConfigField(Boolean::class.java.name, "IS_BETA", beta.toString())
        }
    }

    packagingOptions.resources.excludes += setOf("META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**.json",
        "**VERSION")

    lint {
        disable += "AppCompatResource"
    }

    dependenciesInfo.includeInApk = false

    androidComponents.onVariants { v ->
        val variant = v as com.android.build.api.variant.impl.ApplicationVariantImpl
        variant.outputs.forEach {
            val name = when (variant.flavorName) {
                "lite" -> "SimpleHookL"
                "root" -> "SimpleHookR"
                else -> "SimpleHook"
            }
            val tempVerName = verName + if (beta) "_beta" else ""
            it.outputFileName.set("$name-${variant.flavorName}-${tempVerName}-${verCode}.apk")
        }
    }

    tasks.matching {
        it.name.contains("optimize(.*)ReleaseRes".toRegex())
    }.configureEach {
        notCompatibleWithConfigurationCache("optimizeReleaseRes tasks haven't support CC.")
        val flavor = name.removeSurrounding("optimize", "ReleaseResources").toLowerCaseAsciiOnly()
        doLast {
            val aapt2 = File(androidComponents.sdkComponents.sdkDirectory.get().asFile,
                "build-tools/${project.android.buildToolsVersion}/aapt2")
            val zip = Paths.get(buildDir.path,
                "intermediates",
                "optimized_processed_res",
                "${flavor}Release",
                "resources-${flavor}-release-optimize.ap_")
            val optimized = File("${zip}.opt")
            val cmd = exec {
                commandLine(aapt2, "optimize", "--collapse-resource-names", "-o", optimized, zip)
                isIgnoreExitValue = false
            }
            if (cmd.exitValue == 0) {
                delete(zip)
                optimized.renameTo(zip.toFile())
            }
        }
    }

}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // room
    val room_version = "2.5.0-beta02"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // xposed
    compileOnly("de.robv.android.xposed:api:82")
    //compileOnly("de.robv.android.xposed:api:82:sources")
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    val nav_version = "2.5.3"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    //悬浮窗
    implementation("com.github.princekin-f:EasyFloat:2.0.3")

    //json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.google.code.gson:gson:2.9.1")

    //splashScreen
    implementation("androidx.core:core-splashscreen:1.0.0")

    //paging3
    val pagingVersion = "3.1.1"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.room:room-paging:$room_version")

    //glide
    val glideVersion = "4.15.1"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    ksp("com.github.bumptech.glide:ksp:$glideVersion")

    val libsuVersion = "5.0.4"
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")
    implementation("com.github.topjohnwu.libsu:io:${libsuVersion}")

    // rikka
    implementation(files("libs/simplemenu-preference-release.aar"))

    //darkeet
    implementation("com.drakeet.about:about:2.5.2")
    implementation("com.drakeet.multitype:multitype:4.3.0")

    //workmanager
    val work_version = "2.8.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")

    //
    implementation("com.github.thegrizzlylabs:sardine-android:0.8")


    //leakcanary
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    //glacne
    debugImplementation("com.guolindev.glance:glance:1.1.0")

}