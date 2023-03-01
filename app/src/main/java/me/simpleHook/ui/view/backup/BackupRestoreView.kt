package me.simpleHook.ui.view.backup

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.extension.dp

class BackupRestoreView(context: Context) : LinearLayoutCompat(context) {

    private val title =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_title)).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 16.dp
            }
            text = "恢复备份"
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

    init {
        addView(title)
        addView(listView)
        setPadding(0, 20.dp, 0, 5.dp)
        orientation = VERTICAL
    }

}