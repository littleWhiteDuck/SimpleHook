package me.simpleHook.ui.fragment.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.extension.dp
import me.simpleHook.extension.showToast
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.util.TimeUtil
import me.simpleHook.worker.BackupWorkerHelper
import java.util.zip.ZipInputStream

class BackupFragment(private val uri: Uri?) : PreferenceFragmentCompat() {

    private val appViewModel by viewModels<AppViewModel>()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                updatePath(uri.toString())
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.backup_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDividerHeight(0)
        initExternalBackup()
        findPreference<Preference>("backup_path")?.apply {
            summary =
                GlobalValue.sp.backup_path.toString().ifEmpty { "选择一个路径进行备份，推荐Android/Documents" }
            setOnPreferenceClickListener {
                selectBackupPath()
                true
            }
        }
        findPreference<Preference>("backup_local")?.apply {
            setOnPreferenceClickListener {
                startBackupConfig(local = true)
                true
            }
        }
        findPreference<Preference>("backup_cloud")?.apply {
            setOnPreferenceClickListener {
                startBackupConfig(cloud = true)
                true
            }
        }
        findPreference<Preference>("restore_local")?.apply {
            setOnPreferenceClickListener {
                showLocalRestoreFragment()
                true
            }
        }
        findPreference<Preference>("restore_cloud")?.apply {
            setOnPreferenceClickListener {
                showCloudRestoreFragment()
                true
            }
        }
        findPreference<EditTextPreference>("web_dav_host")?.apply {
            summary = GlobalValue.sp.web_dav_host
            setOnBindEditTextListener {
                it.hint = "https://dav.xxx.com/dav/"
                it.setText(GlobalValue.sp.web_dav_host)
            }
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue as String
                true
            }
        }
        findPreference<EditTextPreference>("web_dav_account")?.apply {
            summary = GlobalValue.sp.web_dav_account
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue as String
                true
            }
            setOnBindEditTextListener {
                it.setText(GlobalValue.sp.web_dav_account)
            }
        }
        findPreference<EditTextPreference>("web_dav_pw")?.apply {
            summary = "******"
            setOnBindEditTextListener {
                it.setText(GlobalValue.sp.web_dav_pw)
            }
        }
    }

    private fun initExternalBackup() {
        if (uri != null) {
            val document = DocumentFile.fromSingleUri(requireContext(), uri) ?: return
            showRestoreLocal(RestoreItem(document.name!!,
                document.uri,
                TimeUtil.calculateRangeToNow(requireContext(), document.lastModified())))
        }
    }

    private fun showLocalRestoreFragment() {
        if (isLocalBackupEnable()) {
            BackupRestoreFragment(callBackLocal = {
                showRestoreLocal(it)
            }) {
                showRestoreCloud(it)
            }.show(requireActivity().supportFragmentManager, "RESTORE_LOCAL")
        }
    }

    private fun isLocalBackupEnable(): Boolean {
        if (GlobalValue.sp.backup_path.isNullOrEmpty()) {
            requireContext().showToast("请先选择备份目录")
            return false
        }
        return true
    }

    private fun showCloudRestoreFragment() {
        if (isCloudBackupEnable()) {
            BackupRestoreFragment(callBackLocal = {
                showRestoreLocal(it)
            }) {
                showRestoreCloud(it)
            }.show(requireActivity().supportFragmentManager, "RESTORE_CLOUD")
        }
    }

    private fun isCloudBackupEnable(): Boolean {
        if (GlobalValue.sp.web_dav_account.isNullOrEmpty() || GlobalValue.sp.web_dav_pw.isNullOrEmpty() || GlobalValue.sp.web_dav_host.isNullOrEmpty()) {
            requireContext().showToast("请填写云备份信息")
            return false
        }
        return true
    }

    private fun showRestoreLocal(restoreItem: RestoreItem) {
        customDialog(requireContext(),
            title = "恢复",
            message = "是否恢复：${restoreItem.name}, 备份于：${restoreItem.time}",
            cancelText = "取消",
            okText = "恢复",
            okClick = {
                restoreConfigFromUri(restoreItem.uri)
            }).show()
    }

    private fun showRestoreCloud(restoreItem: RestoreCloudItem) {
        customDialog(requireContext(),
            title = "恢复云备份",
            message = "是否恢复：${restoreItem.name}, 备份于：${restoreItem.time}",
            cancelText = "取消",
            okText = "恢复",
            okClick = {
                restoreConfigFromCloud(restoreItem.url)
            }).show()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun restoreConfigFromCloud(url: String) {
        val loadingDialog = LoadingDialog(requireActivity(), "Recovering... ")
        loadingDialog.show()
        runCatching {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                var customConfigs: List<AppConfig> = emptyList()
                var extensionConfigs: List<AssistConfig> = emptyList()
                val sardine = OkHttpSardine()
                sardine.setCredentials(GlobalValue.sp.web_dav_account, GlobalValue.sp.web_dav_pw)
                val inputStream = sardine.get(url)
                val zipInputStream = ZipInputStream(inputStream)
                zipInputStream.use {
                    if (it.nextEntry.name == "custom_config.json") {
                        customConfigs = Json.decodeFromStream(it)
                    }
                    if (it.nextEntry.name == "extension_config.json") {
                        extensionConfigs = Json.decodeFromStream(it)
                    }
                }
                customConfigs.forEach {
                    it.id = 0
                }
                extensionConfigs.forEach {
                    it.id = 0
                }
                appViewModel.insertConfigs(*customConfigs.toTypedArray())
                appViewModel.insertAssistConfigs(*extensionConfigs.toTypedArray())
            }
        }.onSuccess {
            loadingDialog.dismiss()
            requireActivity().showToast("恢复成功")
        }.onFailure {
            loadingDialog.dismiss()
            requireActivity().showToast("恢复失败")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun restoreConfigFromUri(uri: Uri) {
        val loadingDialog = LoadingDialog(requireActivity(), "Recovering... ")
        loadingDialog.show()
        runCatching {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                var customConfigs: List<AppConfig> = emptyList()
                var extensionConfigs: List<AssistConfig> = emptyList()
                val zipInputStream =
                    ZipInputStream(requireActivity().contentResolver.openInputStream(uri))
                zipInputStream.use {
                    if (it.nextEntry.name == "custom_config.json") {
                        customConfigs = Json.decodeFromStream(it)
                    }
                    if (it.nextEntry.name == "extension_config.json") {
                        extensionConfigs = Json.decodeFromStream(it)
                    }
                }
                customConfigs.forEach {
                    it.id = 0
                }
                extensionConfigs.forEach {
                    it.id = 0
                }
                appViewModel.insertConfigs(*customConfigs.toTypedArray())
                appViewModel.insertAssistConfigs(*extensionConfigs.toTypedArray())
            }
        }.onSuccess {
            loadingDialog.dismiss()
            requireActivity().showToast("恢复成功")
        }.onFailure {
            loadingDialog.dismiss()
            requireActivity().showToast("恢复失败")
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, 0, 0, 40.dp)
        }
        return recyclerView
    }

    private fun startBackupConfig(local: Boolean = false, cloud: Boolean = false) {
        if ((local && isLocalBackupEnable()) || (cloud && isCloudBackupEnable())) {
            val loadingDialog = LoadingDialog(requireActivity(), "saving... ")
            loadingDialog.show()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                BackupWorkerHelper.localBackupConfig(requireContext())
                val scope = GlobalValue.sp.backup_scope
                val success = BackupWorkerHelper.startBackupConfig(requireContext(),
                    scope == "BACKUP_SCOPE_CUSTOM",
                    scope == "BACKUP_SCOPE_EXTENSION",
                    scope == "BACKUP_SCOPE_ALL",
                    local = local,
                    cloud)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (success) {
                        requireActivity().showToast("备份成功")
                    } else {
                        requireActivity().showToast("备份失败")
                    }
                }
            }
        }
    }


    private fun updatePath(uri: String) {
        GlobalValue.sp.backup_path = uri
        findPreference<Preference>("backup_path")?.summary = uri
    }

    private fun selectBackupPath() {
        startActivityForData.launch(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments"))
    }

}