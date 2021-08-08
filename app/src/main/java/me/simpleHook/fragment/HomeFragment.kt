package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.constant.Constant
import me.simpleHook.custom.BottomDialog
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.util.FileUtils
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.ToolUtils
import me.simpleHook.util.toast
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread


@Suppress("COMPATIBILITY_WARNING")
class HomeFragment : BaseFragment(),SearchView.OnQueryTextListener, CoroutineScope by MainScope() {
    private lateinit var viewModel: AppViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var filterConfigsLive: LiveData<List<AppConfigEntity>>
    private var config = "错误"
    private var mContext:Context? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentHomeBinding.inflate(inflater, container, false).let {
        binding = it
        it.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mContext = requireContext()
        initView()
        val adapter = HomeAdapter.getHomeAdapter(   { appConfigEntity -> adapterOnClick(appConfigEntity) },
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            { appConfigEntity, builder -> itemOnLongClick(appConfigEntity, builder) })
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
        if (this::filterConfigsLive.isInitialized && filterConfigsLive.hasObservers()){
            filterConfigsLive.removeObservers(requireActivity())
        }
        launch {
            filterConfigsLive = viewModel.getAllConfigs()
            filterConfigsLive.observe(requireActivity()) {
                adapter.submitList(it).also {
                    binding.progressBar2.visibility = View.GONE
                }
            }
            val linearLayoutManager = LinearLayoutManager(requireContext())
            binding.mainRecycler.apply {
                this.adapter = adapter
                layoutManager = linearLayoutManager
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

    fun deleteConfig(appConfigEntity: AppConfigEntity) {
        viewModel.deleteConfigs(appConfigEntity)
        Snackbar.make(
            requireActivity().findViewById(R.id.fragment),
            getString(R.string.delete_config_tip), Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.revocation)) {
            viewModel.insertConfigs(appConfigEntity)
        }.show()
    }

    private fun itemOnLongClick(appConfigEntity: AppConfigEntity, builder: XPopup.Builder) {
        val arrayOfString = requireContext().resources.getStringArray(R.array.home_item_select_item)
        builder.asAttachList(arrayOfString, null) { _, text ->
            when (text) {
                getString(R.string.edit) -> editConfig(appConfigEntity)
                getString(R.string.delete) -> deleteConfig(appConfigEntity)
                getString(R.string.share) -> copyConfigs(appConfigEntity.config)
            }
        }.show()
    }

    private fun editConfig(appConfigEntity: AppConfigEntity) {
        val viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        )[MethodViewModel::class.java]
        viewModel.configLive.value = appConfigEntity
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
        XPopup.Builder(requireContext())
            .hasStatusBarShadow(false)
            .isDestroyOnDismiss(true)
            .autoOpenSoftInput(true)
            .isDarkTheme(false)
            .asInputConfirm("请输入网址",null,null){
                importConfigsFromInternet(it.trim())
            }
            .show()
    }

    private fun importConfigsFromInternet(urlString: String) {
        val regex = """((http|ftp|https)://[\w\-_]+(\.[\w\-_]+)+([\w\-.,@?^=%&:/~+#]*[\w\-@?^=%&/~+#])?)"""//设置正则表达式
        if (Pattern.matches(regex,urlString)){
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
            if (config == "错误"){
                "稍后再试，网络错误或者其他错误".toast(requireContext())
            }else{
                importConfigs(config)
            }
        }else{
            "网址不正确".toast(requireContext())
        }

    }
    private fun shareConfigs() {
        val strConfig = getStrConfig(filterConfigsLive.value)
        ToolUtils.toClip(requireContext(),strConfig)
        getString(R.string.export_configs_tip).toast(requireContext())
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
                                AppConfigEntity(
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
                        AppConfigEntity(
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

    private fun switchOnChange(appConfigEntity: AppConfigEntity, isChecked: Boolean) {
        val oldCan = appConfigEntity.canUse
        val newConfig = appConfigEntity.config.replace("canUse\":$oldCan","canUse\":$isChecked")
        appConfigEntity.apply {
            config = newConfig
            canUse = isChecked
        }
        viewModel.updateConfigs(appConfigEntity)
        FileUtils.verifyStoragePermissions(requireActivity())
        refreshConfig("${Constant.CONFIG_DIRECTORY+appConfigEntity.packageName}/","config",appConfigEntity.config)
    }

    private fun adapterOnClick(appConfig: AppConfigEntity) {
        XPopup.Builder(requireContext())
            .isDestroyOnDismiss(true)
            .asCustom(
                BottomDialog(
                    requireContext(),
                    appConfig,
                    onClick = { editConfig(appConfig) })
            )
            .show()
    }

    private fun toAddFragment() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_homeFragment_to_addFragment)
    }

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String) = true.also {
        val adapter = HomeAdapter.getHomeAdapter(   { appConfigEntity -> adapterOnClick(appConfigEntity) },
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            { appConfigEntity, builder -> itemOnLongClick(appConfigEntity, builder) })
        val pattern = "%${newText.trim()}%"
        filterConfigsLive.removeObservers(requireActivity())
        filterConfigsLive = viewModel.getFilterConfigs(pattern)
        filterConfigsLive.observe(requireActivity()){
            adapter.submitList(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home,menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = "输入应用名或包名"
        searchView.setOnQueryTextListener(this)
    }

    /**
     * 刷新配置
     */
    private fun refreshConfig(url:String, name:String,fileContent:String){

        FileUtils.writeData(url,name,fileContent)
    }

    /**
     * 获取所有配置文本形式
     */
    private fun getStrConfig(list: List<AppConfigEntity>?,formatConfig:Boolean = true) = list?.let {
        val configs = StringBuilder()
        for (i in it.indices) {
            configs.append("${it[i].config},")
        }
        val strConfigs = "[${configs.substring(0, configs.length - 1)}]"
        val strConfig = if (formatConfig) JsonUtil.formatJson(strConfigs) else strConfigs
        strConfig
    }?:""


}