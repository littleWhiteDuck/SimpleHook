package me.simpleHook.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.AppUtils
import me.simpleHook.util.lineFeesItem
import java.util.regex.Matcher
import java.util.regex.Pattern


class RecordActivity : BaseActivity() {
    private lateinit var binding: ActivityRecordBinding
    private var currentText = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val bundle = intent.getBundleExtra("bundle")
        val printLog: PrintLog = bundle!!.getParcelable("printLog")!!
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
        supportActionBar?.title = AppUtils.getAppName(this@RecordActivity, logBean.packageName)
        supportActionBar?.subtitle = logBean.packageName
        val stackTraces: List<String> = logBean.other as List<String>
        val sb = StringBuilder()
        stackTraces.forEach {
            sb.append(it).append("\n")
        }
        val typeOne = "Toast|PopupWindow|弹窗|点击事件".contains(logBean.type)
        val typeTwo = "SHA1|SHA-1|SHA-224|SHA-256|SHA-384|SHA-512|base64|MD5".contains(logBean.type)
        val typeThree = logBean.type.startsWith("AES", ignoreCase = true)
        val nLine: Int = when {
            typeOne -> 0
            typeTwo -> 3
            typeThree -> 6
            else -> -1
        }
        currentText = StringBuilder().lineFeesItem(
            stackTraces, "类型：${logBean.type}\n", nLine = nLine, nLineString =
            "调用堆栈：\n"
        )
            .replace("类：", "  ")
            .replace("方法：", "")
        binding.record.text = currentText
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val keyword = newText?.trim() ?: ""
                    binding.record.text = findSearch(Color.RED, currentText, keyword)
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
        }
        return true
    }

    private fun findSearch(color: Int, text: String, keyword: String): SpannableString {
        val spannableString = SpannableString(text)
        val pattern: Pattern = Pattern.compile("(?i)$keyword")
        val matcher: Matcher = pattern.matcher(spannableString)
        while (matcher.find()) {
            val start: Int = matcher.start()
            val end: Int = matcher.end()
            spannableString.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }
}