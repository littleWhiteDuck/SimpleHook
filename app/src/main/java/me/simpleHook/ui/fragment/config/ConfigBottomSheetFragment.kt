package me.simpleHook.ui.fragment.config

import android.annotation.SuppressLint
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.base.BaseBottomFragment
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.databinding.FragemntConfigDialogBinding
import me.simpleHook.extension.isContainState
import me.simpleHook.extension.showPopup
import me.simpleHook.util.SPUtils
import java.util.regex.Pattern


private const val smaliPattern = """^L.*;"""
private const val pattern_basic = """([BSIJFDZC])([BSIJFDZCL])"""
private const val pattern_basic_array = """\[([BSIJFDZC])"""
private const val pattern_object_array = """\[L(.*)"""
private const val CLASS_NAME_STATE = 1
private const val METHOD_NAME_STATE = 1 shl 1
private const val PARAMS_STATE = 1 shl 2
private const val RESULT_VALUE_STATE = 1 shl 3
private const val FIELD_NAME_STATE = 1 shl 4
private const val FIELD_CLASS_NAME_STATE = 1 shl 5
private const val HOOK_POINT_STATE = 1 shl 6
private const val RETURN_CLASS_NAME = 1 shl 7
private const val HOOK_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE
private const val HOOK_RETURN2_CHECK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or RETURN_CLASS_NAME
private const val HOOK_PARAM_CHECK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val HOOK_BREAK_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val HOOK_STATIC_FIELD_CHECK =
    FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE
private const val HOOK_RECORD_STATIC_FIELD_CHECK = FIELD_NAME_STATE or FIELD_CLASS_NAME_STATE
private const val HOOK_FIELD_CHECK =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE
private const val HOOK_RECORD_FIELD_CHECK =
    CLASS_NAME_STATE or FIELD_NAME_STATE or METHOD_NAME_STATE
private const val RECORD_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val RECORD_PARAMS_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RETURN_PARAMS =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val SHOW_RETURN2 =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE or RETURN_CLASS_NAME
private const val SHOW_STATIC_FIELD =
    HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_FIELD =
    HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RECORD_RETURN_PARAMS_BREAK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE

private const val SHOW_RECORD_STATIC_FIELD =
    HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RECORD_INSTANCE_FIELD =
    HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE

class ConfigBottomSheetFragment(
    private val configBean: ConfigBean,
    private val saveConfig: (ConfigBean) -> Unit,
    private val deleteConfig: () -> Unit
) : BaseBottomFragment<FragemntConfigDialogBinding>() {

    private var hookMode = Constant.HOOK_RETURN
    private var configEnable = true
    private val sp by lazy { SPUtils(requireContext()) }


    override fun init() {
        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        with(binding) {
            saveConfig.setOnClickListener { toCheck() }
            deleteConfig.setOnClickListener {
                deleteConfig()
                dismissAllowingStateLoss()
            }
            deleteConfig.isVisible = tag != "ADD"
            moreSettings.setOnClickListener { popupSettingMenu(it) }
            with(configBean) {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsTypeEdit.setText(params)
                fieldNameEdit.setText(fieldName)
                fieldClassNameEdit.setText(fieldClassName)
                resultValueEdit.setText(resultValues)
                hookPointEdit.setText(hookPoint)
                returnClassNameEdit.setText(returnClassName)
                configItemDescEdit.setText(desc)
                hookMode = mode
                configEnable = enable
            }
            configItemDescInput.isVisible = sp.config_item_show_desc
            onModeChange()
        }
        val list = resources.getStringArray(R.array.config_hook_mode_item)
        val listValue = resources.getIntArray(R.array.config_hook_mode_item_value)
        val realPosition = listValue.indexOf(hookMode)
        binding.modeSelectButton.text = list[realPosition] + ">"
        binding.modeSelectButton.setOnClickListener {
            HookModeViewFragment(hookMode, list) {
                binding.modeSelectButton.text = "${list[it]}>"
                hookMode = listValue[it]
                onModeChange()
            }.show(requireActivity().supportFragmentManager, "config")
        }
    }

    private fun popupSettingMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, GravityCompat.START)
        popupMenu.inflate(R.menu.menu_config_more)
        popupMenu.menu.findItem(R.id.autoXParam).isChecked = sp.auto_x_param
        popupMenu.menu.findItem(R.id.show_desc).isChecked = sp.config_item_show_desc
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.autoXParam -> {
                    it.isChecked = !it.isChecked
                    sp.auto_x_param = it.isChecked
                }

                R.id.show_desc -> {
                    it.isChecked = !it.isChecked
                    sp.config_item_show_desc = it.isChecked
                    binding.configItemDescInput.isVisible = it.isChecked
                }
            }
            true
        }
    }

    private fun toCheck() {
        val className = smali2Java(binding.classNameEdit.text.toString().trim())
        val methodName = binding.methodNameEdit.text.toString().trim()
        val params = tranParams(binding.paramsTypeEdit.text.toString().trim())
        val results = binding.resultValueEdit.text.toString().trim()
        val fieldName = binding.fieldNameEdit.text.toString().trim()
        val fieldClassName = tranParams(binding.fieldClassNameEdit.text.toString())
        val hookPoint = binding.hookPointEdit.text.toString().trim().let {
            if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                ""
            } else {
                if (it == "before") it else "after"
            }
        }
        val returnClassName = smali2Java(binding.returnClassNameEdit.text.toString().trim())
        val configDesc = binding.configItemDescEdit.text.toString().trim()
        var stateCheck = getCheckStateMode(this.hookMode)
        if (className.isNotEmpty()) stateCheck = stateCheck and CLASS_NAME_STATE.inv()
        if (methodName.isNotEmpty()) stateCheck = stateCheck and METHOD_NAME_STATE.inv()
        if (params.isNotEmpty()) stateCheck = stateCheck and PARAMS_STATE.inv()
        if (results.isNotEmpty()) stateCheck = stateCheck and RESULT_VALUE_STATE.inv()
        if (fieldName.isNotEmpty()) stateCheck = stateCheck and FIELD_NAME_STATE.inv()
        if (fieldClassName.isNotEmpty()) stateCheck = stateCheck and FIELD_CLASS_NAME_STATE.inv()
        if (hookPoint.isNotEmpty()) stateCheck = stateCheck and HOOK_POINT_STATE.inv()
        if (returnClassName.isNotEmpty()) stateCheck = stateCheck and RETURN_CLASS_NAME.inv()
        val canCancel = stateCheck == 0
        if (canCancel) {
            if (methodName == "<init>" && (hookMode == Constant.HOOK_RETURN || hookMode == Constant.HOOK_BREAK)) {
                requireActivity().showPopup(getString(R.string.config_hook_constructor_tip))
            }
            val configBean = ConfigBean(
                this.hookMode,
                className,
                methodName,
                params,
                fieldName,
                fieldClassName,
                results,
                hookPoint = hookPoint,
                returnClassName = returnClassName,
                desc = configDesc,
                enable = configEnable
            )
            saveConfig(configBean)
            dismiss()
        } else {
            requireActivity().showPopup(getString(R.string.config_info_not_match_mode))
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
        if (!sp.auto_x_param) return params
        val isSmali = params.contains(Regex("[/;]")) || isPrimitiveType(params)
        if (!isSmali || params.isEmpty()) return params
        var paramStr = params
        val json = "<ON>"
        if (params.contains("JSON")) {
            paramStr = paramStr.replace("JSON", json)
        }
        paramStr = paramStr.replace("[]", "防止加逗号")
        paramStr = paramStr.replace("[", ",[")
        paramStr = paramStr.replace("防止加逗号", "[]")
        paramStr = paramStr.replace("VERSION", "防止加逗号")
        while (paramStr.contains(Regex(pattern_basic))) {
            paramStr = paramStr.replace(Regex(pattern_basic), "$1,$2")
        }
        paramStr = paramStr.replace("防止加逗号", "VERSION")
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
            isSmali =
                paramArray[i].contains(Regex("""[BSIJFDZC]""")) || paramArray[i].contains(
                    Regex(
                        pattern_basic_array
                    )
                ) || paramArray[i].isEmpty()
        }
        return isSmali
    }

    private fun onModeChange() {
        val checkStateMode = getShowStateMode(hookMode)
        binding.apply {
            showView(
                checkStateMode isContainState METHOD_NAME_STATE,
                methodNameInput,
                methodNameEdit
            )
            showView(checkStateMode isContainState PARAMS_STATE, paramsTypeInput, paramsTypeEdit)
            showView(checkStateMode isContainState FIELD_NAME_STATE, fieldNameInput, fieldNameEdit)
            showView(
                checkStateMode isContainState FIELD_CLASS_NAME_STATE,
                fieldClassNameInput,
                fieldClassNameEdit
            )
            showView(
                checkStateMode isContainState RESULT_VALUE_STATE,
                resultValueInput,
                resultValueEdit
            )
            showView(checkStateMode isContainState HOOK_POINT_STATE, hookPointInput, hookPointEdit)
            showView(
                checkStateMode isContainState RETURN_CLASS_NAME,
                returnClassNameInput,
                returnClassNameEdit
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
        Constant.HOOK_RECORD_INSTANCE_FIELD -> HOOK_RECORD_FIELD_CHECK
        Constant.HOOK_STATIC_FIELD -> HOOK_STATIC_FIELD_CHECK
        Constant.HOOK_RECORD_STATIC_FIELD -> HOOK_RECORD_STATIC_FIELD_CHECK
        Constant.HOOK_RECORD_RETURN -> RECORD_RETURN_CHECK
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_PARAMS_RETURN -> RECORD_PARAMS_CHECK
        Constant.HOOK_RETURN2 -> HOOK_RETURN2_CHECK
        else -> 0
    }


    private fun getShowStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN, Constant.HOOK_PARAM -> SHOW_RETURN_PARAMS
        Constant.HOOK_FIELD -> SHOW_FIELD
        Constant.HOOK_STATIC_FIELD -> SHOW_STATIC_FIELD
        Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS, Constant.HOOK_BREAK, Constant.HOOK_RECORD_PARAMS_RETURN -> SHOW_RECORD_RETURN_PARAMS_BREAK
        Constant.HOOK_RECORD_STATIC_FIELD -> SHOW_RECORD_STATIC_FIELD
        Constant.HOOK_RECORD_INSTANCE_FIELD -> SHOW_RECORD_INSTANCE_FIELD
        Constant.HOOK_RETURN2 -> SHOW_RETURN2
        else -> 0
    }

}