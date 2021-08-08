package me.simpleHook.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.AppSelectAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.FragmentAppUserBinding
import me.simpleHook.viewmodel.AppViewModel
import me.simpleHook.viewmodel.MethodViewModel


class AppUserFragment : Fragment() {
    private lateinit var binding:FragmentAppUserBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppUserBinding.inflate(inflater,container,false)
        return binding.root
    }
    @DelicateCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val methodViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.NewInstanceFactory())[MethodViewModel::class.java]
        val viewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application))[AppViewModel::class.java]
        val appAdapter = AppSelectAdapter.getAppSelectAdapter1().also {
            it.setOnClickListener(object :AppSelectAdapter.OnItemClickListener{
                override fun onItemClickListener(appItem: AppItem) {
                    methodViewModel.appLive.value = appItem
                    back()
                }
            })
        }
        binding.userRecyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val swipeRefreshLayout = requireActivity().findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        viewModel.userApps.observe(viewLifecycleOwner){
            appAdapter.submitList(it).also {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    fun back() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigateUp()
    }
}