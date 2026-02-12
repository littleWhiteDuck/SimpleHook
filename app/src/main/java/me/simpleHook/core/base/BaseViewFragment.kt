package me.simpleHook.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import me.simpleHook.core.ui.WindowPreferencesManager


abstract class BaseViewFragment<T : View> : BaseFragment() {

    private var _root: T? = null
    val root get() = _root!!

    abstract fun initRootView(): T
    abstract fun init()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _root = initRootView()
        WindowPreferencesManager(requireContext()).applyEdgeToEdgePreference(requireActivity().window)
        init()
        return _root
    }


    override fun onDestroyView() {
        _root = null
        super.onDestroyView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true


}