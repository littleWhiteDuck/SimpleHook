package me.simpleHook.ui.view.permission

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import me.simpleHook.R
import me.simpleHook.ui.custom.CustomViewGroup
import me.simpleHook.extension.dp

class PermissionSortView(context: Context) : CustomViewGroup(context) {

    private val sortModeTitle =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            text = context.getString(R.string.permission_sort_rule)
        }

    private val sortModeGroup = ChipGroup(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 5.dp
            }
        isSingleSelection = true
    }

    val nameChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_sort_app_name)
        isCheckable = true
    }
    val packageNameChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_sort__package_name)
        isCheckable = true
    }
    val installedTimeChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_sort_install_time)
        isCheckable = true
    }

    private val reverseModeGroup = ChipGroup(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 5.dp
            }
        isSingleSelection = true
    }
    val forwardSortChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_sort_forward)
        isCheckable = true
    }
    val reverseSortChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_sort_backward)
        isCheckable = true
    }

    private val showAppsTitle =
        AppCompatTextView(ContextThemeWrapper(context, R.style.text_view_item)).apply {
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 5.dp
                    text = context.getString(R.string.permission_show_what_app)
                }
        }
    private val showAppsGroup = ChipGroup(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 5.dp
            }
        isSingleSelection = true
    }
    val userAppChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_display_user_app)
        isCheckable = true
    }

    val systemAppChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_display_system_app)
        isCheckable = true
    }

    val allAppChip = Chip(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        text = context.getString(R.string.permission_display_all_app)
        isCheckable = true
    }


    init {
        sortModeGroup.addView(nameChip)
        sortModeGroup.addView(packageNameChip)
        sortModeGroup.addView(installedTimeChip)
        reverseModeGroup.addView(forwardSortChip)
        reverseModeGroup.addView(reverseSortChip)
        showAppsGroup.addView(userAppChip)
        showAppsGroup.addView(systemAppChip)
        showAppsGroup.addView(allAppChip)
        addView(sortModeTitle)
        addView(sortModeGroup)
        addView(reverseModeGroup)
        addView(showAppsTitle)
        addView(showAppsGroup)
        setPadding(10.dp, 5.dp, 10.dp, 5.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val actualWidth = measuredWidth - paddingStart - paddingEnd
        sortModeTitle.measure(
            actualWidth.toExactlyMeasureSpec(), sortModeTitle.defaultHeightMeasureSpec(this)
        )
        sortModeGroup.measure(
            actualWidth.toExactlyMeasureSpec(), sortModeGroup.defaultHeightMeasureSpec(this)
        )
        reverseModeGroup.measure(
            actualWidth.toExactlyMeasureSpec(), reverseModeGroup.defaultHeightMeasureSpec(this)
        )
        showAppsTitle.measure(
            actualWidth.toExactlyMeasureSpec(), showAppsTitle.defaultHeightMeasureSpec(this)
        )
        showAppsGroup.measure(
            actualWidth.toExactlyMeasureSpec(), showAppsGroup.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            paddingTop + paddingEnd + sortModeTitle.measuredHeightWithMargins + sortModeGroup.measuredHeightWithMargins + reverseModeGroup.measuredHeightWithMargins + showAppsTitle.measuredHeightWithMargins + showAppsGroup.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        sortModeTitle.autoLayout(paddingStart, paddingEnd)
        sortModeGroup.autoLayout(paddingStart, sortModeTitle.bottom + sortModeGroup.marginTop)
        reverseModeGroup.autoLayout(paddingStart, sortModeGroup.bottom + reverseModeGroup.marginTop)
        showAppsTitle.autoLayout(paddingStart, reverseModeGroup.bottom + showAppsTitle.marginTop)
        showAppsGroup.autoLayout(paddingStart, showAppsTitle.bottom + showAppsGroup.marginTop)
    }


}