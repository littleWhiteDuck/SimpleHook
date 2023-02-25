package me.simpleHook.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType


abstract class BaseFragment<VB : ViewBinding> : Fragment(), IBinding<VB> {

    private var _binding: VB? = null

    override val binding: VB get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    abstract fun init()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewBinding> Any.inflateBinding(inflater: LayoutInflater): T {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<T>>()
            .first().getDeclaredMethod("inflate", LayoutInflater::class.java)
            .also { it.isAccessible = true }.invoke(null, inflater) as T
    }

}




