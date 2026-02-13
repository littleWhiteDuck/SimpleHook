package me.simpleHook.feature.config.ui

import android.annotation.SuppressLint
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.core.base.BaseBottomFragment
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.extension.isContainState
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.utils.SPUtil
import me.simpleHook.core.utils.SmaliSignatureParser
import me.simpleHook.data.HookConfig
import me.simpleHook.databinding.FragemntConfigDialogBinding

class ConfigBottomFragment(
    private val hookConfig: HookConfig,
    private val saveConfig: (HookConfig) -> Unit,
    private val deleteConfig: () -> Unit
) : BaseBottomFragment<FragemntConfigDialogBinding>() {

    private var hookMode = Constant.HOOK_RETURN
    private var configEnable = true
    private val sp by lazy { SPUtil(requireContext()) }
    override var enableUpdateHeight: Boolean = false

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
            with(hookConfig) {
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
        val className =
            SmaliSignatureParser.classDescriptorToJavaOrSelf(binding.classNameEdit.text.toString().trim())
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
        val returnClassName =
            SmaliSignatureParser.classDescriptorToJavaOrSelf(binding.returnClassNameEdit.text.toString().trim())
        val configDesc = binding.configItemDescEdit.text.toString().trim()
        val stateCheck = ConfigModeState.unresolvedState(
            mode = this.hookMode,
            className = className,
            methodName = methodName,
            params = params,
            resultValues = results,
            fieldName = fieldName,
            fieldClassName = fieldClassName,
            hookPoint = hookPoint,
            returnClassName = returnClassName
        )
        val canCancel = stateCheck == 0
        if (canCancel) {
            if (methodName == "<init>" && (hookMode == Constant.HOOK_RETURN || hookMode == Constant.HOOK_BREAK)) {
                binding.root.showPopup(getString(R.string.config_hook_constructor_tip))
            }
            val hookConfig = HookConfig(
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
            saveConfig(hookConfig)
            dismiss()
        } else {
            binding.root.showPopup(getString(R.string.config_info_not_match_mode))
        }
    }

    private fun tranParams(params: String): String {
        if (!sp.auto_x_param) return params
        return SmaliSignatureParser.toJavaParametersOrSelf(params)
    }

    private fun onModeChange() {
        val checkStateMode = ConfigModeState.showState(hookMode)
        binding.apply {
            showView(
                checkStateMode isContainState ConfigModeState.METHOD_NAME,
                methodNameInput,
                methodNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.PARAMS,
                paramsTypeInput,
                paramsTypeEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.FIELD_NAME,
                fieldNameInput,
                fieldNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.FIELD_CLASS_NAME,
                fieldClassNameInput,
                fieldClassNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.RESULT_VALUE,
                resultValueInput,
                resultValueEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.HOOK_POINT,
                hookPointInput,
                hookPointEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.RETURN_CLASS_NAME,
                returnClassNameInput,
                returnClassNameEdit
            )
        }
    }

    private fun showView(isShow: Boolean, input: TextInputLayout, edit: TextInputEditText) {
        input.visibility = if (isShow) View.VISIBLE else View.GONE
        if (!isShow) edit.setText("")
    }
}
