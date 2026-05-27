package me.simpleHook.core.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnPermissionResult
import com.lzf.easyfloat.permission.PermissionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.data.config.RecordIngestCoordinator
import me.simpleHook.data.config.RecordIngestReason
import me.simpleHook.core.contract.OpenDocumentTreeContract
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.extension.showToast
import me.simpleHook.feature.record.viewmodel.RecordViewModel
import me.simpleHook.feature.record.ui.adapter.FloatRecordAdapter
import me.simpleHook.feature.record.ui.view.ControlView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.JsonUtil
import me.simpleHook.core.utils.LanguageUtil
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.core.utils.PermissionUtil
import me.simpleHook.core.utils.TimeUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel

@Keep
open class BaseActivity : AppCompatActivity() {

    private val appConfigViewModel by viewModels<AppConfigViewModel>()
    private val recordViewModel by viewModels<RecordViewModel>()
    private val mAdapter: FloatRecordAdapter by lazy {
        FloatRecordAdapter { smallRecord ->
            showRecordDetailFromId(smallRecord.id)
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private var readFileJob: Job? = null
    private var floatCollectJob: Job? = null
    private var adapterDataObserver: RecyclerView.AdapterDataObserver? = null
    private var dismissFloat = false
    private val refresh = object : Runnable {
        override fun run() {
            if (dismissFloat) return
            readFileLogInsert(
                reason = RecordIngestReason.FloatRealtime,
                skipIfRunning = true
            )
            handler.postDelayed(this, FLOAT_REFRESH_INTERVAL_MS)
        }
    }
    private var stopPrint = false
    private var floatStartRecordId = 0
    private var floatWindowStartTime = ""
    private var latestFloatRecords = emptyList<SmallRecordEntity>()
    private lateinit var extConfigList: List<ExtensionConfigEntity>
    private lateinit var configs: List<AppConfig>
    private var needCheckPacks = mutableSetOf<String>()
    protected val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            PermissionUtil.takePersistableUriPermission(this, uri)
        }

    @SuppressLint("RestrictedApi")
    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return if (menu is MenuBuilder) {
            try {
                @Suppress("UsePropertyAccessSyntax")
                menu.setOptionalIconsVisible(true)
                super.onMenuOpened(featureId, menu)
            } catch (_: Exception) {
                super.onMenuOpened(featureId, menu)
            }
        } else super.onMenuOpened(featureId, menu)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (DynamicColors.isDynamicColorAvailable() && GlobalValue.sp.enableSystemAccent) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
        super.onCreate(savedInstanceState)
    }


    fun readFileLogInsert(
        reason: RecordIngestReason = RecordIngestReason.Background,
        skipIfRunning: Boolean = true
    ) {
        if (readFileJob?.isActive == true) {
            if (skipIfRunning) return
            readFileJob?.cancel()
        }
        readFileJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                ensureNeedCheckPackages()
                RecordIngestCoordinator.requestIngest(
                    context = this@BaseActivity,
                    packageNames = needCheckPacks,
                    reason = reason,
                    skipIfRunning = skipIfRunning
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.outLog(e.stackTraceToString())
            }
        }
    }

    private suspend fun ensureNeedCheckPackages() {
        if (::extConfigList.isInitialized && ::configs.isInitialized && needCheckPacks.isNotEmpty()) {
            return
        }
        extConfigList = appConfigViewModel.getExtConfigs()
        configs = appConfigViewModel.getConfigs()
        needCheckPacks.clear()
        extConfigList.forEach {
            if (it.enable && AppUtil.isAppInstalled(it.packageName)) {
                needCheckPacks.add(it.packageName)
            }
        }
        configs.forEach {
            if (it.enable && AppUtil.isAppInstalled(it.packageName)) {
                needCheckPacks.add(it.packageName)
            }
        }
    }

    private fun showRecordDetailFromId(recordId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val message = runCatching {
                val recordEntity = recordViewModel.getRecordByID(recordId)
                    ?: error("record not found")
                JsonUtil.formatJson(recordEntity.record.replace("\\u003e", ">"))
            }.getOrDefault("failed to load record detail")
            launch(Dispatchers.Main) {
                showFloatDetailDialog(message)
            }
        }
    }

    private fun showFloatDetailDialog(message: String) {
        val dialog = MaterialAlertDialogBuilder(this).setMessage(message)
            .setPositiveButton(getString(R.string.record_detail_menu_copy)) { dialog, _ ->
                ToolUtil.toClip(this, message)
                showToast(getString(R.string.copied))
                dialog.dismiss()
            }.setNegativeButton(getString(R.string.dialog_cancel), null)
            .create()
        if (Build.VERSION.SDK_INT >= 26) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        dialog.show()
    }

    fun initPrintFloat() {
        if (EasyFloat.getFloatView("floatPrint") != null) return
        if (PermissionUtils.checkPermission(this)) {
            showPrintFloat()
        } else {
            showPopup(message = getString(R.string.float_window_require_permission))
            PermissionUtils.requestPermission(this, object : OnPermissionResult {
                override fun permissionResult(isOpen: Boolean) {
                    if (isOpen) {
                        showPrintFloat()
                    } else {
                        showPopup(message = getString(R.string.float_window_require_permission_failed))
                    }
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showPrintFloat() {
        readFileJob?.cancel()
        floatCollectJob?.cancel()
        dismissFloat = false
        stopPrint = false
        floatWindowStartTime = TimeUtil.getCurrentTime("yy-MM-dd HH:mm:ss")
        latestFloatRecords = emptyList()
        mAdapter.submitList(emptyList())
        EasyFloat.with(this).setLayout(R.layout.window_float) {
            val recyclerView = it.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(this@BaseActivity)
                addItemDecoration(
                    DividerItemDecoration(
                        this@BaseActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
            val dragCheckBox = it.findViewById<CheckBox>(R.id.dragEnable)
            dragCheckBox.setOnCheckedChangeListener { _, isChecked ->
                EasyFloat.dragEnable(isChecked, "floatPrint")
            }
            val miniSize = it.findViewById<ImageButton>(R.id.mini_window)
            miniSize.setOnClickListener {
                if (EasyFloat.getFloatView("floatControl") != null) {
                    EasyFloat.show("floatControl")
                } else {
                    initControlFloat()
                }
                EasyFloat.hide("floatPrint")
            }
            val clearConfig = it.findViewById<ImageButton>(R.id.clear_record)
            clearConfig.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    recordViewModel.deleteFloatRecordsAfterNow(
                        afterId = floatStartRecordId,
                        startTime = floatWindowStartTime
                    )
                    val nextStartRecordId = recordViewModel.getLatestRecordIdNow()
                    val nextStartTime = TimeUtil.getCurrentTime("yy-MM-dd HH:mm:ss")
                    withContext(Dispatchers.Main) {
                        floatStartRecordId = nextStartRecordId
                        floatWindowStartTime = nextStartTime
                        latestFloatRecords = emptyList()
                        mAdapter.submitList(emptyList())
                        if (!dismissFloat) {
                            startFloatRecordCollection()
                        }
                    }
                }
            }
            val closeWindow = it.findViewById<ImageButton>(R.id.close_window)
            closeWindow.setOnClickListener {
                EasyFloat.dismiss("floatPrint")
                EasyFloat.dismiss("floatControl")
                cleanupFloatWindow()
            }
            val pausePrintRecord = it.findViewById<ImageButton>(R.id.pause_print_record)
            pausePrintRecord.setOnClickListener {
                stopPrint = !stopPrint
                val bgId =
                    if (stopPrint) R.drawable.ic_start_float_24 else R.drawable.ic_outline_pause_24
                pausePrintRecord.setImageResource(bgId)
                if (!stopPrint) {
                    mAdapter.submitList(latestFloatRecords)
                }
            }
            adapterDataObserver?.let {
                mAdapter.unregisterAdapterDataObserver(it)
            }
            adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    if (mAdapter.itemCount - 1 > 0) {
                        recyclerView.smoothScrollToPosition(mAdapter.itemCount - 1)
                    }
                }
            }
            mAdapter.registerAdapterDataObserver(adapterDataObserver!!)
            lifecycleScope.launch {
                floatStartRecordId = withContext(Dispatchers.IO) {
                    recordViewModel.getLatestRecordIdNow()
                }
                if (dismissFloat) return@launch
                startFloatRecordCollection()
                readFileLogInsert(
                    reason = RecordIngestReason.FloatRealtime,
                    skipIfRunning = true
                )
                handler.postDelayed(refresh, FLOAT_REFRESH_INTERVAL_MS)
            }
        }.setTag("floatPrint").setShowPattern(ShowPattern.ALL_TIME).setDragEnable(false)
            .setSidePattern(SidePattern.DEFAULT).setLocation(100, 100)
            .setMatchParent(widthMatch = false, heightMatch = false).setAnimator(DefaultAnimator())
            .registerCallback {
                dismiss {
                    cleanupFloatWindow()
                }
            }.show()
    }

    private fun startFloatRecordCollection() {
        floatCollectJob?.cancel()
        floatCollectJob = lifecycleScope.launch {
            recordViewModel.getFloatRecordsAfter(
                afterId = floatStartRecordId,
                startTime = floatWindowStartTime,
                limit = MAX_FLOAT_RECORD_COUNT
            ).collectLatest { records ->
                latestFloatRecords = records
                if (!stopPrint) {
                    mAdapter.submitList(records)
                }
            }
        }
    }

    private fun cleanupFloatWindow() {
        dismissFloat = true
        handler.removeCallbacks(refresh)
        floatCollectJob?.cancel()
        floatCollectJob = null
        adapterDataObserver?.let {
            mAdapter.unregisterAdapterDataObserver(it)
        }
        adapterDataObserver = null
        latestFloatRecords = emptyList()
        mAdapter.submitList(emptyList())
    }

    companion object {
        private const val MAX_FLOAT_RECORD_COUNT = 300
        private const val FLOAT_REFRESH_INTERVAL_MS = 500L
    }

    private fun initControlFloat() {
        EasyFloat.with(this).setLayout(ControlView(this)) {
            it.setOnClickListener {
                EasyFloat.show("floatPrint")
                EasyFloat.hide("floatControl")
            }
        }.setTag("floatControl").setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL).setDragEnable(true).setLocation(100, 200)
            .setAnimator(DefaultAnimator()).show()
    }
}
