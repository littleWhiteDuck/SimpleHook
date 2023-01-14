package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnPermissionResult
import com.lzf.easyfloat.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.PrintLogAdapter
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.ui.view.ControlView
import me.simpleHook.util.*

@Keep
open class BaseActivity : AppCompatActivity() {
    protected var isSaving = false

    private val viewModel by viewModels<AppViewModel>()
    private val mAdapter: PrintLogAdapter by lazy { PrintLogAdapter() }
    private val list = ArrayList<PrintLog>()
    private val handler = Handler(Looper.getMainLooper())
    private val assistConfigs by lazy { viewModel.getAssistConfigs() }
    private val configs by lazy { viewModel.getConfigs() }
    private var dismissFloat = false
    private val refresh = object : Runnable {
        override fun run() {
            if (dismissFloat) return
            readFileLogInsert()
            updateData()
            handler.postDelayed(this, 500)
        }
    }
    private val uri = Uri.parse("content://littleWhiteDuck/print_logs")
    private var stopPrint = false
    private var currentTime = ""
    private var startTime = ""
    private var tempCount = 0

    @SuppressLint("RestrictedApi")
    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return if (menu is MenuBuilder) {
            try {
                menu.setOptionalIconsVisible(true)
                super.onMenuOpened(featureId, menu)
                /*   val method: Method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.java)
                            method.isAccessible = true
                            method.invoke(menu, true)*/
            } catch (e: Exception) {
                super.onMenuOpened(featureId, menu)
            }
        } else super.onMenuOpened(featureId, menu)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase))
    }

    private fun readFileLogInsert() {
        lifecycleScope.launch(Dispatchers.IO) {
            assistConfigs.forEach {
                val list = FileUtils.readLogFile(this@BaseActivity, it.packageName)
                viewModel.insertRecord(*list.toTypedArray())
            }
            configs.forEach {
                val list = FileUtils.readLogFile(this@BaseActivity, it.packageName)
                viewModel.insertRecord(*list.toTypedArray())
            }
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun updateData() {
        if (stopPrint) return
        contentResolver.query(
            uri, null, "time > ?", arrayOf(currentTime), null
        )?.apply {
            while (moveToNext()) {
                val log = getString(getColumnIndex("log"))
                val packageName = getString(getColumnIndex("packageName"))
                val time = getString(getColumnIndex("time"))
                list.add(
                    PrintLog(
                        log = log, packageName = packageName, time = time
                    )
                )
                currentTime = time
            }
            close()
        }
        val runnable = Runnable {
            if (list.size != tempCount) {
                tempCount = list.size
                mAdapter.submitList(list)
                mAdapter.notifyDataSetChanged()
            }
        }
        handler.postDelayed(runnable, 0)
    }

    fun initPrintFloat() {
        if (EasyFloat.getFloatView("floatPrint") != null) return
        if (PermissionUtils.checkPermission(this)) {
            showPrintFloat()
        } else {
            getString(R.string.float_window_require_permission).toast(this)
            PermissionUtils.requestPermission(this, object : OnPermissionResult {
                override fun permissionResult(isOpen: Boolean) {
                    if (isOpen) {
                        showPrintFloat()
                    } else {
                        getString(R.string.float_window_require_permission_failed).toast(this@BaseActivity)
                    }
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showPrintFloat() {
        dismissFloat = false
        currentTime = TimeUtil.getDateTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        startTime = currentTime
        EasyFloat.with(this).setLayout(R.layout.window_float) {
            val recyclerView = it.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(this@BaseActivity)
                addItemDecoration(
                    DividerItemDecoration(
                        this@BaseActivity, DividerItemDecoration.VERTICAL
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
                viewModel.deleteRecordByTimeRange(start = startTime, end = currentTime)
                list.clear()
                mAdapter.notifyDataSetChanged()
            }
            val closeWindow = it.findViewById<ImageButton>(R.id.close_window)
            closeWindow.setOnClickListener {
                EasyFloat.dismiss("floatPrint")
                EasyFloat.dismiss("floatControl")
                dismissFloat = true
            }
            val pausePrintRecord = it.findViewById<ImageButton>(R.id.pause_print_record)
            pausePrintRecord.setOnClickListener {
                stopPrint = !stopPrint
                val bgId =
                    if (stopPrint) R.drawable.ic_start_float_24 else R.drawable.ic_outline_pause_24
                pausePrintRecord.setImageResource(bgId)
            }
            mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    if (mAdapter.itemCount - 1 > 0) {
                        recyclerView.smoothScrollToPosition(mAdapter.itemCount - 1)
                    }
                }
            })
            handler.postDelayed(refresh, 500)
        }.setTag("floatPrint").setShowPattern(ShowPattern.ALL_TIME).setDragEnable(false)
            .setSidePattern(SidePattern.DEFAULT).setLocation(100, 100)
            .setMatchParent(widthMatch = false, heightMatch = false).setAnimator(DefaultAnimator())
            .registerCallback {
                dismiss {
                    dismissFloat = true
                    list.clear()
                }
            }.show()
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