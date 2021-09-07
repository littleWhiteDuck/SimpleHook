package me.simpleHook.ui.activity

import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.clans.fab.BuildConfig.VERSION_CODE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import littleWhiteDuck.WindowPreferencesManager
import me.simpleHook.BuildConfig.*
import me.simpleHook.R
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.ui.fragment.AssistFragment
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.util.*
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sp by lazy { SPUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        initView()
        if (!isModuleLive()) "模块未激活".toast(this)
        if (sp.openStorage) FileUtils.verifyStoragePermissions(this)
        initUpdateTip()
    }

    private fun initUpdateTip() {
        if (sp.updateShow == VERSION_CODE.toString()) return
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
        MaterialAlertDialogBuilder(this)
            .setTitle("更新内容")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("不再提示") { dialog, _ ->
                sp.updateShow = VERSION_CODE.toString()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount() = 3

                    override fun createFragment(position: Int) = when (position) {
                        0 -> HomeFragment()
                        1 -> AssistFragment()
                        else -> SettingsFragment()
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
            binding.bottomNavigationView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.homeFragment -> setCurrentItem(0)
                    R.id.assistFragment -> setCurrentItem(1)
                    R.id.settingsFragment -> setCurrentItem(2)
                }
                true
            }
        }
    }

    @Keep
    fun isModuleLive() = false

}