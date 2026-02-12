package me.simpleHook.feature.extension.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.core.base.BaseVBFragment
import me.simpleHook.data.EXtGuiseSignItem
import me.simpleHook.databinding.FragmentGuiseSignBinding
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.showPopup
import me.simpleHook.platform.hook.utils.HookUtils.byte2Sting
import me.simpleHook.feature.applist.ui.AppListActivity
import me.simpleHook.core.ui.custom.LoadingDialog
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.custom.exitDialog
import me.simpleHook.feature.extension.ui.view.EditSignatureView
import me.simpleHook.feature.extension.ui.view.GuiseSignatureItem
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.FileUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.extension.viewmodel.ExViewModel
import java.io.IOException
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


class GuiseSignVBFragment : BaseVBFragment<FragmentGuiseSignBinding>() {
    private val viewModel by activityViewModels<ExViewModel>()
    private val appSignItems = ArrayList<EXtGuiseSignItem>()
    private val navController: NavController by lazy { findNavController() }
    private val startActivityForAppInfo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data!!
                val appName = data.getStringExtra("appName")!!
                val packageName = data.getStringExtra("packageName")!!
                addApp(appName, packageName)
            }
        }
    private val readApkSign =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
            resultUri?.let {
                readApkSignInfo(it)
            }
        }

    private val adapter by lazy {
        GuiseSignAdapter(
            onClick = { onItemClick(it) },
            onCheckedChange = { position: Int, checked: Boolean ->
                onItemChanged(position, checked)
            })
    }


    private val loadingDialog by lazy { LoadingDialog(requireActivity(), "loading...") }
    private val cacheFile by lazy {
        requireActivity().getExternalFilesDir(null)!!.resolve("apk").also {
            it.mkdir()
        }
    }
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (appSignItems == viewModel.extensionConfig.value!!.signConfig.guiseSign.signConfigs) {
                    onBackPressedCallback.isEnabled = false
                    dispatcher.onBackPressed()
                } else {
                    exitDialog(context, okClick = { saveGuiseInfo(exit = true) }, neutralClick = {
                        onBackPressedCallback.isEnabled = false
                        dispatcher.onBackPressed()
                    }, cancelClick = {
                        saveGuiseInfo(false)
                    })
                }
            }
        }
        dispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun init() {
        initMenu()
        initView()
        initData()
    }


    private fun saveGuiseInfo(exit: Boolean) {
        viewModel.updateGuiseSign(appSignItems)
        if (exit) navController.navigateUp()
    }


    private fun initData() {
        appSignItems.addAll(viewModel.extensionConfig.value!!.signConfig.guiseSign.signConfigs)
        notifyDataSetChanged()
        binding.progressBar.isVisible = false
        binding.tip.isVisible = appSignItems.isEmpty()
    }

    private fun initView() {
        with(binding) {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            add.setOnClickListener {
                startActivityForAppInfo.launch(
                    Intent(
                        requireContext(),
                        AppListActivity::class.java
                    )
                )
            }
        }
        val layoutParams = binding.add.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture =
                navigationInsets.bottom <= 20 * requireActivity().resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(binding.root, windowInsets)
            var paddingBottom = 0
            if (navigationInsets.bottom == 0) paddingBottom += 10.dp
            paddingBottom = if (isGesture) {
                paddingBottom + navigationInsets.bottom
            } else {
                paddingBottom + navigationInsets.bottom
            }
            layoutParams.bottomMargin =
                if (isGesture) paddingBottom + navigationInsets.bottom else paddingBottom + navigationInsets.bottom
            binding.add.layoutParams = layoutParams
            windowInsets
        }
    }

    private fun readApkSignInfo(uri: Uri) {
        loadingDialog.show()
        runCatching {
            var result: String? = null
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val apk = DocumentFile.fromSingleUri(requireContext(), uri)
                    ?: throw IOException("IO is null")
                val outputFile = cacheFile.resolve(apk.name!!)
                val input = requireActivity().contentResolver.openInputStream(uri)
                    ?: throw IOException("uri is null")
                input.use {
                    outputFile.outputStream().use { output ->
                        it.copyTo(output)
                    }
                }
                ZipFile(outputFile).use { zipFile ->
                    val entries: Enumeration<out ZipEntry> = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.matches(Regex("(META-INF/.*)\\.(RSA|DSA|EC)"))) {
                            val `is`: InputStream = zipFile.getInputStream(entry)
                            val certFactory = CertificateFactory.getInstance("X509")
                            val x509Cert = certFactory.generateCertificate(`is`) as X509Certificate
                            result = byte2Sting(x509Cert.encoded)
                            break
                        }
                    }
                }
                FileUtil.deleteDir(cacheFile)
                withContext(Dispatchers.Main) {
                    viewModel.signInfoEdit.value = result ?: ""
                    loadingDialog.dismiss()
                }
            }

        }.onFailure {
            requireActivity().showPopup("失败")
        }.recoverCatching {
            FileUtil.deleteDir(cacheFile)
        }
    }

    private fun onItemChanged(position: Int, checked: Boolean) {
        appSignItems[position].enable = checked
    }


    private fun onItemClick(position: Int) {
        val appInfo = appSignItems[position]
        val editSignatureView = EditSignatureView(requireContext()).apply {
            changeSignButton.setOnClickListener {
                loadingDialog.parentView = this
                readApkSign.launch(arrayOf("application/vnd.android.package-archive"))
            }
        }
        viewModel.signInfoEdit.observe(viewLifecycleOwner) {
            editSignatureView.editText.setText(it)
        }
        viewModel.signInfoEdit.value = appInfo.signData
        customDialog(
            requireContext(),
            title = getString(R.string.extension_guise_update_sign),
            contentView = editSignatureView,
            okText = getString(R.string.extension_guise_update),
            okClick = {
                appInfo.signData = editSignatureView.editText.text.toString()
                notifyDataSetChanged(position)
            },
            cancelText = getString(R.string.dialog_cancel),
            cancelAble = false,
            neutralText = getString(R.string.extension_guise_delete),
            neutralClick = {
                appSignItems.remove(appInfo)
                notifyDataSetChanged(position)
            }).show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDataSetChanged(position: Int? = null) {
        adapter.items = appSignItems
        position?.let {
            adapter.notifyItemChanged(position)
        } ?: adapter.notifyDataSetChanged()
    }


    @Suppress("DEPRECATION")
    private fun addApp(appName: String, packageName: String) {
        val packInfo =
            GlobalValue.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        appSignItems.add(
            EXtGuiseSignItem(
                appName = appName,
                packageName = packageName,
                signData = packInfo.signatures!![0].toCharsString()
            )
        )
        notifyDataSetChanged(appSignItems.size)
        binding.tip.isVisible = false
    }


    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_extension_guise_sign, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_save -> saveGuiseInfo(exit = true)
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}

class GuiseSignAdapter(
    val onClick: (position: Int) -> Unit,
    val onCheckedChange: (position: Int, checked: Boolean) -> Unit
) : RecyclerView.Adapter<GuiseSignAdapter.ViewHolder>() {

    var items = ArrayList<EXtGuiseSignItem>()

    inner class ViewHolder(view: GuiseSignatureItem) : RecyclerView.ViewHolder(view) {
        val icon = view.containerView.icon
        val appName = view.containerView.appName
        val packageName = view.containerView.packageName
        val signMd5 = view.containerView.otherInfo
        val checkBox = view.containerView.checkBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val guiseSignatureItem = GuiseSignatureItem(parent.context)
        guiseSignatureItem.setOnClickListener {
            val position = it.getTag(R.id.item_guise_sign_position) as Int
            onClick(position)
        }
        guiseSignatureItem.containerView.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val position = guiseSignatureItem.getTag(R.id.item_guise_sign_position) as Int
            onCheckedChange(position, isChecked)
        }
        return ViewHolder(guiseSignatureItem)
    }

    override fun getItemCount() = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = items[position]
        holder.itemView.setTag(R.id.item_guise_sign_position, position)
        with(holder) {
            appName.text = appInfo.appName
            packageName.text = appInfo.packageName
            signMd5.text =
                "MD5:" + ToolUtil.getDigest(Signature(appInfo.signData).toByteArray())
                    .uppercase()
            icon.setImageDrawable(AppUtil.getIcon(appInfo.packageName))
            checkBox.isChecked = appInfo.enable
        }
    }

}