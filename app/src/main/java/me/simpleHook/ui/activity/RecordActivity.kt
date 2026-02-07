package me.simpleHook.ui.activity

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
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.base.BaseActivity
import me.simpleHook.constant.Constant
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.extension.dp
import me.simpleHook.extension.showPopup
import me.simpleHook.recyclerview.adapter.RecordAdapter
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.utils.AppUtil
import me.simpleHook.utils.FastScrollerUtil
import me.simpleHook.utils.JsonUtil
import me.simpleHook.utils.TimeUtil
import me.simpleHook.viewmodel.RecordViewModel
import java.io.FileOutputStream


class RecordActivity : BaseActivity() {
    private val recordViewModel by viewModels<RecordViewModel>()
    private lateinit var binding: ActivityRecordBinding
    private var isType = false
    private var typeOrPackageName = ""
    private val recordAdapter by lazy {
        RecordAdapter(isType = isType, onItemClick = {
//            if (!it.isRead) recordViewModel.updateRecord(it.copy(isRead = true))
            RecordDetailActivity.startActivity(this, it.packageName, it.id)
        }, deleteRecord = { recordEntity ->
            recordViewModel.deleteRecordById(recordEntity.id)
        }, markRecord = { recordEntity ->
//            recordViewModel.updateRecord(recordEntity.copy(isMark = !recordEntity.isMark))
        }, onItemLongClick = { recordEntity ->
//            recordViewModel.updateRecord(recordEntity.copy(isMark = !recordEntity.isMark))
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
                if (typeOrPackageName.startsWith("Error")) "Hook Error" else typeOrPackageName
        } else {
            supportActionBar?.title =
                if (typeOrPackageName.startsWith("error")) "Hook Error" else AppUtil.getAppName(
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
                    lifecycleScope.launch {
                        recordViewModel.getRecordEntity(typeOrPackageName, isType).collectLatest {
                            recordAdapter.addOnPagesUpdatedListener {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.swipeRefreshLayout.isRefreshing = false
                                }, 800)
                            }
                            recordAdapter.submitData(it)
                        }
                    }
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
        FastScrollerUtil.bind(binding.recyclerView)
        binding.search.setOnClickListener { showSearchDialog() }
    }


    private fun initData() {
        lifecycleScope.launch {
            recordViewModel.getRecordEntity(typeOrPackageName, isType).collectLatest {
                recordAdapter.addOnPagesUpdatedListener {
                    binding.progressBar.isVisible = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.swipeRefreshLayout.isRefreshing = false
                    }, 500)
                }
                recordAdapter.submitData(it)
            }
        }
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
                    list.forEach {
                        val content = JsonUtil.formatJson(it.replace("\\u003e", "> "))
                        FileOutputStream(parcel.fileDescriptor).use { output ->
                            output.write(content.toByteArray())
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
        customDialog(this,
            title = getString(R.string.record_search_dialog_title),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = { dialogInterface ->
                recordViewModel.queryPattern.value = inputView.editText.text.toString().trim()
                if (recordViewModel.queryPattern.value.isNotEmpty()) {
                    supportActionBar?.title = recordViewModel.queryPattern.value
                    supportActionBar?.subtitle = ""
                }
                val loadingDialog =
                    LoadingDialog(this, getString(R.string.record_loading_tip_searching))
                loadingDialog.show()
                lifecycleScope.launch {
                    recordViewModel.getRecordEntity(typeOrPackageName, isType).collectLatest {
                        recordAdapter.addOnPagesUpdatedListener {
                            binding.swipeRefreshLayout.isRefreshing = false
                            loadingDialog.dismiss()
                        }
                        recordAdapter.submitData(it)

                    }
                }
                dialogInterface.dismiss()
            }, cancelText = getString(R.string.dialog_cancel), cancelAble = false).show()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
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