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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import me.simpleHook.R
import me.simpleHook.adapter.RecordSummaryAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentRecordBinding
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.RecordType
import me.simpleHook.util.SPUtils


class RecordFragment : Fragment() {


    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    private val appViewModel: AppViewModel by activityViewModels()
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private val recorderSummaryAdapter by lazy {
        RecordSummaryAdapter {
            val bundle = Bundle()
            bundle.putParcelable("recordSummary", it)
            val intent = Intent(requireContext(), RecordActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        }
    }
    private val sp by lazy { SPUtils(requireContext()) }
    private var currentPattern = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.progressBar.visibility = View.GONE
        appViewModel.filterRecord.observe(requireActivity()) {
            if (it.size >= 66666 && !sp.showMoreDataTip) {
                warningDialog(
                    requireContext(),
                    title = "提示",
                    message = "数据过多可能造成查询较慢等问题，建议删除部分或全部数据",
                    okText = "不再提示",
                    okClick = { sp.showMoreDataTip = true }
                )
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
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
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
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData(0)
        }
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
                warningDialog(requireContext(), title = "警告",
                    message = "你是否确定删除所有记录？", okClick = {
                        appViewModel.deleteAllLogs()
                        refreshData()
                    })
            }
            R.id.delete_read -> {
                warningDialog(requireContext(), title = "警告",
                    message = "你是否确定删除所有已读记录？", okClick = {
                        appViewModel.deleteRecordByRead()
                        refreshData()
                    })
            }
            R.id.scroll_top -> {
                binding.recyclerView.smoothScrollToPosition(0)
            }
            R.id.scroll_bottom -> {
                binding.recyclerView.smoothScrollToPosition(recorderSummaryAdapter.itemCount - 1)
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
            appViewModel.filterRecord(currentPattern)
        }, time)
    }

    override fun onResume() {
        super.onResume()
        refreshData(0, false)
        val layoutParams = bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams
        val bottomViewNavigationBehavior = layoutParams.behavior as HideBottomViewOnScrollBehavior
        bottomViewNavigationBehavior.slideUp(bottomNavigationView)
    }
}
