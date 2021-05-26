package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.R
import me.simpleHook.adapter.MethodAdapter
import me.simpleHook.bean.AppConfig
import me.simpleHook.bean.MethodConfig
import me.simpleHook.constant.ModeConstant
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ClassConfigDialogBinding
import me.simpleHook.databinding.FragmentAddBinding
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field

class AddFragment : BaseFragment() {
    private lateinit var viewModel: MethodViewModel
    private val methodsList = ArrayList<MethodConfig>()
    private var mode = ModeConstant.HOOK_RETURN
    private var appMode = ModeConstant.HOOK_ORIGIN
    private lateinit var binding: FragmentAddBinding
    private lateinit var appViewModel: AppViewModel
    private var modify = false
    private var modifyMethodConfig = false
    private var modifyMethodConfigPosition = 0
    private var toSelectApp = false
    private var configId = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentAddBinding.inflate(inflater, container, false).let {
        binding = it
        it.root

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val list = arrayListOf("普通模式", "360加固", "腾讯御安全", "其他加固")
        binding.modeSelectSpinner.adapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, list)
        binding.modeSelectSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    appMode = position
                    when (list[position]) {
                        "其他加固" -> binding.applicationEdit.visibility = View.VISIBLE
                        else -> binding.applicationEdit.visibility = View.GONE
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        initViewModel()

    }


    private fun initViewModel() {
        val adapter = MethodAdapter(object : MethodAdapter.OnItemClickListener {
            override fun onItemClickListener(position: Int) {
                modifyMethodConfigPosition = position
                val methodConfig = methodsList[position]
                showDialog(
                    methodConfig.mode,
                    methodConfig.className,
                    methodConfig.methodName,
                    methodConfig.params,
                    methodConfig.resultValues,
                )
            }
        })
        viewModel =
            ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                MethodViewModel::class.java
            )
        viewModel.getMethodLive()?.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        viewModel.configLive.value?.let {
            modify = true
            configId = it.id
            val jsonObject = JSONObject(it.config)
            binding.apply {
                appNameEdit.setText(it.appName)
                appVersionNameEdit.setText(it.versionName)
                packageNameEdit.setText(it.packageName)
                appVersionNameEdit.setText(it.versionName)
                descStringEdit.setText(it.description)
                modeSelectSpinner.setSelection(jsonObject.getInt("mode"))
                val jsonArray = JSONArray(jsonObject.getString("config"))
                methodsList.clear()
                for (i in 0 until jsonArray.length()) {
                    jsonArray.getJSONObject(i).apply {
                        methodsList.add(
                            MethodConfig(
                                getInt("mode"),
                                getString("className"),
                                getString("methodName"),
                                getString("params"),
                                getString("resultValues")
                            )
                        )
                    }
                }
                viewModel.getMethodLive()?.value = methodsList
            }
        }
        viewModel.appLive.observe(viewLifecycleOwner) {
            binding.apply {
                appNameEdit.setText(it.appName)
                appVersionNameEdit.setText(it.versionName)
                packageNameEdit.setText(it.packageName)
            }
        }
        binding.methodRV.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireActivity())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
        appViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.getMethodLive()?.value = ArrayList()
        viewModel.configLive.value = null
    }

    private fun showDialog(
        mode: Int = 0,
        className: String = "",
        methodName: String = "",
        params: String = "",
        results: String = "",
    ) {
        val dialogBinding = ClassConfigDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.apply {
            classNameEdit.setText(className)
            methodNameEdit.setText(methodName)
            paramsEdit.setText(params)
            resultValueEdit.setText(results)
        }
        AlertDialog.Builder(requireActivity()).apply {
            setView(dialogBinding.root)
            val list = arrayListOf("Hook返回值", "Hook参数值", "中断执行")
            dialogBinding.modeSelectSpinner.adapter =
                ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, list)
            dialogBinding.modeSelectSpinner.setSelection(mode)
            dialogBinding.modeSelectSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        this@AddFragment.mode = position
                        when (list[position]) {
                            "中断执行" -> {
                                dialogBinding.resultValueInput.visibility = View.GONE
                            }
                            else -> {
                                dialogBinding.resultValueInput.visibility = View.VISIBLE
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            setCancelable(false)
            if (className.isNotEmpty()) {
                modifyMethodConfig = true
                setPositiveButton("修改") { d, _ ->
                    dialogDismiss(
                        d, toCheck(dialogBinding)
                    )
                }
            } else {
                setPositiveButton("确认") { d, _ ->
                    dialogDismiss(
                        d, toCheck(dialogBinding)
                    )
                }
            }
            setNegativeButton("取消", null)
                .create().show()
        }


    }

    private fun dialogDismiss(dialog: DialogInterface, canCancel: Boolean) {
        try {
            val mShowing: Field =
                dialog.javaClass.superclass!!.superclass!!.getDeclaredField("mShowing")
            mShowing.isAccessible = true
            mShowing.set(dialog, canCancel)
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun toCheck(dialogBinding: ClassConfigDialogBinding): Boolean {
        var canCancel = true
        val className = dialogBinding.classNameEdit.text.toString().trim()
        val methodName = dialogBinding.methodNameEdit.text.toString().trim()
        val params = dialogBinding.paramsEdit.text.toString().trim()
        val results = dialogBinding.resultValueEdit.text.toString().trim()
        if (mode != 2) {
            dialogBinding.apply {
                if (className.isNotEmpty() &&
                    methodName.isNotEmpty() &&
                    results.isNotEmpty()
                ) {
                    /*val methodConfigItem = MethodConfig(mode, className, methodName, params, results)*/
                    val methodConfig =
                        MethodConfig(mode, className, methodName, params, results)
                    if (modifyMethodConfig) {
                        methodsList[modifyMethodConfigPosition] = methodConfig
                    } else {
                        methodsList.add(methodConfig)
                    }
                    viewModel.getMethodLive()?.value = methodsList
                } else {
                    Toast.makeText(requireActivity(), "有必填项为空", Toast.LENGTH_SHORT).show()
                    canCancel = false
                }
            }

        } else {
            val methodConfig = MethodConfig(mode, className, methodName, params, results)
            MethodConfig(mode, className, methodName, params, results)
            if (modifyMethodConfig) {
                methodsList[modifyMethodConfigPosition] = methodConfig
            } else {
                methodsList.add(methodConfig)
            }
            viewModel.getMethodLive()?.value = methodsList
        }
        return canCancel
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_add, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_method_config -> showDialog()
            android.R.id.home -> {
                back()
            }
            R.id.save_config -> {
                if (methodsList.size == 0) {
                    Toast.makeText(requireContext(), "这是什么东西，不能保存", Toast.LENGTH_LONG).show()
                    return true
                }
                val appConfig = toCheckCanSave().toString()
                binding.apply {
                    val appName = binding.appNameEdit.text.toString()
                    val packageName = binding.packageNameEdit.text.toString()
                    val description = binding.descStringEdit.text.toString()
                    val versionName = binding.appVersionNameEdit.text.toString()
                    AppConfigEntity(
                        packageName,
                        appName,
                        versionName,
                        description,
                        appConfig,
                        id = configId
                    ).apply {
                        if (modify) {
                            appViewModel.updateConfigs(this)
                        } else {
                            appViewModel.insertConfigs(this)
                        }
                    }
                }
                back()
            }
            R.id.select_app -> {
                toSelectApp = true
                val navHostFragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.action_addFragment_to_appSelectFragment)
            }
        }
        return true
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun toCheckCanSave(): AppConfig {
        val appName = binding.appNameEdit.text.toString()
        val packageName = binding.packageNameEdit.text.toString()
        val description = binding.descStringEdit.text.toString()
        val versionName = binding.appVersionNameEdit.text.toString()
        val config = StringBuilder()
        config.append("\"config\": [")
        for (i in 0 until methodsList.size) {
            config.append("${methodsList[i].toString()},")
        }
        return AppConfig(
            appName,
            packageName,
            appMode,
            description,
            versionName,
            config.substring(0, config.length - 1) + "]"
        )
    }


}