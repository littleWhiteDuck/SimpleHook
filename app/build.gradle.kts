import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.FileInputStream
import com.android.build.api.dsl.ApplicationExtension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone
import javax.inject.Inject

abstract class OptimizeReleaseResources @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aapt2: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val resourceArchive: RegularFileProperty

    @TaskAction
    fun optimize() {
        val archive = resourceArchive.get().asFile
        val executable = aapt2.get().asFile
        if (!executable.exists()) {
            throw GradleException("aapt2 not found at: $executable")
        }
        if (!archive.exists()) {
            throw GradleException("Resource archive not found: ${archive.absolutePath}")
        }

        val optimizedArchive = File("${archive.absolutePath}.opt")
        val result = execOperations.exec {
            commandLine(
                executable.absolutePath,
                "optimize",
                "--collapse-resource-names",
                "--enable-sparse-encoding",
                "-o",
                optimizedArchive.absolutePath,
                archive.absolutePath
            )
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0 || !optimizedArchive.exists()) {
            throw GradleException("aapt2 resource optimization failed with exit code ${result.exitValue}")
        }
        if (!archive.delete() || !optimizedArchive.renameTo(archive)) {
            throw GradleException("Failed to replace resource archive with optimized output")
        }
        logger.lifecycle("Optimized release resources: ${archive.absolutePath}")
    }
}

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("sign.properties")
if (signingPropertiesFile.isFile) {
    FileInputStream(signingPropertiesFile).use(signingProperties::load)
}

fun configurationValue(propertyName: String, environmentName: String): String? =
    signingProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val signingConfig = mapOf(
    "alias" to configurationValue("alias", "SIGNING_ALIAS"),
    "keyPassword" to configurationValue("keyPassword", "SIGNING_KEY_PASSWORD"),
    "file" to configurationValue("file", "SIGNING_STORE_FILE"),
    "password" to configurationValue("password", "SIGNING_STORE_PASSWORD")
)
val hasSigningValues = signingConfig.values.any { it != null }
val hasReleaseSigning = signingConfig.values.all { it != null }

require(!hasSigningValues || hasReleaseSigning) {
    "Release signing is incomplete. Configure alias, keyPassword, file, and password " +
        "in sign.properties or through SIGNING_* environment variables."
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val verName = configurationValue("verName", "VERSION_NAME") ?: "0.0.0-dev"
val verCode = run {
    val sdf = SimpleDateFormat("yyMMddHH")
    sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
    sdf.format(Date()).toInt()
}
val androidSdkDirectory = objects.directoryProperty()

extensions.configure<ApplicationExtension>("android") {
    signingConfigs {
        if (hasReleaseSigning) {
            create("keyStore") {
                keyAlias = signingConfig.getValue("alias")
                keyPassword = signingConfig.getValue("keyPassword")
                storeFile = File(signingConfig.getValue("file"))
                storePassword = signingConfig.getValue("password")
                enableV3Signing = true
            }
        }
    }
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    androidSdkDirectory.set(androidComponents.sdkComponents.sdkDirectory)
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
        val signConfig = signingConfigs.findByName("keyStore")
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signConfig != null) {
                signingConfig = signConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isDefault = true
            if (signConfig != null) {
                signingConfig = signConfig
            }
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
            versionName = verName
            buildConfigField("java.lang.String", "APP_NAME", "\"SimpleHookR\"")
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
            it.outputFileName.set("$name-${variant.flavorName}-${verName}-${verCode}.apk")
        }
    }

    sourceSets {
        named("main") {
            kotlin.directories += "$rootDir/libs/ezxhelper/src/main/java"
        }
    }

}

val optimizeRootReleaseRes = tasks.register<OptimizeReleaseResources>("optimizeRootReleaseRes") {
    group = "build"
    description = "Optimizes root release resources with aapt2."
    aapt2.set(
        androidSdkDirectory.map { sdkDir ->
            sdkDir.file("build-tools/36.0.0/aapt2")
        }
    )
    resourceArchive.set(
        layout.buildDirectory.file(
            "intermediates/optimized_processed_res/rootRelease/" +
                "optimizeRootReleaseResources/resources-root-release-optimize.ap_"
        )
    )
}

tasks.configureEach {
    if (name == "optimizeRootReleaseResources") {
        finalizedBy(optimizeRootReleaseRes)
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
}
