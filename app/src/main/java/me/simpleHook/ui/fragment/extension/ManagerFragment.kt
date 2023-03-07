package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.base.BaseExtensionFragment
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.compat.BundleCompat
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentExtensionManagerBinding
import me.simpleHook.extension.dp
import me.simpleHook.extension.showToast
import me.simpleHook.extension.snack
import me.simpleHook.recyclerview.adapter.DividerItemDecoration
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.view.extension.ExtensionItemTitleView
import me.simpleHook.ui.view.extension.SelectItemView
import me.simpleHook.ui.view.extension.SubSelectItemView
import me.simpleHook.util.*
import me.simpleHook.viewmodel.ExViewModel


class ManagerFragment : BaseExtensionFragment<FragmentExtensionManagerBinding>() {

    private val extensionConfig: AssistConfig by lazy {
        BundleCompat.getParcelable(requireArguments(), "EXTENSION_CONFIG")!!
    }
    private val exViewModel by activityViewModels<ExViewModel>()
    private var editMode: Boolean = true
    private val appViewModel by viewModels<AppViewModel>()
    private val items = ArrayList<Any>()
    private var configBean: ExtensionConfig = ExtensionConfig()
    private var tempConfigStr = ""
    private val adapter = MultiTypeAdapter()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireActivity().contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }
    private val navController by lazy {
        findNavController(requireActivity(), R.id.nav_host_fragment)
    }


    override fun init() {
        initMenu()
        initView()
        initData()
    }

    private fun initData() {
        if (exViewModel.extensionConfig.value == null) {
            exViewModel.extensionConfig.value = configBean
        }
        exViewModel.extensionConfig.observe(requireActivity()) {
            it?.let {
                configBean = it
            }
        }
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val isInstalled =
                    AppUtils.isAppInstalled(requireContext(), extensionConfig.packageName)
                menuInflater.inflate(R.menu.menu_extension_manager, menu)
                menu.findItem(R.id.menu_open_float).isChecked = GlobalValue.sp.startFloat
                menu.findItem(R.id.menu_open_float).setOnMenuItemClickListener {
                    it.isChecked = !it.isChecked
                    GlobalValue.sp.startFloat = it.isChecked
                    true
                }
                if (GlobalValue.packageManager.getLaunchIntentForPackage(extensionConfig.packageName) == null || !FlavorUtils.rootVersion) {
                    menu.removeItem(R.id.menu_relaunch)
                }
                if (!isInstalled) {
                    menu.removeItem(R.id.menu_force_stop)
                    menu.removeItem(R.id.menu_relaunch)
                    menu.removeItem(R.id.menu_app_info)
                    menu.removeItem(R.id.menu_launch)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_launch -> {
                        showFloatWindow()
                        AppUtils.startApp(extensionConfig.packageName, requireContext())
                    }
                    R.id.menu_save_config -> saveConfig()
                    R.id.menu_force_stop -> {
                        showFloatWindow()
                        if (FlavorUtils.rootVersion) {
                            SuUtil.forceStopApp(extensionConfig.packageName)
                        } else {
                            AppUtils.jumpAppInfoPage(requireContext(), extensionConfig.packageName)
                        }
                    }
                    R.id.menu_relaunch -> {
                        showFloatWindow()
                        if (FlavorUtils.rootVersion) {
                            val intent =
                                GlobalValue.packageManager.getLaunchIntentForPackage(extensionConfig.packageName)
                            intent?.component?.className?.let { className ->
                                SuUtil.reLaunchApp(extensionConfig.packageName, className)
                            }
                        }
                    }
                    R.id.menu_app_info -> AppUtils.jumpAppInfoPage(requireContext(),
                        extensionConfig.packageName)
                }
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun onSubItemClick(tag: String) {
        when (tag) {
            TAG_STOP_DIALOG -> {
                navController.navigate(R.id.action_managerFragment_to_disableDialogFragment)
            }
            TAG_FILTER_CLIPBOARD -> {
                navController.navigate(R.id.action_managerFragment_to_clipboardFragment)
            }
            TAG_GUISE_SIGN -> {
                navController.navigate(R.id.action_managerFragment_to_guiseSignFragment)
            }
            TAG_FILE_MONITOR -> {
                navController.navigate(R.id.action_managerFragment_to_fileMonitorFragment)
            }
            TAG_APP_EXIT -> {
                navController.navigate(R.id.action_managerFragment_to_exitFragment)
            }
        }
    }

    private fun onItemClick(tag: String, checked: Boolean) {
        if (tag == "hotFix" && extensionConfig.packageName != Constant.MODEL_EXTENSION_CONFIG) {
            createDexDirectory()
        }
        when (tag) {
            TAG_STOP_DIALOG -> {
                configBean.stopDialog.enable = checked
            }
            TAG_FILTER_CLIPBOARD -> {
                configBean.filterClipboard.enable = checked
            }
            TAG_GUISE_SIGN -> {
                configBean.guiseSign.enable = checked
            }
            TAG_FILE_MONITOR -> {
                configBean.fileMonitor.enable = checked
            }
            TAG_APP_EXIT -> {
                configBean.exit.enable = checked
            }
            else -> {
                Class.forName(ExtensionConfig::class.java.name).apply {
                    getDeclaredField(tag).apply {
                        isAccessible = true
                        setBoolean(configBean, checked)
                    }
                }
            }
        }
    }


    private fun createDexDirectory() {
        val filePath = if (FlavorUtils.rootVersion) {
            val path = Constant.ROOT_CONFIG_MAIN_DIRECTORY + extensionConfig.packageName + "/dex"
            SuUtil.makeDirs(path)
            path
        } else {
            val path = Constant.ANDROID_DATA_PATH + extensionConfig.packageName + "/simpleHook/dex"
            if (OSUtils.atLeastR()) {
                DocumentCompat.makeDirs(requireContext(), path, extensionConfig.packageName)
            } else {
                FileUtils.makeDirs(path)
            }
            path
        }
        ToolUtils.toClip(requireContext(), filePath)
        requireActivity().showToast(getString(R.string.extension_tip_dex_path_to_clip))
    }

    private fun showFloatWindow() {
        if (sp.startFloat) (requireActivity() as ExtensionActivity).initPrintFloat()
        if (tempConfigStr != configBean.toString()) {
            saveConfig()
        }
    }


    private fun checkPermission(): Boolean {
        if (FlavorUtils.normalVersion && OSUtils.atLeastT() && extensionConfig.packageName != Constant.MODEL_EXTENSION_CONFIG && !PermissionUtils.isGrantPackage(
                extensionConfig.packageName)
        ) {
            requestPermissionDialog(requireContext(),
                message = getString(R.string.android_13_no_permission)) {
                val uri = DocumentCompat.generateAppUri(extensionConfig.packageName)
                startActivityForData.launch(uri)
            }
            return false
        }
        return true
    }

    @SuppressLint("Range")
    private fun saveConfig(exit: Boolean = false): Boolean {
        if (!checkPermission()) return false
        val loadingDialog = LoadingDialog(requireActivity(), getString(R.string.main_loading))
        loadingDialog.show()
        val config = Json.encodeToString(configBean)
        tempConfigStr = configBean.toString()
        extensionConfig.config = config
        extensionConfig.allSwitch = configBean.all
        if (editMode) {
            appViewModel.updateAssistConfigs(extensionConfig)
        } else {
            appViewModel.insertAssistConfigs(extensionConfig)
        }
        if (extensionConfig.packageName != Constant.MODEL_EXTENSION_CONFIG) {
            saveConfig(extensionConfig.packageName, config)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            getString(R.string.extension_save_success).snack(binding.recyclerView)
            loadingDialog.quickDismiss()
            if (exit) {
                backPressed()
            }
        }, 500)
        return true
    }

    private fun saveConfig(packageName: String, config: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            configSystem.saveExConfig(packageName, config)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        editMode = requireActivity().intent.getBooleanExtra("EXTENSION_CONFIG_EDIT", true)
        val dexPosition = getString(R.string.extension_dex_position)
        val dexPath = if (FlavorUtils.normalVersion) {
            dexPosition + "/Android/data/${extensionConfig.packageName}/simpleHook/dex/"
        } else {
            dexPosition + "/data/local/tmp/simpleHook/${extensionConfig.packageName}/dex/"
        }
        val config = extensionConfig.config
        configBean = if (config.isNotEmpty()) Json.decodeFromString(config) else ExtensionConfig()
        tempConfigStr = configBean.toString()
        adapter.register(Title::class.java, TitleViewDelegate())
        adapter.register(ExtensionItem::class.java, ManagerItemViewDelegate { tag, checked ->
            onItemClick(tag, checked)
        })
        adapter.register(ExtensionSubItem::class.java,
            ManagerSubItemViewDelegate(onClick = { tag, checked ->
                onItemClick(tag, checked)
            }, onSubClick = { tag -> onSubItemClick(tag) }))
        binding.recyclerView.addItemDecoration(DividerItemDecoration(adapter))
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.isVerticalScrollBarEnabled = false
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
            binding.recyclerView.setPadding(0, 0, 0, paddingBottom + 10.dp)
            windowInsets
        }
        if (items.isNotEmpty()) return
        configBean.apply {
            items.apply {
                add(Title(getString(R.string.extension_item_title_basic)))
                add(ExtensionItem(getString(R.string.extension_item_title_all_switch),
                    all,
                    "all",
                    getString(R.string.extension_item_desc_all_switch)))
                add(ExtensionItem(getString(R.string.extension_item_title_hook_success_tip),
                    tip,
                    "tip",
                    getString(R.string.extension_item_desc_hook_success_tip)))
                add(Title(getString(R.string.extension_item_title_algorithm_analysis)))
                add(ExtensionItem(getString(R.string.extension_item_title_base64),
                    base64,
                    "base64",
                    getString(R.string.extension_item_desc_base64)))
                add(ExtensionItem(getString(R.string.extension_item_title_digest_algorithm),
                    digest,
                    "digest",
                    getString(R.string.extension_item_desc_digest_algorithm)))
                add(ExtensionItem(getString(R.string.extension_item_title_hmac),
                    hmac,
                    "hmac",
                    getString(R.string.extension_item_desc_hmac)))
                add(ExtensionItem(getString(R.string.extension_item_title_encrypt_algorithm),
                    crypt,
                    "crypt",
                    getString(R.string.extension_item_desc_encrypt_algorithm)))
                add(Title(getString(R.string.extension_item_title_hot_fix)))
                add(ExtensionItem(getString(R.string.extension_item_title_hot_fix_dex),
                    hotFix,
                    "hotFix",
                    dexPath))
                add(Title(getString(R.string.extension_item_title_ui)))
                add(ExtensionItem(getString(R.string.extension_item_title_dialog),
                    dialog,
                    "dialog",
                    getString(R.string.extension_item_desc_dialog)))
                add(ExtensionItem(getString(R.string.extension_item_title_dialog_cancel),
                    diaCancel,
                    "diaCancel",
                    getString(R.string.extension_item_desc_dialog_cancel)))
                add(ExtensionItem(getString(R.string.extension_item_title_toast),
                    toast,
                    "toast",
                    getString(R.string.extension_item_desc_toast)))
                add(ExtensionItem(getString(R.string.extension_item_title_popup_window),
                    popup,
                    "popup",
                    getString(R.string.extension_item_desc_popup_window)))
                add(ExtensionItem(getString(R.string.extension_item_title_popup_window_cancel),
                    popCancel,
                    "popCancel",
                    getString(R.string.extension_item_desc_popup_window_cancel)))
                add(ExtensionItem(getString(R.string.extension_item_title_click_event),
                    click,
                    "click",
                    getString(R.string.extension_item_desc_click_event)))
                add(ExtensionSubItem(getString(R.string.extension_item_title_block_dialog),
                    stopDialog.enable,
                    TAG_STOP_DIALOG,
                    getString(R.string.extension_item_desc_block_dialog)))
                add(Title(getString(R.string.extension_item_title_security)))
                add(ExtensionItem(title = getString(R.string.extension_item_title_disable_sensor),
                    disSensorAG,
                    "disSensorAG",
                    getString(R.string.extension_item_title_disable_acceleration_gyroscope)))
                add(ExtensionItem(title = getString(R.string.extension_item_title_disable_sensor),
                    disSensorSport,
                    "disSensorSport",
                    getString(R.string.extension_item_title_disable_sport_sensor)))
                add(ExtensionItem(title = getString(R.string.extension_item_title_contact),
                    contact,
                    "contact",
                    getString(R.string.extension_item_desc_contact)))
                add(Title("JSON"))
                add(ExtensionItem(getString(R.string.extension_item_title_json_object),
                    jsonObject,
                    "jsonObject",
                    getString(R.string.extension_item_desc_json_object)))
                add(ExtensionItem(getString(R.string.extension_item_title_json_array),
                    jsonArray,
                    "jsonArray",
                    getString(R.string.extension_item_desc_json_array)))
                add(Title(getString(R.string.extension_item_title_others)))
                add(ExtensionItem(getString(R.string.extension_item_title_signature),
                    signature,
                    "signature",
                    getString(R.string.extension_item_desc_signature)))
                add(ExtensionSubItem(getString(R.string.extension_item_title_guise_sign),
                    guiseSign.enable,
                    TAG_GUISE_SIGN,
                    getString(R.string.extension_item_desc_guise_sign)))
                add(ExtensionSubItem(title = getString(R.string.extension_item_title_filter_clipboard),
                    filterClipboard.enable,
                    TAG_FILTER_CLIPBOARD,
                    getString(R.string.extension_item_desc_filter_clipboard)))
                add(ExtensionSubItem(getString(R.string.extension_item_title_file),
                    fileMonitor.enable,
                    TAG_FILE_MONITOR,
                    getString(R.string.extension_item_desc_file)))
                add(ExtensionItem(getString(R.string.extension_item_title_intent),
                    intent,
                    "intent",
                    getString(R.string.extension_item_desc_intent)))
                add(ExtensionItem(title = "Application",
                    application,
                    "application",
                    getString(R.string.extension_item_desc_application_name)))
                add(ExtensionItem(title = "ADB",
                    adb,
                    "adb",
                    getString(R.string.extension_item_desc_adb)))
                add(ExtensionSubItem(title = getString(R.string.extension_item_title_app_exit),
                    exit.enable,
                    TAG_APP_EXIT,
                    getString(R.string.extension_item_desc_app_exit)))
                add(Title(getString(R.string.extension_item_title_network)))
                add(ExtensionItem(getString(R.string.extension_item_title_vpn),
                    vpn,
                    "vpn",
                    getString(R.string.extension_item_desc_vpn)))
                add(Title("WebView"))
                add(ExtensionItem(title = "loadUrl",
                    webLoadUrl,
                    "webLoadUrl",
                    getString(R.string.extension_item_desc_web_load_url)))
                add(ExtensionItem(title = "Debug",
                    webDebug,
                    "webDebug",
                    getString(R.string.extension_item_desc_web_debug)))
            }
            adapter.items = items
            adapter.notifyDataSetChanged()
        }
    }

    override fun canBack(): Boolean {
        return tempConfigStr == configBean.toString()
    }

    override fun performBack() {
        backPressed()
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    override fun enableCallback(): Boolean {
        return true
    }

    companion object {
        private const val TAG_STOP_DIALOG = "DISABLE_DIALOG"
        private const val TAG_FILTER_CLIPBOARD = "FILTER_CLIP"
        private const val TAG_GUISE_SIGN = "GUISE_SIGN"
        private const val TAG_FILE_MONITOR = "FILE_MONITOR"
        private const val TAG_APP_EXIT = "APP_EXIT"
    }

}


data class Title(val name: String)

class TitleViewDelegate : ViewDelegate<Title, ExtensionItemTitleView>() {
    override fun onBindView(view: ExtensionItemTitleView, item: Title) {
        view.text = item.name
    }

    override fun onCreateView(context: Context): ExtensionItemTitleView {
        return ExtensionItemTitleView(context)
    }

}

data class ExtensionItem(
    val title: String,
    var checked: Boolean,
    val tag: String,
    val desc: String = "",
    val other: String = ""
)

data class ExtensionSubItem(
    val title: String,
    var checked: Boolean,
    val tag: String,
    val desc: String = "",
    val other: String = ""
)

class ManagerItemViewDelegate(val onClick: (tag: String, checked: Boolean) -> Unit) :
    ViewDelegate<ExtensionItem, SelectItemView>() {
    override fun onBindView(view: SelectItemView, item: ExtensionItem) {
        view.apply {
            title.text = item.title
            desc.text = item.desc
            switch.isChecked = item.checked
            setOnClickListener {
                switch.isChecked = !switch.isChecked
                onClick(item.tag, switch.isChecked)
                item.checked = switch.isChecked
            }
        }
    }

    override fun onCreateView(context: Context): SelectItemView {
        return SelectItemView(context)
    }
}


class ManagerSubItemViewDelegate(
    val onClick: (tag: String, checked: Boolean) -> Unit, val onSubClick: (tag: String) -> Unit
) : ViewDelegate<ExtensionSubItem, SubSelectItemView>() {
    override fun onBindView(view: SubSelectItemView, item: ExtensionSubItem) {
        view.apply {
            containerView.title.text = item.title
            containerView.desc.text = item.desc
            switch.isChecked = item.checked
            switch.setOnCheckedChangeListener { v, isChecked ->
                if (v.isPressed) {
                    onClick(item.tag, isChecked)
                    item.checked = isChecked
                }
            }
            containerView.setOnClickListener {
                onSubClick(item.tag)
            }
        }
    }

    override fun onCreateView(context: Context): SubSelectItemView {
        return SubSelectItemView(context)
    }
}