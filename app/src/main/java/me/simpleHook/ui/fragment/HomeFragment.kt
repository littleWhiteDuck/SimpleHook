package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.simpleHook.R
import me.simpleHook.adapter.HomeAdapter
import me.simpleHook.bean.ConfigItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.FragmentHomeBinding
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.activity.ConfigActivity
import me.simpleHook.ui.custom.PopupWindowList
import me.simpleHook.util.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class HomeFragment : Fragment(), SearchView.OnQueryTextListener,
    HideScrollListener {
    private val viewModel: AppViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var filterConfigs: List<AppConfig> = ArrayList()
    private var config = "错误"
    private lateinit var mContext: Context
    private var currentPattern = ""
    private val mAdapter: HomeAdapter by lazy {
        HomeAdapter({ appConfigEntity -> adapterOnClick(appConfigEntity) },
            { appConfigEntity, isChecked -> switchOnChange(appConfigEntity, isChecked) },
            { appConfigEntity -> itemOnLongClick(appConfigEntity) })
    }
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }

    private val sp by lazy { SPUtils(requireContext()) }

    private val configPref by lazy { XUtils(requireContext(), "hookConfig").configPref }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mContext = requireContext()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        initView()
        initViewModel()
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initViewModel() {
        var tempSize = 0
        viewModel.getAllConfigs().observe(requireActivity()) {
            filterConfigs = it
            if (currentPattern.isEmpty()) {
                mAdapter.submitList(it)
                if (tempSize < it.size) {
                    tempSize = it.size
                    binding.mainRecycler.smoothScrollToPosition(0)
                }
                if (binding.progressBar2.visibility != View.GONE) binding.progressBar2.visibility =
                    View.GONE
            } else {
                toFilterData(currentPattern)
            }
        }

        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.mainRecycler.apply {
            this.adapter = mAdapter
            layoutManager = linearLayoutManager
            addOnScrollListener(FabScrollListener(this@HomeFragment))
        }
        binding.mainRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val configDelete = filterConfigs[viewHolder.adapterPosition]
                deleteConfig(configDelete)
            }

        }).attachToRecyclerView(binding.mainRecycler)
/*        val verticalThumbDrawable = resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val verticalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        val horizontalThumbDrawable = resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val horizontalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        MyFastScroller(binding.mainRecycler,verticalThumbDrawable, verticalTrackDrawable, horizontalThumbDrawable, horizontalTrackDrawable,
            resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.fastscroll_margin))*/
    }

    fun deleteConfig(appConfig: AppConfig) {
        viewModel.deleteConfigs(appConfig)
        configPref?.edit()?.remove(appConfig.packageName)?.apply()
        FileUtils.deleteFile(appConfig.packageName)
        Snackbar.make(
            binding.fab,
            getString(R.string.main_home_delete_config_tip), Snackbar.LENGTH_LONG
        ).apply {
            anchorView = binding.fab
        }.setAction(getString(R.string.main_home_undo_delete_config)) {
            viewModel.insertConfigs(appConfig)
            if (sp.openStorage) FileUtils.createConfigFile(appConfig.packageName, appConfig.config)
            if (sp.openXml) configPref?.edit()?.putString(appConfig.packageName, appConfig.config)
                ?.apply()
        }.show()
    }

    private fun itemOnLongClick(appConfig: AppConfig) {
        val arrayList = requireContext().resources.getStringArray(R.array.main_home_item_select_item)
        val popupWindowList = PopupWindowList.Builder(requireContext())
            .setItemList(arrayList)
            .setOutsideTouchable(true)
            .build()
        popupWindowList.setOnItemClickListener { _, _, position, _ ->
            popupWindowList.dismiss()
            when (position) {
                0 -> editConfig(appConfig)
                1 -> deleteConfig(appConfig)
                2 -> copyConfigs(appConfig.config)
            }
        }.show()
    }

    private fun editConfig(appConfig: AppConfig) {
        val bundle = Bundle()
        bundle.putParcelable("appConfig", appConfig)
        toAddFragment(bundle)
    }

    private fun copyConfigs(config: String) {
        ToolUtils.toClip(requireContext(), JsonUtil.formatJson(config))
        getString(R.string.main_home_export_configs_tip).toast(requireContext())
    }

    private fun initView() {
        binding.apply {
            addConfig.setOnClickListener { toAddFragment(null) }
            importConfigsFromPaste.setOnClickListener {
                ToolUtils.getClipboardContent(requireContext())?.let { importConfigs(it) }
            }
            shareConfigs.setOnClickListener { shareConfigs() }
            importConfigsFromInternet.setOnClickListener {
                showInternetImportConfigDialog()
            }
            importConfigsFromFile.setOnClickListener {readConfigsFromFile()}
        }

        bottomNavigationView.post {
            val layoutParams = binding.fab.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 20.dp, px2dp(PhoneUtils.getAppHeight(requireContext())) - px2dp(PhoneUtils.getViewY(bottomNavigationView)) + bottomNavigationView.height)
            binding.fab.layoutParams = layoutParams
        }
    }

    private fun readConfigsFromFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        startActivityForResult(intent, 2)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            val readText = uri?.let { readTextFromUri(it) } ?: ""
            if (readText == "") {
                Toast.makeText(requireContext(), "错误", Toast.LENGTH_SHORT).show()
            } else {
                importConfigs(readText)
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            requireActivity().contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            "error".toast(requireContext())
        }

        return stringBuilder.toString()
    }


    private fun showInternetImportConfigDialog() {
        val textInputLayout = TextInputLayout(requireContext())
        val textInput = TextInputEditText(requireContext())
        textInput.background = null
        textInputLayout.addView(textInput)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("请输入网址")
            .setCancelable(true)
            .setView(textInputLayout)
            .setPositiveButton("确认") { dialog, _ ->
                importConfigsFromInternet(textInput.text.toString().trim())
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun importConfigsFromInternet(urlString: String) {
        val regex =
            """((http|ftp|https)://[\w\-_]+(\.[\w\-_]+)+([\w\-.,@?^=%&:/~+#]*[\w\-@?^=%&/~+#])?)"""//设置正则表达式
        if (Pattern.matches(regex, urlString)) {
            thread {
                var connection: HttpURLConnection? = null
                try {
                    val response = StringBuilder()
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    val input = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    reader.use {
                        reader.forEachLine {
                            response.append(it)
                        }
                    }
                    config = response.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
            }
            if (config == "错误") {
                "稍后再试，网络错误或者其他错误".toast(requireContext())
            } else {
                importConfigs(config)
            }
        } else {
            "网址不正确".toast(requireContext())
        }

    }

    private fun shareConfigs() {
        if (filterConfigs.isNotEmpty()) {
            val dataList = ArrayList<ConfigItem>()
            for (config in filterConfigs) {
                dataList.add(ConfigItem(config))
            }
            ConfigDialogFragment(dataList, false).show(
                requireActivity().supportFragmentManager,
                "export"
            )
        }
    }

    private fun importConfigs(configs: String) {
        try {
            when {
                JsonUtil.isJsonArray(configs) -> {
                    val configsJsonArray = JSONArray(configs)
                    val dataList = ArrayList<ConfigItem>()
                    for (i in 0 until configsJsonArray.length()) {
                        configsJsonArray.getJSONObject(i).apply {
                            dataList.add(
                                ConfigItem(
                                    AppConfig(
                                        getString("packageName"),
                                        getString("appName"),
                                        getString("versionName"),
                                        getString("description"),
                                        toString()
                                    )
                                )
                            )
                        }
                    }
                    ConfigDialogFragment(dataList).show(
                        requireActivity().supportFragmentManager,
                        "import"
                    )
                }
                JsonUtil.isJsonObject(configs) -> {
                    JSONObject(configs).apply {
                        viewModel.insertConfigs(
                            AppConfig(
                                getString("packageName"),
                                getString("appName"),
                                getString("versionName"),
                                getString("description"),
                                toString()
                            )
                        )
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            getString(R.string.main_home_import_incorrect_format_tip).toast(requireContext())
        }
    }

    private fun switchOnChange(appConfig: AppConfig, isChecked: Boolean) {
        val oldCan = appConfig.canUse
        val newConfig = appConfig.config.replace("canUse\":$oldCan", "canUse\":$isChecked")
        appConfig.apply {
            config = newConfig
            canUse = isChecked
        }
        viewModel.updateConfigs(appConfig)
        if (sp.openStorage) {
            FileUtils.verifyStoragePermissions(mContext as Activity)
            FileUtils.createConfigFile(appConfig.packageName, appConfig.config)
        }
        if (sp.openXml) {
            configPref?.edit()?.putString(appConfig.packageName, appConfig.config)?.apply()
        }
    }


    private fun adapterOnClick(appConfig: AppConfig) {
        val bottomSheetDialog = me.simpleHook.ui.custom.BottomSheetDialog(
            requireContext(),
            appConfig,
            onClick = { editConfig(appConfig) })
        bottomSheetDialog.setContentView()
        val windowPreferencesManager = WindowPreferencesManager(requireContext())
        windowPreferencesManager.applyEdgeToEdgePreference(bottomSheetDialog.window!!)
        bottomSheetDialog.show()
    }

    private fun toAddFragment(bundle: Bundle?) {
        val intent = Intent(requireActivity(), ConfigActivity::class.java)
        intent.putExtra("bundle", bundle)
        startActivity(intent)
    }

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = newText.trim()
        currentPattern = pattern
        toFilterData(pattern)
        return true
    }

    private fun toFilterData(pattern: String) {
        val filter = filterConfigs.filter {
            it.appName.contains(pattern) || it.packageName.contains(pattern)
        }
        mAdapter.submitList(filter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.apply {
            queryHint = context.getString(R.string.main_home_toolbar_search_hint)
            setOnQueryTextListener(this@HomeFragment)
        }
    }

    class FabScrollListener(private val listener: HideScrollListener) :
        RecyclerView.OnScrollListener() {
        private var distance = 0
        private var visible = true //是否可见
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (distance > THRESHOLD && visible) {
                //隐藏动画
                visible = false
                listener.onHide()
                distance = 0
            } else if (distance < -20 && !visible) {
                //显示动画
                visible = true
                listener.onShow()
                distance = 0
            }
            if (visible && dy > 0 || !visible && dy < 0) {
                distance += dy
            }
        }

        companion object {
            private const val THRESHOLD = 20
        }
    }

    override fun onShow() {
        bottomNavigationView
        binding.fab.animate().translationY(0f).interpolator = DecelerateInterpolator(3f)
    }

    override fun onHide() {
        binding.fab.animate().translationY(binding.fab.height.px).interpolator =
            DecelerateInterpolator(1.5f)
    }


}

interface HideScrollListener {
    fun onShow()
    fun onHide()
}