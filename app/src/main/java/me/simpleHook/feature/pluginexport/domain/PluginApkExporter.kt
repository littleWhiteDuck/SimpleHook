package me.simpleHook.feature.pluginexport.domain

import android.content.Context
import android.util.Base64
import com.android.apksig.ApkSigner
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.array.ArrayBag
import com.reandroid.arsc.value.array.ArrayBagItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.simpleHook.data.local.db.entity.AppConfig
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.random.Random

data class PluginExportRequest(
    val pluginPackageName: String,
    val pluginAppName: String,
    val pluginVersionName: String,
    val pluginVersionCode: Int,
    val configs: List<AppConfig>
)

data class PluginExportResult(
    val apkFile: File,
    val isSigned: Boolean,
    val signingErrorMessage: String? = null
)

class PluginApkExporter(private val context: Context) {

    suspend fun export(request: PluginExportRequest): PluginExportResult = withContext(Dispatchers.IO) {
        require(request.pluginAppName.isNotBlank()) { "plugin app name is empty" }
        require(isValidPackageName(request.pluginPackageName)) { "plugin package name is invalid" }
        require(request.pluginVersionName.isNotBlank()) { "plugin version name is empty" }
        require(isValidVersionCode(request.pluginVersionCode)) { "plugin version code is invalid" }
        require(request.configs.isNotEmpty()) { "configs is empty" }

        val exportDir = File(context.cacheDir, EXPORT_DIRECTORY_NAME).apply {
            mkdirs()
            listFiles()?.forEach { child ->
                if (child.isFile) {
                    child.delete()
                }
            }
        }
        val templateApk = File(exportDir, TEMPLATE_COPY_FILE_NAME)
        val unsignedApk = File(exportDir, UNSIGNED_FILE_NAME)
        val signedApk = File(exportDir, buildExportFileName(request, signed = true))
        val unsignedOutputApk = File(exportDir, buildExportFileName(request, signed = false))

        copyAssetToFile(TEMPLATE_ASSET_NAME, templateApk)
        try {
            rebuildTemplateApk(templateApk, unsignedApk, request)
            val signingFailure = runCatching {
                signApk(unsignedApk, signedApk)
            }.exceptionOrNull()

            if (signingFailure == null) {
                unsignedApk.delete()
                PluginExportResult(
                    apkFile = signedApk,
                    isSigned = true
                )
            } else {
                signedApk.delete()
                PluginExportResult(
                    apkFile = moveOrCopyFile(unsignedApk, unsignedOutputApk),
                    isSigned = false,
                    signingErrorMessage = signingFailure.toReadableMessage()
                )
            }
        } finally {
            templateApk.delete()
        }
    }

    private fun rebuildTemplateApk(
        templateApk: File,
        outputApk: File,
        request: PluginExportRequest
    ) {
        val configAssetBytes = Json.encodeToString(normalizeConfigs(request.configs))
            .toByteArray(Charsets.UTF_8)
        val scopePackageNames = collectScopePackageNames(request.configs)

        ApkModule.loadApkFile(templateApk).use { apkModule ->
            apkModule.setLoadDefaultFramework(false)
            ensureTemplateStructure(apkModule)
            updatePluginPackageName(apkModule, request.pluginPackageName)
            updateAppName(apkModule, request.pluginAppName)
            updatePluginVersion(apkModule, request.pluginVersionCode, request.pluginVersionName)
            updateScopePackages(apkModule, scopePackageNames)
            replaceConfigAsset(apkModule, configAssetBytes)
            apkModule.refreshManifest()
            apkModule.refreshTable()
            apkModule.writeApk(outputApk)
        }
    }

    private fun ensureTemplateStructure(apkModule: ApkModule) {
        require(apkModule.containsFile(ANDROID_MANIFEST_FILE_NAME)) {
            "plugin template missing AndroidManifest.xml"
        }
        require(apkModule.containsFile(RESOURCES_FILE_NAME)) {
            "plugin template missing resources.arsc"
        }
        require(apkModule.containsFile(CONFIG_ASSET_FILE_NAME)) {
            "plugin template missing assets/configs.xml"
        }
        require(findEntries(apkModule.tableBlock, APP_NAME_RESOURCE_TYPE, APP_NAME_RESOURCE_NAME).isNotEmpty()) {
            "plugin template missing string/app_name"
        }
        val scopeEntries = findEntries(
            apkModule.tableBlock,
            XPOSED_SCOPE_RESOURCE_TYPE,
            XPOSED_SCOPE_RESOURCE_NAME
        )
        require(scopeEntries.isNotEmpty()) {
            "plugin template missing array/xposed_scope"
        }
        require(scopeEntries.all { ArrayBag.create(it) != null }) {
            "plugin template xposed_scope is not a string-array resource"
        }
    }

    private fun updatePluginPackageName(apkModule: ApkModule, packageName: String) {
        apkModule.androidManifest.setPackageName(packageName)
    }

    private fun updateAppName(apkModule: ApkModule, appName: String) {
        val appNameEntries = findEntries(
            apkModule.tableBlock,
            APP_NAME_RESOURCE_TYPE,
            APP_NAME_RESOURCE_NAME
        )
        require(appNameEntries.isNotEmpty()) {
            "plugin template missing string/app_name"
        }
        appNameEntries.forEach { entry ->
            require(!entry.isComplex) {
                "plugin template app_name is not a string resource"
            }
            entry.setValueAsString(appName)
        }
    }

    private fun updatePluginVersion(
        apkModule: ApkModule,
        versionCode: Int,
        versionName: String
    ) {
        apkModule.androidManifest.setVersionCode(versionCode)
        apkModule.androidManifest.setVersionName(versionName)
    }

    private fun updateScopePackages(apkModule: ApkModule, packageNames: List<String>) {
        val scopeEntries = findEntries(
            apkModule.tableBlock,
            XPOSED_SCOPE_RESOURCE_TYPE,
            XPOSED_SCOPE_RESOURCE_NAME
        )
        require(scopeEntries.isNotEmpty()) {
            "plugin template missing array/xposed_scope"
        }
        scopeEntries.forEach { entry ->
            val arrayBag = ArrayBag.create(entry)
                ?: error("plugin template xposed_scope is not a string-array resource")
            val tableStringPool = entry.packageBlock.tableBlock.tableStringPool
            arrayBag.clear()
            packageNames.forEach { packageName ->
                arrayBag.add(
                    arrayBag.size,
                    ArrayBagItem.string(tableStringPool.getOrCreate(packageName))
                )
            }
        }
    }

    private fun replaceConfigAsset(apkModule: ApkModule, configAssetBytes: ByteArray) {
        apkModule.removeInputSource(CONFIG_ASSET_FILE_NAME)
        apkModule.add(ByteInputSource(configAssetBytes, CONFIG_ASSET_FILE_NAME))
    }

    private fun collectScopePackageNames(configs: List<AppConfig>): List<String> {
        val packageNames = configs
            .asSequence()
            .map { it.packageName.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        require(packageNames.isNotEmpty()) { "plugin scope package list is empty" }
        return packageNames
    }

    private fun findEntries(
        tableBlock: TableBlock,
        typeName: String,
        entryName: String
    ): List<Entry> {
        val entries = mutableListOf<Entry>()
        for (packageBlock in tableBlock.listPackages()) {
            val iterator = packageBlock.getEntries(typeName, entryName)
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.isNull) {
                    entries += entry
                }
            }
        }
        return entries
    }

    private fun signApk(unsignedApk: File, signedApk: File) {
        val signingMaterial = loadSigningMaterial()
        if (signedApk.exists()) {
            signedApk.delete()
        }

        val signerConfig = ApkSigner.SignerConfig.Builder(
            SIGNER_NAME,
            signingMaterial.privateKey,
            signingMaterial.certificates
        ).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .setOtherSignersSignaturesPreserved(false)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }

    private fun loadSigningMaterial(): SigningMaterial {
        val certificate = loadCertificate()
        val privateKey = loadPrivateKey(certificate.publicKey.algorithm)
        return SigningMaterial(
            privateKey = privateKey,
            certificates = listOf(certificate)
        )
    }

    private fun loadPrivateKey(keyAlgorithm: String): PrivateKey {
        val privateKeyBytes = decodePemAsset(SIGNING_PRIVATE_KEY_ASSET_NAME, PRIVATE_KEY_PEM_LABEL)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        return KeyFactory.getInstance(keyAlgorithm).generatePrivate(keySpec)
    }

    private fun loadCertificate(): X509Certificate {
        val certificateBytes = decodePemAsset(
            SIGNING_CERTIFICATE_ASSET_NAME,
            CERTIFICATE_PEM_LABEL
        )
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certificateBytes)) as X509Certificate
    }

    private fun decodePemAsset(assetName: String, sectionName: String): ByteArray {
        val pemText = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val beginMarker = "-----BEGIN $sectionName-----"
        val endMarker = "-----END $sectionName-----"
        val beginIndex = pemText.indexOf(beginMarker)
        val endIndex = pemText.indexOf(endMarker)
        require(beginIndex != -1 && endIndex != -1 && endIndex > beginIndex) {
            "invalid PEM asset: $assetName"
        }
        val body = pemText.substring(beginIndex + beginMarker.length, endIndex)
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString("")
        return Base64.decode(body, Base64.DEFAULT)
    }

    private fun normalizeConfigs(configs: List<AppConfig>): List<AppConfig> {
        return configs.map { appConfig ->
            appConfig.copy(enable = true, id = 0)
        }
    }

    private fun copyAssetToFile(assetName: String, outputFile: File) {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun moveOrCopyFile(source: File, target: File): File {
        if (target.exists()) {
            target.delete()
        }
        if (source.renameTo(target)) {
            return target
        }
        source.copyTo(target, overwrite = true)
        source.delete()
        return target
    }

    private fun Throwable.toReadableMessage(): String {
        return generateSequence(this) { it.cause }
            .mapNotNull { throwable ->
                throwable.message?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throwable.javaClass.simpleName.takeIf { it.isNotBlank() }
            }
            .firstOrNull()
            ?: "unknown signing error"
    }

    private fun buildExportFileName(request: PluginExportRequest, signed: Boolean): String {
        val safeVersionName = sanitizeFileNamePart(request.pluginVersionName)
        val unsignedSuffix = if (signed) "" else "-unsigned"
        return "plugin-${request.pluginPackageName}-v${safeVersionName}-${request.pluginVersionCode}$unsignedSuffix.apk"
    }

    companion object {
        const val DEFAULT_PLUGIN_APP_NAME = "SimpleHookPlugin"
        const val DEFAULT_PLUGIN_PACKAGE_NAME_PREFIX = "simplehook.plugin"
        const val DEFAULT_PLUGIN_VERSION_NAME = "0.1"
        const val DEFAULT_PLUGIN_VERSION_CODE = 1

        private const val EXPORT_DIRECTORY_NAME = "plugin-export"
        private const val TEMPLATE_ASSET_NAME = "plugin_template.apk"
        private const val SIGNING_PRIVATE_KEY_ASSET_NAME = "plugin_default_sign_key.pem"
        private const val SIGNING_CERTIFICATE_ASSET_NAME = "plugin_default_sign_cert.pem"
        private const val TEMPLATE_COPY_FILE_NAME = "template.apk"
        private const val UNSIGNED_FILE_NAME = "plugin-unsigned.apk"
        private const val CONFIG_ASSET_FILE_NAME = "assets/configs.xml"
        private const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"
        private const val RESOURCES_FILE_NAME = "resources.arsc"
        private const val SIGNER_NAME = "SimpleHookPluginExport"
        private const val PRIVATE_KEY_PEM_LABEL = "PRIVATE KEY"
        private const val CERTIFICATE_PEM_LABEL = "CERTIFICATE"
        private const val APP_NAME_RESOURCE_TYPE = "string"
        private const val APP_NAME_RESOURCE_NAME = "app_name"
        private const val XPOSED_SCOPE_RESOURCE_TYPE = "array"
        private const val XPOSED_SCOPE_RESOURCE_NAME = "xposed_scope"
        private const val DEFAULT_APP_NAME_SUFFIX_LENGTH = 4
        private const val DEFAULT_APP_NAME_SUFFIX_CHARS = "0123456789"
        private const val DEFAULT_PACKAGE_SUFFIX_LENGTH = 6
        private const val DEFAULT_PACKAGE_SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val SAFE_FILE_NAME_EXTRA_CHARS = "._-"

        fun generateDefaultAppName(random: Random = Random.Default): String {
            val suffix = buildString(DEFAULT_APP_NAME_SUFFIX_LENGTH) {
                repeat(DEFAULT_APP_NAME_SUFFIX_LENGTH) {
                    append(DEFAULT_APP_NAME_SUFFIX_CHARS[random.nextInt(DEFAULT_APP_NAME_SUFFIX_CHARS.length)])
                }
            }
            return DEFAULT_PLUGIN_APP_NAME + suffix
        }

        fun generateDefaultPackageName(random: Random = Random.Default): String {
            val suffix = buildString(DEFAULT_PACKAGE_SUFFIX_LENGTH) {
                repeat(DEFAULT_PACKAGE_SUFFIX_LENGTH) {
                    append(DEFAULT_PACKAGE_SUFFIX_CHARS[random.nextInt(DEFAULT_PACKAGE_SUFFIX_CHARS.length)])
                }
            }
            return "$DEFAULT_PLUGIN_PACKAGE_NAME_PREFIX.$suffix"
        }

        fun isValidPackageName(packageName: String): Boolean {
            val trimmedPackageName = packageName.trim()
            return trimmedPackageName.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$"))
        }

        fun parseVersionCode(versionCodeText: String): Int? {
            return versionCodeText.trim().toIntOrNull()?.takeIf(::isValidVersionCode)
        }

        fun isValidVersionCode(versionCode: Int): Boolean {
            return versionCode > 0
        }

        private fun sanitizeFileNamePart(value: String): String {
            return value.trim()
                .map { char ->
                    if (char.isLetterOrDigit() || SAFE_FILE_NAME_EXTRA_CHARS.contains(char)) {
                        char
                    } else {
                        '_'
                    }
                }
                .joinToString("")
                .trim('.')
                .ifEmpty { "unknown" }
        }
    }

    private data class SigningMaterial(
        val privateKey: PrivateKey,
        val certificates: List<X509Certificate>
    )
}
