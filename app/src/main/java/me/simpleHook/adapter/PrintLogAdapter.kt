package me.simpleHook.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.custom.LogDetailDialog
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.databinding.LogItemBinding
import me.simpleHook.util.PhoneUtils
import org.json.JSONObject

class PrintLogAdapter : RecyclerView.Adapter<PrintLogAdapter.ViewHolder>() {
    private var dataList: List<PrintLog> = ArrayList()

    inner class ViewHolder(binding: LogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvLog = binding.logView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val printLog = viewHolder.itemView.getTag(R.id.item_print_position) as PrintLog
            val textView = TextView(parent.context)
            textView.text = printLog.log
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printLog = dataList[position]
        holder.itemView.setTag(R.id.item_print_position, printLog)
        val jsonObject = JSONObject(printLog.log)
        holder.tvLog.text = jsonObject.getString("type")
    }

    fun setDataList(list: List<PrintLog>) {
        dataList = list
    }

    override fun getItemCount() = dataList.size
}