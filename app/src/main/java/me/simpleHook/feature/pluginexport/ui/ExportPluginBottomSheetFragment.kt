package me.simpleHook.feature.pluginexport.ui

import android.annotation.SuppressLint
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.core.base.BaseBottomFragment
import me.simpleHook.core.extension.showPopup
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.databinding.FragmentExportPluginSheetBinding
import me.simpleHook.feature.config.ui.adapter.ImExportAdapter
import me.simpleHook.feature.pluginexport.domain.PluginApkExporter
import me.simpleHook.feature.pluginexport.domain.PluginExportRequest
import kotlin.math.max

class ExportPluginBottomSheetFragment(
    private val configsList: List<AppConfigItem2>,
    private val startFromInfoStep: Boolean = false
) : BaseBottomFragment<FragmentExportPluginSheetBinding>() {

    private val adapter by lazy {
        ImExportAdapter { checked, position ->
            configsList[position].isChecked = checked
            updateSelectionUi()
        }
    }

    private var currentStep = STEP_SELECT
    private var isExporting = false
    private var actionBarBasePaddingBottom = 0
    private val defaultPluginAppName by lazy {
        PluginApkExporter.generateDefaultAppName()
    }
    private val defaultPluginPackageName by lazy {
        PluginApkExporter.generateDefaultPackageName()
    }
    private val defaultPluginVersionName = PluginApkExporter.DEFAULT_PLUGIN_VERSION_NAME
    private val defaultPluginVersionCode = PluginApkExporter.DEFAULT_PLUGIN_VERSION_CODE.toString()
    private val isSingleConfigDirectFlow: Boolean
        get() = startFromInfoStep && configsList.size == 1 && selectedConfigs().size == 1

    override fun init() {
        if (startFromInfoStep && selectedConfigs().isNotEmpty()) {
            currentStep = STEP_INFO
        }
        actionBarBasePaddingBottom = binding.actionBar.paddingBottom
        initRecyclerView()
        initForm()
        initButtons()
        renderStep()
        updatePrimaryButtonState()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
//        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val bottomInset = max(navigationInsets.bottom, imeInsets.bottom)
        binding.actionBar.updatePadding(bottom = actionBarBasePaddingBottom + bottomInset)
    }


    private fun initRecyclerView() {
        adapter.setDataList(configsList)
        with(binding) {
            with(recyclerView) {
                adapter = this@ExportPluginBottomSheetFragment.adapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(
                    DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
                )
            }
            emptyTip.isVisible = configsList.isEmpty()
            recyclerView.isVisible = configsList.isNotEmpty()
        }
    }

    private fun initForm() {
        with(binding) {
            pluginAppNameEdit.setText(defaultPluginAppName)
            pluginPackageNameEdit.setText(defaultPluginPackageName)
            pluginVersionNameEdit.setText(defaultPluginVersionName)
            pluginVersionCodeEdit.setText(defaultPluginVersionCode)
            pluginAppNameEdit.doAfterTextChanged {
                pluginAppNameInput.isErrorEnabled = false
                updatePrimaryButtonState()
            }
            pluginPackageNameEdit.doAfterTextChanged {
                pluginPackageNameInput.isErrorEnabled = false
                updatePrimaryButtonState()
            }
            pluginVersionNameEdit.doAfterTextChanged {
                pluginVersionNameInput.isErrorEnabled = false
                updatePrimaryButtonState()
            }
            pluginVersionCodeEdit.doAfterTextChanged {
                pluginVersionCodeInput.isErrorEnabled = false
                updatePrimaryButtonState()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initButtons() {
        with(binding) {
            secondaryButton.setOnClickListener {
                if (isExporting) return@setOnClickListener
                when (currentStep) {
                    STEP_SELECT -> {
                        configsList.forEach { it.isChecked = !it.isChecked }
                        adapter.notifyDataSetChanged()
                        updatePrimaryButtonState()
                    }

                    STEP_INFO -> {
                        if (isSingleConfigDirectFlow) {
                            dismissAllowingStateLoss()
                        } else {
                            currentStep = STEP_SELECT
                            renderStep()
                        }
                    }
                }
            }
            primaryButton.setOnClickListener {
                if (isExporting) return@setOnClickListener
                when (currentStep) {
                    STEP_SELECT -> goNextStep()
                    STEP_INFO -> exportPlugin()
                }
            }
        }
    }

    private fun goNextStep() {
        if (selectedConfigs().isEmpty()) {
            requireActivity().showPopup(getString(R.string.config_dialog_selection_empty))
            return
        }
        currentStep = STEP_INFO
        renderStep()
    }

    private fun exportPlugin() {
        val request = buildExportRequest() ?: return
        setExporting(true)
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            runCatching {
                PluginApkExporter(appContext).export(request)
            }.onSuccess { apkFile ->
                parentFragmentManager.setFragmentResult(
                    EXPORT_PLUGIN_RESULT_KEY,
                    bundleOf(EXPORT_PLUGIN_APK_PATH_KEY to apkFile.absolutePath)
                )
                dismissAllowingStateLoss()
            }.onFailure {
                setExporting(false)
                requireActivity().showPopup(
                    getString(
                        R.string.plugin_export_failed,
                        it.message ?: getString(R.string.common_failed)
                    )
                )
            }
        }
    }

    private fun buildExportRequest(): PluginExportRequest? {
        val pluginAppName = binding.pluginAppNameEdit.text?.toString()?.trim().orEmpty()
        val pluginPackageName = binding.pluginPackageNameEdit.text?.toString()?.trim().orEmpty()
        val pluginVersionName = binding.pluginVersionNameEdit.text?.toString()?.trim().orEmpty()
        val pluginVersionCodeText = binding.pluginVersionCodeEdit.text?.toString().orEmpty()
        val pluginVersionCode = PluginApkExporter.parseVersionCode(pluginVersionCodeText)
        var valid = true
        with(binding) {
            if (pluginAppName.isEmpty()) {
                pluginAppNameInput.error = getString(R.string.plugin_export_app_name_empty)
                valid = false
            } else {
                pluginAppNameInput.isErrorEnabled = false
            }
            if (!PluginApkExporter.isValidPackageName(pluginPackageName)) {
                pluginPackageNameInput.error =
                    getString(R.string.plugin_export_package_name_invalid)
                valid = false
            } else {
                pluginPackageNameInput.isErrorEnabled = false
            }
            if (pluginVersionName.isEmpty()) {
                pluginVersionNameInput.error = getString(R.string.plugin_export_version_name_empty)
                valid = false
            } else {
                pluginVersionNameInput.isErrorEnabled = false
            }
            if (pluginVersionCode == null) {
                pluginVersionCodeInput.error =
                    getString(R.string.plugin_export_version_code_invalid)
                valid = false
            } else {
                pluginVersionCodeInput.isErrorEnabled = false
            }
        }
        val selectedConfigs = selectedConfigs()
        if (selectedConfigs.isEmpty()) {
            requireActivity().showPopup(getString(R.string.config_dialog_selection_empty))
            valid = false
        }
        if (!valid) return null
        val resolvedPluginVersionCode = pluginVersionCode ?: return null
        return PluginExportRequest(
            pluginPackageName = pluginPackageName,
            pluginAppName = pluginAppName,
            pluginVersionName = pluginVersionName,
            pluginVersionCode = resolvedPluginVersionCode,
            configs = selectedConfigs
        )
    }

    private fun updateSelectionUi() {
        updatePrimaryButtonState()
    }

    private fun renderStep() {
        val isSelectStep = currentStep == STEP_SELECT
        with(binding) {
            stepSelectLayout.isVisible = isSelectStep
            stepInfoLayout.isVisible = !isSelectStep
            if (!isSelectStep) {
                stepInfoLayout.scrollTo(0, 0)
            }
            stepLabel.text = getString(
                if (isSelectStep) R.string.plugin_export_step_select
                else R.string.plugin_export_step_info
            )
            subtitle.text = getString(
                when {
                    isSelectStep -> R.string.plugin_export_step_select_tip
                    isSingleConfigDirectFlow -> R.string.plugin_export_step_info_tip_single
                    else -> R.string.plugin_export_step_info_tip
                }
            )
            secondaryButton.text = getString(
                when {
                    isSelectStep -> R.string.config_dialog_button_invert_selection
                    isSingleConfigDirectFlow -> R.string.dialog_cancel
                    else -> R.string.plugin_export_back
                }
            )
            primaryButton.text = getString(
                if (isSelectStep) R.string.plugin_export_next
                else R.string.plugin_export_type_plugin_apk
            )
        }
        updatePrimaryButtonState()
    }

    private fun updatePrimaryButtonState() {
        with(binding) {
            primaryButton.isEnabled = when {
                isExporting -> false
                currentStep == STEP_SELECT -> selectedConfigs().isNotEmpty()
                else -> pluginAppNameEdit.text?.toString()?.trim()?.isNotEmpty() == true &&
                    PluginApkExporter.isValidPackageName(
                        pluginPackageNameEdit.text?.toString()?.trim().orEmpty()
                    ) &&
                    pluginVersionNameEdit.text?.toString()?.trim()?.isNotEmpty() == true &&
                    PluginApkExporter.parseVersionCode(
                        pluginVersionCodeEdit.text?.toString().orEmpty()
                    ) != null
            }
        }
    }

    private fun setExporting(exporting: Boolean) {
        isExporting = exporting
        isCancelable = !exporting
        with(binding) {
            progressIndicator.isVisible = exporting
            secondaryButton.isEnabled = !exporting
            recyclerView.isEnabled = !exporting
            pluginAppNameInput.isEnabled = !exporting
            pluginPackageNameInput.isEnabled = !exporting
            pluginVersionNameInput.isEnabled = !exporting
            pluginVersionCodeInput.isEnabled = !exporting
            pluginAppNameEdit.isEnabled = !exporting
            pluginPackageNameEdit.isEnabled = !exporting
            pluginVersionNameEdit.isEnabled = !exporting
            pluginVersionCodeEdit.isEnabled = !exporting
            contentContainer.alpha = if (exporting) 0.6f else 1f
        }
        updatePrimaryButtonState()
    }


    private fun selectedConfigs() = configsList.filter { it.isChecked }.map { it.appConfig }

    companion object {
        const val EXPORT_PLUGIN_RESULT_KEY = "export_plugin_result"
        const val EXPORT_PLUGIN_APK_PATH_KEY = "export_plugin_apk_path"

        private const val STEP_SELECT = 0
        private const val STEP_INFO = 1
    }
}
