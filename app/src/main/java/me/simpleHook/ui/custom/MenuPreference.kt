package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import me.simpleHook.R


class MenuPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    var entries = arrayOf<String>()

    init {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MenuPreference)
        val textArray = typeArray.getTextArray(R.styleable.MenuPreference_entries)
        val itemList = mutableListOf<String>()
        textArray.forEach {
            itemList.add(it.toString())
        }
        entries = itemList.toTypedArray()
        typeArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        PopupWindowList.Builder(context).watchView(holder.itemView)
    }

    @SuppressLint("RestrictedApi")
    override fun performClick(view: View?) {
        if (entries.isEmpty()) super.performClick(view)
        val popupWindowList = PopupWindowList.Builder(context)
            .setItemList(entries)
            .setOutsideTouchable(true)
            .build()
        popupWindowList.setOnItemClickListener { _, _, position, _ ->
            callChangeListener(entries[position])
            popupWindowList.dismiss()
        }.show()
    }
}