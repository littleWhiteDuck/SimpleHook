package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.RecordAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.AppUtils
import me.simpleHook.util.FastScrollerUtil
import me.simpleHook.util.FileUtils

class RecordActivity : BaseActivity() {
    private val appViewModel by viewModels<AppViewModel>()
    private lateinit var binding: ActivityRecordBinding
    private var currentPattern = ""
    private var isType = false
    private var typeOrPackageName = ""
    private val recordAdapter by lazy {
        RecordAdapter(isType = isType, onItemClick = {
            appViewModel.updateRecord(it.copy(read = true))
            val bundle = Bundle()
            bundle.putParcelable("printLog", it)
            val intent = Intent(this, RecordDetailActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        val bundle = intent.getBundleExtra("bundle")
        val recordSummary: RecordSummary = bundle!!.getParcelable("recordSummary")!!
        isType = recordSummary.type.isNotEmpty()
        typeOrPackageName = if (isType) recordSummary.type else recordSummary.packageName
        if (isType) {
            supportActionBar?.title = typeOrPackageName
        } else {
            supportActionBar?.apply {
                title = AppUtils.getAppName(this@RecordActivity, typeOrPackageName)
                subtitle = typeOrPackageName
            }
        }
        initView()
        initData()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        binding.apply {
            upFab.hide()
            downFab.hide()
            upFab.setOnClickListener {
                recyclerView.scrollToPosition(0)
                upFab.hide()
            }
            downFab.setOnClickListener {
                recyclerView.scrollToPosition(recordAdapter.itemCount - 1)
                downFab.hide()
            }
        }
        binding.recyclerView.apply {
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(this@RecordActivity)
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
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private var distance = 0
                private var visible = true
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (distance > 20 && visible) {
                        visible = false
                        showDownFab()
                        distance = 0
                    } else if (distance < -20 && !visible) {
                        visible = true
                        distance = 0
                        showUpFab()
                    }
                    if (visible && dy > 0 || !visible && dy < 0) {
                        distance += dy
                    }
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        FastScrollerUtil.bind(binding.recyclerView)
    }

    private fun showDownFab() {
        binding.downFab.show()
        binding.upFab.hide()
    }

    private fun showUpFab() {
        binding.upFab.show()
        binding.downFab.hide()
    }

    private fun initData() {
        appViewModel.filterRecord2.observe(this) {
            recordAdapter.submitList(it)
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        }
        refreshData(0)
    }

    private fun refreshData(delayTime: Long = 500) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isType) {
                appViewModel.filterRecordByType(typeOrPackageName, currentPattern)
            } else {
                appViewModel.filterRecordByPack(typeOrPackageName, currentPattern)
            }
        }, delayTime)
        readFileLogInsert()
    }

    private fun readFileLogInsert() {
        lifecycleScope.launch(Dispatchers.IO) {
            val assistConfigs = appViewModel.getAssistConfigs()
            assistConfigs.forEach {
                val list = FileUtils.readLogFile(this@RecordActivity, it.packageName)
                appViewModel.insertRecord(*list.toTypedArray())
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    currentPattern = newText?.trim() ?: ""
                    refreshData(0)
                    return true
                }

            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.delete_all -> {
                warningDialog(this, title = "警告",
                    message = "你是否确定删除所有记录？", okClick = {
                        if (isType) {
                            appViewModel.deleteRecordByType(typeOrPackageName)
                        } else {
                            appViewModel.deleteRecordByPack(typeOrPackageName)
                        }
                        refreshData()
                    })
            }
            R.id.delete_read -> {
                warningDialog(this, title = "警告",
                    message = "你是否确定删除所有已读记录？", okClick = {
                        if (isType) {
                            appViewModel.deleteReadRecordByType(type = typeOrPackageName)
                        } else {
                            appViewModel.deleteReadRecordByPack(packageName = typeOrPackageName)
                        }
                        refreshData()
                    })
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }
}