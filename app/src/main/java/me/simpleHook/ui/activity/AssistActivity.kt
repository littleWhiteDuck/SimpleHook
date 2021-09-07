package me.simpleHook.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import littleWhiteDuck.WindowPreferencesManager
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
import me.simpleHook.databinding.ActivityAssistBinding
import me.simpleHook.ui.fragment.FloatFragment
import me.simpleHook.util.*

private const val DIALOG_SWITCH = "dialogSwitch"
private const val TOAST_SWITCH = "toastSwitch"
private const val POPUP_SWITCH = "popupSwitch"
private const val POPUP_CANCEL_SWITCH = "popupCancelSwitch"
private const val DIALOG_CANCEL = "dialogCancel"
private const val ALL_SWITCH = "allSwitch"
private const val TINKER_FIX = "hotFix"
private const val INTENT_DATA = "intentData"
private const val VPN_CHECK = "vpnCheck"
private const val CLICK_LISTENER = "click"

class AssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssistBinding
    private lateinit var assistConfig: AssistConfig
    private val hashMap = HashMap<String, Boolean>()
    private val sp by lazy { SPUtils(this) }
    private val assistPref by lazy { XUtils(this, "assistConfig").configPref }
    private val appViewModel by viewModels<AppViewModel>()
    private val itemList = ArrayList<Any>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
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
                put(ALL_SWITCH, all)
                put(DIALOG_SWITCH, dialog)
                put(DIALOG_CANCEL, diaCancel)
                put(TOAST_SWITCH, toast)
                put(POPUP_SWITCH, popup)
                put(TINKER_FIX, tinker)
                put(INTENT_DATA, intent)
                put(VPN_CHECK, vpn)
                put(CLICK_LISTENER, click)
                put(POPUP_CANCEL_SWITCH, popCancel)
            }
        }
        itemList.apply {
            configBean.apply {
                add(AssistTitle("基本"))
                add(AssistItem("启动应用", false, "startApp", assistConfig.packageName, assistConfig.appName))
                add(AssistItem("总开关", all, ALL_SWITCH, ""))
                add(AssistTitle("界面"))
                add(AssistItem("弹窗", dialog, DIALOG_SWITCH, "打印弹窗调用"))
                add(AssistItem("弹窗", diaCancel, DIALOG_CANCEL, "用于一般弹窗的强制可取消"))
                add(AssistItem("Toast", toast, TOAST_SWITCH, "打印toast调用"))
                add(AssistItem("PopupWindow", popup, POPUP_SWITCH, "打印调用（也可作为弹窗）"))
                add(AssistItem("PopupWindow可取消", popCancel, POPUP_CANCEL_SWITCH, "点击弹窗外部/返回取消"))
                add(AssistItem("点击事件",click, CLICK_LISTENER, "打印点击调用"))
                add(AssistTitle("其他"))
                add(AssistItem("intent", intent, INTENT_DATA, "打印常见启动activity时传递的intent"))
                add(AssistItem("热修复", tinker, TINKER_FIX, "dex放在Download/simpleHook\n/hotFix/${assistConfig.packageName}/hotfix.dex"))
                add(AssistTitle("网络"))
                add(AssistItem("vpn", vpn, VPN_CHECK, "去除一般的VPN检测"))
            }
        }
    }

    private fun initView() {
        val mAdapter = MultiTypeAdapter(itemList, object : BasicViewHolderFactory() {
            override fun getLayoutResId(position: Int, data: Any) = when (data) {
                is AssistTitle -> R.layout.item_assist_setting_title
                is AssistItem -> R.layout.item_assist_setting_item
                else -> throw IllegalArgumentException("unknown data: $data")
            }

            override fun onCreateViewHolder(
                inflater: LayoutInflater,
                parent: ViewGroup,
                layoutResId: Int
            ): BasicViewHolder<*> {
                val itemView = inflater.inflate(layoutResId, parent, false)
                return when (layoutResId) {
                    R.layout.item_assist_setting_title -> TitleHolder(itemView)
                    R.layout.item_assist_setting_item -> ItemHolder(itemView) { checked, tag ->
                        onClick(
                            checked,
                            tag
                        )
                    }
                    else -> throw IllegalArgumentException("unknown layoutResId: $layoutResId")
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
        private val tvTitle = itemView as TextView
        override fun onBindData(position: Int, data: AssistTitle) {
            tvTitle.text = data.title
        }
    }

    class ItemHolder(itemView: View, val onClick: (Boolean, String) -> Unit) :
        BasicViewHolder<AssistItem>(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_description)
        private val tvControl: TextView = itemView.findViewById(R.id.control)
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
            appViewModel.deleteAllLogs()
            saveConfig()
            startAppAndFloat()
            return
        }
        if (tag == TINKER_FIX && checked){
            FileUtils.verifyStoragePermissions(this)
            FileUtils.makeRootDirectory("$HOT_FIX_DIRECTORY/${assistConfig.packageName}/")
        }
        hashMap[tag] = checked
    }

    private fun startAppAndFloat() {
        initPrintFloat()
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
            hashMap[ALL_SWITCH] == true,
            hashMap[DIALOG_SWITCH] == true,
            hashMap[POPUP_SWITCH] == true,
            hashMap[DIALOG_CANCEL] == true,
            hashMap[TOAST_SWITCH] == true,
            hashMap[INTENT_DATA] == true,
            hashMap[TINKER_FIX] == true,
            hashMap[VPN_CHECK] == true,
            hashMap[CLICK_LISTENER] == true,
            hashMap[POPUP_CANCEL_SWITCH] == true
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

    private fun initPrintFloat() {
        EasyFloat.with(this)
            .setLayout(R.layout.float_window_layout) {
                val viewPager = it.findViewById<ViewPager2>(R.id.float_viewpager2)
                viewPager.adapter = object : FragmentStateAdapter(this) {
                    override fun getItemCount() = 1

                    override fun createFragment(position: Int) = FloatFragment()
                }
                it.findViewById<ImageView>(R.id.minify_window).setOnClickListener {
                    if (EasyFloat.getFloatView("floatControl") != null) {
                        EasyFloat.show("floatControl")
                    } else {
                        initControlFloat()
                    }
                    EasyFloat.hide("floatPrint")
                }
            }
            .setTag("floatPrint")
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            .setDragEnable(false)
            .setLocation(0, 50)
            .setMatchParent(widthMatch = true, heightMatch = false)
            .setAnimator(DefaultAnimator())
            .show()
    }

    private fun initControlFloat() {
        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.float_control_icon)
        }
        EasyFloat.with(this)
            .setLayout(imageView) {
                it.setOnClickListener {
                    EasyFloat.show("floatPrint")
                    EasyFloat.hide("floatControl")
                }
            }
            .setTag("floatControl")
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            .setDragEnable(true)
            .setLocation(100, 200)
            .setAnimator(DefaultAnimator())
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add, menu)
        menu.removeItem(R.id.select_app)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EasyFloat.getFloatView("floatControl") != null) {
            EasyFloat.dismiss("floatControl")
        }
        EasyFloat.dismiss("floatPrint")
    }
}