package me.simpleHook.adapter

import android.os.Build
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import me.simpleHook.R
import me.simpleHook.bean.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.ItemPrintLogBinding
import me.simpleHook.ui.custom.LogDetailDialog
import me.simpleHook.util.JsonUtil

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
            val textView = TextView(parent.context)
            textView.text = JsonUtil.formatJson(printLog.log.replace("\\u003e", ">"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small)
            }
            textView.movementMethod = ScrollingMovementMethod.getInstance()
            textView.setTextIsSelectable(true)
            val dialog = LogDetailDialog(parent.context, textView)
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