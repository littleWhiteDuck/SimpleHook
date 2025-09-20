package me.simpleHook.ui.compose.page

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import me.simpleHook.R
import me.simpleHook.data.ClassNode
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MethodInfo
import me.simpleHook.data.Node
import me.simpleHook.data.PackageNode
import me.simpleHook.ui.compose.component.ClassCircularChar
import me.simpleHook.ui.compose.component.FieldCircularChar
import me.simpleHook.ui.compose.component.InterfaceCircularChar
import me.simpleHook.ui.compose.component.MethodCircularChar
import me.simpleHook.viewmodel.DexBrowserViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexBrowser(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onMethodDone: (className: String, MethodInfo) -> Unit,
    onFieldDone: (className: String, FieldInfo) -> Unit
) {
    val viewModel: DexBrowserViewModel = viewModel(factory = DexBrowserViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.onFilePicked(it)
            }
        }
    )

    // 显示方法详情弹窗
    viewModel.selectedMethod?.let { method ->
        MethodDetailsDialog(
            className = viewModel.selectedClassName,
            method = method,
            onDismiss = { viewModel.closeDetails() },
            onDone = {
                onMethodDone(viewModel.selectedClassName, method)
            },
        )
    }

    // 显示字段详情弹窗
    viewModel.selectedField?.let { field ->
        FieldDetailsDialog(
            className = viewModel.selectedClassName,
            field = field,
            onDismiss = { viewModel.closeDetails() },
            onDone = { onFieldDone(viewModel.selectedClassName, field) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.dex_browser_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back to last page"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            launcher.launch(arrayOf("*/*"))
                        },
                    ) {
                        Text(stringResource(R.string.dex_browser_select_file))
                    }

                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "错误: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.rootNode != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        uiState.rootNode?.children?.let { children ->
                            addItems(children, indent = 0, viewModel = viewModel)
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.dex_browser_select_file_tip),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MethodDetailsDialog(
    className: String,
    method: MethodInfo,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dex_browser_method_detail),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailItem(stringResource(R.string.dex_browser_class_name), className)
                DetailItem(stringResource(R.string.dex_browser_method_name), method.name)
                DetailItem(stringResource(R.string.dex_browser_param_type), method.parameters.joinToString(", "))
                DetailItem(stringResource(R.string.dex_browser_return_type), method.returnType)
                DetailItem(stringResource(R.string.dex_browser_type), if (method.isStatic) stringResource(
                    R.string.dex_browser_static_method
                ) else stringResource(R.string.dex_browser_instance_method)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.dex_browser_button_cancel))
                    }
                    Button(onClick = onDone) {
                        Text(text = stringResource(R.string.dex_browser_button_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun FieldDetailsDialog(
    className: String,
    field: FieldInfo,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dex_browser_field_detail),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailItem(stringResource(R.string.dex_browser_class_name), className)
                DetailItem(stringResource(R.string.dex_browser_field_name), field.name)
                DetailItem(stringResource(R.string.dex_browser_field_type), field.type)
                DetailItem(stringResource(R.string.dex_browser_type), if (field.isStatic) stringResource(
                    R.string.dex_browser_static_field
                ) else stringResource(R.string.dex_browser_instance_field)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.dex_browser_button_cancel))
                    }
                    Button(onClick = onDone) {
                        Text(text = stringResource(R.string.dex_browser_button_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

fun LazyListScope.addItems(
    nodes: List<Node>,
    indent: Int,
    viewModel: DexBrowserViewModel,
    classNamePrefix: String = ""
) {

    val sortedNodes = nodes.sortedWith(
        compareBy({ !it.isPackage }, { it.name })
    )

    sortedNodes.forEach { node ->
        when (node) {
            is PackageNode -> {
                val fullPackageName =
                    if (classNamePrefix.isEmpty()) node.name else "$classNamePrefix.${node.name}"

                item(key = "pkg_${node.hashCode()}") {
                    PackageNodeItem(
                        node = node,
                        indent = indent,
                        onItemClick = { viewModel.toggleNodeExpansion(node) }
                    )
                }
                if (node.expanded.value) {
                    addItems(node.children, indent + 1, viewModel, fullPackageName)
                }
            }

            is ClassNode -> {
                item(key = "cls_${node.hashCode()}") {
                    ClassNodeItem(
                        node = node,
                        indent = indent,
                        onItemClick = { viewModel.toggleNodeExpansion(node) }
                    )
                }
                if (node.expanded.value) {
                    item(key = "details_${node.hashCode()}") {
                        ClassDetails(
                            node = node,
                            indent = indent,
                            viewModel = viewModel,
                            className = node.className
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageNodeItem(
    node: PackageNode,
    indent: Int,
    onItemClick: () -> Unit
) {
    val expanded by node.expanded

    val icon = if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder
    val iconTint =
        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        tonalElevation = if (expanded) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Row(modifier = Modifier.padding(start = (indent * 16).dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint
                    )
                }
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "collapse" else "expand"
                )
            },
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNodeItem(
    node: ClassNode,
    indent: Int,
    onItemClick: () -> Unit
) {
    val expanded by node.expanded
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (expanded) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

            },
            supportingContent = {
                if (expanded) {
                    Text(
                        stringResource(
                            R.string.dex_browser_field_method_count,
                            node.fields.size,
                            node.methods.size
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            leadingContent = {
                Row(modifier = Modifier.padding(start = (indent * 16).dp)) {
                    if (node.isInterface) {
                        InterfaceCircularChar()
                    } else {
                        ClassCircularChar()
                    }
                }
            },
            trailingContent = {
                if (node.fields.isNotEmpty() || node.methods.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "collapse" else "expand"
                    )
                }
            },
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

@Composable
fun ClassDetails(
    node: ClassNode,
    indent: Int,
    viewModel: DexBrowserViewModel,
    className: String
) {
    val expanded by node.expanded
    val startPadding = (indent * 16 + 48).dp
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, top = 4.dp, end = 16.dp, bottom = 8.dp)
        ) {
            if (node.fields.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.dex_browser_fields),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    node.fields.sortedBy { it.name }.forEach { field ->
                        Row(
                            modifier = Modifier
                                .clickable {
                                    viewModel.showFieldDetails(className, field)
                                }
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FieldCircularChar()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = field.name, maxLines = 2)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (node.methods.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.dex_browser_methods),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    node.methods.sortedBy { it.name }.forEach { method ->
                        Row(
                            modifier = Modifier
                                .clickable {
                                    viewModel.showMethodDetails(className, method)
                                }
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MethodCircularChar()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = method.name, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}


