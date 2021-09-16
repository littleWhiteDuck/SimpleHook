package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import me.simpleHook.R
import me.simpleHook.util.PhoneUtils


class PopupWindowList private constructor(
    private val builder: Builder,
    private val mContext: Context
) {
    private var popupWindowWidth: Int = 0
    private var popupWindowHeight: Int = 0
    var foscuable = false
    private lateinit var popupWindow: PopupWindow

    init {
        foscuable = builder.outsideTouchable
    }

    private var locationX = 0
    private var locationY = 0
    private var mOnItemClickListener: AdapterView.OnItemClickListener? = null


    fun show() {
        val width = PhoneUtils.getWindowWidth(mContext)

        val contentView = ListView(mContext)
        contentView.apply {
            adapter =
                ArrayAdapter(mContext, android.R.layout.simple_list_item_1, builder.itemList!!)
            background = ContextCompat.getDrawable(mContext, R.drawable.popup_shape)
            isVerticalScrollBarEnabled = false
            onItemClickListener = mOnItemClickListener
        }
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        popupWindowWidth = getMaxWidth(contentView) + 10
        if (popupWindowWidth == 0) {
            popupWindowWidth = width / 3
        }
        if (popupWindowHeight == 0) {
            popupWindowHeight = builder.itemList!!.size * contentView.measuredHeight
        }
        setXY()
        popupWindow = PopupWindow(contentView, popupWindowWidth, popupWindowHeight)

        popupWindow.apply {
            isOutsideTouchable = builder.outsideTouchable
            isFocusable = foscuable
            setBackgroundDrawable(ColorDrawable(-0))
            showAtLocation(contentView, Gravity.NO_GRAVITY, locationX, locationY)
        }
    }

    private fun setXY() {
        val width = PhoneUtils.getWindowWidth(mContext)
        val height = PhoneUtils.getAppHeight(mContext)
        val point = Builder.point
        locationY = if (point.y > height / 2) {
            point.y - popupWindowHeight
        } else {
            point.y
        }
        locationX = if (point.x > width / 2) {
            point.x - popupWindowWidth
        } else {
            point.x
        }
    }

    private fun getMaxWidth(listView: ListView): Int {
        var maxWidth = 0
        if (listView.adapter == null) {
            return maxWidth
        }
        val count = listView.adapter.count
        var view: View?
        for (i in 0 until count) {
            view = listView.adapter.getView(i, null, listView)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            if (view.measuredWidth > maxWidth) {
                maxWidth = view.measuredWidth
            }
        }
        return maxWidth
    }

    fun setOnItemClickListener(onItemClickListener: AdapterView.OnItemClickListener): PopupWindowList {
        mOnItemClickListener = onItemClickListener
        return this
    }

    fun dismiss() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    class Builder(private val mContext: Context) {
        companion object {
            val point = Point()
        }

        var itemList: Array<String>? = null

        var outsideTouchable = false

        fun setOutsideTouchable(touchable: Boolean): Builder {
            outsideTouchable = touchable
            return this
        }

        fun setItemList(list: Array<String>): Builder {
            itemList = list
            return this
        }

        @SuppressLint("ClickableViewAccessibility")
        fun watchView(view: View): Builder {
            view.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    point.set(event.rawX.toInt(), event.rawY.toInt())
                }
                false
            }
            return this
        }

        fun build(): PopupWindowList {
            return PopupWindowList(this, mContext)
        }
    }


}