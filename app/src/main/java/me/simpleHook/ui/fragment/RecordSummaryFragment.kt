package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.RecordSummaryAdapter
import me.simpleHook.base.BaseExtensionFragment
import me.simpleHook.bean.RecordSummary
import me.simpleHook.config.RecordsHelper
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentRecordSummaryBinding
import me.simpleHook.ui.activity.MainActivity
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*


class RecordSummaryFragment : BaseExtensionFragment<FragmentRecordSummaryBinding>() {
    private val appViewModel: AppViewModel by activityViewModels()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var tempListSize = 0
    private val recorderSummaryAdapter by lazy {
        RecordSummaryAdapter(onClick = {
            val bundle = Bundle()
            bundle.putParcelable("recordSummary", it)
            val intent = Intent(requireContext(), RecordActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        }, onDeleteClick = {
            deleteRecord(it)
        })
    }

    private lateinit var assistConfigs: List<AssistConfig>
    private lateinit var configs: List<AppConfig>
    private var needCheckPacks = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.progressBar.visibility = View.GONE
        appViewModel.getAllConfigs().observe(requireActivity()) {
            it.forEach { appConfig ->
                if (appConfig.enable && AppUtils.isAppInstalled(requireContext(),
                        appConfig.packageName)
                ) {
                    needCheckPacks.add(appConfig.packageName)
                }
            }
        }
        appViewModel.getAllAssistConfigs().observe(requireActivity()) {
            it.forEach { exConfig ->
                if (exConfig.allSwitch && AppUtils.isAppInstalled(requireContext(),
                        exConfig.packageName)
                ) {
                    needCheckPacks.add(exConfig.packageName)
                }
            }
        }
        appViewModel.filterRecordPT.observe(requireActivity()) {
            if (it.isEmpty()) {
                binding.emptyTip.visibility = View.VISIBLE
            } else {
                binding.emptyTip.visibility = View.GONE
            }
            if (it.size >= 66666 && !sp.showMoreDataTip) {
                warningDialog(requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(R.string.record_warn_dialog_message_more_data),
                    okText = getString(R.string.record_warn_dialog_ok_more_data),
                    okClick = { sp.showMoreDataTip = true })
            }
            val hashSet = HashSet<String>()
            val hasMap = HashMap<String, Int>()
            it.forEach { printLog ->
                if (sp.showByType) {
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
            val list = mutableListOf<RecordSummary>()
            hashSet.forEach { value ->
                if (sp.showByType) {
                    list.add(RecordSummary(type = value, count = hasMap[value] ?: 0))
                } else {
                    list.add(RecordSummary(packageName = value, count = hasMap[value] ?: 0))
                }
            }
            if (tempListSize != list.size) {
                recorderSummaryAdapter.submitList(emptyList())
                recorderSummaryAdapter.submitList(emptyList())
            }
            tempListSize = list.size
            recorderSummaryAdapter.submitList(list)
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        }
        if (binding.swipeRefreshLayout.isRefreshing) {
            refreshData()
        }
        binding.recyclerView.apply {
            adapter = recorderSummaryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
                ) {
                    // Get the position of the view in the recycler view
                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        return
                    }

                    if (position == parent.adapter!!.itemCount - 1) {
                        // Add padding to the last item. You should probably use a @dimen resource.
                        outRect.bottom = 200
                    }
                }
            })
        }
        FastScrollerUtil.bind(binding.recyclerView)
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData(0)
        }
    }

    private fun deleteRecord(recordSummary: RecordSummary) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (recordSummary.type.isNotEmpty()) {
                appViewModel.deleteRecordByType(recordSummary.type)
            } else {
                appViewModel.deleteRecordByPack(recordSummary.packageName)
            }
        }
        refreshData(200, true)
    }

    override fun canBack(): Boolean {
        return true
    }

    override fun performBack() {

    }

    override fun notBackTip() {

    }

    override fun enableCallback() = false

    override fun init() {
        initMenu()
        initView()
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_record_fragment, menu)
                if (menu.findItem(R.id.search) != null) {
                    menu.removeItem(R.id.search)
                }
                if (sp.showByType) {
                    menu.findItem(R.id.toTypeShow).isChecked = true
                } else {
                    menu.findItem(R.id.toAppShow).isChecked = true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.delete_all -> {
                        warningDialog(requireContext(),
                            title = getString(R.string.record_warn_dialog_title),
                            message = getString(R.string.record_warn_dialog_message_delete_all),
                            okClick = {
                                appViewModel.deleteAllLogs()
                                refreshData()
                            })
                    }
                    R.id.delete_read -> {
                        warningDialog(requireContext(),
                            title = getString(R.string.record_warn_dialog_title),
                            message = getString(R.string.record_warn_dialog_message_delete_read),
                            okClick = {
                                appViewModel.deleteRecordByRead(read = true)
                                refreshData()
                            })
                    }
                    R.id.toAppShow -> {
                        if (sp.showByType) {
                            refreshData(0)
                            sp.showByType = false
                        }
                        menuItem.isChecked = !menuItem.isChecked
                    }
                    R.id.toTypeShow -> {
                        if (!sp.showByType) {
                            refreshData(0)
                            sp.showByType = true
                        }
                        menuItem.isChecked = !menuItem.isChecked
                    }
                    R.id.startFloat -> {
                        (requireActivity() as MainActivity).initPrintFloat()
                    }
                }
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun refreshData(time: Long = 500, showRefresh: Boolean = true) {
        if (!binding.swipeRefreshLayout.isRefreshing && showRefresh) binding.swipeRefreshLayout.isRefreshing =
            true
        Handler(Looper.getMainLooper()).postDelayed({
            appViewModel.getAllRecord()
        }, time)
        readFileLogInsert()
        readFileLogInsert()
    }

    @Synchronized
    private fun readFileLogInsert() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!::assistConfigs.isInitialized) {
                    assistConfigs = appViewModel.getAssistConfigs()
                    assistConfigs.forEach {
                        if (it.allSwitch && AppUtils.isAppInstalled(requireContext(),
                                it.packageName)
                        ) needCheckPacks.add(it.packageName)
                    }
                    configs = appViewModel.getConfigs()
                    configs.forEach {
                        if (it.enable && AppUtils.isAppInstalled(requireContext(),
                                it.packageName)
                        ) needCheckPacks.add(it.packageName)
                    }
                    needCheckPacks.forEach {
                        val list = RecordsHelper.insertRecordsFromFile(requireContext(), it)
                        appViewModel.insertRecord(*list.toTypedArray())
                    }
                } else {
                    needCheckPacks.forEach {
                        val list = RecordsHelper.insertRecordsFromFile(requireContext(), it)
                        appViewModel.insertRecord(*list.toTypedArray())
                    }
                }
            } catch (e: Exception) {
                LogUtils.outLog(e.stackTraceToString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(100, false)
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        val bottomViewNavigationBehavior = layoutParams.behavior as HideBottomViewOnScrollBehavior
        bottomViewNavigationBehavior.slideUp(bottomNavigationView)
    }
}
