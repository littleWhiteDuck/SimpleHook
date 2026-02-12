package me.simpleHook.core.utils

import android.content.Context
import me.simpleHook.R
import me.simpleHook.core.constant.Constant

object HookModeUtil {
    fun getShowText(mode: Int, context: Context): String {
        return when (mode) {
            Constant.HOOK_RETURN -> context.getString(R.string.config_mode_tip_return_value)
            Constant.HOOK_PARAM -> context.getString(R.string.config_mode_tip_param_value)
            Constant.HOOK_STATIC_FIELD -> context.getString(R.string.config_mode_tip_static_field_value)
            Constant.HOOK_FIELD -> context.getString(R.string.config_mode_tip_field_value)
            Constant.HOOK_BREAK -> context.getString(R.string.config_mode_tip_break)
            Constant.HOOK_RECORD_PARAMS -> context.getString(R.string.config_mode_tip_record_param_value)
            Constant.HOOK_RECORD_RETURN -> context.getString(R.string.config_mode_tip_record_return_value)
            Constant.HOOK_RECORD_PARAMS_RETURN -> context.getString(R.string.config_mode_tip_record_param_return_value)
            Constant.HOOK_RECORD_STATIC_FIELD -> context.getString(R.string.config_mode_tip_record_static_field_value)
            Constant.HOOK_RECORD_INSTANCE_FIELD -> context.getString(R.string.config_mode_tip_record_instance_field_value)
            Constant.HOOK_RETURN2 -> context.getString(R.string.config_mode_tip_return2_value)
            else -> "未知"
        }
    }
}