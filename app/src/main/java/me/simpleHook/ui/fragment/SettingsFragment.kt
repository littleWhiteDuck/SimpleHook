package me.simpleHook.ui.fragment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.config.ConfigHelper
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import me.simpleHook.viewmodel.SettingsViewModel
import rikka.preference.SimpleMenuPreference
import java.io.*
import java.util.*
import kotlin.concurrent.thread

class SettingsFragment : PreferenceFragmentCompat() {
    private val sp by lazy { SPUtils(requireContext()) }
    private val viewModel: AppViewModel by activityViewModels()
    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val restoreConfigs =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
            resultUri?.let {
                importConfigs(readTextFromUri(it))
            }
        }
    private val backupConfigs =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/json")) { resultUri ->
            resultUri?.apply {
                thread {
                    alterDocument(this, JsonUtil.formatJson(Gson().toJson(viewModel.getConfigs())))
                }
            }
        }
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val contentResolver = requireActivity().contentResolver
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
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
        findPreference<Preference>("backupConfigs")?.apply {
            setOnPreferenceClickListener {
                backupConfigs()
                true
            }
        }
        findPreference<Preference>("restoreConfigs")?.apply {
            setOnPreferenceClickListener {
                restoreConfigs()
                true
            }
        }
        findPreference<Preference>("termsOfUse")?.apply {
            setOnPreferenceClickListener {
                showUseTerms()
                true
            }
        }
        findPreference<Preference>("clearConfigData")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                warningDialog(requireContext(),
                    getString(R.string.settings_clear_warning_dialog_title),
                    getString(
                        R.string.settings_clear_warning_dialog_message
                    ),
                    okText = getString(
                        R.string.settings_clear_warning_dialog_confirm
                    ),
                    okClick = {
                        when (newValue as String) {
                            getString(R.string.settings_clear_hook_config) -> clearHookConfig(0)
                            getString(R.string.setting_clear_extension_config) -> clearHookConfig(1)
                            getString(R.string.settings_clear_all_record) -> clearHookConfig(2)
                            getString(R.string.settings_clear_all_favorites) -> {
                                FileUtils.deleteFile(requireActivity().getExternalFilesDir(null)!!.path + "/collection_config.json")
                            }
                        }
                    })
                true
            }
        }
        val themePreference = findPreference<SimpleMenuPreference>("themeMode")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                ThemeModeUtil.setMode(newValue as String)
                if (sp.themeMode != newValue) requireActivity().recreate()
                true
            }
        }!!
        val themeNames =
            requireContext().resources.getStringArray(R.array.main_settings_theme_mode_item_entries)
        themePreference.summary =
            themeNames[listOf(*themePreference.entryValues).indexOf(themePreference.value)]
        val languagePreference = findPreference<SimpleMenuPreference>("language")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {
                    LanguageUtils.switchLanguage(
                        newValue, requireActivity(), MainActivity::class.java
                    )
                }
                true
            }
        }!!
        val languageNames =
            requireContext().resources.getStringArray(R.array.main_settings_language_item_entries)
        languagePreference.summary =
            languageNames[listOf(*languagePreference.entryValues).indexOf(languagePreference.value)]
        findPreference<Preference>("toJSConfig")?.apply {
            setOnPreferenceClickListener {
                toJSConfig()
                true
            }
        }
        findPreference<Preference>("leftConfig")?.also {
            it.setOnPreferenceChangeListener { _, newValue ->
                val itemValue =
                    requireActivity().resources.getStringArray(R.array.main_settings_left_config_select_item)
                val position = itemValue.indexOf(newValue as String)
                deleteLeftConfigs(position)
                true
            }
        }
    }

    private fun checkPermission() {
        if (FlavorUtils.isLiteVersion) {
            if (ConfigHelper.getHookConfigPref(requireContext()) == null) {
                settingsViewModel.permStatus.value = Constant.NO_ALIVE
            }
            return
        }
        if (Shell.isAppGrantedRoot() == true || FileUtils.isGrant(requireContext())) {
            settingsViewModel.permStatus.value = Constant.IS_GRANT
            return
        }
        settingsViewModel.permStatus.value = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            Constant.NO_ROOT
        } else {
            Constant.NO_STORAGE
        }
    }


    private fun deleteLeftConfigs(position: Int) {
        val loadingDialog = LoadingDialog(
            requireActivity(), getString(R.string.main_delete_left_config_loading_tip)
        )
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val apps = when (position) {
                0 -> AppUtils.getUserPackageNames(requireContext())
                1 -> AppUtils.getSystemPackageNames(requireContext())
                else -> AppUtils.getPackageNames(requireContext())
            }
            val appPackageNames = viewModel.getAllPackageNames()
            val extensionPackageNames = viewModel.getAllPackageNames()
            for (i in apps.indices) {
                if (apps[i] !in appPackageNames) {
                    ConfigHelper.deleteConfig(requireContext(), apps[i], Constant.APP_CONFIG_NAME)
                }
                if (apps[i] !in extensionPackageNames) {
                    ConfigHelper.deleteConfig(
                        requireContext(), apps[i], Constant.EXTENSION_CONFIG_NAME
                    )
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
            ConfigDialogFragment(
                configItems, Constant.CONFIG_EXPORT_JS_MODE
            ).show(
                requireActivity().supportFragmentManager, "toJS"
            )
        }
    }

    private fun clearHookConfig(mode: Int) {
        showNotification("正在删除中", "请勿退出应用")
        lifecycleScope.launch(Dispatchers.IO) {
            when (mode) {
                0 -> {
                    val configs = viewModel.getConfigs()
                    configs.forEach {
                        ConfigHelper.deleteConfig(
                            requireActivity(),
                            it.packageName,
                            Constant.APP_CONFIG_NAME,
                        )
                    }
                    viewModel.deleteAllConfigs()
                    showNotification("完成", "Hook配置已经删除成功")
                }
                1 -> {
                    val configs = viewModel.getAssistConfigs()
                    configs.forEach {
                        ConfigHelper.deleteConfig(
                            requireActivity(), it.packageName, Constant.EXTENSION_CONFIG_NAME
                        )
                    }
                    viewModel.deleteAssistConfigsByPackageName("模板配置")
                    showNotification("完成", "扩展配置已经删除成功")
                }
                2 -> {
                    viewModel.deleteAllLogs()
                    showNotification("完成", "记录已经删除成功")
                }
            }
        }

    }

    private fun showNotification(title: String, content: String) {
        val manager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "delete", "删除通知", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(requireActivity(), "delete")
            .setContentTitle(title).setContentText(content)
            .setSmallIcon(R.drawable.ic_outline_delete_forever_24).setLargeIcon(
                BitmapFactory.decodeResource(
                    resources, R.drawable.ic_outline_delete_forever_24
                )
            ).build()
        manager.notify(1, notification)
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

    private fun restoreConfigs() {
        restoreConfigs.launch(arrayOf("application/json", "text/plain"))
    }

    private fun backupConfigs() {
        val time = TimeUtil.getDateTime(System.currentTimeMillis(), pattern = "yyMMdd")
        backupConfigs.launch("simpleHook_backup_$time.json")
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.isVerticalScrollBarEnabled = false
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                // Get the position of the view in the recycler view
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) {
                    return
                }

                if (position == parent.adapter!!.itemCount - 1) {
                    // Add padding to the last item. You should probably use a @dimen resource.
                    outRect.bottom = 200
                }
            }
        })
        initViewModel()
        return recyclerView
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
                        getString(R.string.not_root_tip).toast(requireContext())
                    }
                    Constant.NO_STORAGE -> {
                        requestPermissionDialog(requireContext()) {
                            FileUtils.verifyStoragePermissions(requireActivity())
                        }
                    }
                }
                checkPermission()
                true
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            requireActivity().contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            "error".toast(requireContext())
        }

        return stringBuilder.toString()
    }

    private fun importConfigs(configs: String) {
        try {
            when {
                JsonUtil.isJsonArray(configs) -> {
                    val dataList = JsonUtil.importConfigs(configs)
                    if (dataList.isEmpty()) {
                        getString(R.string.main_home_import_incorrect_format_tip).toast(
                            requireContext()
                        )
                        return
                    } else {
                        ConfigDialogFragment(
                            dataList as ArrayList<ConfigItem>, Constant.CONFIG_IMPORT_MODE
                        ).show(
                            requireActivity().supportFragmentManager, "import"
                        )
                    }
                }
                JsonUtil.isJsonObject(configs) -> {
                    val appConfig = Gson().fromJson(configs, AppConfig::class.java)
                    viewModel.insertConfigs(appConfig)
                }
            }
        } catch (e: java.lang.Exception) {
            getString(R.string.main_home_import_incorrect_format_tip).toast(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun alterDocument(uri: Uri, strConfigs: String) {
        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                // use{} lets the document provider know you're done by automatically closing the stream
                FileOutputStream(it.fileDescriptor).use { put ->
                    put.write((strConfigs).toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}