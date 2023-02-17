package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalServices
import me.simpleHook.R
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.compat.ConfigSystemUtil
import me.simpleHook.compat.DocumentCompatUtils
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentExtensionManagerBinding
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.ui.view.extension.ExtensionItemTitleView
import me.simpleHook.ui.view.extension.SelectItemView
import me.simpleHook.ui.view.extension.SubSelectItemView
import me.simpleHook.util.*
import me.simpleHook.viewmodel.ExViewModel


class ManagerFragment : Fragment() {
    private var _binding: FragmentExtensionManagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionConfig: AssistConfig
    private val exViewModel by activityViewModels<ExViewModel>()
    private val sp by lazy { SPUtils(requireContext()) }
    private var editMode = true
    private val appViewModel by viewModels<AppViewModel>()
    private val items = ArrayList<Any>()
    private var configBean: ExtensionConfigBean = ExtensionConfigBean()
    private var tempConfigStr = ""
    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }
    private val adapter = MultiTypeAdapter()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireActivity().contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }
    private val dispatcher by lazy { requireActivity().onBackPressedDispatcher }
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtensionManagerBinding.inflate(inflater)
        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        extensionConfig = requireArguments().getParcelable("EXTENSION_CONFIG")!!
        initMenu()
        initView()
        initData()
    }

    private fun initData() {
        if (exViewModel.extensionConfig.value == null) {
            exViewModel.extensionConfig.value = configBean
        }
        exViewModel.extensionConfig.observe(requireActivity()) {
            configBean = it
        }
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val isInstalled =
                    AppUtils.isAppInstalled(requireContext(), extensionConfig.packageName)
                menuInflater.inflate(R.menu.menu_extension_manager, menu)
                if (GlobalServices.packageManager.getLaunchIntentForPackage(extensionConfig.packageName) == null || !FlavorUtils.rootVersion) {
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
                            val intent = GlobalServices.packageManager.getLaunchIntentForPackage(
                                extensionConfig.packageName)
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
                showEditStopDialogKeyWord()
            }
            TAG_FILTER_CLIPBOARD -> {
                val navController = findNavController(requireActivity(), R.id.nav_host_fragment)
                val action =
                    ManagerFragmentDirections.actionManagerFragmentToClipboardFragment(configBean.filterClipboard.info)
                navController.navigate(action)
            }
            TAG_GUISE_SIGN -> {
                val action =
                    ManagerFragmentDirections.actionManagerFragmentToGuiseSignFragment(configBean.guiseSign.info)
                val navController = findNavController(requireActivity(), R.id.nav_host_fragment)
                navController.navigate(action)
            }
        }
    }

    private fun onItemClick(tag: String, checked: Boolean) {
        if (tag == "hotFix") {
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
            else -> {
                Class.forName(ExtensionConfigBean::class.java.name).apply {
                    getDeclaredField(tag).apply {
                        isAccessible = true
                        setBoolean(configBean, checked)
                    }
                }
            }
        }
    }


    private fun showEditStopDialogKeyWord() {
        val inputView = InputView(requireContext())
        inputView.textInputLayout.helperText = getString(R.string.extension_block_dialog_helper_tip)
        inputView.editText.setText(configBean.stopDialog.info)
        customDialog(requireContext(),
            title = getString(R.string.extension_block_dialog_title),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                val keyWords = inputView.editText.text.toString().replace("，", ",").trim()
                configBean.stopDialog.info = keyWords
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel)).show()
    }


    private fun createDexDirectory() {
        val filePath = if (FlavorUtils.rootVersion) {
            val path = Constant.ROOT_CONFIG_MAIN_DIRECTORY + extensionConfig.packageName + "/dex"
            SuUtil.makeDirs(path)
            path
        } else {
            val path = Constant.ANDROID_DATA_PATH + extensionConfig.packageName + "/simpleHook/dex"
            if (OSUtils.atLeastR()) {
                DocumentCompatUtils.makeDirs(requireContext(), path, extensionConfig.packageName)
            } else {
                FileUtils.makeDirs(path)
            }
            path
        }
        ToolUtils.toClip(requireContext(), filePath)
        getString(R.string.extension_tip_dex_path_to_clip).toast(requireContext())
    }

    private fun showFloatWindow() {
        if (sp.startFloat) (requireActivity() as ExtensionActivity).initPrintFloat()
        if (tempConfigStr != configBean.toString()) {
            saveConfig()
        }
    }


    private fun checkPermission(): Boolean {
        if (FlavorUtils.normalVersion && OSUtils.atLeastT() && extensionConfig.packageName != "模板配置" && !PermissionUtils.isGrantPackage(
                extensionConfig.packageName)
        ) {
            requestPermissionDialog(requireContext(),
                message = getString(R.string.android_13_no_permission)) {
                val uri = DocumentCompatUtils.generateAppUri(extensionConfig.packageName)
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
        if (extensionConfig.packageName != "模板配置") {
            saveConfig(extensionConfig.packageName, config)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            getString(R.string.extension_save_success).snack(binding.recyclerView)
            loadingDialog.quickDismiss()
            if (exit) {
                onBackPressedCallback.isEnabled = false
                dispatcher.onBackPressed()
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
        val dexPosition = getString(R.string.extension_dex_position)
        val dexPath = if (FlavorUtils.normalVersion) {
            dexPosition + "/Android/data/${extensionConfig.packageName}/simpleHook/dex/"
        } else {
            dexPosition + "/data/local/tmp/simpleHook/${extensionConfig.packageName}/dex/"
        }
        val config = extensionConfig.config
        configBean =
            if (config.isNotEmpty()) Json.decodeFromString(config) else ExtensionConfigBean()
        tempConfigStr = configBean.toString()
        adapter.register(Title::class.java, TitleViewDelegate())
        adapter.register(ExtensionItem::class.java, ItemViewDelegate { tag, checked ->
            onItemClick(tag, checked)
        })
        adapter.register(ExtensionSubItem::class.java,
            SubItemViewDelegate(onClick = { tag, checked ->
                onItemClick(tag, checked)
            }, onSubClick = { tag -> onSubItemClick(tag) }))
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(),
            LinearLayout.VERTICAL))
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
                add(Title("WebView"))
                add(ExtensionItem(title = "loadUrl",
                    webLoadUrl,
                    "webLoadUrl",
                    getString(R.string.extension_item_desc_web_load_url)))
                add(ExtensionItem(title = "Debug",
                    webDebug,
                    "webDebug",
                    getString(R.string.extension_item_desc_web_debug)))
                add(Title(getString(R.string.extension_item_title_others)))
                add(ExtensionItem(getString(R.string.extension_item_title_signature),
                    signature,
                    "signature",
                    getString(R.string.extension_item_desc_signature)))
                add(ExtensionSubItem(getString(R.string.extension_item_title_guise_sign),
                    guiseSign.enable,
                    TAG_GUISE_SIGN,
                    getString(R.string.extension_item_desc_guise_sign)))
                add(ExtensionItem(getString(R.string.extension_item_title_intent),
                    intent,
                    "intent",
                    getString(R.string.extension_item_desc_intent)))
                add(ExtensionSubItem(title = getString(R.string.extension_item_title_filter_clipboard),
                    filterClipboard.enable,
                    TAG_FILTER_CLIPBOARD,
                    getString(R.string.extension_item_desc_filter_clipboard)))
                add(ExtensionItem(title = "Application",
                    application,
                    "application",
                    getString(R.string.extension_item_desc_application_name)))
                add(ExtensionItem(title = "ADB",
                    adb,
                    "adb",
                    getString(R.string.extension_item_desc_adb)))
                add(Title(getString(R.string.extension_item_title_network)))
                add(ExtensionItem(getString(R.string.extension_item_title_vpn),
                    vpn,
                    "vpn",
                    getString(R.string.extension_item_desc_vpn)))
            }
            adapter.items = items
            adapter.notifyDataSetChanged()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tempConfigStr == configBean.toString()) {
                    onBackPressedCallback.isEnabled = false
                    dispatcher.onBackPressed()
                } else {
                    customDialog(requireContext(),
                        title = getString(R.string.save_config_warning),
                        message = getString(R.string.save_config_warning_message),
                        okText = getString(R.string.save_and_exit),
                        okClick = {
                            saveConfig(exit = true)
                        },
                        neutralText = getString(R.string.exit),
                        neutralClick = {
                            onBackPressedCallback.isEnabled = false
                            dispatcher.onBackPressed()
                        },
                        cancelText = getString(R.string.only_save),
                        cancelClick = {
                            saveConfig()
                        }).show()
                }
            }
        }
        dispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_STOP_DIALOG = "stop_dialog"
        private const val TAG_FILTER_CLIPBOARD = "filter_clip_board"
        private const val TAG_GUISE_SIGN = "guise_sign"
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

class ItemViewDelegate(val onClick: (tag: String, checked: Boolean) -> Unit) :
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


class SubItemViewDelegate(
    val onClick: (tag: String, checked: Boolean) -> Unit, val onSubClick: (tag: String) -> Unit
) : ViewDelegate<ExtensionSubItem, SubSelectItemView>() {
    override fun onBindView(view: SubSelectItemView, item: ExtensionSubItem) {
        view.apply {
            containerView.title.text = item.title
            containerView.desc.text = item.desc
            switch.isChecked = item.checked
            switch.setOnCheckedChangeListener { _, isChecked ->
                onClick(item.tag, isChecked)
                item.checked = isChecked
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