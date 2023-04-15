package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.extension.showToast
import me.simpleHook.recyclerview.adapter.ImExportAdapter
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.util.*

class ConfigDialogFragment(
    private val configsList: List<ConfigItem>, private val mode: Int
) : DialogFragment() {
    private var _binding: FragmentConfigImExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter by lazy {
        ImExportAdapter { checked: Boolean, position: Int ->
            onCheckedChange(checked, position)
        }
    }
    private val configSystem = ConfigSystemUtil.getConfigSystem()
    private var isAnti = false
    private lateinit var mContext: Context


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigImExportBinding.inflate(inflater, container, false)
        mContext = requireContext()
        initView()
        return binding.root
    }

    private fun initView() {
        mAdapter.setDataList(configsList)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
        this.isCancelable = false
        binding.apply {
            title.text = when (mode) {
                Constant.CONFIG_IMPORT_MODE -> getString(R.string.config_dialog_title_import_config)
                Constant.CONFIG_EXPORT_MODE -> getString(R.string.config_dialog_title_export_config)
                else -> getString(R.string.config_dialog_title_export_js_config)
            }
            confirm.setOnClickListener {
                var checkIsZero = true
                if (mode == Constant.CONFIG_IMPORT_MODE) {
                    val tempList = mutableListOf<AppConfig>()
                    for (item in configsList) {
                        val isEnableSave = configSystem.isEnableSave(item.appConfig.packageName)
                        if (item.isChecked) {
                            checkIsZero = false
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (isEnableSave) configSystem.saveCustomConfig(
                                    item.appConfig.packageName,
                                    Json.encodeToString(item.appConfig)
                                )
                            }
                            tempList.add(item.appConfig.copy(enable = isEnableSave))
                        }
                    }
                    tempList.reverse()
                    viewModel.insertConfigs(*tempList.toTypedArray())
                    if (checkIsZero) {
                        requireActivity().showToast("为空")
                    } else {
                        requireActivity().showToast("导入成功")
                        this@ConfigDialogFragment.dismiss()
                    }
                    if (tag == "from text import") {
                        goMainActivity()
                    }
                } else {
                    val tempList = ArrayList<ConfigItem>()
                    for (item in configsList) {
                        if (item.isChecked) {
                            checkIsZero = false
                            tempList.add(item)
                        }
                    }
                    if (checkIsZero) {
                        requireActivity().showToast("为空")
                    } else {
                        val strConfig =
                            if (mode == Constant.CONFIG_EXPORT_MODE) getStrConfig(tempList) else JsHook.getStringJSConfig(
                                tempList
                            )
                        ToolUtils.toClip(mContext, strConfig)
                        requireActivity().showToast(getString(R.string.main_home_export_configs_tip))
                        this@ConfigDialogFragment.dismiss()
                    }
                }
            }
            selectAll.setOnClickListener {
                if (isAnti) {
                    antiSelect()
                } else {
                    isAnti = true
                    selectAll.text =
                        if (isAnti) getString(R.string.config_dialog_button_invert_selection) else getString(
                            R.string.config_dialog_button_select_all
                        )
                    setAllSelect()
                }
            }
            cancel.setOnClickListener {
                this@ConfigDialogFragment.dismiss()
                if (tag == "from text import") {
                    goMainActivity()
                }
            }
        }
    }

    private fun goMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        requireActivity().startActivity(intent)
        requireActivity().finish()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setAllSelect() {
        for (element in configsList) {
            element.isChecked = true
        }
        mAdapter.setDataList(configsList)
        mAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun antiSelect() {
        configsList.forEach { configItem ->
            configItem.isChecked = !configItem.isChecked
        }
        mAdapter.setDataList(configsList)
        mAdapter.notifyDataSetChanged()
    }

    private fun onCheckedChange(isChecked: Boolean, position: Int) {
        configsList[position].isChecked = isChecked
    }

    /**
     * 获取所有配置文本形式
     */
    private fun getStrConfig(list: List<ConfigItem>?) = list?.let {
        val appConfigs = ArrayList<AppConfig>()
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            appConfigs.add(appConfig)
        }
        Json.encodeToString(appConfigs)
    } ?: ""


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = (PhoneUtils.getAppHeight(requireActivity()) * 0.6).toInt()
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        super.onResume()
    }
}