package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.RecordAdapter
import me.simpleHook.bean.RecordSummary
import me.simpleHook.database.AppViewModel
import me.simpleHook.databinding.ActivityRecordBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.custom.LoadingDialog
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*

class RecordActivity : BaseActivity() {
    private val appViewModel by viewModels<AppViewModel>()
    private lateinit var binding: ActivityRecordBinding
    private var isType = false
    private var typeOrPackageName = ""
    private val recordAdapter by lazy {
        RecordAdapter(isType = isType, onItemClick = {
            appViewModel.updateRecord(it.copy(read = true))
            val bundle = Bundle()
            bundle.putParcelable("printLog", it)
            val intent = Intent(this, RecordDetailActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        }, deleteRecord = { printLog ->
            appViewModel.deleteRecordById(printLog.id)
        }, markRecord = { printLog ->
            val tempIsMark = !printLog.isMark
            appViewModel.updateRecord(printLog.copy(isMark = tempIsMark))
        })
    }
    private val assistConfigs by lazy { appViewModel.getAssistConfigs() }
    private val configs by lazy { appViewModel.getConfigs() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)
        val bundle = intent.getBundleExtra("bundle")
        val recordSummary: RecordSummary = bundle!!.getParcelable("recordSummary")!!
        isType = recordSummary.type.isNotEmpty()
        typeOrPackageName = if (isType) recordSummary.type else recordSummary.packageName
        if (isType) {
            supportActionBar?.title =
                if (typeOrPackageName.startsWith("Error")) "Hook Error" else typeOrPackageName
        } else {
            supportActionBar?.apply {
                title =
                    if (typeOrPackageName.startsWith("error")) "Hook Error" else AppUtils.getAppName(
                        this@RecordActivity, typeOrPackageName
                    )
                subtitle = typeOrPackageName
            }
        }
        initView()
        initData()
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
            /*ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    (viewHolder as RecordAdapter.ViewHolder).id?.let {
                        appViewModel.deleteRecordById(it)
                    }
                }

            }).attachToRecyclerView(binding.recyclerView)*/
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
            appViewModel.getRecord(typeOrPackageName, isType).collectLatest {
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

    private fun readFileLogInsert() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (FlavorUtils.isNormal()) {
                assistConfigs.forEach {
                    val list = FileUtils.readLogFile(this@RecordActivity, it.packageName)
                    appViewModel.insertRecord(*list.toTypedArray())
                }
                configs.forEach {
                    val list = FileUtils.readLogFile(this@RecordActivity, it.packageName)
                    appViewModel.insertRecord(*list.toTypedArray())
                }
            } else {
                val list = FileUtils.readLogFile()
                appViewModel.insertRecord(*list.toTypedArray())
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_record, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.delete_all -> {
                warningDialog(this, title = "警告", message = "你是否确定删除所有记录？", okClick = {
                    if (isType) {
                        appViewModel.deleteRecordByType(typeOrPackageName)
                    } else {
                        appViewModel.deleteRecordByPack(typeOrPackageName)
                    }
                    refreshData()
                })
            }
            R.id.delete_read -> {
                warningDialog(this, title = "警告", message = "你是否确定删除所有已读记录？", okClick = {
                    if (isType) {
                        appViewModel.deleteReadRecordByType(type = typeOrPackageName)
                    } else {
                        appViewModel.deleteReadRecordByPack(packageName = typeOrPackageName)
                    }
                    refreshData()
                })
            }
            R.id.delete_mark -> {
                warningDialog(this, title = "警告", message = "你是否确定删除所有标记过的记录？", okClick = {
                    if (isType) {
                        appViewModel.deleteMarkedRecordByType(isMark = true, typeOrPackageName)
                    } else {
                        appViewModel.deleteMarkedRecordByPack(isMark = true, typeOrPackageName)
                    }
                    refreshData()
                })
            }
            R.id.delete_not_mark -> {
                warningDialog(this, title = "警告", message = "你是否确定删除所有未标记的记录？", okClick = {
                    if (isType) {
                        appViewModel.deleteMarkedRecordByType(isMark = false, typeOrPackageName)
                    } else {
                        appViewModel.deleteMarkedRecordByPack(isMark = false, typeOrPackageName)
                    }
                    refreshData()
                })
            }
        }
        return true
    }

    private fun showSearchDialog() {
        val textInputLayout = TextInputLayout(this)
        val textInput = TextInputEditText(this)
        textInput.background = null
        textInputLayout.addView(textInput)
        customDialog(
            this,
            title = "查询",
            contentView = textInputLayout,
            okText = "确认",
            okClick = { dialogInterface ->
                appViewModel.queryPattern.value = textInput.text.toString().trim()
                val loadingDialog = LoadingDialog(this, "正在搜索中")
                loadingDialog.show()
                lifecycleScope.launch {
                    appViewModel.getRecord(typeOrPackageName, isType).collectLatest {
                        recordAdapter.addOnPagesUpdatedListener {
                            binding.swipeRefreshLayout.isRefreshing = false
                            loadingDialog.dismiss()
                        }
                        recordAdapter.submitData(it)

                    }
                }
                dialogInterface.dismiss()
            },
            cancelText = "取消"
        ).show()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onBackPressed() {
        if (appViewModel.queryPattern.value.isNullOrEmpty()) {
            super.onBackPressed()
        } else {
            binding.swipeRefreshLayout.isRefreshing = true
            appViewModel.queryPattern.value = ""
            lifecycleScope.launch {
                appViewModel.getRecord(typeOrPackageName, isType).collectLatest {
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