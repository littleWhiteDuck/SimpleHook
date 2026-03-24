package me.simpleHook.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File


@Parcelize
data class MethodInfo(
    val name: String,
    val parameters: List<String>,
    val returnType: String,
    val isStatic: Boolean
) : Parcelable

@Parcelize
data class FieldInfo(
    val name: String,
    val type: String,
    val isStatic: Boolean
) : Parcelable

data class ClassInfo(
    val name: String,
    val isInterface: Boolean,
    val methods: List<MethodInfo>,
    val fields: List<FieldInfo>
)


sealed interface Node {
    val name: String
    val isPackage: Boolean get() = this is PackageNode
    var expanded: Boolean

    fun toggleExpansion() {
        expanded = !expanded
    }
}

class PackageNode(
    override val name: String,
    val children: MutableList<Node> = mutableListOf(),
    override var expanded: Boolean = false
) : Node

class ClassNode(
    override val name: String,
    val className: String,
    val methods: List<MethodInfo>,
    val fields: List<FieldInfo>,
    val isInterface: Boolean,
    override var expanded: Boolean = false
) : Node {
    override val isPackage: Boolean get() = false
}

data class DexUiState(
    val isLoading: Boolean = false,
    val loadingMessageResId: Int? = null,
    val errorResId: Int? = null,
    val errorMessage: String? = null,
    val items: List<DexBrowserListItem> = emptyList(),
    val hasLoadedSource: Boolean = false,
    val dexCandidates: List<DexCandidate> = emptyList(),
    val selectedDexCandidateIds: Set<String> = emptySet(),
    val showDexSelectionDialog: Boolean = false,
    val dexSelectionSource: String? = null
)

data class DexCandidate(
    val id: String,
    val file: File,
    val displayName: String,
    val sourceName: String
)

sealed interface DexBrowserListItem {
    val id: String
    val indentLevel: Int

    data class PackageItem(
        override val id: String,
        override val indentLevel: Int,
        val title: String,
        val fullName: String,
        val expanded: Boolean,
        val node: PackageNode
    ) : DexBrowserListItem

    data class ClassItem(
        override val id: String,
        override val indentLevel: Int,
        val title: String,
        val expanded: Boolean,
        val fieldCount: Int,
        val methodCount: Int,
        val isInterface: Boolean,
        val node: ClassNode
    ) : DexBrowserListItem

    data class SectionItem(
        override val id: String,
        override val indentLevel: Int,
        val title: String
    ) : DexBrowserListItem

    data class FieldItem(
        override val id: String,
        override val indentLevel: Int,
        val className: String,
        val field: FieldInfo
    ) : DexBrowserListItem

    data class MethodItem(
        override val id: String,
        override val indentLevel: Int,
        val className: String,
        val method: MethodInfo
    ) : DexBrowserListItem
}
