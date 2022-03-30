package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.bean.LogBean2
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.ActivityRecordDetailBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class RecordDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityRecordDetailBinding
    private var currentText = ""
    private lateinit var jsonText: String
    private var darkMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        darkMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } else {
            false
        }
        val bundle = intent.getBundleExtra("bundle")
        val printLog: PrintLog = bundle!!.getParcelable("printLog")!!
        jsonText = printLog.log
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
        supportActionBar?.title =
            if (printLog.packageName == Constant.SIMPLE_HOOK_ERROR) "SimpleHook" else AppUtils.getAppName(
                this@RecordDetailActivity, logBean.packageName
            )
        supportActionBar?.subtitle = logBean.packageName
        val foreStr = if (LanguageUtils.isNotChinese()) "Type: " else "类型："
        if (logBean.type.equals("intent", ignoreCase = true)) {
            val logBean2 = Gson().fromJson(printLog.log, LogBean2::class.java)
            val intentBean = logBean2.other[0]
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
            val logList: List<String> = logBean.other as List<String>
            val sb = StringBuilder()
            logList.forEach {
                sb.append(it).append("\n")
            }
            val nLine: Int = -1
            currentText = StringBuilder().lineFeesItem(
                logList, "${foreStr + logBean.type}\n", nLine = nLine, nLineString = ""
            ).replace("类：", "  ").replace("方法：", "").replace("Class : ", "  ")
                .replace("Method : ", "")
        }
        initView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {
        binding.record.text = currentText
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record_detail, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val keyword = newText?.trim() ?: ""
                    val color = if (darkMode) "#9C786C".toColorInt() else Color.RED
                    val result = findSearch(currentText, keyword, color)
                    binding.record.text = result
                    return true
                }

            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.help -> {
                warningDialog(
                    this,
                    title = "可能出现的问题",
                    message = "加解密过程中byte[]与string转换可能会采用不同的编码，会使获取到的数据乱码，造成结果的不准确"
                )
            }
            R.id.copy_text -> {
                ToolUtils.toClip(this, currentText)
                getString(R.string.main_home_export_configs_tip).toast(this)
            }
            R.id.copy_json -> {
                ToolUtils.toClip(this, JsonUtil.formatJson(jsonText).replace("\\u003e", "-> "))
                getString(R.string.main_home_export_configs_tip).toast(this)
            }
        }
        return true
    }

/*     private fun findSearch(text: String, keyword: String, color: String = "red"): String {
         if (keyword.isEmpty()) return text
         var tempText = text
         if (tempText.contains(Regex("(?i)$keyword"))) {
             tempText =
                 tempText.replace(Regex("(?i)$keyword"), "<font color = \"$color\">$0</font>")
         }
         return tempText
     }*/

    private fun findSearch(text: String, keyword: String, color: Int = Color.RED): SpannableString {
        val spannableString = SpannableString(text)
        val pattern: Pattern = Pattern.compile("(?i)$keyword")
        val matcher: Matcher = pattern.matcher(spannableString)
        while (matcher.find()) {
            val start: Int = matcher.start()
            val end: Int = matcher.end()
            spannableString.setSpan(
                ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

}