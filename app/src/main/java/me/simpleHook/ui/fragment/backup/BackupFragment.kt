package me.simpleHook.ui.fragment.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
import me.simpleHook.extension.showToast
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.worker.BackupWorkerHelper
import java.util.zip.ZipInputStream

class BackupFragment : PreferenceFragmentCompat() {

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
        findPreference<Preference>("backup_path")?.apply {
            summary =
                GlobalValue.sp.backup_path.toString().ifEmpty { "选择一个路径进行备份，推荐Android/Documents" }
            setOnPreferenceClickListener {
                selectBackupPath()
                true
            }
        }
        findPreference<Preference>("backup")?.apply {
            setOnPreferenceClickListener {
                startBackupConfig()
                true
            }
        }
        findPreference<Preference>("restore")?.apply {
            setOnPreferenceClickListener {
                showRestoreFragment()
                true
            }
        }
    }

    private fun showRestoreFragment() {
        if (GlobalValue.sp.backup_path.isNullOrEmpty()) {
            requireContext().showToast("请先选择备份目录")
            return
        }
        BackupRestoreFragment {
            showRestoreConfigConfirmDialog(it)
        }.show(requireActivity().supportFragmentManager, "restore")
    }


    private fun showRestoreConfigConfirmDialog(restoreItem: RestoreItem) {
        customDialog(requireContext(),
            title = "恢复",
            message = "是否恢复：${restoreItem.name}, 备份于：${restoreItem.time}",
            cancelText = "取消",
            okText = "恢复",
            okClick = {
                restoreConfigFromUri(restoreItem.uri)
            }).show()
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

    private fun startBackupConfig() {
        if (GlobalValue.sp.backup_path.isNullOrEmpty()) {
            requireContext().showToast("请先选择备份目录")
            return
        }
        val loadingDialog = LoadingDialog(requireActivity(), "saving... ")
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            BackupWorkerHelper.nowBackupConfig(requireContext())
            val scope = GlobalValue.sp.backup_scope
            val success = BackupWorkerHelper.outConfigs(requireContext(),
                scope == "custom",
                scope == "extension",
                scope == "all")
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


    private fun updatePath(uri: String) {
        GlobalValue.sp.backup_path = uri
        findPreference<Preference>("backup_path")?.summary = uri
    }

    private fun selectBackupPath() {
        startActivityForData.launch(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments"))
    }

}