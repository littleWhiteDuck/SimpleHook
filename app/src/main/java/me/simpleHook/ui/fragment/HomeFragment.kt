package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.activity.ConfigActivity
import me.simpleHook.ui.custom.PopupWindowList
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.util.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread
import kotlin.math.min


class HomeFragment : Fragment(), SearchView.OnQueryTextListener, HideScrollListener {

    private var fabDistance = 0
    private val viewModel: AppViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var filterConfigs: List<AppConfig> = ArrayList()
    private var config = "错误"
    private lateinit var mContext: Context
    private var currentPattern = ""
    private val mAdapter: HomeAdapter by lazy {
        HomeAdapter(onClick = { appConfig, mode ->
            onItemClick(mode, appConfig)
        }, onChange = { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) })
    }
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var isFabShow = true

    private fun onItemClick(mode: Int, appConfig: AppConfig) {
        when (mode) {
            Constant.HOME_ITEM_CLICK_NORMAL -> adapterOnClick(appConfig)
            Constant.HOME_ITEM_CLICK_LONG -> itemOnLongClick(appConfig)
            Constant.HOME_ITEM_CLICK_EDIT -> editConfig(appConfig)
            Constant.HOME_ITEM_CLICK_COPY -> copyConfigs(appConfig)
            Constant.HOME_ITEM_CLICK_DELETE -> deleteConfig(appConfig)
        }
    }

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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.deleteConfigs(appConfig)
            FileUtils.realDeleteConfig(
                requireContext(), appConfig.packageName, Constant.APP_CONFIG_NAME
            )
            Snackbar.make(
                binding.fab, getString(R.string.main_home_delete_config_tip), Snackbar.LENGTH_LONG
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
                viewModel.insertConfigs(appConfig)
                val configStr = Gson().toJson(appConfig)
                saveToText(appConfig.packageName, configStr)
            }.show()
        }
    }

    private fun itemOnLongClick(appConfig: AppConfig) {
        val arrayList =
            requireContext().resources.getStringArray(R.array.main_home_item_select_item)
        val popupWindowList = PopupWindowList.Builder(requireContext()).setItemList(arrayList)
            .setOutsideTouchable(true).build()
        popupWindowList.setOnItemClickListener { _, _, position, _ ->
            popupWindowList.dismiss()
            when (position) {
                0 -> copyConfigs(appConfig)
                1 -> deleteConfig(appConfig)
                2 -> editConfig(appConfig)
            }
        }.show()
    }

    private fun editConfig(appConfig: AppConfig) {
        val bundle = Bundle()
        bundle.putParcelable("appConfig", appConfig)
        toAddFragment(bundle)
    }

    private fun copyConfigs(config: AppConfig) {
        /* val originConfig = config.configs
         val configs = if (sp.encryptConfigs && !originConfig.startsWith("config://")) {
             CipherUtils.encrypt(config.configs)
         } else {
             config.configs
         }*/
        config.apply {
            config.configs = configs
            ToolUtils.toClip(requireContext(), Gson().toJson(config))
            getString(R.string.main_home_export_configs_tip).toast(requireContext())
        }
//        config.configs = originConfig
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
            addConfig.setOnClickListener { toAddFragment(null) }
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
        val textInputLayout = TextInputLayout(requireContext())
        val textInput = TextInputEditText(requireContext())
        textInput.background = null
        textInputLayout.addView(textInput)
        customDialog(
            requireContext(),
            title = "请输入网址",
            contentView = textInputLayout,
            okText = "确认",
            okClick = { dialogInterface ->
                importConfigsFromInternet(textInput.text.toString().trim())
                dialogInterface.dismiss()
            },
            cancelText = "取消"
        ).show()
    }

    private fun importConfigsFromInternet(urlString: String) {
        val regex =
            """((http|ftp|https)://[\w\-_]+(\.[\w\-_]+)+([\w\-.,@?^=%&:/~+#]*[\w\-@?^=%&/~+#])?)"""//设置正则表达式
        if (Pattern.matches(regex, urlString)) {
            thread {
                var connection: HttpURLConnection? = null
                try {
                    val response = StringBuilder()
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    val input = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    reader.use {
                        reader.forEachLine {
                            response.append(it)
                        }
                    }
                    config = response.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
            }
            if (config == "错误") {
                "稍后再试，网络错误或者其他错误".toast(requireContext())
            } else {
                importConfigs(config)
            }
        } else {
            "网址不正确".toast(requireContext())
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
                        FileUtils.saveConfig(
                            mContext, appConfig.packageName, Constant.APP_CONFIG_NAME, configs
                        )
                    }
                } catch (e: java.lang.Exception) {
                    getString(R.string.main_home_import_incorrect_format_tip).toast(mContext)
                }
            }
            else -> getString(R.string.main_home_import_incorrect_format_tip).toast(requireContext())
        }
    }

    private fun switchOnChange(appConfig: AppConfig, isChecked: Boolean) {
        appConfig.enable = isChecked
        viewModel.updateConfigs(appConfig)
        val configStr = Gson().toJson(appConfig)
        saveToText(appConfig.packageName, configStr)
    }

    private fun saveToText(packageName: String, configs: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            FileUtils.saveConfig(
                requireContext(), packageName, Constant.APP_CONFIG_NAME, configs
            )
        }
    }

    private fun adapterOnClick(appConfig: AppConfig) {
        val bottomSheetDialog = me.simpleHook.ui.custom.BottomSheetDialog(
            requireContext(),
            appConfig,
            onClick = { editConfig(appConfig) })
        bottomSheetDialog.setContentView()
        val windowPreferencesManager = WindowPreferencesManager(requireContext())
        windowPreferencesManager.applyEdgeToEdgePreference(bottomSheetDialog.window!!)
        bottomSheetDialog.show()
    }

    private fun toAddFragment(bundle: Bundle?) {
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