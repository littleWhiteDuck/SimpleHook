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
import com.lxj.xpopup.XPopup
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.custom.BottomDialog
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.utils.JsonUtil
import me.simpleHook.utils.ToolUtils
import me.simpleHook.utils.toast
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray
import org.json.JSONObject


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
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            { appConfigEntity, builder -> itemOnLongClick(appConfigEntity, builder) })
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
        val allConfigs = viewModel.getAllConfigs()
        allConfigs.observe(requireActivity()) {
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
                val configDelete = allConfigs.value!![viewHolder.adapterPosition]
                deleteConfig(configDelete)
            }

        }).attachToRecyclerView(binding.mainRecycler)
    }

    fun deleteConfig(appConfigEntity: AppConfigEntity) {
        viewModel.deleteConfigs(appConfigEntity)
        Snackbar.make(
            requireActivity().findViewById(R.id.fragment),
            "已删除此配置", Snackbar.LENGTH_LONG
        ).setAction("撤销") {
            viewModel.insertConfigs(appConfigEntity)
        }.show()
    }

    private fun itemOnLongClick(appConfigEntity: AppConfigEntity, builder: XPopup.Builder) {
        val arrayOfString = arrayOf("编辑", "删除", "分享")
        builder.asAttachList(arrayOfString, null) { _, text ->
            when (text) {
                "编辑" -> editConfig(appConfigEntity)
                "删除" -> deleteConfig(appConfigEntity)
                "分享" -> copyConfigs(appConfigEntity.config)
            }
        }.show()

    }

    private fun editConfig(appConfigEntity: AppConfigEntity) {
        val viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        )[MethodViewModel::class.java]
        viewModel.configLive.value = appConfigEntity
        toAddFragment()
    }

    private fun copyConfigs(config: String) {
        ToolUtils.toClip(requireContext(), JsonUtil.formatJson(config))
        Toast.makeText(requireContext(), "已复制到剪切板", Toast.LENGTH_LONG).show()
    }

    private fun initView() {
        binding.addConfig.setOnClickListener { toAddFragment() }
        binding.importConfigs.setOnClickListener {
            ToolUtils.getClipboardContent(requireContext())?.let { importConfigs(it) }
        }
        binding.shareConfigs.setOnClickListener { shareConfigs() }
        binding.importLearnHookConfigs.setOnClickListener {
            ToolUtils.getClipboardContent(requireContext())?.let { importLearnHookConfigs(it) }
        }
    }

    private fun shareConfigs(){
        val list = viewModel.getAllConfigs().value
        val configs = StringBuilder()
        for (i in list!!.indices) {
            configs.append("${list[i].config},")
        }
        ToolUtils.toClip(
            requireContext(),
            JsonUtil.formatJson("[${configs.substring(0, configs.length - 1)}]")
        )
        "已复制到剪切板".toast(requireContext())
    }

    private fun importLearnHookConfigs(configs: String){
        when{
            JsonUtil.isJsonArray(configs) -> {

            }
            JsonUtil.isJsonObject(configs) -> {

            }
            else -> {

            }
        }
    }

    private fun importConfigs(configs: String) {
        when {
            JsonUtil.isJsonArray(configs) -> {
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
                tip.toast(requireContext())
            }
            JsonUtil.isJsonObject(configs) -> {
                JSONObject(configs).apply {
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
            else -> {
                "格式错误".toast(requireContext())
            }
        }
    }

    private fun switchOnChange(appConfigEntity: AppConfigEntity, isChecked: Boolean) {
        appConfigEntity.canUse = isChecked
        viewModel.updateConfigs(appConfigEntity)
    }

    private fun adapterOnClick(appConfig: AppConfigEntity) {
        XPopup.Builder(requireContext())
            .isDestroyOnDismiss(true)
            .asCustom(
                BottomDialog(
                    requireContext(),
                    appConfig,
                    onClick = { editConfig(appConfig) })
            )
            .show()
    }

    private fun toAddFragment() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_homeFragment_to_addFragment)
    }

}