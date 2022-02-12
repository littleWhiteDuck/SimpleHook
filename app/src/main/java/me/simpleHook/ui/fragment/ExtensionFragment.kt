package me.simpleHook.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
import me.simpleHook.contract.OpenDocumentTreeContract
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAssistBinding
import me.simpleHook.ui.activity.AppListActivity
import me.simpleHook.ui.activity.AssistActivity
import me.simpleHook.ui.custom.requestPermissionDialog
import me.simpleHook.util.*

class ExtensionFragment : Fragment() {
    private val appViewModel by activityViewModels<AppViewModel>()
    private val sp by lazy { SPUtils(requireContext()) }

    //    private val assistPref by lazy { XUtils(requireContext(), "assistConfig").configPref }
    private var _binding: FragmentAssistBinding? = null
    private val binding get() = _binding!!
    private val mAdapter: AssistAdapter by lazy {
        AssistAdapter({ assistConfig -> itemOnClick(assistConfig) },
            { assistConfig -> itemOnLongClick(assistConfig) })
    }
    private val startActivityForData by lazy {
        registerForActivityResult(OpenDocumentTreeContract()) { uri ->
            uri?.also {
                val contentResolver = requireActivity().contentResolver
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }
    }

    private fun itemOnLongClick(assistConfig: AssistConfig) {
        if (assistConfig.appName == "默认配置" && assistConfig.packageName == "默认配置") {
            "默认配置不可删除".toast(requireContext())
            return
        }
        appViewModel.deleteAssistConfigs(assistConfig)
        FileUtils.fakeDeleteConfig(
            requireContext(),
            assistConfig.packageName,
            Constant.EXTENSION_CONFIG_NAME
        )
//        sp.remove(assistConfig.packageName)
        val bottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        Snackbar.make(
            binding.addConfig,
            getString(R.string.main_extension_delete_config_tip),
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = bottomNavigationView
        }.addCallback(object : Snackbar.Callback() {
            override fun onShown(sb: Snackbar?) {
                super.onShown(sb)
                binding.addConfig.animate().translationY((-50f).dp).interpolator =
                    DecelerateInterpolator(1.5f)
            }

            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                binding.addConfig.animate().translationY(0f).interpolator =
                    DecelerateInterpolator(1.5f)
            }
        }).setAction(getString(R.string.main_extension_undo_delete_config)) {
            appViewModel.insertAssistConfigs(assistConfig)
            saveToText(assistConfig.packageName, assistConfig.config)
//            if (sp.openXml) {
//                assistPref?.edit()?.putString(assistConfig.packageName, assistConfig.config)
//                    ?.apply()
//            }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        }
        return true
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
        lifecycleScope.launch(Dispatchers.IO) {
            if (appViewModel.queryDefaultExConfig().isEmpty()) {
                appViewModel.insertAssistConfigs(
                    AssistConfig(
                        appName = "默认配置",
                        packageName = "默认配置"
                    )
                )
            }
        }
        appViewModel.getAllAssistConfigs().observe(viewLifecycleOwner) {
            mAdapter.submitList(it)
        }

    }

    private fun initView() {
        binding.apply {
            addConfig.setOnClickListener {
                val intent = Intent(requireActivity(), AppListActivity::class.java).apply {
                    putExtra("isFromAssist", true)
                }
                startActivity(intent)
            }
            assistRev.apply {
                adapter = mAdapter
                layoutManager = GridLayoutManager(requireContext(), 2)
            }
        }
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).post {
            val bottomNavigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            val layoutParams = binding.addConfig.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(
                0,
                0,
                20.dp,
                px2dp(PhoneUtils.getAppHeight(requireContext())) - px2dp(
                    PhoneUtils.getViewY(bottomNavigationView)
                ) + bottomNavigationView.height
            )
            binding.addConfig.layoutParams = layoutParams
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}