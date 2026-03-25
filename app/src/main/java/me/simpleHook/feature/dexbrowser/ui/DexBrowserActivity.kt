package me.simpleHook.feature.dexbrowser.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.core.extension.dp
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.utils.FastScrollerUtil
import me.simpleHook.data.DexUiState
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MethodInfo
import me.simpleHook.databinding.ActivityDexBrowserBinding
import me.simpleHook.databinding.ItemDexBrowserDetailRowBinding
import me.simpleHook.databinding.LayoutDexBrowserDetailBinding
import me.simpleHook.feature.applist.ui.AppListActivity
import me.simpleHook.feature.dexbrowser.ui.adapter.DexBrowserAdapter
import me.simpleHook.feature.dexbrowser.viewmodel.DexBrowserViewModel

class DexBrowserActivity : BaseActivity() {

    private lateinit var binding: ActivityDexBrowserBinding
    private val viewModel by viewModels<DexBrowserViewModel> { DexBrowserViewModel.Factory }
    private val adapter by lazy {
        DexBrowserAdapter(
            onNodeClick = viewModel::toggleNodeExpansion,
            onFieldClick = { className, item -> showFieldDetails(className, item.field) },
            onMethodClick = { className, item -> showMethodDetails(className, item.method) }
        )
    }

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::onFilePicked)
        }

    private val pickInstalledApp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val packageName = data.getStringExtra("packageName").orEmpty()
            if (packageName.isBlank()) return@registerForActivityResult
            viewModel.onInstalledAppPicked(
                packageName = packageName,
                appName = data.getStringExtra("appName").orEmpty()
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDexBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.dex_browser_title)
        initView()
        collectUiState()
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DexBrowserActivity)
            adapter = this@DexBrowserActivity.adapter
            clipToPadding = false
        }
        binding.selectSourceButton.setOnClickListener {
            showSourceMenu()
        }
        FastScrollerUtil.bind(binding.recyclerView)
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentContainer) { view, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                navigationInsets.left,
                view.paddingTop,
                navigationInsets.right,
                0
            )
            binding.recyclerView.updatePadding(bottom = navigationInsets.bottom + 24.dp)
            windowInsets
        }
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect(::render)
        }
    }

    private fun showSourceMenu() {
        PopupMenu(this, binding.selectSourceButton).apply {
            menuInflater.inflate(R.menu.menu_dex_browser_source, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pick_file -> {
                        openDocument.launch(arrayOf("*/*"))
                        true
                    }

                    R.id.action_pick_app -> {
                        pickInstalledApp.launch(
                            Intent(this@DexBrowserActivity, AppListActivity::class.java).apply {
                                putExtra("isFromAssist", true)
                            }
                        )
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun render(uiState: DexUiState) {
        val hasError = uiState.errorResId != null || !uiState.errorMessage.isNullOrBlank()
        val errorText = when {
            uiState.errorResId != null -> getString(uiState.errorResId)
            !uiState.errorMessage.isNullOrBlank() -> uiState.errorMessage
            else -> getString(R.string.common_unknown_error)
        }

        adapter.submitList(uiState.items)
        binding.loadingContainer.isVisible = uiState.isLoading
        binding.loadingMessageView.isVisible = uiState.isLoading && uiState.loadingMessageResId != null
        binding.loadingMessageView.text = uiState.loadingMessageResId?.let(::getString).orEmpty()
        binding.errorView.isVisible = !uiState.isLoading && hasError
        binding.recyclerView.isVisible =
            !uiState.isLoading && !hasError && uiState.items.isNotEmpty()
        binding.emptyView.isVisible =
            !uiState.isLoading && !hasError && uiState.items.isEmpty()

        if (hasError) {
            binding.errorView.text = getString(R.string.dex_browser_error_format, errorText)
        }

        binding.emptyView.text = if (uiState.hasLoadedSource) {
            getString(R.string.dex_browser_empty)
        } else {
            getString(R.string.dex_browser_select_source_tip)
        }

        renderDexSelectionDialog(uiState)
    }

    private fun renderDexSelectionDialog(uiState: DexUiState) {
        val fragment = supportFragmentManager.findFragmentByTag(
            DexSelectionBottomSheetFragment.TAG
        ) as? DexSelectionBottomSheetFragment

        if (!uiState.showDexSelectionDialog) {
            fragment?.dismissAllowingStateLoss()
            return
        }

        if (fragment == null && !supportFragmentManager.isStateSaved) {
            DexSelectionBottomSheetFragment().show(
                supportFragmentManager,
                DexSelectionBottomSheetFragment.TAG
            )
        }
    }

    private fun showMethodDetails(className: String, method: MethodInfo) {
        showDetailDialog(
            title = getString(R.string.dex_browser_method_detail),
            items = listOf(
                getString(R.string.dex_browser_class_name) to className,
                getString(R.string.dex_browser_method_name) to method.name,
                getString(R.string.dex_browser_param_type) to method.parameters.joinToString(", "),
                getString(R.string.dex_browser_return_type) to method.returnType,
                getString(R.string.dex_browser_type) to if (method.isStatic) {
                    getString(R.string.dex_browser_static_method)
                } else {
                    getString(R.string.dex_browser_instance_method)
                }
            ),
            onConfirm = { finishWithMethod(className, method) }
        )
    }

    private fun showFieldDetails(className: String, field: FieldInfo) {
        showDetailDialog(
            title = getString(R.string.dex_browser_field_detail),
            items = listOf(
                getString(R.string.dex_browser_class_name) to className,
                getString(R.string.dex_browser_field_name) to field.name,
                getString(R.string.dex_browser_field_type) to field.type,
                getString(R.string.dex_browser_type) to if (field.isStatic) {
                    getString(R.string.dex_browser_static_field)
                } else {
                    getString(R.string.dex_browser_instance_field)
                }
            ),
            onConfirm = { finishWithField(className, field) }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun showDetailDialog(
        title: String,
        items: List<Pair<String, String>>,
        onConfirm: () -> Unit
    ) {
        val binding = LayoutDexBrowserDetailBinding.inflate(layoutInflater)
        binding.titleView.text = title
        items.forEach { (label, value) ->
            val rowBinding = ItemDexBrowserDetailRowBinding.inflate(layoutInflater)
            rowBinding.labelView.text = "$label:"
            rowBinding.valueView.text = value
            binding.detailContainer.addView(
                rowBinding.root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val dialog: AlertDialog = customDialog(
            context = this,
            contentView = binding.root
        )
        binding.cancelButton.setOnClickListener { dialog.dismiss() }
        binding.confirmButton.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
    }

    private fun finishWithMethod(className: String, method: MethodInfo) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra("className", className)
                putExtra("methodInfo", method)
            }
        )
        finish()
    }

    private fun finishWithField(className: String, field: FieldInfo) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra("className", className)
                putExtra("fieldInfo", field)
            }
        )
        finish()
    }
}
