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
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
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
import me.simpleHook.ui.view.extension.ExtensionItemView
import me.simpleHook.util.*
import javax.crypto.Mac

private const val ALL_STATUS = 1
private const val BASE_64_STATUS = 1 shl 1
private const val DIGEST_STATUS = 1 shl 2
private const val HMAC_STATUS = 1 shl 3
private const val CRYPT_STATUS = 1 shl 4
private const val DIALOG_STATUS = 1 shl 5
private const val DIALOG_CANCEL_STATUS = 1 shl 6
private const val TOAST_STATUS = 1 shl 7
private const val POPUP_STATUS = 1 shl 8
private const val POPUP_CANCEL_STATUS = 1 shl 9
private const val CLICK_LISTENER_STATUS = 1 shl 10
private const val INTENT_DATA_STATUS = 1 shl 11
private const val VPN_CHECK_STATUS = 1 shl 12
private const val HOT_FIX_STATUS = 1 shl 13
private const val JSON_OBJECT_STATUS = 1 shl 14
private const val JSON_ARRAY_STATUS = 1 shl 15
private const val WEB_LOAD_URL_STATUS = 1 shl 16
private const val WEB_DEBUG_STATUS = 1 shl 17
private const val STOP_DIALOG_STATUS = 1 shl 18
private const val FILTER_CLIPBOARD_STATUS = 1 shl 19
private const val APPLICATION_STATUS = 1 shl 20
private const val startAppTag = 666

class AssistActivity : BaseActivity() {


    private lateinit var binding: ActivityExtensionBinding
    private lateinit var assistConfig: AssistConfig
    private val sp by lazy { SPUtils(this) }
    private var editMode = true
    private val appViewModel by viewModels<AppViewModel>()
    private val itemList = ArrayList<Any>()
    private var statusChecked = 0
    private var statusUnChecked = 0
    private lateinit var configBean: ExtensionConfigBean

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
        /*lifecycleScope.launch(Dispatchers.IO) {
            if (assistConfig.packageName != MODEL_EXTENSION_CONFIG) {
                saveToText(assistConfig.packageName, "")
            }
        }*/
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
        itemList.apply {
            configBean.apply {
                add(AssistTitle(getString(R.string.extension_item_title_basic)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_application),
                        false,
                        startAppTag,
                        assistConfig.packageName,
                        assistConfig.appName
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_all_switch), all, ALL_STATUS, ""
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_algorithm_analysis)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_base64),
                        base64,
                        BASE_64_STATUS,
                        getString(
                            R.string.extension_item_desc_base64
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_digest_algorithm),
                        digest,
                        DIGEST_STATUS,
                        getString(
                            R.string.extension_item_desc_digest_algorithm
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_hmac), hmac, HMAC_STATUS, getString(
                            R.string.extension_item_desc_hmac
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_encrypt_algorithm),
                        crypt,
                        CRYPT_STATUS,
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
                        HOT_FIX_STATUS,
                        dexPath
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_ui)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_dialog),
                        dialog,
                        DIALOG_STATUS,
                        getString(
                            R.string.extension_item_desc_dialog
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_dialog_cancel),
                        diaCancel,
                        DIALOG_CANCEL_STATUS,
                        getString(
                            R.string.extension_item_desc_dialog_cancel
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_toast),
                        toast,
                        TOAST_STATUS,
                        getString(
                            R.string.extension_item_desc_toast
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_popup_window),
                        popup,
                        POPUP_STATUS,
                        getString(
                            R.string.extension_item_desc_popup_window
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_popup_window_cancel),
                        popCancel,
                        POPUP_CANCEL_STATUS,
                        getString(
                            R.string.extension_item_desc_popup_window_cancel
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_click_event),
                        click,
                        CLICK_LISTENER_STATUS,
                        getString(
                            R.string.extension_item_desc_click_event
                        )
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_block_dialog),
                        stopDialog.enable,
                        STOP_DIALOG_STATUS,
                        getString(
                            R.string.extension_item_desc_block_dialog
                        )
                    )
                )
                add(AssistTitle("JSON"))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_json_object),
                        jsonObject,
                        JSON_OBJECT_STATUS,
                        getString(R.string.extension_item_desc_json_object)
                    )
                )
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_json_array),
                        jsonArray,
                        JSON_ARRAY_STATUS,
                        getString(R.string.extension_item_desc_json_array)
                    )
                )
                add(AssistTitle("WebView"))
                add(
                    AssistItem(
                        title = "loadUrl",
                        webLoadUrl,
                        WEB_LOAD_URL_STATUS,
                        getString(R.string.extension_item_desc_web_load_url)
                    )
                )
                add(
                    AssistItem(
                        title = "Debug",
                        webDebug,
                        WEB_DEBUG_STATUS,
                        getString(R.string.extension_item_desc_web_debug)
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_others)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_intent),
                        intent,
                        INTENT_DATA_STATUS,
                        getString(
                            R.string.extension_item_desc_intent
                        )
                    )
                )
                add(
                    AssistItem(
                        title = getString(R.string.extension_item_title_filter_clipboard),
                        filterClipboard.enable,
                        FILTER_CLIPBOARD_STATUS,
                        getString(R.string.extension_item_desc_filter_clipboard)
                    )
                )
                add(
                    AssistItem(
                        title = "Application",
                        application,
                        APPLICATION_STATUS,
                        getString(R.string.extension_item_desc_application_name)
                    )
                )
                add(AssistTitle(getString(R.string.extension_item_title_network)))
                add(
                    AssistItem(
                        getString(R.string.extension_item_title_vpn),
                        vpn,
                        VPN_CHECK_STATUS,
                        getString(
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
                1 -> AppCompatTextView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.setMargins(16.dp, 0, 0, 0)
                    }
                }
                2 -> ExtensionItemView(parent.context)
                else -> throw IllegalArgumentException("unknown viewType: $viewType")
            }

            override fun onCreateViewHolder(
                parent: ViewGroup, itemView: View
            ): BasicViewHolder<*> {
                return when (itemView) {
                    is AppCompatTextView -> TitleHolder(itemView)
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
            layoutManager = LinearLayoutManager(this@AssistActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@AssistActivity, LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    private fun onItemClick(tag: Int) {
        if (tag == STOP_DIALOG_STATUS) {
            showEditStopDialogKeyWord()
        } else if (tag == FILTER_CLIPBOARD_STATUS) {
            showEditFilterClipboardKeyWord()
        }
    }


    private fun onChangeChecked(checked: Boolean, tag: Int) {
        if (tag == startAppTag) {
            if (assistConfig.packageName == MODEL_EXTENSION_CONFIG) return
            saveConfig()
            Handler(Looper.getMainLooper()).postDelayed({
                startAppAndFloat()
            }, 100)
            return
        } else {
            if (tag == HOT_FIX_STATUS && checked && assistConfig.packageName != MODEL_EXTENSION_CONFIG) {
                createDexDirectory()
            }
            if (checked) {
                if (statusUnChecked isContainState tag) {
                    statusUnChecked = statusUnChecked and tag.inv()
                }
                statusChecked = if (statusChecked == 0) tag else statusChecked or tag
            } else {
                if (statusChecked isContainState tag) {
                    statusChecked = statusChecked and tag.inv()
                }
                statusUnChecked = if (statusUnChecked == 0) tag else statusUnChecked or tag
            }

        }
    }

    private fun showEditStopDialogKeyWord() {
        val textInputLayout = TextInputLayout(this).apply {
            helperText = getString(R.string.extension_block_dialog_helper_tip)
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        val textInput = TextInputEditText(this).apply {
            background = null
            setText(configBean.stopDialog.info)
        }
        textInputLayout.addView(textInput)
        customDialog(
            this,
            title = getString(R.string.extension_block_dialog_title),
            contentView = textInputLayout,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                val keyWords = textInput.text.toString().replace("，", ",").trim()
                configBean.stopDialog.info = keyWords
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun showEditFilterClipboardKeyWord() {
        val textInputLayout = TextInputLayout(this).apply {
            helperText = getString(R.string.extension_filter_clipboard_helper)
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        val textInput = TextInputEditText(this).apply {
            background = null
            setText(configBean.filterClipboard.info)
        }
        textInputLayout.addView(textInput)
        customDialog(
            this,
            title = getString(R.string.extension_filter_clipboard_title),
            contentView = textInputLayout,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                val keyWords = textInput.text.toString().trim()
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

    @SuppressLint("Range")
    private fun saveConfig() {
        isSaving = true
        val loadingDialog = LoadingDialog(this, getString(R.string.main_loading))
        loadingDialog.show()
        if (isContains(ALL_STATUS)) {
            configBean.all = isChecked(ALL_STATUS)
        }
        if (isContains(DIALOG_STATUS)) {
            configBean.dialog = isChecked(DIALOG_STATUS)
        }
        if (isContains(DIALOG_CANCEL_STATUS)) {
            configBean.diaCancel = isChecked(DIALOG_CANCEL_STATUS)
        }
        if (isContains(POPUP_CANCEL_STATUS)) {
            configBean.popCancel = isChecked(POPUP_CANCEL_STATUS)
        }
        if (isContains(POPUP_STATUS)) {
            configBean.popup = isChecked(POPUP_STATUS)
        }
        if (isContains(TOAST_STATUS)) {
            configBean.toast = isChecked(TOAST_STATUS)
        }
        if (isContains(INTENT_DATA_STATUS)) {
            configBean.intent = isChecked(INTENT_DATA_STATUS)
        }
        if (isContains(HOT_FIX_STATUS)) {
            configBean.hotFix = isChecked(HOT_FIX_STATUS)
            if (configBean.hotFix) {
                createDexDirectory(tipToast = false)
            }
        }
        if (isContains(VPN_CHECK_STATUS)) {
            configBean.vpn = isChecked(VPN_CHECK_STATUS)
        }
        if (isContains(CLICK_LISTENER_STATUS)) {
            configBean.click = isChecked(CLICK_LISTENER_STATUS)
        }
        if (isContains(DIGEST_STATUS)) {
            configBean.digest = isChecked(DIGEST_STATUS)
        }
        if (isContains(HMAC_STATUS)) {
            configBean.hmac = isChecked(HMAC_STATUS)
        }
        if (isContains(CRYPT_STATUS)) {
            configBean.crypt = isChecked(CRYPT_STATUS)
        }
        if (isContains(BASE_64_STATUS)) {
            configBean.base64 = isChecked(BASE_64_STATUS)
        }
        if (isContains(JSON_OBJECT_STATUS)) {
            configBean.jsonObject = isChecked(JSON_OBJECT_STATUS)
        }
        if (isContains(JSON_ARRAY_STATUS)) {
            configBean.jsonArray = isChecked(JSON_ARRAY_STATUS)
        }
        if (isContains(WEB_LOAD_URL_STATUS)) {
            configBean.webLoadUrl = isChecked(WEB_LOAD_URL_STATUS)
        }
        if (isContains(WEB_DEBUG_STATUS)) {
            configBean.webDebug = isChecked(WEB_DEBUG_STATUS)
        }
        if (isContains(STOP_DIALOG_STATUS)) {
            configBean.stopDialog.enable = isChecked(STOP_DIALOG_STATUS)
        }

        if (isContains(FILTER_CLIPBOARD_STATUS)) {
            configBean.filterClipboard.enable = isChecked(FILTER_CLIPBOARD_STATUS)
        }
        if (isContains(APPLICATION_STATUS)) {
            configBean.application = isChecked(APPLICATION_STATUS)
        }
        val config = Gson().toJson(configBean)
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
            isSaving = false
            getString(R.string.extension_config_save_success_tip).toast(this)
        }, 500)

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isSaving) super.onBackPressed()
    }

    private fun saveToText(packageName: String, config: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigHelper.saveConfig(
                this@AssistActivity, packageName, Constant.EXTENSION_CONFIG_NAME, config
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_assist, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
        }
        return true
    }

    class TitleHolder(itemView: View) : BasicViewHolder<AssistTitle>(itemView) {
        private val tvTitle = itemView as AppCompatTextView
        override fun onBindData(position: Int, data: AssistTitle) {
            tvTitle.text = data.title
        }
    }

    class ItemHolder(
        itemView: View, val onChangeChecked: (Boolean, Int) -> Unit, val onClick: (Int) -> Unit
    ) : BasicViewHolder<AssistItem>(itemView) {
        private val assistItemView = itemView as ExtensionItemView
        private val tvTitle: TextView = assistItemView.title
        private val tvDesc: TextView = assistItemView.desc
        private val tvControl: TextView = assistItemView.control
        override fun onBindData(position: Int, data: AssistItem) {
            tvDesc.isVisible = data.desc.isNotEmpty()
            tvTitle.text = data.title
            tvDesc.text = data.desc
            when {
                data.tag == startAppTag -> tvControl.text = data.other
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
            if (data.tag == STOP_DIALOG_STATUS || data.tag == FILTER_CLIPBOARD_STATUS) {
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
                                tag == startAppTag -> {
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