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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.adapter.PrintLogAdapter
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.FragmentFloatBinding
import me.simpleHook.ui.view.ControlView
import me.simpleHook.util.FileUtils
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


class FloatFragment : Fragment() {


    private val viewModel by activityViewModels<AppViewModel>()
    private var _binding: FragmentFloatBinding? = null
    private val binding get() = _binding!!
    private val mAdapter: PrintLogAdapter by lazy { PrintLogAdapter() }
    private val list = ArrayList<PrintLog>()
    private val handler = Handler(Looper.getMainLooper())
    private val assistConfigs by lazy { viewModel.getAssistConfigs() }
    private val configs by lazy { viewModel.getConfigs() }
    private val refresh = object : Runnable {
        override fun run() {
            readFileLogInsert()
            updateData()
            handler.postDelayed(this, 500)
        }
    }
    private val uri = Uri.parse("content://littleWhiteDuck/print_logs")
    private var stopPrint = false
    private var currentTime = ""
    private var strLog = ""
    private val exportLog =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { resultUri ->
            resultUri?.also {
                thread {
                    FileUtils.alterDocument(
                        requireContext(), it, JsonUtil.formatJson("[\n${strLog}\n]")
                    )
                }
            }
        }

    private fun readFileLogInsert() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (FlavorUtils.isNormal()) {
                assistConfigs.forEach {
                    val list = FileUtils.readLogFile(requireContext(), it.packageName)
                    viewModel.insertRecord(*list.toTypedArray())
                }
                configs.forEach {
                    val list = FileUtils.readLogFile(requireContext(), it.packageName)
                    viewModel.insertRecord(*list.toTypedArray())
                }
            } else {
                val list = FileUtils.readLogFile()
                viewModel.insertRecord(*list.toTypedArray())
            }
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun updateData() {
        if (!isAdded) return
        requireContext().contentResolver.query(
            uri, null, "time > ?", arrayOf(currentTime), null
        )?.apply {
            while (moveToNext()) {
                val log = getString(getColumnIndex("log"))
                val packageName = getString(getColumnIndex("packageName"))
                val time = getString(getColumnIndex("time"))
                if (!stopPrint) list.add(
                    PrintLog(
                        log = log, packageName = packageName, time = time
                    )
                )
                currentTime = time
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
                        requireContext(), DividerItemDecoration.VERTICAL
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
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.assist_float_export_config_empty_tip),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                val date = Date()
                val time = SimpleDateFormat("yy_MM_dd_hh_mm_ss").format(date) + ".json"
                val writeList = list
                val str = StringBuilder()
                for (i in writeList.indices) {
                    str.append("  ${writeList[i].log},\n")
                }
                strLog =
                    str.toString().substring(0, str.toString().length - 2).replace("\\u003e", ">")
                exportLog.launch(time)
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
        currentTime = TimeUtil.getDateTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        handler.postDelayed(refresh, 500)
    }

    private fun initControlFloat() {
        EasyFloat.with(requireActivity()).setLayout(ControlView(requireContext())) {
            it.setOnClickListener {
                EasyFloat.show("floatPrint")
                EasyFloat.hide("floatControl")
            }
        }.setTag("floatControl").setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL).setDragEnable(true).setLocation(100, 200)
            .setAnimator(DefaultAnimator()).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}