package me.simpleHook.data

import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.parcelize.Parcelize


@Parcelize
data class MethodInfo(
    val name: String,
    val parameters: List<String>,
    val returnType: String,
    val isStatic: Boolean
): Parcelable

@Parcelize
data class FieldInfo(
    val name: String,
    val type: String,
    val isStatic: Boolean
):Parcelable

data class ClassInfo(
    val name: String,
    val isInterface: Boolean,
    val methods: List<MethodInfo>,
    val fields: List<FieldInfo>
)


sealed interface Node {
    val name: String
    val isPackage: Boolean get() = this is PackageNode
    val expanded: State<Boolean>
    fun toggleExpansion()
}

data class PackageNode(
    override val name: String,
    val children: SnapshotStateList<Node> = mutableStateListOf(),
    override val expanded: MutableState<Boolean> = mutableStateOf(
        false
    )
) : Node {
    override fun toggleExpansion() {
        expanded.value = !expanded.value
    }
}

data class ClassNode(
    override val name: String,
    val className: String,
    val methods: List<MethodInfo>,
    val fields: List<FieldInfo>,
    val isInterface: Boolean,
    override val expanded: MutableState<Boolean> = mutableStateOf(
        false
    )
) : Node {
    override val isPackage: Boolean get() = false
    override fun toggleExpansion() {
        expanded.value = !expanded.value
    }
}

data class DexUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val rootNode: PackageNode? = null
)