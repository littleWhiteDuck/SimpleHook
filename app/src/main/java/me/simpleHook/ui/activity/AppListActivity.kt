package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_TARGET_API
import me.simpleHook.constant.Constant.CLICK_TIME
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityAppListBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.fragment.AppListFragment
import me.simpleHook.util.SPUtils

class AppListActivity : BaseActivity(), CoroutineScope by MainScope() {
    private val blackList = "me.simpleHook,bin.mt.plus.canary,com.drakeet.purewriter"
    private lateinit var binding: ActivityAppListBinding
    private var currentQueryText = ""
    private val userAdapter by lazy { AppListAdapter.getAppSelectAdapter1() }
    private val systemAdapter by lazy { AppListAdapter.getAppSelectAdapter2() }
    private var isFromAssist = false
    private val viewModel by viewModels<AppViewModel>()
    private val mViewModel by viewModels<me.simpleHook.viewmodel.AppViewModel>()
    private var assistConfig: AssistConfig? = null
    private val sp by lazy { SPUtils(this) }
    private var currentSortSelected = 0
    private var currentSortReverse = false
    private var firstClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isFromAssist = intent.getBooleanExtra("isFromAssist", false)
        lifecycleScope.launch(Dispatchers.IO) {
            assistConfig = viewModel.queryDefaultExConfig()[0]
        }
        initView()
        initData()
    }

    private fun initData() {
        currentSortSelected = sp.appListSortSelected
        currentSortReverse = sp.appListReverse
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
        mViewModel.fetchData(currentSortSelected, currentSortReverse)
    }

    private fun initView() {
        binding.toolbar.setOnClickListener {
            if (System.currentTimeMillis() - firstClickTime < CLICK_TIME) {
                findViewById<RecyclerView>(R.id.recycler_view).scrollToPosition(0)
            } else {
                firstClickTime = System.currentTimeMillis()
            }
        }
        binding.swipeRefreshLayout.isRefreshing = true
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int) = when (position) {
                0 -> AppListFragment()
                else -> AppListFragment("system")
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
            mViewModel.fetchData(currentSortSelected, currentSortReverse)
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
            assistConfig?.let {
                it.packageName = appItem.packageName
                it.appName = appItem.name
                it.id = 0
                viewModel.insertAssistConfigs(it)
            } ?: viewModel.insertAssistConfigs(
                AssistConfig(
                    appName = appItem.name,
                    packageName = appItem.packageName
                )
            )

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
            R.id.app_list_sort_settings -> showSettingsDialog()
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun showSettingsDialog() {
        val contentView = layoutInflater.inflate(R.layout.app_list_sort_settings, null)
        val radioGroup = contentView.findViewById<RadioGroup>(R.id.radio_settings)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentSortSelected = when (checkedId) {
                R.id.app_name -> APP_LIST_BY_NAME
                R.id.package_name -> APP_LIST_BY_PACKAGE_NAME
                R.id.installed_time -> APP_LIST_BY_INSTALLED_TIME
                else -> APP_LIST_BY_TARGET_API
            }
        }
        radioGroup.apply {
            when (currentSortSelected) {
                APP_LIST_BY_NAME -> findViewById<RadioButton>(R.id.app_name).isChecked = true
                APP_LIST_BY_PACKAGE_NAME -> findViewById<RadioButton>(R.id.package_name).isChecked =
                    true
                APP_LIST_BY_INSTALLED_TIME -> findViewById<RadioButton>(R.id.installed_time).isChecked =
                    true
                else -> findViewById<RadioButton>(R.id.target_api).isChecked = true
            }
        }
        val reverseSort = contentView.findViewById<CheckBox>(R.id.reverse_sort)
        reverseSort.setOnCheckedChangeListener { _, isChecked -> currentSortReverse = isChecked }
        reverseSort.isChecked = currentSortReverse
        customDialog(this, title = "排序方式", okText = "确定", okClick = {
            sp.appListSortSelected = currentSortSelected
            sp.appListReverse = currentSortReverse
            mViewModel.fetchData(currentSortSelected, currentSortReverse)
            binding.swipeRefreshLayout.isRefreshing = true
        }, cancelText = "取消", contentView = contentView)
    }
}
