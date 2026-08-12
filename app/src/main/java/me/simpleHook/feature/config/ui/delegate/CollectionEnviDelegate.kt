package me.simpleHook.feature.config.ui.delegate

import android.content.Context
import androidx.core.widget.doAfterTextChanged
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.feature.config.ui.view.CollectionEnviItem

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