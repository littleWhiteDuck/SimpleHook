package me.simpleHook.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.DialogFragment

class HelpDialogFragment(private val url: String): DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val webView = WebView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        webView.loadUrl(url)
        return webView
    }

    override fun onResume() {
        super.onResume()
        val window = dialog!!.window
        val layoutParams = window!!.attributes
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        window.attributes = layoutParams
    }
}