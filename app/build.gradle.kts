import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.dsl.ApplicationExtension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.Random
import java.util.TimeZone

fun String.capitalizedCompat(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

val configFile = rootProject.file("sign.properties")
val prop = Properties()
prop.load(FileInputStream(configFile))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val beta = prop.getProperty("beta").toBoolean()
val verName: String = prop.getProperty("verName")
val verCode = run {
    val sdf = SimpleDateFormat("yyMMddHH")
    sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
    sdf.format(Date()).toInt()
}

extensions.configure<ApplicationExtension>("android") {
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
        aidl = true
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
        this.forEach {
            it.buildConfigField(Boolean::class.java.name, "IS_BETA", beta.toString())
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "okhttp3/**",
                "kotlin/**",
                "org/**",
                "**.properties",
                "**.bin",
                "**.json",
                "**VERSION"
            )
        }
    }

    lint {
        disable += "AppCompatResource"
    }

    dependenciesInfo.includeInApk = false

    androidComponents.onVariants { v ->
        val variant = v as com.android.build.api.variant.impl.ApplicationVariantImpl
        variant.outputs.forEach {
            val name = when (variant.flavorName) {
                "root" -> "SimpleHookR"
                else -> "SimpleHook"
            }
            val tempVerName = verName + if (beta) "_beta" else ""
            it.outputFileName.set("$name-${variant.flavorName}-${tempVerName}-${verCode}.apk")
        }
    }

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        androidComponents.onVariants { variant ->
            if (variant.buildType == "release") {
                val flavorName = variant.flavorName.orEmpty()
                val taskName = if (flavorName.isNotEmpty()) {
                    "optimize${flavorName.capitalizedCompat()}ReleaseRes"
                } else {
                    "optimizeReleaseRes"
                }

                val optimizeTask = tasks.register(taskName) {
                    doLast {
                        val androidComponentsExt = project.extensions.findByType(
                            ApplicationAndroidComponentsExtension::class.java
                        )
                            ?: throw GradleException("ApplicationAndroidComponentsExtension not found")

                        val sdkDir = androidComponentsExt.sdkComponents.sdkDirectory.get().asFile
                        val buildToolsVersion = project.extensions.findByType(
                            ApplicationExtension::class.java
                        )?.buildToolsVersion ?: throw GradleException("BuildToolsVersion not found")

                        val aapt2Path = sdkDir.resolve("build-tools/$buildToolsVersion/aapt2")
                        val aapt2File = if (aapt2Path.exists()) {
                            aapt2Path
                        } else {
                            sdkDir.resolve("build-tools/$buildToolsVersion/aapt2.exe")
                        }
                        if (!aapt2File.exists()) {
                            throw GradleException("aapt2 not found at: $aapt2File")
                        }

                        val flavorPath =
                            if (flavorName.isNotEmpty()) "${flavorName}Release/" else "release/"
                        val optimizeDir = if (flavorName.isNotEmpty()) {
                            "optimize${flavorName.capitalizedCompat()}ReleaseResources"
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
                        val result = project.providers.exec {
                            commandLine(
                                aapt2File.absolutePath,
                                "optimize",
                                "--collapse-resource-names",
                                "--enable-sparse-encoding",
                                "-o",
                                optimizedFile.absolutePath,
                                zipFile.absolutePath
                            )
                            isIgnoreExitValue = true
                        }.result.get()

                        val exitCode = result.exitValue
                        if (exitCode == 0 && optimizedFile.exists()) {
                            if (zipFile.delete()) {
                                if (optimizedFile.renameTo(zipFile)) {
                                    logger.lifecycle("Successfully optimized resources: ${zipFile.absolutePath}")
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

                tasks.configureEach {
                    val optimizeReleaseResourcesTaskName = if (flavorName.isNotEmpty()) {
                        "optimize${flavorName.capitalizedCompat()}ReleaseResources"
                    } else {
                        "optimizeReleaseResources"
                    }
                    if (name == optimizeReleaseResourcesTaskName) {
                        finalizedBy(optimizeTask)
                    }
                }
            }
        }
    }

    sourceSets {
        named("main") {
            kotlin.directories += "$rootDir/libs/ezxhelper/src/main/java"
        }
    }

}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(projects.libs.arsclibWrapper)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(projects.loader.hookapi)
    compileOnly(libs.xposed.api)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.easyfloat)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.room.paging)

    implementation(libs.glide)
    ksp(libs.glide.ksp)

    implementation(libs.libsu.core)
    implementation(libs.libsu.io)

    // rikka
    implementation(files("libs/simplemenu-preference-release.aar"))

    implementation(libs.drakeet.about)
    implementation(libs.drakeet.multitype)


    // webdav, 0.9版修改了exists函数实现，响应头判断会出问题
    implementation(libs.sardine.android) {
        exclude(group = "xpp3", module = "xpp3")
    }


    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.dexlib2)
    implementation(libs.sora.editor)
    implementation(libs.apksig)

    debugImplementation(libs.glance)


    implementation(libs.androidx.lifecycle.runtime.ktx)

}
