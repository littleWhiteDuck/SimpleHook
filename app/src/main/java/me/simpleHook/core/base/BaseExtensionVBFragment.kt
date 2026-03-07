package me.simpleHook.core.base

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.viewbinding.ViewBinding
import me.simpleHook.R
import me.simpleHook.core.GlobalValue
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.contract.OpenDocumentTreeContract
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.ui.custom.requestPermissionDialog
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.core.utils.OSUtil
import me.simpleHook.core.utils.PermissionUtil
import me.simpleHook.core.utils.SPUtil


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
        } else if (GlobalValue.isRootWork) {
            requireActivity().showPopup(getString(R.string.root_version_no_permission))
        } else if (GlobalValue.isShizukuWork) {
            requireActivity().showPopup(getString(R.string.no_shizuku_tip))
        } else {
            if (OSUtil.atLeastT()) {
                requestPermissionDialog(requireContext()) {
                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
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
