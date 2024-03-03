package me.simpleHook.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType


abstract class BaseVBFragment<VB : ViewBinding> : BaseFragment(), IBinding<VB> {

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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true

    abstract fun init()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewBinding> Any.inflateBinding(inflater: LayoutInflater): T {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<T>>()
            .first().getDeclaredMethod("inflate", LayoutInflater::class.java)
            .also { it.isAccessible = true }.invoke(null, inflater) as T
    }

}




