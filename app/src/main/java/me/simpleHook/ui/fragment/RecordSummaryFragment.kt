package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.base.BaseViewFragment
import me.simpleHook.config.RecordsHelper
import me.simpleHook.data.RecordShowPack
import me.simpleHook.data.RecordShowType
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.recyclerview.delegate.RecordPackDelegate
import me.simpleHook.recyclerview.delegate.RecordTypeDelegate
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.record.RecordSummaryFragmentView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.util.LogUtils
import me.simpleHook.util.RecordType
import me.simpleHook.viewmodel.AppConfigViewModel
import me.simpleHook.viewmodel.RecordViewModel


class RecordSummaryFragment : BaseViewFragment<RecordSummaryFragmentView>() {
    private val appConfigViewModel: AppConfigViewModel by activityViewModels()
    private val recordViewModel by activityViewModels<RecordViewModel>()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var tempListSize = 0
    private val multiTypeAdapter = MultiTypeAdapter()
    private lateinit var assistConfigs: List<AssistConfig>
    private lateinit var configs: List<AppConfig>
    private var needCheckPacks = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        root.swipeRefreshLayout.isRefreshing = true
        root.progressBar.hide()
        appConfigViewModel.getAllConfigs().observe(requireActivity()) {
            it.forEach { appConfig ->
                if (appConfig.enable && AppUtils.isAppInstalled(appConfig.packageName)
                ) {
                    needCheckPacks.add(appConfig.packageName)
                }
            }
        }
        appConfigViewModel.getAllAssistConfigs().observe(requireActivity()) {
            it.forEach { exConfig ->
                if (exConfig.allSwitch && AppUtils.isAppInstalled(exConfig.packageName)
                ) {
                    needCheckPacks.add(exConfig.packageName)
                }
            }
        }
        recordViewModel.filterRecordPT.observe(requireActivity()) {
            if (it.isEmpty()) {
                root.emptyTip.visibility = View.VISIBLE
            } else {
                root.emptyTip.visibility = View.GONE
            }
            if (it.size >= 66666 && !GlobalValue.sp.showMoreDataTip) {
                warningDialog(
                    requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(R.string.record_warn_dialog_message_more_data),
                    okText = getString(R.string.record_warn_dialog_ok_more_data),
                    okClick = { GlobalValue.sp.showMoreDataTip = true })
            }
            val hashSet = HashSet<String>()
            val hasMap = HashMap<String, Int>()
            it.forEach { printLog ->
                if (GlobalValue.sp.showByType) {
                    val type = RecordType.getSimpleText(printLog.type)
                    hashSet.add(type)
                    hashSet.forEach { typeStr ->
                        if (typeStr == type) {
                            val tempCount = hasMap[typeStr] ?: 0
                            hasMap[typeStr] = tempCount + 1
                        }
                    }
                } else {
                    hashSet.add(printLog.packageName)
                    hashSet.forEach { pack ->
                        if (pack == printLog.packageName) {
                            val tempCount = hasMap[pack] ?: 0
                            hasMap[pack] = tempCount + 1
                        }
                    }
                }
            }
            val list = mutableListOf<Any>()
            hashSet.forEach { value ->
                if (GlobalValue.sp.showByType) {
                    list.add(RecordShowType(type = value, count = hasMap[value] ?: 0))
                } else {
                    list.add(RecordShowPack(packageName = value, count = hasMap[value] ?: 0))
                }
            }
            tempListSize = list.size
            multiTypeAdapter.items = list
            multiTypeAdapter.notifyDataSetChanged()
            root.progressBar.visibility = View.GONE
            root.swipeRefreshLayout.isRefreshing = false
        }
        if (root.swipeRefreshLayout.isRefreshing) {
            refreshData()
        }
        multiTypeAdapter.register(RecordShowType::class.java, RecordTypeDelegate(onClick = {
            RecordActivity.startActivity(requireContext(), null, it.type)
        }, onDeleteClick = {
            deleteRecord(it)
        }))
        multiTypeAdapter.register(RecordShowPack::class.java, RecordPackDelegate(onClick = {
            RecordActivity.startActivity(requireContext(), it.packageName, null)
        }, onDeleteClick = {
            deleteRecord(it)
        }))

        with(root.recyclerView) {
            adapter = multiTypeAdapter
            layoutManager = LinearLayoutManager(requireContext())
            FastScrollerUtil.bind(this)
        }
        root.swipeRefreshLayout.setOnRefreshListener {
            refreshData(0)
        }
    }

    private fun deleteRecord(recordSummary: Any) {
        if (recordSummary is RecordShowType) {
            recordViewModel.deleteRecordByType(recordSummary.type)
        } else if (recordSummary is RecordShowPack) {
            recordViewModel.deleteRecordByPack(recordSummary.packageName)
        }
        refreshData(200, true)
    }

    override fun initRootView(): RecordSummaryFragmentView {
        return RecordSummaryFragmentView(requireContext())
    }

    override fun init() {
        initView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_record_fragment, menu)
        if (GlobalValue.sp.showByType) {
            menu.findItem(R.id.toTypeShow).isChecked = true
        } else {
            menu.findItem(R.id.toAppShow).isChecked = true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.delete_all -> {
                warningDialog(
                    requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(R.string.record_warn_dialog_message_delete_all),
                    okClick = {
                        recordViewModel.deleteAllLogs()
                        refreshData()
                    })
            }

            R.id.delete_read -> {
                warningDialog(
                    requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(R.string.record_warn_dialog_message_delete_read),
                    okClick = {
                        recordViewModel.deleteRecordByRead(read = true)
                        refreshData()
                    })
            }

            R.id.toAppShow -> {
                if (GlobalValue.sp.showByType) {
                    refreshData(0)
                    GlobalValue.sp.showByType = false
                }
                menuItem.isChecked = !menuItem.isChecked
            }

            R.id.toTypeShow -> {
                if (!GlobalValue.sp.showByType) {
                    refreshData(0)
                    GlobalValue.sp.showByType = true
                }
                menuItem.isChecked = !menuItem.isChecked
            }

            R.id.startFloat -> {
                (requireActivity() as MainActivity).initPrintFloat()
            }
        }
        return true
    }

    private fun refreshData(time: Long = 500, showRefresh: Boolean = true) {
        if (!root.swipeRefreshLayout.isRefreshing && showRefresh) root.swipeRefreshLayout.isRefreshing =
            true
        Handler(Looper.getMainLooper()).postDelayed({
            recordViewModel.getAllRecord()
        }, time)
        readFileLogInsert()
        readFileLogInsert()
    }

    @Synchronized
    private fun readFileLogInsert() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!::assistConfigs.isInitialized) {
                    assistConfigs = appConfigViewModel.getAssistConfigs()
                    assistConfigs.forEach {
                        if (it.allSwitch && AppUtils.isAppInstalled(it.packageName)
                        ) needCheckPacks.add(it.packageName)
                    }
                    configs = appConfigViewModel.getConfigs()
                    configs.forEach {
                        if (it.enable && AppUtils.isAppInstalled(it.packageName)
                        ) needCheckPacks.add(it.packageName)
                    }
                    needCheckPacks.forEach {
                        val list = RecordsHelper.insertRecordsFromFile(requireContext(), it)
                        recordViewModel.insertRecord(*list.toTypedArray())
                    }
                } else {
                    needCheckPacks.forEach {
                        val list = RecordsHelper.insertRecordsFromFile(requireContext(), it)
                        recordViewModel.insertRecord(*list.toTypedArray())
                    }
                }
            } catch (e: Exception) {
                LogUtils.outLog(e.stackTraceToString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(0, false)
        refreshData(100, false)
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        val bottomViewNavigationBehavior = layoutParams.behavior as HideBottomViewOnScrollBehavior
        bottomViewNavigationBehavior.slideUp(bottomNavigationView, true)
    }
}
