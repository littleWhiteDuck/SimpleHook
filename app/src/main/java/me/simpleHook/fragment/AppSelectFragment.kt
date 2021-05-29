package me.simpleHook.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.simpleHook.adapter.AppSelectAdapter
import me.simpleHook.bean.AppItem
import me.simpleHook.databinding.FragmentAppSelectBinding
import me.simpleHook.utils.AppUtils
import me.simpleHook.viewmodel.MethodViewModel


class AppSelectFragment : BaseFragment(), CoroutineScope by MainScope() {
    private lateinit var list: ArrayList<AppItem>
    private lateinit var binding: FragmentAppSelectBinding
    private lateinit var viewModel: MethodViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSelectBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        )[MethodViewModel::class.java]
        val adapter = AppSelectAdapter(object : AppSelectAdapter.OnItemClickListener {
            override fun onItemClickListener(position: Int) {
                viewModel.appLive.value = list[position]
                back()
            }
        })
        val linearLayoutManager = LinearLayoutManager(requireContext())
        launch {
            list = getList()
            adapter.setAppList(list).also {
                binding.progressBar.visibility = View.GONE
            }
            binding.appSelectRec.apply {
                this.adapter = adapter
                layoutManager = linearLayoutManager
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private fun getList(): ArrayList<AppItem> {
        val appList = ArrayList<AppItem>()
        val context = requireContext()
        val list = AppUtils.getInstalledApp(context)
        for (i in list.indices) {
            val appName = AppUtils.getAppName(context, list[i])
            val appIcon = AppUtils.getAppIcon(context, list[i])
            val packageName = list[i].packageName
            val versionName = AppUtils.getAppVersionName(context, packageName)
            appList.add(AppItem(appName, packageName, appIcon, versionName))
        }
        return appList
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                back()
            }
        }
        return true
    }

}