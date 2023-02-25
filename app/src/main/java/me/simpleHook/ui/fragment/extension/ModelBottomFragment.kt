package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import me.simpleHook.base.BaseBottomFragment
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.extension.showToast
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.fragment.config.HookModeAdapter
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.ui.view.extension.ModelListView

class ModelBottomFragment(private val label: String) : BaseBottomFragment<ModelListView>() {

    private val modelList = ArrayList<AssistConfig>()

    private val viewModel by viewModels<AppViewModel>()

    private val adapter by lazy {
        HookModeAdapter() { position, mode ->
            onItemClick(position, mode)
        }
    }

    private fun onItemClick(position: Int, mode: Int) {
        if (mode == 1) {
            viewModel.deleteAssistConfigs(modelList[position])
            dismiss()
        } else if (mode == 0) {
            val extensionConfig = modelList[position]
            if (label == "edit") {
                editModel(extensionConfig)
            }
        }
    }

    override fun initRootView(): ModelListView {
        val modelListView = ModelListView(requireContext()).apply {
            recyclerView.adapter = adapter
        }
        return modelListView
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun init() {
        viewModel.getAllAssistConfigs().observe(viewLifecycleOwner) {
            modelList.clear()
            val items = ArrayList<String>()
            for (extension in it) {
                if (extension.packageName == Constant.MODEL_EXTENSION_CONFIG) {
                    modelList.add(extension)
                    items.add(extension.appName)
                }
            }
            adapter.items = items.toTypedArray()
            adapter.notifyDataSetChanged()
        }
        root.clearAll.setOnClickListener {
            viewModel.deleteAllAssistConfigs()
            dismiss()
        }
        root.closeButton.setOnClickListener { dismiss() }
    }


    private fun editModel(
        assistConfig: AssistConfig
    ) {
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint = "给模板起个名字"
            textInputLayout.counterMaxLength = 15
            textInputLayout.isCounterEnabled = true
            editText.setText(assistConfig.appName)
        }
        customDialog(requireContext(),
            title = "修改模板",
            contentView = inputView,
            okText = "去修改",
            okClick = {
                val modelName = inputView.editText.text.toString()
                if (modelName.isNotEmpty() && modelName.length < 15) {
                    assistConfig.appName = modelName
                    ExtensionActivity.startActivity(requireContext(), assistConfig, true)
                    dismiss()
                } else {
                    requireActivity().showToast("不能为空或名字太长")
                }
            },
            cancelText = "取消").show()
    }
}