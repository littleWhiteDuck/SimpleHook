package me.simpleHook.ui.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
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
import java.lang.reflect.ParameterizedType


abstract class BaseFragment<VB : ViewBinding> : Fragment(), IBinding<VB> {

    private var _binding: VB? = null

    override val binding: VB get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    abstract fun init()

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

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewBinding> Any.inflateBinding(inflater: LayoutInflater): T {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<T>>()
            .first().getDeclaredMethod("inflate", LayoutInflater::class.java)
            .also { it.isAccessible = true }.invoke(null, inflater) as T
    }

}




