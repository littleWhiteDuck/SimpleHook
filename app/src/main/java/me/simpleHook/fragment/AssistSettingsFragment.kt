package me.simpleHook.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
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
import me.simpleHook.databinding.FragmentAssistSettingsBinding
import me.simpleHook.util.AppUtils
import me.simpleHook.util.toast
import org.json.JSONObject

private const val DIALOG_SWITCH = "dialogSwitch"
private const val TOAST_SWITCH = "toastSwitch"
private const val POPUP_SWITCH = "popupWindowSwitch"
private const val DIALOG_CANCEL = "dialogCancel"
private const val ALL_SWITCH = "allSwitch"

class AssistSettingsFragment : Fragment() {

    private lateinit var binding: FragmentAssistSettingsBinding
    private lateinit var assistConfig: AssistConfig
    private val list = ArrayList<AssistGroup>()
    private val hashMap = HashMap<String, Boolean>()
    private val appViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        assistConfig =
            arguments?.getParcelable("assistConfig") ?: AssistConfig(0, "", true, "出现错误", "错误")
        initData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssistSettingsBinding.inflate(inflater, container, false)
        initView()
        return binding.root
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
            add(AssistItem("Dialog取消", hashMap[DIALOG_CANCEL] == true, DIALOG_CANCEL))
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
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun onClick(checked: Boolean, tag: String) {
        if (tag == "startApp") {
            appViewModel.deleteAllLogs()
            toSaveConfig()
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
        AppUtils.startApp(assistConfig.packageName, requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_add, menu)
        menu.removeItem(R.id.select_app)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val navController = Navigation.findNavController(binding.recyclerView)
                navController.navigateUp()
            }
            R.id.save_config -> {
                toSaveConfig()
            }
        }
        return true
    }

    private fun toSaveConfig() {
        val config =
            "{\"allSwitch\":${hashMap[ALL_SWITCH] == true},\"dialogSwitch\":${hashMap[DIALOG_SWITCH] == true}," +
                    "\"toastSwitch\":${hashMap[TOAST_SWITCH] == true},\"popupWindowSwitch\":${hashMap[POPUP_SWITCH] == true}," +
                    "\"dialogCancel\":${hashMap[DIALOG_CANCEL] == true}}"
        assistConfig.config = config
        appViewModel.updateAssistConfigs(assistConfig)
        Thread.sleep(200)
        "已保存".toast(requireContext())
    }

    private fun initPrintFloat() {
        EasyFloat.with(requireActivity())
            .setLayout(R.layout.float_window_layout) {
                val viewPager = it.findViewById<ViewPager2>(R.id.float_viewpager2)
                viewPager.adapter = object : FragmentStateAdapter(requireActivity()) {
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
        val imageView = ImageView(requireActivity()).apply {
            setImageResource(R.drawable.float_control_icon)
        }
        EasyFloat.with(requireActivity())
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

    override fun onDestroy() {
        super.onDestroy()
        if (EasyFloat.getFloatView("floatControl") != null) {
            EasyFloat.dismiss("floatControl")
        }
        EasyFloat.dismiss("floatPrint")
    }

}