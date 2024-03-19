package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.base.BaseBottomViewFragment
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.extension.showPopup
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.view.config.HookModeView
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.ui.view.extension.ModelListView

class ModelBottomViewFragment(private val label: String) : BaseBottomViewFragment<ModelListView>() {

    private val modelList = ArrayList<AssistConfig>()

    private val viewModel by viewModels<AppViewModel>()

    private val adapter by lazy {
        ModelAdapter { position, mode ->
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
            viewModel.deleteAssistConfigsByPackageName(Constant.MODEL_EXTENSION_CONFIG)
            dismiss()
        }
        root.closeButton.setOnClickListener { dismiss() }
    }


    private fun editModel(
        assistConfig: AssistConfig
    ) {
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint = context.getString(R.string.extension_template_edit_name_hint)
            textInputLayout.counterMaxLength = 15
            textInputLayout.isCounterEnabled = true
            editText.setText(assistConfig.appName)
        }
        customDialog(
            requireContext(),
            title = getString(R.string.extension_template_modify_model),
            contentView = inputView,
            okText = getString(R.string.extension_template_go_modify),
            okClick = {
                val modelName = inputView.editText.text.toString()
                if (modelName.isNotEmpty() && modelName.length < 15) {
                    assistConfig.appName = modelName
                    ExtensionActivity.startActivity(requireContext(), assistConfig, true)
                    dismiss()
                } else {
                    requireActivity().showPopup(getString(R.string.extension_template_illegal_name))
                }
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }
}

class ModelAdapter(val onClick: (Int, mode: Int) -> Unit) :
    RecyclerView.Adapter<ModelAdapter.ViewHolder>() {
    var items: Array<String> = emptyArray()

    inner class ViewHolder(view: HookModeView) : RecyclerView.ViewHolder(view) {
        val title = view.title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val hookModeView = HookModeView(parent.context)
        hookModeView.setOnClickListener {
            val position = it.getTag(R.id.item_hook_mode) as Int
            onClick(position, 0)
        }
        hookModeView.setOnLongClickListener {
            val position = it.getTag(R.id.item_hook_mode) as Int
            onClick(position, 1)
            true
        }
        return ViewHolder(hookModeView)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = items[position]
        holder.itemView.setTag(R.id.item_hook_mode, position)
        holder.title.text = str
    }
}