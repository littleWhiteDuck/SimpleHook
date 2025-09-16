package me.simpleHook.recyclerview.adapter


import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.ContextMenu
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.data.ConfigBean
import me.simpleHook.constant.Constant
import me.simpleHook.hook.util.Type
import me.simpleHook.ui.view.config.ConfigItemView
import me.simpleHook.ui.view.config.RoundBackgroundColorSpan
import me.simpleHook.util.HookModeUtil
import me.simpleHook.extension.dp
import me.simpleHook.extension.random
import org.json.JSONObject

class ConfigAdapter(
    private val onClick: (position: Int) -> Unit,
    private val menuListener: (position: Int, menu: ContextMenu) -> Unit,
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
        val desc = containerView.desc
    }

    @SuppressLint("ResourceType")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val configView = if (isCollect) {
            ConfigItemView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                radius = 0f
            }
        } else {
            ConfigItemView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(5.dp, 5.dp, 5.dp, 0)
                }
                cardElevation = 2f.dp
                radius = 5f.dp
            }
        }
        val viewHolder = ViewHolder(configView)
        viewHolder.apply {
            itemView.apply {
                setOnClickListener {
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    onClick(position)
                }
                setOnCreateContextMenuListener { menu, _, _ ->
                    val position: Int = viewHolder.itemView.getTag(R.id.item_position) as Int
                    menuListener(position, menu)
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
            val tempClassName =
                if (methodConfig.mode == Constant.HOOK_STATIC_FIELD) methodConfig.fieldClassName else methodConfig.className
            if (methodConfig.className.length >= 30) {
                tvClassName.text = getClassSimpleName(tempClassName)
            } else {
                tvClassName.text = tempClassName
            }
            when (methodConfig.mode) {
                Constant.HOOK_BREAK -> {
                    val showText = "${methodConfig.methodName}(${methodConfig.params})"
                    val spannableString = SpannableString(showText).also {
                        it.setSpan(StrikethroughSpan(),
                            0,
                            showText.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                    tvOtherName.text = spannableString
                }
                Constant.HOOK_FIELD, Constant.HOOK_STATIC_FIELD -> {
                    val showText = "${methodConfig.fieldName} -> ${
                        transformValue(Type.getDataTypeValue(methodConfig.resultValues))
                    }"
                    tvOtherName.text = showText
                }
                Constant.HOOK_RECORD_PARAMS, Constant.HOOK_RECORD_RETURN, Constant.HOOK_RECORD_PARAMS_RETURN -> {
                    val showText = "${methodConfig.methodName}(${methodConfig.params})"
                    val spannableString = SpannableString(showText).also {
                        it.setSpan(StyleSpan(Typeface.ITALIC),
                            0,
                            showText.length,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
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
                    val returnValue = Type.getDataTypeValue(methodConfig.resultValues)
                    val resultValue = if (returnValue is String) {
                        try {
                            val jsonObject = JSONObject(returnValue)
                            if (jsonObject.has("random") && jsonObject.has("length") && jsonObject.has(
                                    "key")
                            ) {
                                val randomSeed = jsonObject.optString("random", "abcdefgh123456789")
                                val len = jsonObject.optInt("length", 10)
                                "\"${randomSeed.random(len)}\""
                            } else {
                                transformValue(Type.getDataTypeValue(methodConfig.resultValues))
                            }
                        } catch (e: Exception) {
                            transformValue(Type.getDataTypeValue(methodConfig.resultValues))
                        }
                    } else {
                        transformValue(Type.getDataTypeValue(methodConfig.resultValues))
                    }
                    val showText =
                        "${methodConfig.methodName}(${methodConfig.params}) -> $resultValue"
                    tvOtherName.text = showText
                }
                else -> {
                    tvOtherName.text = methodConfig.methodName.ifEmpty { methodConfig.fieldName }
                }
            }
            tvNumber.text = (position + 1).toString()
            enable.isChecked = methodConfig.enable
            val tipText = HookModeUtil.getShowText(methodConfig.mode, context)
            val span = SpannableString("$tipText ").also {
                @Suppress("DEPRECATION")
                val bgColor = context.resources.getColor(R.color.config_tag_background)

                @Suppress("DEPRECATION")
                val textColor = context.resources.getColor(R.color.config_tag_text_color)
                it.setSpan(RoundBackgroundColorSpan(bgColor, textColor),
                    0,
                    tipText.length,
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
            }
            tip.text = span

            desc.text = methodConfig.desc
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