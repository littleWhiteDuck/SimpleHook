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
import me.simpleHook.recyclerview.adapter.ImExportAdapter
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ConfigItem
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentConfigImExportBinding
import me.simpleHook.extension.showToast
import me.simpleHook.hook.util.Type
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.util.*

private const val findAndHookMethod = """
    XposedHelpers.findAndHookMethod('类名', runtime.classLoader, "方法名", 参数类型 XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));    
"""
private const val isHook = """
if (hostPackageName == 'package_name') {
    // description
    逻辑代码
}
"""

private const val hookStaticField = """
    XposedHelpers.setStaticObjectField(XposedHelpers.findClass("变量类名", runtime.classLoader), "变量名", 变量值);
"""
private const val hookInstanceField = """
    XposedHelpers.setObjectField(param.thisObject, "变量名", 变量值);
"""
private const val recordStaticField = """
    console.log(XposedHelpers.getStaticObjectField(XposedHelpers.findClass("变量类名", runtime.classLoader), "变量名"));  
"""
private const val recordInstanceField = """
    console.log(XposedHelpers.getObjectField(param.thisObject, "变量名"));
"""
private const val hookAllMethods = """
    XposedBridge.hookAllMethods(XposedHelpers.findClass("类名", runtime.classLoader), "方法名", XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));
"""
private const val findAndHookConstructor = """
    XposedHelpers.findAndHookConstructor('类名', runtime.classLoader, 参数类型 XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));     
"""
private const val hookAllConstructors = """
    XposedBridge.hookAllConstructors(XposedHelpers.findClass("类名", runtime.classLoader), XC_MethodHook({
        beforeHookedMethod: function (param) {
            before_hook
        },
        afterHookedMethod: function (param) {
            after_hook
        }
    }));    
"""

private const val hostPackageName = "var hostPackageName = runtime.packageName;"

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
                            if (mode == Constant.CONFIG_EXPORT_MODE) getStrConfig(tempList) else getStringJSConfig(
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

    private fun getStringJSConfig(list: List<ConfigItem>?) = list?.let {
        var result = "$hostPackageName\n\n"
        list.forEach { configItem ->
            val appConfig = configItem.appConfig
            val configStr = toJSConfig(appConfig.configs)
            result += isHook.replace("package_name", appConfig.packageName)
                .replace("description", appConfig.description)
                .replace("逻辑代码", configStr) + "\n"
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
                            hookStaticField.replace("变量类名", fieldClassName)
                                .replace("变量名", fieldName)
                                .replace(
                                    "变量值",
                                    getValue(Type.getDataTypeValue(resultValues)).toString()
                                )
                        if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                            "\n${staticFieldStr}\n"
                        } else {
                            val thisResult = if (hookPoint == "before") {
                                hookMode.replace("after_hook", "")
                                    .replace("before_hook", staticFieldStr)
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            } else {
                                hookMode.replace("after_hook", staticFieldStr)
                                    .replace("before_hook", "")
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            }
                            "\n${thisResult}\n"
                        }
                    }

                    Constant.HOOK_FIELD -> {
                        val instanceFieldStr = hookInstanceField.replace("变量名", fieldName)
                            .replace(
                                "变量值",
                                getValue(Type.getDataTypeValue(resultValues)).toString()
                            )
                        val thisResult = if (hookPoint == "before") {
                            hookMode.replace("after_hook", "")
                                .replace("before_hook", instanceFieldStr)
                                .replace("类名", className).replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        } else {
                            hookMode.replace("after_hook", instanceFieldStr)
                                .replace("before_hook", "")
                                .replace("类名", className).replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        }
                        "\n${thisResult}\n"
                    }

                    Constant.HOOK_RETURN -> {
                        val resultValue =
                            " param.setResult(${getValue(Type.getDataTypeValue(resultValues))});"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", resultValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_PARAM -> {
                        val paramValue = transParamValues(params, resultValues)
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", paramValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_BREAK -> {
                        val resultValue = " param.setResult(null);"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", resultValue).replace("after_hook", "")
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_RECORD_RETURN -> {
                        val resultValue = "console.log('返回值: ' + param.getResult());"
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_RECORD_PARAMS -> {
                        var resultValue = ""
                        for (i in params.split(",").indices) {
                            resultValue += "console.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_RECORD_PARAMS_RETURN -> {
                        var resultValue = "common.log('返回值: ' + param.getResult());\n\t"
                        for (i in params.split(",").indices) {
                            resultValue += "common.log('参数$i: ' + param.args[$i]);\n\t"
                        }
                        hookMode.replace("类名", className).replace("方法名", methodName)
                            .replace("before_hook", "").replace("after_hook", resultValue)
                            .replace("参数类型", transParams(params))
                    }

                    Constant.HOOK_RECORD_STATIC_FIELD -> {
                        val staticFieldStr =
                            recordStaticField.replace("变量类名", fieldClassName)
                                .replace("变量名", fieldName)
                        if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                            "\n${staticFieldStr}\n"
                        } else {
                            val thisResult = if (hookPoint == "before") {
                                hookMode.replace("after_hook", "")
                                    .replace("before_hook", staticFieldStr)
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            } else {
                                hookMode.replace("after_hook", staticFieldStr)
                                    .replace("before_hook", "")
                                    .replace("类名", className).replace("方法名", methodName)
                                    .replace("参数类型", transParams(params))
                            }
                            "\n${thisResult}\n"
                        }
                    }

                    Constant.HOOK_RECORD_INSTANCE_FIELD -> {
                        val instanceFieldStr = recordInstanceField.replace("变量名", fieldName)
                        val thisResult = if (hookPoint == "before") {
                            hookMode.replace("after_hook", "")
                                .replace("before_hook", instanceFieldStr)
                                .replace("类名", className).replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        } else {
                            hookMode.replace("after_hook", instanceFieldStr)
                                .replace("before_hook", "")
                                .replace("类名", className).replace("方法名", methodName)
                                .replace("参数类型", transParams(params))
                        }
                        "\n${thisResult}\n"
                    }

                    Constant.HOOK_RETURN2 -> {
                        "\n暂未支持\n"
                    }

                    else -> "//未知"
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
                hookAllConstructors
            } else {
                findAndHookConstructor
            }
        } else {
            if (params == "*") {
                hookAllMethods
            } else {
                findAndHookMethod
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
        if (params == "") return ""
        val methodParams = params.split(",")
        methodParams.forEach { param ->
            val classType = Type.getClassType(param)
            value += if (classType == null) {
                "'${param}'"
            } else {
                "'${classType.name}'"
            }
            value += ","
        }
        return value
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