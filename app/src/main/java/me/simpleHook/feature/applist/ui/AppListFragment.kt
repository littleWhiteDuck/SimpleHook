package me.simpleHook.feature.applist.ui

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
import me.simpleHook.core.base.BaseViewFragment
import me.simpleHook.core.extension.dp
import me.simpleHook.feature.applist.ui.adapter.AppListAdapter
import me.simpleHook.core.utils.FastScrollerUtil
import me.simpleHook.feature.applist.viewmodel.AppListViewModel


open class AppListFragment() : BaseViewFragment<RecyclerView>() {

    protected open val currentLabel = LABEL_USER

    private val appListViewModel by activityViewModels<AppListViewModel>()
    private val appAdapter = AppListAdapter {
        appListViewModel.updateSelectApp(it)
    }
    private val swipeRefreshLayout by lazy {
        requireActivity().findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)!!
    }

    override fun initRootView(): RecyclerView {
        val recyclerView = RecyclerView(requireContext()).apply {
            id = if (currentLabel == LABEL_USER) R.id.user_app_recycler else R.id.system_app_recycler
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
        if (currentLabel == LABEL_USER) {
            appListViewModel.userApps.observe(viewLifecycleOwner) {
                appAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        } else {
            appListViewModel.systemApps.observe(viewLifecycleOwner) {
                appAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    private fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(root, windowInsets)
            root.setPadding(0, 0, 0, navigationInsets.bottom + 24.dp)
            windowInsets
        }
        with(root) {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
            FastScrollerUtil.bind(root)
                .setSwipeRefreshLayout(requireActivity().findViewById(R.id.swipeRefreshLayout))
        }
    }

    companion object {
        const val LABEL_USER = "USER"
        const val LABEL_SYSTEM = "SYSTEM"
    }

}


class UserAppListFragment() : AppListFragment() {
    override val currentLabel: String = LABEL_USER
}

class SystemAppListFragment() : AppListFragment() {
    override val currentLabel: String = LABEL_SYSTEM
}