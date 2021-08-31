package me.simpleHook.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import me.simpleHook.R
import me.simpleHook.adapter.AssistSettingAdapter
import me.simpleHook.bean.AssistGroup
import me.simpleHook.bean.AssistItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityAssistBinding
import me.simpleHook.ui.fragment.FloatFragment
import me.simpleHook.util.*
import org.json.JSONObject

private const val DIALOG_SWITCH = "dialogSwitch"
private const val TOAST_SWITCH = "toastSwitch"
private const val POPUP_SWITCH = "popupWindowSwitch"
private const val DIALOG_CANCEL = "dialogCancel"
private const val ALL_SWITCH = "allSwitch"

class AssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssistBinding
    private lateinit var assistConfig: AssistConfig
    private val list = ArrayList<AssistGroup>()
    private val hashMap = HashMap<String, Boolean>()
    private val sp by lazy { SPUtils(this) }
    private val assistPref by lazy { XUtils(this, "assistConfig").configPref }
    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val bundle = intent.getBundleExtra("bundle")
        assistConfig = bundle?.getParcelable("assistConfig") ?: AssistConfig(0, "", false, "", "")
        initData()
        initView()
    }


    private fun initData() {
        val config = assistConfig.config
        if (config.isNotEmpty()) {
            JSONObject(config).apply {
                hashMap.apply {
                    put(ALL_SWITCH, getBoolean(ALL_SWITCH))
                    put(DIALOG_SWITCH, getBoolean(DIALOG_SWITCH))
                    put(TOAST_SWITCH, getBoolean(TOAST_SWITCH))
                    put(POPUP_SWITCH, getBoolean(POPUP_SWITCH))
                    put(DIALOG_CANCEL, getBoolean(DIALOG_CANCEL))
                }
            }
        }
        val baseItemList = ArrayList<AssistItem>()
        baseItemList.apply {
            add(AssistItem("启动应用", false, "startApp", assistConfig.appName))
            add(AssistItem("总开关", hashMap[ALL_SWITCH] == true, ALL_SWITCH))
        }
        val baseGroup = AssistGroup("基本", baseItemList)
        val uiItemList = ArrayList<AssistItem>()
        uiItemList.apply {
            add(AssistItem("Dialog调用", hashMap[DIALOG_SWITCH] == true, DIALOG_SWITCH))
            add(AssistItem("Toast调用", hashMap[TOAST_SWITCH] == true, TOAST_SWITCH))
            add(AssistItem("PopupWindow调用", hashMap[POPUP_SWITCH] == true, POPUP_SWITCH))
            add(AssistItem("Dialog强制取消", hashMap[DIALOG_CANCEL] == true, DIALOG_CANCEL))
        }
        val uiGroup = AssistGroup("UI", uiItemList)
        val itemList = ArrayList<AssistItem>()
        val group = AssistGroup("待增加", itemList)
        list.apply {
            add(baseGroup)
            add(uiGroup)
            add(group)
        }

    }

    private fun initView() {
        val mAdapter = AssistSettingAdapter(list) { isChecked, tag -> onClick(isChecked, tag) }
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

    private fun onClick(checked: Boolean, tag: String) {
        if (tag == "startApp") {
            appViewModel.deleteAllLogs()
            saveConfig()
            startAppAndFloat()
            return
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
        val config =
            "{\"allSwitch\":${hashMap[ALL_SWITCH] == true},\"dialogSwitch\":${hashMap[DIALOG_SWITCH] == true}," +
                    "\"toastSwitch\":${hashMap[TOAST_SWITCH] == true},\"popupWindowSwitch\":${hashMap[POPUP_SWITCH] == true}," +
                    "\"dialogCancel\":${hashMap[DIALOG_CANCEL] == true}}"
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