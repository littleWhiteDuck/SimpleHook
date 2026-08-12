package me.simpleHook.feature.config.ui

import me.simpleHook.core.constant.Constant

internal object ConfigModeState {

    const val CLASS_NAME = 1
    const val METHOD_NAME = 1 shl 1
    const val PARAMS = 1 shl 2
    const val RESULT_VALUE = 1 shl 3
    const val FIELD_NAME = 1 shl 4
    const val FIELD_CLASS_NAME = 1 shl 5
    const val HOOK_POINT = 1 shl 6
    const val RETURN_CLASS_NAME = 1 shl 7

    private const val HOOK_RETURN_CHECK = CLASS_NAME or METHOD_NAME or RESULT_VALUE
    private const val HOOK_RETURN2_CHECK =
        CLASS_NAME or METHOD_NAME or RESULT_VALUE or RETURN_CLASS_NAME
    private const val HOOK_PARAM_CHECK =
        CLASS_NAME or METHOD_NAME or RESULT_VALUE or PARAMS
    private const val HOOK_BREAK_CHECK = CLASS_NAME or METHOD_NAME
    private const val HOOK_STATIC_FIELD_CHECK =
        FIELD_NAME or RESULT_VALUE or FIELD_CLASS_NAME
    private const val HOOK_RECORD_STATIC_FIELD_CHECK = FIELD_NAME or FIELD_CLASS_NAME
    private const val HOOK_FIELD_CHECK =
        CLASS_NAME or FIELD_NAME or RESULT_VALUE or METHOD_NAME
    private const val HOOK_RECORD_FIELD_CHECK =
        CLASS_NAME or FIELD_NAME or METHOD_NAME
    private const val RECORD_RETURN_CHECK = CLASS_NAME or METHOD_NAME
    private const val RECORD_PARAMS_CHECK = CLASS_NAME or METHOD_NAME or PARAMS

    private const val SHOW_RETURN_PARAMS =
        CLASS_NAME or METHOD_NAME or RESULT_VALUE or PARAMS
    private const val SHOW_RETURN2 =
        CLASS_NAME or METHOD_NAME or RESULT_VALUE or PARAMS or RETURN_CLASS_NAME
    private const val SHOW_STATIC_FIELD =
        HOOK_POINT or CLASS_NAME or FIELD_NAME or RESULT_VALUE or FIELD_CLASS_NAME or METHOD_NAME or PARAMS
    private const val SHOW_FIELD =
        HOOK_POINT or CLASS_NAME or FIELD_NAME or RESULT_VALUE or METHOD_NAME or PARAMS
    private const val SHOW_RECORD_RETURN_PARAMS_BREAK =
        CLASS_NAME or METHOD_NAME or PARAMS
    private const val SHOW_RECORD_STATIC_FIELD =
        HOOK_POINT or CLASS_NAME or FIELD_NAME or FIELD_CLASS_NAME or METHOD_NAME or PARAMS
    private const val SHOW_RECORD_INSTANCE_FIELD =
        HOOK_POINT or CLASS_NAME or FIELD_NAME or METHOD_NAME or PARAMS

    fun requiredState(mode: Int): Int = when (mode) {
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

    fun showState(mode: Int): Int = when (mode) {
        Constant.HOOK_RETURN, Constant.HOOK_PARAM -> SHOW_RETURN_PARAMS
        Constant.HOOK_FIELD -> SHOW_FIELD
        Constant.HOOK_STATIC_FIELD -> SHOW_STATIC_FIELD
        Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS, Constant.HOOK_BREAK, Constant.HOOK_RECORD_PARAMS_RETURN -> SHOW_RECORD_RETURN_PARAMS_BREAK
        Constant.HOOK_RECORD_STATIC_FIELD -> SHOW_RECORD_STATIC_FIELD
        Constant.HOOK_RECORD_INSTANCE_FIELD -> SHOW_RECORD_INSTANCE_FIELD
        Constant.HOOK_RETURN2 -> SHOW_RETURN2
        else -> 0
    }

    fun unresolvedState(
        mode: Int,
        className: String,
        methodName: String,
        params: String,
        resultValues: String,
        fieldName: String,
        fieldClassName: String,
        hookPoint: String,
        returnClassName: String
    ): Int {
        var state = requiredState(mode)
        if (className.isNotEmpty()) state = state and CLASS_NAME.inv()
        if (methodName.isNotEmpty()) state = state and METHOD_NAME.inv()
        if (params.isNotEmpty()) state = state and PARAMS.inv()
        if (resultValues.isNotEmpty()) state = state and RESULT_VALUE.inv()
        if (fieldName.isNotEmpty()) state = state and FIELD_NAME.inv()
        if (fieldClassName.isNotEmpty()) state = state and FIELD_CLASS_NAME.inv()
        if (hookPoint.isNotEmpty()) state = state and HOOK_POINT.inv()
        if (returnClassName.isNotEmpty()) state = state and RETURN_CLASS_NAME.inv()
        return state
    }
}
