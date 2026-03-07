package me.simpleHook.feature.record.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.drakeet.multitype.MultiTypeAdapter
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.databinding.ActivityRecordDetailBinding
import me.simpleHook.core.extension.showPopup
import me.simpleHook.feature.record.ui.adapter.RecordDetailAdapter
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.JsonUtil
import me.simpleHook.core.utils.ThemeModeUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.feature.record.viewmodel.RecordViewModel


class RecordDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityRecordDetailBinding
    private val recordViewModel by viewModels<RecordViewModel>()
    private var currentText = ""
    private var currentPattern = ""
    private lateinit var recordEntity: RecordEntity
    private val recordAdapter = MultiTypeAdapter()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var tempCodeStyle = false
    private var tempText = ""

    private val fadeTransition = Fade().apply {
        duration = 300
    }

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
        initBack()
    }

    private fun initView() {
        binding.progressBar.show()

        with(binding.editor) {
            colorScheme =
                if (ThemeModeUtil.isDarkMode()) SchemeDarcula() else EditorColorScheme()
            setTextSize(14f)
        }

        with(binding.recyclerView) {
            isNestedScrollingEnabled = true
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(this@RecordDetailActivity)
        }

        with(binding) {
            editor.isVisible = !GlobalValue.sp.recordCardStyle
            recyclerView.isVisible = GlobalValue.sp.recordCardStyle
        }

        recordAdapter.register(RecordDetailAdapter { recordDetailItem ->
            tempText = recordDetailItem.fullContent
            tempCodeStyle = true
            switchDetailStyle()
        })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initData() {
        val id = intent.getIntExtra(KEY_RECORD_ID, -1)
        recordViewModel.fetchRecordDetail(id)

        recordViewModel.recordDetailItems.observe(this) {
            if (it.isNotEmpty()) {
                recordAdapter.items = it
                recordAdapter.notifyDataSetChanged()
                binding.progressBar.hide()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            recordViewModel.recordDetail.collect {
                if (it.isNotEmpty()) {
                    currentText = it.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        updateEditorView()
                        binding.progressBar.hide()
                    }
                }
            }
        }

    }

    private fun initBack() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tempCodeStyle) {
                    tempCodeStyle = false
                    switchDetailStyle()
                } else {
                    onBackPressedCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }


    private fun updateEditorView() {
        with(binding.editor) {
            isWordwrap = GlobalValue.sp.wordWrap
            isLineNumberEnabled = GlobalValue.sp.record_line_number
            getComponent(Magnifier::class.java).isEnabled = GlobalValue.sp.record_magnifier_enable
            setText(if (tempCodeStyle) tempText else currentText)
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
        val cardStyle = GlobalValue.sp.recordCardStyle

        menuInflater.inflate(R.menu.menu_record_detail, menu)
        menu.findItem(R.id.menu_word_wrap).isChecked = GlobalValue.sp.wordWrap
        menu.findItem(R.id.menu_magnifier).isChecked = GlobalValue.sp.record_magnifier_enable
        menu.findItem(R.id.menu_line_number).isChecked = GlobalValue.sp.record_line_number
        menu.findItem(R.id.menu_card_style).isChecked = cardStyle
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


        menu.findItem(R.id.menu_word_wrap).isVisible = tempCodeStyle || !cardStyle
        menu.findItem(R.id.menu_magnifier).isVisible = tempCodeStyle || !cardStyle
        menu.findItem(R.id.menu_line_number).isVisible = tempCodeStyle || !cardStyle
        menu.findItem(R.id.menu_word_wrap).isVisible = tempCodeStyle || !cardStyle
        menu.findItem(R.id.search).isVisible = tempCodeStyle || !cardStyle
        menu.findItem(R.id.menu_card_style).isVisible = !tempCodeStyle


        return super.onCreateOptionsMenu(menu)
    }

    private fun switchDetailStyle() {
        TransitionManager.beginDelayedTransition(binding.root, fadeTransition)

        if (tempCodeStyle || !GlobalValue.sp.recordCardStyle) {
            binding.editor.isInvisible = false
            binding.recyclerView.isInvisible = true

            updateEditorView()
        } else {
            binding.editor.isInvisible = true
            binding.recyclerView.isInvisible = false
        }

        invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()

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

            R.id.menu_card_style -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.recordCardStyle = item.isChecked
                switchDetailStyle()
            }

            R.id.menu_word_wrap -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.wordWrap = item.isChecked
                updateEditorView()
            }

            R.id.menu_line_number -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.record_line_number = item.isChecked
                updateEditorView()
            }

            R.id.menu_magnifier -> {
                item.isChecked = !item.isChecked
                GlobalValue.sp.record_magnifier_enable = item.isChecked
                updateEditorView()
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
