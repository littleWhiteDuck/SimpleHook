package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.base.BaseActivity
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.extension.fetchJson
import me.simpleHook.extension.setCurrentItem
import me.simpleHook.extension.showToast
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.RecordSummaryFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.ui.fragment.extension.ExtensionFragment
import me.simpleHook.util.*

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sp by lazy { SPUtils(this) }
    private val isActive by lazy { isModuleLive() }
    private val viewModel by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        initView()
        checkActive()
        checkUpdate()
        initPermission()
        initBackup()
        super.onCreate(savedInstanceState)
    }

    private fun initBackup() {
        viewModel.backupLocalWorkerID.observe(this) { ID ->
            if (ID != null) {
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(ID).observe(this) { work ->
                    if (work.state == WorkInfo.State.FAILED) {
                        showToast(getString(R.string.backup_tip_local_auto_backup_failed))
                    }
                }
            }
        }
        viewModel.backupCloudWorkerID.observe(this) { ID ->
            if (ID != null) {
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(ID).observe(this) { work ->
                    if (work.state == WorkInfo.State.FAILED) {
                        showToast(getString(R.string.backup_tip_cloud_auto_backup_failed))
                    }
                }
            }
        }
    }

    private fun checkActive() {
        if (!isActive) {
            supportActionBar?.title = BuildConfig.APP_NAME + getString(R.string.main_not_activated)
        }
    }

    private fun initPermission() {
        if (FlavorUtils.liteVersion) {
            if (!isActive) {
                customDialog(this,
                    title = getString(R.string.module_not_activated),
                    message = getString(R.string.module_not_activated_message),
                    okText = getString(R.string.module_not_activated_ok),
                    okClick = {
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },
                    cancelAble = false,
                    cancelText = getString(R.string.dialog_cancel),
                    cancelClick = { it.dismiss() }).show()
            }
        } else if (FlavorUtils.rootVersion) {
            SuUtil.init()
        } else if (OSUtils.atLeastT()) {
            if (sp.showA13Tip) {
                customDialog(this,
                    title = "Tip",
                    message = getString(R.string.main_android_13_tip),
                    okText = getString(R.string.dialog_cancel),
                    cancelText = getString(R.string.read_introduction_not_remind),
                    cancelClick = {
                        sp.showA13Tip = false
                    }).show()
            }
        } else if (OSUtils.atR2T()) {
            if (!PermissionUtils.isGrantData(Constant.ANDROID_DATA_URI)) {
                requestPermissionDialog(this) {
                    startActivityForData.launch(Constant.ANDROID_DATA_URI.toUri())
                }
            }
        } else if (OSUtils.atMostQ()) {
            if (!PermissionUtils.isGrantWritePermission(this)) {
                requestPermissionDialog(this) {
                    PermissionUtils.verifyStoragePermissions(this)
                }
            }
        }
    }

    private fun checkUpdate() {
        if (BuildConfig.VERSION_NAME.contains("beta")) return
        lifecycleScope.launch(Dispatchers.Main) {
            val result =
                fetchJson("https://api.github.com/repos/littleWhiteDuck/SimpleHook/releases/latest")
                    ?: return@launch
            val versionName = result.optString("name")
            if (versionName.isNotEmpty() && BuildConfig.VERSION_NAME != versionName) {
                val body = result.optString("body").substringAfterLast("更新记录").trim()
                val message = body.ifEmpty { "有新版本，修复若干bug，请更新" }
                customDialog(this@MainActivity,
                    title = getString(R.string.main_version_update) + versionName,
                    message = message,
                    okText = getString(R.string.main_go_upgrade),
                    okClick = {
                        val intent = Intent(Intent.ACTION_VIEW).also {
                            it.data =
                                Uri.parse("https://github.com/littleWhiteDuck/SimpleHook/releases/latest")
                        }
                        startActivity(intent)
                    },
                    cancelText = getString(R.string.dialog_cancel),
                    cancelClick = {
                        it.dismiss()
                    },
                    cancelAble = false).show()
            }
        }
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                adapter = if (FlavorUtils.liteVersion) {
                    object : FragmentStateAdapter(this@MainActivity) {
                        override fun getItemCount() = 2

                        override fun createFragment(position: Int) = when (position) {
                            0 -> HomeFragment()
                            else -> SettingsFragment()
                        }
                    }
                } else {
                    object : FragmentStateAdapter(this@MainActivity) {
                        override fun getItemCount() = 4

                        override fun createFragment(position: Int) = when (position) {
                            0 -> HomeFragment()
                            1 -> ExtensionFragment()
                            2 -> RecordSummaryFragment()
                            else -> SettingsFragment()
                        }
                    }
                }
                isUserInputEnabled = false
                offscreenPageLimit = 3
            }
            fun setCurrentItem(index: Int) {
                if (!viewPager.isFakeDragging) {
                    viewPager.setCurrentItem(index, 300L)
                }

            }
            if (FlavorUtils.liteVersion) {
                binding.bottomNavigationView.menu.removeItem(R.id.assistFragment)
                binding.bottomNavigationView.menu.removeItem(R.id.recordFragment)
                binding.bottomNavigationView.setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.homeFragment -> setCurrentItem(0)
                        R.id.settingsFragment -> setCurrentItem(1)
                    }
                    true
                }
            } else {
                binding.bottomNavigationView.setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.homeFragment -> setCurrentItem(0)
                        R.id.assistFragment -> setCurrentItem(1)
                        R.id.recordFragment -> setCurrentItem(2)
                        R.id.settingsFragment -> setCurrentItem(3)
                    }
                    true
                }
            }

        }
    }

    @Keep
    private fun isModuleLive() = false


}