package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.databinding.FragmentAppListBinding
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.util.dp


class AppListFragment(private val tagFragment: String = "user") : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ViewCompat.onApplyWindowInsets(requireActivity().window.decorView, windowInsets)
            binding.recyclerView.updatePadding(bottom = navigationInsets.bottom + 20.dp)
            windowInsets
        }
        val appAdapter =
            if (tagFragment == "user") AppListAdapter.getAppSelectAdapter1() else AppListAdapter.getAppSelectAdapter2()
        binding.recyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val myFastScroller = FastScrollerUtil.bind(binding.recyclerView)
        myFastScroller.setSwipeRefreshLayout(requireActivity().findViewById(R.id.swipeRefreshLayout))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}