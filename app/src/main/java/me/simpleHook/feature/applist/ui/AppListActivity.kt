package me.simpleHook.feature.applist.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.core.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_TARGET_API
import me.simpleHook.core.constant.Constant.CLICK_TIME
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.SPUtil
import me.simpleHook.data.AppListItem
import me.simpleHook.databinding.ActivityAppListBinding
import me.simpleHook.feature.applist.viewmodel.AppListViewModel


class AppListActivity : BaseActivity() {
    private lateinit var binding: ActivityAppListBinding
    private var isFromAssist = false
    private val appListViewModel by viewModels<AppListViewModel>()
    private val sp by lazy { SPUtil(this) }
    private var currentSortSelected = 0
    private var currentSortReverse = false
    private var firstClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isFromAssist = intent.getBooleanExtra("isFromAssist", false)
        initView()

        try {
            // 国产手机应用列表权限
            val permissionInfo = applicationContext.packageManager
                .getPermissionInfo("com.android.permission.GET_INSTALLED_APPS", 0)
            if (permissionInfo != null) {
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        "com.android.permission.GET_INSTALLED_APPS"
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@AppListActivity,
                        arrayOf("com.android.permission.GET_INSTALLED_APPS"),
                        999
                    )
                }
            } else {
                initData()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            initData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 999) {
            initData()
        }
    }

    private fun initData() {
        currentSortSelected = sp.appListSortSelected
        currentSortReverse = sp.appListReverse
        appListViewModel.fetchData(currentSortSelected, currentSortReverse)
    }

    private fun initView() {
        with(binding) {
            swipeRefreshLayout.isRefreshing = true
            viewPager.adapter =
                object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                    override fun getItemCount() = 2

                    override fun createFragment(position: Int) = when (position) {
                        0 -> UserAppListFragment()
                        else -> SystemAppListFragment()
                    }
                }
            viewPager.offscreenPageLimit = 1
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                run {
                    when (position) {
                        0 -> tab.text = getString(R.string.app_list_tab_user_app)
                        1 -> tab.text = getString(R.string.app_list_tab_system_app)
                    }
                }
            }.attach()
            swipeRefreshLayout.setOnRefreshListener {
                appListViewModel.fetchData(currentSortSelected, currentSortReverse)
            }
            toolbar.setOnClickListener {
                if (System.currentTimeMillis() - firstClickTime < CLICK_TIME) {
                    val systemRecyclerView = findViewById<RecyclerView>(R.id.user_app_recycler)
                    systemRecyclerView.smoothScrollToPosition(0)
                    val appRecyclerView = findViewById<RecyclerView>(R.id.system_app_recycler)
                    appRecyclerView.smoothScrollToPosition(0)
                } else {
                    firstClickTime = System.currentTimeMillis()
                }
            }
        }
        appListViewModel.selectAppListItem.observe(this) {
            if (it != null) clickResponse(it)
        }

    }

    private fun clickResponse(appListItem: AppListItem) {
        val appName =
            appListItem.name.ifEmpty { AppUtil.getAppName(appListItem.packageName) }
        if (isFromAssist) {
            val intent = Intent().also {
                it.putExtra("appName", appName)
                it.putExtra("packageName", appListItem.packageName)
            }
            setResult(RESULT_OK, intent)
        } else {
            val intent = Intent().also {
                it.putExtra("appName", appName)
                it.putExtra("packageName", appListItem.packageName)
                it.putExtra("versionName", appListItem.versionName)
            }
            setResult(RESULT_OK, intent)
        }
        finish()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
//        searchView.setTextColor(Color.WHITE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String): Boolean {
                appListViewModel.queryPattern.value = newText.trim()
                appListViewModel.filerAppItems(newText.trim())
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
        customDialog(
            this,
            title = getString(R.string.app_list_sort_dialog_title),
            okText = getString(R.string.app_list_sort_dialog_confirm),
            okClick = {
                sp.appListSortSelected = currentSortSelected
                sp.appListReverse = currentSortReverse
                appListViewModel.fetchData(currentSortSelected, currentSortReverse)
                binding.swipeRefreshLayout.isRefreshing = true
            },
            cancelText = getString(R.string.app_list_sort_dialog_cancel),
            cancelClick = {
                currentSortReverse = sp.appListReverse
                currentSortSelected = sp.appListSortSelected
            },
            contentView = contentView,
            cancelAble = false
        ).show()
    }
}
