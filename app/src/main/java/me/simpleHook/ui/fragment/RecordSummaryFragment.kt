package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
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
import me.simpleHook.recyclerview.delegate.RecordPackDelegate
import me.simpleHook.recyclerview.delegate.RecordTypeDelegate
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.record.RecordSummaryFragmentView
import me.simpleHook.utils.FastScrollerUtil
import me.simpleHook.utils.LogUtil
import me.simpleHook.viewmodel.AppConfigViewModel
import me.simpleHook.viewmodel.RecordViewModel


class RecordSummaryFragment : BaseViewFragment<RecordSummaryFragmentView>() {
    private val appConfigViewModel: AppConfigViewModel by activityViewModels()
    private val recordViewModel by activityViewModels<RecordViewModel>()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }

    private val multiTypeAdapter = MultiTypeAdapter()


    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        root.swipeRefreshLayout.isRefreshing = true
        root.progressBar.hide()

        /*if (it.size >= 66666 && !GlobalValue.sp.showMoreDataTip) {
            warningDialog(
                requireContext(),
                title = getString(R.string.record_warn_dialog_title),
                message = getString(R.string.record_warn_dialog_message_more_data),
                okText = getString(R.string.record_warn_dialog_ok_more_data),
                okClick = { GlobalValue.sp.showMoreDataTip = true })
        }*/

        recordViewModel.recordShowItems.observe(requireActivity()) { showItems ->

            root.emptyTip.isVisible = showItems.isEmpty()

            multiTypeAdapter.items = showItems
            multiTypeAdapter.notifyDataSetChanged()

            root.progressBar.hide()
            root.swipeRefreshLayout.isRefreshing = false
        }

        if (root.swipeRefreshLayout.isRefreshing) {
            refreshData()
        }

        multiTypeAdapter.register(RecordShowType::class.java, RecordTypeDelegate(onClick = {
            RecordActivity.startActivity(requireContext(), null, it.type.name)
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
            recordViewModel.deleteRecordByType(recordSummary.subType)
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
                        recordViewModel.deleteAllRecords()
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
            recordViewModel.fetchRecordShowItems()
        }, time)
        readFileLogInsert()
    }

    private fun readFileLogInsert() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                appConfigViewModel.getEnabledPackageNames().forEach {
                    val recordEntities = RecordsHelper.insertRecordsFromFile(requireContext(), it)
                    recordViewModel.insertRecords(*recordEntities.toTypedArray())
                }
            } catch (e: Exception) {
                LogUtil.outLog(e.stackTraceToString())
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
