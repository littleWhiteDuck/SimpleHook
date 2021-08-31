package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.icu.text.SimpleDateFormat
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import littleWhiteDuck.WindowPreferencesManager
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityAppListBinding
import me.simpleHook.ui.fragment.AppSystemFragment
import me.simpleHook.ui.fragment.AppUserFragment
import me.simpleHook.util.AppUtils
import java.util.*
import kotlin.concurrent.thread

class AppListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppListBinding
    private lateinit var userAppList: List<AppItem>
    private lateinit var systemAppList: List<AppItem>
    private var currentQueryText = ""
    private val updateList = 1
    private val userAdapter by lazy { AppListAdapter.getAppSelectAdapter1() }
    private val systemAdapter by lazy { AppListAdapter.getAppSelectAdapter2() }
    private var isFromAssist = false
    private val viewModel by viewModels<AppViewModel>()
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateList -> {
                    userAppList = getAppList(AppUtils.getInstalledUserApp(this@AppListActivity))
                    systemAppList = getAppList(AppUtils.getInstalledSystemApp(this@AppListActivity))
                    filterUserList(userAppList)
                    filterSystemList(systemAppList)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val windowPreferencesManager = WindowPreferencesManager(this)
        windowPreferencesManager.applyEdgeToEdgePreference(window)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isFromAssist = intent.getBooleanExtra("isFromAssist", false)
        binding.swipeRefreshLayout.isRefreshing = true
        updateAppList()
        initView()
    }

    private fun initView() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int) = when (position) {
                0 -> AppUserFragment()
                else -> AppSystemFragment()
            }
        }
        binding.apply {
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                run {
                    when (position) {
                        0 -> tab.text = "用户应用"
                        1 -> tab.text = "系统应用"
                    }
                }
            }.attach()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            updateAppList()
        }
        userAdapter.setOnClickListener(object : AppListAdapter.OnItemClickListener {
            override fun onItemClickListener(appItem: AppItem) {
                clickResponse(appItem)
            }
        })
        systemAdapter.setOnClickListener(object : AppListAdapter.OnItemClickListener {
            override fun onItemClickListener(appItem: AppItem) {
                clickResponse(appItem)
            }
        })
    }

    private fun clickResponse(appItem: AppItem) {
        if (isFromAssist) {
            val assistConfig =
                AssistConfig(appName = appItem.name, packageName = appItem.packageName)
            viewModel.insertAssistConfigs(assistConfig)
        } else {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putParcelable("appItem", appItem)
            intent.putExtra("bundle", bundle)
            setResult(RESULT_OK, intent)
            finish()
        }
        finish()
    }

    private fun updateAppList() {

        thread {
            val msg = Message()
            msg.what = updateList
            handler.sendMessage(msg)
        }
    }

    private fun filterUserList(userList: List<AppItem>) {
        if (currentQueryText == "") {
            userAdapter.submitList(userList)
        } else {
            val filter1 = userAppList.filter {
                it.packageName.contains(currentQueryText, true) ||
                        it.name.contains(currentQueryText, true)
            }
            userAdapter.submitList(filter1)
        }
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun filterSystemList(systemList: List<AppItem>) {
        if (currentQueryText == "") {
            systemAdapter.submitList(systemList)
        } else {
            val filter2 = systemAppList.filter {
                it.packageName.contains(currentQueryText, true) ||
                        it.name.contains(currentQueryText, true)
            }
            systemAdapter.submitList(filter2)
        }
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun getAppList(packageInfoList: List<PackageInfo>): List<AppItem> {
        val appList = ArrayList<AppItem>()
        for (i in packageInfoList.indices) {
            packageInfoList[i].apply {
                appList.add(
                    AppItem(
                        AppUtils.getAppName(this@AppListActivity, this),
                        packageName,
                        AppUtils.getAppVersionName(this@AppListActivity, packageName),
                        getDateTime(lastUpdateTime)
                    )
                )
            }
        }
        return appList
    }

    /**
     * 获取最后一次更新时间
     */
    @SuppressLint("SimpleDateFormat")
    private fun getDateTime(time: Long) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat("yy-MM-dd").format(time)
    } else ""

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                currentQueryText = newText.trim()
                filterUserList(userAppList)
                filterSystemList(systemAppList)
                return true
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}
