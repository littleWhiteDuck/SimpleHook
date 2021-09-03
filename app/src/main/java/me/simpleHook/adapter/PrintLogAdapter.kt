package me.simpleHook.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.ItemPrintLogBinding
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.ToolUtils
import me.simpleHook.util.snack
import me.simpleHook.util.toast

class PrintLogAdapter : RecyclerView.Adapter<PrintLogAdapter.ViewHolder>() {
    private var dataList: List<PrintLog> = ArrayList()

    inner class ViewHolder(binding: ItemPrintLogBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvLog = binding.logView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemPrintLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val printLog = viewHolder.itemView.getTag(R.id.item_print_position) as PrintLog
            val message = JsonUtil.formatJson(printLog.log.replace("\\u003e", ">"))
            val dialog = MaterialAlertDialogBuilder(parent.context)
                .setMessage(message)
                .setPositiveButton("复制") { dialog, _ ->
                    ToolUtils.toClip(parent.context, message)
                    "已复制".toast(parent.context)
                    dialog.dismiss()
                }.setNegativeButton("取消", null)
                .create()
            if (Build.VERSION.SDK_INT >= 26) {
                dialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                dialog.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
            dialog.show()
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = dataList[position]
        holder.itemView.setTag(R.id.item_print_position, printLog)
        val logBean = Gson().fromJson(printLog.log, LogBean::class.java)
        holder.tvLog.text = logBean.type
    }

    fun setDataList(list: List<PrintLog>) {
        dataList = list
    }

    override fun getItemCount() = dataList.size
}