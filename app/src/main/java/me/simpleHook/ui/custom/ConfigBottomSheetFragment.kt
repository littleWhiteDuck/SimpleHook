package me.simpleHook.ui.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.databinding.ConfigDialogBinding
import me.simpleHook.util.isContainState
import me.simpleHook.util.toast
import java.util.regex.Pattern

private const val smaliPattern = """^L.*;"""
private const val pattern_basic = """(B|S|I|J|F|D|Z|C)(B|S|I|J|F|D|Z|C|L)"""
private const val pattern_basic_array = """\[(B|S|I|J|F|D|Z|C)"""
private const val pattern_object_array = """\[L(.*)"""
private const val CLASS_NAME_STATE = 1
private const val METHOD_NAME_STATE = 1 shl 1
private const val PARAMS_STATE = 1 shl 2
private const val RESULT_VALUE_STATE = 1 shl 3
private const val FIELD_NAME_STATE = 1 shl 4
private const val FIELD_CLASS_NAME_STATE = 1 shl 5
private const val HOOK_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE
private const val HOOK_PARAM_CHECK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val HOOK_BREAK_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val HOOK_STATIC_FIELD_CHECK =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE
private const val HOOK_FIELD_CHECK =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE
private const val RECORD_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val RECORD_PARAMS_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RETURN_PARAMS =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val SHOW_STATIC_FIELD =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_FIELD =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RECORD_RETURN_PARAMS_BREAK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE

class ConfigBottomSheetFragment(
    private val configBean: ConfigBean,
    private val saveConfig: (ConfigBean) -> Unit,
    private val deleteConfig: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: ConfigDialogBinding? = null
    private val binding get() = _binding!!

    private var hookMode = Constant.HOOK_RETURN
    private var configEnable = true

    /* override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
         return BottomSheetDialog(requireContext(), R.style.translucent)
     }*/
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ConfigDialogBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.apply {
            saveConfig.setOnClickListener {
                toCheck()
            }
            deleteConfig.setOnClickListener {
                deleteConfig()
                dismissAllowingStateLoss()
            }
            if (tag == "ADD") {
                deleteConfig.isVisible = false
            }
        }
        /*modifyConfig = if (isSmali2Config) false else configBean.className.isNotEmpty()*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            configBean.apply {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsTypeEdit.setText(params)
                fieldNameEdit.setText(fieldName)
                fieldClassNameEdit.setText(fieldClassName)
                resultValueEdit.setText(resultValues)
                hookMode = mode
                configEnable = enable
            }
            onModeChange()
        }
        val list = resources.getStringArray(R.array.config_hook_mode_item)
        binding.modeSelectSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, list
        )
        binding.modeSelectSpinner.setSelection(configBean.mode)
        binding.modeSelectSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    hookMode = position
                    onModeChange()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun toCheck() {
        val className = smali2Java(binding.classNameEdit.text.toString().trim())
        val methodName = binding.methodNameEdit.text.toString().trim()
        val params = tranParams(binding.paramsTypeEdit.text.toString().trim())
        val results = binding.resultValueEdit.text.toString().trim()
        val fieldName = binding.fieldNameEdit.text.toString()
        val fieldClassName = tranParams(binding.fieldClassNameEdit.text.toString())
        var stateCheck = getCheckStateMode(this.hookMode)
        if (className.isNotEmpty()) stateCheck = stateCheck and CLASS_NAME_STATE.inv()
        if (methodName.isNotEmpty()) stateCheck = stateCheck and METHOD_NAME_STATE.inv()
        if (params.isNotEmpty()) stateCheck = stateCheck and PARAMS_STATE.inv()
        if (results.isNotEmpty()) stateCheck = stateCheck and RESULT_VALUE_STATE.inv()
        if (fieldName.isNotEmpty()) stateCheck = stateCheck and FIELD_NAME_STATE.inv()
        if (fieldClassName.isNotEmpty()) stateCheck = stateCheck and FIELD_CLASS_NAME_STATE.inv()
        val canCancel = stateCheck == 0
        if (canCancel) {
            if (methodName == "<init>" && (hookMode == Constant.HOOK_RETURN || hookMode == Constant.HOOK_BREAK)) {
                getString(R.string.config_hook_constructor_tip).toast(requireContext())
            }
            val configBean = ConfigBean(
                this.hookMode,
                className,
                methodName,
                params,
                fieldName,
                fieldClassName,
                results,
                configEnable
            )
            saveConfig(configBean)
            dismiss()
        } else {
            getString(R.string.config_info_not_match_mode).toast(requireContext())
        }
    }

    private fun smali2Java(strSmali: String) = if (Pattern.matches(smaliPattern, strSmali)) {
        strSmali.replaceFirst("L", "").replace("/", ".").replace(";", "")
    } else {
        strSmali
    }

    private fun tranParam(param: String): String {
        var temp = param
        temp = temp.replace(Regex(pattern_object_array), "$1[]")
        if (temp.startsWith("L")) {
            temp = temp.replaceFirst("L", "")
        }
        return temp.replace("/", ".")
    }

    private fun tranParams(params: String): String {
        val isSmali = params.contains(Regex("[/;]")) || isPrimitiveType(params)
        if (!isSmali || params.isEmpty()) return params
        var paramStr = params
        val json = "<ON>"
        if (params.contains("JSON")) {
            paramStr = paramStr.replace("JSON", json)
        }
        paramStr = paramStr.replace("[", ",[")
        while (paramStr.contains(Regex(pattern_basic))) {
            paramStr = paramStr.replace(Regex(pattern_basic), "$1,$2")
        }
        paramStr = paramStr.replace(Regex(pattern_basic_array), "[$1,")
        val paramArray = paramStr.split(Regex("[,;]"))
        val sb = StringBuilder()
        for (i in paramArray.indices) {
            if (paramArray[i].trim().isEmpty()) continue
            sb.append(tranParam(paramArray[i])).append(",")
        }
        var temp = sb.toString()
        if (params.contains("JSON")) {
            temp = temp.replace(json, "JSON")
        }
        if (temp[temp.length - 1] == ',') {
            temp = temp.substring(0, temp.length - 1)
        }
        return temp
    }

    private fun isPrimitiveType(params: String): Boolean {
        var isSmali = true
        var paramStr = params
        paramStr = paramStr.replace("[", ",[")
        while (paramStr.contains(Regex(pattern_basic))) {
            paramStr = paramStr.replace(Regex(pattern_basic), "$1,$2")
        }
        paramStr = paramStr.replace(Regex(pattern_basic_array), "[$1,")
        val paramArray = paramStr.split(",")
        for (i in paramArray.indices) {
            if (paramArray[i].trim().isEmpty()) continue
            isSmali = paramArray[i].contains(Regex("""[BSIJFDZC]""")) || paramArray[i].contains(
                Regex(pattern_basic_array)
            ) || paramArray[i].isEmpty()
        }
        return isSmali
    }

    private fun onModeChange() {
        val checkStateMode = getShowStateMode(hookMode)
        binding.apply {
            showView(
                checkStateMode isContainState METHOD_NAME_STATE, methodNameInput, methodNameEdit
            )
            showView(checkStateMode isContainState PARAMS_STATE, paramsTypeInput, paramsTypeEdit)
            showView(checkStateMode isContainState FIELD_NAME_STATE, fieldNameInput, fieldNameEdit)
            showView(
                checkStateMode isContainState FIELD_CLASS_NAME_STATE,
                fieldClassNameInput,
                fieldClassNameEdit
            )
            showView(
                checkStateMode isContainState RESULT_VALUE_STATE, resultValueInput, resultValueEdit
            )
        }
    }

    private fun showView(isShow: Boolean, input: TextInputLayout, edit: TextInputEditText) {
        input.visibility = if (isShow) View.VISIBLE else View.GONE
        if (!isShow) edit.setText("")
    }

    private fun getCheckStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN -> HOOK_RETURN_CHECK
        Constant.HOOK_PARAM -> HOOK_PARAM_CHECK
        Constant.HOOK_BREAK -> HOOK_BREAK_CHECK
        Constant.HOOK_FIELD -> HOOK_FIELD_CHECK
        Constant.HOOK_STATIC_FIELD -> HOOK_STATIC_FIELD_CHECK
        Constant.HOOK_RECORD_RETURN -> RECORD_RETURN_CHECK
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_PARAMS_RETURN -> RECORD_PARAMS_CHECK
        else -> 0
    }

    private fun getShowStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN, Constant.HOOK_PARAM -> SHOW_RETURN_PARAMS
        Constant.HOOK_FIELD -> SHOW_FIELD
        Constant.HOOK_STATIC_FIELD -> SHOW_STATIC_FIELD
        Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS, Constant.HOOK_BREAK, Constant.HOOK_RECORD_PARAMS_RETURN -> SHOW_RECORD_RETURN_PARAMS_BREAK
        else -> 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}