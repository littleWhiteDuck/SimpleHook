package me.simpleHook.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.viewbinding.ViewBinding
import me.simpleHook.R
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.extension.showToast
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.OSUtils
import me.simpleHook.util.PermissionUtils
import me.simpleHook.util.SPUtils


abstract class BaseExtensionFragment<VB : ViewBinding> : BaseFragment<VB>() {

    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(enableCallback()) {
            override fun handleOnBackPressed() {
                if (canBack()) {
                    performBack()
                } else {
                    notBackTip()
                }
            }
        }
        dispatcher.addCallback(this, onBackPressedCallback)
    }

    abstract fun canBack(): Boolean
    abstract fun performBack()
    abstract fun notBackTip()
    abstract fun enableCallback(): Boolean

    protected fun backPressed() {
        onBackPressedCallback.isEnabled = false
        dispatcher.onBackPressed()
    }

    protected val sp by lazy { SPUtils(requireContext()) }

    protected val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireActivity().contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }


    protected fun requirePermission(packageName: String) {
        if (FlavorUtils.liteVersion) {
            requireActivity().showToast(getString(R.string.lite_version_not_active))
        } else if (FlavorUtils.rootVersion) {
            requireActivity().showToast(getString(R.string.root_version_no_permission))
        } else {
            if (OSUtils.atLeastT()) {
                requestPermissionDialog(requireContext(),
                    message = getString(R.string.android_13_no_permission)) {
                    val uri = DocumentCompat.generateAppUri(packageName)
                    startActivityForData.launch(uri)
                }
            } else if (OSUtils.atLeastR()) {
                requestPermissionDialog(requireContext()) {
                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                }
            } else {
                requestPermissionDialog(requireContext()) {
                    PermissionUtils.verifyStoragePermissions(requireActivity())
                }
            }
        }
    }

}




