package me.simpleHook.ui.fragment

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.databinding.FragmentAppListBinding
import me.simpleHook.extension.dp
import me.simpleHook.base.BaseFragment
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.viewmodel.AppViewModel


class AppListFragment(private val label: String) : BaseFragment<FragmentAppListBinding>() {

    private val appViewModel by activityViewModels<AppViewModel>()
    private val mAdapter = AppListAdapter {
        appViewModel.updateSelectApp(it)
    }
    private val swipeRefreshLayout by lazy {
        requireActivity().findViewById(R.id.swipeRefreshLayout) as SwipeRefreshLayout
    }

    override fun init() {
        initData()
        initView()
    }

    private fun initData() {
        if (label == "user") {
            appViewModel.userApps.observe(viewLifecycleOwner) {
                mAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        } else {
            appViewModel.systemApps.observe(viewLifecycleOwner) {
                mAdapter.submitList(it)
                if (it.isNotEmpty()) swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    private fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(requireActivity().window.decorView, windowInsets)
            binding.recyclerView.updatePadding(bottom = navigationInsets.bottom + 20.dp)
            windowInsets
        }
        val appAdapter = mAdapter
        binding.recyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        FastScrollerUtil.bind(binding.recyclerView)
    }

}