import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.Random
import java.util.TimeZone

val configFile = rootProject.file("sign.properties")
val prop = Properties()
prop.load(FileInputStream(configFile))

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("org.jetbrains.kotlin.android")
}

val beta = prop.getProperty("beta").toBoolean()
val verName: String = prop.getProperty("verName")

android {
    val verCode = run {
        val sdf = SimpleDateFormat("yyMMddHH")
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        sdf.format(Date()).toInt()
    }
    signingConfigs {
        create("keyStore") {
            keyAlias = prop.getProperty("alias")
            keyPassword = prop.getProperty("keyPassword")
            storeFile = File(prop.getProperty("file"))
            storePassword = prop.getProperty("password")
            enableV3Signing = true
        }
    }
    compileSdk = 36
    defaultConfig {

        @Suppress("UnstableApiUsage")
        androidResources.localeFilters += setOf("en", "zh-rCN", "zh-rTW")
        applicationId = "me.simpleHook"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
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
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isDefault = true
            signingConfig = signConfig
            versionNameSuffix = Random().nextInt(1000).toString()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_17
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    namespace = "me.simpleHook"
    productFlavors {
        create("root") {
            isDefault = true
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

    packagingOptions.resources.excludes += setOf(
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**.json",
        "**VERSION"
    )

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

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        // 获取所有release变体
        androidComponents.onVariants { variant ->
            if (variant.buildType == "release") {
                val flavorName = variant.flavorName!!
                val taskName = if (flavorName.isNotEmpty()) {
                    "optimize${flavorName.capitalized()}ReleaseRes"
                } else {
                    "optimizeReleaseRes"
                }

                val optimizeTask = tasks.register<OptimizeReleaseResTask>(taskName) {
                    this.flavor.set(flavorName)
                }

                tasks.configureEach {
                    if (name == taskName) {
                        finalizedBy(optimizeTask)
                    }
                }
            }
        }
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.6")
    implementation("androidx.documentfile:documentfile:1.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // room
    val roomVersion = "2.8.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // xposed
    compileOnly("de.robv.android.xposed:api:82")
    //compileOnly("de.robv.android.xposed:api:82:sources")
    compileOnly("io.github.libxposed:api:100")
    implementation("io.github.libxposed:service:100-1.0.0")
//    implementation("com.github.kyuubiran:EzXHelper:1.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    val navVersion = "2.9.4"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    //悬浮窗
    implementation("com.github.princekin-f:EasyFloat:2.0.3")

    //json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.google.code.gson:gson:2.13.2")

    //splashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    //paging3
    val pagingVersion = "3.3.6"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.room:room-paging:$roomVersion")

    //glide
    val glideVersion = "5.0.4"
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

    implementation("androidx.work:work-runtime-ktx:2.10.4")

    // webdav, 0.9版修改了exists函数实现，响应头判断会出问题
    implementation("com.github.thegrizzlylabs:sardine-android:0.8")
    //leakcanary
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    //glacne
    debugImplementation("com.guolindev.glance:glance:1.1.0")

}



abstract class OptimizeReleaseResTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    // 接收flavor名称参数
    @get:Input
    abstract val flavor: Property<String>

    @TaskAction
    fun optimize() {
        val flavorName = flavor.get()
        val androidComponents =
            project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
                ?: throw GradleException("ApplicationAndroidComponentsExtension not found")

        val sdkDir = androidComponents.sdkComponents.sdkDirectory.get().asFile

        val buildToolsVersion = project.extensions.findByType(AppExtension::class.java)
            ?.buildToolsVersion
            ?: throw GradleException("BuildToolsVersion not found")

        val aapt2File = sdkDir.resolve("build-tools/$buildToolsVersion/aapt2")
        if (!aapt2File.exists()) {
            throw GradleException("aapt2 not found at: $aapt2File")
        }

        // 根据flavor构建资源文件路径
        val flavorPath = if (flavorName.isNotEmpty()) "${flavorName}Release/" else "release/"
        val optimizeDir = if (flavorName.isNotEmpty()) {
            "optimize${flavorName.capitalized()}ReleaseResources"
        } else {
            "optimizeReleaseResources"
        }
        val resourceFileName = if (flavorName.isNotEmpty()) {
            "resources-${flavorName}-release-optimize.ap_"
        } else {
            "resources-release-optimize.ap_"
        }

        val zipFile = project.layout.buildDirectory
            .file("intermediates/optimized_processed_res/$flavorPath$optimizeDir/$resourceFileName")
            .get().asFile

        if (!zipFile.exists()) {
            throw GradleException("Resource file not found: ${zipFile.absolutePath}")
        }

        val optimizedFile = File("${zipFile.absolutePath}.opt")

        val result = execOperations.exec {
            commandLine(
                aapt2File.absolutePath, "optimize",
                "--collapse-resource-names",
                "--enable-sparse-encoding",
                "-o", optimizedFile.absolutePath,
                zipFile.absolutePath
            )
            isIgnoreExitValue = true
        }

        val exitCode = result.exitValue
        if (exitCode == 0 && optimizedFile.exists()) {
            if (zipFile.delete()) {
                if (optimizedFile.renameTo(zipFile)) {
                    logger.lifecycle("✅ Successfully optimized resources: ${zipFile.absolutePath}")
                } else {
                    throw GradleException("Failed to rename optimized file to original name")
                }
            } else {
                throw GradleException("Failed to delete original resource file")
            }
        } else {
            throw GradleException("aapt2 optimization failed with exit code $exitCode")
        }
    }
}
