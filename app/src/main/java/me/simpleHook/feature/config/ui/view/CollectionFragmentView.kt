package me.simpleHook.feature.config.ui.view

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import me.simpleHook.R
import me.simpleHook.core.extension.dp

class CollectionFragmentView(context: Context) : LinearLayoutCompat(context) {

    @Suppress("DEPRECATION")
    private val title =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_title)).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 16.dp
            }
            text = context.getString(R.string.config_label_collection)
            setTextColor(context.resources.getColor(R.color.normal_text_color))
        }

    val listView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).also {
            it.topMargin = 5.dp
        }
        isVerticalScrollBarEnabled = false
        setPadding(0, 0, 0, 20.dp)
        clipToPadding = false
        layoutManager = LinearLayoutManager(context)
    }

    val progressBar = LinearProgressIndicator(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = 16.dp
            marginEnd = 16.dp
            topMargin = 5.dp
        }
        isIndeterminate = true

    }

    init {
        addView(title)
        addView(progressBar)
        addView(listView)
        setPadding(0, 20.dp, 0, 5.dp)
        orientation = VERTICAL
    }

}