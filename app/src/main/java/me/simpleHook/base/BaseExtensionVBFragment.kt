package me.simpleHook.base

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.viewbinding.ViewBinding
import me.simpleHook.R
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.extension.showPopup
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.utils.OSUtil
import me.simpleHook.utils.PermissionUtil
import me.simpleHook.utils.SPUtil


abstract class BaseExtensionVBFragment<VB : ViewBinding> : BaseVBFragment<VB>() {

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
        onBackPressedCallback.isEnabled = true
    }

    protected val sp by lazy { SPUtil(requireContext()) }

    protected val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            PermissionUtil.takePersistableUriPermission(requireContext(), uri)
        }


    protected fun requirePermission(packageName: String) {
        if (FlavorUtil.liteVersion) {
            requireActivity().showPopup(getString(R.string.lite_version_not_active))
        } else if (FlavorUtil.rootVersion) {
            requireActivity().showPopup(getString(R.string.root_version_no_permission))
        } else {
            if (OSUtil.atLeastT()) {
                requestPermissionDialog(requireContext(),
                    message = getString(R.string.android_13_no_permission)) {
                    val uri = DocumentCompat.generateAppUri(packageName)
                    startActivityForData.launch(uri)
                }
            } else if (OSUtil.atLeastR()) {
                requestPermissionDialog(requireContext()) {
                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                }
            } else {
                requestPermissionDialog(requireContext()) {
                    PermissionUtil.verifyStoragePermissions(requireActivity())
                }
            }
        }
    }

}




