package me.simpleHook.util

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey

@GlideModule
class IconGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(String::class.java, Drawable::class.java, IconModelLoaderFactory(context))
    }


}

class IconModelLoaderFactory(private val context: Context) : ModelLoaderFactory<String, Drawable> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, Drawable> {
        return IconModelLoader(context)
    }

    override fun teardown() {

    }

}

class IconModelLoader(private val context: Context) : ModelLoader<String, Drawable> {
    override fun buildLoadData(
        model: String, width: Int, height: Int, options: Options
    ): ModelLoader.LoadData<Drawable> {
        return ModelLoader.LoadData(
            ObjectKey(model),
            IconDataFetcher(context = context, packageName = model)
        )
    }

    override fun handles(model: String) = true

}

class IconDataFetcher(private val context: Context, private val packageName: String) :
    DataFetcher<Drawable> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        val icon = AppUtils.getIcon(context, packageName)
        callback.onDataReady(icon)
    }

    override fun cleanup() {

    }

    override fun cancel() {

    }

    override fun getDataClass(): Class<Drawable> {
        return Drawable::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }

}