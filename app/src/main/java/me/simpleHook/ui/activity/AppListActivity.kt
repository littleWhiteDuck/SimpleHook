package me.simpleHook.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import littleWhiteDuck.WindowPreferencesManager
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityAppListBinding
import me.simpleHook.ui.fragment.AppSystemFragment
import me.simpleHook.ui.fragment.AppUserFragment

class AppListActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val blackList = "me.simpleHook,bin.mt.plus.canary,com.drakeet.purewriter"
    private lateinit var binding: ActivityAppListBinding
    private var currentQueryText = ""
    private val userAdapter by lazy { AppListAdapter.getAppSelectAdapter1() }
    private val systemAdapter by lazy { AppListAdapter.getAppSelectAdapter2() }
    private var isFromAssist = false
    private val viewModel by viewModels<AppViewModel>()
    private val mViewModel by viewModels<me.simpleHook.viewmodel.AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isFromAssist = intent.getBooleanExtra("isFromAssist", false)
        initView()
        initViewModel()
    }

    private fun initViewModel() {
        mViewModel.userApps.observe(this) {
            if (currentQueryText.isNotEmpty()) {
                filterUserList()
            } else {
                userAdapter.submitList(it)
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
        mViewModel.systemApps.observe(this) {
            if (currentQueryText.isNotEmpty()) {
                filterUserList()
            } else {
                systemAdapter.submitList(it)
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
        mViewModel.fetchData()
    }

    private fun initView() {
        binding.swipeRefreshLayout.isRefreshing = true
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
            mViewModel.fetchData()
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


    private fun filterUserList() {
        val filter1 = mViewModel.userApps.value?.filter {
            !blackList.contains(it.packageName) && (it.packageName.contains(
                currentQueryText,
                true
            ) || it.name.contains(currentQueryText, true))
        }
        userAdapter.submitList(filter1)
    }

    private fun filterSystemList() {
        val filter2 = mViewModel.systemApps.value?.filter {
            it.packageName.contains(currentQueryText, true) ||
                    it.name.contains(currentQueryText, true)
        }
        systemAdapter.submitList(filter2)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                currentQueryText = newText.trim()
                filterUserList()
                filterSystemList()
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
