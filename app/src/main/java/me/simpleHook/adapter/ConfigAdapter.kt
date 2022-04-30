package me.simpleHook.adapter


import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.bean.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.hook.Type
import me.simpleHook.ui.custom.PopupWindowList
import me.simpleHook.ui.view.config.ConfigItemView
import me.simpleHook.ui.view.config.RoundBackgroundColorSpan
import me.simpleHook.util.dp
import me.simpleHook.util.marquee

class ConfigAdapter(
    private val onClick: (position: Int) -> Unit,
    private val onLongClick: (position: Int) -> Unit,
    private val onCheckedChange: (position: Int, isChecked: Boolean) -> Unit,
    private val isCollect: Boolean = false
) : ListAdapter<ConfigBean, ConfigAdapter.ViewHolder>(MethodConfigDiffCallback) {


    inner class ViewHolder(configItemView: ConfigItemView) :
        RecyclerView.ViewHolder(configItemView) {
        private val containerView = configItemView.containerView
        val tvClassName = containerView.className
        val tvOtherName = containerView.otherName
        val tvNumber = containerView.num
        val tip = containerView.tip
        val enable = containerView.enable
    }

    @SuppressLint("ResourceType")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val configView = if (isCollect) {
            ConfigItemView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                radius = 0f
            }
        } else {
            ConfigItemView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.setMargins(5.dp, 5.dp, 5.dp, 0)
                }
                cardElevation = 2f.dp
                radius = 5f.dp
            }
        }
        val viewHolder = ViewHolder(configView)
        viewHolder.apply {
            tvClassName.marquee()
            tvOtherName.marquee()
            itemView.apply {
                PopupWindowList.Builder(parent.context).watchView(this)
                setOnClickListener {
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    onClick(position)
                }
                setOnLongClickListener {
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    onLongClick(position)
                    true
                }
            }
            enable.setOnCheckedChangeListener { _, isChecked ->
                val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                onCheckedChange(position, isChecked)
            }
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setTag(R.id.item_position, position)
        val methodConfig = getItem(position)
        val context = holder.itemView.context
        holder.apply {
            if (methodConfig.className.length >= 30) {
                tvClassName.text = getClassSimpleName(methodConfig.className)
            } else {
                tvClassName.text = methodConfig.className
            }
            when (methodConfig.mode) {
                Constant.HOOK_BREAK -> {
                    val showText = "${methodConfig.methodName}(${methodConfig.params})"
                    val spannableString = SpannableString(showText).also {
                        it.setSpan(
                            StrikethroughSpan(),
                            0,
                            showText.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                    tvOtherName.text = spannableString
                }
                Constant.HOOK_FIELD, Constant.HOOK_STATIC_FIELD -> {
                    val showText = "${methodConfig.fieldName} -> ${
                        transformValue(
                            Type.getDataTypeValue(methodConfig.resultValues)
                        )
                    }"
                    tvOtherName.text = showText
                }
                Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS_RETURN -> {
                    val showText = "${methodConfig.methodName}(${methodConfig.params})"
                    val spannableString = SpannableString(showText).also {
                        it.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            0,
                            showText.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                    tvOtherName.text = spannableString
                }
                Constant.HOOK_PARAM -> {
                    if (methodConfig.params == "*" || methodConfig.resultValues == "") {
                        val showText =
                            "${methodConfig.methodName}(${methodConfig.params}) -> ${methodConfig.resultValues}"
                        tvOtherName.text = showText
                    } else {
                        val params = methodConfig.params.split(",")
                        val values = methodConfig.resultValues.split(",")
                        var temp = ""
                        for (i in params.indices) {
                            var value = ""
                            if (i <= values.size - 1) {
                                value = values[i]
                            }
                            temp += if (value == "") {
                                params[i]
                            } else {
                                params[i] + "->" + transformValue(Type.getDataTypeValue(value))
                            }
                            if (i != params.size - 1) temp += ","
                        }
                        val showText = "${methodConfig.methodName}(${temp})"
                        tvOtherName.text = showText
                    }

                }
                Constant.HOOK_RETURN -> {
                    val showText = "${methodConfig.methodName}(${methodConfig.params}) -> ${
                        transformValue(
                            Type.getDataTypeValue(methodConfig.resultValues)
                        )
                    }"
                    tvOtherName.text = showText
                }
                else -> {
                    tvOtherName.text = methodConfig.methodName.ifEmpty { methodConfig.fieldName }
                }
            }
            tvNumber.text = (position + 1).toString()
            enable.isChecked = methodConfig.enable
            val tipText = when (methodConfig.mode) {
                Constant.HOOK_RETURN -> context.getString(R.string.config_mode_tip_return_value)
                Constant.HOOK_PARAM -> context.getString(R.string.config_mode_tip_param_value)
                Constant.HOOK_STATIC_FIELD, Constant.HOOK_FIELD -> context.getString(R.string.config_mode_tip_field_value)
                Constant.HOOK_BREAK -> context.getString(R.string.config_mode_tip_break)
                Constant.HOOK_RECORD_PARAMS -> context.getString(R.string.config_mode_tip_record_param_value)
                Constant.HOOK_RECORD_RETURN -> context.getString(R.string.config_mode_tip_record_return_value)
                Constant.HOOK_RECORD_PARAMS_RETURN -> context.getString(R.string.config_mode_tip_record_param_return_value)
                else -> "未知"
            }
            val span = SpannableString("$tipText ").also {
                val bgColor = context.resources.getColor(R.color.config_tag_background)
                val textColor = context.resources.getColor(R.color.config_tag_text_color)
                it.setSpan(
                    RoundBackgroundColorSpan(bgColor, textColor),
                    0,
                    tipText.length,
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            tip.text = span
        }
    }

    private fun transformValue(value: Any?): String {
        return if (value is String) {
            "\"$value\""
        } else {
            value.toString()
        }
    }

    private fun getClassSimpleName(classStr: String): String {
        return if (classStr.contains(".")) {
            val classStrNames = classStr.split(".")
            classStrNames[classStrNames.size - 1]
        } else {
            classStr
        }
    }

    object MethodConfigDiffCallback : DiffUtil.ItemCallback<ConfigBean>() {
        override fun areItemsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className && oldItem.methodName == newItem.methodName && oldItem.mode == newItem.mode && oldItem.params == newItem.params && oldItem.resultValues == newItem.resultValues && oldItem.fieldClassName == newItem.fieldClassName && oldItem.fieldName == newItem.fieldName
        }

        override fun areContentsTheSame(oldItem: ConfigBean, newItem: ConfigBean): Boolean {
            return oldItem.className == newItem.className && oldItem.methodName == newItem.methodName && oldItem.mode == newItem.mode && oldItem.params == newItem.params && oldItem.resultValues == newItem.resultValues && oldItem.fieldClassName == newItem.fieldClassName && oldItem.fieldName == newItem.fieldName
        }
    }

}