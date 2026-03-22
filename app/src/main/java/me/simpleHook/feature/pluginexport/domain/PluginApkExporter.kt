package me.simpleHook.feature.pluginexport.domain

import android.content.Context
import com.android.apksig.ApkSigner
import com.wind.meditor.core.ManifestEditor
import com.wind.meditor.property.AttributeItem
import com.wind.meditor.property.ModificationProperty
import com.wind.meditor.utils.NodeValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.simpleHook.data.local.db.entity.AppConfig
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class PluginExportRequest(
    val pluginPackageName: String,
    val pluginAppName: String,
    val configs: List<AppConfig>
)

class PluginApkExporter(private val context: Context) {

    suspend fun export(request: PluginExportRequest): File = withContext(Dispatchers.IO) {
        require(request.pluginAppName.isNotBlank()) { "plugin app name is empty" }
        require(isValidPackageName(request.pluginPackageName)) { "plugin package name is invalid" }
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
        val signedApk = File(exportDir, buildSignedFileName(request.pluginPackageName))

        copyAssetToFile(TEMPLATE_ASSET_NAME, templateApk)
        rebuildTemplateApk(templateApk, unsignedApk, request)
        signApk(unsignedApk, signedApk)

        unsignedApk.delete()
        templateApk.delete()
        signedApk
    }

    private fun rebuildTemplateApk(
        templateApk: File,
        outputApk: File,
        request: PluginExportRequest
    ) {
        val property = ModificationProperty()
            .addManifestAttribute(
                AttributeItem(NodeValue.Manifest.PACKAGE, request.pluginPackageName)
                    .setNamespace(null)
            )
            .addApplicationAttribute(
                AttributeItem(NodeValue.Application.LABEL, request.pluginAppName)
            )
        val configAssetBytes = Json.encodeToString(normalizeConfigs(request.configs))
            .toByteArray(Charsets.UTF_8)

        ZipFile(templateApk).use { zipFile ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOutputStream ->
                val entries = zipFile.entries()
                var hasWrittenConfigs = false
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name
                    if (isSignatureEntry(entryName)) {
                        continue
                    }
                    when {
                        entryName == ANDROID_MANIFEST_FILE_NAME -> {
                            zipOutputStream.putNextEntry(ZipEntry(entryName))
                            zipFile.getInputStream(entry).use { inputStream ->
                                ManifestEditor(inputStream, zipOutputStream, property).processManifest()
                            }
                            zipOutputStream.closeEntry()
                        }

                        entryName == CONFIG_ASSET_FILE_NAME -> {
                            zipOutputStream.putNextEntry(ZipEntry(entryName))
                            zipOutputStream.write(configAssetBytes)
                            zipOutputStream.closeEntry()
                            hasWrittenConfigs = true
                        }

                        entry.isDirectory -> {
                            zipOutputStream.putNextEntry(copyZipEntry(entry))
                            zipOutputStream.closeEntry()
                        }

                        else -> {
                            zipOutputStream.putNextEntry(copyZipEntry(entry))
                            zipFile.getInputStream(entry).use { inputStream ->
                                inputStream.copyTo(zipOutputStream)
                            }
                            zipOutputStream.closeEntry()
                        }
                    }
                }
                if (!hasWrittenConfigs) {
                    zipOutputStream.putNextEntry(ZipEntry(CONFIG_ASSET_FILE_NAME))
                    zipOutputStream.write(configAssetBytes)
                    zipOutputStream.closeEntry()
                }
            }
        }
    }

    private fun signApk(unsignedApk: File, signedApk: File) {
        val keyStore = loadSigningKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray()) as? PrivateKey
            ?: error("private key not found")
        val certificates = keyStore.getCertificateChain(KEY_ALIAS)
            ?.map { certificate -> certificate as X509Certificate }
            .orEmpty()
        require(certificates.isNotEmpty()) { "certificate chain is empty" }

        val signerConfig = ApkSigner.SignerConfig.Builder(
            SIGNER_NAME,
            privateKey,
            certificates
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

    private fun loadSigningKeyStore(): KeyStore {
        val keyStoreBytes = context.assets.open(KEYSTORE_ASSET_NAME).use { inputStream ->
            inputStream.readBytes()
        }
        val lastErrors = mutableListOf<Throwable>()
        KEYSTORE_TYPES.forEach { storeType ->
            runCatching {
                KeyStore.getInstance(storeType).apply {
                    load(keyStoreBytes.inputStream(), KEYSTORE_PASSWORD.toCharArray())
                }
            }.onSuccess { keyStore ->
                return keyStore
            }.onFailure { error ->
                lastErrors += error
            }
        }
        throw IllegalStateException(
            "unsupported keystore types: ${KEYSTORE_TYPES.joinToString()}",
            lastErrors.lastOrNull()
        )
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

    private fun copyZipEntry(entry: ZipEntry): ZipEntry {
        return ZipEntry(entry.name).also { newEntry ->
            newEntry.comment = entry.comment
            newEntry.setExtra(entry.extra)
            newEntry.time = entry.time
            newEntry.method = entry.method
            if (entry.method == ZipEntry.STORED) {
                newEntry.size = entry.size
                newEntry.compressedSize = entry.compressedSize
                newEntry.crc = entry.crc
            }
        }
    }

    private fun isSignatureEntry(entryName: String): Boolean {
        return entryName.startsWith("META-INF/") && (
            entryName.endsWith(".SF", ignoreCase = true) ||
                entryName.endsWith(".RSA", ignoreCase = true) ||
                entryName.endsWith(".DSA", ignoreCase = true) ||
                entryName.endsWith(".EC", ignoreCase = true)
            )
    }

    private fun buildSignedFileName(packageName: String): String {
        return "plugin-${packageName}.apk"
    }

    companion object {
        const val DEFAULT_PLUGIN_APP_NAME = "SimpleHookPlugin"
        const val DEFAULT_PLUGIN_PACKAGE_NAME = "me.simplehook.plugin.generated"

        private const val EXPORT_DIRECTORY_NAME = "plugin-export"
        private const val TEMPLATE_ASSET_NAME = "plugin_template.apk"
        private const val KEYSTORE_ASSET_NAME = "plugin_default_sign.p12"
        private const val TEMPLATE_COPY_FILE_NAME = "template.apk"
        private const val UNSIGNED_FILE_NAME = "plugin-unsigned.apk"
        private const val CONFIG_ASSET_FILE_NAME = "assets/configs.xml"
        private const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"
        private const val KEYSTORE_PASSWORD = "SimpleHookPluginExport2026"
        private const val KEY_ALIAS = "simplehookplugin_export"
        private const val SIGNER_NAME = "SimpleHookPluginExport"
        private val KEYSTORE_TYPES = listOf("PKCS12")

        fun isValidPackageName(packageName: String): Boolean {
            val trimmedPackageName = packageName.trim()
            return trimmedPackageName.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$"))
        }
    }
}
