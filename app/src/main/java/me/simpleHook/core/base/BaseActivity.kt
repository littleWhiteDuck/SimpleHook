package me.simpleHook.core.base

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
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
import androidx.core.net.toUri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.data.config.RecordIngestor
import me.simpleHook.core.contract.OpenDocumentTreeContract
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.extension.showToast
import me.simpleHook.feature.record.viewmodel.RecordViewModel
import me.simpleHook.feature.record.ui.adapter.FloatRecordAdapter
import me.simpleHook.feature.record.ui.view.ControlView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.FlavorUtil
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
    private val list = ArrayList<SmallRecordEntity>()
    private val handler = Handler(Looper.getMainLooper())
    private var readFileJob: Job? = null
    private var adapterDataObserver: RecyclerView.AdapterDataObserver? = null
    private var dismissFloat = false
    private var lastRecordId = -1
    private val refresh = object : Runnable {
        override fun run() {
            if (dismissFloat) return
            readFileLogInsert()
            updateData()
            handler.postDelayed(this, 500)
        }
    }
    private val uri = FlavorUtil.PROVIDER_RECORD_URI.toUri()
    private var stopPrint = false
    private var currentTime = ""
    private var startTime = ""
    private var floatWindowStartTime = ""
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


    fun readFileLogInsert() {
        if (readFileJob?.isActive == true) return
        readFileJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                ensureNeedCheckPackages()
                RecordIngestor.ingestRecordsFromPackages(this@BaseActivity, needCheckPacks) { batch ->
                    recordViewModel.insertRecordsNow(batch)
                }

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

    @SuppressLint("Range")
    private fun updateData() {
        if (stopPrint) return
        val newRecords = ArrayList<SmallRecordEntity>()
        queryRecordCursor()?.use { cursor ->
            val idIndex = cursor.getColumnIndex("id")
            val typeIndex = cursor.getColumnIndex("type")
            val subTypeIndex = cursor.getColumnIndex("subType")
            val packageNameIndex = cursor.getColumnIndex("packageName")
            val isReadIndex = cursor.getColumnIndex("isRead")
            val isMarkIndex = cursor.getColumnIndex("isMark")
            val timeIndex = cursor.getColumnIndex("time")
            while (cursor.moveToNext()) {
                val packageName = cursor.getStringOrEmpty(packageNameIndex)
                val time = cursor.getStringOrEmpty(timeIndex)
                if (!isAfterFloatWindowStart(time)) {
                    continue
                }
                val idFromDb = cursor.getIntOrNull(idIndex)
                val type = enumValues<RecordType>().firstOrNull {
                    it.name == cursor.getStringOrEmpty(typeIndex)
                } ?: RecordType.RecordReturn
                val subType = cursor.getStringOrEmpty(subTypeIndex).ifEmpty { type.name }
                val recordId = idFromDb ?: (packageName + time + type.name + subType).hashCode()
                newRecords.add(
                    SmallRecordEntity(
                        id = recordId,
                        packageName = packageName,
                        time = time,
                        type = type,
                        subType = subType,
                        isRead = cursor.getBooleanOrFalse(isReadIndex),
                        isMark = cursor.getBooleanOrFalse(isMarkIndex)
                    )
                )
                if (idFromDb != null && idFromDb > lastRecordId) {
                    lastRecordId = idFromDb
                }
                if (time.isNotEmpty()) {
                    currentTime = time
                }
            }
        }
        if (newRecords.isEmpty()) {
            return
        }
        list.addAll(newRecords)
        if (list.size > MAX_FLOAT_RECORD_COUNT) {
            list.subList(0, list.size - MAX_FLOAT_RECORD_COUNT).clear()
        }
        mAdapter.submitList(list.toList())
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

    private fun queryRecordCursor(): Cursor? {
        if (lastRecordId >= 0) {
            runCatching {
                contentResolver.query(
                    uri,
                    FLOAT_QUERY_PROJECTION,
                    "id > ?",
                    arrayOf(lastRecordId.toString()),
                    "id ASC"
                )
            }.getOrNull()?.let {
                return it
            }
            lastRecordId = -1
        }
        return runCatching {
            contentResolver.query(
                uri,
                FLOAT_QUERY_PROJECTION,
                "time > ?",
                arrayOf(currentTime),
                "time ASC"
            )
        }.getOrNull()
    }

    private fun initLastRecordId() {
        lastRecordId = runCatching {
            contentResolver.query(uri, arrayOf("id"), null, null, "id DESC")?.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            } ?: 0
        }.getOrDefault(-1)
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
        dismissFloat = false
        floatWindowStartTime = TimeUtil.getCurrentTime("yy-MM-dd HH:mm:ss")
        currentTime = floatWindowStartTime
        startTime = currentTime
        initLastRecordId()
        list.clear()
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
                    recordViewModel.deleteRecordByTimeRange(start = startTime, end = currentTime)
                    list.clear()
                    mAdapter.submitList(emptyList())
                    floatWindowStartTime = TimeUtil.getCurrentTime("yy-MM-dd HH:mm:ss")
                    currentTime = floatWindowStartTime
                    startTime = currentTime
                    initLastRecordId()
                }
            val closeWindow = it.findViewById<ImageButton>(R.id.close_window)
            closeWindow.setOnClickListener {
                EasyFloat.dismiss("floatPrint")
                EasyFloat.dismiss("floatControl")
                dismissFloat = true
                handler.removeCallbacks(refresh)
            }
            val pausePrintRecord = it.findViewById<ImageButton>(R.id.pause_print_record)
            pausePrintRecord.setOnClickListener {
                stopPrint = !stopPrint
                val bgId =
                    if (stopPrint) R.drawable.ic_start_float_24 else R.drawable.ic_outline_pause_24
                pausePrintRecord.setImageResource(bgId)
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
            handler.postDelayed(refresh, 500)
        }.setTag("floatPrint").setShowPattern(ShowPattern.ALL_TIME).setDragEnable(false)
            .setSidePattern(SidePattern.DEFAULT).setLocation(100, 100)
            .setMatchParent(widthMatch = false, heightMatch = false).setAnimator(DefaultAnimator())
            .registerCallback {
                dismiss {
                    dismissFloat = true
                    handler.removeCallbacks(refresh)
                    adapterDataObserver?.let {
                        mAdapter.unregisterAdapterDataObserver(it)
                    }
                    adapterDataObserver = null
                    list.clear()
                }
            }.show()
    }

    private fun Cursor.getStringOrEmpty(index: Int): String {
        if (index == -1 || isNull(index)) return ""
        return getString(index) ?: ""
    }

    private fun Cursor.getIntOrNull(index: Int): Int? {
        if (index == -1 || isNull(index)) return null
        return getInt(index)
    }

    private fun Cursor.getBooleanOrFalse(index: Int): Boolean {
        if (index == -1 || isNull(index)) return false
        return getInt(index) == 1
    }

    private fun isAfterFloatWindowStart(time: String): Boolean {
        if (time.isEmpty() || floatWindowStartTime.isEmpty()) return true
        return time >= floatWindowStartTime
    }

    companion object {
        private const val MAX_FLOAT_RECORD_COUNT = 300
        private val FLOAT_QUERY_PROJECTION = arrayOf(
            "id",
            "type",
            "subType",
            "packageName",
            "isRead",
            "isMark",
            "time"
        )
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
