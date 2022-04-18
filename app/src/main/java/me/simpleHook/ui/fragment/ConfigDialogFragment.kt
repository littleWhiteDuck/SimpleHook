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
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.ImExportAdapter
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.hook.Type
import me.simpleHook.util.*

class ConfigDialogFragment(
    private val configsList: List<ConfigItem>, private val mode: Int
) : DialogFragment() {
    private var _binding: FragmentConfigImExportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter by lazy {
        ImExportAdapter { checked: Boolean, position: Int ->
            onCheckedChange(
                checked, position
            )
        }
    }
    private val staticField = "common.setStaticObjectField('类名', '变量名', 变量值);"
    private val instanceField = "common.setObjectField(param.thisObject, '变量名', 变量值);"
    private val constructor = """
        common.hookAllConstructors('类名', function (param) {
            具体前行为
        }, function (param) {
            具体后行为
        });
    """.trimIndent()
    private val commonMethod = """
        common.hookAllMethods('类名', '方法名', function (param) {
           具体前行为
        }, function (param) {
           具体后行为
        });
    """.trimIndent()
    private var isAnti = false
    private val sp by lazy { SPUtils(requireContext()) }
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
                val isGrant = sp.openStorage
                if (mode == Constant.CONFIG_IMPORT_MODE) {
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
                        val strConfig =
                            if (mode == Constant.CONFIG_EXPORT_MODE) getStrConfig(tempList) else getStringJSConfig(
                                tempList
                            )
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
                    selectAll.text =
                        if (isAnti) getString(R.string.config_dialog_button_invert_selection) else getString(
                            R.string.config_dialog_button_select_all
                        )
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
    private fun getStrConfig(list: List<ConfigItem>?) = list?.let {
        val appConfigs = ArrayList<AppConfig>()
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            appConfigs.add(appConfig)
        }
        Gson().toJson(appConfigs)
    } ?: ""

    private fun getStringJSConfig(list: List<ConfigItem>?) = list?.let {
        var result = ""
        if (list.size != 1) result += "//请自行将各应用所属配置分开，否则可能会出错\n\n\n"
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            val name = appConfig.appName
            val configStr = toJSConfig(appConfig.configs)
            result += "//$name\n\n$configStr"
        }
        result
    } ?: ""

    private fun toJSConfig(configStr: String): String {
        val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
        val configs = Gson().fromJson<ArrayList<ConfigBean>>(configStr, listType)
        var result = ""
        configs.forEach {
            it.apply {
                val temp = when (mode) {
                    Constant.HOOK_STATIC_FIELD -> {
                        "\n${
                            staticField.replace("类名", className).replace("变量名", fieldName).replace(
                                "变量值", getValue(Type.getDataTypeValue(resultValues)).toString()
                            )
                        }\n"
                    }
                    Constant.HOOK_FIELD -> {
                        val instanceFieldStr = instanceField.replace("变量名", fieldName).replace(
                            "变量值", getValue(Type.getDataTypeValue(resultValues)).toString()
                        )
                        "\n${
                            constructor.replace("类名", className).replace("具体后行为", instanceFieldStr)
                                .replace("具体前行为", "")
                        }\n"
                    }
                    Constant.HOOK_RETURN -> {
                        val resultValue =
                            " param.setResult(${getValue(Type.getDataTypeValue(resultValues))});"
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", resultValue).replace("具体后行为", "")
                    }
                    Constant.HOOK_PARAM -> {
                        val paramValue = transParamValues(params, resultValues)
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", paramValue).replace("具体后行为", "")
                    }
                    Constant.HOOK_BREAK -> {
                        val resultValue = " param.setResult(null);"
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", resultValue).replace("具体后行为", "")
                    }
                    Constant.HOOK_RECORD_RETURN -> {
                        val resultValue = "common.log('返回值: ' + param.getResult());"
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", "").replace("具体后行为", resultValue)
                    }
                    Constant.HOOK_RECORD_PARAMS -> {
                        var resultValue = ""
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n"
                        }
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", "").replace("具体后行为", resultValue)
                    }
                    Constant.HOOK_RECORD_PARAMS_RETURN -> {
                        var resultValue = "common.log('返回值: ' + param.getResult());\n"
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n"
                        }
                        commonMethod.replace("类名", className).replace("方法名", methodName)
                            .replace("具体前行为", "").replace("具体后行为", resultValue)
                    }
                    else -> "//error"
                }
                result += "$temp \n\n"
            }
        }
        return result
    }

    private fun getValue(value: Any?): Any? {
        return when (value) {
            is String -> "'$value'"
            is Int -> "java.lang.Integer('$value')"
            is Long -> "java.lang.Long('$value')"
            is Short -> "java.lang.Short('$value')"
            else -> value
        }
    }

    private fun transParamValues(params: String, resultValues: String): String {
        val methodParams = params.split(",")
        var result = ""
        for (i in methodParams.indices) {
            if (resultValues.split(",")[i] == "") continue
            val targetValue = getValue(Type.getDataTypeValue(resultValues.split(",")[i]))
            result += "param.args[$i] = $targetValue;"
            if (i != methodParams.size - 1) result += "\n"
        }
        return result
    }

    private fun transParams(params: String): String {
        var value = ""
        if (params == "") return value
        val methodParams = params.split(",")
        methodParams.forEachIndexed { index, param ->
            val classType = Type.getClassType(param)
            value += if (classType == null) {
                "'${param}'"
            } else {
                "'${classType.name}'"
            }
            if (index != methodParams.size - 1) {
                value += ","
            }
        }
        return "[$value],"
    }

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