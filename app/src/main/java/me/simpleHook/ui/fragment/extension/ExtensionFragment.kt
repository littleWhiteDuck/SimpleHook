package me.simpleHook.ui.fragment.extension

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.base.BaseViewFragment
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.constant.Constant.MODEL_EXTENSION_CONFIG
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.extension.dp
import me.simpleHook.extension.showPopup
import me.simpleHook.lsposed.LSPosedHelper
import me.simpleHook.recyclerview.adapter.AssistAdapter
import me.simpleHook.ui.activity.AppListActivity
import me.simpleHook.ui.activity.ExtensionActivity
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.ui.view.extension.ExtensionFragmentView
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.OSUtils
import me.simpleHook.util.PermissionUtils
import kotlin.math.min

class ExtensionFragment : BaseViewFragment<ExtensionFragmentView>() {

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
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            if (uri != Uri.EMPTY) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireActivity().contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }

    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }
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


    override fun initRootView(): ExtensionFragmentView {
        return ExtensionFragmentView(requireContext())
    }

    override fun init() {
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
            root.emptyText.visibility = if (showList.isEmpty()) View.VISIBLE else View.GONE
            mAdapter.submitList(showList)
            root.progressBar.isVisible = false
        }
    }

    private fun initView() {
        root.progressBar.isVisible = true
        var maybeABug = 0
        val layoutParams = root.addConfig.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture =
                navigationInsets.bottom <= 20 * requireActivity().resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(root, windowInsets)
            maybeABug = if (maybeABug == 0) {
                bottomNavigationView.bottom - bottomNavigationView.top
            } else {
                min(maybeABug, bottomNavigationView.bottom - bottomNavigationView.top)
            }
            if (navigationInsets.bottom == 0) maybeABug += 10.dp
            layoutParams.bottomMargin = if (isGesture) maybeABug + navigationInsets.bottom
            else maybeABug + navigationInsets.bottom / 5
            fabHideDistance = layoutParams.bottomMargin.toFloat() * 2
            root.addConfig.layoutParams = layoutParams
            root.recyclerView.updatePadding(bottom = maybeABug / 2)
            windowInsets
        }
        root.apply {
            addConfig.setOnClickListener {
                addConfig()
            }
            addConfig.setOnLongClickListener {
                directAddConfig()
                true
            }
            recyclerView.apply {
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
                root.recyclerView.scrollToPosition(0)
            }
        })
        FastScrollerUtil.bind(root.recyclerView)
    }

    private fun showFab() {
        root.addConfig.animate().translationY(0f).interpolator = DecelerateInterpolator(1.5f)
        isFabShow = true
    }

    private fun upFab() {
        root.addConfig.animate().translationY((-50f).dp).interpolator = DecelerateInterpolator(1.5f)
    }

    private fun hideFab() {
        root.addConfig.animate().translationY(fabHideDistance).interpolator =
            DecelerateInterpolator(1.5f)
        isFabShow = false
    }

    private fun itemOnLongClick(assistConfig: AssistConfig) {
        if (configSystem.isEnableDelete(assistConfig.packageName)) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                LSPosedHelper.changeScope(assistConfig.packageName, false)
                appViewModel.deleteAssistConfigs(assistConfig)
                configSystem.deleteExConfig(assistConfig.packageName)
            }
            Snackbar.make(root.addConfig,
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
                LSPosedHelper.changeScope(assistConfig.packageName, true)
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
            textInputLayout.hint = context.getString(R.string.extension_template_edit_name_hint)
            textInputLayout.counterMaxLength = 15
            textInputLayout.isCounterEnabled = true
        }
        customDialog(mContext,
            title = getString(R.string.extension_title_create_template),
            contentView = inputView,
            okText = getString(R.string.extension_go_create_template),
            okClick = {
                val modelName = inputView.editText.text.toString()
                if (modelName.isNotEmpty() || modelName.length > 15) {
                    assistConfig.appName = modelName
                    ExtensionActivity.startActivity(requireContext(), assistConfig, false)
                } else {
                    requireActivity().showPopup(getString(R.string.extension_template_illegal_name))
                }
            },
            cancelText = getString(R.string.dialog_cancel)).show()
    }

    private fun showModelDialog() {
        if (modelList.isEmpty()) {
            requireActivity().showPopup(getString(R.string.extension_no_template_tip))
            return
        }
        ModelBottomViewFragment("edit").show(requireActivity().supportFragmentManager, "model")
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
            title = getString(R.string.extension_title_select_template),
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_assist_fragment, menu)
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

    private fun showAboutModel() {
        warningDialog(mContext,
            title = getString(R.string.extension_title_about_template),
            message = getString(R.string.extension_message_about_template))
    }

    private fun requirePermission(packageName: String) {
        if (FlavorUtils.liteVersion) {
            requireActivity().showPopup(getString(R.string.lite_version_not_active))
        } else if (FlavorUtils.rootVersion) {
            requireActivity().showPopup(getString(R.string.root_version_no_permission))
        } else {
            if (OSUtils.atLeastT()) {
                requestPermissionDialog(requireContext(),
                    message = getString(R.string.android_13_no_permission)) {
                    val uri = DocumentCompat.generateAppUri(packageName)
                    startActivityForData.launch(uri)
                }
            } else if (OSUtils.atLeastR()) {
                requestPermissionDialog(requireContext()) {
                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                }
            } else {
                requestPermissionDialog(requireContext()) {
                    PermissionUtils.verifyStoragePermissions(requireActivity())
                }
            }
        }
    }

}