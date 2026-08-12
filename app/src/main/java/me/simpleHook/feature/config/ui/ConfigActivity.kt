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
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.core.compat.BundleCompat
import me.simpleHook.core.compat.getParcelableExtraCompat
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.data.config.ConfigRemoteSyncHelper
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.HookConfig
import me.simpleHook.data.FieldInfo
import me.simpleHook.data.MemberInfo
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
import me.simpleHook.core.utils.SmaliSignatureParser
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.applist.ui.AppListActivity
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.feature.config.viewmodel.CollectionViewModel
import me.simpleHook.feature.dexbrowser.ui.DexBrowserActivity
import java.lang.reflect.Field


class ConfigActivity : BaseActivity() {

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
            binding.configRV.setPadding(0, 0, 0, paddingBottom + 64.dp)
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
        sp.ensureConfigItemDescVisibleByDefault()
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
                configItemDescEdit.setText(desc)
                configItemDescInput.isVisible = sp.config_item_show_desc
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
        customDialog(this, okText = okText, okClick = { dialogInterface ->
            dialogDismiss(dialogInterface, toCheck(dialogBinding, hookMode, hookConfig.enable))
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
        val checkStateMode = ConfigModeState.showState(hookMode)
        with(dialogBinding) {
            showView(
                checkStateMode isContainState ConfigModeState.METHOD_NAME,
                methodNameInput,
                methodNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.PARAMS,
                paramsTypeInput,
                paramsTypeEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.FIELD_NAME,
                fieldNameInput,
                fieldNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.FIELD_CLASS_NAME,
                fieldClassNameInput,
                fieldClassNameEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.RESULT_VALUE,
                resultValueInput,
                resultValueEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.HOOK_POINT,
                hookPointInput,
                hookPointEdit
            )
            showView(
                checkStateMode isContainState ConfigModeState.RETURN_CLASS_NAME,
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
        val className =
            SmaliSignatureParser.classDescriptorToJavaOrSelf(dialogBinding.classNameEdit.text.toString().trim())
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
            SmaliSignatureParser.classDescriptorToJavaOrSelf(dialogBinding.returnClassNameEdit.text.toString().trim())
        val configDesc = dialogBinding.configItemDescEdit.text.toString().trim()
        val stateCheck = ConfigModeState.unresolvedState(
            mode = this.hookMode,
            className = className,
            methodName = methodName,
            params = params,
            resultValues = results,
            fieldName = fieldName,
            fieldClassName = fieldClassName,
            hookPoint = hookPoint,
            returnClassName = returnClassName
        )
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
                desc = configDesc,
                enable = enable
            )
            addConfig(hookConfig)
        } else {
            showPopup(getString(R.string.config_info_not_match_mode))
        }
        return canCancel
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
            config?.let(::showConfigEditor)
                ?: showPopup(getString(R.string.config_collection_illegal_format))
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
            val configStr = Json.encodeToString(appConfig)
            if (modify) {
                appConfigViewModel.updateConfigs(appConfig)
            } else {
                appConfigViewModel.insertConfigs(appConfig)
            }
            withContext(Dispatchers.IO) {
                saveConfig(appConfig.packageName, configStr)
                if (tempPackageName.isNotEmpty() && tempPackageName != appConfig.packageName) {
                    ConfigRemoteSyncHelper.deleteCustomConfig(configSystem, tempPackageName)
                }
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

    private suspend fun saveConfig(packageName: String, configStr: String) {
        ConfigRemoteSyncHelper.saveCustomConfig(configSystem, packageName, configStr)
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
        val config = parseClipboardToConfig(string)
        config?.let { showConfigEditor(it) } ?: showPopup(getString(R.string.config_smali_to_config_error))
    }

    private fun parseClipboardToConfig(string: String): HookConfig? {
        if (JsonUtil.isJsonObject(string)) {
            return runCatching {
                Json.decodeFromString<HookConfig>(string)
            }.onFailure {
                showPopup(getString(R.string.config_tip_error_config_format))
            }.getOrNull()
        }

        return when (val info = SmaliSignatureParser.parse(string)) {
            is MemberInfo.MethodInfo -> {
                val params = info.parameters.joinToString(",")
                val mode = getMode(info.returnType, params)
                HookConfig(
                    mode = mode,
                    className = info.className,
                    methodName = info.methodName,
                    params = params,
                    resultValues = if (mode == Constant.HOOK_RETURN) getReturnValue(info.returnType) else ""
                )
            }

            is MemberInfo.FieldInfo -> {
                val normalized = string.trim()
                val isStaticField = info.isStatic || (
                    !normalized.startsWith("iget") &&
                        !normalized.startsWith("iput") &&
                        !normalized.startsWith("sget") &&
                        !normalized.startsWith("sput")
                    )
                val resultValue = getReturnValue(info.fieldType)
                if (isStaticField) {
                    HookConfig(
                        mode = Constant.HOOK_STATIC_FIELD,
                        fieldName = info.fieldName,
                        fieldClassName = info.className,
                        resultValues = resultValue
                    )
                } else {
                    HookConfig(
                        mode = Constant.HOOK_FIELD,
                        className = info.className,
                        fieldName = info.fieldName,
                        resultValues = resultValue
                    )
                }
            }

            null -> null
        }
    }

    private fun showConfigEditor(config: HookConfig) {
        modifyConfig = false
        if (sp.bottomConfigDialog) {
            ConfigBottomFragment(saveConfig = { save ->
                addConfig(save)
            }, deleteConfig = {

            }, hookConfig = config).show(supportFragmentManager, "ADD")
        } else {
            showDialog(config, isSmali2Config = true)
        }
    }


    private fun parseDexBrowserCallback(data: Intent) {
        val className = data.getStringExtra("className")!!
        val methodInfo = data.getParcelableExtraCompat<MethodInfo>("methodInfo")
        val fieldInfo = data.getParcelableExtraCompat<FieldInfo>("fieldInfo")

        val hookConfig = if (methodInfo != null) {
            val params = methodInfo.parameters.joinToString(separator = ",") {
                SmaliSignatureParser.toJavaTypeOrSelf(it)
            }
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

        showConfigEditor(hookConfig)
    }

    private fun tranParams(params: String): String {
        return SmaliSignatureParser.toJavaParametersOrSelf(params)
    }

    private fun getReturnValue(returnType: String): String {
        return when (returnType.trim()) {
            "Z", "boolean" -> "true"
            "F", "float" -> "1f"
            "J", "long" -> "4787107805000l"
            "D", "double" -> "2d"
            "Ljava/lang/String;", "java.lang.String", "String" -> "isVip"
            "S", "short" -> "1short"
            "C", "char" -> "1c"
            "I", "int" -> "1"
            "B", "byte" -> "1"
            else -> "null"
        }
    }

    private fun getMode(returnType: String, params: String): Int {
        val normalizedReturnType = returnType.trim()
        val isVoid = normalizedReturnType == "V" || normalizedReturnType == "void"
        return if (isVoid && params.isNotEmpty()) {
            Constant.HOOK_PARAM
        } else if (isVoid) {
            Constant.HOOK_BREAK
        } else {
            Constant.HOOK_RETURN
        }

    }

}

data class AppInfo(val appName: String, val packageName: String, val versionName: String)
