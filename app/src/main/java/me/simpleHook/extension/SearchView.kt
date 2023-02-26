package me.simpleHook.extension

import androidx.appcompat.widget.SearchView
import me.simpleHook.R

fun SearchView.setTextColor(color: Int) {
    val searchAutoComplete: SearchView.SearchAutoComplete = findViewById(R.id.search_src_text)
    searchAutoComplete.setHintTextColor(color)
    searchAutoComplete.setTextColor(color)
}