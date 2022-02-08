package me.simpleHook.ui.activity

import android.content.Intent
import android.graphics.Color
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.adapter.BasicViewHolder
import me.simpleHook.adapter.BasicViewHolderFactory
import me.simpleHook.adapter.MultiTypeAdapter
import me.simpleHook.bean.AssistConfigBean
import me.simpleHook.bean.AssistItem
import me.simpleHook.bean.AssistTitle
import me.simpleHook.constant.Constant.HOT_FIX_DIRECTORY
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityExtensionBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.activity.ExtensionTag.*
import me.simpleHook.ui.view.extension.ExtensionItemView
import me.simpleHook.util.*
import javax.crypto.Mac

enum class ExtensionTag(val tag: String, val title: String) {
    ALL_SWITCH("all", "总开关"),
    BASE_64("base64", "Base64"),
    DIGEST("digest", "摘要算法"),
    HMAC("hmac", "信息摘要算法"),
    CRYPT("crypt", "加密算法"),
    DIALOG_SWITCH("dialog", "弹窗"),
    DIALOG_CANCEL("diaCancel", "弹窗取消"),
    TOAST_SWITCH("toast", "Toast"),
    POPUP_SWITCH("popup", "popupWindow"),
    POPUP_CANCEL_SWITCH("popCancel", "PopupWindow取消"),
    CLICK_LISTENER("click", "点击事件"),
    INTENT_DATA("intent", "intent"),
    VPN_CHECK("vpn", "vpn"),
    HOT_FIX("hotFix", "热修复")
}


class AssistActivity : BaseActivity() {
    private lateinit var binding: ActivityExtensionBinding
    private lateinit var assistConfig: AssistConfig
    private val hashMap = HashMap<String, Boolean>()
    private val sp by lazy { SPUtils(this) }
    private val assistPref by lazy { XUtils(this, "assistConfig").configPref }
    private val appViewModel by viewModels<AppViewModel>()
    private val itemList = ArrayList<Any>()
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

        val bundle = intent.getBundleExtra("bundle")
        assistConfig = bundle?.getParcelable("assistConfig") ?: AssistConfig(
            0,
            "",
            false,
            "",
            ""
        )
        initData()
        initView()
    }


    private fun initData() {
        val config = assistConfig.config
        val configBean = if (config.isNotEmpty()) Gson().fromJson(
            config,
            AssistConfigBean::class.java
        ) else AssistConfigBean()
        configBean.apply {
            hashMap.apply {
                put(ALL_SWITCH.name, all)
                put(DIALOG_SWITCH.name, dialog)
                put(DIALOG_CANCEL.name, diaCancel)
                put(TOAST_SWITCH.name, toast)
                put(POPUP_SWITCH.name, popup)
                put(HOT_FIX.name, hotFix)
                put(INTENT_DATA.name, intent)
                put(VPN_CHECK.name, vpn)
                put(CLICK_LISTENER.name, click)
                put(POPUP_CANCEL_SWITCH.name, popCancel)
                put(BASE_64.name, base64)
                put(DIGEST.name, digest)
                put(HMAC.name, hmac)
                put(CRYPT.name, crypt)
            }
        }
        itemList.apply {
            configBean.apply {
                add(AssistTitle("基本"))
                add(
                    AssistItem(
                        "应用",
                        false,
                        "startApp",
                        assistConfig.packageName,
                        assistConfig.appName
                    )
                )
                add(AssistItem("总开关", all, ALL_SWITCH.name, ""))
                add(AssistTitle("算法分析(Alpha)"))
                add(AssistItem(BASE_64.title, base64, BASE_64.name, "Base64加解密"))
                add(AssistItem(DIGEST.title, digest, DIGEST.name, "MD5、SHA等"))
                add(AssistItem(HMAC.title, hmac, HMAC.name, "Hmac"))
                add(AssistItem(CRYPT.title, crypt, CRYPT.name, "AES、DES、RSA等"))
                add(AssistTitle("界面"))
                add(AssistItem("弹窗", dialog, DIALOG_SWITCH.name, "打印弹窗调用"))
                add(AssistItem("弹窗", diaCancel, DIALOG_CANCEL.name, "用于一般弹窗的强制可取消"))
                add(AssistItem("Toast", toast, TOAST_SWITCH.name, "打印toast调用"))
                add(AssistItem("PopupWindow", popup, POPUP_SWITCH.name, "打印调用（也可作为弹窗）"))
                add(
                    AssistItem(
                        "PopupWindow可取消",
                        popCancel,
                        POPUP_CANCEL_SWITCH.name,
                        "点击弹窗外部/返回取消"
                    )
                )
                add(AssistItem("点击事件", click, CLICK_LISTENER.name, "打印点击调用"))
                add(AssistTitle("其他"))
                add(AssistItem("intent", intent, INTENT_DATA.name, "打印常见启动activity时传递的intent"))
                add(
                    AssistItem(
                        "热修复",
                        hotFix,
                        HOT_FIX.name,
                        "dex放在Download/simpleHook\n/hotFix/${assistConfig.packageName}/"
                    )
                )
                add(AssistTitle("网络"))
                add(AssistItem("vpn", vpn, VPN_CHECK.name, "去除一般的VPN检测"))
                /* add(AssistTitle("环境"))
                 add(AssistItem("隐藏Xposed", xposed, XPOSED_CHECK, "屏蔽一般的xposed检测"))*/
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
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.setMargins(16.dp, 0, 0, 0)
                    }
                }
                2 -> ExtensionItemView(parent.context)
                else -> throw IllegalArgumentException("unknown viewType: $viewType")
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                itemView: View
            ): BasicViewHolder<*> {
                return when (itemView) {
                    is AppCompatTextView -> TitleHolder(itemView)
                    is ExtensionItemView -> ItemHolder(itemView)
                    { checked, tag ->
                        onClick(
                            checked,
                            tag
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
                    this@AssistActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    class TitleHolder(itemView: View) : BasicViewHolder<AssistTitle>(itemView) {
        private val tvTitle = itemView as AppCompatTextView
        override fun onBindData(position: Int, data: AssistTitle) {
            tvTitle.text = data.title
        }
    }

    class ItemHolder(itemView: View, val onClick: (Boolean, String) -> Unit) :
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
                data.tag == "startApp" -> tvControl.text = data.other
                data.isChecked -> {
                    tvControl.text = "已开启"
                    tvControl.setTextColor(Color.parseColor("#4F9BFA"))
                }
                else -> {
                    tvControl.text = "未开启"
                    tvControl.setTextColor(Color.parseColor("#aaaaaa"))
                }
            }
            itemView.setOnClickListener {
                data.isChecked = !data.isChecked
                tvControl.apply {
                    data.apply {
                        when {
                            tag == "startApp" -> {
                                onClick(false, tag)
                            }
                            isChecked -> {
                                text = "已开启"
                                setTextColor(Color.parseColor("#4F9BFA"))
                                onClick(true, tag)
                            }
                            else -> {
                                text = "未开启"
                                setTextColor(Color.parseColor("#aaaaaa"))
                                onClick(false, tag)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun onClick(checked: Boolean, tag: String) {
        if (tag == "startApp") {
            saveConfig()
            startAppAndFloat()
            return
        }
        if (tag == HOT_FIX.name && checked) {
            FileUtils.verifyStoragePermissions(this)
            FileUtils.makeRootDirectory("$HOT_FIX_DIRECTORY/${assistConfig.packageName}/")
        }
        hashMap[tag] = checked
    }

    private fun startAppAndFloat() {
        /* initPrintFloat()*/
        val intent = Intent()
        intent.apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
        AppUtils.startApp(assistConfig.packageName, this)
    }

    private fun saveConfig() {
        val configBean = AssistConfigBean(
            hashMap[ALL_SWITCH.name] == true,
            hashMap[DIALOG_SWITCH.name] == true,
            hashMap[POPUP_SWITCH.name] == true,
            hashMap[DIALOG_CANCEL.name] == true,
            hashMap[TOAST_SWITCH.name] == true,
            hashMap[INTENT_DATA.name] == true,
            hashMap[HOT_FIX.name] == true,
            hashMap[VPN_CHECK.name] == true,
            hashMap[CLICK_LISTENER.name] == true,
            hashMap[POPUP_CANCEL_SWITCH.name] == true,
            hashMap[DIGEST.name] == true,
            hashMap[HMAC.name] == true,
            hashMap[CRYPT.name] == true,
            hashMap[BASE_64.name] == true
        )
        val config = Gson().toJson(configBean)
        assistConfig.config = config
        appViewModel.updateAssistConfigs(assistConfig)
        if (sp.openStorage) {
            FileUtils.createConfigFile(assistConfig.packageName, assistConfig.config, false)
        }
        if (sp.openXml) {
            assistPref?.edit()?.putString(packageName, config)?.commit()
                ?: "模块未激活，将将无法使用New XSharedPreferences获取配置".toast(this, 1)
        }
        Thread.sleep(150)
        "已保存".toast(this)
    }

    /*   private fun initPrintFloat() {
           EasyFloat.with(this)
               .setLayout(R.layout.float_window_layout) {
                   val viewPager = it.findViewById<ViewPager2>(R.id.float_viewpager2)
                   viewPager.adapter = object : FragmentStateAdapter(this) {
                       override fun getItemCount() = 1

                       override fun createFragment(position: Int) = FloatFragment()
                   }
               }
               .setTag("floatPrint")
               .setShowPattern(ShowPattern.ALL_TIME)
               .setSidePattern(SidePattern.RESULT_HORIZONTAL)
               .setDragEnable(false)
               .setLocation(0, 0)
               .setMatchParent(widthMatch = true, heightMatch = false)
               .setAnimator(DefaultAnimator())
               .show()
       }
   */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_assist, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
            /*  R.id.assistFragment_startFloat -> initPrintFloat()*/
        }
        return true
    }

}