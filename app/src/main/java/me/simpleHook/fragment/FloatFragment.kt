package me.simpleHook.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lzf.easyfloat.EasyFloat
import me.simpleHook.adapter.PrintLogAdapter
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.FragmentFloatBinding
import me.simpleHook.util.FileUtils
import me.simpleHook.util.toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FloatFragment : Fragment() {
    private val viewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AppViewModel::class.java]
    }
    private var _binding: FragmentFloatBinding? = null
    private val binding get() = _binding!!
    private val mAdapter: PrintLogAdapter by lazy {
        PrintLogAdapter()
    }
    private val list = ArrayList<PrintLog>()
    private val handler = Handler()
    private val refresh = object : Runnable {
        override fun run() {
            updateData()
            handler.postDelayed(this, 500)
        }
    }
    private val uri = Uri.parse("content://littleWhiteDuck/print_logs")
    private var stopPrint = false
    private var currentId = 0

    private fun updateData() {
        /*val selectionArg = if (!stopPrint) arrayOf(currentId.toString()) else arrayOf(stopCount.toString())*/
        requireContext().contentResolver.query(
            uri,
            null,
            "id > ?",
            arrayOf(currentId.toString()),
            null
        )?.apply {
            while (moveToNext()) {
                val log = getString(getColumnIndex("log"))
                val packageName = getString(getColumnIndex("packageName"))
                val id = getInt(getColumnIndex("id"))
                if (!stopPrint) list.add(PrintLog(id, log, packageName))
                currentId = id
            }
            close()
        }
        val runnable = Runnable {
            if (!stopPrint){
                mAdapter.setDataList(list)
                mAdapter.notifyDataSetChanged()
                binding.recyclerView.smoothScrollToPosition(list.size)
            }
        }
        handler.postDelayed(runnable, 0)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFloatBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    @SuppressLint("SimpleDateFormat")
    private fun initView() {
        binding.apply {
            recyclerView.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
            closeWindow.setOnClickListener {
                if (EasyFloat.getFloatView("floatControl") != null) {
                    EasyFloat.dismiss("floatControl")
                }
                EasyFloat.dismiss("floatPrint")
            }
            clearAllData.setOnClickListener {
                viewModel.deleteAllLogs()
                list.clear()
                mAdapter.notifyDataSetChanged()
            }
            pausePrintLog.setOnClickListener {
                stopPrint = !stopPrint
                pausePrintLog.text = if (stopPrint) "继续" else "暂停"
            }
            importToFile.setOnClickListener {
                if (list.isEmpty()) {
                    "什么东西".toast(requireContext())
                    return@setOnClickListener
                }
                val date = Date()
                val time = SimpleDateFormat("yy_MM_dd_hh_mm_ss").format(date)
                val url = Constant.PRINT_LOG__DIRECTORY
                val writeList = list
                val str = StringBuilder()
                for (i in writeList.indices) {
                    str.append("  ${writeList[i].log},\n")
                }
                val strLog = str.toString().substring(0, str.toString().length - 2)
                FileUtils.writeData(url, time, "[\n${strLog}\n]")
                "导出路径为：${Constant.PRINT_LOG__DIRECTORY + time}.json".toast(requireContext(), 1)
            }

        }
        handler.postDelayed(refresh, 500)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}