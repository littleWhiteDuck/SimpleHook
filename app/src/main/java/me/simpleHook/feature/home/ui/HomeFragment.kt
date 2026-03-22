package me.simpleHook.feature.home.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Patterns
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.core.base.BaseExtensionVBFragment
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.AppConfigItem
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.fetchText
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.extension.showPopupWithCopyMsg
import me.simpleHook.feature.config.ui.ConfigActivity
import me.simpleHook.feature.config.ui.ConfigDialogFragment
import me.simpleHook.core.ui.custom.LoadingDialog
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.view.edit.InputView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.FastScrollerUtil
import me.simpleHook.core.utils.JsonUtil
import me.simpleHook.core.utils.SuUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.feature.home.ui.adapter.HomeAdapter
import me.simpleHook.feature.pluginexport.domain.PluginApkShareHelper
import me.simpleHook.feature.pluginexport.ui.ExportPluginBottomSheetFragment
import me.simpleHook.platform.shizuku.ShizukuFileManager
import java.io.File
import kotlin.math.min


class HomeFragment : BaseExtensionVBFragment<FragmentHomeBinding>(), HideScrollListener {

    private var fabDistance = 0
    private val viewModel: AppConfigViewModel by activityViewModels()
    private var filterConfigs: List<AppConfigItem> = ArrayList()
    private var tempConfigs = ArrayList<AppConfigItem>()
    private var currentPattern = ""
    private lateinit var configOfItemMenu: AppConfig
    private val mAdapter: HomeAdapter by lazy {
        HomeAdapter(
            menuListener = { appConfig, menu ->
                onItemCreateContextMenu(appConfig, menu)
            },
            onClick = { appConfig, mode ->
                onItemClick(mode, appConfig)
            },
            onChange = { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            onDrag = { holder -> startDrag(holder) })
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var isFabShow = true
    private var isDrag = false

    private fun initData() {
        registerPluginExportResultListener()
        viewModel.getAllConfigs().observe(requireActivity()) {
            binding.emptyTip.isVisible = it.isEmpty()
            filterConfigs = it.map { appConfig -> AppConfigItem(appConfig) }
            if (currentPattern.isEmpty()) {
                mAdapter.submitList(filterConfigs)
                binding.progressBar.hide()
            } else {
                toFilterData(currentPattern)
            }
        }
        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.mainRecycler.scrollToPosition(0)
            }
        })
        with(binding.mainRecycler) {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(FabScrollListener(this@HomeFragment))
            FastScrollerUtil.bind(this)
        }

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val finalPosition = target.bindingAdapterPosition
                val tempConfig = tempConfigs[fromPosition]
                tempConfigs[fromPosition] = tempConfigs[finalPosition]
                tempConfigs[finalPosition] = tempConfig
                val tempConfigId = tempConfigs[fromPosition].appConfig.id
                tempConfigs[fromPosition].appConfig.id = tempConfigs[finalPosition].appConfig.id
                tempConfigs[finalPosition].appConfig.id = tempConfigId
                mAdapter.notifyItemMoved(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.mainRecycler)
    }

    private fun deleteConfig(appConfig: AppConfig) {
        if (configSystem.isEnableDelete(appConfig.packageName)) {
            viewModel.deleteConfigs(appConfig)
            Snackbar.make(
                binding.fab,
                getString(R.string.main_home_delete_config_tip),
                Snackbar.LENGTH_LONG
            ).apply {
                anchorView = bottomNavigationView
            }.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    super.onShown(sb)
                    if (isFabShow) binding.fab.animate().translationY((-50f).dp).interpolator =
                        DecelerateInterpolator(1.5f)
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (isFabShow) binding.fab.animate().translationY(0f).interpolator =
                        DecelerateInterpolator(1.5f)
                }
            }).setAction(getString(R.string.main_home_undo_delete_config)) {
                saveConfig(appConfig)
            }.show()
        } else {
            requirePermission(appConfig.packageName)
        }
    }

    private fun startDrag(holder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(holder)
    }


    private fun editConfig(appConfig: AppConfig) {
        if (isDrag) return
        toAddConfig(bundle = Bundle().apply {
            putParcelable("appConfig", appConfig)
        })
    }

    private fun onItemClick(mode: Int, appConfig: AppConfig) {
        when (mode) {
            Constant.HOME_ITEM_CLICK_NORMAL -> editConfig(appConfig)
            Constant.HOME_ITEM_CLICK_EDIT -> editConfig(appConfig)
            Constant.HOME_ITEM_CLICK_COPY -> copyConfigs(appConfig)
            Constant.HOME_ITEM_CLICK_DELETE -> deleteConfig(appConfig)
        }
    }

    private fun onItemCreateContextMenu(appConfig: AppConfig, menu: ContextMenu) {
        if (isDrag) return
        configOfItemMenu = appConfig
        val isInstalled = AppUtil.isAppInstalled(appConfig.packageName)
        if (isInstalled) {
            requireActivity().menuInflater.inflate(R.menu.menu_app_item, menu)
            if (GlobalValue.packageManager.getLaunchIntentForPackage(appConfig.packageName) == null) {
                menu.removeItem(R.id.menu_launch)
                menu.removeItem(R.id.menu_relaunch)
            }
            if (GlobalValue.isNormalWork) {
                menu.removeItem(R.id.menu_relaunch)
            }
        } else {
            requireActivity().menuInflater.inflate(R.menu.menu_app_item2, menu)
        }
        menu.setHeaderTitle(appConfig.appName)
    }

    private fun copyConfigs(config: AppConfig) {
        val msg = Json.encodeToString(config)
        requireActivity().showPopupWithCopyMsg(
            getString(R.string.main_home_export_configs_tip),
            msg
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        var maybeABug = 0
        val layoutParams = binding.fab.layoutParams as ViewGroup.MarginLayoutParams
        val layoutParams2 = binding.sortDone.layoutParams as ViewGroup.MarginLayoutParams
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
            val bottomMargin = if (isGesture) maybeABug + navigationInsets.bottom
            else maybeABug + navigationInsets.bottom / 5
            layoutParams.bottomMargin = bottomMargin
            layoutParams2.bottomMargin = bottomMargin
            binding.fab.layoutParams = layoutParams
            binding.sortDone.layoutParams = layoutParams2
            fabDistance = bottomMargin + binding.fab.height * 2
            windowInsets
        }
        with(binding) {
            addConfig.setOnClickListener { toAddConfig(null) }
            importConfigsFromPaste.setOnClickListener {
                ToolUtil.getClipboardContent(requireContext())?.let { importConfigs(it) }
            }
            shareConfigs.setOnClickListener { shareConfigs() }
            importConfigsFromInternet.setOnClickListener {
                showInternetImportConfigDialog()
            }
            sortDone.setOnClickListener {
                progressBar.isVisible = true
                sortDone.isVisible = false
                fab.isVisible = true
                viewModel.updateConfigs(
                    *tempConfigs.map { it.appConfig }.toTypedArray(),
                    needWriteToFile = false
                )
                isDrag = false
            }
        }
    }

    private fun showInternetImportConfigDialog() {
        val inputView = InputView(requireContext())
        inputView.editText.doAfterTextChanged {
            it?.let {
                if (it.toString().isEmpty() || Patterns.WEB_URL.matcher(it.toString()).matches()) {
                    inputView.textInputLayout.isErrorEnabled = false
                } else {
                    inputView.textInputLayout.isErrorEnabled = true
                    inputView.textInputLayout.error = getString(R.string.url_is_incorrect)
                }
            }
        }
        customDialog(
            requireContext(),
            title = getString(R.string.please_input_url),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                importConfigsFromInternet(inputView.editText.text.toString().trim())
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun registerPluginExportResultListener() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            ExportPluginBottomSheetFragment.EXPORT_PLUGIN_RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val apkPath =
                bundle.getString(ExportPluginBottomSheetFragment.EXPORT_PLUGIN_APK_PATH_KEY)
                    .orEmpty()
            if (apkPath.isBlank()) {
                requireActivity().showPopup(getString(R.string.plugin_export_open_failed))
                return@setFragmentResultListener
            }
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                requireActivity().showPopup(getString(R.string.plugin_export_open_failed))
                return@setFragmentResultListener
            }
            showPluginExportSuccessDialog(apkFile)
        }
    }

    private fun showPluginExportSuccessDialog(apkFile: File) {
        customDialog(
            requireActivity(),
            title = getString(R.string.plugin_export_success_title),
            message = getString(R.string.plugin_export_success_message, apkFile.absolutePath),
            okText = getString(R.string.plugin_export_install),
            okClick = {
                installPluginApk(apkFile)
            },
            neutralText = getString(R.string.plugin_export_share),
            neutralClick = {
                sharePluginApk(apkFile)
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun installPluginApk(apkFile: File) {
        runCatching {
            val intent = if (PluginApkShareHelper.canRequestPackageInstalls(requireActivity())) {
                PluginApkShareHelper.createInstallIntent(requireActivity(), apkFile)
            } else {
                requireActivity().showPopup(getString(R.string.plugin_export_install_permission_tip))
                PluginApkShareHelper.createInstallPermissionIntent(requireActivity())
            }
            requireActivity().startActivity(intent)
        }.onFailure {
            requireActivity().showPopup(getString(R.string.plugin_export_open_failed))
        }
    }

    private fun sharePluginApk(apkFile: File) {
        runCatching {
            val intent = Intent.createChooser(
                PluginApkShareHelper.createShareIntent(requireActivity(), apkFile),
                getString(R.string.plugin_export_share_chooser_title)
            )
            requireActivity().startActivity(intent)
        }.onFailure {
            requireActivity().showPopup(getString(R.string.plugin_export_open_failed))
        }
    }

    private fun importConfigsFromInternet(urlString: String) {
        val loadingDialog = LoadingDialog(requireActivity(), getString(R.string.data_loading))
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            fetchText(urlString)?.let {
                importConfigs(it)
            } ?: requireActivity().showPopup(getString(R.string.error_get_config_from_internet))
            loadingDialog.dismiss()
        }
    }

    private fun shareConfigs() {
        if (filterConfigs.isEmpty()) return
        customDialog(
            requireContext(),
            title = getString(R.string.plugin_export_type_dialog_title),
            message = getString(R.string.plugin_export_type_dialog_message),
            okText = getString(R.string.plugin_export_type_plugin_apk),
            okClick = {
                showPluginExportSheet()
            },
            neutralText = getString(R.string.plugin_export_type_plain_config),
            neutralClick = {
                showConfigExportDialog(Constant.CONFIG_EXPORT_MODE)
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun showPluginExportSheet() {
        val dataList = ArrayList<AppConfigItem2>()
        for (config in filterConfigs) {
            dataList.add(AppConfigItem2(config.appConfig))
        }
        ExportPluginBottomSheetFragment(dataList).show(
            requireActivity().supportFragmentManager,
            "export_plugin_sheet"
        )
    }

    private fun showConfigExportDialog(mode: Int) {
        val dataList = ArrayList<AppConfigItem2>()
        for (config in filterConfigs) {
            dataList.add(AppConfigItem2(config.appConfig))
        }
        ConfigDialogFragment(
            dataList,
            mode
        ).show(
            requireActivity().supportFragmentManager,
            "export"
        )
    }

    private fun importConfigs(configs: String) {
        when {
            JsonUtil.isJsonArray(configs) -> {
                val dataList = JsonUtil.importConfigs(configs)
                if (dataList.isEmpty()) {
                    requireActivity().showPopup(getString(R.string.main_home_import_incorrect_format_tip))
                    return
                } else {
                    ConfigDialogFragment(
                        dataList as ArrayList<AppConfigItem2>,
                        Constant.CONFIG_IMPORT_MODE
                    ).show(
                        requireActivity().supportFragmentManager,
                        "import"
                    )
                }
            }

            JsonUtil.isJsonObject(configs) -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val appConfig = Json.decodeFromString<AppConfig>(configs)
                        appConfig.id = 0
                        viewModel.insertConfigs(appConfig)
                        configSystem.saveCustomConfig(appConfig.packageName, configs)
                    }.onFailure {
                        Looper.prepare()
                        requireActivity().showPopup(getString(R.string.main_home_import_incorrect_format_tip))
                        Looper.loop()
                    }
                }
            }

            else -> requireActivity().showPopup(getString(R.string.main_home_import_incorrect_format_tip))
        }
    }

    private fun switchOnChange(appConfig: AppConfig, isChecked: Boolean) {
        if (configSystem.isEnableSave(appConfig.packageName)) {
            appConfig.enable = isChecked
            viewModel.updateConfigs(appConfig)
        } else {
            requirePermission(appConfig.packageName)
        }
    }

    private fun saveConfig(appConfig: AppConfig) {
        if (configSystem.isEnableSave(appConfig.packageName)) {
            viewModel.insertConfigs(appConfig)
        } else {
            requirePermission(appConfig.packageName)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_launch -> AppUtil.startApp(configOfItemMenu.packageName, requireContext())
            R.id.menu_force_stop -> {
                if (GlobalValue.isRootWork) {
                    SuUtil.forceStopApp(configOfItemMenu.packageName)
                } else if (GlobalValue.isShizukuWork) {
                    ShizukuFileManager.service?.forceStopPackage(configOfItemMenu.packageName)
                } else {
                    AppUtil.jumpAppInfoPage(requireContext(), configOfItemMenu.packageName)
                }
            }

            R.id.menu_relaunch -> {
                if (!GlobalValue.isNormalWork) {
                    val intent =
                        requireActivity().packageManager.getLaunchIntentForPackage(configOfItemMenu.packageName)
                    intent?.component?.className?.let { className ->
                        if (GlobalValue.isRootWork) {
                            SuUtil.reLaunchApp(configOfItemMenu.packageName, className)
                        } else if (GlobalValue.isShizukuWork) {
                            ShizukuFileManager.service?.reLaunchApp(
                                configOfItemMenu.packageName,
                                className
                            )
                        }
                    }
                }
            }

            R.id.menu_app_info -> AppUtil.jumpAppInfoPage(
                requireContext(),
                configOfItemMenu.packageName
            )

            R.id.menu_copy_config -> copyConfigs(configOfItemMenu)
            R.id.menu_delete_config -> deleteConfig(configOfItemMenu)
            R.id.menu_edit_config -> editConfig(configOfItemMenu)
            R.id.menu_drag_sort -> {
                if (currentPattern.isEmpty()) {
                    startDragSort()
                } else {
                    requireActivity().showPopup(getString(R.string.main_sort_tip_exit_search))
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun startDragSort() {
        binding.fab.isVisible = false
        binding.sortDone.isVisible = true
        isDrag = true
        tempConfigs.clear()
        filterConfigs.forEach {
            tempConfigs.add(it.copy(drag = true))
        }
        mAdapter.submitList(tempConfigs)
        //  mAdapter.notifyDataSetChanged()
    }

    private fun toAddConfig(bundle: Bundle?) {
        val intent = Intent(requireActivity(), ConfigActivity::class.java)
        intent.putExtra("bundle", bundle)
        startActivity(intent)
    }


    private fun toFilterData(pattern: String) {
        val filter = filterConfigs.filter {
            it.appConfig.appName.contains(pattern) || it.appConfig.packageName.contains(pattern)
        }
        mAdapter.submitList(filter)
    }

    override fun init() {
        initView()
        initData()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        with(searchView) {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isDrag) return true
                    val pattern = newText.trim()
                    currentPattern = pattern
                    toFilterData(pattern)
                    return true
                }

            })
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = true

    override fun canBack() = isDrag

    override fun notBackTip() {
        backPressed()
    }

    override fun performBack() {
        cancelDragSort()
    }

    override fun enableCallback() = true

    @SuppressLint("NotifyDataSetChanged")
    private fun cancelDragSort() {
        isDrag = false
        binding.sortDone.isVisible = false
        binding.fab.isVisible = true
        mAdapter.submitList(filterConfigs.toList())
        mAdapter.notifyDataSetChanged()
    }

    override fun onShow() {
        isFabShow = true
        binding.fab.animate().translationY(0f).interpolator = DecelerateInterpolator(3f)
    }

    override fun onHide() {
        isFabShow = false
        binding.fab.animate().translationY(fabDistance.toFloat()).interpolator =
            DecelerateInterpolator(1.5f)
    }

    class FabScrollListener(private val listener: HideScrollListener) :
        RecyclerView.OnScrollListener() {
        private var distance = 0
        private var visible = true //鏄惁鍙
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (distance > THRESHOLD && visible) {
                //闅愯棌鍔ㄧ敾
                visible = false
                listener.onHide()
                distance = 0
            } else if (distance < -20 && !visible) {
                //鏄剧ず鍔ㄧ敾
                visible = true
                listener.onShow()
                distance = 0
            }
            if (visible && dy > 0 || !visible && dy < 0) {
                distance += dy
            }
        }

        companion object {
            private const val THRESHOLD = 20
        }
    }

}


interface HideScrollListener {
    fun onShow()
    fun onHide()
}
