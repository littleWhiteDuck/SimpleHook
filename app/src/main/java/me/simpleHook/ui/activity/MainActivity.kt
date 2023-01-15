package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.databinding.ActivityMainBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.fragment.ExtensionFragment
import me.simpleHook.ui.fragment.HomeFragment
import me.simpleHook.ui.fragment.RecordSummaryFragment
import me.simpleHook.ui.fragment.SettingsFragment
import me.simpleHook.util.*
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import kotlin.random.Random


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sp by lazy { SPUtils(this) }
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            uri?.also {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        initView()
        if (!isModuleLive()) getString(R.string.main_module_not_activated_tip).toast(this)
        if (FlavorUtils.isNormal()) {
            if (!FileUtils.isGrant(this)) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    requestPermissionDialog(this) {
                        val document = DocumentFile.fromTreeUri(
                            this, Uri.parse(Constant.ANDROID_DATA_URI)
                        )
                        startActivityForData.launch(
                            document?.uri ?: Uri.parse(Constant.ANDROID_DATA_URI)
                        )
                    }
                } else {
                    requestPermissionDialog(this) {
                        FileUtils.verifyStoragePermissions(this)
                    }
                }
            }
        } else {
            SuUtil.init(this)
        }
        //initUseTip()
        val file = File(getExternalFilesDir("")!!.path + "/logTemp/log.txt")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        checkUpdate()
        super.onCreate(savedInstanceState)
    }

    private fun checkUpdate() {
        if (BuildConfig.VERSION_NAME.contains("beta")) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result =
                    JSONObject(URL("https://api.github.com/repos/littleWhiteDuck/SimpleHook/releases/latest").readText())
                val versionName = result.optString("name")
                if (versionName.isNotEmpty() && BuildConfig.VERSION_NAME.replace(
                        Regex("""_root|_beta"""), ""
                    ) != versionName
                ) {
                    val body = result.optString("body").substringAfterLast("更新记录")
                    val message = body.ifEmpty { "有新版本，修复若干bug，请更新" }
                    Looper.prepare()
                    customDialog(
                        this@MainActivity,
                        title = "新版本 $versionName",
                        message = message,
                        okText = "更新",
                        okClick = {
                            val intent = Intent(Intent.ACTION_VIEW).also {
                                it.data =
                                    Uri.parse("https://github.com/littleWhiteDuck/SimpleHook/releases/latest")
                            }
                            startActivity(intent)
                        },
                        cancelText = "取消",
                        cancelClick = {
                            it.dismiss()
                        },
                        cancelAble = false
                    ).show()
                    Looper.loop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun initUseTip() {
        if (sp.termsOfUse) return
        val message = AssetsUtil.getText(this, "terms_of_use")
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
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount() = 4

                    override fun createFragment(position: Int) = when (position) {
                        0 -> HomeFragment()
                        1 -> ExtensionFragment()
                        2 -> RecordSummaryFragment()
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
                    R.id.recordFragment -> setCurrentItem(2)
                    R.id.settingsFragment -> setCurrentItem(3)
                }
                true
            }
        }
    }

    @Keep
    private fun isModuleLive() = false


}