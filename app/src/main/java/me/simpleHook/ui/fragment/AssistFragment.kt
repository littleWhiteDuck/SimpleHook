package me.simpleHook.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
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
        Snackbar.make(binding.addConfig, "已删除此配置", Snackbar.LENGTH_LONG).apply {
           anchorView = bottomNavigationView
        }.setAction("撤销") {
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
        }
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).post {
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            val layoutParams = binding.addConfig.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 20.dp, px2dp(PhoneUtils.getAppHeight(requireContext())) - px2dp(PhoneUtils.getViewY(bottomNavigationView)) + bottomNavigationView.height)
            binding.addConfig.layoutParams = layoutParams
        }
    }

}