package me.simpleHook.feature.settings.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.base.IMenuProvider
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.contract.OpenDocumentTreeContract
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.ui.custom.LoadingDialog
import me.simpleHook.core.ui.custom.MaterialSwitchPreference
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.custom.requestPermissionDialog
import me.simpleHook.core.ui.custom.warningDialog
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.AssetsUtil
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.core.utils.LanguageUtil
import me.simpleHook.core.utils.OSUtil
import me.simpleHook.core.utils.PermissionUtil
import me.simpleHook.core.utils.SPUtil
import me.simpleHook.core.utils.SuUtil
import me.simpleHook.core.utils.ThemeModeUtil
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.data.PermissionState
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.data.local.configstore.PrefConfigHelper
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.feature.about.ui.AboutActivity
import me.simpleHook.feature.backup.ui.BackupActivity
import me.simpleHook.feature.config.ui.ConfigDialogFragment
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.feature.config.viewmodel.CollectionViewModel
import me.simpleHook.feature.main.ui.MainActivity
import me.simpleHook.feature.record.viewmodel.RecordViewModel
import me.simpleHook.feature.settings.viewmodel.SettingsViewModel
import me.simpleHook.platform.shizuku.ShizukuFileManager
import rikka.preference.SimpleMenuPreference
import rikka.shizuku.Shizuku

class SettingsFragment : PreferenceFragmentCompat() {
    private val sp by lazy { SPUtil(requireContext()) }
    private val viewModel: AppConfigViewModel by viewModels()
    private val collViewModel by viewModels<CollectionViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()

    private val recordViewModel by viewModels<RecordViewModel>()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            PermissionUtil.takePersistableUriPermission(requireContext(), uri)
        }
    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        findPreference<MaterialSwitchPreference>("checkPermission")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    customDialog(
                        requireContext(),
                        title = "Tip",
                        message = getString(R.string.main_settings_tip_close_check_permission),
                        okText = "ok"
                    ).show()
                }
                true
            }
        }
        findPreference<Preference>("batch_grant")?.apply {
            isVisible = OSUtil.atLeastT() && FlavorUtil.normalVersion
            setOnPreferenceClickListener {
                val intent = Intent(requireContext(), A33PermissionActivity::class.java)
                startActivity(intent)
                true
            }
        }
        findPreference<Preference>("about")?.apply {
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                true
            }
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        }
        findPreference<Preference>("help")?.apply {
            setOnPreferenceClickListener {
                showHelp()
                true
            }
        }
        findPreference<Preference>("updateRecord")?.apply {
            setOnPreferenceClickListener {
                showUpdateRecord()
                true
            }
        }
        findPreference<Preference>("backup_and_restore")?.apply {
            setOnPreferenceClickListener {
                Intent(requireContext(), BackupActivity::class.java).apply {
                    startActivity(this)
                }
                true
            }
        }
        findPreference<Preference>("termsOfUse")?.apply {
            setOnPreferenceClickListener {
                showUseTerms()
                true
            }
        }
        findPreference<SimpleMenuPreference>("clearConfigData")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                warningDialog(
                    requireContext(),
                    getString(R.string.settings_clear_warning_dialog_title),
                    getString(R.string.settings_clear_warning_dialog_message),
                    okText = getString(R.string.settings_clear_warning_dialog_confirm),
                    okClick = {
                        when (newValue as String) {
                            "hook" -> clearHookConfig(0)
                            "extension" -> clearHookConfig(1)
                            "record" -> clearHookConfig(2)
                            "favourite" -> clearHookConfig(3)
                        }
                    })
                true
            }
        }
        findPreference<SimpleMenuPreference>("themeMode")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                ThemeModeUtil.setMode(newValue as String)
                if (sp.themeMode != newValue) requireActivity().recreate()
                true
            }
        }
        findPreference<MaterialSwitchPreference>("enableSystemAccent")?.apply {
            isVisible = DynamicColors.isDynamicColorAvailable()
            setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }
        }
        findPreference<SimpleMenuPreference>("language")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {
                    LanguageUtil.switchLanguage(
                        newValue,
                        requireActivity(),
                        MainActivity::class.java
                    )
                }
                true
            }
        }
        findPreference<Preference>("toJSConfig")?.apply {
            setOnPreferenceClickListener {
                toJSConfig()
                true
            }
        }
        findPreference<Preference>("leftConfig")?.also {
            it.setOnPreferenceChangeListener { _, newValue ->
                deleteLeftConfigs(newValue as String)
                true
            }
        }
        findPreference<SimpleMenuPreference>("workMode")?.apply {
            summary = GlobalValue.sp.workMode
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {
                    summary = newValue
                    GlobalValue.sp.workMode = newValue
                    checkPermission()
                }
                true
            }
        }
    }

    private fun checkPermission() {
        if (FlavorUtil.liteVersion) {
            settingsViewModel.permStatus.value =
                if ((configSystem as PrefConfigHelper).customPref == null) {
                    PermissionState.NO_ALIVE
                } else {
                    PermissionState.GRANT
                }
            return
        }
        if (FlavorUtil.rootVersion) {
            settingsViewModel.permStatus.value = if (GlobalValue.isRootWork) {
                if (SuUtil.isGrantedRoot()) {
                    PermissionState.GRANT
                } else {
                    PermissionState.NO_ROOT
                }
            } else {
                if (ShizukuFileManager.isPermissionGranted) {
                    PermissionState.GRANT
                } else {
                    PermissionState.NO_SHIZUKU
                }
            }
            return
        }
        if (OSUtil.atLeastT()) {
            settingsViewModel.permStatus.value = PermissionState.GRANT
        } else if (OSUtil.atLeastR()) {
            settingsViewModel.permStatus.value = if (PermissionUtil.isGrantData()) {
                PermissionState.GRANT
            } else {
                PermissionState.NO_STORAGE
            }
        } else {
            settingsViewModel.permStatus.value =
                if (PermissionUtil.isGrantWritePermission(requireContext())) {
                    PermissionState.GRANT
                } else {
                    PermissionState.NO_STORAGE
                }
        }
    }


    private fun deleteLeftConfigs(mode: String) {
        val loadingDialog = LoadingDialog(
            requireActivity(),
            getString(R.string.main_delete_left_config_loading_tip)
        )
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                val apps = when (mode) {
                    "user" -> AppUtil.getUserPackageNames()
                    "system" -> AppUtil.getSystemPackageNames()
                    else -> AppUtil.getPackageNames()
                }
                val appPackageNames = viewModel.getAllPackageNames()
                val extensionPackageNames = viewModel.getAllPackageNames()
                for (i in apps.indices) {
                    if (apps[i] !in appPackageNames) {
                        configSystem.deleteCustomConfig(apps[i])
                    }
                    if (apps[i] !in extensionPackageNames) {
                        configSystem.deleteExConfig(apps[i])
                    }
                }
            }
            loadingDialog.dismiss()
        }
    }

    private fun toJSConfig() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val appConfigItem2s = mutableListOf<AppConfigItem2>()
            viewModel.getConfigs().forEach {
                appConfigItem2s.add(AppConfigItem2(it))
            }
            ConfigDialogFragment(
                appConfigItem2s,
                Constant.CONFIG_EXPORT_JS_MODE
            ).show(
                requireActivity().supportFragmentManager,
                "toJS"
            )
        }
    }

    private fun clearHookConfig(mode: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (mode) {
                0 -> {
                    val deleteConfigs = viewModel.getConfigs().filter {
                        configSystem.isEnableDelete(it.packageName)
                    }
                    viewModel.deleteConfigs(*deleteConfigs.toTypedArray())
                }

                1 -> {
                    val configs = viewModel.getExtConfigs()
                    val tempConfigs = ArrayList<ExtensionConfigEntity>()
                    configs.forEach {
                        if (configSystem.isEnableDelete(it.packageName)) {
                            configSystem.deleteExConfig(it.packageName)
                            tempConfigs.add(it)
                            viewModel.deleteExtConfigs(it)
                        }
                    }
                    viewModel.deleteExtConfigs(*tempConfigs.toTypedArray())
                }

                2 -> {
                    recordViewModel.deleteAllRecords()
                }

                3 -> {
                    collViewModel.deleteAllCollections()
                }
            }
        }

    }

    private fun showUseTerms() {
        val message = AssetsUtil.getText(requireContext(), "agreement")
        warningDialog(requireContext(), title = "用户协议【已同意】", message = message!!)
    }

    private fun showUpdateRecord() {
        val message = AssetsUtil.getText(requireContext(), "update")
        warningDialog(requireContext(), title = "更新记录", message = message!!)
    }

    private fun showHelp() {
        val intent = Intent(Intent.ACTION_VIEW).also {
            it.data = "https://github.com/littleWhiteDuck/SimpleHookShare".toUri()
        }
        startActivity(intent)
    }


    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.isVerticalScrollBarEnabled = false
        recyclerView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(recyclerView, windowInsets)
            recyclerView.setPadding(0, 0, 0, navigationInsets.bottom)
            windowInsets
        }
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDividerHeight(0)
        initViewModel()
    }

    private fun initViewModel() {
        if (FlavorUtil.rootVersion) {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuFileManager.isPermissionGranted = true
                    if (!ShizukuFileManager.isAvailable) {
                        ShizukuFileManager.bindService()
                    }
                    checkPermission()
                }
            }
        }
        findPreference<Preference>("necessary_permission")?.apply {
            settingsViewModel.permStatus.observe(viewLifecycleOwner) {
                when (it) {
                    PermissionState.GRANT -> {
                        title = getString(R.string.main_settings_title_storage_permission)
                        summary = getString(R.string.main_settings_summary_storage_permission)
                    }

                    PermissionState.NO_ROOT -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_storage_no_root)
                    }

                    PermissionState.NO_SHIZUKU -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_storage_no_shizuku)
                    }

                    PermissionState.NO_STORAGE -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_storage_no)
                    }

                    PermissionState.NO_ALIVE -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_no_alive)
                    }
                }
            }
            setOnPreferenceClickListener {
                when (settingsViewModel.permStatus.value) {
                    PermissionState.NO_ROOT -> {
                        requireActivity().showPopup(getString(R.string.not_root_tip))
                    }

                    PermissionState.NO_SHIZUKU -> {
                        if (ShizukuFileManager.binderAvailable) {
                            Shizuku.requestPermission(1314)
                        } else {
                            requireActivity().showPopup(message = getString(R.string.no_shizuku_tip))
                        }
                    }

                    PermissionState.NO_STORAGE -> {
                        if (OSUtil.atR2T()) {
                            if (!PermissionUtil.isGrantData(Constant.ANDROID_DATA_URI)) {
                                requestPermissionDialog(requireContext()) {
                                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                                }
                            }
                        } else if (OSUtil.atMostQ()) {
                            if (!PermissionUtil.isGrantWritePermission(requireContext())) {
                                requestPermissionDialog(requireContext()) {
                                    PermissionUtil.verifyStoragePermissions(requireActivity())
                                }
                            }
                        }
                    }

                    else -> {}
                }
                checkPermission()
                true
            }
        }
        checkPermission()
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        (activity as? IMenuProvider)?.let {
            it.currentMenuProvider?.let { menuProvider ->
                activity?.removeMenuProvider(menuProvider)
            }
            it.currentMenuProvider = null
        }
    }
}

