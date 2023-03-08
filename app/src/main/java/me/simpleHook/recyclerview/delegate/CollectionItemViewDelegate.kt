package me.simpleHook.recyclerview.delegate

import android.content.Context
import com.drakeet.multitype.ViewDelegate
import me.simpleHook.R
import me.simpleHook.database.entity.CollectionEntity
import me.simpleHook.ui.view.config.CollectionItemView

class CollectionItemViewDelegate(private val onClick: (CollectionEntity) -> Unit) :
    ViewDelegate<CollectionEntity, CollectionItemView>() {
    override fun onBindView(view: CollectionItemView, item: CollectionEntity) {
        view.title.text = item.name
        view.desc.text = item.config
        view.icon.setImageResource(R.drawable.ic_cloud_done_24)
        view.setOnClickListener {
            onClick(item)
        }
    }

    override fun onCreateView(context: Context): CollectionItemView {
        return CollectionItemView(context)
    }
}