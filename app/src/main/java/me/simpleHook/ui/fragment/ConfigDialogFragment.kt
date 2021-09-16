package me.simpleHook.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.R
import me.simpleHook.adapter.ImExportAdapter
import me.simpleHook.bean.ConfigItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.PhoneUtils
import me.simpleHook.util.ToolUtils
import me.simpleHook.util.toast

class ConfigDialogFragment(private val configsList: ArrayList<ConfigItem>, private val isImport:Boolean = true) : DialogFragment() {
    private var _binding: FragmentConfigImExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter  by lazy { ImExportAdapter{ checked: Boolean, position: Int ->  onCheckedChange(checked, position)} }
    private var isAnti = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigImExportBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView(){
        mAdapter.setDataList(configsList)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
        binding.apply {
            title.text = if (isImport) "导入配置" else "导出配置"
            confirm.setOnClickListener {
                var checkIsZero =  0
                if (isImport){
                    for(item in configsList){
                        if (item.isChecked){
                            checkIsZero++
                            viewModel.insertConfigs(item.appConfig)
                        }
                    }
                    if (checkIsZero ==0) {
                        "为空".toast(requireContext())
                        return@setOnClickListener
                    }
                }else{
                    val tempList = ArrayList<ConfigItem>()
                    for(item in configsList){
                        if (item.isChecked){
                            checkIsZero++
                            tempList.add(item)
                        }
                    }
                    if (checkIsZero ==0) {
                        "为空".toast(requireContext())
                        return@setOnClickListener
                    }
                    val strConfig = getStrConfig(tempList)
                    ToolUtils.toClip(requireContext(), strConfig)
                    getString(R.string.main_home_export_configs_tip).toast(requireContext())
                }
                val toast = if (isImport) "导入成功" else "导出成功"
                toast.toast(requireContext())
                this@ConfigDialogFragment.dismiss()
            }
            selectAll.setOnClickListener {
                if (isAnti){
                    antiSelect()
                }else{
                    isAnti = !isAnti
                    selectAll.text = if (isAnti) "反选" else "全选"
                    setAllSelect()
                }
            }
            cancel.setOnClickListener { this@ConfigDialogFragment.dismiss() }
        }
    }

    private fun setAllSelect() {
        for (i in 0 until configsList.size){
            configsList[i].isChecked = true
        }
        mAdapter.setDataList(configsList)
        mAdapter.notifyDataSetChanged()
    }

    private fun antiSelect(){
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
    private fun getStrConfig(list: List<ConfigItem>?, formatConfig: Boolean = true) =
        list?.let {
            val configs = StringBuilder()
            for (i in it.indices) {
                configs.append("${it[i].appConfig.config},")
            }
            val strConfigs = "[${configs.substring(0, configs.length - 1)}]"
            val strConfig = if (formatConfig) JsonUtil.formatJson(strConfigs) else strConfigs
            strConfig
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