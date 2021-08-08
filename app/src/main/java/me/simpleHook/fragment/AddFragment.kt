package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
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
import me.simpleHook.R
import me.simpleHook.adapter.MethodAdapter
import me.simpleHook.bean.AppConfig
import me.simpleHook.bean.MethodConfig
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppConfigEntity
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentAddBinding.inflate(inflater, container, false).let {
        binding = it
        it.root

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val list = arrayListOf("普通模式", "加固模式")
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
        initView()
    }

    private fun initView() {
        binding.addMethodConfig.setOnClickListener { showDialog() }
    }


    private fun initViewModel() {
        val adapter = MethodAdapter(object : MethodAdapter.OnItemClickListener {
            override fun onItemClickListener(position: Int) {
                modifyMethodConfigPosition = position
                val methodConfig = methodsList[position]
                showDialog(methodConfig)
            }
        })
        viewModel =
            ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                MethodViewModel::class.java
            )
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
                                JsonUtil.getElementString(this,"fieldName"),
                                JsonUtil.getElementString(this,"fieldType"),
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
                appNameEdit.setText(it.name)
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

    private fun showDialog(methodConfig: MethodConfig = MethodConfig(0,"","","","","","")) {
        val dialogBinding = ConfigDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.apply {
            methodConfig.apply {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsEdit.setText(params)
                filedNameEdit.setText(fieldName)
                fieldTypeEdit.setText(fieldType)
                resultValueEdit.setText(resultValues)
                help.setOnClickListener{showHelpDialog()}
                when(mode){
                    2 -> breakHook(dialogBinding)
                    3 -> staticFieldHook(dialogBinding)
                }
            }
        }
        AlertDialog.Builder(requireActivity()).apply {
            setView(dialogBinding.root)
            val list = arrayListOf("Hook返回值", "Hook参数值", "中断执行","Hook静态变量")
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
                            else -> othersHook(dialogBinding)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            setCancelable(false)
            modifyMethodConfig = methodConfig.className.isNotEmpty().also {
                val positiveButtonText = if (it)"修改" else "增加"
                setPositiveButton(positiveButtonText) { d, _ ->
                    dialogDismiss(
                        d, toCheck(dialogBinding)
                    )
                }
                if (it){
                    setNeutralButton("删除"){ d, _ ->
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
    private fun othersHook(dialogBinding: ConfigDialogBinding){
        dialogBinding.apply {
            setViewShow(fieldTypeInput)
            setViewShow(filedNameInput)
            setViewShow(resultValueInput,true)
            setViewShow(paramsTypeInput,true)
            setViewShow(methodNameInput,true)
        }
    }
    private fun breakHook(dialogBinding: ConfigDialogBinding){
        dialogBinding.apply {
            setViewShow(resultValueInput)
            setViewShow(fieldTypeInput)
            setViewShow(filedNameInput)
            setViewShow(paramsTypeInput,true)
            setViewShow(methodNameInput,true)
        }
    }

    private fun staticFieldHook(dialogBinding: ConfigDialogBinding){
        dialogBinding.apply {
            setViewShow(paramsTypeInput)
            setViewShow(methodNameInput)
            setViewShow(fieldTypeInput,true)
            setViewShow(filedNameInput,true)
            setViewShow(resultValueInput,true)
        }
    }

    private fun setViewShow(view:View,isShow:Boolean = false){
        view.visibility = if (isShow)View.VISIBLE else View.GONE
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
            setPositiveButton("取消",null)
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
        when(mode){
            2 -> {
                if (className.isNotEmpty()&&methodName.isNotEmpty()){
                    val methodConfig = MethodConfig(mode, className, methodName, params)
                    addConfig(methodConfig)
                }else{
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
            3 -> {
                if(className.isNotEmpty()&&fieldName.isNotEmpty()&&fieldType.isNotEmpty()&&results.isNotEmpty()){
                    val methodConfig = MethodConfig(mode, className, fieldName = fieldName, fieldType = fieldType, resultValues = results)
                    addConfig(methodConfig)
                }else{
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
            else -> {
                if (className.isNotEmpty() && methodName.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig = MethodConfig(mode, className, methodName, params, resultValues = results)
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(requireContext())
                    canCancel = false
                }
            }
        }
        return canCancel
    }

    private fun addConfig(methodConfig: MethodConfig){
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
                        FileUtils.writeData("${Constant.CONFIG_DIRECTORY+packageName}/","config",appConfig)
                    }
                }
                Thread.sleep(150)
                back()
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
            "\"config\": [${ config.substring(0, config.length - 1)}]"
        )
    }


}