package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.base.BaseExtensionVBFragment
import me.simpleHook.compat.BundleCompat
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.ConfigConstant
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.data.ExConfigTag
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.database.entity.ExtensionConfigEntity
import me.simpleHook.databinding.FragmentExtensionManagerBinding
import me.simpleHook.extension.dp
import me.simpleHook.extension.showPopupWithCopyMsg
import me.simpleHook.extension.snack
import me.simpleHook.recyclerview.adapter.DividerItemDecoration
import me.simpleHook.shizuku.ShizukuFileManager
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.view.extension.ExtensionItemTitleView
import me.simpleHook.ui.view.extension.SelectItemView
import me.simpleHook.ui.view.extension.SubNextItemView
import me.simpleHook.ui.view.extension.SubSelectItemView
import me.simpleHook.utils.AppUtil
import me.simpleHook.utils.FileUtil
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.utils.OSUtil
import me.simpleHook.utils.PermissionUtil
import me.simpleHook.utils.SuUtil
import me.simpleHook.viewmodel.AppConfigViewModel
import me.simpleHook.viewmodel.ExViewModel


class ManagerVBFragment : BaseExtensionVBFragment<FragmentExtensionManagerBinding>() {

    private val extensionConfigEntity: ExtensionConfigEntity by lazy {
        BundleCompat.getParcelable(requireArguments(), "EXTENSION_CONFIG")!!
    }
    private val exViewModel by activityViewModels<ExViewModel>()
    private var editMode: Boolean = true
    private val appConfigViewModel by viewModels<AppConfigViewModel>()
    private val items = ArrayList<Any>()
    private var extensionConfig: ExtensionConfig = ExtensionConfig()
    private var tempConfigStr = ""
    private val managerAdapter = MultiTypeAdapter()
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
        exViewModel.initExtensionConfig(extensionConfig)
        exViewModel.extensionConfig.observe(requireActivity()) {
            extensionConfig = it
        }
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val isInstalled =
                    AppUtil.isAppInstalled(extensionConfigEntity.packageName)
                menuInflater.inflate(R.menu.menu_extension_manager, menu)
                menu.findItem(R.id.menu_open_float).isChecked = GlobalValue.sp.startFloat
                menu.findItem(R.id.menu_open_float).setOnMenuItemClickListener {
                    it.isChecked = !it.isChecked
                    GlobalValue.sp.startFloat = it.isChecked
                    true
                }
                if (GlobalValue.packageManager.getLaunchIntentForPackage(extensionConfigEntity.packageName) == null || !FlavorUtil.rootVersion) {
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
                        AppUtil.startApp(extensionConfigEntity.packageName, requireContext())
                    }

                    R.id.menu_save_config -> saveConfig()
                    R.id.menu_force_stop -> {
                        showFloatWindow()
                        if (FlavorUtil.rootVersion) {
                            if (GlobalValue.isRootWork) {
                                SuUtil.forceStopApp(extensionConfigEntity.packageName)
                            } else {
                                ShizukuFileManager.service?.forceStopPackage(extensionConfigEntity.packageName)
                            }
                        } else {
                            AppUtil.jumpAppInfoPage(
                                requireContext(),
                                extensionConfigEntity.packageName
                            )
                        }
                    }

                    R.id.menu_relaunch -> {
                        showFloatWindow()
                        if (FlavorUtil.rootVersion) {
                            val intent =
                                GlobalValue.packageManager.getLaunchIntentForPackage(
                                    extensionConfigEntity.packageName
                                )
                            intent?.component?.className?.let { className ->
                                if (GlobalValue.isRootWork) {
                                    SuUtil.reLaunchApp(extensionConfigEntity.packageName, className)
                                } else {
                                    ShizukuFileManager.service?.reLaunchApp(
                                        extensionConfigEntity.packageName,
                                        className
                                    )
                                }
                            }
                        }
                    }

                    R.id.menu_app_info -> AppUtil.jumpAppInfoPage(
                        requireContext(),
                        extensionConfigEntity.packageName
                    )
                }
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun onSubItemClick(tag: String) {
        when (tag) {
            TAG_BLOCK_DIALOG -> {
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

            TAG_RECORD -> {
                navController.navigate(R.id.action_managerFragment_to_recordSettingsFragment)
            }
        }
    }

    private fun onItemClick(tag: String, checked: Boolean) {
        if (tag == "hotFix" && extensionConfigEntity.packageName != Constant.MODEL_EXTENSION_CONFIG) {
            createDexDirectory()
        }
        enableTag(tag = tag, target = extensionConfig, enabled = checked)
    }


    private fun enableTag(tag: String, target: Any, enabled: Boolean) {
        val tagSegments = tag.split("_")
        target.javaClass.getDeclaredField(tagSegments.first()).apply {
            isAccessible = true
            if (tagSegments.size == 1) {
                setBoolean(target, enabled)
            } else {
                val remainingTag = tag.substringAfter("_")
                val nestedTarget =
                    get(target) ?: throw NullPointerException("check ExtensionConfig field")
                enableTag(remainingTag, nestedTarget, enabled)
            }
        }
    }


    private fun createDexDirectory() {
        val filePath = if (FlavorUtil.rootVersion) {
            val path = ConfigConstant.ROOT_DEX_PATH.format(extensionConfigEntity.packageName)
            SuUtil.makeDirs(path)
            path
        } else {
            val path = ConfigConstant.NORMAL_DEX_PATH.format(extensionConfigEntity.packageName)
            if (OSUtil.atLeastR()) {
                DocumentCompat.makeDirs(requireContext(), path, extensionConfigEntity.packageName)
            } else {
                FileUtil.makeDirs(path)
            }
            path
        }
        requireActivity().showPopupWithCopyMsg(
            title = getString(R.string.extension_tip_dex_path_to_clip),
            message = filePath
        )
    }

    private fun showFloatWindow() {
        if (sp.startFloat) (requireActivity() as ExtensionActivity).initPrintFloat()
        if (tempConfigStr != extensionConfig.toString()) {
            saveConfig()
        }
    }


    private fun checkPermission(): Boolean {
        if (FlavorUtil.normalVersion && OSUtil.atLeastT() && extensionConfigEntity.packageName != Constant.MODEL_EXTENSION_CONFIG && !PermissionUtil.isGrantPackage(
                extensionConfigEntity.packageName
            )
        ) {
            requestPermissionDialog(
                requireContext(),
                message = getString(R.string.android_13_no_permission)
            ) {
                val uri = DocumentCompat.generateAppUri(extensionConfigEntity.packageName)
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
        val config = Json.encodeToString(extensionConfig)
        tempConfigStr = extensionConfig.toString()
        extensionConfigEntity.config = config
        extensionConfigEntity.enable = extensionConfig.all
        if (editMode) {
            appConfigViewModel.updateExtConfigs(extensionConfigEntity)
        } else {
            appConfigViewModel.insertExtConfigs(extensionConfigEntity)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        editMode = requireActivity().intent.getBooleanExtra("EXTENSION_CONFIG_EDIT", true)
        val config = extensionConfigEntity.config
        extensionConfig =
            if (config.isNotEmpty()) Json.decodeFromString(config) else ExtensionConfig()
        tempConfigStr = extensionConfig.toString()
        with(managerAdapter) {
            register(Title::class.java, TitleViewDelegate())
            register(ExtensionItem::class.java, ManagerItemViewDelegate { tag, checked ->
                onItemClick(tag, checked)
            })
            register(
                ExtensionSubItem::class.java,
                ManagerSubItemViewDelegate(onClick = { tag, checked ->
                    onItemClick(tag, checked)
                }, onSubClick = { tag -> onSubItemClick(tag) })
            )
            register(
                ExtensionSubNextItem::class.java,
                ManagerSubNextItemViewDelegate(onSubClick = { tag -> onSubItemClick(tag) })
            )
        }

        with(binding.recyclerView) {
            addItemDecoration(DividerItemDecoration(managerAdapter))
            adapter = managerAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isVerticalScrollBarEnabled = false
        }

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
        with(extensionConfig) {
            with(items) {
                add(Title(getString(R.string.extension_item_title_basic)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_all_switch),
                        all,
                        "all",
                        getString(R.string.extension_item_desc_all_switch)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_hook_success_tip),
                        hookTip,
                        "hookTip",
                        getString(R.string.extension_item_desc_hook_success_tip)
                    )
                )
                add(
                    ExtensionSubNextItem(
                        getString(R.string.extension_item_title_record),
                        recordSettings.enable,
                        TAG_RECORD,
                        getString(R.string.extension_item_desc_record)
                    )
                )
                add(Title(getString(R.string.extension_item_title_algorithm_analysis)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_base64),
                        algorithmConfig.base64,
                        ExConfigTag.BASE64,
                        getString(R.string.extension_item_desc_base64)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_digest_algorithm),
                        algorithmConfig.messageDigest,
                        ExConfigTag.MASSAGE_DIGEST,
                        getString(R.string.extension_item_desc_digest_algorithm)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_hmac),
                        algorithmConfig.hmac,
                        ExConfigTag.HMAC,
                        getString(R.string.extension_item_desc_hmac)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_crypt_algorithm),
                        algorithmConfig.cipher,
                        ExConfigTag.CIPHER,
                        getString(R.string.extension_item_desc_crypt_algorithm)
                    )
                )
                add(Title(getString(R.string.extension_item_title_hot_fix)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_hot_fix_dex),
                        hotFix,
                        "hotFix",
                        getDexDesc()
                    )
                )
                add(Title(getString(R.string.extension_item_title_ui)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_dialog),
                        popupConfig.recordDialog,
                        ExConfigTag.RECORD_DIALOG,
                        getString(R.string.extension_item_desc_dialog)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_dialog_cancel),
                        popupConfig.cancelDialog,
                        ExConfigTag.CANCEL_DIALOG,
                        getString(R.string.extension_item_desc_dialog_cancel)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_toast),
                        popupConfig.recordToast,
                        ExConfigTag.RECORD_TOAST,
                        getString(R.string.extension_item_desc_toast)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_popup_window),
                        popupConfig.recordPopup,
                        ExConfigTag.RECORD_POPUP,
                        getString(R.string.extension_item_desc_popup_window)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_popup_window_cancel),
                        popupConfig.cancelPopup,
                        ExConfigTag.CANCEL_POPUP,
                        getString(R.string.extension_item_desc_popup_window_cancel)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_click_event),
                        click,
                        "click",
                        getString(R.string.extension_item_desc_click_event)
                    )
                )
                add(
                    ExtensionSubItem(
                        getString(R.string.extension_item_title_block_dialog),
                        popupConfig.blockDialog.enable,
                        ExConfigTag.BLOCK_DIALOG,
                        getString(R.string.extension_item_desc_block_dialog)
                    )
                )
                add(Title(getString(R.string.extension_item_title_security)))
                add(
                    ExtensionItem(
                        title = getString(R.string.extension_item_title_disable_sensor),
                        sensorConfig.disableAG,
                        ExConfigTag.DISABLE_AG_SENSOR,
                        getString(R.string.extension_item_title_disable_acceleration_gyroscope)
                    )
                )
                add(
                    ExtensionItem(
                        title = getString(R.string.extension_item_title_disable_sensor),
                        sensorConfig.disableSport,
                        ExConfigTag.DISABLE_SPORT_SENSOR,
                        getString(R.string.extension_item_title_disable_sport_sensor)
                    )
                )
                add(
                    ExtensionItem(
                        title = getString(R.string.extension_item_title_contact),
                        contact,
                        "contact",
                        getString(R.string.extension_item_desc_contact)
                    )
                )
                add(Title("JSON"))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_json_object),
                        jsonConfig.recordObject,
                        ExConfigTag.RECORD_JSON_OBJECT,
                        getString(R.string.extension_item_desc_json_object)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_json_array),
                        jsonConfig.recordArray,
                        ExConfigTag.RECORD_JSON_ARRAY,
                        getString(R.string.extension_item_desc_json_array)
                    )
                )
                add(Title(getString(R.string.extension_item_title_others)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_signature),
                        signConfig.recordSignature,
                        ExConfigTag.RECORD_SIGNATURE,
                        getString(R.string.extension_item_desc_signature)
                    )
                )
                add(
                    ExtensionSubItem(
                        getString(R.string.extension_item_title_guise_sign),
                        signConfig.guiseSign.enable,
                        ExConfigTag.GUISE_SIGN,
                        getString(R.string.extension_item_desc_guise_sign)
                    )
                )
                add(
                    ExtensionSubItem(
                        title = getString(R.string.extension_item_title_filter_clipboard),
                        filterClipboard.enable,
                        TAG_FILTER_CLIPBOARD,
                        getString(R.string.extension_item_desc_filter_clipboard)
                    )
                )
                add(
                    ExtensionSubItem(
                        getString(R.string.extension_item_title_file),
                        fileMonitor.enable,
                        TAG_FILE_MONITOR,
                        getString(R.string.extension_item_desc_file)
                    )
                )
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_intent),
                        intent,
                        "intent",
                        getString(R.string.extension_item_desc_intent)
                    )
                )
                add(
                    ExtensionItem(
                        title = "Application",
                        application,
                        "application",
                        getString(R.string.extension_item_desc_application_name)
                    )
                )
                add(
                    ExtensionItem(
                        title = "ADB",
                        adb,
                        "adb",
                        getString(R.string.extension_item_desc_adb)
                    )
                )
                add(
                    ExtensionSubItem(
                        title = getString(R.string.extension_item_title_app_exit),
                        exitConfig.enable,
                        TAG_APP_EXIT,
                        getString(R.string.extension_item_desc_app_exit)
                    )
                )
                add(Title(getString(R.string.extension_item_title_network)))
                add(
                    ExtensionItem(
                        getString(R.string.extension_item_title_vpn),
                        vpn,
                        "vpn",
                        getString(R.string.extension_item_desc_vpn)
                    )
                )
                add(Title("WebView"))
                add(
                    ExtensionItem(
                        title = "loadUrl",
                        webConfig.recordUrl,
                        ExConfigTag.RECORD_WEB_URL,
                        getString(R.string.extension_item_desc_web_load_url)
                    )
                )
                add(
                    ExtensionItem(
                        title = "Debug",
                        webConfig.enableDebug,
                        ExConfigTag.ENABLE_WEB_DEBUG,
                        getString(R.string.extension_item_desc_web_debug)
                    )
                )
            }
        }
        managerAdapter.items = items
        managerAdapter.notifyDataSetChanged()
    }

    private fun getDexDesc(): String {
        val dexPath = if (FlavorUtil.normalVersion) {
            ConfigConstant.NORMAL_DEX_PATH.format(extensionConfigEntity.packageName)
        } else {
            ConfigConstant.ROOT_DEX_PATH.format(extensionConfigEntity.packageName)
        }
        val dexPathDesc = getString(R.string.extension_dex_desc_format, dexPath)
        val dexDesc = if (OSUtil.atLeastU()) {
            dexPathDesc + "\n" + getString(R.string.extension_dex_extra_desc)
        } else {
            dexPathDesc
        }
        return dexDesc
    }

    override fun canBack(): Boolean {
        return tempConfigStr == extensionConfig.toString()
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
        private const val TAG_BLOCK_DIALOG = ExConfigTag.BLOCK_DIALOG
        private const val TAG_FILTER_CLIPBOARD = ExConfigTag.FILTER_CLIPBOARD
        private const val TAG_GUISE_SIGN = ExConfigTag.GUISE_SIGN
        private const val TAG_FILE_MONITOR = ExConfigTag.FILE_MONITOR
        private const val TAG_APP_EXIT = ExConfigTag.BLOCK_EXIT
        private const val TAG_RECORD = "record"
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

data class ExtensionSubNextItem(
    val title: String,
    var checked: Boolean,
    val tag: String,
    val desc: String = "",
    val other: String = ""
)

class ManagerItemViewDelegate(val onClick: (tag: String, checked: Boolean) -> Unit) :
    ViewDelegate<ExtensionItem, SelectItemView>() {
    override fun onBindView(view: SelectItemView, item: ExtensionItem) {
        with(view) {
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
        with(view) {
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

class ManagerSubNextItemViewDelegate(
    val onSubClick: (tag: String) -> Unit
) : ViewDelegate<ExtensionSubNextItem, SubNextItemView>() {
    override fun onBindView(view: SubNextItemView, item: ExtensionSubNextItem) {
        with(view) {
            title.text = item.title
            desc.text = item.desc
            setOnClickListener {
                onSubClick(item.tag)
            }
        }
    }

    override fun onCreateView(context: Context): SubNextItemView {
        return SubNextItemView(context)
    }
}