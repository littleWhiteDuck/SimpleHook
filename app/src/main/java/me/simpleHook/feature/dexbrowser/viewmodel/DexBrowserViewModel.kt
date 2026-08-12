package me.simpleHook.feature.dexbrowser.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.core.utils.ApkExtractionManager
import me.simpleHook.data.ClassInfo
import me.simpleHook.data.ClassNode
import me.simpleHook.data.DexBrowserListItem
import me.simpleHook.data.DexCandidate
import me.simpleHook.data.DexUiState
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MethodInfo
import me.simpleHook.data.Node
import me.simpleHook.data.PackageNode
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedHashSet
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DexBrowserViewModel(private val application: Application) : ViewModel() {

    private class LocalizedDexException(val resId: Int) : IllegalStateException()

    private enum class PickedFileType {
        Dex,
        Apk,
        Apks,
        Unsupported
    }

    private data class PickedFileInfo(
        val displayName: String,
        val type: PickedFileType
    )

    private val _uiState = MutableStateFlow(DexUiState())
    val uiState: StateFlow<DexUiState> = _uiState.asStateFlow()

    private var rootNode: PackageNode? = null

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            startLoading(R.string.dex_browser_loading_processing)
            try {
                val pickedInfo = withContext(Dispatchers.IO) { resolvePickedFileInfo(uri) }
                when (pickedInfo.type) {
                    PickedFileType.Dex -> {
                        val dexFile = withContext(Dispatchers.IO) {
                            clearImportWorkspace()
                            val workspace = createImportWorkspace("dex")
                            copyUriToFile(uri, File(workspace, pickedInfo.displayName))
                        }
                        parseAndPublishDexFiles(listOf(dexFile))
                    }

                    PickedFileType.Apk -> {
                        val candidates = withContext(Dispatchers.IO) {
                            buildDexCandidatesFromApk(uri, pickedInfo.displayName)
                        }
                        showDexSelection(candidates, pickedInfo.displayName)
                    }

                    PickedFileType.Apks -> {
                        val candidates = withContext(Dispatchers.IO) {
                            buildDexCandidatesFromApks(uri, pickedInfo.displayName)
                        }
                        showDexSelection(candidates, pickedInfo.displayName)
                    }

                    PickedFileType.Unsupported -> {
                        throw LocalizedDexException(R.string.dex_browser_picker_unsupported_type)
                    }
                }
            } catch (e: Exception) {
                handleLoadError(e)
            }
        }
    }

    fun onInstalledAppPicked(packageName: String, appName: String) {
        viewModelScope.launch {
            startLoading(R.string.dex_browser_loading_extracting)
            try {
                val candidates = withContext(Dispatchers.IO) {
                    buildDexCandidatesFromInstalledApp(packageName)
                }
                showDexSelection(candidates, appName.ifBlank { packageName })
            } catch (e: Exception) {
                handleLoadError(e)
            }
        }
    }

    fun toggleDexCandidateSelection(candidateId: String) {
        val current = _uiState.value
        val next = current.selectedDexCandidateIds.toMutableSet().apply {
            if (!add(candidateId)) {
                remove(candidateId)
            }
        }
        _uiState.value = current.copy(selectedDexCandidateIds = next)
    }

    fun toggleSelectAllDexCandidates() {
        val current = _uiState.value
        val next = if (current.selectedDexCandidateIds.size == current.dexCandidates.size) {
            emptySet()
        } else {
            current.dexCandidates.mapTo(LinkedHashSet()) { it.id }
        }
        _uiState.value = current.copy(selectedDexCandidateIds = next)
    }

    fun dismissDexSelectionDialog() {
        _uiState.value = _uiState.value.copy(showDexSelectionDialog = false)
    }

    fun confirmDexSelection() {
        val current = _uiState.value
        if (current.selectedDexCandidateIds.isEmpty()) {
            _uiState.value = current.copy(
                errorResId = R.string.dex_browser_picker_no_dex_selected,
                errorMessage = null
            )
            return
        }

        val selectedFiles = current.dexCandidates
            .filter { it.id in current.selectedDexCandidateIds }
            .map { it.file }

        viewModelScope.launch {
            _uiState.value = current.copy(
                isLoading = true,
                loadingMessageResId = R.string.dex_browser_loading_parsing,
                errorResId = null,
                errorMessage = null,
                items = emptyList(),
                hasLoadedSource = false,
                showDexSelectionDialog = false
            )
            try {
                parseAndPublishDexFiles(selectedFiles)
            } catch (e: Exception) {
                handleLoadError(e)
            }
        }
    }

    fun toggleNodeExpansion(node: Node) {
        node.toggleExpansion()
        rootNode?.let { root ->
            _uiState.value = _uiState.value.copy(
                items = buildVisibleItems(root),
                errorResId = null,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        clearImportWorkspace()
        super.onCleared()
    }

    private fun startLoading(messageResId: Int) {
        rootNode = null
        _uiState.value = DexUiState(
            isLoading = true,
            loadingMessageResId = messageResId
        )
    }

    private fun handleLoadError(error: Exception) {
        error.printStackTrace()
        val errorResId = (error as? LocalizedDexException)?.resId
        _uiState.value = DexUiState(
            isLoading = false,
            errorResId = errorResId,
            errorMessage = error.message.takeIf { errorResId == null && !it.isNullOrBlank() }
        )
    }

    private suspend fun parseAndPublishDexFiles(dexFiles: List<File>) {
        val root = withContext(Dispatchers.Default) {
            val classes = parseDexFilesInParallel(dexFiles).distinctBy { it.name }
            buildTree(classes).also(::sortTree)
        }
        rootNode = root
        _uiState.value = DexUiState(
            isLoading = false,
            items = buildVisibleItems(root),
            hasLoadedSource = true
        )
    }

    private suspend fun parseDexFilesInParallel(dexFiles: List<File>): List<ClassInfo> = coroutineScope {
        if (dexFiles.size <= 1) {
            return@coroutineScope dexFiles.flatMap(::parseDex)
        }

        val maxConcurrency = minOf(
            dexFiles.size,
            Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        )
        val semaphore = Semaphore(maxConcurrency)
        dexFiles.map { file ->
            async(Dispatchers.Default) {
                semaphore.withPermit {
                    parseDex(file)
                }
            }
        }.awaitAll().flatten()
    }

    private fun showDexSelection(candidates: List<DexCandidate>, sourceLabel: String) {
        _uiState.value = DexUiState(
            isLoading = false,
            dexCandidates = candidates,
            selectedDexCandidateIds = candidates.mapTo(LinkedHashSet()) { it.id },
            showDexSelectionDialog = true,
            dexSelectionSource = sourceLabel
        )
    }

    private fun resolvePickedFileInfo(uri: Uri): PickedFileInfo {
        val mimeType = application.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
        val fileName = queryDisplayName(uri)?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: throw LocalizedDexException(R.string.dex_browser_picker_missing_name)
        val lowered = fileName.lowercase(Locale.ROOT)

        val type = when {
            lowered.endsWith(".dex") -> PickedFileType.Dex
            lowered.endsWith(".apk") -> PickedFileType.Apk
            lowered.endsWith(".apks") -> PickedFileType.Apks
            mimeType.contains("android.package-archive") -> PickedFileType.Apk
            mimeType.contains("zip") -> PickedFileType.Apks
            else -> PickedFileType.Unsupported
        }
        return PickedFileInfo(fileName, type)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return application.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index == -1 || !cursor.moveToFirst()) {
                null
            } else {
                cursor.getString(index)
            }
        }
    }

    private fun clearImportWorkspace() {
        File(application.cacheDir, WORKSPACE_ROOT).deleteRecursively()
    }

    private fun createImportWorkspace(prefix: String): File {
        val root = File(application.cacheDir, WORKSPACE_ROOT).apply { mkdirs() }
        return File(root, "${prefix}_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    private fun copyUriToFile(uri: Uri, outFile: File): File {
        application.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw LocalizedDexException(R.string.common_unknown_error)
        return outFile
    }

    private fun buildDexCandidatesFromApk(uri: Uri, displayName: String): List<DexCandidate> {
        clearImportWorkspace()
        val workspace = createImportWorkspace("apk")
        val sourceApk = copyUriToFile(uri, File(workspace, displayName))
        val extractedDir = File(workspace, "apk_dex_only").apply { mkdirs() }
        val dexFiles = extractTopLevelFilesByExtension(sourceApk, extractedDir, "dex")
        if (dexFiles.isEmpty()) {
            throw LocalizedDexException(R.string.dex_browser_picker_no_dex_in_apk)
        }
        return dexFiles.map { dexFile ->
            DexCandidate(
                id = "apk:${dexFile.name}",
                file = dexFile,
                displayName = dexFile.name,
                sourceName = displayName
            )
        }
    }

    private fun buildDexCandidatesFromApks(uri: Uri, displayName: String): List<DexCandidate> {
        clearImportWorkspace()
        val workspace = createImportWorkspace("apks")
        val sourceApks = copyUriToFile(uri, File(workspace, displayName))
        val outerDir = File(workspace, "apks_apk_only").apply { mkdirs() }
        val innerApkFiles = extractTopLevelFilesByExtension(sourceApks, outerDir, "apk")
            .sortedBy { it.name }
        if (innerApkFiles.isEmpty()) {
            throw LocalizedDexException(R.string.dex_browser_picker_no_apk_in_apks)
        }
        return collectDexCandidatesFromApkFiles(innerApkFiles, workspace)
    }

    private fun buildDexCandidatesFromInstalledApp(packageName: String): List<DexCandidate> {
        clearImportWorkspace()
        val workspace = createImportWorkspace("installed")
        val extractOutputDir = File(workspace, "apk_from_app").apply { mkdirs() }
        val extractionResult =
            ApkExtractionManager.extractPackage(application, packageName, extractOutputDir)
        val apkFiles = when (extractionResult) {
            is ApkExtractionManager.ExtractionResult.Success -> {
                extractionResult.files
                    .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
                    .sortedBy { it.name }
            }

            is ApkExtractionManager.ExtractionResult.Failure -> {
                throw LocalizedDexException(R.string.dex_browser_picker_extract_app_failed)
            }
        }
        if (apkFiles.isEmpty()) {
            throw LocalizedDexException(R.string.dex_browser_picker_no_apk_in_apks)
        }
        return collectDexCandidatesFromApkFiles(apkFiles, workspace)
    }

    private fun collectDexCandidatesFromApkFiles(
        apkFiles: List<File>,
        workspace: File
    ): List<DexCandidate> {
        val result = mutableListOf<DexCandidate>()
        apkFiles.forEach { apkFile ->
            val apkOutDir = File(workspace, "inner_${apkFile.nameWithoutExtension}").apply { mkdirs() }
            val dexFiles = extractTopLevelFilesByExtension(apkFile, apkOutDir, "dex")
            dexFiles.forEach { dexFile ->
                result += DexCandidate(
                    id = "${apkFile.name}:${dexFile.name}",
                    file = dexFile,
                    displayName = dexFile.name,
                    sourceName = apkFile.name
                )
            }
        }
        if (result.isEmpty()) {
            throw LocalizedDexException(R.string.dex_browser_picker_no_dex_in_apk)
        }
        return result
    }

    private fun extractTopLevelFilesByExtension(
        zipFile: File,
        outputDir: File,
        extension: String
    ): List<File> {
        val result = mutableListOf<File>()
        val expectedExtension = extension.lowercase(Locale.ROOT)
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { input ->
            var entry: ZipEntry? = input.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val isTopLevel = !entryName.contains('/')
                val isTargetFile = !entry.isDirectory &&
                    isTopLevel &&
                    entryName.substringAfterLast('.', "").lowercase(Locale.ROOT) == expectedExtension

                if (isTargetFile) {
                    val outFile = File(outputDir, entryName.substringAfterLast('/'))
                    val outputCanonical = outputDir.canonicalPath + File.separator
                    if (!outFile.canonicalPath.startsWith(outputCanonical)) {
                        throw SecurityException("Invalid zip entry path: ${entry.name}")
                    }
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    result += outFile
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return result
    }

    fun buildTree(classes: List<ClassInfo>): PackageNode {
        val root = PackageNode("root")
        classes.forEach { cls ->
            val parts = cls.name.split(".")
            var current = root
            parts.forEachIndexed { index, part ->
                if (index == parts.lastIndex) {
                    current.children += ClassNode(
                        name = part,
                        className = cls.name,
                        methods = cls.methods,
                        fields = cls.fields,
                        isInterface = cls.isInterface
                    )
                } else {
                    val pkg = current.children.find {
                        it is PackageNode && it.name == part
                    } as? PackageNode ?: PackageNode(part).also(current.children::add)
                    current = pkg
                }
            }
        }
        return compressPackages(root)
    }

    fun compressPackages(node: PackageNode): PackageNode {
        val newChildren = mutableListOf<Node>()
        node.children.forEach { child ->
            if (child is PackageNode) {
                val compressedChild = compressPackages(child)
                if (compressedChild.children.size == 1 && compressedChild.children[0] is PackageNode) {
                    val grandChild = compressedChild.children[0] as PackageNode
                    val merged = PackageNode(
                        name = "${compressedChild.name}.${grandChild.name}",
                        children = grandChild.children,
                        expanded = compressedChild.expanded
                    )
                    newChildren += compressPackages(merged)
                } else {
                    newChildren += compressedChild
                }
            } else {
                newChildren += child
            }
        }
        return PackageNode(name = node.name, children = newChildren, expanded = node.expanded)
    }

    private fun sortTree(node: PackageNode) {
        node.children.forEach { child ->
            if (child is PackageNode) {
                sortTree(child)
            }
        }
        node.children.sortWith(compareBy<Node>({ !it.isPackage }, { it.name }))
    }

    private fun buildVisibleItems(rootNode: PackageNode): List<DexBrowserListItem> {
        val items = mutableListOf<DexBrowserListItem>()
        rootNode.children.forEach { child ->
            addNodeItems(
                node = child,
                parentPackageName = "",
                indentLevel = 0,
                items = items
            )
        }
        return items
    }

    private fun addNodeItems(
        node: Node,
        parentPackageName: String,
        indentLevel: Int,
        items: MutableList<DexBrowserListItem>
    ) {
        when (node) {
            is PackageNode -> {
                val fullPackageName = if (parentPackageName.isEmpty()) {
                    node.name
                } else {
                    "$parentPackageName.${node.name}"
                }
                items += DexBrowserListItem.PackageItem(
                    id = "pkg:$fullPackageName",
                    indentLevel = indentLevel,
                    title = node.name,
                    fullName = fullPackageName,
                    expanded = node.expanded,
                    node = node
                )
                if (node.expanded) {
                    node.children.forEach { child ->
                        addNodeItems(
                            node = child,
                            parentPackageName = fullPackageName,
                            indentLevel = indentLevel + 1,
                            items = items
                        )
                    }
                }
            }

            is ClassNode -> {
                items += DexBrowserListItem.ClassItem(
                    id = "class:${node.className}",
                    indentLevel = indentLevel,
                    title = node.name,
                    expanded = node.expanded,
                    fieldCount = node.fields.size,
                    methodCount = node.methods.size,
                    isInterface = node.isInterface,
                    node = node
                )
                if (node.expanded) {
                    if (node.fields.isNotEmpty()) {
                        items += DexBrowserListItem.SectionItem(
                            id = "section:${node.className}:fields",
                            indentLevel = indentLevel + 1,
                            title = application.getString(R.string.dex_browser_fields)
                        )
                        node.fields.forEach { field ->
                            items += DexBrowserListItem.FieldItem(
                                id = "field:${node.className}:${field.name}:${field.type}:${field.isStatic}",
                                indentLevel = indentLevel + 2,
                                className = node.className,
                                field = field
                            )
                        }
                    }

                    if (node.methods.isNotEmpty()) {
                        items += DexBrowserListItem.SectionItem(
                            id = "section:${node.className}:methods",
                            indentLevel = indentLevel + 1,
                            title = application.getString(R.string.dex_browser_methods)
                        )
                        node.methods.forEach { method ->
                            items += DexBrowserListItem.MethodItem(
                                id = buildMethodItemId(node.className, method),
                                indentLevel = indentLevel + 2,
                                className = node.className,
                                method = method
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildMethodItemId(className: String, method: MethodInfo): String {
        val params = method.parameters.joinToString(",")
        return "method:$className:${method.name}:$params:${method.returnType}:${method.isStatic}"
    }

    fun parseDex(dexFile: File): List<ClassInfo> {
        val dexBackedFile = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault())
        return dexBackedFile.classes.map { classDef ->
            val className = classDef.type.removePrefix("L").removeSuffix(";").replace("/", ".")
            val isInterface = (classDef.accessFlags and 0x200) != 0
            val methods = classDef.methods.map { method ->
                MethodInfo(
                    name = method.name,
                    parameters = method.parameterTypes,
                    returnType = method.returnType,
                    isStatic = (method.accessFlags and 0x8) != 0
                )
            }.sortedBy { it.name }
            val fields = classDef.fields.map { field ->
                FieldInfo(
                    name = field.name,
                    type = field.type,
                    isStatic = (field.accessFlags and 0x8) != 0
                )
            }.sortedBy { it.name }
            ClassInfo(className, isInterface, methods, fields)
        }
    }

    companion object {
        private const val WORKSPACE_ROOT = "dex_import_workspace"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DexBrowserViewModel(application)
            }
        }
    }
}
