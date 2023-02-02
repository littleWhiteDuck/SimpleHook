package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.BasicViewHolder
import me.simpleHook.adapter.BasicViewHolderFactory
import me.simpleHook.adapter.MultiTypeAdapter
import me.simpleHook.bean.AssistItem
import me.simpleHook.bean.AssistTitle
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.config.ConfigHelper
import me.simpleHook.constant.Constant
import me.simpleHook.constant.Constant.ANDROID_DATA_PATH
import me.simpleHook.constant.Constant.MODEL_EXTENSION_CONFIG
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityExtensionBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.ui.view.extension.ExtensionItemTitleView
import me.simpleHook.ui.view.extension.ExtensionItemView
import me.simpleHook.util.*
import javax.crypto.Mac

private const val TAG_START_APP = "start_app"
private const val TAG_STOP_DIALOG = "stop_dialog"
private const val TAG_FILTER_CLIPBOARD = "filter_clip_board"

class ExtensionActivity : BaseActivity() {


    private lateinit var binding: ActivityExtensionBinding
    private lateinit var assistConfig: AssistConfig
    private val sp by lazy { SPUtils(this) }
    private var editMode = true
    private val appViewModel by viewModels<AppViewModel>()
    private val itemList = ArrayList<Any>()
    private var statusChecked = 0
    private var statusUnChecked = 0
    private lateinit var configBean: ExtensionConfigBean
    private var tempConfigStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionBinding.inflate(layoutInflater)
        Mac::class.java
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.recyclerView.apply {
                setPadding(paddingLeft, paddingTop, paddingRight, insets.bottom)
            }
            ViewCompat.onApplyWindowInsets(window.decorView, windowInsets)
            windowInsets
        }
        editMode = intent.getBooleanExtra("editMode", true)
        val bundle = intent.getBundleExtra("bundle")
        assistConfig = bundle!!.getParcelable("assistConfig")!!
        supportActionBar?.title = assistConfig.appName
        supportActionBar?.subtitle = assistConfig.packageName
        initData()
        initView()
    }

    private fun initData() {
        val dexPosition = getString(R.string.extension_dex_position)
        val dexPath = if (FlavorUtils.isNormal()) {
            dexPosition + "/Android/data/${assistConfig.packageName}/simpleHook/dex/"
        } else {
            dexPosition + "/data/local/tmp/simpleHook/${assistConfig.packageName}/dex/"
        }
        val config = assistConfig.config
        configBean = if (config.isNotEmpty()) Gson().fromJson(
            config, ExtensionConfigBean::class.java
        ) else ExtensionConfigBean()
        tempConfigStr = configBean.toString()
        itemList.apply {
            configBean.apply {
                add(AssistTitle(getString(R.string.extension_item_title_basic)))
                add(
                    AssistItem(
                        assistConfig.appName, false, TAG_START_APP, assistConfig.packageName, ""
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_all_switch), all, "all", getString(
                            R.string.extension_item_desc_all_switch
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_hook_success_tip),
                        tip,
                        "tip",
                        getString(
                            R.string.extension_item_desc_hook_success_tip
                        )
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_algorithm_analysis)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_base64),
                        base64,
                        "base64",
                        getString(
                            R.string.extension_item_desc_base64
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_digest_algorithm),
                        digest,
                        "digest",
                        getString(
                            R.string.extension_item_desc_digest_algorithm
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_hmac), hmac, "hmac", getString(
                            R.string.extension_item_desc_hmac
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_encrypt_algorithm),
                        crypt,
                        "crypt",
                        getString(
                            R.string.extension_item_desc_encrypt_algorithm
                        )
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_hot_fix)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_hot_fix_dex),
                        hotFix,
                        "hotFix",
                        dexPath
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_ui)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_dialog),
                        dialog,
                        "dialog",
                        getString(
                            R.string.extension_item_desc_dialog
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_dialog_cancel),
                        diaCancel,
                        "diaCancel",
                        getString(
                            R.string.extension_item_desc_dialog_cancel
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_toast), toast, "toast", getString(
                            R.string.extension_item_desc_toast
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_popup_window),
                        popup,
                        "popup",
                        getString(
                            R.string.extension_item_desc_popup_window
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_popup_window_cancel),
                        popCancel,
                        "popCancel",
                        getString(
                            R.string.extension_item_desc_popup_window_cancel
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_click_event),
                        click,
                        "click",
                        getString(
                            R.string.extension_item_desc_click_event
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_block_dialog),
                        stopDialog.enable,
                        TAG_STOP_DIALOG,
                        getString(
                            R.string.extension_item_desc_block_dialog
                        )
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_security)))
                add(
                    AssistItem(
                        title = getString(R.string.extension_item_title_disable_sensor),
                        disSensorAG,
                        "disSensorAG",
                        getString(R.string.extension_item_title_disable_acceleration_gyroscope)
                    )
                )
                add(
                    AssistItem(
                        title = getString(R.string.extension_item_title_disable_sensor),
                        disSensorSport,
                        "disSensorSport",
                        getString(R.string.extension_item_title_disable_sport_sensor)
                    )
                )
                add(
                    AssistItem(
                        title = getString(R.string.extension_item_title_contact),
                        contact,
                        "contact",
                        getString(R.string.extension_item_desc_contact)
                    )
                )
                add(AssistTitle("JSON"))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_json_object),
                        jsonObject,
                        "jsonObject",
                        getString(R.string.extension_item_desc_json_object)
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_json_array),
                        jsonArray,
                        "jsonArray",
                        getString(R.string.extension_item_desc_json_array)
                    )
                )
                add(AssistTitle("WebView"))
                add(
                    AssistItem(
                        title = "loadUrl",
                        webLoadUrl,
                        "webLoadUrl",
                        getString(R.string.extension_item_desc_web_load_url)
                    )
                )
                add(
                    AssistItem(
                        title = "Debug",
                        webDebug,
                        "webDebug",
                        getString(R.string.extension_item_desc_web_debug)
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_others)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_signature),
                        signature,
                        "signature",
                        getString(R.string.extension_item_desc_signature)
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_intent),
                        intent,
                        "intent",
                        getString(
                            R.string.extension_item_desc_intent
                        )
                    )
                )
                add(
                    AssistItem(
                        title = getString(R.string.extension_item_title_filter_clipboard),
                        filterClipboard.enable,
                        TAG_FILTER_CLIPBOARD,
                        getString(R.string.extension_item_desc_filter_clipboard)
                    )
                )
                add(
                    AssistItem(
                        title = "Application",
                        application,
                        "application",
                        getString(R.string.extension_item_desc_application_name)
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_network)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_vpn), vpn, "vpn", getString(
                            R.string.extension_item_desc_vpn
                        )
                    )
                )
            }
        }
    }

    private fun initView() {
        val mAdapter = MultiTypeAdapter(itemList, object : BasicViewHolderFactory() {
            override fun getItemViewType(position: Int, data: Any) = when (data) {
                is AssistTitle -> 1
                is AssistItem -> 2
                else -> throw IllegalArgumentException("unknown data: $data")
            }

            override fun getItemView(parent: ViewGroup, viewType: Int) = when (viewType) {
                1 -> ExtensionItemTitleView(parent.context)
                2 -> ExtensionItemView(parent.context)
                else -> throw IllegalArgumentException("unknown viewType: $viewType")
            }

            override fun onCreateViewHolder(
                parent: ViewGroup, itemView: View
            ): BasicViewHolder<*> {
                return when (itemView) {
                    is ExtensionItemTitleView -> TitleHolder(itemView)
                    is ExtensionItemView -> ItemHolder(itemView, onChangeChecked = { checked, tag ->
                        onChangeChecked(
                            checked, tag
                        )
                    }, onClick = {
                        onItemClick(it)
                    })
                    else -> throw IllegalArgumentException("unknown view: $itemView")
                }
            }


        })
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@ExtensionActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@ExtensionActivity, LinearLayoutManager.VERTICAL
                )
            )
        }
    }


    private fun onItemClick(tag: String) {
        if (tag == TAG_STOP_DIALOG) {
            showEditStopDialogKeyWord()
        } else if (tag == TAG_FILTER_CLIPBOARD) {
            showEditFilterClipboardKeyWord()
        }
    }


    private fun onChangeChecked(checked: Boolean, tag: String) {
        if (tag == TAG_START_APP) {
            if (assistConfig.packageName == MODEL_EXTENSION_CONFIG) return
            saveConfig()
            Handler(Looper.getMainLooper()).postDelayed({
                startAppAndFloat()
            }, 100)
            return
        } else {
            if (tag == "hotFix" && checked && assistConfig.packageName != MODEL_EXTENSION_CONFIG) {
                createDexDirectory()
            }
            refreshConfigBean(tag, checked)
        }
    }

    private fun showEditStopDialogKeyWord() {
        val inputView = InputView(this)
        inputView.textInputLayout.helperText = getString(R.string.extension_block_dialog_helper_tip)
        inputView.editText.setText(configBean.stopDialog.info)
        customDialog(
            this,
            title = getString(R.string.extension_block_dialog_title),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                val keyWords = inputView.editText.text.toString().replace("，", ",").trim()
                configBean.stopDialog.info = keyWords
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun showEditFilterClipboardKeyWord() {
        val inputView = InputView(this)
        inputView.textInputLayout.helperText = getString(R.string.extension_filter_clipboard_helper)
        inputView.editText.setText(configBean.filterClipboard.info)
        customDialog(
            this,
            title = getString(R.string.extension_filter_clipboard_title),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                val keyWords = inputView.editText.text.toString().trim()
                configBean.filterClipboard.info = keyWords
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun createDexDirectory(tipToast: Boolean = true) {
        val tip = """
                     1. 无效，取消此应用作用域，再给此应用作用域
                     2. 无效，清除数据，重复1
                     3. 无效，卸载重装，重复1
                     4. 无效，重启系统，如依旧用不了，那就是用不了
                    """.trimIndent()
        if (FlavorUtils.isNormal()) {
            if (FileUtils.isGrant(this)) {
                val filePath = ANDROID_DATA_PATH + assistConfig.packageName + "/simpleHook/dex/"
                FileUtils.writeTextToFile(
                    tip, filePath, "说明.txt"
                )
                if (tipToast) {
                    ToolUtils.toClip(this, filePath)
                    "dex存放目录已复制到剪切板中".toast(this)
                }

            } else {
                requestPermissionDialog(this) {
                    FileUtils.verifyStoragePermissions(this)
                }
            }
        } else {
            val filePath =
                Constant.ROOT_CONFIG_MAIN_DIRECTORY + assistConfig.packageName + "/dex/说明.txt"
            val suFile = SuFile.open(filePath)
            if (!suFile.exists()) {
                suFile.parentFile?.mkdirs()
            }
            SuFileOutputStream.open(suFile).writer().use {
                it.write(tip)
            }
            if (tipToast) {
                ToolUtils.toClip(this, filePath)
                "dex存放目录已复制到剪切板中".toast(this)
            }
        }
    }

    private fun startAppAndFloat() {
        if (sp.startFloat) initPrintFloat()
        AppUtils.startApp(assistConfig.packageName, this)
    }

    private fun isContains(state: Int) =
        statusChecked isContainState state || statusUnChecked isContainState state

    private fun isChecked(state: Int) = statusChecked isContainState state

    private fun refreshConfigBean(tag: String, isChecked: Boolean) {
        when (tag) {
            TAG_STOP_DIALOG -> {
                configBean.stopDialog.enable = isChecked
            }
            TAG_FILTER_CLIPBOARD -> {
                configBean.filterClipboard.enable = isChecked
            }
            else -> {
                Class.forName(ExtensionConfigBean::class.java.name).apply {
                    getDeclaredField(tag).apply {
                        isAccessible = true
                        setBoolean(configBean, isChecked)
                    }
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun saveConfig(exit: Boolean = false) {
        val loadingDialog = LoadingDialog(this, getString(R.string.main_loading))
        loadingDialog.show()
        val config = Gson().toJson(configBean)
        tempConfigStr = configBean.toString()
        assistConfig.config = config
        assistConfig.allSwitch = configBean.all
        if (editMode) {
            appViewModel.updateAssistConfigs(assistConfig)
        } else {
            appViewModel.insertAssistConfigs(assistConfig)
        }
        if (assistConfig.packageName != "模板配置") {
            saveToText(assistConfig.packageName, config)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            loadingDialog.quickDismiss()
            if (exit) finish()
        }, 500)

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (tempConfigStr != configBean.toString()) {
            customDialog(
                this,
                title = getString(R.string.save_config_warning),
                message = getString(R.string.save_config_warning_message),
                okText = getString(R.string.save_and_exit),
                okClick = {
                    saveConfig(exit = true)
                },
                neutralText = getString(R.string.exit),
                neutralClick = {
                    finish()
                },
                cancelText = getString(R.string.only_save),
                cancelClick = {
                    saveConfig()
                }).show()
        } else {
            finish()
        }
    }

    private fun saveToText(packageName: String, config: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigHelper.saveConfig(
                this@ExtensionActivity, packageName, Constant.EXTENSION_CONFIG_NAME, config
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val isInstalled = AppUtils.isAppInstalled(this, assistConfig.packageName)
        menuInflater.inflate(R.menu.menu_assist, menu)
        if (packageManager.getLaunchIntentForPackage(packageName) == null || Shell.isAppGrantedRoot() != true) {
            menu.removeItem(R.id.menu_relaunch)
        }
        if (!isInstalled) {
            menu.removeItem(R.id.menu_force_stop)
            menu.removeItem(R.id.menu_relaunch)
            menu.removeItem(R.id.menu_app_info)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
            R.id.menu_force_stop -> {
                if (Shell.isAppGrantedRoot() == true) {
                    SuUtil.forceStopApp(assistConfig.packageName)
                } else {
                    AppUtils.jumpAppInfoPage(this, assistConfig.packageName)
                }
            }
            R.id.menu_relaunch -> {
                val intent = packageManager.getLaunchIntentForPackage(assistConfig.packageName)
                intent?.component?.className?.let { className ->
                    SuUtil.reLaunchApp(assistConfig.packageName, className)
                }
            }
            R.id.menu_app_info -> AppUtils.jumpAppInfoPage(this, assistConfig.packageName)
        }
        return true
    }

    class TitleHolder(itemView: View) : BasicViewHolder<AssistTitle>(itemView) {
        private val tvTitle = itemView as ExtensionItemTitleView
        override fun onBindData(position: Int, data: AssistTitle) {
            tvTitle.text = data.title
        }
    }

    class ItemHolder(
        itemView: View,
        val onChangeChecked: (Boolean, String) -> Unit,
        val onClick: (String) -> Unit
    ) : BasicViewHolder<AssistItem>(itemView) {
        private val assistItemView = itemView as ExtensionItemView
        private val tvTitle: TextView = assistItemView.title
        private val tvDesc: TextView = assistItemView.desc
        private val tvControl: TextView = assistItemView.control
        private val lineView: View = assistItemView.lineView
        override fun onBindData(position: Int, data: AssistItem) {
            tvDesc.isVisible = data.desc.isNotEmpty()
            tvTitle.text = data.title
            tvDesc.text = data.desc
            tvControl.isVisible = data.tag != TAG_START_APP
            lineView.isVisible = data.tag == TAG_FILTER_CLIPBOARD || data.tag == TAG_STOP_DIALOG
            when {
                data.tag == TAG_START_APP -> tvControl.text = data.other
                data.isChecked -> {
                    tvControl.text = itemView.context.getString(R.string.extension_item_status_open)
                    tvControl.setTextColor(Color.parseColor("#4F9BFA"))
                }
                else -> {
                    tvControl.text =
                        itemView.context.getString(R.string.extension_item_status_close)
                    tvControl.setTextColor(Color.parseColor("#aaaaaa"))
                }
            }
            if (data.tag == TAG_STOP_DIALOG || data.tag == TAG_FILTER_CLIPBOARD) {
                tvControl.setOnClickListener {
                    data.isChecked = !data.isChecked
                    tvControl.apply {
                        data.apply {
                            when {
                                isChecked -> {
                                    text =
                                        itemView.context.getString(R.string.extension_item_status_open)
                                    setTextColor(Color.parseColor("#4F9BFA"))
                                    onChangeChecked(true, tag)
                                }
                                else -> {
                                    text =
                                        itemView.context.getString(R.string.extension_item_status_close)
                                    setTextColor(Color.parseColor("#aaaaaa"))
                                    onChangeChecked(false, tag)
                                }
                            }
                        }
                    }
                }
                itemView.setOnClickListener {
                    onClick(data.tag)
                }
            } else {
                itemView.setOnClickListener {
                    data.isChecked = !data.isChecked
                    tvControl.apply {
                        data.apply {
                            when {
                                tag == TAG_START_APP -> {
                                    onChangeChecked(false, tag)
                                }
                                isChecked -> {
                                    text =
                                        itemView.context.getString(R.string.extension_item_status_open)
                                    setTextColor(Color.parseColor("#4F9BFA"))
                                    onChangeChecked(true, tag)
                                }
                                else -> {
                                    text =
                                        itemView.context.getString(R.string.extension_item_status_close)
                                    setTextColor(Color.parseColor("#aaaaaa"))
                                    onChangeChecked(false, tag)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}