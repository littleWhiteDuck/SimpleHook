package me.simpleHook.ui.fragment

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import me.simpleHook.R
import me.simpleHook.base.BaseViewFragment
import me.simpleHook.extension.dp
import me.simpleHook.recyclerview.adapter.AppListAdapter
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.viewmodel.AppViewModel


class AppListFragment(private val label: String) : BaseViewFragment<RecyclerView>() {

    private val appViewModel by activityViewModels<AppViewModel>()
    private val appAdapter = AppListAdapter {
        appViewModel.updateSelectApp(it)
    }
    private val swipeRefreshLayout by lazy {
        requireActivity().findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)!!
    }

    override fun initRootView(): RecyclerView {
        val recyclerView = RecyclerView(requireContext()).apply {
            id = if (label == "user") R.id.user_app_recycler else R.id.system_app_recycler
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            clipToPadding = false
        }
        return recyclerView
    }


    override fun init() {
        initData()
        initView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return true
    }

    private fun initData() {
        if (label == "user") {
            appViewModel.userApps.observe(viewLifecycleOwner) {
                appAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        } else {
            appViewModel.systemApps.observe(viewLifecycleOwner) {
                appAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    private fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(root, windowInsets)
            root.setPadding(0, 0, 0, navigationInsets.bottom + 20.dp)
            windowInsets
        }
        with(root) {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
            FastScrollerUtil.bind(root)
                .setSwipeRefreshLayout(requireActivity().findViewById(R.id.swipeRefreshLayout))
        }
    }

}