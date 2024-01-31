package me.simpleHook.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.config.PrefConfigHelper
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.extension.showToast
import me.simpleHook.ui.activity.A33PermissionActivity
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.ui.activity.BackupActivity
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import me.simpleHook.viewmodel.CollectionViewModel
import me.simpleHook.viewmodel.SettingsViewModel
import rikka.preference.SimpleMenuPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private val sp by lazy { SPUtils(requireContext()) }
    private val viewModel: AppViewModel by viewModels()
    private val collViewModel by viewModels<CollectionViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val contentResolver = requireActivity().contentResolver
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }
    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
//        findPreference<CheckBoxPreference>("lspScope")?.isVisible = FlavorUtils.rootVersion
        findPreference<CheckBoxPreference>("checkPermission")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    customDialog(requireContext(),
                        title = "Tip",
                        message = getString(R.string.main_settings_tip_close_check_permission),
                        okText = "ok").show()
                }
                true
            }
        }
        findPreference<Preference>("batch_grant")?.apply {
            if (OSUtils.atLeastT() && FlavorUtils.normalVersion) isVisible = true
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
                warningDialog(requireContext(),
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
        findPreference<SimpleMenuPreference>("language")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {
                    LanguageUtils.switchLanguage(newValue,
                        requireActivity(),
                        MainActivity::class.java)
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
    }

    private fun checkPermission() {
        if (FlavorUtils.liteVersion) {
            settingsViewModel.permStatus.value =
                if ((configSystem as PrefConfigHelper).customPref == null) {
                    Constant.NO_ALIVE
                } else {
                    Constant.IS_GRANT
                }
            return
        }
        if (FlavorUtils.rootVersion) {
            settingsViewModel.permStatus.value = if (SuUtil.isGrantedRoot()) {
                Constant.IS_GRANT
            } else {
                Constant.NO_ROOT
            }
            return
        }
        if (OSUtils.atLeastT()) {
            settingsViewModel.permStatus.value = Constant.IS_GRANT
        } else if (OSUtils.atLeastR()) {
            settingsViewModel.permStatus.value = if (PermissionUtils.isGrantData()) {
                Constant.IS_GRANT
            } else {
                Constant.NO_STORAGE
            }
        } else {
            settingsViewModel.permStatus.value =
                if (PermissionUtils.isGrantWritePermission(requireContext())) {
                    Constant.IS_GRANT
                } else {
                    Constant.NO_STORAGE
                }
        }
    }


    private fun deleteLeftConfigs(mode: String) {
        val loadingDialog = LoadingDialog(requireActivity(),
            getString(R.string.main_delete_left_config_loading_tip))
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                val apps = when (mode) {
                    "user" -> AppUtils.getUserPackageNames(requireContext())
                    "system" -> AppUtils.getSystemPackageNames(requireContext())
                    else -> AppUtils.getPackageNames(requireContext())
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
            val configItems = mutableListOf<ConfigItem>()
            viewModel.getConfigs().forEach {
                configItems.add(ConfigItem(it))
            }
            ConfigDialogFragment(configItems,
                Constant.CONFIG_EXPORT_JS_MODE).show(requireActivity().supportFragmentManager,
                "toJS")
        }
    }

    private fun clearHookConfig(mode: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (mode) {
                0 -> {
                    val configs = viewModel.getConfigs()
                    val tempConfigs = ArrayList<AppConfig>()
                    configs.forEach {
                        if (configSystem.isEnableDelete(it.packageName)) {
                            configSystem.deleteCustomConfig(it.packageName)
                            tempConfigs.add(it)
                        }
                    }
                    viewModel.deleteConfigs(*tempConfigs.toTypedArray())
                }
                1 -> {
                    val configs = viewModel.getAssistConfigs()
                    val tempConfigs = ArrayList<AssistConfig>()
                    configs.forEach {
                        if (configSystem.isEnableDelete(it.packageName)) {
                            configSystem.deleteExConfig(it.packageName)
                            tempConfigs.add(it)
                            viewModel.deleteAssistConfigs(it)
                        }
                    }
                    viewModel.deleteAssistConfigs(*tempConfigs.toTypedArray())
                }
                2 -> {
                    viewModel.deleteAllLogs()
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
            it.data = Uri.parse("https://github.com/littleWhiteDuck/SimpleHookShare")
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
        findPreference<Preference>("necessary_permission")?.apply {
            settingsViewModel.permStatus.observe(viewLifecycleOwner) {
                when (it) {
                    Constant.IS_GRANT -> {
                        title = getString(R.string.main_settings_title_storage_permission)
                        summary = getString(R.string.main_settings_summary_storage_permission)
                    }
                    Constant.NO_ROOT -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_storage_no_root)
                    }
                    Constant.NO_ALIVE -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_no_alive)
                    }
                    else -> {
                        title = getString(R.string.main_settings_title_storage_no_permission)
                        summary = getString(R.string.main_settings_summary_storage_no)
                    }
                }
            }
            setOnPreferenceClickListener {
                when (settingsViewModel.permStatus.value) {
                    Constant.NO_ROOT -> {
                        requireActivity().showToast(getString(R.string.not_root_tip))
                    }
                    Constant.NO_STORAGE -> {
                        if (OSUtils.atR2T()) {
                            if (!PermissionUtils.isGrantData(Constant.ANDROID_DATA_URI)) {
                                requestPermissionDialog(requireContext()) {
                                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                                }
                            }
                        } else if (OSUtils.atMostQ()) {
                            if (!PermissionUtils.isGrantWritePermission(requireContext())) {
                                requestPermissionDialog(requireContext()) {
                                    PermissionUtils.verifyStoragePermissions(requireActivity())
                                }
                            }
                        }
                    }
                }
                checkPermission()
                true
            }
        }
        checkPermission()
        checkPermission()
    }
}