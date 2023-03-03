package me.simpleHook.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.bean.IntentBean
import me.simpleHook.bean.LogBean
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.ActivityRecordDetailBinding
import me.simpleHook.extension.lineFeesItem
import me.simpleHook.extension.showToast
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class RecordDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordDetailBinding
    private val appViewModel by viewModels<AppViewModel>()
    private var currentText = ""
    private var darkMode = false
    private var rawData = ""
    private var cryptResult = ""
    private var returnValue = ""
    private var currentPattern = ""
    private lateinit var printLog: PrintLog

    // private var showReturnValue = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        darkMode = if (OSUtils.atLeastO()) {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } else {
            false
        }
        val recordPackageName = intent.getStringExtra(KEY_PACKAGE_NAME)!!
        supportActionBar?.title =
            if (recordPackageName.startsWith("error")) "Hook Error" else AppUtils.getAppName(this,
                recordPackageName)
        supportActionBar?.subtitle = recordPackageName
        initView()
    }

    private fun initView() {
        binding.progressBar.isVisible = true
        lifecycleScope.launch(Dispatchers.IO) {
            val recordId = intent.getIntExtra(KEY_RECORD_ID, -1)
            printLog = appViewModel.getRecordByID(recordId)
            initData()
        }
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.Main) {
            val logBean = Json.decodeFromString<LogBean>(printLog.log)
            val foreStr = if (LanguageUtils.isNotChinese()) "Type: " else "类型："
            if (logBean.type.equals("intent", ignoreCase = true)) {
                val intentBean = Json.decodeFromString<IntentBean>(logBean.other[0])
                val sb = StringBuilder()
                sb.append("${foreStr + logBean.type}\n")
                    .append("packageName：${intentBean.packageName}\n")
                    .append("className：${intentBean.className}\n")
                    .append("action：${intentBean.action}\n").append("data：${intentBean.data}\n")
                    .append("extras：\n")
                intentBean.extras.forEach {
                    sb.append("   type：${it.type}，key：${it.key}，value：${it.value}\n")
                }
                currentText = sb.toString()
            } else {
                val logList: List<String> = logBean.other
                val sb = StringBuilder()
                logList.forEach {
                    if (it.startsWith("原始数据：") || it.startsWith("Raw Data: ")) {
                        rawData = it.replaceFirst(Regex("""原始数据：|Raw data: """), "")
                    } else if (it.startsWith("加密结果：") || it.startsWith("解密结果：") || it.startsWith("Decrypt result: ") || it.startsWith(
                            "Encrypt result: ")
                    ) {
                        cryptResult =
                            it.replaceFirst(Regex("""加密结果：|解密结果：|Encrypt result: |Decrypt result: """),
                                "")
                        //返回值|参返|Param&Return Value|Return value
                    } else if (it.startsWith("返回值：") || it.startsWith("Return value: ")) {
                        returnValue = it.replaceFirst(Regex("""返回值：|Return value: """), "")
                    }
                    sb.append(it).append("\n")
                }
                val nLine: Int = -1
                currentText = StringBuilder().lineFeesItem(logList,
                    "${foreStr + logBean.type}\n",
                    nLine = nLine,
                    nLineString = "").replace("类：", "  ").replace("方法：", "")
                    .replace("Class : ", "  ").replace("Method : ", "")
            }
            updateView(currentPattern)
            binding.progressBar.isVisible = false
        }
    }


    private fun updateView(keyword: String) {
        val color = if (darkMode) "#9C786C".toColorInt() else Color.RED
        if (GlobalValue.sp.wordWrap) {
            binding.normalRecord.isVisible = true
            binding.wordWrapScrollView.isVisible = false
            binding.normalRecord.apply {
                text = if (keyword.isBlank()) {
                    currentText
                } else {
                    val result = findSearch(currentText, keyword, color)
                    result
                }
            }
            binding.wordWrapRecord.text = ""
        } else {
            binding.wordWrapScrollView.isVisible = true
            binding.normalRecord.isVisible = false
            binding.wordWrapRecord.apply {
                text = if (keyword.isBlank()) {
                    currentText
                } else {
                    val result = findSearch(currentText, keyword, color)
                    result
                }
            }
            binding.normalRecord.text = ""
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record_detail, menu)
        menu.findItem(R.id.menu_word_wrap).isChecked = GlobalValue.sp.wordWrap
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    currentPattern = newText.trim()
                    updateView(currentPattern)
                    return true
                }

            })
        }
        menu.findItem(R.id.copy_raw_data).isVisible = rawData.isNotEmpty()
        menu.findItem(R.id.copy_crypt_result).isVisible = cryptResult.isNotEmpty()
        menu.findItem(R.id.copy_return_value).isVisible = returnValue.isNotEmpty()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            R.id.help -> {
                warningDialog(this,
                    title = "可能出现的问题",
                    message = "加解密过程中byte[]与string转换可能会采用不同的编码，会使获取到的数据乱码，造成结果的不准确")
            }
            R.id.copy_text -> {
                ToolUtils.toClip(this, currentText)
                showToast(getString(R.string.main_home_export_configs_tip))
            }
            R.id.copy_json -> {
                ToolUtils.toClip(this, JsonUtil.formatJson(printLog.log).replace("\\u003e", "-> "))
                showToast(getString(R.string.main_home_export_configs_tip))
            }
            R.id.copy_raw_data -> {
                ToolUtils.toClip(this, rawData)
                showToast(getString(R.string.main_home_export_configs_tip))
            }
            R.id.copy_crypt_result -> {
                ToolUtils.toClip(this, cryptResult)
                showToast(getString(R.string.main_home_export_configs_tip))
            }
            R.id.copy_return_value -> {
                ToolUtils.toClip(this, returnValue)
                showToast(getString(R.string.main_home_export_configs_tip))
            }
            R.id.menu_word_wrap -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.wordWrap = item.isChecked
                updateView(currentPattern)
            }
        }
        return true
    }

    private fun findSearch(text: String, keyword: String, color: Int = Color.RED): SpannableString {
        val spannableString = SpannableString(text)
        val pattern: Pattern = Pattern.compile("(?i)$keyword")
        val matcher: Matcher = pattern.matcher(spannableString)
        while (matcher.find()) {
            val start: Int = matcher.start()
            val end: Int = matcher.end()
            spannableString.setSpan(ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableString
    }

    companion object {
        private const val KEY_PACKAGE_NAME = "PACKAGE_NAME"
        private const val KEY_RECORD_ID = "RECORD_ID"
        fun startActivity(context: Context, packageName: String, id: Int) {
            val intent = Intent(context, RecordDetailActivity::class.java)
            intent.putExtra(KEY_PACKAGE_NAME, packageName)
            intent.putExtra(KEY_RECORD_ID, id)
            context.startActivity(intent)
        }
    }

}