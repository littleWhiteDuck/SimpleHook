package me.simpleHook.ui.fragment

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import me.simpleHook.R
import me.simpleHook.adapter.AssistAdapter
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.databinding.FragmentAssistBinding
import me.simpleHook.ui.activity.AppListActivity
import me.simpleHook.ui.activity.AssistActivity
import me.simpleHook.util.*

class AssistFragment : Fragment() {
    private val appViewModel by activityViewModels<AppViewModel>()
    private val sp by lazy { SPUtils(requireContext()) }
    private val assistPref by lazy { XUtils(requireContext(), "assistConfig").configPref }
    private lateinit var binding: FragmentAssistBinding
    private val mAdapter: AssistAdapter by lazy {
        AssistAdapter({ assistConfig -> itemOnClick(assistConfig) },
            { assistConfig -> itemOnLongClick(assistConfig) })
    }

    private fun itemOnLongClick(assistConfig: AssistConfig) {
        appViewModel.deleteAssistConfigs(assistConfig)
        FileUtils.deleteFile(assistConfig.packageName, false)
        sp.remove(assistConfig.packageName)
        val bottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        Snackbar.make(
            binding.addConfig,
            getString(R.string.main_assist_delete_config_tip),
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = bottomNavigationView
        }.setAction(getString(R.string.main_assist_undo_delete_config)) {
            appViewModel.insertAssistConfigs(assistConfig)
            if (sp.openStorage) {
                FileUtils.createConfigFile(assistConfig.packageName, assistConfig.config, false)
            }
            if (sp.openXml) {
                assistPref?.edit()?.putString(assistConfig.packageName, assistConfig.config)
                    ?.apply()
            }
        }.show()
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
        binding = FragmentAssistBinding.inflate(inflater, container, false)
        initView()
        initViewModel()
        return binding.root
    }

    private fun initViewModel() {
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
            assistRev.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

                    if (position == parent.adapter!!.itemCount - 2) {
                        outRect.bottom = 200
                    }
                }
            })
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

}