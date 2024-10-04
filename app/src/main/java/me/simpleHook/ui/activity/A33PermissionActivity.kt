package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.base.BaseActivity
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.Constant
import me.simpleHook.contract.OpenDocumentTreeContract2
import me.simpleHook.databinding.ActivityA33PermissionBinding
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.ui.view.permission.PermissionItemView
import me.simpleHook.ui.view.permission.PermissionSortView
import me.simpleHook.util.AppUtils
import me.simpleHook.util.FileUtils
import me.simpleHook.util.PermissionUtils
import me.simpleHook.util.SPUtils
import me.simpleHook.util.TimeUtil


private const val SHOW_APP_USER = 0
private const val SHOW_APP_SYSTEM = 1
private const val SHOW_APP_ALL = 2

private const val SORT_BY_NAME = 0
private const val SORT_BY_PACK_NAME = 1
private const val SORT_BY_INSTALLED_TIME = 2

class A33PermissionActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: ActivityA33PermissionBinding
    private var appList: List<PermissionBean> = ArrayList()
    private var currentPattern = ""
    private var needApplyApps = HashSet<String>()
    private val startActivityForData2 =
        registerForActivityResult(OpenDocumentTreeContract2()) { callBackIntent ->
            callBackIntent?.let {
                it.data?.let { uri ->
                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    if (uri == DocumentCompat.generateAppUri(needApplyApps.first())) {
                        needApplyApps.remove(needApplyApps.first())
                        batchGrant()
                        if (needApplyApps.isEmpty()) {
                            fetchData()
                        }
                    } else {
                        needApplyApps.clear()
                        fetchData()
                    }
                } ?: run {
                    val clipData = it.clipData ?: return@run
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        val takeFlags: Int =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                    }
                    fetchData()
                }
            }
        }


    private val permissionAdapter by lazy {
        PermissionAdapter(onClick = { packageName, checked -> onItemClick(packageName, checked) })
    }

    private val sp by lazy { SPUtils(this) }

    private fun onItemClick(packageName: String, checked: Boolean) {
        if (checked) {
            needApplyApps.add(packageName)
        } else {
            needApplyApps.remove(packageName)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityA33PermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initView()
        fetchData()
    }

    private fun fetchData() {
        binding.progressBar.isVisible = true
        lifecycleScope.launch(Dispatchers.IO) {
            val userAppList = ArrayList<PermissionBean>()
            val systemAppList = ArrayList<PermissionBean>()
            AppUtils.getInstalledUserApp(this@A33PermissionActivity).forEach {
                val isExists = FileUtils.isFileExists(Constant.ANDROID_DATA_PATH, it.packageName)
                if (isExists) {
                    val hasGrant = PermissionUtils.isGrantPackage(it.packageName)
                    if (!hasGrant) {
                        userAppList.add(
                            PermissionBean(
                                it.packageName,
                                AppUtils.getAppName(this@A33PermissionActivity, it),
                                TimeUtil.getTime(it.lastUpdateTime, "yyyy-MM-dd HH:mm:ss"),
                                needApplyApps.contains(it.packageName)
                            )
                        )
                    }
                }
            }
            AppUtils.getInstalledSystemApp(this@A33PermissionActivity).forEach {
                val isExists = FileUtils.isFileExists(Constant.ANDROID_DATA_PATH, it.packageName)
                if (isExists) {
                    val hasGrant = PermissionUtils.isGrantPackage(it.packageName)
                    if (!hasGrant) {
                        systemAppList.add(
                            PermissionBean(
                                it.packageName,
                                AppUtils.getAppName(this@A33PermissionActivity, it),
                                TimeUtil.getTime(it.lastUpdateTime, "yyyy-MM-dd HH:mm:ss"),
                                needApplyApps.contains(it.packageName)
                            )
                        )
                    }
                }
                appList = when (sp.permissionAppShowMode) {
                    SHOW_APP_USER -> userAppList
                    SHOW_APP_SYSTEM -> systemAppList
                    else -> userAppList + systemAppList
                }.sortedBy { permissionBean ->
                    when (sp.permissionSortMode) {
                        SORT_BY_NAME -> permissionBean.name
                        SORT_BY_PACK_NAME -> permissionBean.packageName
                        else -> permissionBean.installedTime
                    }
                }
                if (sp.permissionReverseSort) {
                    appList = appList.reversed()
                }
                withContext(Dispatchers.Main) {
                    if (currentPattern.isEmpty()) {
                        permissionAdapter.submitList(appList)
                    } else {
                        val tempList = appList.filter { tempPermissionBean ->
                            tempPermissionBean.name.contains(currentPattern, true)
                        }
                        permissionAdapter.submitList(tempList)
                    }
                    binding.progressBar.isVisible = false
                }
            }
        }
    }

    private fun initView() {
        binding.recyclerView.apply {
            adapter = permissionAdapter
            layoutManager = LinearLayoutManager(this@A33PermissionActivity)
        }
        binding.doneAll.setOnClickListener {
            batchGrant()
        }
    }

    private fun batchGrant() {
        if (needApplyApps.isNotEmpty()) {
            startActivityForData2.launch(DocumentCompat.generateAppUri(needApplyApps.first()))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_permisssion, menu)
        val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
//        searchView.setTextColor(Color.WHITE)
        searchView.apply {
            queryHint = context.getString(R.string.permission_search_hint_app_name)
            setOnQueryTextListener(this@A33PermissionActivity)
        }

        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_help -> {
                showHelpDialog()
            }

            R.id.menu_sort -> {
                showSortDialog()
            }

            R.id.menu_select_all -> {
                appList = appList.filter {
                    needApplyApps.add(it.packageName)
                    it.checked = true
                    true
                }
                permissionAdapter.submitList(appList)
                permissionAdapter.notifyDataSetChanged()
            }

            R.id.menu_invert_selection -> {
                appList = appList.filter {
                    if (needApplyApps.contains(it.packageName)) {
                        needApplyApps.remove(it.packageName)
                        it.checked = false
                    } else {
                        needApplyApps.add(it.packageName)
                        it.checked = true
                    }
                    true
                }
                permissionAdapter.submitList(appList)
                permissionAdapter.notifyDataSetChanged()
            }

            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    private fun showHelpDialog() {
        warningDialog(
            this,
            title = getString(R.string.record_detail_menu_help),
            message = getString(R.string.permission_tip)
        )
    }

    private fun showSortDialog() {
        val permissionSortView = PermissionSortView(this)
        var sortMode = sp.permissionSortMode
        var appShowMode = sp.permissionAppShowMode
        var reverseSort = sp.permissionReverseSort
        permissionSortView.apply {
            when (sortMode) {
                SORT_BY_NAME -> nameChip.isChecked = true
                SORT_BY_PACK_NAME -> packageNameChip.isChecked = true
                SORT_BY_INSTALLED_TIME -> installedTimeChip.isChecked = true
            }
            when (appShowMode) {
                SHOW_APP_USER -> userAppChip.isChecked = true
                SHOW_APP_SYSTEM -> systemAppChip.isChecked = true
                SHOW_APP_ALL -> allAppChip.isChecked = true
            }
            if (reverseSort) {
                reverseSortChip.isChecked = true
            } else {
                forwardSortChip.isChecked = true
            }
            nameChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) sortMode = SORT_BY_NAME
            }
            packageNameChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) sortMode = SORT_BY_PACK_NAME
            }
            installedTimeChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) sortMode = SORT_BY_INSTALLED_TIME
            }
            userAppChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) appShowMode = SHOW_APP_USER
            }
            systemAppChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) appShowMode = SHOW_APP_SYSTEM
            }
            allAppChip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) appShowMode = SHOW_APP_ALL
            }
            reverseSortChip.setOnCheckedChangeListener { _, isChecked ->
                reverseSort = isChecked
            }
        }

        customDialog(
            this,
            contentView = permissionSortView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                sp.permissionSortMode = sortMode
                sp.permissionReverseSort = reverseSort
                sp.permissionAppShowMode = appShowMode
                fetchData()
            },
            cancelText = getString(R.string.dialog_cancel)
        ).show()
    }

    override fun onQueryTextSubmit(query: String?) = false

    /**
     * {@link OnQueryTextListener}
     */
    override fun onQueryTextChange(newText: String): Boolean {
        filterData(newText.trim())
        return true
    }

    private fun filterData(pattern: String) {
        if (pattern.isEmpty()) {
            binding.doneAll.show()
        } else {
            binding.doneAll.hide()
        }
        currentPattern = pattern
        fetchData()
    }

}


data class PermissionBean(
    val packageName: String,
    val name: String,
    val installedTime: String,
    var checked: Boolean = false
)


class PermissionAdapter(
    private val onClick: (packageName: String, checked: Boolean) -> Unit
) : ListAdapter<PermissionBean, PermissionAdapter.ViewHolder>(AppDiffCallback) {

    inner class ViewHolder(permissionItemView: PermissionItemView) :
        RecyclerView.ViewHolder(permissionItemView) {
        val icon = permissionItemView.containerView.icon
        val packageName = permissionItemView.containerView.packageName
        val appName = permissionItemView.containerView.appName
        val checkBox = permissionItemView.containerView.checkBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val permissionItemView = PermissionItemView(parent.context)
        val viewHolder = ViewHolder(permissionItemView)
        permissionItemView.setOnClickListener {
            val position = viewHolder.itemView.getTag(R.id.item_permission_grant_position) as Int
            val permissionBean = getItem(position)
            viewHolder.checkBox.isChecked = !viewHolder.checkBox.isChecked
            onClick(permissionBean.packageName, viewHolder.checkBox.isChecked)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val permissionBean = getItem(position)
        holder.itemView.setTag(R.id.item_permission_grant_position, position)
        holder.apply {
            Glide.with(icon).load(permissionBean.packageName).into(icon)
            appName.text = permissionBean.name
            packageName.text = permissionBean.packageName
            checkBox.isChecked = permissionBean.checked
        }
    }


    object AppDiffCallback : DiffUtil.ItemCallback<PermissionBean>() {
        override fun areItemsTheSame(oldItem: PermissionBean, newItem: PermissionBean): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: PermissionBean, newItem: PermissionBean): Boolean =
            oldItem.name == newItem.name && oldItem.packageName == newItem.packageName && oldItem.checked == newItem.checked

    }

}