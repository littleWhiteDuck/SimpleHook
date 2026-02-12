package me.simpleHook.feature.config.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.core.compat.BundleCompat
import me.simpleHook.core.compat.getParcelableExtraCompat
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.HookConfig
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MethodInfo
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.CollectionEntity
import me.simpleHook.databinding.ActivityConfigBinding
import me.simpleHook.databinding.ConfigDialogBinding
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.isContainState
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.extension.showPopupWithCopyMsg
import me.simpleHook.feature.config.ui.adapter.ConfigAdapter
import me.simpleHook.core.ui.custom.LoadingDialog
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.custom.exitDialog
import me.simpleHook.core.ui.listener.AppBarStateChangeListener
import me.simpleHook.feature.config.ui.view.InputCollectionView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.HookModeUtil
import me.simpleHook.core.utils.JsonUtil
import me.simpleHook.core.utils.SPUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.applist.ui.AppListActivity
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.feature.config.viewmodel.CollectionViewModel
import me.simpleHook.feature.dexbrowser.ui.DexBrowserActivity
import java.lang.reflect.Field
import java.util.regex.Pattern
import java.util.regex.Pattern.matches


class ConfigActivity : BaseActivity() {

    private val smaliPattern = """^L.*;"""
    private var configList = ArrayList<HookConfig>()
    private var hookMode = Constant.HOOK_RETURN
    private var modify = false
    private var modifyConfig = false
    private var modifyConfigPosition = 0
    private var configId = 0
    private var visibleFab = true
    private lateinit var binding: ActivityConfigBinding
    private var appConfig: AppConfig? = null
    private var tempPackageName = ""
    private val sp by lazy { SPUtil(this) }
    private val appConfigViewModel by viewModels<AppConfigViewModel>()
    private val mAdapter by lazy {
        ConfigAdapter(
            { position -> onClick(position) },
            { position, menu -> onItemCreateContextMenu(position, menu) },
            { position, isChecked -> onCheckedChange(position, isChecked) })
    }
    private val packageInfo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val appName = it.data?.getStringExtra("appName")!!
                val versionName = it.data?.getStringExtra("versionName")!!
                val packageName = it.data?.getStringExtra("packageName")!!
                refreshAppInfo(AppInfo(appName, packageName, versionName))
            }
        }

    private val dexBrowserLaunch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                parseDexBrowserCallback(it.data!!)
            }
        }


    private lateinit var tempConfigStr: String
    private var tempVersionName: String = ""
    private var longClickPosition = 0
    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val collectionViewModel by viewModels<CollectionViewModel>()
    private val json by lazy {
        Json {
            prettyPrint = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val bundle = intent.getBundleExtra("bundle")
        appConfig = bundle?.let { BundleCompat.getParcelable(it, "appConfig") }
        tempPackageName = appConfig?.packageName ?: ""
        initView()
        initBack()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        with(binding) {
            configRV.adapter = mAdapter
            configRV.layoutManager = LinearLayoutManager(this@ConfigActivity)

            appInfo.setOnClickListener {
                packageInfo.launch(Intent(this@ConfigActivity, AppListActivity::class.java))
            }
            addMethodConfig.setOnClickListener {
                modifyConfig = false
                if (sp.bottomConfigDialog) {
                    ConfigBottomFragment(saveConfig = {
                        addConfig(it)
                    }, deleteConfig = {

                    }, hookConfig = HookConfig()).show(supportFragmentManager, "ADD")
                } else {
                    showDialog()
                }
            }
            addMethodConfig.setOnLongClickListener {
                visibleFab = false
                addMethodConfig.hide()
                true
            }
            toolbar.setOnClickListener {
                visibleFab = true
                addMethodConfig.show()
            }
            appBar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
                override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                    if (state == State.EXPANDED) {
                        binding.addMethodConfig.show()
                    } else if (state == State.COLLAPSED) {
                        binding.addMethodConfig.hide()
                    }
                    collapsing.isTitleEnabled = state == State.COLLAPSED
                }
            })
        }
        appConfig?.let {
            modify = true
            configId = it.id
            with(binding) {
                if (it.appName.isEmpty() || it.packageName.isEmpty()) {
                    appInfo.containerView.appName.text = getString(R.string.config_no_app_info)
                    appInfo.containerView.icon.setImageDrawable(
                        AppUtil.getIcon(
                            it.packageName
                        )
                    )
                } else {
                    refreshAppInfo(AppInfo(it.appName, it.packageName, it.versionName))
                }
                descStringEdit.setText(it.description)
                configList = Json.decodeFromString(it.configs)
                mAdapter.submitList(configList)
                mAdapter.notifyDataSetChanged()

                collapsing.title = it.appName
            }
        } ?: run {
            with(binding.appInfo.containerView) {
                appName.text = getString(R.string.config_no_app_info)
                Glide.with(icon).load(BuildConfig.APPLICATION_ID).into(icon)
            }
        }
        showIntroductionDialog()
        tempConfigStr = getAppConfig().copy(enable = true).toString()
        val layoutParams = binding.addMethodConfig.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture = navigationInsets.bottom <= 20 * resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(binding.root, windowInsets)
            var paddingBottom = 0
            if (navigationInsets.bottom == 0) paddingBottom += 10.dp
            layoutParams.bottomMargin = paddingBottom + navigationInsets.bottom + 5.dp
            paddingBottom = if (isGesture) {
                paddingBottom + navigationInsets.bottom
            } else {
                paddingBottom + navigationInsets.bottom
            }
            binding.configRV.setPadding(0, 0, 0, paddingBottom)
            binding.addMethodConfig.layoutParams = layoutParams
            windowInsets
        }
    }

    private fun refreshAppInfo(appInfo: AppInfo) {
        with(binding.appInfo.containerView) {
            appName.text = appInfo.appName
            packageName.text = appInfo.packageName
            otherInfo.text = getString(
                R.string.config_version_support,
                AppUtil.getAppVersionName(appInfo.packageName),
                appInfo.versionName
            )
            icon.setImageDrawable(AppUtil.getIcon(appInfo.packageName))
        }
        tempVersionName = appInfo.versionName
    }

    private fun showIntroductionDialog() {
        if (sp.readIntroduction) {
            customDialog(
                title = getString(R.string.read_introduction_title),
                message = getString(R.string.read_introduction_message),
                okText = getString(R.string.record_introduction_ok),
                okClick = {
                    val intent = Intent(ACTION_VIEW).also {
                        it.data = "https://github.com/littleWhiteDuck/SimpleHook".toUri()
                    }
                    startActivity(intent)
                },
                context = this,
                cancelText = getString(R.string.read_introduction_not_remind),
                cancelClick = {
                    sp.readIntroduction = false
                    it.dismiss()
                },
                neutralText = getString(R.string.dialog_cancel)
            ).show()
        }
    }


    private fun onItemCreateContextMenu(position: Int, menu: ContextMenu) {
        menuInflater.inflate(R.menu.menu_config_item, menu)
        longClickPosition = position
        menu.setHeaderTitle(HookModeUtil.getShowText(configList[position].mode, this))
    }

    private fun onClick(position: Int) {
        modifyConfigPosition = position
        val methodConfig = configList[position]
        modifyConfig = true
        if (sp.bottomConfigDialog) {
            ConfigBottomFragment(methodConfig, saveConfig = {
                addConfig(it)
            }, deleteConfig = {
                addRemoveItem(methodConfig, isAdd = false)
            }).show(supportFragmentManager, "config")
        } else {
            showDialog(methodConfig)
        }
    }

    private fun onCheckedChange(position: Int, checked: Boolean) {
        configList[position] = configList[position].copy(enable = checked)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val methodConfig = configList[longClickPosition]
        when (item.itemId) {
            R.id.menu_collect -> {
                showCollectConfigConfirmDialog(
                    CollectionEntity(
                        name = "",
                        config = json.encodeToString(methodConfig)
                    )
                )
            }

            R.id.menu_copy -> {
                showPopupWithCopyMsg(
                    title = getString(R.string.main_home_export_configs_tip),
                    message = Json.encodeToString(methodConfig)
                )
            }

            R.id.menu_duplicate -> {
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

            R.id.menu_delete_same_type -> {
                val size = configList.size
                for (i in 0 until configList.size) {
                    if (configList[i + configList.size - size].mode == methodConfig.mode) {
                        configList.removeAt(i + configList.size - size)
                    }
                }
                mAdapter.notifyDataSetChanged()
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun showCollectConfigConfirmDialog(collectionEntity: CollectionEntity) {
        val inputCollectionView = InputCollectionView(this).apply {
            configEditText.setText(collectionEntity.config)
            nameEditText.setText(collectionEntity.name)
            configEditText.setOnFocusChangeListener { _, hasFocus ->
                insertEnviVar.isEnabled = hasFocus
            }
        }
        val scrollView = NestedScrollView(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(inputCollectionView)
        }
        customDialog(
            this,
            title = getString(R.string.config_collection_edit_collection),
            contentView = scrollView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                val name = inputCollectionView.nameEditText.text.toString()
                val config: String? = runCatching {
                    val hookConfig =
                        Json.decodeFromString<HookConfig>(inputCollectionView.configEditText.text.toString())
                    Json.encodeToString(hookConfig)
                }.getOrNull()
                config?.let {
                    collectionViewModel.insertCollections(
                        collectionEntity.copy(
                            name = name,
                            config = it
                        )
                    )
                    showPopup(message = getString(R.string.config_collect_success_tip))
                } ?: showPopup(message = getString(R.string.config_collection_illegal_format))

            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    private fun showDialog(
        hookConfig: HookConfig = HookConfig(), isSmali2Config: Boolean = false
    ) {
        val dialogBinding = ConfigDialogBinding.inflate(layoutInflater, null, false)
        with(dialogBinding) {
            with(hookConfig) {
                classNameEdit.setText(className)
                methodNameEdit.setText(methodName)
                paramsTypeEdit.setText(params)
                fieldNameEdit.setText(fieldName)
                fieldClassNameEdit.setText(fieldClassName)
                resultValueEdit.setText(resultValues)
                hookPointEdit.setText(hookPoint)
                returnClassNameEdit.setText(returnClassName)
                hookMode = mode
                onModeChange(dialogBinding)
            }
        }
        val list = resources.getStringArray(R.array.config_hook_mode_item)
        val listValue = resources.getIntArray(R.array.config_hook_mode_item_value)
        val realPosition = listValue.indexOf(hookConfig.mode)
        dialogBinding.modeSelectSpinner.adapter =
            ArrayAdapter(this@ConfigActivity, android.R.layout.simple_spinner_dropdown_item, list)
        dialogBinding.modeSelectSpinner.setSelection(realPosition)
        dialogBinding.modeSelectSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    hookMode = listValue[position]
                    onModeChange(dialogBinding)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        modifyConfig =
            if (isSmali2Config) false else hookConfig.className.isNotEmpty() || (hookConfig.className.isEmpty() && hookConfig.mode == Constant.HOOK_STATIC_FIELD || hookConfig.mode == Constant.HOOK_RECORD_STATIC_FIELD)
        val okText =
            if (modifyConfig) getString(R.string.config_dialog_alter_this) else getString(R.string.config_dialog_add_a_new)
        val neutralText = if (modifyConfig) getString(R.string.config_dialog_delete_this) else ""
        customDialog(this, okText = okText, okClick = { dialog ->
            dialogDismiss(dialog, toCheck(dialogBinding, hookMode, hookConfig.enable))
        }, cancelText = getString(R.string.config_dialog_cancel), cancelClick = { dialogInterface ->
            dialogDismiss(dialogInterface, true)
        }, neutralText = neutralText, neutralClick = { dialogInterface ->
            deleteConfig(hookConfig)
            dialogDismiss(dialogInterface, true)
        }, cancelAble = false, contentView = dialogBinding.root).show()
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
        with(dialogBinding) {
            showView(
                checkStateMode isContainState METHOD_NAME_STATE,
                methodNameInput,
                methodNameEdit
            )
            showView(checkStateMode isContainState PARAMS_STATE, paramsTypeInput, paramsTypeEdit)
            showView(checkStateMode isContainState FIELD_NAME_STATE, fieldNameInput, fieldNameEdit)
            showView(
                checkStateMode isContainState FIELD_CLASS_NAME_STATE,
                fieldClassNameInput,
                fieldClassNameEdit
            )
            showView(
                checkStateMode isContainState RESULT_VALUE_STATE,
                resultValueInput,
                resultValueEdit
            )
            showView(checkStateMode isContainState HOOK_POINT_STATE, hookPointInput, hookPointEdit)
            showView(
                checkStateMode isContainState RETURN_CLASS_NAME,
                returnClassNameInput,
                returnClassNameEdit
            )
        }
    }

    private fun showView(isShow: Boolean, input: TextInputLayout, edit: TextInputEditText) {
        input.visibility = if (isShow) View.VISIBLE else View.GONE
        if (!isShow) edit.setText("")
    }

    private fun toCheck(
        dialogBinding: ConfigDialogBinding, hookMode: Int, enable: Boolean
    ): Boolean {
        val className = this.smali2Java(dialogBinding.classNameEdit.text.toString().trim())
        val methodName = dialogBinding.methodNameEdit.text.toString().trim()
        val params = tranParams(dialogBinding.paramsTypeEdit.text.toString().trim())
        val results = dialogBinding.resultValueEdit.text.toString().trim()
        val fieldName = dialogBinding.fieldNameEdit.text.toString().trim()
        val fieldClassName = tranParams(dialogBinding.fieldClassNameEdit.text.toString())
        val hookPoint = dialogBinding.hookPointEdit.text.toString().trim().let {
            if (className.isEmpty() && methodName.isEmpty() && params.isEmpty()) {
                ""
            } else {
                if (it == "before") it else "after"
            }
        }
        val returnClassName =
            this.smali2Java(dialogBinding.returnClassNameEdit.text.toString().trim())
        var stateCheck = getCheckStateMode(this.hookMode)
        if (className.isNotEmpty()) stateCheck = stateCheck and CLASS_NAME_STATE.inv()
        if (methodName.isNotEmpty()) stateCheck = stateCheck and METHOD_NAME_STATE.inv()
        if (params.isNotEmpty()) stateCheck = stateCheck and PARAMS_STATE.inv()
        if (results.isNotEmpty()) stateCheck = stateCheck and RESULT_VALUE_STATE.inv()
        if (fieldName.isNotEmpty()) stateCheck = stateCheck and FIELD_NAME_STATE.inv()
        if (fieldClassName.isNotEmpty()) stateCheck = stateCheck and FIELD_CLASS_NAME_STATE.inv()
        if (hookPoint.isNotEmpty()) stateCheck = stateCheck and HOOK_POINT_STATE.inv()
        if (returnClassName.isNotEmpty()) stateCheck = stateCheck and RETURN_CLASS_NAME.inv()
        val canCancel = stateCheck == 0
        if (canCancel) {
            if (methodName == "<init>" && (hookMode == Constant.HOOK_RETURN || hookMode == Constant.HOOK_BREAK)) {
                showPopup(getString(R.string.config_hook_constructor_tip))
            }
            val hookConfig = HookConfig(
                this.hookMode,
                className,
                methodName,
                params,
                fieldName,
                fieldClassName,
                results,
                hookPoint = hookPoint,
                returnClassName = returnClassName,
                enable = enable
            )
            addConfig(hookConfig)
        } else {
            showPopup(getString(R.string.config_info_not_match_mode))
        }
        return canCancel
    }

    private fun getCheckStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN -> HOOK_RETURN_CHECK
        Constant.HOOK_PARAM -> HOOK_PARAM_CHECK
        Constant.HOOK_BREAK -> HOOK_BREAK_CHECK
        Constant.HOOK_FIELD -> HOOK_FIELD_CHECK
        Constant.HOOK_RECORD_INSTANCE_FIELD -> HOOK_RECORD_FIELD_CHECK
        Constant.HOOK_STATIC_FIELD -> HOOK_STATIC_FIELD_CHECK
        Constant.HOOK_RECORD_STATIC_FIELD -> HOOK_RECORD_STATIC_FIELD_CHECK
        Constant.HOOK_RECORD_RETURN -> RECORD_RETURN_CHECK
        Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_PARAMS_RETURN -> RECORD_PARAMS_CHECK
        Constant.HOOK_RETURN2 -> HOOK_RETURN2_CHECK
        else -> 0
    }


    private fun getShowStateMode(mode: Int) = when (mode) {
        Constant.HOOK_RETURN, Constant.HOOK_PARAM -> SHOW_RETURN_PARAMS
        Constant.HOOK_FIELD -> SHOW_FIELD
        Constant.HOOK_STATIC_FIELD -> SHOW_STATIC_FIELD
        Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS, Constant.HOOK_BREAK, Constant.HOOK_RECORD_PARAMS_RETURN -> SHOW_RECORD_RETURN_PARAMS_BREAK
        Constant.HOOK_RECORD_STATIC_FIELD -> SHOW_RECORD_STATIC_FIELD
        Constant.HOOK_RECORD_INSTANCE_FIELD -> SHOW_RECORD_INSTANCE_FIELD
        Constant.HOOK_RETURN2 -> SHOW_RETURN2
        else -> 0
    }

    private fun smali2Java(strSmali: String) = if (matches(smaliPattern, strSmali)) {
        strSmali.replaceFirst("L", "").replace("/", ".").replace(";", "")
    } else {
        strSmali
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun addConfig(hookConfig: HookConfig) {
        if (modifyConfig) {
            configList[modifyConfigPosition] = hookConfig
            mAdapter.submitList(configList)
            mAdapter.notifyDataSetChanged()
        } else {
            addRemoveItem(hookConfig)
        }
    }

    private fun deleteConfig(hookConfig: HookConfig) {
        addRemoveItem(hookConfig, false)
        if (!visibleFab) binding.addMethodConfig.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addRemoveItem(hookConfig: HookConfig, isAdd: Boolean = true) {
        configList.apply {
            if (isAdd) add(hookConfig) else remove(hookConfig)
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
            R.id.config_smali_to_config -> {
                ToolUtil.getClipboardContent(this)?.let { patternStr(it.trim()) }
            }

            R.id.collect -> {
                showCollectConfigDialog()
            }

            R.id.menu_dex_browser -> {
                dexBrowserLaunch.launch(Intent(this, DexBrowserActivity::class.java))
            }
        }
        return true
    }

    private fun showCollectConfigDialog() {
        CollectionViewFragment {
            val config: HookConfig? = runCatching {
                Json.decodeFromString<HookConfig>(it.config)
            }.getOrNull()
            config?.let { config ->
                if (sp.bottomConfigDialog) {
                    ConfigBottomFragment(saveConfig = { save ->
                        modifyConfig = false
                        addConfig(save)
                    }, deleteConfig = {

                    }, hookConfig = config).show(supportFragmentManager, "ADD")
                } else {
                    showDialog(config, isSmali2Config = true)
                }
            } ?: showPopup(getString(R.string.config_collection_illegal_format))
        }.show(supportFragmentManager, "collect")
    }

    @SuppressLint("Range")
    private fun saveConfig(exit: Boolean = true) {
        if (configList.isEmpty()) {
            showPopup(getString(R.string.config_save_empty_config_tip))
            return
        }
        if (binding.appInfo.containerView.packageName.text.toString().isEmpty()) {
            showPopup(getString(R.string.config_app_info_is_empty_tip))
            return
        }
        val loadingDialog = LoadingDialog(this, getString(R.string.main_loading))
        loadingDialog.show()
        lifecycleScope.launch(Dispatchers.Main) {
            val appConfig = getAppConfig()
            if (modify) {
                appConfigViewModel.updateConfigs(appConfig)
            } else {
                appConfigViewModel.insertConfigs(appConfig)
            }
            val configStr = Json.encodeToString(appConfig)
            saveConfig(appConfig.packageName, configStr)
            if (tempPackageName.isNotEmpty() && tempPackageName != appConfig.packageName) {
                configSystem.deleteCustomConfig(tempPackageName)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            loadingDialog.dismiss()
            tempConfigStr = getAppConfig().copy(enable = true).toString()
            if (exit) {
                finish()
            }
        }, 800)
    }

    private fun saveConfig(packageName: String, configStr: String) {
        configSystem.saveCustomConfig(packageName, configStr)
    }

    private fun initBack() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (configList.isEmpty()) {
                    onBackPressedCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else if (tempConfigStr != getAppConfig().copy(enable = true).toString()) {
                    exitDialog(
                        this@ConfigActivity,
                        okClick = { saveConfig(exit = true) },
                        neutralClick = {
                            onBackPressedCallback.isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        },
                        cancelClick = {
                            saveConfig(false)
                        })
                } else {
                    onBackPressedCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun getAppConfig(): AppConfig {
        val appName = binding.appInfo.containerView.appName.text.toString()
        val packageName = binding.appInfo.containerView.packageName.text.toString()
        val description = binding.descStringEdit.text.toString()
        val configs = Json.encodeToString(configList)
        return AppConfig(
            appName = appName,
            packageName = packageName,
            description = description,
            versionName = tempVersionName,
            configs = configs,
            id = configId,
            enable = configSystem.isEnableSave(packageName)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun patternStr(string: String) {
        var hookConfig: HookConfig? = null
        val config: HookConfig? = when {
            matches(PATTERN_METHOD, string) -> {
                val matcher = Pattern.compile(PATTERN_METHOD).matcher(string)
                if (matcher.find()) {
                    val className = smali2Java(matcher.group(2)!!)
                    val methodName = matcher.group(3)!!
                    val params = matcher.group(4)!!
                    val returnType = matcher.group(5)!!

                    if (getMode(returnType, params) == Constant.HOOK_RETURN) {
                        hookConfig = HookConfig(
                            Constant.HOOK_RETURN,
                            className,
                            methodName,
                            tranParams(params),
                            "",
                            "",
                            getReturnValue(returnType)
                        )

                    } else {
                        hookConfig = HookConfig(
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
                hookConfig
            }

            matches(PATTERN_FIELD, string) -> {
                val matcher = Pattern.compile(PATTERN_FIELD).matcher(string)
                if (matcher.find()) {
                    val className = smali2Java(matcher.group(2)!!)
                    val fieldName = matcher.group(3)!!
                    val fieldType = matcher.group(4)!!
                    val fieldMode =
                        if (string.startsWith("iget") || string.startsWith("iput")) Constant.HOOK_FIELD else Constant.HOOK_STATIC_FIELD
                    hookConfig = if (fieldMode == Constant.HOOK_STATIC_FIELD) {
                        HookConfig(
                            mode = Constant.HOOK_STATIC_FIELD,
                            "",
                            "",
                            "",
                            fieldName = fieldName,
                            fieldClassName = smali2Java(className),
                            resultValues = getReturnValue(fieldType)
                        )
                    } else {
                        HookConfig(
                            fieldMode,
                            className = smali2Java(className),
                            "",
                            "",
                            fieldName = fieldName,
                            "",
                            getReturnValue(fieldType)
                        )
                    }

                }
                hookConfig
            }

            JsonUtil.isJsonObject(string) -> {
                try {
                    hookConfig = Json.decodeFromString<HookConfig>(string)
                } catch (_: java.lang.Exception) {
                    showPopup(getString(R.string.config_tip_error_config_format))
                }
                hookConfig
            }

            else -> null
        }
        config?.also {
            modifyConfig = false
            if (sp.bottomConfigDialog) {
                ConfigBottomFragment(saveConfig = { save ->
                    addConfig(save)
                }, deleteConfig = {

                }, hookConfig = it).show(supportFragmentManager, "ADD")
            } else {
                showDialog(it, isSmali2Config = true)
            }
        } ?: showPopup(getString(R.string.config_smali_to_config_error))
    }


    private fun parseDexBrowserCallback(data: Intent) {
        val className = data.getStringExtra("className")!!
        val methodInfo = data.getParcelableExtraCompat<MethodInfo>("methodInfo")
        val fieldInfo = data.getParcelableExtraCompat<FieldInfo>("fieldInfo")

        val hookConfig = if (methodInfo != null) {
            val params = methodInfo.parameters.map {
                if (it.startsWith("[L")) {
                    it.removePrefix("[L").replace("/", ".").replace(";", "[]")
                } else if (it.startsWith("L")) {
                    it.removePrefix("L").removeSuffix(";").replace("/", ".")
                } else if (it.startsWith("[")) {
                    "${it.removePrefix("[")}[]"
                } else it
            }.fastJoinToString(separator = ",")
            HookConfig(
                className = className,
                methodName = methodInfo.name,
                mode = getMode(
                    methodInfo.returnType,
                    params
                ),
                params = params,
                resultValues = getReturnValue(methodInfo.returnType),
                desc = "from dex browser"
            )
        } else {
            if (fieldInfo!!.isStatic) {
                HookConfig(
                    fieldClassName = className,
                    fieldName = fieldInfo.name,
                    mode = Constant.HOOK_STATIC_FIELD,
                    resultValues = getReturnValue(fieldInfo.type),
                    desc = "from dex browser"
                )
            } else {
                HookConfig(
                    className = className,
                    fieldName = fieldInfo.name,
                    mode = Constant.HOOK_FIELD,
                    resultValues = getReturnValue(fieldInfo.type),
                    desc = "from dex browser"
                )
            }

        }

        modifyConfig = false
        if (sp.bottomConfigDialog) {
            ConfigBottomFragment(saveConfig = { save ->
                addConfig(save)
            }, deleteConfig = {

            }, hookConfig = hookConfig).show(supportFragmentManager, "ADD")
        } else {
            showDialog(hookConfig, isSmali2Config = true)
        }
    }

    private fun tranParam(param: String): String {
        var temp = param
        temp = temp.replace(Regex(PATTERN_OBJECT_ARRAY), "$1[]")
        if (temp.startsWith("L")) {
            temp = temp.replaceFirst("L", "")
        }
        return temp.replace("/", ".")
    }

    private fun tranParams(params: String): String {
        val isSmali = params.contains(Regex("[/;]")) || isPrimitiveType(params)
        if (!isSmali || params.isEmpty()) return params
        var paramStr = params
        val json = "<ON>"
        if (params.contains("JSON")) {
            paramStr = paramStr.replace("JSON", json)
        }
        paramStr = paramStr.replace("[]", "闃叉鍔犻€楀彿")
        paramStr = paramStr.replace("[", ",[")
        paramStr = paramStr.replace("闃叉鍔犻€楀彿", "[]")
        paramStr = paramStr.replace("VERSION", "闃叉鍔犻€楀彿")
        while (paramStr.contains(Regex(PATTERN_BASIC))) {
            paramStr = paramStr.replace(Regex(PATTERN_BASIC), "$1,$2")
        }
        paramStr = paramStr.replace("闃叉鍔犻€楀彿", "VERSION")
        paramStr = paramStr.replace(Regex(PATTERN_BASIC_ARRAY), "[$1,")
        val paramArray = paramStr.split(Regex("[,;]"))
        val sb = StringBuilder()
        for (i in paramArray.indices) {
            if (paramArray[i].trim().isEmpty()) continue
            sb.append(tranParam(paramArray[i])).append(",")
        }
        var temp = sb.toString()
        if (params.contains("JSON")) {
            temp = temp.replace(json, "JSON")
        }
        if (temp[temp.length - 1] == ',') {
            temp = temp.substring(0, temp.length - 1)
        }
        return temp
    }

    private fun isPrimitiveType(params: String): Boolean {
        var isSmali = true
        var paramStr = params
        paramStr = paramStr.replace("[", ",[")
        while (paramStr.contains(Regex(PATTERN_BASIC))) {
            paramStr = paramStr.replace(Regex(PATTERN_BASIC), "$1,$2")
        }
        paramStr = paramStr.replace(Regex(PATTERN_BASIC_ARRAY), "[$1,")
        val paramArray = paramStr.split(",")
        for (i in paramArray.indices) {
            if (paramArray[i].trim().isEmpty()) continue
            isSmali =
                paramArray[i].contains(Regex("""[BSIJFDZC]""")) || paramArray[i].contains(
                    Regex(
                        PATTERN_BASIC_ARRAY
                    )
                ) || paramArray[i].isEmpty()
        }
        return isSmali
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

    companion object {
        private const val PATTERN_METHOD = """(.*, )?(.*)->(.*)\((.*)\)(.*)"""
        private const val PATTERN_FIELD = """(.*, )?(.*)->(.*):(.*)"""
        private const val PATTERN_BASIC = """([BSIJFDZC])([BSIJFDZCL])"""
        private const val PATTERN_BASIC_ARRAY = """\[([BSIJFDZC])"""
        private const val PATTERN_OBJECT_ARRAY = """\[L(.*)"""
        private const val CLASS_NAME_STATE = 1
        private const val METHOD_NAME_STATE = 1 shl 1
        private const val PARAMS_STATE = 1 shl 2
        private const val RESULT_VALUE_STATE = 1 shl 3
        private const val FIELD_NAME_STATE = 1 shl 4
        private const val FIELD_CLASS_NAME_STATE = 1 shl 5
        private const val HOOK_POINT_STATE = 1 shl 6
        private const val RETURN_CLASS_NAME = 1 shl 7
        private const val HOOK_RETURN_CHECK =
            CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE
        private const val HOOK_RETURN2_CHECK =
            CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or RETURN_CLASS_NAME
        private const val HOOK_PARAM_CHECK =
            CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
        private const val HOOK_BREAK_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
        private const val HOOK_STATIC_FIELD_CHECK =
            FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE
        private const val HOOK_RECORD_STATIC_FIELD_CHECK =
            FIELD_NAME_STATE or FIELD_CLASS_NAME_STATE
        private const val HOOK_FIELD_CHECK =
            CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE
        private const val HOOK_RECORD_FIELD_CHECK =
            CLASS_NAME_STATE or FIELD_NAME_STATE or METHOD_NAME_STATE
        private const val RECORD_RETURN_CHECK = CLASS_NAME_STATE or METHOD_NAME_STATE
        private const val RECORD_PARAMS_CHECK =
            CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
        private const val SHOW_RETURN_PARAMS =
            CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE
        private const val SHOW_RETURN2 =
            CLASS_NAME_STATE or METHOD_NAME_STATE or RESULT_VALUE_STATE or PARAMS_STATE or RETURN_CLASS_NAME
        private const val SHOW_STATIC_FIELD =
            HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
        private const val SHOW_FIELD =
            HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or RESULT_VALUE_STATE or METHOD_NAME_STATE or PARAMS_STATE
        private const val SHOW_RECORD_RETURN_PARAMS_BREAK =
            CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE

        private const val SHOW_RECORD_STATIC_FIELD =
            HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or FIELD_CLASS_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE
        private const val SHOW_RECORD_INSTANCE_FIELD =
            HOOK_POINT_STATE or CLASS_NAME_STATE or FIELD_NAME_STATE or METHOD_NAME_STATE or PARAMS_STATE

    }

}

data class AppInfo(val appName: String, val packageName: String, val versionName: String)
