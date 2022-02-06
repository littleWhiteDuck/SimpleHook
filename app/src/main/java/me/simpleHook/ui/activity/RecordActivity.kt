package me.simpleHook.ui.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import me.simpleHook.R
import me.simpleHook.adapter.RecordAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.ui.custom.MyFastScroller
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.AppUtils

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

    private fun initView() {
        binding.recyclerView.apply {
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(this@RecordActivity)
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        val verticalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val verticalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        val horizontalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val horizontalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        val myFastScroller = MyFastScroller(
            binding.recyclerView,
            verticalThumbDrawable,
            verticalTrackDrawable,
            horizontalThumbDrawable,
            horizontalTrackDrawable,
            resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.fastscroll_margin)
        )
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

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record_fragment, menu)
        menu.removeItem(R.id.toShow)
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
            R.id.scroll_top -> {
                binding.recyclerView.scrollToPosition(0)
            }
            R.id.scroll_bottom -> {
                binding.recyclerView.scrollToPosition(recordAdapter.itemCount - 1)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }
}