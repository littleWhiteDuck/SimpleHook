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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.adapter.ImExportAdapter
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ConfigItem
import me.simpleHook.compat.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.hook.util.Type
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
            onCheckedChange(
                checked, position
            )
        }
    }
    private val configSystem = ConfigSystemUtil.getConfigSystem()
    private val staticField = "common.setStaticObjectField('类名', '变量名', 变量值);"
    private val instanceField = "common.setObjectField(param.thisObject, '变量名', 变量值);"
    private val hookAllConstructor = """
        common.hookAllConstructors('类名', function (param) {
            beforeHook
        }, function (param) {
            afterHook
        });
    """.trimIndent()
    private val hookConstructor = """
        common.hookConstructor('类名', params function (param) {
            beforeHook
        }, function (param) {
            afterHook
        });
    """.trimIndent()
    private val hookMethod = """
        common.hookMethod('类名', '方法名', params function (param) {
            beforeHook
        }, function (param) {
            afterHook
        });
    """.trimIndent()
    private val hookAllMethods = """
        common.hookAllMethods('类名', '方法名',function (param) {
            beforeHook
        }, function (param) {
            afterHook
        });
    """.trimIndent()
    private val getPackageName = "var currentPackageName = common.getlpparam().packageName;"
    private val ifFormat = """
        if (currentPackageName == 'packageName') {
            //description
        implement
        } 
    """.trimIndent()
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
                                    item.appConfig.packageName, Json.encodeToString(item.appConfig)
                                )
                            }
                            tempList.add(item.appConfig.copy(enable = isEnableSave))
                        }
                    }
                    tempList.reverse()
                    viewModel.insertConfigs(*tempList.toTypedArray())
                    if (checkIsZero) {
                        "为空".toast(mContext)
                    } else {
                        "导入成功".toast(mContext)
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

    private fun getStringJSConfig(list: List<ConfigItem>?) = list?.let {
        var result = "$getPackageName\n//请手动将同方法的hook移至同一个hook代码内，否则后面的不会生效\n"
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            val configStr = toJSConfig(appConfig.configs)
            result += ifFormat.replace("packageName", appConfig.packageName)
                .replace("description", appConfig.description)
                .replace("implement", configStr) + "\n"
        }
        result
    } ?: ""

    private fun toJSConfig(configStr: String): String {
        val configs = Json.decodeFromString<List<ConfigBean>>(configStr)
        var result = ""
        configs.forEach {
            it.apply {
                val hookMode = getHookMode(methodName, params)
                val temp = when (mode) {
                    Constant.HOOK_STATIC_FIELD -> {
                        val staticFieldStr =
                            staticField.replace("类名", fieldClassName).replace("变量名", fieldName)
                                .replace(
                                    "变量值", getValue(Type.getDataTypeValue(resultValues)).toString()
                                )
                        val thisResult =
                            hookMode.replace("afterHook", staticFieldStr).replace("beforeHook", "")
                                .replace("类名", className).replace("方法名", methodName)
                                .replace("params", transParams(params))
                        "\n${thisResult}\n"
                    }
                    Constant.HOOK_FIELD -> {
                        val instanceFieldStr = instanceField.replace("变量名", fieldName).replace(
                            "变量值", getValue(Type.getDataTypeValue(resultValues)).toString()
                        )
                        val thisResult = hookMode.replace("afterHook", instanceFieldStr)
                            .replace("beforeHook", "").replace("类名", className)
                            .replace("方法名", methodName).replace("params", transParams(params))
                        "\n${thisResult}\n"
                    }
                    Constant.HOOK_RETURN -> {
                        val resultValue =
                            " param.setResult(${getValue(Type.getDataTypeValue(resultValues))});"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", resultValue).replace("afterHook", "")
                            .replace("params", transParams(params))
                    }
                    Constant.HOOK_PARAM -> {
                        val paramValue = transParamValues(params, resultValues)
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", paramValue).replace("afterHook", "")
                            .replace("params", transParams(params))
                    }
                    Constant.HOOK_BREAK -> {
                        val resultValue = " param.setResult(null);"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", resultValue).replace("afterHook", "")
                            .replace("params", transParams(params))
                    }
                    Constant.HOOK_RECORD_RETURN -> {
                        val resultValue = "common.log('返回值: ' + param.getResult());"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", "").replace("afterHook", resultValue)
                            .replace("params", transParams(params))
                    }
                    Constant.HOOK_RECORD_PARAMS -> {
                        var resultValue = ""
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", "").replace("afterHook", resultValue)
                            .replace("params", transParams(params))
                    }
                    Constant.HOOK_RECORD_PARAMS_RETURN -> {
                        var resultValue = "common.log('返回值: ' + param.getResult());\n\t"
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("beforeHook", "").replace("afterHook", resultValue)
                            .replace("params", transParams(params))
                    }
                    else -> "//error"
                }
                result += "$temp \n\n"
            }
        }
        return result
    }

    private fun getValue(value: Any?): Any {
        return when (value) {
            is String -> "'$value'"
            null -> "null"
            else -> "${value.javaClass.name}.valueOf('$value')"
        }
    }

    private fun getHookMode(methodName: String, params: String): String {
        return if (methodName == "<init>") {
            if (params == "*") {
                hookAllConstructor
            } else {
                hookConstructor
            }
        } else {
            if (params == "*") {
                hookAllMethods
            } else {
                hookMethod
            }
        }
    }

    private fun transParamValues(params: String, resultValues: String): String {
        val methodParams = params.split(",")
        var result = ""
        for (i in methodParams.indices) {
            if (resultValues.split(",")[i] == "") continue
            val targetValue = getValue(Type.getDataTypeValue(resultValues.split(",")[i]))
            result += "param.args[$i] = $targetValue;"
            if (i != methodParams.size - 1) result += "\n\t"
        }
        return result
    }

    private fun transParams(params: String): String {
        var value = ""
        if (params == "") return "[],"
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
        params.height = (PhoneUtils.getAppHeight(requireActivity()) * 0.6).toInt()
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        super.onResume()
    }
}