package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.ConfigAdapter
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.ActivityConfigBinding
import me.simpleHook.databinding.ConfigDialogBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.util.*
import java.lang.reflect.Field
import java.util.regex.Pattern
import java.util.regex.Pattern.matches

private const val PATTERN_METHOD = """(.*, )?(.*)->(.*)\((.*)\)(.*)"""
private const val PATTERN_FIELD = """(.*, )?(.*)->(.*):(.*)"""
private const val pattern_basic = """(B|S|I|J|F|D|Z|C)(B|S|I|J|F|D|Z|C|L)"""
private const val pattern_basic_array = """\[(B|S|I|J|F|D|Z|C)"""
private const val pattern_object_array = """\[L(.*)"""
private const val CLASS_NAME_STATE = 1
private const val METHOD_NAME_STATE = 1 shl 1
private const val PARAMS_STATE = 1 shl 2
private const val RESULT_VALUE_STATE = 1 shl 3
private const val FIELD_NAME_STATE = 1 shl 4
private const val FIELD_TYPE_STATE = 1 shl 5
private const val HOOK_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE
private const val HOOK_PARAM_CHECK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val HOOK_BREAK_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val HOOK_FIELD_CHECK = CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE
private const val RECORD_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
private const val RECORD_PARAMS_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
private const val SHOW_RETURN_PARAMS =
    CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
private const val SHOW_FIELD =
    CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_TYPE_STATE
private const val SHOW_RECORD_RETURN_PARAMS_BREAK =
    CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE


class ConfigActivity : BaseActivity() {
    private val smaliPattern = """^L.*;"""
    private var configList = ArrayList<ConfigBean>()
    private var hookMode = Constant.HOOK_RETURN
    private var modify = false
    private var modifyConfig = false
    private var modifyConfigPosition = 0
    private var configId = 0
    private var visibleFab = true
    private lateinit var binding: ActivityConfigBinding
    private var appConfig: AppConfig? = null
    private val sp by lazy { SPUtils(this) }
    private val appViewModel by viewModels<AppViewModel>()
    private val mAdapter by lazy {
        ConfigAdapter({ position -> onClick(position) },
            { position -> onLongClick(position) },
            { position, isChecked -> onCheckedChange(position, isChecked) })
    }
    private val packageInfo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val appName = it.data?.getStringExtra("appName")
                val versionName = it.data?.getStringExtra("versionName")
                val packageName = it.data?.getStringExtra("packageName")
                /* lifecycleScope.launch(Dispatchers.IO) {
                     preprocessCreateFile(appItem.packageName)
                 }*/
                binding.apply {
                    appNameEdit.setText(appName)
                    appVersionNameEdit.setText(versionName)
                    packageNameEdit.setText(packageName)
                }
            }
        }
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            uri?.also {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val bundle = intent.getBundleExtra("bundle")
        appConfig = bundle?.getParcelable("appConfig")
        initView()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        binding.apply {
            packageNameInputLayout.isEnabled = false
            addMethodConfig.setOnClickListener { showDialog() }
            addMethodConfig.setOnLongClickListener {
                visibleFab = false
                addMethodConfig.hide()
                true
            }
            toolbar.setOnClickListener {
                if (!visibleFab) {
                    visibleFab = true
                    addMethodConfig.show()
                }
            }
        }
        appConfig?.let {
            modify = true
            configId = it.id
            binding.apply {
                appNameEdit.setText(it.appName)
                appVersionNameEdit.setText(it.versionName)
                packageNameEdit.setText(it.packageName)
                appVersionNameEdit.setText(it.versionName)
                descStringEdit.setText(it.description)
                val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
                configList = Gson().fromJson(it.configs, listType)
                mAdapter.submitList(configList)
                mAdapter.notifyDataSetChanged()
            }
        }
        binding.configRV.apply {
            this.adapter = mAdapter
            layoutManager = LinearLayoutManager(this@ConfigActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@ConfigActivity, DividerItemDecoration.VERTICAL
                )
            )
        }
        binding.nsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val distance = scrollY - oldScrollY
            if (distance > 20 && visibleFab) {
                visibleFab = false
                binding.addMethodConfig.hide()
            } else if (distance < -20 && !visibleFab) {
                visibleFab = true
                binding.addMethodConfig.show()
            }
        })
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun onLongClick(position: Int) {
        val methodConfig = configList[position]
        addRemoveItem(methodConfig)
        Snackbar.make(
            binding.addMethodConfig,
            getString(R.string.config_add_repeat_config_tip),
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = binding.addMethodConfig
        }.setAction(getString(R.string.config_undo_repeat_config)) {
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

    private fun onCheckedChange(position: Int, checked: Boolean) {
        configList[position] = configList[position].copy(enable = checked)
    }

    private fun showDialog(configBean: ConfigBean = ConfigBean(0, "", "", "", "", "", "")) {
        val dialogBinding = ConfigDialogBinding.inflate(layoutInflater, null, false)
        dialogBinding.apply {
            configBean.apply {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsTypeEdit.setText(params)
                fieldNameEdit.setText(fieldName)
                fieldTypeEdit.setText(fieldType)
                resultValueEdit.setText(resultValues)
                help.setOnClickListener { showHelpDialog() }
                hookMode = mode
                onModeChange(dialogBinding)
            }
        }
        val list = resources.getStringArray(R.array.config_hook_mode_item)
        dialogBinding.modeSelectSpinner.adapter = ArrayAdapter(
            this@ConfigActivity, android.R.layout.simple_spinner_dropdown_item, list
        )
        dialogBinding.modeSelectSpinner.setSelection(configBean.mode)
        dialogBinding.modeSelectSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    hookMode = position
                    onModeChange(dialogBinding)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        modifyConfig = configBean.className.isNotEmpty()
        val okText = if (modifyConfig) getString(R.string.config_dialog_alter_this) else getString(
            R.string.config_dialog_add_a_new
        )
        val neutralText = if (modifyConfig) getString(R.string.config_dialog_delete_this) else ""
        customDialog(this,
            okText = okText,
            okClick = { dialog ->
                dialogDismiss(
                    dialog, toCheck(dialogBinding, hookMode)
                )
            },
            cancelText = getString(R.string.config_dialog_cancel),
            cancelClick = { dialogInterface -> dialogDismiss(dialogInterface, true) },
            neutralText = neutralText,
            neutralClick = { dialogInterface ->
                deleteConfig(configBean)
                dialogDismiss(
                    dialogInterface, true
                )
            },
            cancelAble = false,
            contentView = dialogBinding.root
        ).show()
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

    private fun onModeChange(dialogBinding: ConfigDialogBinding) {
        val checkStateMode = getShowStateMode(hookMode)
        dialogBinding.apply {
            showView(
                checkStateMode isContainState METHOD_NAME_STATE, methodNameInput, methodNameEdit
            )
            showView(checkStateMode isContainState PARAMS_STATE, paramsTypeInput, paramsTypeEdit)
            showView(checkStateMode isContainState FIELD_NAME_STATE, fieldNameInput, fieldNameEdit)
            showView(checkStateMode isContainState FIELD_TYPE_STATE, fieldTypeInput, fieldTypeEdit)
            showView(
                checkStateMode isContainState RESULT_VALUE_STATE, resultValueInput, resultValueEdit
            )
        }
    }

    private fun showView(isShow: Boolean, input: TextInputLayout, edit: TextInputEditText) {
        input.visibility = if (isShow) View.VISIBLE else View.GONE
        if (!isShow) edit.setText("")
    }

    private fun toCheck(dialogBinding: ConfigDialogBinding, hookMode: Int): Boolean {
        val className = this.smali2Java(dialogBinding.classNameEdit.text.toString().trim())
        val methodName = dialogBinding.methodNameEdit.text.toString().trim()
        val params = tranParams(dialogBinding.paramsTypeEdit.text.toString().trim())
        val results = dialogBinding.resultValueEdit.text.toString().trim()
        val fieldName = dialogBinding.fieldNameEdit.text.toString()
        val fieldType = tranParams(dialogBinding.fieldTypeEdit.text.toString())
        var stateCheck = getCheckStateMode(this.hookMode)
        if (className.isNotEmpty()) stateCheck = stateCheck and CLASS_NAME_STATE.inv()
        if (methodName.isNotEmpty()) stateCheck = stateCheck and METHOD_NAME_STATE.inv()
        if (params.isNotEmpty()) stateCheck = stateCheck and PARAMS_STATE.inv()
        if (results.isNotEmpty()) stateCheck = stateCheck and RESULT_VALUE_STATE.inv()
        if (fieldName.isNotEmpty()) stateCheck = stateCheck and FIELD_NAME_STATE.inv()
        if (fieldType.isNotEmpty()) stateCheck = stateCheck and FIELD_TYPE_STATE.inv()
        val canCancel = stateCheck == 0
        if (canCancel) {
            val tempMethodName = if (getClassSimpleName(className) == methodName) {
                if (hookMode == Constant.HOOK_RETURN || hookMode == Constant.HOOK_BREAK || methodName == "<init>") {
                    getString(R.string.config_hook_constructor_tip).toast(this)
                }
                "<init>"
            } else {
                methodName
            }
            val configBean = ConfigBean(
                this.hookMode,
                className,
                tempMethodName,
                params,
                fieldName,
                fieldType,
                results
            )
            addConfig(configBean)
        } else {
            getString(R.string.config_info_not_match_mode).toast(this)
        }
        return canCancel
    }

    private fun getCheckStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN -> HOOK_RETURN_CHECK
        Constant.HOOK_PARAM -> HOOK_PARAM_CHECK
        Constant.HOOK_BREAK -> HOOK_BREAK_CHECK
        Constant.HOOK_FIELD, Constant.HOOK_STATIC_FIELD -> HOOK_FIELD_CHECK
        Constant.HOOK_RECORD_RETURN -> RECORD_RETURN_CHECK
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_PARAMS_RETURN -> RECORD_PARAMS_CHECK
        else -> 0
    }

    private fun getShowStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN, Constant.HOOK_PARAM -> SHOW_RETURN_PARAMS
        Constant.HOOK_FIELD, Constant.HOOK_STATIC_FIELD -> SHOW_FIELD
        Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS, Constant.HOOK_BREAK, Constant.HOOK_RECORD_PARAMS_RETURN -> SHOW_RECORD_RETURN_PARAMS_BREAK
        else -> 0
    }

    private fun smali2Java(strSmali: String) = if (matches(smaliPattern, strSmali)) {
        strSmali.replaceFirst("L", "").replace("/", ".").replace(";", "")
    } else {
        strSmali
    }

    private fun showHelpDialog() {
        val intent = Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse("https://github.com/littleWhiteDuck/SimpleHook")
        }
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
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
        if (!visibleFab) binding.addMethodConfig.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addRemoveItem(configBean: ConfigBean, isAdd: Boolean = true) {
        configList.apply {
            if (isAdd) add(configBean) else remove(configBean)
        }
        mAdapter.submitList(configList)
        mAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_config, menu)
        if (!sp.smali2Config) menu.removeItem(R.id.config_smali_to_config)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_config -> saveConfig()
            R.id.select_app -> packageInfo.launch(Intent(this, AppListActivity::class.java))
            R.id.config_smali_to_config -> {
                ToolUtils.getClipboardContent(this)?.let { patternStr(it.trim()) }
            }
        }
        return true
    }

    private fun saveConfig() {
        if (configList.size == 0) {
            getString(R.string.config_save_empty_config_tip).toast(this)
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val appConfig = getAppConfig()
            if (modify) {
                appViewModel.updateConfigs(appConfig)
            } else {
                appViewModel.insertConfigs(appConfig)
            }
            val configStr = Gson().toJson(appConfig)
            saveToText(appConfig.packageName, configStr)
        }
        fakeWaitForSave()
    }

    private fun saveToText(packageName: String, configStr: String) {
        if (sp.openStorage) {
            if (FlavorUtils.isNormal()) {
                if (FileUtils.isGrant(this)) {
                    FileUtils.saveConfig(
                        this, packageName, Constant.APP_CONFIG_NAME, configStr
                    )
                } else {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        requestPermissionDialog(this) {
                            startActivityForData.launch(Uri.parse(Constant.ANDROID_DATA_URI))
                        }
                    } else {
                        requestPermissionDialog(this) {
                            FileUtils.verifyStoragePermissions(this)
                        }
                    }
                }
            } else {
                FileUtils.saveConfig(
                    this, packageName, Constant.APP_CONFIG_NAME, configStr
                )
            }
        }
    }

    private fun fakeWaitForSave() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
            onBackPressed()
        }, 2000)
    }


    private fun getAppConfig(): AppConfig {
        val appName = binding.appNameEdit.text.toString()
        val packageName = binding.packageNameEdit.text.toString()
        val description = binding.descStringEdit.text.toString()
        val versionName = binding.appVersionNameEdit.text.toString()
        val configs = Gson().toJson(configList)
        return AppConfig(
            appName = appName,
            packageName = packageName,
            description = description,
            versionName = versionName,
            configs = configs,
            id = configId
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun patternStr(string: String) {
        var configBean: ConfigBean? = null
        val config: ConfigBean? = when {
            matches(PATTERN_METHOD, string) -> {
                val matcher = Pattern.compile(PATTERN_METHOD).matcher(string)
                if (matcher.find()) {
                    val className = smali2Java(matcher.group(2)!!)
                    val methodName = matcher.group(3)!!
                    val params = matcher.group(4)!!
                    val returnType = matcher.group(5)!!

                    if (getMode(returnType, params) == Constant.HOOK_RETURN) {
                        configBean = ConfigBean(
                            Constant.HOOK_RETURN,
                            className,
                            methodName,
                            tranParams(params),
                            "",
                            "",
                            getReturnValue(returnType)
                        )

                    } else {
                        configBean = ConfigBean(
                            getMode(returnType, params),
                            smali2Java(className),
                            methodName,
                            tranParams(params),
                            "",
                            "",
                            ""
                        )

                    }

                }
                configBean
            }
            matches(PATTERN_FIELD, string) -> {
                val matcher = Pattern.compile(PATTERN_FIELD).matcher(string)
                if (matcher.find()) {
                    val className = smali2Java(matcher.group(2)!!)
                    val fieldName = matcher.group(3)!!
                    val fieldType = matcher.group(4)!!
                    val fieldMode =
                        if (string.startsWith("iget") || string.startsWith("iput")) Constant.HOOK_FIELD else Constant.HOOK_STATIC_FIELD
                    configBean = ConfigBean(
                        fieldMode,
                        smali2Java(className),
                        "",
                        "",
                        fieldName,
                        smali2Java(fieldType),
                        getReturnValue(fieldType)
                    )
                }
                configBean
            }
            else -> configBean
        }
        config?.also {
            configList.add(it)
            mAdapter.submitList(configList)
            mAdapter.notifyDataSetChanged()
            Snackbar.make(
                binding.addMethodConfig,
                getString(R.string.config_add_config_tip),
                Snackbar.LENGTH_LONG
            ).apply {
                anchorView = binding.addMethodConfig
            }.show()
        } ?: getString(R.string.config_smali_to_config_error).toast(this)
    }

    private fun tranParam(param: String): String {
        var temp = param
        temp = temp.replace(Regex(pattern_object_array), "$1[]")
        if (temp.startsWith("L")) {
            temp = temp.replaceFirst("L", "")
        }
        return temp.replace("/", ".")
    }

    private fun tranParams(params: String): String {
        if (params.isEmpty()) return ""
        var paramStr = params
        while (paramStr.contains(Regex(pattern_basic))) {
            paramStr = paramStr.replace(Regex(pattern_basic), "$1,$2")
        }
        paramStr = paramStr.replace(Regex(pattern_basic_array), "[$1,")
        val paramArray = paramStr.split(Regex("[,;]"))
        val sb = StringBuilder()
        for (i in paramArray.indices) {
            if (paramArray[i].trim().isEmpty()) continue
            sb.append(tranParam(paramArray[i])).append(",")
        }
        var temp = sb.toString()
        if (temp[temp.length - 1] == ',') {
            temp = temp.substring(0, temp.length - 1)
        }
        return temp
    }


    private fun getReturnValue(returnType: String): String {
        return when (returnType) {
            "Z" -> "true"
            "F" -> "1f"
            "J" -> "4787107805000l"
            "D" -> "2d"
            "Ljava/lang/String;" -> "isVip"
            "S" -> "1short"
            "C" -> "1c"
            "I" -> "1"
            "B" -> "1"
            else -> "null"
        }
    }

    private fun getMode(returnType: String, params: String): Int {
        return if (returnType == "V" && params.isNotEmpty()) {
            Constant.HOOK_PARAM
        } else if (returnType == "V") {
            Constant.HOOK_BREAK
        } else {
            Constant.HOOK_RETURN
        }
    }

    private fun getClassSimpleName(classStr: String): String {
        return if (classStr.contains(".")) {
            val classStrNames = classStr.split(".")
            classStrNames[classStrNames.size - 1]
        } else {
            classStr
        }
    }
}