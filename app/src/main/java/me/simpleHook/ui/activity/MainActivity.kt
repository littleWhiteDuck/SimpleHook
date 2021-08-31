package me.simpleHook.ui.activity

import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import littleWhiteDuck.WindowPreferencesManager
import me.simpleHook.R
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.ui.fragment.AssistFragment
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sp by lazy { SPUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val windowPreferencesManager = WindowPreferencesManager(this)
        windowPreferencesManager.applyEdgeToEdgePreference(window)
        initView()
        if (!isModuleLive()) "模块未激活".toast(this)
        if (sp.openStorage) FileUtils.verifyStoragePermissions(this)
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
            binding.bottomNavigationView.setOnNavigationItemSelectedListener {
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