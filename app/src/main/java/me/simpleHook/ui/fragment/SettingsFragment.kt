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
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.MenuPreference
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import me.simpleHook.viewmodel.SettingsViewModel
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
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { resultUri ->
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
        findPreference<MenuPreference>("uiMode")?.apply {
            val arrayList =
                requireContext().resources.getStringArray(R.array.main_settings_ui_mode_item_entries)
            this.summary = when (sp.uiMode) {
                MODE_NIGHT_YES -> arrayList[1]
                MODE_NIGHT_NO -> arrayList[0]
                else -> arrayList[2]
            }
            setOnPreferenceChangeListener { _, newValue ->
                when (newValue as String) {
                    arrayList[0] -> {
                        if (sp.uiMode != MODE_NIGHT_NO) {
                            sp.uiMode = MODE_NIGHT_NO
                            setDefaultNightMode(MODE_NIGHT_NO)
                        }
                    }
                    arrayList[1] -> {
                        if (sp.uiMode != MODE_NIGHT_YES) {
                            sp.uiMode = MODE_NIGHT_YES
                            setDefaultNightMode(MODE_NIGHT_YES)
                        }
                    }
                    arrayList[2] -> {
                        if (sp.uiMode != MODE_NIGHT_FOLLOW_SYSTEM) {
                            sp.uiMode = MODE_NIGHT_FOLLOW_SYSTEM
                            setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                }
                requireActivity().recreate()
                true
            }
        }

        findPreference<MenuPreference>("selectLanguage")?.apply {
            val arrayList =
                requireContext().resources.getStringArray(R.array.main_settings_language_item_entries)
            this.summary = when (sp.language) {
                Locale.SIMPLIFIED_CHINESE.toString() -> arrayList[1]
                Locale.TRADITIONAL_CHINESE.toString() -> arrayList[2]
                Locale.ENGLISH.language.toString() -> arrayList[3]
                else -> arrayList[0]
            }
            setOnPreferenceChangeListener { _, newValue ->
                when (newValue as String) {
                    arrayList[1] -> {
                        LanguageUtils.switchLanguage(
                            Locale.SIMPLIFIED_CHINESE.toString(),
                            requireActivity(),
                            MainActivity::class.java
                        )
                    }
                    arrayList[2] -> {
                        LanguageUtils.switchLanguage(
                            Locale.TRADITIONAL_CHINESE.toString(),
                            requireActivity(),
                            MainActivity::class.java
                        )
                    }
                    arrayList[3] -> {
                        LanguageUtils.switchLanguage(
                            Locale.ENGLISH.toString(), requireActivity(), MainActivity::class.java
                        )
                    }
                    else -> {
                        LanguageUtils.switchLanguage(
                            "system", requireActivity(), MainActivity::class.java
                        )
                    }
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
                val itemValue =
                    requireActivity().resources.getStringArray(R.array.main_settings_left_config_select_item)
                val position = itemValue.indexOf(newValue as String)
                deleteLeftConfigs(position)
                true
            }
        }
    }

    private fun checkPermission() {
        if (SuUtil.isRoot || FileUtils.isGrant(requireContext())) {
            settingsViewModel.permStatus.value = Constant.IS_GRANT
            return
        }
        settingsViewModel.permStatus.value = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            Constant.NO_ROOT
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            Constant.NO_STORAGE_1
        } else {
            Constant.NO_STORAGE_2
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
                    FileUtils.realDeleteConfig(requireContext(), apps[i], Constant.APP_CONFIG_NAME)
                }
                if (apps[i] !in extensionPackageNames) {
                    FileUtils.realDeleteConfig(
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
                        FileUtils.realDeleteConfig(
                            requireContext(),
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
                        FileUtils.realDeleteConfig(
                            requireContext(), it.packageName, Constant.EXTENSION_CONFIG_NAME
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
        warningDialog(requireContext(), title = "用户协议【已同意】", message = message)
    }

    private fun showUpdateRecord() {
        val message = AssetsUtil.getText(requireContext(), "update")
        warningDialog(requireContext(), title = "更新记录", message = message)
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
                        title = "缺少必要权限"
                        summary = "点击获取ROOT权限"
                    }
                    else -> {
                        title = "缺少必要权限"
                        summary = "点击获取存储权限"
                    }
                }
            }
            setOnPreferenceClickListener {
                when (settingsViewModel.permStatus.value) {
                    Constant.NO_ROOT -> SuUtil.init(requireContext())
                    Constant.NO_STORAGE_1 -> {
                        requestPermissionDialog(requireContext()) {
                            val document = DocumentFile.fromTreeUri(
                                requireContext(), Uri.parse(Constant.ANDROID_DATA_URI)
                            )
                            startActivityForData.launch(
                                document?.uri ?: Uri.parse(Constant.ANDROID_DATA_URI)
                            )
                        }
                    }
                    Constant.NO_STORAGE_2 -> {
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