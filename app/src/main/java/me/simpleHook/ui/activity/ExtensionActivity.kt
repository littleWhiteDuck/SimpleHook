package me.simpleHook.ui.activity

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.BasicViewHolder
import me.simpleHook.adapter.BasicViewHolderFactory
import me.simpleHook.adapter.MultiTypeAdapter
import me.simpleHook.bean.AssistConfigBean
import me.simpleHook.bean.AssistItem
import me.simpleHook.bean.AssistTitle
import me.simpleHook.constant.Constant
import me.simpleHook.constant.Constant.ANDROID_DATA_PATH
import me.simpleHook.constant.Constant.MODEL_EXTENSION_CONFIG
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityExtensionBinding
import me.simpleHook.ui.WindowPreferencesManager
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
private const val startAppTag = 666

class AssistActivity : BaseActivity() {


    private lateinit var binding: ActivityExtensionBinding
    private lateinit var assistConfig: AssistConfig
    private val sp by lazy { SPUtils(this) }
    private var editMode = true
    private val appViewModel by viewModels<AppViewModel>()
    private val itemList = ArrayList<Any>()
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            uri?.also {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }
    private var statusChecked = 0
    private var statusUnChecked = 0
    private lateinit var configBean: AssistConfigBean
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
        val dexPosition = if (LanguageUtils.isNotChinese()) "dex on " else "dex放在"
        val dexPath = if (FlavorUtils.isNormal()) {
            dexPosition + "/Android/data/${assistConfig.packageName}/simpleHook/dex/"
        } else {
            dexPosition + "/data/simpleHook//${assistConfig.packageName}/dex/"
        }
        val config = assistConfig.config
        configBean = if (config.isNotEmpty()) Gson().fromJson(
            config, AssistConfigBean::class.java
        ) else AssistConfigBean()
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
                    is ExtensionItemView -> ItemHolder(itemView) { checked, tag ->
                        onClick(
                            checked, tag
                        )
                    }
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

    private fun onClick(checked: Boolean, tag: Int) {
        if (tag == startAppTag) {
            if (assistConfig.packageName == MODEL_EXTENSION_CONFIG) return
            saveConfig()
            startAppAndFloat()
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

    private fun createDexDirectory(tipToast: Boolean = true) {
        if (sp.openStorage) {
            val tip = """
                     若是root版：导入dex后，手动给可读权限（Root）或重新打开simpleHook软件（需有Root权限）
                     1. 无效，取消此应用作用域，再给此应用作用域
                     2. 无效，清除数据，重复1
                     3. 无效，卸载重装，重复1
                     4. 无效，与你无缘，用不了
                    """.trimIndent()
            if (FlavorUtils.isNormal()) {
                if (FileUtils.isGrant(this)) {
                    val filePath = ANDROID_DATA_PATH + assistConfig.packageName + "/simpleHook/dex/"
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        FileUtils.writeDocumentFile(
                            content = tip,
                            context = this,
                            path = "/${assistConfig.packageName}/simpleHook/dex/",
                            fileName = "说明.txt",
                            mimiType = "text/plain"
                        )
                    } else {
                        FileUtils.writeTextToFile(
                            tip, filePath, "说明.txt"
                        )
                    }
                    if (tipToast) {
                        ToolUtils.toClip(this, filePath)
                        "dex存放目录已复制到剪切板中".toast(this)
                    }

                } else {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        requestPermissionDialog(this) {
                            startActivityForData.launch(Uri.parse(Constant.ANDROID_DATA_URI))
                        }
                    } else {
                        requestPermissionDialog(this) {
                            FileUtils.verifyStoragePermissions(this)
                        }
                    }
                }

            } else {
                val filePath = Constant.CONFIG_MAIN_DIRECTORY + assistConfig.packageName + "/dex/"
                FileUtils.writeTextToFile(
                    tip, filePath, "说明.txt"
                )
                if (tipToast) {
                    ToolUtils.toClip(this, filePath)
                    "dex存放目录已复制到剪切板中".toast(this)
                }
            }

        } else {
            "未开启增加读取配置：不可用".toast(this)
        }
    }

    private fun startAppAndFloat() {
        val intent = Intent()
        intent.apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
        AppUtils.startApp(assistConfig.packageName, this)
    }

    private fun isContains(state: Int) =
        statusChecked isContainState state || statusUnChecked isContainState state

    private fun isChecked(state: Int) = statusChecked isContainState state
    private fun saveConfig() {

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
        val config = Gson().toJson(configBean)
        assistConfig.config = config
        if (editMode) {
            appViewModel.updateAssistConfigs(assistConfig)
        } else {
            appViewModel.insertAssistConfigs(assistConfig)
        }
        if (assistConfig.packageName != "模板配置") {
            saveToText(assistConfig.packageName, config)
        }
        Thread.sleep(150)
        "已保存".toast(this)
    }

    private fun saveToText(packageName: String, config: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (sp.openStorage) {
                FileUtils.saveConfig(
                    this@AssistActivity, packageName, Constant.EXTENSION_CONFIG_NAME, config
                )
            }
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

    class ItemHolder(itemView: View, val onClick: (Boolean, Int) -> Unit) :
        BasicViewHolder<AssistItem>(itemView) {
        private val assistItemView = itemView as ExtensionItemView
        private val tvTitle: TextView = assistItemView.title
        private val tvDesc: TextView = assistItemView.desc
        private val tvControl: TextView = assistItemView.control
        override fun onBindData(position: Int, data: AssistItem) {
            if (data.desc == "") tvDesc.visibility = View.GONE
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
            itemView.setOnClickListener {
                data.isChecked = !data.isChecked
                tvControl.apply {
                    data.apply {
                        when {
                            tag == startAppTag -> {
                                onClick(false, tag)
                            }
                            isChecked -> {
                                text =
                                    itemView.context.getString(R.string.extension_item_status_open)
                                setTextColor(Color.parseColor("#4F9BFA"))
                                onClick(true, tag)
                            }
                            else -> {
                                text =
                                    itemView.context.getString(R.string.extension_item_status_close)
                                setTextColor(Color.parseColor("#aaaaaa"))
                                onClick(false, tag)
                            }
                        }
                    }
                }
            }
        }

    }

}