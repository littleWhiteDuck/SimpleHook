package me.simpleHook.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.R
import me.simpleHook.adapter.RecordAdapter
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.FragmentRecordBinding
import me.simpleHook.ui.activity.RecordActivity
import me.simpleHook.ui.custom.warningDialog


class RecordFragment : Fragment(), SearchView.OnQueryTextListener {


    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    private val appViewModel: AppViewModel by activityViewModels()
    private val recordAdapter by lazy {
        RecordAdapter(onItemClick = {
            appViewModel.updateRecord(it.copy(read = true))
            val bundle = Bundle()
            bundle.putParcelable("printLog", it)
            val intent = Intent(requireContext(), RecordActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        })
    }
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
        var tempSize = 0
        binding.swipeRefreshLayout.isRefreshing = true
        appViewModel.filterRecord.observe(requireActivity()) {
            recordAdapter.submitList(it)
            if (tempSize < it.size){
                tempSize = it.size
                binding.recyclerView.smoothScrollToPosition(0)
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
        if (binding.swipeRefreshLayout.isRefreshing) {
            refreshData()
        }
        binding.recyclerView.apply {
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (menu.findItem(R.id.app_bar_search) == null) {
            inflater.inflate(R.menu.menu_record_fragment, menu)
            val searchView = menu.findItem(R.id.search).actionView as SearchView
            searchView.apply {
                queryHint = context.getString(R.string.main_home_toolbar_search_hint)
                setOnQueryTextListener(this@RecordFragment)
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
                        appViewModel.deleteHaveReadRecord()
                        refreshData()
                    })
            }
            R.id.scroll_top -> {
                binding.recyclerView.scrollToPosition(0)
            }
            R.id.scroll_bottom -> {
                binding.recyclerView.scrollToPosition(recordAdapter.itemCount - 1)
            }
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String?): Boolean {
        currentPattern = newText?.trim() ?: ""
        refreshData()
        return true
    }

    private fun refreshData() {
        appViewModel.filterRecord(currentPattern)
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }
}
