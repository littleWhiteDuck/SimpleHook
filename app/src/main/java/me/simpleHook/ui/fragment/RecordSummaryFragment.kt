package me.simpleHook.ui.fragment

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.RecordSummaryAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentRecordSummaryBinding
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*


class RecordSummaryFragment : Fragment() {


    private var _binding: FragmentRecordSummaryBinding? = null
    private val binding get() = _binding!!
    private val appViewModel: AppViewModel by activityViewModels()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
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
    private val sp by lazy { SPUtils(requireContext()) }
    private val assistConfigs by lazy { appViewModel.getAssistConfigs() }
    private val configs by lazy { appViewModel.getConfigs() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordSummaryBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.progressBar.visibility = View.GONE
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (menu.findItem(R.id.app_bar_search) == null) {
            inflater.inflate(R.menu.menu_record_fragment, menu)
            if (menu.findItem(R.id.search) != null) {
                menu.removeItem(R.id.search)
            }
            if (sp.showByType) {
                menu.findItem(R.id.toTypeShow).isChecked = true
            } else {
                menu.findItem(R.id.toAppShow).isChecked = true
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> {
                warningDialog(requireContext(),
                    title = getString(R.string.record_warn_dialog_title),
                    message = getString(
                        R.string.record_warn_dialog_message_delete_all
                    ),
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
                        appViewModel.deleteRecordByRead()
                        refreshData()
                    })
            }
            R.id.toAppShow -> {
                if (sp.showByType) {
                    refreshData(0)
                    sp.showByType = false
                }
                item.isChecked = !item.isChecked
            }
            R.id.toTypeShow -> {
                if (!sp.showByType) {
                    refreshData(0)
                    sp.showByType = true
                }
                item.isChecked = !item.isChecked
            }
        }
        return true
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

    private fun readFileLogInsert() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (FlavorUtils.isNormal()) {
                assistConfigs.forEach {
                    val list = FileUtils.readLogFile(requireContext(), it.packageName)
                    appViewModel.insertRecord(*list.toTypedArray())
                }
                configs.forEach {
                    val list = FileUtils.readLogFile(requireContext(), it.packageName)
                    appViewModel.insertRecord(*list.toTypedArray())
                }
            } else {
                val list = FileUtils.readLogFile()
                appViewModel.insertRecord(*list.toTypedArray())
            }
        }
    }


    override fun onResume() {
        super.onResume()
        refreshData(0, false)
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        val bottomViewNavigationBehavior = layoutParams.behavior as HideBottomViewOnScrollBehavior
        bottomViewNavigationBehavior.slideUp(bottomNavigationView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
