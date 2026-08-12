package me.simpleHook.feature.extension.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.core.base.BaseBottomViewFragment
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.core.extension.showPopup
import me.simpleHook.feature.extension.ui.ExtensionActivity
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.feature.config.ui.view.HookModeView
import me.simpleHook.core.ui.view.edit.InputView
import me.simpleHook.feature.extension.ui.view.ModelListView

class ModelBottomViewFragment(private val label: String) : BaseBottomViewFragment<ModelListView>() {

    private val modelList = ArrayList<ExtensionConfigEntity>()

    private val viewModel by viewModels<AppConfigViewModel>()

    private val adapter by lazy {
        ModelAdapter { position, mode ->
            onItemClick(position, mode)
        }
    }

    private fun onItemClick(position: Int, mode: Int) {
        if (mode == 1) {
            viewModel.deleteExtConfigs(modelList[position])
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
        viewModel.getAllExtConfigs().observe(viewLifecycleOwner) {
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
            viewModel.deleteExtConfigsByPackageName(Constant.MODEL_EXTENSION_CONFIG)
            dismiss()
        }
        root.closeButton.setOnClickListener { dismiss() }
    }


    private fun editModel(
        extConfigEntity: ExtensionConfigEntity
    ) {
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint = context.getString(R.string.extension_template_edit_name_hint)
            textInputLayout.counterMaxLength = 15
            textInputLayout.isCounterEnabled = true
            editText.setText(extConfigEntity.appName)
        }
        customDialog(
            requireContext(),
            title = getString(R.string.extension_template_modify_model),
            contentView = inputView,
            okText = getString(R.string.extension_template_go_modify),
            okClick = {
                val modelName = inputView.editText.text.toString()
                if (modelName.isNotEmpty() && modelName.length < 15) {
                    extConfigEntity.appName = modelName
                    ExtensionActivity.startActivity(requireContext(), extConfigEntity, true)
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
