package me.simpleHook.extension

import android.annotation.SuppressLint
import androidx.appcompat.widget.SearchView
import me.simpleHook.R

@SuppressLint("RestrictedApi")
fun SearchView.setTextColor(color: Int) {
    val searchAutoComplete: SearchView.SearchAutoComplete = findViewById(R.id.search_src_text)
    searchAutoComplete.setHintTextColor(color)
    searchAutoComplete.setTextColor(color)
}