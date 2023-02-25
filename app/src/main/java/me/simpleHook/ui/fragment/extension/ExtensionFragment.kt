package me.simpleHook.ui.fragment.extension

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter
import me.simpleHook.base.BaseExtensionFragment
import me.simpleHook.constant.Constant.MODEL_EXTENSION_CONFIG
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAssistBinding
import me.simpleHook.extension.dp
import me.simpleHook.extension.showToast
import me.simpleHook.ui.activity.AppListActivity
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.util.LanguageUtils
import java.util.*
import kotlin.math.min

class ExtensionFragment : BaseExtensionFragment<FragmentAssistBinding>() {

    private val appViewModel by activityViewModels<AppViewModel>()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private lateinit var mContext: Context
    private var isFabShow = true
    private var fabHideDistance = 0f
    private val mAdapter: AssistAdapter by lazy {
        AssistAdapter({ assistConfig -> itemOnClick(assistConfig) },
            { assistConfig -> itemOnLongClick(assistConfig) })
    }

    private val startActivityForModelCreate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data = it.data!!
                val appName = data.getStringExtra("appName")!!
                val packageName = data.getStringExtra("packageName")!!
                if (currentModel == -1) {
                    appViewModel.insertAssistConfigs(AssistConfig(appName = appName,
                        packageName = packageName))
                } else {
                    val modelConfig = modelList[currentModel]
                    modelConfig.appName = appName
                    modelConfig.packageName = packageName
                    modelConfig.id = 0
                    currentModel = -1
                    saveConfig(modelConfig)
                }
            }
        }
    private var modelList = mutableListOf<AssistConfig>()
    private var currentModel = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireActivity()
    }

    override fun canBack(): Boolean {
        return true
    }

    override fun performBack() {

    }

    override fun notBackTip() {

    }

    override fun enableCallback() = false

    override fun init() {
        initMenu()
        initView()
        initData()
    }


    private fun initData() {
        appViewModel.getAllAssistConfigs().observe(viewLifecycleOwner) {
            modelList.clear()
            val showList = mutableListOf<AssistConfig>()
            for (assist in it) {
                if (assist.packageName == MODEL_EXTENSION_CONFIG) {
                    modelList.add(assist)
                } else {
                    showList.add(assist)
                }
            }
            binding.emptyTip.visibility = if (showList.isEmpty()) View.VISIBLE else View.GONE
            mAdapter.submitList(showList)
            binding.progressBar4.hide()
        }
    }

    private fun initView() {
        binding.progressBar4.show()
        var maybeABug = 0
        val layoutParams = binding.addConfig.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture =
                navigationInsets.bottom <= 20 * requireActivity().resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(binding.root, windowInsets)
            maybeABug = if (maybeABug == 0) {
                bottomNavigationView.bottom - bottomNavigationView.top
            } else {
                min(maybeABug, bottomNavigationView.bottom - bottomNavigationView.top)
            }
            if (navigationInsets.bottom == 0) maybeABug += 10.dp
            layoutParams.bottomMargin = if (isGesture) maybeABug + navigationInsets.bottom
            else maybeABug + navigationInsets.bottom / 5
            fabHideDistance = layoutParams.bottomMargin.toFloat() * 2
            binding.addConfig.layoutParams = layoutParams
            binding.assistRev.updatePadding(bottom = maybeABug / 2)
            windowInsets
        }
        binding.apply {
            addConfig.setOnClickListener {
                addConfig()
            }
            addConfig.setOnLongClickListener {
                directAddConfig()
                true
            }
            assistRev.apply {
                adapter = mAdapter
                layoutManager = GridLayoutManager(requireContext(), 2)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    private var distance = 0
                    private var visible = true
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (distance > 20 && visible) {
                            visible = false
                            hideFab()
                            distance = 0
                        } else if (distance < -20 && !visible) {
                            visible = true
                            distance = 0
                            showFab()
                        }
                        if (visible && dy > 0 || !visible && dy < 0) {
                            distance += dy
                        }
                    }
                })
            }
        }
        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.assistRev.scrollToPosition(0)
            }
        })
        FastScrollerUtil.bind(binding.assistRev)
    }

    private fun showFab() {
        binding.addConfig.animate().translationY(0f).interpolator = DecelerateInterpolator(1.5f)
        isFabShow = true
    }

    private fun upFab() {
        binding.addConfig.animate().translationY((-50f).dp).interpolator =
            DecelerateInterpolator(1.5f)
    }

    private fun hideFab() {
        binding.addConfig.animate().translationY(fabHideDistance).interpolator =
            DecelerateInterpolator(1.5f)
        isFabShow = false
    }

    private fun itemOnLongClick(assistConfig: AssistConfig) {
        if (configSystem.isEnableDelete(assistConfig.packageName)) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                appViewModel.deleteAssistConfigs(assistConfig)
                configSystem.deleteExConfig(assistConfig.packageName)
            }
            Snackbar.make(binding.addConfig,
                getString(R.string.main_extension_delete_config_tip),
                Snackbar.LENGTH_LONG).apply {
                anchorView = bottomNavigationView
            }.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    super.onShown(sb)
                    if (isFabShow) upFab()
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (isFabShow) showFab()
                }
            }).setAction(getString(R.string.main_extension_undo_delete_config)) {
                saveConfig(assistConfig)
            }.show()
        } else {
            requirePermission(assistConfig.packageName)
        }
    }

    private fun saveConfig(assistConfig: AssistConfig) {
        if (configSystem.isEnableSave(assistConfig.packageName)) {
            lifecycleScope.launch(Dispatchers.IO) {
                appViewModel.insertAssistConfigs(assistConfig)
                configSystem.saveExConfig(assistConfig.packageName, assistConfig.config)
            }
        } else {
            requirePermission(assistConfig.packageName)
        }
    }

    private fun itemOnClick(assistConfig: AssistConfig) {
        ExtensionActivity.startActivity(requireContext(), assistConfig)
    }

    private fun addConfig() {
        if (modelList.isEmpty()) {
            directAddConfig()
        } else {
            showSelectModelDialog()
        }
    }

    private fun directAddConfig() {
        val intent = Intent(requireActivity(), AppListActivity::class.java).apply {
            putExtra("isFromAssist", true)
        }
        startActivityForModelCreate.launch(intent)
    }

    private fun createModel() {
        val assistConfig = AssistConfig(appName = "", packageName = MODEL_EXTENSION_CONFIG)
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint = "给模板起个名字"
            textInputLayout.counterMaxLength = 15
            textInputLayout.isCounterEnabled = true
        }
        customDialog(mContext, title = "创建模板", contentView = inputView, okText = "去创建", okClick = {
            val modelName = inputView.editText.text.toString()
            if (modelName.isNotEmpty() || modelName.length > 15) {
                assistConfig.appName = modelName
                ExtensionActivity.startActivity(requireContext(), assistConfig, false)
            } else {
                requireActivity().showToast("不能为空或名字太长")
            }
        }, cancelText = "取消").show()
    }

    private fun showModelDialog() {
        if (modelList.isEmpty()) {
            requireActivity().showToast("请先创建")
            return
        }
        ModelBottomFragment("edit").show(requireActivity().supportFragmentManager, "model")
    }

    private fun showSelectModelDialog() {
        val showList = mutableListOf<String>()
        modelList.forEach {
            showList.add(it.appName)
        }
        val listView = ListView(mContext)
        val adapter = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, showList)
        listView.adapter = adapter
        val dialog = customDialog(
            mContext,
            title = "选择一个模板",
            contentView = listView,
        )
        listView.setOnItemClickListener { _, _, position, _ ->
            currentModel = position
            val intent = Intent(requireActivity(), AppListActivity::class.java).apply {
                putExtra("isFromAssist", true)
            }
            startActivityForModelCreate.launch(intent)
            dialog.cancel()
        }
        dialog.show()

    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_assist_fragment, menu)
                if (LanguageUtils.isNotChinese() || sp.language == Locale.ENGLISH.language) {
                    menu.removeItem(R.id.create_model)
                    menu.removeItem(R.id.show_model)
                    menu.removeItem(R.id.about_model)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.startFloat -> {
                        (requireActivity() as MainActivity).initPrintFloat()
                    }
                    R.id.create_model -> createModel()
                    R.id.show_model -> showModelDialog()
                    R.id.about_model -> showAboutModel()
                }
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showAboutModel() {
        warningDialog(mContext, title = "关于模板", message = """
            创建模板后，在创建配置的时候可以选择模板，所创建的配置中的选中状态和模板一样，简化操作
            查看模板：
                ->点击模板（进入编辑模式）
                ->长按模板（删除模板）
            长按加号按钮：不使用模板选择App
        """.trimIndent())
    }


}