package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.icu.text.SimpleDateFormat
import android.os.*
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import me.simpleHook.R
import me.simpleHook.adapter.AppSelectAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAppSelectBinding
import me.simpleHook.util.AppUtils
import me.simpleHook.viewmodel.MethodViewModel
import java.util.*
import kotlin.concurrent.thread


class AppSelectFragment : BaseFragment() {
    private lateinit var userAppList: List<AppItem>
    private lateinit var systemAppList: List<AppItem>
    private var _binding: FragmentAppSelectBinding? = null
    private val binding get() = _binding!!
    private var currentQueryText = ""
    private val updateList = 1
    private val userAdapter by lazy { AppSelectAdapter.getAppSelectAdapter1() }
    private val systemAdapter by lazy { AppSelectAdapter.getAppSelectAdapter2() }
    private val methodViewModel by lazy { ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory())[MethodViewModel::class.java]}
    private var isFromAssist = false
    private val viewModel by lazy { ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory(requireActivity().application))[AppViewModel::class.java] }
    private val handler = @SuppressLint("HandlerLeak")
    object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateList -> {
                    userAppList = getAppList(AppUtils.getInstalledUserApp(requireContext()))
                    systemAppList = getAppList(AppUtils.getInstalledSystemApp(requireContext()))
                    filterUserList(userAppList)
                    filterSystemList(systemAppList)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFromAssist = arguments?.getBoolean("isFromAssist") == true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSelectBinding.inflate(layoutInflater, container, false)
        initView()
        updateAppList()
        return binding.root
    }

    private fun updateAppList() {
        thread {
            val msg = Message()
            msg.what = updateList
            handler.sendMessage(msg)
        }
    }

    private fun initView() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.viewPager.adapter = object : FragmentStateAdapter(requireActivity()) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int) = when (position) {
                0 -> AppUserFragment()
                else -> AppSystemFragment()
            }
        }
        binding.apply {
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                run {
                    when (position) {
                        0 -> tab.text = "用户应用"
                        1 -> tab.text = "系统应用"
                    }
                }
            }.attach()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            updateAppList()
        }
        userAdapter.setOnClickListener(object : AppSelectAdapter.OnItemClickListener {
            override fun onItemClickListener(appItem: AppItem) {
                clickResponse(appItem)
            }
        })
        systemAdapter.setOnClickListener(object : AppSelectAdapter.OnItemClickListener {
            override fun onItemClickListener(appItem: AppItem) {
                clickResponse(appItem)
            }
        })
    }
    private fun clickResponse(appItem: AppItem){
        if (isFromAssist){
            val assistConfig = AssistConfig(appName = appItem.name, packageName = appItem.packageName)
            viewModel.insertAssistConfigs(assistConfig)
        }else{
            methodViewModel.appLive.value = appItem
        }
        back()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_select, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                currentQueryText = newText.trim()
                filterUserList(userAppList)
                filterSystemList(systemAppList)
                return true
            }

        })
    }

    private fun filterUserList(userList: List<AppItem>) {
        if (currentQueryText == "") {
            userAdapter.submitList(userList)
        } else {
            val filter1 = userAppList.filter {
                it.packageName.contains(currentQueryText, true) ||
                        it.name.contains(currentQueryText, true)
            }
            userAdapter.submitList(filter1)
        }
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressBar3.visibility = View.GONE
    }

    private fun filterSystemList(systemList: List<AppItem>) {
        if (currentQueryText == "") {
            systemAdapter.submitList(systemList)
        } else {
            val filter2 = systemAppList.filter {
                it.packageName.contains(currentQueryText, true) ||
                        it.name.contains(currentQueryText, true)
            }
            systemAdapter.submitList(filter2)
        }
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progressBar3.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                back()
            }
        }
        return true
    }

    private fun getAppList(packageInfoList: List<PackageInfo>): List<AppItem> {
        val appList = ArrayList<AppItem>()
        for (i in packageInfoList.indices) {
            packageInfoList[i].apply {
                appList.add(
                    AppItem(
                        AppUtils.getAppName(requireContext(), this),
                        AppUtils.getAppIcon(requireContext(), this),
                        packageName,
                        AppUtils.getAppVersionName(requireContext(), packageName),
                        getDateTime(lastUpdateTime)
                    )
                )
            }
        }
        return appList
    }

    /**
     * 获取最后一次更新时间
     */
    @SuppressLint("SimpleDateFormat")
    private fun getDateTime(time: Long) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat("yy-MM-dd").format(time)
    } else ""



}