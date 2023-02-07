package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.SystemServices
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.ui.activity.ConfigActivity
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.util.*
import kotlin.math.min


class HomeFragment : BaseFragment(), SearchView.OnQueryTextListener, HideScrollListener {

    private var fabDistance = 0
    private val viewModel: AppViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var filterConfigs: List<AppConfig> = ArrayList()
    private lateinit var mContext: Context
    private var currentPattern = ""
    private lateinit var configOfItemMenu: AppConfig
    private val mAdapter: HomeAdapter by lazy {
        HomeAdapter(menuListener = { appConfig, menu ->
            onItemCreateContextMenu(appConfig, menu)
        }, onClick = { appConfig, mode ->
            onItemClick(mode, appConfig)
        }, onChange = { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) })
    }

    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var isFabShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mContext = requireContext()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        initView()
        initData()
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initData() {
        viewModel.getAllConfigs().observe(requireActivity()) {
            if (it.isEmpty()) {
                binding.emptyTip.visibility = View.VISIBLE
            } else {
                binding.emptyTip.visibility = View.GONE
            }
            filterConfigs = it
            if (currentPattern.isEmpty()) {
                mAdapter.submitList(it)
                if (binding.progressBar2.visibility != View.GONE) binding.progressBar2.visibility =
                    View.GONE
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
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.mainRecycler.apply {
            this.adapter = mAdapter
            layoutManager = linearLayoutManager
            addOnScrollListener(FabScrollListener(this@HomeFragment))
        }
        binding.mainRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                // Get the position of the view in the recycler view
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) {
                    return
                }

                if (position == parent.adapter!!.itemCount - 1) {
                    // Add padding to the last item. You should probably use a @dimen resource.
                    outRect.bottom = 200
                }
            }
        })
        FastScrollerUtil.bind(binding.mainRecycler)
    }

    private fun deleteConfig(appConfig: AppConfig) {
        if (configSystem.isEnableDelete(appConfig.packageName)) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteConfigs(appConfig)
                configSystem.deleteCustomConfig(appConfig.packageName)
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
            }
        } else {
            requirePermission(appConfig.packageName)
        }
    }


    private fun itemOnLongClick(appConfig: AppConfig) {
        //appInfo = AppUtils.appInfo(requireContext(), appConfig.packageName)
    }

    private fun editConfig(appConfig: AppConfig) {
        val bundle = Bundle()
        bundle.putParcelable("appConfig", appConfig)
        toAddConfig(bundle)
    }

    private fun onItemClick(mode: Int, appConfig: AppConfig) {
        when (mode) {
            Constant.HOME_ITEM_CLICK_NORMAL -> editConfig(appConfig)
            Constant.HOME_ITEM_CLICK_LONG -> itemOnLongClick(appConfig)
            Constant.HOME_ITEM_CLICK_EDIT -> editConfig(appConfig)
            Constant.HOME_ITEM_CLICK_COPY -> copyConfigs(appConfig)
            Constant.HOME_ITEM_CLICK_DELETE -> deleteConfig(appConfig)
        }
    }

    private fun onItemCreateContextMenu(appConfig: AppConfig, menu: ContextMenu) {
        configOfItemMenu = appConfig
        val isInstalled = AppUtils.isAppInstalled(appConfig.packageName)
        if (isInstalled) {
            requireActivity().menuInflater.inflate(R.menu.menu_app_item, menu)
            if (SystemServices.packageManager.getLaunchIntentForPackage(appConfig.packageName) == null) {
                menu.removeItem(R.id.menu_launch)
                menu.removeItem(R.id.menu_relaunch)
            }
            if (FlavorUtils.normalVersion || FlavorUtils.liteVersion) {
                menu.removeItem(R.id.menu_relaunch)
            }
        } else {
            requireActivity().menuInflater.inflate(R.menu.menu_app_item2, menu)
        }
        menu.setHeaderTitle(appConfig.appName)
    }

    private fun copyConfigs(config: AppConfig) {
        config.apply {
            config.configs = configs
            ToolUtils.toClip(requireContext(), Gson().toJson(config))
            getString(R.string.main_home_export_configs_tip).toast(requireContext())
        }
    }

    private fun initView() {
        var maybeABug = 0
        val layoutParams = binding.fab.layoutParams as ViewGroup.MarginLayoutParams
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
            binding.fab.layoutParams = layoutParams
            fabDistance = bottomMargin + binding.fab.height * 2
            windowInsets
        }
        binding.apply {
            addConfig.setOnClickListener { toAddConfig(null) }
            importConfigsFromPaste.setOnClickListener {
                ToolUtils.getClipboardContent(requireContext())?.let { importConfigs(it) }
            }
            shareConfigs.setOnClickListener { shareConfigs() }
            importConfigsFromInternet.setOnClickListener {
                showInternetImportConfigDialog()
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

    private fun importConfigsFromInternet(urlString: String) {
        val loadingDialog = LoadingDialog(requireActivity(), getString(R.string.data_loading))
        loadingDialog.show()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            fetchText(urlString)?.let {
                importConfigs(it)
            } ?: getString(R.string.error_get_config_from_internet).toast(requireContext())
            loadingDialog.dismiss()
        }
    }

    private fun shareConfigs() {
        if (filterConfigs.isNotEmpty()) {
            val dataList = ArrayList<ConfigItem>()
            for (config in filterConfigs) {
                dataList.add(ConfigItem(config))
            }
            ConfigDialogFragment(dataList, Constant.CONFIG_EXPORT_MODE).show(
                requireActivity().supportFragmentManager, "export"
            )
        }
    }

    private fun importConfigs(configs: String) {
        when {
            JsonUtil.isJsonArray(configs) -> {
                val dataList = JsonUtil.importConfigs(configs)
                if (dataList.isEmpty()) {
                    getString(R.string.main_home_import_incorrect_format_tip).toast(
                        requireContext()
                    )
                    return
                } else {
                    ConfigDialogFragment(
                        dataList as ArrayList<ConfigItem>, Constant.CONFIG_IMPORT_MODE
                    ).show(
                        requireActivity().supportFragmentManager, "import"
                    )
                }
            }
            JsonUtil.isJsonObject(configs) -> {
                try {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val appConfig = Gson().fromJson(configs, AppConfig::class.java)
                        appConfig.id = 0
                        viewModel.insertConfigs(appConfig)
                        configSystem.saveCustomConfig(appConfig.packageName, configs)
                    }
                } catch (e: java.lang.Exception) {
                    getString(R.string.main_home_import_incorrect_format_tip).toast(mContext)
                }
            }
            else -> getString(R.string.main_home_import_incorrect_format_tip).toast(requireContext())
        }
    }

    private fun switchOnChange(appConfig: AppConfig, isChecked: Boolean) {
        if (configSystem.isEnableSave(appConfig.packageName)) {
            appConfig.enable = isChecked
            viewModel.updateConfigs(appConfig)
            val configStr = Gson().toJson(appConfig)
            configSystem.saveCustomConfig(appConfig.packageName, configStr)
        } else {
            requirePermission(appConfig.packageName)
        }
    }

    private fun saveConfig(appConfig: AppConfig) {
        if (configSystem.isEnableSave(appConfig.packageName)) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.insertConfigs(appConfig)
                val configStr = Gson().toJson(appConfig)
                configSystem.saveCustomConfig(appConfig.packageName, configStr)
            }
        } else {
            requirePermission(appConfig.packageName)
        }
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_launch -> AppUtils.startApp(configOfItemMenu.packageName, requireContext())
            R.id.menu_force_stop -> {
                if (FlavorUtils.rootVersion) {
                    SuUtil.forceStopApp(configOfItemMenu.packageName)
                } else {
                    AppUtils.jumpAppInfoPage(requireContext(), configOfItemMenu.packageName)
                }
            }
            R.id.menu_relaunch -> {
                if (FlavorUtils.rootVersion) {
                    val intent =
                        requireActivity().packageManager.getLaunchIntentForPackage(configOfItemMenu.packageName)
                    intent?.component?.className?.let { className ->
                        SuUtil.reLaunchApp(configOfItemMenu.packageName, className)
                    }
                }
            }
            R.id.menu_app_info -> AppUtils.jumpAppInfoPage(
                requireContext(), configOfItemMenu.packageName
            )
            R.id.menu_copy_config -> copyConfigs(configOfItemMenu)
            R.id.menu_delete_config -> deleteConfig(configOfItemMenu)
            R.id.menu_edit_config -> editConfig(configOfItemMenu)
        }
        return super.onContextItemSelected(item)
    }

    private fun toAddConfig(bundle: Bundle?) {
        val intent = Intent(requireActivity(), ConfigActivity::class.java)
        intent.putExtra("bundle", bundle)
        startActivity(intent)
    }

    override fun onQueryTextSubmit(query: String?) = false
    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = newText.trim()
        currentPattern = pattern
        toFilterData(pattern)
        return true
    }

    private fun toFilterData(pattern: String) {
        val filter = filterConfigs.filter {
            it.appName.contains(pattern) || it.packageName.contains(pattern)
        }
        mAdapter.submitList(filter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(this@HomeFragment)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        private var visible = true //是否可见
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (distance > THRESHOLD && visible) {
                //隐藏动画
                visible = false
                listener.onHide()
                distance = 0
            } else if (distance < -20 && !visible) {
                //显示动画
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