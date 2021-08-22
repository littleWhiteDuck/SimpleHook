package me.simpleHook

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.util.FileUtils
import me.simpleHook.util.px
import me.simpleHook.util.snack
import me.simpleHook.util.toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        navController = navHostFragment.navController
        val appBarConfiguration =
            AppBarConfiguration.Builder(binding.bottomNavigationView.menu).build()
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addFragment, R.id.appSelectFragment, R.id.assistSettingsFragment -> {
                    runOnUiThread {
                        binding.bottomNavigationView.animate()
                            .translationY(binding.bottomNavigationView.height.px).interpolator =
                            DecelerateInterpolator(1f)
                        binding.bottomNavigationView.visibility = View.GONE
                    }
                }
                else -> runOnUiThread {
                    binding.bottomNavigationView.animate().translationY(0f).interpolator =
                        DecelerateInterpolator(1f)
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        /* window.statusBarColor = Color.TRANSPARENT*/
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (isModuleLive()) "模块已激活".snack(binding.bottomNavigationView)
        FileUtils.verifyStoragePermissions(this)
        /*  val controller = window.insetsController
          controller?.hide(WindowInsets.Type.navigationBars())*/
    }

    @Keep
    fun isModuleLive() = false
/*
    override fun onBackPressed() {
        super.onBackPressed()
        val fragment = navHostFragment.childFragmentManager.primaryNavigationFragment
          if (fragment is HomeFragment) exitProcess(0)
    }*/
}