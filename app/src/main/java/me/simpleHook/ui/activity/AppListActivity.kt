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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import me.simpleHook.R
import me.simpleHook.bean.AppItem
import me.simpleHook.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_TARGET_API
import me.simpleHook.constant.Constant.CLICK_TIME
import me.simpleHook.databinding.ActivityAppListBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.base.BaseActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.fragment.AppListFragment
import me.simpleHook.util.SPUtils
import me.simpleHook.viewmodel.AppViewModel

class AppListActivity : BaseActivity() {
    private lateinit var binding: ActivityAppListBinding
    private var isFromAssist = false
    private val appViewModel by viewModels<AppViewModel>()
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
        initView()
        initData()
    }

    private fun initData() {
        currentSortSelected = sp.appListSortSelected
        currentSortReverse = sp.appListReverse
        appViewModel.fetchData(currentSortSelected, currentSortReverse)
    }

    private fun initView() {
        binding.toolbar.setOnClickListener {
            if (System.currentTimeMillis() - firstClickTime < CLICK_TIME) {
                findViewById<RecyclerView>(R.id.recycler_view).smoothScrollToPosition(0)
            } else {
                firstClickTime = System.currentTimeMillis()
            }
        }
        binding.swipeRefreshLayout.isRefreshing = true
        binding.viewPager.adapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount() = 2

                override fun createFragment(position: Int) = when (position) {
                    0 -> AppListFragment("user")
                    else -> AppListFragment("system")
                }
            }
        binding.viewPager.offscreenPageLimit = 1
        binding.apply {
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                run {
                    when (position) {
                        0 -> tab.text = getString(R.string.app_list_tab_user_app)
                        1 -> tab.text = getString(R.string.app_list_tab_system_app)
                    }
                }
            }.attach()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            appViewModel.fetchData(currentSortSelected, currentSortReverse)
        }
        appViewModel.selectAppItem.observe(this) {
            if (it != null) clickResponse(it)
        }

    }

    private fun clickResponse(appItem: AppItem) {
        if (isFromAssist) {
            val intent = Intent().also {
                it.putExtra("appName", appItem.name)
                it.putExtra("packageName", appItem.packageName)
            }
            setResult(RESULT_OK, intent)
        } else {
            val intent = Intent().also {
                it.putExtra("appName", appItem.name)
                it.putExtra("packageName", appItem.packageName)
                it.putExtra("versionName", appItem.versionName)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        finish()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                appViewModel.queryPattern.value = newText.trim()
                appViewModel.filerAppItems(newText.trim())
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
        customDialog(this,
            title = getString(R.string.app_list_sort_dialog_title),
            okText = getString(R.string.app_list_sort_dialog_confirm),
            okClick = {
                sp.appListSortSelected = currentSortSelected
                sp.appListReverse = currentSortReverse
                appViewModel.fetchData(currentSortSelected, currentSortReverse)
                binding.swipeRefreshLayout.isRefreshing = true
            },
            cancelText = getString(R.string.app_list_sort_dialog_cancel),
            cancelClick = {
                currentSortReverse = sp.appListReverse
                currentSortSelected = sp.appListSortSelected
            },
            contentView = contentView,
            cancelAble = false).show()
    }
}
