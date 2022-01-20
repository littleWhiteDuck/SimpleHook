package me.simpleHook.ui.activity

import android.os.Bundle
import android.view.Menu
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.adapter.FragmentStateAdapter
import me.simpleHook.BuildConfig
import me.simpleHook.BuildConfig.*
import me.simpleHook.R
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.fragment.AssistFragment
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.RecordFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.util.*
import java.io.*
import java.lang.reflect.Method

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sp by lazy { SPUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        initView()
        if (!isModuleLive()) "模块未激活".toast(this)
        if (sp.openStorage) FileUtils.verifyStoragePermissions(this)
        initUpdateTip()
        super.onCreate(savedInstanceState)
    }

    private fun initUpdateTip() {
        if (sp.updateShow == BuildConfig.VERSION_NAME) return
        val bufferedReader = BufferedReader(InputStreamReader(assets.open("update")))
        val message = try {
            var msg = ""
            bufferedReader.readLines().forEach {
                msg += it + "\n"
            }
            msg.substring(0, msg.length - 1)
        } catch (e: IOException) {
            "失败！"
        } finally {
            bufferedReader.close()
        }
        customDialog(
            this,
            title = "更新内容",
            message = message,
            okText = "不再提示",
            okClick = { dialogInterface ->
                sp.updateShow = VERSION_NAME
                dialogInterface.dismiss()
            },
            cancelText = "取消",
            { dialogInterface -> dialogInterface.dismiss() },
            cancelAble = false
        )
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount() = 4

                    override fun createFragment(position: Int) = when (position) {
                        0 -> HomeFragment()
                        1 -> AssistFragment()
                        2 -> RecordFragment()
                        else -> SettingsFragment()
                    }
                }
                isUserInputEnabled = false
                offscreenPageLimit = 4
            }
            fun setCurrentItem(index: Int) {
                if (!viewPager.isFakeDragging) {
                    viewPager.setCurrentItem(index, 300L)
                }

            }
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

    @Keep
    fun isModuleLive() = false

}