package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.ImExportAdapter
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.util.*

class ConfigDialogFragment(
    private val configsList: List<ConfigItem>,
    private val isImport: Boolean = true
) : DialogFragment() {
    private var _binding: FragmentConfigImExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter by lazy {
        ImExportAdapter { checked: Boolean, position: Int ->
            onCheckedChange(
                checked,
                position
            )
        }
    }
    private var isAnti = false
    private val sp by lazy { SPUtils(requireContext()) }
    private lateinit var mContext: Context
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            title.text = if (isImport) "导入配置" else "导出配置"
            confirm.setOnClickListener {
                var checkIsZero = true
                val isGrant = sp.openStorage
                if (isImport) {
                    val tempList = mutableListOf<AppConfig>()
                    for (item in configsList) {
                        if (item.isChecked) {
                            checkIsZero = false
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (isGrant) {
                                    FileUtils.saveConfig(
                                        mContext,
                                        item.appConfig.packageName,
                                        Constant.APP_CONFIG_NAME,
                                        Gson().toJson(item.appConfig)
                                    )
                                }
                            }
                            tempList.add(item.appConfig)
                        }
                    }
                    viewModel.insertConfigs(*tempList.toTypedArray())
                    if (checkIsZero) {
                        "为空".toast(mContext)
                    } else {
                        "导入成功".toast(mContext)
                        this@ConfigDialogFragment.dismiss()
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
                        "为空".toast(mContext)
                    } else {
                        val strConfig = getStrConfig(tempList)
                        ToolUtils.toClip(mContext, strConfig)
                        getString(R.string.main_home_export_configs_tip).toast(mContext)
                        this@ConfigDialogFragment.dismiss()
                    }
                }
            }
            selectAll.setOnClickListener {
                if (isAnti) {
                    antiSelect()
                } else {
                    isAnti = !isAnti
                    selectAll.text = if (isAnti) "反选" else "全选"
                    setAllSelect()
                }
            }
            cancel.setOnClickListener { this@ConfigDialogFragment.dismiss() }
        }
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
    private fun getStrConfig(list: List<ConfigItem>?) =
        list?.let {
            val appConfigs = ArrayList<AppConfig>()
            list.forEach { configItem ->
                val appConfig = configItem.appConfig
                if (sp.encryptConfigs && !appConfig.configs.startsWith("config://")) {
                    appConfig.configs = CipherUtils.encrypt(appConfig.configs).toString()
                }
                appConfigs.add(appConfig)
            }
            Gson().toJson(appConfigs)
        } ?: ""

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = (PhoneUtils.getAppHeight(requireContext()) * 0.6).toInt()
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        super.onResume()
    }
}