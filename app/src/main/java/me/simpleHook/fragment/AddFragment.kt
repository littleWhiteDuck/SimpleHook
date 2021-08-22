package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import me.simpleHook.R
import me.simpleHook.adapter.MethodAdapter
import me.simpleHook.bean.AppConfig
import me.simpleHook.bean.MethodConfig
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ConfigDialogBinding
import me.simpleHook.databinding.FragmentAddBinding
import me.simpleHook.util.FileUtils
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.toast
import me.simpleHook.viewmodel.MethodViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field

class AddFragment : BaseFragment() {
    private lateinit var viewModel: MethodViewModel
    private val methodsList = ArrayList<MethodConfig>()
    private var mode = Constant.HOOK_RETURN
    private var appMode = Constant.HOOK_ORIGIN
    private lateinit var binding: FragmentAddBinding
    private lateinit var appViewModel: AppViewModel
    private var modify = false
    private var modifyMethodConfig = false
    private var modifyMethodConfigPosition = 0
    private var configId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBinding.inflate(inflater, container, false)
        initView()
        initViewModel()
        return binding.root
    }

    private fun initView() {
        binding.addMethodConfig.setOnClickListener { showDialog() }
        val list = arrayListOf("普通模式", "加固模式")
        binding.modeSelectSpinner.adapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, list)
        binding.modeSelectSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    appMode = position
                    when (list[position]) {
                        "其他加固" -> binding.applicationEdit.visibility = View.VISIBLE
                        else -> binding.applicationEdit.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }


    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(MethodViewModel::class.java)
        val adapter = MethodAdapter(object : MethodAdapter.OnItemClickListener {
            override fun onItemClickListener(position: Int) {
                modifyMethodConfigPosition = position
                val methodConfig = methodsList[position]
                showDialog(methodConfig)
            }

            override fun onItemLongClickListener(position: Int) {
                val methodConfig = methodsList[position]
                methodsList.add(methodConfig)
                viewModel.getMethodLive()?.value = methodsList
                Snackbar.make(binding.addMethodConfig, "已经重复新增了一个，快去更改",Snackbar.LENGTH_LONG)
                    .setAction("撤销"){
                        methodsList.removeAt(methodsList.size - 1)
                        viewModel.getMethodLive()?.value = methodsList
                    }.show()
            }
        })

        viewModel.getMethodLive()?.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
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
                                JsonUtil.getElementString(this, "fieldName"),
                                JsonUtil.getElementString(this, "fieldType"),
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
                if (viewModel.appLive.value != null){
                    appNameEdit.setText(it.name)
                    appVersionNameEdit.setText(it.versionName)
                    packageNameEdit.setText(it.packageName)
                }
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
        Log.d("====================", "onDestroy: ")
        viewModel.getMethodLive()?.value = ArrayList()
        viewModel.configLive.value = null
        viewModel.appLive.value = null
    }

    private fun showDialog(methodConfig: MethodConfig = MethodConfig(0, "", "", "", "", "", "")) {
        val dialogBinding = ConfigDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.apply {
            methodConfig.apply {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsEdit.setText(params)
                filedNameEdit.setText(fieldName)
                fieldTypeEdit.setText(fieldType)
                resultValueEdit.setText(resultValues)
                help.setOnClickListener { showHelpDialog() }
                when (mode) {
                    Constant.HOOK_BREAK -> breakHook(dialogBinding)
                    Constant.HOOK_STATIC_FIELD -> staticFieldHook(dialogBinding)
                    Constant.HOOK_FIELD -> fieldHook(dialogBinding)
                }
            }
        }
        AlertDialog.Builder(requireActivity()).apply {
            setView(dialogBinding.root)
            val list = arrayListOf("Hook返回值", "Hook参数值", "中断执行", "Hook静态变量", "Hook变量")
            dialogBinding.modeSelectSpinner.adapter =
                ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, list)
            dialogBinding.modeSelectSpinner.setSelection(methodConfig.mode)
            dialogBinding.modeSelectSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        mode = position
                        when (list[position]) {
                            "中断执行" -> breakHook(dialogBinding)
                            "Hook静态变量" -> staticFieldHook(dialogBinding)
                            "Hook变量" -> fieldHook(dialogBinding)
                            else -> othersHook(dialogBinding)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            setCancelable(false)
            modifyMethodConfig = methodConfig.className.isNotEmpty().also {
                val positiveButtonText = if (it) "修改" else "增加"
                setPositiveButton(positiveButtonText) { d, _ ->
                    dialogDismiss(
                        d, toCheck(dialogBinding)
                    )
                }
                if (it) {
                    setNeutralButton("删除") { d, _ ->
                        deleteConfig(methodConfig)
                        dialogDismiss(
                            d, true
                        )
                    }
                }
            }
            setNegativeButton("取消") { d, _ ->
                dialogDismiss(d, true)
            }
                .create().show()
        }
    }

    private fun fieldHook(dialogBinding: ConfigDialogBinding) {
        dialogBinding.apply {
            paramsTypeInput.helperText = "输入构造方法参数类型(注意用了哪个构造方法)"
            filedNameInput.hint = "输入变量名"
            setViewShow(paramsTypeInput, true)
            setViewShow(methodNameInput)
            setViewShow(fieldTypeInput, true)
            setViewShow(filedNameInput, true)
            setViewShow(resultValueInput, true)
        }
    }

    private fun othersHook(dialogBinding: ConfigDialogBinding) {
        dialogBinding.apply {
            paramsTypeInput.helperText = "例如：boolean,int,java.lang.String"
            setViewShow(fieldTypeInput)
            setViewShow(filedNameInput)
            setViewShow(resultValueInput, true)
            setViewShow(paramsTypeInput, true)
            setViewShow(methodNameInput, true)
        }
    }

    private fun breakHook(dialogBinding: ConfigDialogBinding) {
        dialogBinding.apply {
            setViewShow(resultValueInput)
            setViewShow(fieldTypeInput)
            setViewShow(filedNameInput)
            setViewShow(paramsTypeInput, true)
            setViewShow(methodNameInput, true)
        }
    }

    private fun staticFieldHook(dialogBinding: ConfigDialogBinding) {
        dialogBinding.apply {
            setViewShow(paramsTypeInput)
            setViewShow(methodNameInput)
            setViewShow(fieldTypeInput, true)
            setViewShow(filedNameInput, true)
            setViewShow(resultValueInput, true)
        }
    }

    private fun setViewShow(view: View, isShow: Boolean = false) {
        view.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    private fun deleteConfig(methodConfig: MethodConfig) {
        methodsList.remove(methodConfig)
        viewModel.getMethodLive()!!.value = methodsList
    }

    @SuppressLint("SetTextI18n")
    private fun showHelpDialog() {
        val webView = WebView(requireContext())
        webView.loadUrl("file:///android_asset/introduce.html")
        AlertDialog.Builder(requireContext()).apply {
            setTitle("帮助")
            setView(webView)
            setPositiveButton("取消", null)
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

    private fun toCheck(dialogBinding: ConfigDialogBinding): Boolean {
        var canCancel = true
        val className = dialogBinding.classNameEdit.text.toString().trim()
        val methodName = dialogBinding.methodNameEdit.text.toString().trim()
        val params = dialogBinding.paramsEdit.text.toString().trim()
        val results = dialogBinding.resultValueEdit.text.toString().trim()
        val fieldName = dialogBinding.filedNameEdit.text.toString()
        val fieldType = dialogBinding.fieldTypeEdit.text.toString()
        when (mode) {
            Constant.HOOK_BREAK -> {
                if (className.isNotEmpty() && methodName.isNotEmpty()) {
                    val methodConfig = MethodConfig(mode, className, methodName, params)
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
            Constant.HOOK_STATIC_FIELD -> {
                if (className.isNotEmpty() && fieldName.isNotEmpty() && fieldType.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig = MethodConfig(
                        mode,
                        className,
                        fieldName = fieldName,
                        fieldType = fieldType,
                        resultValues = results
                    )
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
            Constant.HOOK_FIELD -> {
                if (className.isNotEmpty() && fieldName.isNotEmpty() && fieldType.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig = MethodConfig(
                        mode,
                        className,
                        params = params,
                        fieldName = fieldName,
                        fieldType = fieldType,
                        resultValues = results
                    )
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
            else -> {
                if (className.isNotEmpty() && methodName.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig =
                        MethodConfig(mode, className, methodName, params, resultValues = results)
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
        }
        return canCancel
    }

    private fun addConfig(methodConfig: MethodConfig) {
        if (modifyMethodConfig) {
            methodsList[modifyMethodConfigPosition] = methodConfig
        } else {
            methodsList.add(methodConfig)
        }
        viewModel.getMethodLive()?.value = methodsList
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_add, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> back()
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
                    me.simpleHook.database.entity.AppConfig(
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
                        FileUtils.writeData(
                            "${Constant.CONFIG_DIRECTORY + packageName}/",
                            "config",
                            appConfig
                        )
                        val pref = try {
                            requireContext().getSharedPreferences(
                                "hookConfig",
                                Context.MODE_WORLD_READABLE
                            )
                        } catch (e: SecurityException) {
                            null
                        }
                        pref?.edit()?.putString(packageName, appConfig)?.apply()
                            ?: "模块未激活，将无法使用New XSharePreferences获取配置".toast(requireContext())
                    }
                }
                progressDialog()
            }
            R.id.select_app -> {
                val navHostFragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.action_addFragment_to_appSelectFragment)
            }
        }
        return true
    }

    private fun progressDialog() {
        val progress = ProgressDialog(requireContext())
        progress.setTitle("正在保存...")
        progress.setCancelable(false)
        progress.show()
        Handler().postDelayed(Runnable {
            progress.dismiss()
            back()
        }, 1500)
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun toCheckCanSave(): AppConfig {
        val appName = binding.appNameEdit.text.toString()
        val packageName = binding.packageNameEdit.text.toString()
        val description = binding.descStringEdit.text.toString()
        val versionName = binding.appVersionNameEdit.text.toString()
        val config = StringBuilder().also {
            for (i in 0 until methodsList.size) {
                it.append("${methodsList[i]},")
            }
        }
        return AppConfig(
            appName,
            packageName,
            appMode,
            description,
            versionName,
            "\"config\": [${config.substring(0, config.length - 1)}]"
        )
    }


}