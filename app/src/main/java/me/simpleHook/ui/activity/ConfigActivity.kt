package me.simpleHook.ui.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.adapter.ConfigAdapter
import me.simpleHook.bean.AppConfigBean
import me.simpleHook.bean.AppItem
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.ActivityConfigBinding
import me.simpleHook.databinding.ConfigDialogBinding
import me.simpleHook.util.*
import java.lang.reflect.Field
import java.util.regex.Pattern.matches

class ConfigActivity : AppCompatActivity() {
    private val smaliPattern = """^L.*;"""
    private var configList = ArrayList<ConfigBean>()
    private var mode = Constant.HOOK_RETURN
    private var appMode = Constant.HOOK_ORIGIN
    private var modify = false
    private var modifyConfig = false
    private var modifyConfigPosition = 0
    private var configId = 0
    private lateinit var binding: ActivityConfigBinding
    private var appConfig: AppConfig? = null
    private val sp by lazy { SPUtils(this) }
    private val configPref by lazy { XUtils(this, "hookConfig").configPref }
    private val appViewModel by viewModels<AppViewModel>()
    private val mAdapter by lazy {
        ConfigAdapter({ position -> onClick(position) },
            { position -> onLongClick(position) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val bundle = intent.getBundleExtra("bundle")
        appConfig = bundle?.getParcelable("appConfig")
        initView()
    }

    private fun initView() {
        binding.apply {
            packageNameInputLayout.isEnabled = false
            addMethodConfig.setOnClickListener { showDialog() }
            val list = arrayListOf("普通模式", "加固模式")
            modeSelectSpinner.adapter =
                ArrayAdapter(
                    this@ConfigActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    list
                )
            modeSelectSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        appMode = position
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
        appConfig?.let {
            modify = true
            configId = it.id
            val appConfigBean = Gson().fromJson(it.config, AppConfigBean::class.java)
            binding.apply {
                appNameEdit.setText(it.appName)
                appVersionNameEdit.setText(it.versionName)
                packageNameEdit.setText(it.packageName)
                appVersionNameEdit.setText(it.versionName)
                descStringEdit.setText(it.description)
                modeSelectSpinner.setSelection(appConfigBean.mode)
                configList = appConfigBean.config
                mAdapter.submitList(configList)
                mAdapter.notifyDataSetChanged()
            }
        }
        binding.configRV.apply {
            this.adapter = mAdapter
            layoutManager = LinearLayoutManager(this@ConfigActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@ConfigActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun onLongClick(position: Int) {
        val methodConfig = configList[position]
        addRemoveItem(methodConfig)
        Snackbar.make(binding.addMethodConfig, "已经重复新增了一个，快去更改", Snackbar.LENGTH_LONG)
            .setAction("撤销") {
                configList.removeAt(configList.size - 1)
                mAdapter.submitList(configList)
                mAdapter.notifyDataSetChanged()
            }.show()
    }

    private fun onClick(position: Int) {
        modifyConfigPosition = position
        val methodConfig = configList[position]
        showDialog(methodConfig)
    }

    private fun showDialog(configBean: ConfigBean = ConfigBean(0, "", "", "", "", "", "")) {
        val dialogBinding = ConfigDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.apply {
            configBean.apply {
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
        MaterialAlertDialogBuilder(this).apply {
            setView(dialogBinding.root)
            val list = arrayListOf("Hook返回值", "Hook参数值", "中断执行", "Hook静态变量", "Hook变量")
            dialogBinding.modeSelectSpinner.adapter =
                ArrayAdapter(
                    this@ConfigActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    list
                )
            dialogBinding.modeSelectSpinner.setSelection(configBean.mode)
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
            modifyConfig = configBean.className.isNotEmpty().also {
                val positiveButtonText = if (it) "修改" else "增加"
                setPositiveButton(positiveButtonText) { d, _ ->
                    dialogDismiss(
                        d, toCheck(dialogBinding)
                    )
                }
                if (it) {
                    setNeutralButton("删除") { d, _ ->
                        deleteConfig(configBean)
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

    private fun fieldHook(dialogBinding: ConfigDialogBinding) {
        dialogBinding.apply {
            filedNameInput.hint = "输入变量名"
            setViewShow(paramsTypeInput)
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


    private fun toCheck(dialogBinding: ConfigDialogBinding): Boolean {
        var canCancel = true
        val className = smaliToJava(dialogBinding.classNameEdit.text.toString().trim())
        val methodName = dialogBinding.methodNameEdit.text.toString().trim()
        val params = smaliToJava(dialogBinding.paramsEdit.text.toString().trim())
        val results = dialogBinding.resultValueEdit.text.toString().trim()
        val fieldName = dialogBinding.filedNameEdit.text.toString()
        val fieldType = smaliToJava(dialogBinding.fieldTypeEdit.text.toString())
        when (mode) {
            Constant.HOOK_BREAK -> {
                if (className.isNotEmpty() && methodName.isNotEmpty()) {
                    val methodConfig = ConfigBean(mode, className, methodName, params)
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(this)
                    canCancel = false
                }
            }
            Constant.HOOK_STATIC_FIELD -> {
                if (className.isNotEmpty() && fieldName.isNotEmpty() && fieldType.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig = ConfigBean(
                        mode,
                        className,
                        fieldName = fieldName,
                        fieldType = fieldType,
                        resultValues = results
                    )
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(this)
                    canCancel = false
                }
            }
            Constant.HOOK_FIELD -> {
                if (className.isNotEmpty() && fieldName.isNotEmpty() && fieldType.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig = ConfigBean(
                        mode,
                        className,
                        params = params,
                        fieldName = fieldName,
                        fieldType = fieldType,
                        resultValues = results
                    )
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(this)
                    canCancel = false
                }
            }
            else -> {
                if (className.isNotEmpty() && methodName.isNotEmpty() && results.isNotEmpty()) {
                    val methodConfig =
                        ConfigBean(mode, className, methodName, params, resultValues = results)
                    addConfig(methodConfig)
                } else {
                    "有必填项为空".toast(this)
                    canCancel = false
                }
            }
        }
        return canCancel
    }

    private fun smaliToJava(strSmali: String) = if (matches(smaliPattern, strSmali)) {
        strSmali.replace("L", "").replace("/", ".").replace(";", "")
    } else {
        strSmali
    }

    private fun showHelpDialog() {
        val webView = WebView(this)
        webView.loadUrl("file:///android_asset/introduce.html")
        MaterialAlertDialogBuilder(this).apply {
            setTitle("帮助")
            setView(webView)
            setPositiveButton("取消", null)
                .create().show()

        }
    }

    private fun addConfig(configBean: ConfigBean) {
        if (modifyConfig) {
            configList[modifyConfigPosition] = configBean
            mAdapter.submitList(configList)
            mAdapter.notifyDataSetChanged()
        } else {
            addRemoveItem(configBean)
        }
    }

    private fun deleteConfig(configBean: ConfigBean) {
        addRemoveItem(configBean, false)
    }

    private fun addRemoveItem(configBean: ConfigBean, isAdd: Boolean = true) {
        configList.apply {
            if (isAdd) add(configBean) else remove(configBean)
        }
        mAdapter.submitList(configList)
        mAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
            R.id.select_app -> {
                val intent = Intent(this, AppListActivity::class.java)
                startActivityForResult(intent, 1)
            }
        }
        return true
    }

    private fun saveConfig() {
        if (configList.size == 0) {
            "配置为空".snack(binding.addMethodConfig)
            return
        }
        val appConfig = Gson().toJson(getAppConfig())
        binding.apply {
            val appName = binding.appNameEdit.text.toString()
            val packageName = binding.packageNameEdit.text.toString()
            val description = binding.descStringEdit.text.toString()
            val versionName = binding.appVersionNameEdit.text.toString()
            AppConfig(
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

                if (sp.openStorage)
                    FileUtils.writeData(
                        "${Constant.CONFIG_DIRECTORY + packageName}/",
                        "config",
                        appConfig
                    )
                if (sp.openXml) {
                    configPref?.edit()?.putString(packageName, appConfig)?.commit()
                        ?: "模块未激活，将将无法使用New XSharedPreferences获取配置".toast(this@ConfigActivity, 1)
                }
            }
        }
        fakeWaitForSave()
    }

    private fun fakeWaitForSave() {
        binding.progressBar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
            finish()
        }, 1500)
    }


    private fun getAppConfig(): AppConfigBean {
        val appName = binding.appNameEdit.text.toString()
        val packageName = binding.packageNameEdit.text.toString()
        val description = binding.descStringEdit.text.toString()
        val versionName = binding.appVersionNameEdit.text.toString()
        return AppConfigBean(
            appName,
            packageName,
            appMode,
            description,
            versionName,
            configList
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val bundle = data?.getBundleExtra("bundle")
            val appItem: AppItem? = bundle?.getParcelable("appItem")
            appItem?.apply {
                binding.apply {
                    appNameEdit.setText(name)
                    appVersionNameEdit.setText(versionName)
                    packageNameEdit.setText(packageName)
                }
            }
        }
    }

}