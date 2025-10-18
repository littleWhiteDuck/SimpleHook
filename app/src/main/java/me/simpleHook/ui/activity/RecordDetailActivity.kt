package me.simpleHook.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.base.BaseActivity
import me.simpleHook.database.entity.RecordEntity
import me.simpleHook.databinding.ActivityRecordDetailBinding
import me.simpleHook.extension.showPopup
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.utils.AppUtil
import me.simpleHook.utils.JsonUtil
import me.simpleHook.utils.ThemeModeUtil
import me.simpleHook.utils.ToolUtil
import me.simpleHook.viewmodel.RecordViewModel


class RecordDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityRecordDetailBinding
    private val recordViewModel by viewModels<RecordViewModel>()
    private var currentText = ""
    private var rawData = ""
    private var cryptResult = ""
    private var returnValue = ""
    private var currentPattern = ""
    private lateinit var recordEntity: RecordEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val recordPackageName = intent.getStringExtra(KEY_PACKAGE_NAME)!!
        supportActionBar?.title = AppUtil.getAppName(recordPackageName)
        supportActionBar?.subtitle = recordPackageName
        initView()
        initData()
    }

    private fun initView() {
        binding.editor.colorScheme =
            if (ThemeModeUtil.isDarkMode()) SchemeDarcula() else EditorColorScheme()
        binding.editor.setTextSize(8f)
        binding.progressBar.show()
    }

    private fun initData() {
        lifecycleScope.launch {
            val id = intent.getIntExtra(KEY_RECORD_ID, -1)
            recordViewModel.fetchRecordDetail(id)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            recordViewModel.recordDetail.collect {
                currentText = it.fastJoinToString("\n")
                withContext(Dispatchers.Main) {
                    updateView()
                    binding.progressBar.hide()
                }
            }
        }

    }


    private fun updateView() {
        with(binding.editor) {
            isWordwrap = GlobalValue.sp.wordWrap
            isLineNumberEnabled = GlobalValue.sp.record_line_number
            getComponent(Magnifier::class.java).isEnabled = GlobalValue.sp.record_magnifier_enable
            setText(currentText)
        }
    }


    private fun commitSearch() {
        val searchOptions = SearchOptions(SearchOptions.TYPE_NORMAL, true)
        if (currentPattern.isNotEmpty()) {
            runCatching {
                binding.editor.searcher.search(currentPattern, searchOptions)
            }
        } else {
            binding.editor.searcher.stopSearch()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record_detail, menu)
        menu.findItem(R.id.menu_word_wrap).isChecked = GlobalValue.sp.wordWrap
        menu.findItem(R.id.menu_magnifier).isChecked = GlobalValue.sp.record_magnifier_enable
        menu.findItem(R.id.menu_line_number).isChecked = GlobalValue.sp.record_line_number
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    currentPattern = newText.trim()
                    commitSearch()
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
                warningDialog(
                    this,
                    title = "可能出现的问题",
                    message = "加解密过程中byte[]与string转换可能会采用不同的编码，会使获取到的数据乱码，造成结果的不准确"
                )
            }

            R.id.copy_text -> {
                ToolUtil.toClip(this, currentText)
                showPopup(getString(R.string.main_home_export_configs_tip))
            }

            R.id.copy_json -> {
                ToolUtil.toClip(
                    this,
                    JsonUtil.formatJson(recordEntity.record).replace("\\u003e", "-> ")
                )
                showPopup(getString(R.string.main_home_export_configs_tip))
            }

            R.id.copy_raw_data -> {
                ToolUtil.toClip(this, rawData)
                showPopup(getString(R.string.main_home_export_configs_tip))
            }

            R.id.copy_crypt_result -> {
                ToolUtil.toClip(this, cryptResult)
                showPopup(getString(R.string.main_home_export_configs_tip))
            }

            R.id.copy_return_value -> {
                ToolUtil.toClip(this, returnValue)
                showPopup(getString(R.string.main_home_export_configs_tip))
            }

            R.id.menu_word_wrap -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.wordWrap = item.isChecked
                updateView()
            }

            R.id.menu_line_number -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.record_line_number = item.isChecked
                updateView()
            }

            R.id.menu_magnifier -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.record_magnifier_enable = item.isChecked
                updateView()
            }
        }
        return true
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