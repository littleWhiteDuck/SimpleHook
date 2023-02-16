package me.simpleHook.ui.custom

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class ChipPreference(context: Context, val onClick: (chip: Chip) -> Unit) : Preference(context) {
    var chipGroup: ChipGroup? = null
    var chipTexts: List<String>? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        chipGroup = holder.itemView as ChipGroup
        chipTexts?.forEach {
            addChip(it)
        }
    }

    fun addChip(keyword: String) {
        chipGroup ?: throw NullPointerException("chip is null")
        val chip = Chip(context).apply {
            text = keyword
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                chipGroup!!.removeView(this)
            }
            setOnClickListener {
                onClick(this)
            }
        }
        chipGroup!!.addView(chip)
    }
}