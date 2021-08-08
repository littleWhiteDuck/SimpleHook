package me.simpleHook.fragment

import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import me.simpleHook.R
import me.simpleHook.adapter.AppSelectAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.FragmentAppSelectBinding
import me.simpleHook.viewmodel.AppViewModel
import me.simpleHook.viewmodel.MethodViewModel


class AppSelectFragment : BaseFragment(), CoroutineScope by MainScope() {
    private lateinit var userAppList: List<AppItem>
    private lateinit var systemAppList: List<AppItem>
    private lateinit var binding: FragmentAppSelectBinding
    private lateinit var viewModel: AppViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSelectBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
        launch {
            viewModel.userApps.value?:viewModel.fetchData()
        }
        viewModel.userApps.observe(viewLifecycleOwner){
            userAppList = it
        }
        viewModel.systemApps.observe(viewLifecycleOwner){
            systemAppList = it
        }
        initView()

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
            viewModel.fetchData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }


/*
    private fun getList(): ArrayList<AppItem> {
        val appList = ArrayList<AppItem>()
        val context = requireContext()
        val list = AppUtils.getInstalledApp(context)
        for (i in list.indices) {
            val appName = AppUtils.getAppName(context, list[i])
            val appIcon = AppUtils.getAppIcon(context, list[i])
            val packageName = list[i].packageName
            val versionName = AppUtils.getAppVersionName(context, packageName)
            appList.add(AppItem(appName, packageName, appIcon, versionName))
        }
        return appList
    }*/

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_select, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                val filter1 = userAppList.filter {
                    it.packageName.contains(newText, true) ||
                            it.name.contains(newText, true)
                }
                val filter2 = systemAppList.filter {
                    it.packageName.contains(newText, true) ||
                            it.name.contains(newText, true)
                }
                updateList(filter1, filter2)
                return true
            }

        })
    }

    private fun updateList(userList: List<AppItem>,systemList:List<AppItem>) {
       AppSelectAdapter.getAppSelectAdapter1().submitList(userList)
        AppSelectAdapter.getAppSelectAdapter2().submitList(systemList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                back()
            }
        }
        return true
    }

}