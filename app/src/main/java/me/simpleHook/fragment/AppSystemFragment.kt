package me.simpleHook.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.adapter.AppSelectAdapter
import me.simpleHook.databinding.FragmentAppSystemBinding


class AppSystemFragment : Fragment() {
    private lateinit var binding: FragmentAppSystemBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSystemBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        val appAdapter = AppSelectAdapter.getAppSelectAdapter2()
        binding.systemRecyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}