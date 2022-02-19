package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment

class HelpDialogFragment(private val url: String) : DialogFragment() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val webView = WebView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        webView.loadUrl(url)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val javaScript = """javascript:function hideBottom(){
                        var elements = document.getElementsByTagName("div");
                        for (var i = 0; i < elements.length; i++) {
                            if (elements[i].style.marginBottom == "30px") {
                                elements[i].style.display = "none";
                                break;
                            }
                        }
                    }
                """.trimIndent()
                webView.loadUrl(javaScript)
                webView.loadUrl("javascript:hideBottom();")
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }

        }
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