package me.simpleHook.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.databinding.FragmentAppUserBinding


class AppUserFragment : Fragment() {

    private lateinit var binding: FragmentAppUserBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppUserBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        val appAdapter = AppListAdapter.getAppSelectAdapter1()
        binding.userRecyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

}