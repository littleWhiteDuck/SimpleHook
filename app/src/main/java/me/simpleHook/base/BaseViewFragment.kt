package me.simpleHook.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.simpleHook.ui.WindowPreferencesManager


abstract class BaseViewFragment<T : View> : Fragment() {

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


}