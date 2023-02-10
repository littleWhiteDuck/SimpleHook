package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.bean.ConfigItem
import me.simpleHook.compat.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.ActivityImportConfigBinding
import me.simpleHook.ui.fragment.ConfigDialogFragment
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ImportConfigActivity : AppCompatActivity() {
    private val configSystem = ConfigSystemUtil.getConfigSystem()
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
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val appConfig = Json.decodeFromString<AppConfig>(configs)
                        appConfig.id = 0
                        viewModel.insertConfigs(appConfig)
                        configSystem.saveCustomConfig(appConfig.packageName, configs)
                    }.onFailure {
                        Looper.prepare()
                        getString(R.string.main_home_import_incorrect_format_tip).toast(this@ImportConfigActivity)
                        Looper.loop()
                    }
                }
            }
            else -> getString(R.string.main_home_import_incorrect_format_tip).toast(this)
        }
    }

}