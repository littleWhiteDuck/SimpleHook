package me.simpleHook.feature.record.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.base.BaseViewFragment
import me.simpleHook.core.ui.custom.warningDialog
import me.simpleHook.core.utils.FastScrollerUtil
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.data.RecordShowPack
import me.simpleHook.data.RecordShowType
import me.simpleHook.data.config.RecordIngestor
import me.simpleHook.feature.config.viewmodel.AppConfigViewModel
import me.simpleHook.feature.main.ui.MainActivity
import me.simpleHook.feature.record.ui.delegate.RecordPackDelegate
import me.simpleHook.feature.record.ui.delegate.RecordTypeDelegate
import me.simpleHook.feature.record.ui.view.RecordSummaryFragmentView
import me.simpleHook.feature.record.viewmodel.RecordViewModel


class RecordSummaryFragment : BaseViewFragment<RecordSummaryFragmentView>() {
    private val appConfigViewModel: AppConfigViewModel by activityViewModels()
    private val recordViewModel by activityViewModels<RecordViewModel>()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var recordSummaryOfItemMenu: Any? = null
    private var refreshJob: Job? = null

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

        recordViewModel.recordShowItems.observe(viewLifecycleOwner) { showItems ->

            root.emptyTip.isVisible = showItems.isEmpty()

            if (multiTypeAdapter.items != showItems) {
                multiTypeAdapter.items = showItems
                multiTypeAdapter.notifyDataSetChanged()
            }

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
        }, onCreateContextMenu = { item, menu ->
            onItemCreateContextMenu(item, menu)
        }))

        multiTypeAdapter.register(RecordShowPack::class.java, RecordPackDelegate(onClick = {
            RecordActivity.startActivity(requireContext(), it.packageName, null)
        }, onDeleteClick = {
            deleteRecord(it)
        }, onCreateContextMenu = { item, menu ->
            onItemCreateContextMenu(item, menu)
        }))

        with(root.recyclerView) {
            adapter = multiTypeAdapter
            layoutManager = LinearLayoutManager(requireContext())
            FastScrollerUtil.bind(this)
        }

        root.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private fun onItemCreateContextMenu(recordSummary: Any, menu: ContextMenu) {
        recordSummaryOfItemMenu = recordSummary
        requireActivity().menuInflater.inflate(R.menu.menu_record_summary_item, menu)
        val title = when (recordSummary) {
            is RecordShowType -> getString(recordSummary.type.displayId)
            is RecordShowPack -> recordSummary.packageName
            else -> ""
        }
        if (title.isNotEmpty()) {
            menu.setHeaderTitle(title)
        }
    }

    private fun deleteRecord(recordSummary: Any) {
        refreshData(preAction = {
            if (recordSummary is RecordShowType) {
                recordViewModel.deleteRecordByTypeNow(recordSummary.type.name)
            } else if (recordSummary is RecordShowPack) {
                recordViewModel.deleteRecordByPackNow(recordSummary.packageName)
            }
        })
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
                        refreshData(preAction = { recordViewModel.deleteAllRecordsNow() })
                    })
            }

            R.id.delete_read -> {
                warningDialog(
                    requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(R.string.record_warn_dialog_message_delete_read),
                    okClick = {
                        refreshData(preAction = { recordViewModel.deleteRecordByReadNow(read = true) })
                    })
            }

            R.id.toAppShow -> {
                if (GlobalValue.sp.showByType) {
                    GlobalValue.sp.showByType = false
                    refreshData()
                }
                menuItem.isChecked = !menuItem.isChecked
            }

            R.id.toTypeShow -> {
                if (!GlobalValue.sp.showByType) {
                    GlobalValue.sp.showByType = true
                    refreshData()
                }
                menuItem.isChecked = !menuItem.isChecked
            }

            R.id.startFloat -> {
                (requireActivity() as MainActivity).initPrintFloat()
            }
        }
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_record_summary_delete) {
            recordSummaryOfItemMenu?.let { deleteRecord(it) }
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun refreshData(showRefresh: Boolean = true, preAction: (suspend () -> Unit)? = null) {
        if (!root.swipeRefreshLayout.isRefreshing && showRefresh) root.swipeRefreshLayout.isRefreshing =
            true
        val appContext = requireContext().applicationContext
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preAction?.invoke()
                    ingestRecords(appContext)
                    recordViewModel.refreshRecordShowItemsNow()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.outLog(e.stackTraceToString())
                root.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun ingestRecords(context: Context) {
        RecordIngestor.ingestFromPackages(
            context = context,
            packageNames = appConfigViewModel.getEnabledPackageNames()
        ) { recordEntities ->
            recordViewModel.insertRecordsNow(recordEntities)
        }
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        refreshJob = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (!root.swipeRefreshLayout.isRefreshing) {
            refreshData(showRefresh = false)
        }
        showBottomNavigation()
    }

    private fun showBottomNavigation() {
        HideViewOnScrollBehavior.from(bottomNavigationView).apply {
            setViewEdge(HideViewOnScrollBehavior.EDGE_BOTTOM)
            slideIn(bottomNavigationView, true)
        }
    }
}
