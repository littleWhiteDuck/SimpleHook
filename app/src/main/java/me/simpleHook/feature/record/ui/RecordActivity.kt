package me.simpleHook.feature.record.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.core.base.BaseActivity
import me.simpleHook.core.constant.Constant
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.core.extension.dp
import me.simpleHook.core.extension.showPopup
import me.simpleHook.feature.record.ui.adapter.RecordAdapter
import me.simpleHook.core.ui.custom.LoadingDialog
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.custom.warningDialog
import me.simpleHook.core.ui.view.edit.InputView
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.FastScrollerUtil
import me.simpleHook.core.utils.JsonUtil
import me.simpleHook.core.utils.TimeUtil
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.feature.record.viewmodel.RecordViewModel
import java.io.FileOutputStream


class RecordActivity : BaseActivity() {
    private val recordViewModel by viewModels<RecordViewModel>()
    private lateinit var binding: ActivityRecordBinding
    private var isType = false
    private var typeOrPackageName = ""
    private val recordAdapter by lazy {
        RecordAdapter(isType = isType, onItemClick = {
            markRecordAsReadIfNeeded(it)
            RecordDetailActivity.startActivity(this, it.packageName, it.id)
        }, deleteRecord = { recordEntity ->
            recordViewModel.deleteRecordById(recordEntity.id)
        }, markRecord = { recordEntity ->
            toggleRecordMark(recordEntity)
        }, onItemLongClick = { recordEntity ->
            toggleRecordMark(recordEntity)
        })
    }
    private val saveMarkedRecord =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/json")) { resultUri ->
            resultUri?.apply {
                saveMarkedRecord(this)
            }
        }

    private val swipeDeleteIcon by lazy {
        ContextCompat.getDrawable(this, R.drawable.ic_delete_black_24)
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var recordCollectJob: Job? = null
    private var searchLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isType = intent.getStringExtra(KEY_PACKAGE_NAME) == null
        typeOrPackageName = if (isType) {
            intent.getStringExtra(KEY_TYPE) ?: throw NullPointerException("Type is null")
        } else {
            intent.getStringExtra(KEY_PACKAGE_NAME)
                ?: throw NullPointerException("PackageName is null")
        }
        updateTitle()
        initView()
        initData()
        initBack()
    }

    private fun updateTitle() {
        if (isType) {
            supportActionBar?.title =
                if (typeOrPackageName.startsWith("Error")) getString(R.string.record_type_error) else typeOrPackageName
        } else {
            supportActionBar?.title =
                if (typeOrPackageName.startsWith("error")) getString(R.string.record_type_error) else AppUtil.getAppName(
                    typeOrPackageName
                )
            supportActionBar?.subtitle = typeOrPackageName
        }
    }

    private fun initBack() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (recordViewModel.queryPattern.value.isEmpty()) {
                    onBackPressedCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    binding.swipeRefreshLayout.isRefreshing = true
                    recordViewModel.queryPattern.value = ""
                    updateTitle()
                    collectRecordPaging()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        val layoutParams = binding.search.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            var maybeABug = 0
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture = navigationInsets.bottom <= 20 * resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(binding.root, windowInsets)
            maybeABug += if (navigationInsets.bottom == 0) 20.dp else 10.dp
            layoutParams.bottomMargin =
                if (isGesture) maybeABug + navigationInsets.bottom else maybeABug + navigationInsets.bottom
            binding.search.layoutParams = layoutParams
            windowInsets
        }
        binding.recyclerView.apply {
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(this@RecordActivity)
            setPadding(0, 0, 0, 40.dp)
            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun getMovementFlags(
                    recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val swipeFlags = ItemTouchHelper.RIGHT
                    return makeMovementFlags(0, swipeFlags)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    (viewHolder as RecordAdapter.ViewHolder).id?.let {
                        recordViewModel.deleteRecordById(it)
                    }
                }


                private val fontMetrics = Paint.FontMetrics()
                private val swipeBackground = Color.LTGRAY.toDrawable()
                private val swipeText = getString(R.string.record_item_swipe_delete_tip)
                private val swipeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
                    it.color = Color.BLACK
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive)
                    val itemView = viewHolder.itemView
                    val iconMargin = (itemView.height - swipeDeleteIcon!!.intrinsicHeight) / 2
                    val iconLeft: Int
                    var iconRight = 0
                    val iconBottom: Int
                    val backLeft: Int
                    val backRight: Int
                    val backTop: Int = itemView.top
                    val backBottom: Int = itemView.bottom
                    val iconTop: Int =
                        itemView.top + (itemView.height - swipeDeleteIcon!!.intrinsicHeight) / 2
                    iconBottom = iconTop + swipeDeleteIcon!!.intrinsicHeight
                    if (dX > 0) {
                        backLeft = itemView.left
                        backRight = itemView.left + dX.toInt()
                        swipeBackground.setBounds(backLeft, backTop, backRight, backBottom)
                        iconLeft = itemView.left + iconMargin
                        iconRight = iconLeft + swipeDeleteIcon!!.intrinsicWidth
                        swipeDeleteIcon!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    } else {
                        swipeBackground.setBounds(0, 0, 0, 0)
                        swipeDeleteIcon!!.setBounds(0, 0, 0, 0)
                    }
                    swipeBackground.draw(c)
                    swipeDeleteIcon!!.draw(c)
                    if (iconRight != 0) {
                        swipeTextPaint.textSize = itemView.height / 3f
                        swipeTextPaint.getFontMetrics(fontMetrics)
                        val offsetFix =
                            (fontMetrics.descent - fontMetrics.ascent - fontMetrics.bottom + fontMetrics.top) * 1.5f
                        c.drawText(swipeText,
                            iconRight.toFloat(),
                            itemView.top.toFloat() + itemView.height / 2 + (fontMetrics.descent - fontMetrics.ascent) / 2 + offsetFix,
                            swipeTextPaint)
                    }

                }

            }).attachToRecyclerView(binding.recyclerView)
        }
        binding.swipeRefreshLayout.isRefreshing = true
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        recordAdapter.addOnPagesUpdatedListener {
            binding.progressBar.isVisible = false
            binding.swipeRefreshLayout.isRefreshing = false
            searchLoadingDialog?.dismiss()
            searchLoadingDialog = null
        }
        FastScrollerUtil.bind(binding.recyclerView)
        binding.search.setOnClickListener { showSearchDialog() }
    }


    private fun initData() {
        collectRecordPaging()
    }

    private fun collectRecordPaging() {
        recordCollectJob?.cancel()
        recordCollectJob = lifecycleScope.launch {
            recordViewModel.getRecordEntity(typeOrPackageName, isType).collectLatest {
                recordAdapter.submitData(it)
            }
        }
    }

    private fun markRecordAsReadIfNeeded(recordEntity: SmallRecordEntity) {
        if (!recordEntity.isRead) {
            recordViewModel.updateRecordReadById(recordEntity.id, true)
        }
    }

    private fun toggleRecordMark(recordEntity: SmallRecordEntity) {
        recordViewModel.updateRecordMarkById(recordEntity.id, !recordEntity.isMark)
    }

    private fun refreshData(delayTime: Long = 500) {
        Handler(Looper.getMainLooper()).postDelayed({
            recordAdapter.refresh()
        }, delayTime)
        readFileLogInsert()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete_all -> {
                warningDialog(this,
                    title = getString(R.string.record_warning_dialog_title),
                    message = getString(R.string.record_waring_dialog_message_delete_all),
                    okClick = {
                        if (isType) {
                            recordViewModel.deleteRecordByType(typeOrPackageName)
                        } else {
                            recordViewModel.deleteRecordByPack(typeOrPackageName)
                        }
                        refreshData()
                    })
            }
            R.id.delete_read -> {
                warningDialog(this,
                    title = getString(R.string.record_warning_dialog_title),
                    message = getString(R.string.record_waring_dialog_message_read),
                    okClick = {
                        if (isType) {
                            recordViewModel.deleteReadRecordByType(type = typeOrPackageName,
                                read = true)
                        } else {
                            recordViewModel.deleteReadRecordByPack(packageName = typeOrPackageName,
                                read = true)
                        }
                        refreshData()
                    })
            }
            R.id.delete_un_read -> {
                warningDialog(this,
                    title = getString(R.string.record_warning_dialog_title),
                    message = getString(R.string.record_waring_dialog_message_unread),
                    okClick = {
                        if (isType) {
                            recordViewModel.deleteReadRecordByType(type = typeOrPackageName,
                                read = false)
                        } else {
                            recordViewModel.deleteReadRecordByPack(packageName = typeOrPackageName,
                                read = false)
                        }
                        refreshData()
                    })
            }
            R.id.delete_mark -> {
                warningDialog(this,
                    title = getString(R.string.record_warning_dialog_title),
                    message = getString(R.string.record_waring_dialog_message_marked),
                    okClick = {
                        if (isType) {
                            recordViewModel.deleteMarkedRecordByType(isMark = true, typeOrPackageName)
                        } else {
                            recordViewModel.deleteMarkedRecordByPack(isMark = true, typeOrPackageName)
                        }
                        refreshData()
                    })
            }
            R.id.delete_not_mark -> {
                warningDialog(this,
                    title = getString(R.string.record_warning_dialog_title),
                    message = getString(R.string.record_warning_dialog_message_unmarked),
                    okClick = {
                        if (isType) {
                            recordViewModel.deleteMarkedRecordByType(isMark = false, typeOrPackageName)
                        } else {
                            recordViewModel.deleteMarkedRecordByPack(isMark = false, typeOrPackageName)
                        }
                        refreshData()
                    })
            }
            R.id.save_marked_record -> {
                val time = TimeUtil.getTime(System.currentTimeMillis(), pattern = "ddHHmmss")
                saveMarkedRecord.launch("simpleHook_record_$time.json")
            }
            R.id.search_by_raw_data -> {
                showSearchDialog(searchMode = Constant.RECORD_SEARCH_RAW_DATA)
            }
            R.id.search_by_result -> {
                showSearchDialog(searchMode = Constant.RECORD_SEARCH_RESULT)
            }
        }
        return true
    }

    private fun saveMarkedRecord(uri: Uri) {
        val loadingDialog =
            LoadingDialog(this, getString(R.string.record_loading_saving_marked_record))
        loadingDialog.show()
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                contentResolver.openFileDescriptor(uri, "rwt")?.use { parcel ->
                    val list =
                        if (isType) recordViewModel.getMarkedRecordByType(typeOrPackageName) else recordViewModel.getMarkedRecordByPack(
                            typeOrPackageName
                        )
                    FileOutputStream(parcel.fileDescriptor).bufferedWriter().use { writer ->
                        list.forEach {
                            val content = JsonUtil.formatJson(it.replace("\\u003e", "> "))
                            writer.appendLine(content)
                        }
                    }
                }
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                if (result) {
                    showPopup(getString(R.string.record_save_marked_record_tip))
                }
            }
        }

    }

    private fun showSearchDialog(searchMode: Int = Constant.RECORD_SEARCH_GLOBAL) {
        val inputView = InputView(this)
        inputView.editText.apply {
            isSingleLine = false
            minLines = 3
            maxLines = 10
            setHorizontallyScrolling(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = getString(R.string.main_home_toolbar_search_hint)
        }
        val fastSearchSwitch = MaterialSwitch(this).apply {
            text = getString(R.string.record_search_fast_switch)
            isChecked = recordViewModel.fastSearchEnabled.value
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16.dp, 8.dp, 16.dp, 0)
        }
        val fastSearchTip = TextView(this).apply {
            text = getString(R.string.record_search_fast_tip)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16.dp, 0, 16.dp, 0)
            alpha = 0.8f
        }
        val contentView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8.dp, 0, 8.dp)
            addView(inputView)
            addView(fastSearchSwitch)
            addView(fastSearchTip)
        }
        val scrollContainer = ScrollView(this).apply {
            isFillViewport = true
            addView(
                contentView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        customDialog(this,
            title = getString(R.string.record_search_dialog_title),
            contentView = scrollContainer,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                recordViewModel.queryPattern.value = inputView.editText.text.toString().trim()
                recordViewModel.fastSearchEnabled.value = fastSearchSwitch.isChecked
                if (recordViewModel.queryPattern.value.isNotEmpty()) {
                    supportActionBar?.title = recordViewModel.queryPattern.value
                    supportActionBar?.subtitle = ""
                } else {
                    updateTitle()
                }
                searchLoadingDialog?.dismiss()
                searchLoadingDialog =
                    LoadingDialog(this, getString(R.string.record_loading_tip_searching)).also {
                        it.show()
                    }
                collectRecordPaging()
                dialogInterface.dismiss()
            }, cancelText = getString(R.string.dialog_cancel), cancelAble = false).show()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onDestroy() {
        recordCollectJob?.cancel()
        searchLoadingDialog?.dismiss()
        searchLoadingDialog = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_PACKAGE_NAME = "PACKAGE_NAME"
        private const val KEY_TYPE = "TYPE"
        fun startActivity(context: Context, packageName: String?, type: String?) {
            val intent = Intent(context, RecordActivity::class.java)
            intent.putExtra(KEY_PACKAGE_NAME, packageName)
            intent.putExtra(KEY_TYPE, type)
            context.startActivity(intent)
        }
    }

}
