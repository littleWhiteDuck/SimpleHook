package me.simpleHook.recyclerview.delegate

import android.content.Context
import androidx.core.widget.doAfterTextChanged
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.ui.view.config.CollectionEnviItem

class CollectionEnviDelegate(private val afterTextChange: (key: String, value: String) -> Unit) :
    ViewDelegate<String, CollectionEnviItem>() {
    override fun onBindView(view: CollectionEnviItem, item: String) {
        view.textInputLayout.helperText = item
        view.editText.doAfterTextChanged {
            afterTextChange(item, it.toString())
        }
    }

    override fun onCreateView(context: Context): CollectionEnviItem {
        return CollectionEnviItem(context)
    }
}