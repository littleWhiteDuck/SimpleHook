package me.simpleHook.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter
import me.simpleHook.constant.Constant
import me.simpleHook.constant.Constant.MODEL_EXTENSION_CONFIG
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAssistBinding
import me.simpleHook.ui.activity.AppListActivity
import me.simpleHook.ui.activity.AssistActivity
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.ui.custom.warningDialog
import me.simpleHook.util.*
import kotlin.math.min

class ExtensionFragment : Fragment() {
    private val appViewModel by activityViewModels<AppViewModel>()
    private val sp by lazy { SPUtils(requireContext()) }
    private val bottomNavigationView by lazy {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    }
    private var _binding: FragmentAssistBinding? = null
    private val binding get() = _binding!!
    private lateinit var mContext: Context
    private var isFabShow = true
    private var fabHideDistance = 0f
    private val mAdapter: AssistAdapter by lazy {
        AssistAdapter({ assistConfig -> itemOnClick(assistConfig) },
            { assistConfig -> itemOnLongClick(assistConfig) })
    }
    private val startActivityForData =
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            uri?.also {
                val contentResolver = requireActivity().contentResolver
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }
    private val startActivityForModelCreate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data = it.data!!
                val appName = data.getStringExtra("appName")!!
                val packageName = data.getStringExtra("packageName")!!
                if (currentModel == -1) {
                    appViewModel.insertAssistConfigs(
                        AssistConfig(
                            appName = appName,
                            packageName = packageName
                        )
                    )
                } else {
                    val modelConfig = modelList[currentModel]
                    modelConfig.appName = appName
                    modelConfig.packageName = packageName
                    modelConfig.id = 0
                    appViewModel.insertAssistConfigs(modelConfig)
                    currentModel = -1
                    saveToText(modelConfig.packageName, modelConfig.config)
                }
            }
        }
    private var modelList = mutableListOf<AssistConfig>()
    private var currentModel = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mContext = requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistBinding.inflate(inflater, container, false)
        initView()
        initData()
        return binding.root
    }

    private fun initData() {
        appViewModel.getAllAssistConfigs().observe(viewLifecycleOwner) {
            modelList.clear()
            val showList = mutableListOf<AssistConfig>()
            for (assist in it) {
                if (assist.packageName == MODEL_EXTENSION_CONFIG) {
                    modelList.add(assist)
                } else {
                    showList.add(assist)
                }
            }
            binding.emptyTip.visibility = if (showList.isEmpty()) View.VISIBLE else View.GONE
            mAdapter.submitList(showList)
            binding.progressBar4.hide()
        }
    }

    private fun initView() {
        binding.progressBar4.show()
        var maybeABug = 0
        val layoutParams = binding.addConfig.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navigationInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGesture =
                navigationInsets.bottom <= 20 * requireActivity().resources.displayMetrics.density
            ViewCompat.onApplyWindowInsets(binding.root, windowInsets)
            maybeABug = if (maybeABug == 0) {
                bottomNavigationView.bottom - bottomNavigationView.top
            } else {
                min(maybeABug, bottomNavigationView.bottom - bottomNavigationView.top)
            }
            if (navigationInsets.bottom == 0) maybeABug += 10.dp
            layoutParams.bottomMargin = if (isGesture) maybeABug + navigationInsets.bottom
            else maybeABug + navigationInsets.bottom / 5
            fabHideDistance = layoutParams.bottomMargin.toFloat() * 2
            binding.addConfig.layoutParams = layoutParams
            binding.assistRev.updatePadding(bottom = maybeABug / 2)
            windowInsets
        }
        binding.apply {
            addConfig.setOnClickListener {
                addConfig()
            }
            addConfig.setOnLongClickListener {
                directAddConfig()
                true
            }
            assistRev.apply {
                adapter = mAdapter
                layoutManager = GridLayoutManager(requireContext(), 2)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    private var distance = 0
                    private var visible = true
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (distance > 20 && visible) {
                            visible = false
                            hideFab()
                            distance = 0
                        } else if (distance < -20 && !visible) {
                            visible = true
                            distance = 0
                            showFab()
                        }
                        if (visible && dy > 0 || !visible && dy < 0) {
                            distance += dy
                        }
                    }
                })
            }
        }
        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.assistRev.scrollToPosition(0)
            }
        })
        FastScrollerUtil.bind(binding.assistRev)
    }

    private fun showFab() {
        binding.addConfig.animate().translationY(0f).interpolator =
            DecelerateInterpolator(1.5f)
        isFabShow = true
    }

    private fun upFab() {
        binding.addConfig.animate().translationY((-50f).dp).interpolator =
            DecelerateInterpolator(1.5f)
    }

    private fun hideFab() {
        binding.addConfig.animate().translationY(fabHideDistance).interpolator =
            DecelerateInterpolator(1.5f)
        isFabShow = false
    }

    private fun itemOnLongClick(assistConfig: AssistConfig) {
        appViewModel.deleteAssistConfigs(assistConfig)
        FileUtils.realDeleteConfig(
            requireContext(),
            assistConfig.packageName,
            Constant.EXTENSION_CONFIG_NAME
        )

        Snackbar.make(
            binding.addConfig,
            getString(R.string.main_extension_delete_config_tip),
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = bottomNavigationView
        }.addCallback(object : Snackbar.Callback() {
            override fun onShown(sb: Snackbar?) {
                super.onShown(sb)
                if (isFabShow) upFab()
            }

            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (isFabShow) showFab()
            }
        }).setAction(getString(R.string.main_extension_undo_delete_config)) {
            appViewModel.insertAssistConfigs(assistConfig)
            saveToText(assistConfig.packageName, assistConfig.config)
        }.show()
    }

    private fun saveToText(packageName: String, configs: String) {
        if (sp.openStorage) {
            if (FileUtils.isGrant(requireContext())) {
                lifecycleScope.launch(Dispatchers.IO) {
                    FileUtils.saveConfig(
                        requireContext(),
                        packageName,
                        Constant.EXTENSION_CONFIG_NAME,
                        configs
                    )
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    requestPermissionDialog(requireContext()) {
                        startActivityForData.launch(Uri.parse(Constant.ANDROID_DATA_URI))
                    }
                } else {
                    requestPermissionDialog(requireContext()) {
                        FileUtils.verifyStoragePermissions(requireActivity())
                    }
                }
            }
        }
    }

    private fun itemOnClick(assistConfig: AssistConfig) {
        val bundle = Bundle()
        bundle.putParcelable("assistConfig", assistConfig)
        val intent = Intent(requireActivity(), AssistActivity::class.java).apply {
            putExtra("bundle", bundle)
        }
        startActivity(intent)
    }

    private fun addConfig() {
        if (modelList.isEmpty()) {
            directAddConfig()
        } else {
            showSelectModelDialog()
        }
    }

    private fun directAddConfig() {
        val intent = Intent(requireActivity(), AppListActivity::class.java).apply {
            putExtra("isFromAssist", true)
        }
        startActivityForModelCreate.launch(intent)
    }

    private fun createModel(
        editMode: Boolean = false,
        assistConfig: AssistConfig = AssistConfig(
            appName = "",
            packageName = MODEL_EXTENSION_CONFIG
        )
    ) {
        val editText = AppCompatEditText(mContext)
        editText.hint = "给模板起个名字"
        if (editMode) editText.setText(assistConfig.appName)
        customDialog(
            mContext,
            title = if (editMode) "修改模板" else "创建模板",
            contentView = editText,
            okText = if (editMode) "去修改" else "去创建",
            okClick = {
                val modelName = editText.text.toString()
                if (modelName.isNotEmpty() && modelName.length < 10) {
                    assistConfig.appName = modelName
                    val bundle = Bundle()
                    bundle.putParcelable("assistConfig", assistConfig)
                    val intent = Intent(requireActivity(), AssistActivity::class.java).apply {
                        putExtra("bundle", bundle)
                        putExtra("editMode", editMode)
                    }
                    startActivity(intent)
                } else {
                    "不能为空或名字太长".toast(mContext)
                }
            },
            cancelText = "取消"
        ).show()
    }

    private fun showModelDialog() {
        if (modelList.isEmpty()) {
            "请先创建".toast(mContext)
            return
        }
        val showList = mutableListOf<String>()
        modelList.forEach {
            showList.add(it.appName)
        }
        val listView = ListView(mContext)
        val adapter = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, showList)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            createModel(editMode = true, assistConfig = modelList[position])
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            adapter.remove(showList[position])
            appViewModel.deleteAssistConfigs(modelList[position])
            modelList.removeAt(position)
            true
        }
        customDialog(
            mContext,
            title = MODEL_EXTENSION_CONFIG,
            contentView = listView,
            okText = "确定",
            cancelText = "取消",
            neutralText = "清空",
            neutralClick = { dialogInterface ->
                appViewModel.deleteAssistConfigs(*modelList.toTypedArray())
                dialogInterface.dismiss()
            },
            cancelAble = false
        ).show()
    }

    private fun showSelectModelDialog() {
        val showList = mutableListOf<String>()
        modelList.forEach {
            showList.add(it.appName)
        }
        val listView = ListView(mContext)
        val adapter = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, showList)
        listView.adapter = adapter
        val dialog = customDialog(
            mContext,
            title = "选择一个模板",
            contentView = listView,
        )
        listView.setOnItemClickListener { _, _, position, _ ->
            currentModel = position
            val intent = Intent(requireActivity(), AppListActivity::class.java).apply {
                putExtra("isFromAssist", true)
            }
            startActivityForModelCreate.launch(intent)
            dialog.cancel()
        }
        dialog.show()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (menu.findItem(R.id.app_bar_search) == null) {
            inflater.inflate(R.menu.menu_assist_fragment, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.assistFragment_startFloat -> {
                initPrintFloat()
            }
            R.id.create_model -> createModel()
            R.id.show_model -> showModelDialog()
            R.id.about_model -> showAboutModel()
        }
        return true
    }

    private fun showAboutModel() {
        warningDialog(
            mContext, title = "关于模板", message = """
            创建模板后，在创建配置的时候可以选择模板，所创建的配置中的选中状态和模板一样，简化操作
            查看模板：
                ->点击模板（进入编辑模式）
                ->长按模板（删除模板）
            长按加号按钮：不使用模板选择App
        """.trimIndent()
        )
    }

    private fun initPrintFloat() {
        EasyFloat.with(requireActivity())
            .setLayout(R.layout.float_window_layout) {
                val viewPager = it.findViewById<ViewPager2>(R.id.float_viewpager2)
                viewPager.adapter = object : FragmentStateAdapter(this) {
                    override fun getItemCount() = 1

                    override fun createFragment(position: Int) = FloatFragment()
                }
            }
            .setTag("floatPrint")
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            .setDragEnable(false)
            .setLocation(0, 0)
            .setMatchParent(widthMatch = true, heightMatch = false)
            .setAnimator(DefaultAnimator())
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}