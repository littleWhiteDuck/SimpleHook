package me.simpleHook.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.preference.PreferenceFragmentCompat

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canBack()) {
                    backPressed()
                } else {
                    notBackTip()
                }
            }
        }
        dispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    abstract fun init()
    abstract fun canBack(): Boolean
    abstract fun notBackTip()

    protected fun backPressed() {
        onBackPressedCallback.isEnabled = false
        dispatcher.onBackPressed()
    }


}

