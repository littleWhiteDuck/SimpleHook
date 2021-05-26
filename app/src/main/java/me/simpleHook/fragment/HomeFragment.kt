package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.viewmodel.MethodViewModel


class HomeFragment : Fragment() {
    private lateinit var viewModel: AppViewModel
    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentHomeBinding.inflate(inflater, container, false).let {
        binding = it
        it.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.floatingActionButton.setOnClickListener { toAddFragment() }
        val adapter = HomeAdapter({ appConfigEntity -> adapterOnClick(appConfigEntity) },
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) })
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
        val allConfigs = viewModel.getAllConfigs()
        allConfigs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.mainRecycler.apply {
            this.adapter = adapter
            layoutManager = linearLayoutManager
        }
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            @SuppressLint("ShowToast")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val configDelete = allConfigs.value?.get(viewHolder.adapterPosition)
                if (configDelete != null) {
                    viewModel.deleteConfigs(configDelete)
                }
                Snackbar.make(requireActivity().findViewById(R.id.fragment),"已删除此配置",Snackbar.LENGTH_LONG).setAction(
                    "撤销", View.OnClickListener {
                        if (configDelete != null) {
                            viewModel.insertConfigs(configDelete)
                        }
                    }).show()
            }

        }).attachToRecyclerView(binding.mainRecycler)
    }

    private fun switchOnChange(appConfigEntity: AppConfigEntity, isChecked: Boolean) {
        appConfigEntity.canUse = isChecked
        viewModel.updateConfigs(appConfigEntity)
    }

    private fun adapterOnClick(appConfig: AppConfigEntity) {
        val viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        )[MethodViewModel::class.java]
        viewModel.configLive.value = appConfig
        toAddFragment()
    }

    private fun toAddFragment() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_homeFragment_to_addFragment)
    }

}