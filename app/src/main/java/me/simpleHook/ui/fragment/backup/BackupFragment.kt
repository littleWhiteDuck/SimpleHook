package me.simpleHook.ui.fragment.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import me.simpleHook.database.entity.CollectionEntity
import me.simpleHook.extension.showToast
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.util.TimeUtil
import me.simpleHook.viewmodel.CollectionViewModel
import me.simpleHook.worker.BackupHelper
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class BackupFragment(private val uri: Uri?) : PreferenceFragmentCompat() {

    private val appViewModel by viewModels<AppViewModel>()
    private val collViewModel by viewModels<CollectionViewModel>()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                updatePath(uri.toString())
            }
        }
    private val restoreConfigs =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
            resultUri?.let {
                restoreConfigBySelect(it)
            }
        }

    private val backupConfigs =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/shbackups")) { resultUri ->
            resultUri?.apply {
                startBackupConfig(local = true, backUri = this)
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
            summary = GlobalValue.sp.backup_path.toString()
                .ifEmpty { getString(R.string.backup_summary_backup_path) }
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
                val value = newValue as String
                if (value.isNotBlank() && Patterns.WEB_URL.matcher(value).matches()) {
                    val result = if (value.endsWith("/")) {
                        value
                    } else {
                        "$value/"
                    }
                    GlobalValue.sp.web_dav_host = result
                    summary = result
                } else {
                    requireActivity().showToast(getString(R.string.url_is_incorrect))
                }
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
        findPreference<Preference>("backup_by_select")?.apply {
            setOnPreferenceClickListener {
                backupConfigBySelect()
                true
            }
        }
        findPreference<Preference>("restore_by_select")?.apply {
            setOnPreferenceClickListener {
                restoreConfigs.launch(arrayOf("*/*"))
                true
            }
        }
    }

    private fun backupConfigBySelect() {
        val time = TimeUtil.getTime(System.currentTimeMillis(), pattern = "yyMMdd")
        backupConfigs.launch("SimpleHook_backup_$time.shbackup")
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
            BackupRestoreViewFragment(callBackLocal = {
                showRestoreLocal(it)
            }) {
                showRestoreCloud(it)
            }.show(requireActivity().supportFragmentManager, "RESTORE_LOCAL")
        }
    }

    private fun isLocalBackupEnable(): Boolean {
        if (GlobalValue.sp.backup_path.isNullOrEmpty() || DocumentFile.fromTreeUri(requireContext(),
                GlobalValue.sp.backup_path!!.toUri())?.exists() == false
        ) {
            requireContext().showToast(getString(R.string.backup_tip_backup_path_is_empty))
            return false
        }
        return true
    }

    private fun showCloudRestoreFragment() {
        if (isCloudBackupEnable()) {
            BackupRestoreViewFragment(callBackLocal = {
                showRestoreLocal(it)
            }) {
                showRestoreCloud(it)
            }.show(requireActivity().supportFragmentManager, "RESTORE_CLOUD")
        }
    }

    private fun isCloudBackupEnable(): Boolean {
        if (GlobalValue.sp.web_dav_account.isNullOrEmpty() || GlobalValue.sp.web_dav_pw.isNullOrEmpty() || GlobalValue.sp.web_dav_host.isNullOrEmpty()) {
            requireContext().showToast(getString(R.string.backup_tip_cloud_info_is_empty))
            return false
        }
        return true
    }

    private fun showRestoreLocal(restoreItem: RestoreItem) {
        customDialog(requireContext(),
            title = getString(R.string.backup_dialog_title_restore_local_backup),
            message = getString(R.string.backup_dialog_message, restoreItem.name, restoreItem.time),
            cancelText = getString(R.string.dialog_cancel),
            okText = getString(R.string.dialog_confirm),
            okClick = {
                restoreConfigFromUri(restoreItem.uri)
            }).show()
    }

    private fun showRestoreCloud(restoreItem: RestoreCloudItem) {
        customDialog(requireContext(),
            title = getString(R.string.backup_dialog_title_restore_cloud_backup),
            message = getString(R.string.backup_dialog_message, restoreItem.name, restoreItem.time),
            cancelText = getString(R.string.dialog_cancel),
            okText = getString(R.string.dialog_confirm),
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
                var collections: List<CollectionEntity> = emptyList()
                val sardine = OkHttpSardine()
                sardine.setCredentials(GlobalValue.sp.web_dav_account, GlobalValue.sp.web_dav_pw)
                val inputStream = sardine.get(url)
                val zipInputStream = ZipInputStream(inputStream)
                zipInputStream.use {
                    var entry: ZipEntry?
                    while ((it.nextEntry.also { zipEntry: ZipEntry? ->
                            entry = zipEntry
                        }) != null) {
                        if (entry?.name == "custom_config.json") {
                            customConfigs = Json.decodeFromStream(it)
                        }
                        if (entry?.name == "extension_config.json") {
                            extensionConfigs = Json.decodeFromStream(it)
                        }
                        if (entry?.name == "collection_config.json") {
                            collections = Json.decodeFromStream(it)
                        }
                    }
                }
                customConfigs.forEach {
                    it.id = 0
                }
                extensionConfigs.forEach {
                    it.id = 0
                }
                collections.forEach {
                    it.id = 0
                }
                appViewModel.insertConfigs(*customConfigs.toTypedArray())
                appViewModel.insertAssistConfigs(*extensionConfigs.toTypedArray())
                collViewModel.insertCollections(*collections.toTypedArray())
            }
        }.onSuccess {
            loadingDialog.dismiss()
            requireActivity().showToast(getString(R.string.backup_tip_restore_success))
        }.onFailure {
            loadingDialog.dismiss()
            requireActivity().showToast(getString(R.string.backup_tip_restore_failure))
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
                var collections: List<CollectionEntity> = emptyList()
                val zipInputStream =
                    ZipInputStream(requireActivity().contentResolver.openInputStream(uri))
                zipInputStream.use {
                    var entry: ZipEntry?
                    while ((it.nextEntry.also { zipEntry: ZipEntry? ->
                            entry = zipEntry
                        }) != null) {
                        if (entry?.name == "custom_config.json") {
                            customConfigs = Json.decodeFromStream(it)
                        }
                        if (entry?.name == "extension_config.json") {
                            extensionConfigs = Json.decodeFromStream(it)
                        }
                        if (entry?.name == "collection_config.json") {
                            collections = Json.decodeFromStream(it)
                        }
                    }
                }
                customConfigs.forEach {
                    it.id = 0
                }
                extensionConfigs.forEach {
                    it.id = 0
                }
                collections.forEach {
                    it.id = 0
                }
                appViewModel.insertConfigs(*customConfigs.toTypedArray())
                appViewModel.insertAssistConfigs(*extensionConfigs.toTypedArray())
                collViewModel.insertCollections(*collections.toTypedArray())

            }
        }.onSuccess {
            loadingDialog.dismiss()
            requireActivity().showToast(getString(R.string.backup_tip_restore_success))
        }.onFailure {
            loadingDialog.dismiss()
            requireActivity().showToast(getString(R.string.backup_tip_restore_failure))
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(recyclerView, windowInsets)
            recyclerView.setPadding(0, 0, 0, navigationInsets.bottom)
            windowInsets
        }
        return recyclerView
    }

    private fun startBackupConfig(
        local: Boolean = false, cloud: Boolean = false, backUri: Uri? = null
    ) {
        if ((backUri != null || local && isLocalBackupEnable()) || (cloud && isCloudBackupEnable())) {
            val loadingDialog = LoadingDialog(requireActivity(), "saving... ")
            loadingDialog.show()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val scope = GlobalValue.sp.backup_scope
                val success = BackupHelper.startBackupConfig(requireContext(),
                    scope == "BACKUP_SCOPE_CUSTOM",
                    scope == "BACKUP_SCOPE_EXTENSION",
                    scope == "BACKUP_SCOPE_COLLECTION",
                    scope == "BACKUP_SCOPE_ALL",
                    local = local,
                    cloud = cloud,
                    backUri = backUri)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (success) {
                        requireActivity().showToast(getString(R.string.backup_tip_backup_success))
                    } else {
                        requireActivity().showToast(getString(R.string.backup_tip_backup_failure))
                    }
                }
            }
        }
    }

    private fun restoreConfigBySelect(it: Uri) {
        val file = DocumentFile.fromSingleUri(requireContext(), it) ?: return
        if (file.name?.endsWith(".shbackup") == true) {
            showRestoreLocal(RestoreItem(file.name!!,
                file.uri,
                TimeUtil.calculateRangeToNow(requireContext(), file.lastModified())))
        } else {
            requireActivity().showToast(getString(R.string.backup_tip_illegal_file_type))
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