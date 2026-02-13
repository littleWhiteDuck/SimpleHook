package me.simpleHook.feature.dexbrowser.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.data.ClassInfo
import me.simpleHook.data.ClassNode
import me.simpleHook.data.DexUiState
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MethodInfo
import me.simpleHook.data.Node
import me.simpleHook.data.PackageNode
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.io.FileOutputStream


class DexBrowserViewModel(private val application: Application) : ViewModel() {

    private val _uiState = MutableStateFlow(DexUiState())
    val uiState: StateFlow<DexUiState> = _uiState.asStateFlow()

    // 用于控制详情弹窗的状态
    var selectedMethod by mutableStateOf<MethodInfo?>(null)
        private set

    var selectedField by mutableStateOf<FieldInfo?>(null)
        private set

    var selectedClassName by mutableStateOf("")
        private set

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, rootNode = null)

            try {
                val dexFile = withContext(Dispatchers.IO) {
                    val destFile = File(application.cacheDir, "temp_selected.dex")
                    application.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile
                }

                val rootNode = withContext(Dispatchers.IO) {
                    val classes = parseDex(dexFile)
                    buildTree(classes)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, rootNode = rootNode)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: application.getString(R.string.common_unknown_error)
                    )
            }
        }
    }

    // 直接修改 Node 的状态即可，因为它是 observable 的
    fun toggleNodeExpansion(node: Node) {
        node.toggleExpansion()
    }

    fun showMethodDetails(className: String, method: MethodInfo) {
        selectedClassName = className
        selectedMethod = method
        selectedField = null
    }

    fun showFieldDetails(className: String, field: FieldInfo) {
        selectedClassName = className
        selectedField = field
        selectedMethod = null
    }

    fun closeDetails() {
        selectedMethod = null
        selectedField = null
        selectedClassName = ""
    }

    fun buildTree(classes: List<ClassInfo>): PackageNode {
        val root = PackageNode("root")

        for (cls in classes) {
            val parts = cls.name.split(".")
            var current = root
            for ((i, part) in parts.withIndex()) {
                if (i == parts.lastIndex) {
                    // It's a class
                    current.children.add(
                        ClassNode(
                            part,
                            cls.name,
                            cls.methods,
                            cls.fields,
                            isInterface = cls.isInterface
                        )
                    )
                } else {
                    // It's a package
                    val pkg = current.children.find {
                        it is PackageNode && it.name == part
                    } as? PackageNode ?: PackageNode(part).also {
                        current.children.add(it)
                    }
                    current = pkg
                }
            }
        }

        return compressPackages(root)
    }

    fun compressPackages(node: PackageNode): PackageNode {
        val newChildren = mutableStateListOf<Node>()
        for (child in node.children) {
            if (child is PackageNode) {
                val compressedChild = compressPackages(child)
                if (compressedChild.children.size == 1 && compressedChild.children[0] is PackageNode) {
                    val grandChild = compressedChild.children[0] as PackageNode
                    val merged = PackageNode(
                        name = "${compressedChild.name}.${grandChild.name}",
                        children = grandChild.children,
                        expanded = compressedChild.expanded
                    )
                    newChildren.add(compressPackages(merged))
                } else {
                    newChildren.add(compressedChild)
                }
            } else {
                newChildren.add(child)
            }
        }
        return node.copy(children = newChildren)
    }

    fun parseDex(dexFile: File): List<ClassInfo> {
        val dexBackedFile = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault())
        return dexBackedFile.classes.map { classDef ->
            val className = classDef.type.removePrefix("L").removeSuffix(";").replace("/", ".")
            val isInterface = (classDef.accessFlags and 0x200) != 0

            // 解析方法信息
            val methods = classDef.methods.map { method ->
                MethodInfo(
                    name = method.name,
                    parameters = method.parameterTypes,
                    returnType = method.returnType,
                    isStatic = (method.accessFlags and 0x8) != 0
                )
            }

            // 解析字段信息
            val fields = classDef.fields.map { field ->
                FieldInfo(
                    name = field.name,
                    type = field.type,
                    isStatic = (field.accessFlags and 0x8) != 0
                )
            }

            ClassInfo(className, isInterface, methods, fields)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DexBrowserViewModel(application)
            }
        }
    }
}
