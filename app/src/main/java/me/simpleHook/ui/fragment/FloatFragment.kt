package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import me.simpleHook.R
import me.simpleHook.adapter.PrintLogAdapter
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.FragmentFloatBinding
import me.simpleHook.ui.view.ControlView
import me.simpleHook.util.FileUtils
import me.simpleHook.util.JsonUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.R.attr.name
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets


class FloatFragment : Fragment() {
    private val viewModel by activityViewModels<AppViewModel>()
    private var _binding: FragmentFloatBinding? = null
    private val binding get() = _binding!!
    private val mAdapter: PrintLogAdapter by lazy { PrintLogAdapter() }
    private val list = ArrayList<PrintLog>()
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            updateData()
            handler.postDelayed(this, 500)
        }
    }
    private val uri = Uri.parse("content://littleWhiteDuck/print_logs")
    private var stopPrint = false
    private var currentId = 0

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun updateData() {
        if (!isAdded) return
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
            if (!stopPrint) {
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

    @SuppressLint("SimpleDateFormat", "ClickableViewAccessibility", "NotifyDataSetChanged")
    private fun initView() {
        binding.apply {
            recyclerView.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
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
                    Toast.makeText(requireContext(), getString(R.string.assist_float_export_config_empty_tip), Toast.LENGTH_SHORT).show()
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
                val strLog = str.toString().substring(0, str.toString().length - 2).replace("\\u003e", ">")
                FileUtils.writeData(url, time, JsonUtil.formatJson("[\n${strLog}\n]"))
                Toast.makeText(
                    requireContext(),
                    "导出路径为：${Constant.PRINT_LOG__DIRECTORY + time}.json",
                    Toast.LENGTH_LONG
                ).show()
            }
            dragEnable.setOnCheckedChangeListener { _, isChecked ->
                EasyFloat.dragEnable(isChecked, "floatPrint")
            }
            minifyWindow.setOnClickListener {
                if (EasyFloat.getFloatView("floatControl") != null) {
                    EasyFloat.show("floatControl")
                } else {
                    initControlFloat()
                }
                EasyFloat.hide("floatPrint")
            }

        }
        handler.postDelayed(refresh, 500)
    }

    private fun initControlFloat() {
        EasyFloat.with(requireActivity())
            .setLayout(ControlView(requireContext())) {
                it.setOnClickListener {
                    EasyFloat.show("floatPrint")
                    EasyFloat.hide("floatControl")
                }
            }
            .setTag("floatControl")
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            .setDragEnable(true)
            .setLocation(100, 200)
            .setAnimator(DefaultAnimator())
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}