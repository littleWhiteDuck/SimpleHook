package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.simpleHook.MainActivity
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.constant.Constant
import me.simpleHook.custom.PopupWindowList
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.util.*
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class HomeFragment : BaseFragment(), SearchView.OnQueryTextListener, CoroutineScope by MainScope(),
    HideScrollListener {
    private lateinit var viewModel: AppViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var filterConfigsLive: LiveData<List<AppConfig>>
    private var config = "错误"
    private lateinit var mContext: Context
    private val mAdapter: HomeAdapter by lazy {
        HomeAdapter({ appConfigEntity -> adapterOnClick(appConfigEntity) },
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            { appConfigEntity -> itemOnLongClick(appConfigEntity) })
    }
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireContext()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        initView()
        initViewModel()
        return binding.root
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
        if (this::filterConfigsLive.isInitialized && filterConfigsLive.hasObservers()) {
            filterConfigsLive.removeObservers(requireActivity())
        }
        launch {
            filterConfigsLive = viewModel.getAllConfigs()
            filterConfigsLive.observe(requireActivity()) {
                mAdapter.submitList(it).also {
                    binding.progressBar2.visibility = View.GONE
                }
            }
            val linearLayoutManager = LinearLayoutManager(requireContext())
            binding.mainRecycler.apply {
                this.adapter = mAdapter
                layoutManager = linearLayoutManager
                addOnScrollListener(FabScrollListener(this@HomeFragment))
            }
        }

        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            @SuppressLint("ShowToast")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val configDelete = filterConfigsLive.value!![viewHolder.adapterPosition]
                deleteConfig(configDelete)
            }

        }).attachToRecyclerView(binding.mainRecycler)
    }

    fun deleteConfig(appConfig: AppConfig) {
        viewModel.deleteConfigs(appConfig)
        Snackbar.make(
            requireActivity().findViewById(R.id.fragment),
            getString(R.string.delete_config_tip), Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.revocation)) {
            viewModel.insertConfigs(appConfig)
        }.show()
    }

    private fun itemOnLongClick(appConfig: AppConfig) {
        val arrayList = arrayListOf("编辑", "删除", "复制")
        val popupWindowList = PopupWindowList.Builder(requireContext())
            .setItemList(arrayList)
            .setOutsideTouchable(true)
            .build()
        popupWindowList.setOnItemClickListener { _, _, position, _ ->
            popupWindowList.dismiss()
            when (position) {
                0 -> editConfig(appConfig)
                1 -> deleteConfig(appConfig)
                2 -> copyConfigs(appConfig.config)
            }
        }.show()
    }

    private fun editConfig(appConfig: AppConfig) {
        val viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        )[MethodViewModel::class.java]
        viewModel.configLive.value = appConfig
        toAddFragment()
    }

    private fun copyConfigs(config: String) {
        ToolUtils.toClip(requireContext(), JsonUtil.formatJson(config))
        getString(R.string.export_configs_tip).toast(requireContext())
    }

    private fun initView() {
        binding.addConfig.setOnClickListener { toAddFragment() }
        binding.importConfigsFromPaste.setOnClickListener {
            ToolUtils.getClipboardContent(requireContext())?.let { importConfigs(it) }
        }
        binding.shareConfigs.setOnClickListener { shareConfigs() }
        binding.importConfigsFromInternet.setOnClickListener {
            showInternetImportConfigDialog()
        }
    }

    private fun showInternetImportConfigDialog() {

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
        if (filterConfigsLive.value?.isNotEmpty() == true) {
            val strConfig = getStrConfig(filterConfigsLive.value)
            ToolUtils.toClip(requireContext(), strConfig)
            getString(R.string.export_configs_tip).toast(requireContext())
        }
    }

    private fun importConfigs(configs: String) {
        when {
            JsonUtil.isJsonArray(configs) -> {
                var tip = getString(R.string.import_success_tip)
                try {
                    val configsJsonArray = JSONArray(configs)
                    /* val arrayConfig = arrayOf<AppConfigEntity>()*/
                    for (i in 0 until configsJsonArray.length()) {
                        configsJsonArray.getJSONObject(i).apply {
                            viewModel.insertConfigs(
                                AppConfig(
                                    getString("packageName"),
                                    getString("appName"),
                                    getString("versionName"),
                                    getString("description"),
                                    toString()
                                )
                            )
                        }
                    }
                    /*viewModel.insertConfigs(*arrayConfig)*/
                } catch (e: Exception) {
                    e.printStackTrace()
                    tip = getString(R.string.import_fail_tip)
                }
                tip.toast(requireContext())
            }
            JsonUtil.isJsonObject(configs) -> {
                JSONObject(configs).apply {
                    viewModel.insertConfigs(
                        AppConfig(
                            getString("packageName"),
                            getString("appName"),
                            getString("versionName"),
                            getString("description"),
                            toString()
                        )
                    )
                }

            }
            else -> {
                getString(R.string.incorrect_format_tip).toast(requireContext())
            }
        }
    }

    private fun switchOnChange(appConfig: AppConfig, isChecked: Boolean) {
        val oldCan = appConfig.canUse
        val newConfig = appConfig.config.replace("canUse\":$oldCan", "canUse\":$isChecked")
        appConfig.apply {
            config = newConfig
            canUse = isChecked
        }
        viewModel.updateConfigs(appConfig)
        FileUtils.verifyStoragePermissions(mContext as Activity)
        refreshConfig(
            "${Constant.CONFIG_DIRECTORY + appConfig.packageName}/",
            "config",
            appConfig.config
        )
        val pref = try {
            requireContext().getSharedPreferences(
                "hookConfig",
                Context.MODE_WORLD_READABLE
            )
        } catch (e: SecurityException) {
            null
        }
        appConfig.apply {
            pref?.edit()?.putString(packageName, config)?.apply()
            /*?: toTipError()*/
        }

    }

  /*  private fun toTipError() {
        if (!MainActivity.isModuleLive()) "模块未激活，将无法使用New XSharePreferences获取配置".toast(
            requireContext()
        )
    }*/

    private fun adapterOnClick(appConfig: AppConfig) {
        val bottomSheetDialog = me.simpleHook.custom.BottomSheetDialog(
            requireContext(),
            appConfig,
            onClick = { editConfig(appConfig) })
        bottomSheetDialog.apply {
            setContentView()
            show()
        }
    }

    private fun toAddFragment() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_homeFragment_to_addFragment)
    }

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = "%${newText.trim()}%"
        filterConfigsLive.removeObservers(requireActivity())
        filterConfigsLive = viewModel.getFilterConfigs(pattern)
        filterConfigsLive.observe(viewLifecycleOwner) {
            mAdapter.submitList(it)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.apply {
            queryHint = "搜索…"
            setOnQueryTextListener(this@HomeFragment)
            maxWidth = (PhoneUtils.getWindowWidth(mContext) * 0.8).toInt()
        }
    }

    /**
     * 刷新配置
     */
    private fun refreshConfig(url: String, name: String, fileContent: String) {

        FileUtils.writeData(url, name, fileContent)
    }

    /**
     * 获取所有配置文本形式
     */
    private fun getStrConfig(list: List<AppConfig>?, formatConfig: Boolean = true) =
        list?.let {
            val configs = StringBuilder()
            for (i in it.indices) {
                configs.append("${it[i].config},")
            }
            val strConfigs = "[${configs.substring(0, configs.length - 1)}]"
            val strConfig = if (formatConfig) JsonUtil.formatJson(strConfigs) else strConfigs
            strConfig
        } ?: ""


    override fun onDestroy() {
        super.onDestroy()
        cancel()
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

    override fun onShow() {
        bottomNavigationView
        binding.fab.animate().translationY(0f).interpolator = DecelerateInterpolator(3f)
       /* bottomNavigationView.animate()
            .translationY(0f).interpolator =
            DecelerateInterpolator(1f)
        bottomNavigationView.visibility = View.VISIBLE*/
    }

    override fun onHide() {
        binding.fab.animate().translationY(binding.fab.height.px).interpolator =
            DecelerateInterpolator(1.5f)
        /*bottomNavigationView.animate()
            .translationY(bottomNavigationView.height.px).interpolator =
            DecelerateInterpolator(1f)
        bottomNavigationView.visibility = View.GONE*/
    }


}

interface HideScrollListener {
    fun onShow()
    fun onHide()
}