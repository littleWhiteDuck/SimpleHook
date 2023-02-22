package me.simpleHook.ui.fragment

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import me.simpleHook.R
import me.simpleHook.compat.ConfigSystemUtil
import me.simpleHook.compat.DocumentCompatUtils
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.util.*

open class BaseFragment : Fragment() {
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
                    val uri = DocumentCompatUtils.generateAppUri(packageName)
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