package me.simpleHook.core.extension

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

@Suppress("UNCHECKED_CAST")
internal fun <T : ViewBinding> Any.inflateBinding(inflater: LayoutInflater): T {
    return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<T>>()
        .first().getDeclaredMethod("inflate", LayoutInflater::class.java)
        .also { it.isAccessible = true }.invoke(null, inflater) as T
}

/*
@BindingAdapter("src")
fun setImageDrawable(imageView: ImageView, icon: Drawable?) {
    icon?.let {
        imageView.setImageDrawable(it)
    } ?: imageView.setImageResource(R.drawable.about_developer)
}*/
