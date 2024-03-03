package me.simpleHook.base

import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(), MenuProvider {
    override fun onResume() {
        super.onResume()
        (activity as? IMenuProvider)?.let { iMenuProvider ->
            if (iMenuProvider.currentMenuProvider != this) {
                iMenuProvider.currentMenuProvider?.let { current ->
                    activity?.removeMenuProvider(current)
                }
                activity?.addMenuProvider(this)
                iMenuProvider.currentMenuProvider = this
            }
        }
    }

}