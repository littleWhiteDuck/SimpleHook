package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.ActivityImportConfigBinding
import me.simpleHook.ui.fragment.ConfigDialogFragment
import me.simpleHook.util.FileUtils
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ImportConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImportConfigBinding
    private val viewModel: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initReceiveFile()
    }

    private fun initReceiveFile() {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.apply {
                val configs = readTextFromUri(this)
                Log.d("littleWhiteDuck", "initReceiveFile: $configs")
                importConfigs(configs)
            }

        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            "error".toast(this)
        }

        return stringBuilder.toString()
    }

    private fun importConfigs(configs: String) {
        when {
            JsonUtil.isJsonArray(configs) -> {
                val dataList = JsonUtil.importConfigs(configs)
                if (dataList.isEmpty()) {
                    getString(R.string.main_home_import_incorrect_format_tip).toast(this)
                    return
                } else {
                    ConfigDialogFragment(
                        dataList as ArrayList<ConfigItem>, Constant.CONFIG_IMPORT_MODE
                    ).show(supportFragmentManager, "from text import")
                }
            }
            JsonUtil.isJsonObject(configs) -> {
                try {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val appConfig = Gson().fromJson(configs, AppConfig::class.java)
                        appConfig.id = 0
                        viewModel.insertConfigs(appConfig)
                        FileUtils.saveConfig(
                            this@ImportConfigActivity,
                            appConfig.packageName,
                            Constant.APP_CONFIG_NAME,
                            configs
                        )
                    }
                } catch (e: java.lang.Exception) {
                    getString(R.string.main_home_import_incorrect_format_tip).toast(this)
                }
            }
            else -> getString(R.string.main_home_import_incorrect_format_tip).toast(this)
        }
    }

}