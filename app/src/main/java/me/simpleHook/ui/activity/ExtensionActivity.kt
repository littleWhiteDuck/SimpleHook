package me.simpleHook.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import me.simpleHook.R
import me.simpleHook.compat.BundleCompat
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.ActivityExtensionBinding
import me.simpleHook.ui.WindowPreferencesManager

class ExtensionActivity : BaseActivity() {
    private lateinit var binding: ActivityExtensionBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var extensionConfig: AssistConfig


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        val bundle =
            intent.getBundleExtra(KEY_CONFIG) ?: throw NullPointerException("Bundle is null")
        extensionConfig = BundleCompat.getParcelable(bundle, KEY_CONFIG)
            ?: throw NullPointerException("config is null")
        initView()
    }

    private fun initView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(emptySet())
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        val bundle = Bundle()
        bundle.putParcelable(KEY_CONFIG, extensionConfig)
        navController.setGraph(R.navigation.nav_extension_graph, bundle)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.label == "fragment_extension_manager") {
                supportActionBar?.title = extensionConfig.appName
                supportActionBar?.subtitle = extensionConfig.packageName
            } else {
                supportActionBar?.subtitle = ""
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (navController.currentDestination?.label == "fragment_extension_manager") {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    companion object {
        private const val KEY_CONFIG = "EXTENSION_CONFIG"
        private const val KEY_CONFIG_EDIT = "EXTENSION_CONFIG_EDIT"

        fun startActivity(context: Context, extensionConfig: AssistConfig, isEdit: Boolean = true) {
            val bundle = Bundle()
            bundle.putParcelable(KEY_CONFIG, extensionConfig)
            val intent = Intent(context, ExtensionActivity::class.java).apply {
                putExtra(KEY_CONFIG, bundle)
                putExtra(KEY_CONFIG_EDIT, isEdit)
            }
            context.startActivity(intent)
        }
    }
}