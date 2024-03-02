package me.simpleHook.ui.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import me.simpleHook.R


class MaterialSwitchPreference : SwitchPreferenceCompat {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        widgetLayoutResource = R.layout.material_switch
    }
}