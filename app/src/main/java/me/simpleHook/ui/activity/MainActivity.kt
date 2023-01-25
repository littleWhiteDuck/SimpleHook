package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.fragment.ExtensionFragment
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.RecordSummaryFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.util.*


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        initView()
        if (FlavorUtils.isLiteVersion) {
            if (!isModuleLive()) {
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
        } else {
            if (!isModuleLive()) getString(R.string.main_module_not_activated_tip).toast(this)
            if (!FlavorUtils.isNormal()) {
                SuUtil.init(this)
            } else if (!FileUtils.isGrant(this)) {
                requestPermissionDialog(this) {
                    FileUtils.verifyStoragePermissions(this)
                }
            }
        }
        //initUseTip()
        checkUpdate()
        getExternalFilesDir(null)
        super.onCreate(savedInstanceState)
    }

    private fun checkUpdate() {
        lifecycleScope.launch(Dispatchers.Main) {
            val result =
                fetchJson("https://api.github.com/repos/littleWhiteDuck/SimpleHook/releases/latest")
                    ?: return@launch
            val versionName = result.optString("name")
            if (versionName.isNotEmpty() && BuildConfig.VERSION_NAME != versionName) {
                val body = result.optString("body").substringAfterLast("更新记录").trim()
                val message = body.ifEmpty { "有新版本，修复若干bug，请更新" }
                customDialog(
                    this@MainActivity,
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
                    cancelAble = false
                ).show()
            }
        }
    }

/*    @SuppressLint("SetTextI18n", "InflateParams")
    private fun initUseTip() {
        if (sp.termsOfUse) return
        val message = AssetsUtil.getText(this, "agreement")
        val random1 = Random.nextInt(5, 10)
        val random2 = Random.nextInt(10, 15)
        val contentView = layoutInflater.inflate(R.layout.terms_use, null)
        val tvMessage = contentView.findViewById<AppCompatTextView>(R.id.message)
        tvMessage.text = message
        val resultInput = contentView.findViewById<TextInputLayout>(R.id.result_input)
        val resultEdit = contentView.findViewById<TextInputEditText>(R.id.result_edit)
        resultInput.hint = "$random1×$random2 = "
        customDialog(
            this,
            title = "使用协议",
            contentView = contentView,
            okText = getString(R.string.main_agreement_confirm),
            okClick = { dialogInterface ->
                val canCancel = resultEdit.text.toString().trim() == (random1 * random2).toString()
                if (canCancel) {
                    sp.termsOfUse = true
                } else {
                    getString(R.string.main_agreement_tip).toast(this)
                }
                dialogDismiss(dialogInterface, canCancel)
            },
            cancelText = getString(R.string.main_agreement_cancel),
            cancelClick = {
                Process.killProcess(Process.myPid())
            },
            cancelAble = false
        ).show()
    }

    private fun dialogDismiss(dialog: DialogInterface, canCancel: Boolean) {
        try {
            val mShowing: Field =
                dialog.javaClass.superclass!!.superclass!!.getDeclaredField("mShowing")
            mShowing.isAccessible = true
            mShowing.set(dialog, canCancel)
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } */

    private fun initView() {
        binding.apply {
            viewPager.apply {
                adapter = if (FlavorUtils.isLiteVersion) {
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
            if (FlavorUtils.isLiteVersion) {
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