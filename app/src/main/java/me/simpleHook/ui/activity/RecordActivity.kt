package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import me.simpleHook.adapter.RecordAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.compat.BundleCompat
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.extension.dp
import me.simpleHook.extension.showToast
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.util.*
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class RecordActivity : BaseActivity() {
    private val appViewModel by viewModels<AppViewModel>()
    private lateinit var binding: ActivityRecordBinding
    private var isType = false
    private var typeOrPackageName = ""
    private val recordAdapter by lazy {
        RecordAdapter(isType = isType, onItemClick = {
            appViewModel.updateRecord(it.copy(read = true))
            val intent = Intent(this, RecordDetailActivity::class.java)
            intent.putExtra("record_id", it.id)
            intent.putExtra("record_package_name", it.packageName)
            startActivity(intent)
        }, deleteRecord = { printLog ->
            appViewModel.deleteRecordById(printLog.id)
        }, markRecord = { printLog ->
            val tempIsMark = !printLog.isMark
            appViewModel.updateRecord(printLog.copy(isMark = tempIsMark))
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
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        val bundle =
            intent.getBundleExtra("bundle") ?: throw NullPointerException("Bundle  is null")
        val recordSummary: RecordSummary = BundleCompat.getParcelable(bundle, "recordSummary")
            ?: throw NullPointerException("Record summary is null")
        isType = recordSummary.type.isNotEmpty()
        typeOrPackageName = if (isType) recordSummary.type else recordSummary.packageName
        if (isType) {
            supportActionBar?.title =
                if (typeOrPackageName.startsWith("Error")) "Hook Error" else typeOrPackageName
        } else {
            supportActionBar?.apply {
                title =
                    if (typeOrPackageName.startsWith("error")) "Hook Error" else AppUtils.getAppName(
                        this@RecordActivity,
                        typeOrPackageName)
                subtitle = typeOrPackageName
            }
        }
        initView()
        initData()
        initBack()
    }

    private fun initBack() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (appViewModel.queryPattern.value.isNullOrEmpty()) {
                    onBackPressedCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    binding.swipeRefreshLayout.isRefreshing = true
                    appViewModel.queryPattern.value = ""
                    lifecycleScope.launch {
                        appViewModel.getRecord(typeOrPackageName,
                            isType,
                            searchMode = Constant.RECORD_SEARCH_GLOBAL).collectLatest {
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
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
                ) {
                    // Get the position of the view in the recycler view
                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        return
                    }

                    if (position == parent.adapter!!.itemCount - 1) {
                        // Add padding to the last item. You should probably use a @dimen resource.
                        outRect.bottom = 200
                    }
                }
            })
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
                        appViewModel.deleteRecordById(it)
                    }
                }


                private val fontMetrics = Paint.FontMetrics()
                private val swipeBackground = ColorDrawable(Color.LTGRAY)
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
            appViewModel.getRecord(typeOrPackageName,
                isType,
                searchMode = Constant.RECORD_SEARCH_GLOBAL).collectLatest {
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
                            appViewModel.deleteRecordByType(typeOrPackageName)
                        } else {
                            appViewModel.deleteRecordByPack(typeOrPackageName)
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
                            appViewModel.deleteReadRecordByType(type = typeOrPackageName,
                                read = true)
                        } else {
                            appViewModel.deleteReadRecordByPack(packageName = typeOrPackageName,
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
                            appViewModel.deleteReadRecordByType(type = typeOrPackageName,
                                read = false)
                        } else {
                            appViewModel.deleteReadRecordByPack(packageName = typeOrPackageName,
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
                            appViewModel.deleteMarkedRecordByType(isMark = true, typeOrPackageName)
                        } else {
                            appViewModel.deleteMarkedRecordByPack(isMark = true, typeOrPackageName)
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
                            appViewModel.deleteMarkedRecordByType(isMark = false, typeOrPackageName)
                        } else {
                            appViewModel.deleteMarkedRecordByPack(isMark = false, typeOrPackageName)
                        }
                        refreshData()
                    })
            }
            R.id.save_marked_record -> {
                val time = TimeUtil.getDateTime(System.currentTimeMillis(), pattern = "ddHHmmss")
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
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openFileDescriptor(uri, "rwt")?.use { parcel ->
                        val list =
                            if (isType) appViewModel.getMarkedRecordByType(typeOrPackageName) else appViewModel.getMarkedRecordByPack(
                                typeOrPackageName)
                        list.forEach {
                            val content = JsonUtil.formatJson(it.replace("\\u003e", "> "))
                            FileOutputStream(parcel.fileDescriptor).use { output ->
                                output.write(content.toByteArray())
                            }
                        }
                    }
                }
                loadingDialog.dismiss()
                showToast(getString(R.string.record_save_marked_record_tip))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
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
                appViewModel.queryPattern.value = inputView.editText.text.toString().trim()
                val loadingDialog =
                    LoadingDialog(this, getString(R.string.record_loading_tip_searching))
                loadingDialog.show()
                lifecycleScope.launch {
                    appViewModel.getRecord(typeOrPackageName, isType, searchMode).collectLatest {
                        recordAdapter.addOnPagesUpdatedListener {
                            binding.swipeRefreshLayout.isRefreshing = false
                            loadingDialog.dismiss()
                        }
                        recordAdapter.submitData(it)

                    }
                }
                dialogInterface.dismiss()
            },
            cancelText = getString(R.string.dialog_cancel),
            cancelAble = false).show()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

}