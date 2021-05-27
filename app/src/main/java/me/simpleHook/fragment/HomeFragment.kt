package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import me.simpleHook.utils.ToolUtils
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray


@Suppress("COMPATIBILITY_WARNING")
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
        initView()
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
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
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
                Snackbar.make(
                    requireActivity().findViewById(R.id.fragment),
                    "已删除此配置",
                    Snackbar.LENGTH_LONG
                ).setAction(
                    "撤销", View.OnClickListener {
                        if (configDelete != null) {
                            viewModel.insertConfigs(configDelete)
                        }
                    }).show()
            }

        }).attachToRecyclerView(binding.mainRecycler)
    }

    private fun initView() {
        binding.addConfig.setOnClickListener { toAddFragment() }
        binding.importConfigs.setOnClickListener {
            ToolUtils.getClipboardContent(requireContext())?.let { toInsertConfigs(it) }
        }
        binding.shareConfigs.setOnClickListener {
            val list = viewModel.getAllConfigs().value
            val configs = StringBuilder()
            for (i in list!!.indices) {
                configs.append("${list[i].config},")
            }
            ToolUtils.toClip(requireContext(), "[${configs.substring(0, configs.length - 1)}]")
            Toast.makeText(requireContext(), "已复制到剪切板", Toast.LENGTH_LONG).show()
        }
    }

    private fun toInsertConfigs(configs: String) {
        var tip = "导入成功"
        try {
            val configsJsonArray = JSONArray(configs)
            /* val arrayConfig = arrayOf<AppConfigEntity>()*/
            for (i in 0 until configsJsonArray.length()) {
                configsJsonArray.getJSONObject(i).apply {
                    viewModel.insertConfigs(
                        AppConfigEntity(
                            getString("packageName"),
                            getString("appName"),
                            getString("versionName"),
                            getString("description"),
                            toString()
                        )
                    )
                }
            }
            /*viewModel.insertConfigs(*arrayConfig)*/

        } catch (e: Exception) {
            e.stackTrace
            tip = "导入失败"
        }
        Toast.makeText(requireContext(), tip, Toast.LENGTH_LONG).show()
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