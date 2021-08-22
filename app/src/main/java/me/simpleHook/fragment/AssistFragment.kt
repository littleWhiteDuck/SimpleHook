package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAssistBinding

class AssistFragment : Fragment() {
    private val appViewModel by lazy { ViewModelProvider(
        requireActivity(),
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application))[AppViewModel::class.java] }
    private lateinit var binding: FragmentAssistBinding
    private val mAdapter: AssistAdapter by lazy { AssistAdapter({assistConfig -> itemOnClick(assistConfig) },
        {assistConfig -> itemOnLongClick(assistConfig) }) }

    @SuppressLint("ShowToast")
    private fun itemOnLongClick(assistConfig: AssistConfig) {
        appViewModel.deleteAssistConfigs(assistConfig)
        Snackbar.make(binding.addConfig, "已删除此配置", Snackbar.LENGTH_LONG)
            .setAction("撤销"){
                appViewModel.insertAssistConfigs(assistConfig)
            }.show()
    }

    private fun itemOnClick(assistConfig: AssistConfig) {
        val navController = Navigation.findNavController(binding.assistRev)
        val bundle = Bundle()
        bundle.putParcelable("assistConfig", assistConfig)
        navController.navigate(R.id.action_assistFragment_to_assistSettingsFragment, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssistBinding.inflate(inflater, container, false)
        initView()
        initViewModel()
        return binding.root
    }

    private fun initViewModel() {
        appViewModel.getAllAssistConfigs().observe(viewLifecycleOwner) {
            mAdapter.submitList(it)
        }
    }

    private fun initView() {
        binding.addConfig.setOnClickListener {
            val navController = Navigation.findNavController(it)
            val bundle = Bundle()
            bundle.putBoolean("isFromAssist", true)
            navController.navigate(R.id.action_assistFragment_to_appSelectFragment, bundle)
        }
        binding.assistRev.apply {
            adapter = mAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

}