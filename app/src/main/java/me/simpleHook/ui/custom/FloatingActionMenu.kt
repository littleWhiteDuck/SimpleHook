package me.simpleHook.ui.custom

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import androidx.core.view.marginBottom
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.simpleHook.R
import me.simpleHook.extension.dp

@SuppressLint("RestrictedApi")
class FloatingActionMenu(context: Context, attrs: AttributeSet) : CustomViewGroup(context, attrs) {
    private var isShow = false
    private val expandAnimatorSet = AnimatorSet()
    private val collapseAnimatorSet = AnimatorSet()
    private val expandAnimators = mutableListOf<Animator>()
    private val collapseAnimators = mutableListOf<Animator>()
    private val menuButton =
        FloatingActionButton(ContextThemeWrapper(context, R.style.FloatButtonTheme)).apply {
            size = FloatingActionButton.SIZE_NORMAL
            layoutParams =
                MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                    it.topMargin = 2.dp
                }
            setOnClickListener {
                isShow = !isShow
                if (isShow) {
                    showButton()
                } else {
                    hideButton()
                }
                rotateFloatingButton()
            }
        }


    init {
        clipChildren = false
        clipToPadding = false
        val typeValue = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionMenu)
        val srcDrawable = typeValue.getDrawable(R.styleable.FloatingActionMenu_menu_src)
        srcDrawable?.let { menuButton.setImageDrawable(srcDrawable) }
        val tint = typeValue.getColorStateList(R.styleable.FloatingActionMenu_menu_tint)
        menuButton.imageTintList = tint
        typeValue.recycle()
        addView(menuButton)
        hideButton()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = 0
        var height = 0
        children.forEach {
            if (it !is FloatingActionButton) {
                it.autoMeasure()
                width = maxOf(width, it.measuredWidth)
                height += it.measuredHeightWithMargins
            }
        }
        menuButton.autoMeasure()
        setMeasuredDimension(
            menuButton.measuredWidth / 2 + width, height + menuButton.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        menuButton.autoLayout(
            0, 0, true, fromBottom = true
        )
        var i = 0L
        var j = 150L
        var offsetY = menuButton.measuredHeightWithMargins
        children.forEach {
            if (it !is FloatingActionButton) {
                it.autoLayout(
                    menuButton.measuredWidth / 2 - (it as me.simpleHook.ui.custom.FloatingActionButton).actionButton.measuredWidth / 2,
                    offsetY + it.marginBottom,
                    fromRight = true,
                    fromBottom = true
                )
                offsetY += it.measuredHeightWithMargins

                val animatorAlpha = ObjectAnimator.ofFloat(it, "actionViewAlpha", 0f, 1f)
                animatorAlpha.startDelay = i
                i += 50L
                expandAnimators.add(animatorAlpha)

                val animatorReverseAlpha = ObjectAnimator.ofFloat(it, "actionViewAlpha", 1f, 0f)
                animatorReverseAlpha.startDelay = j
                j -= 50L
                collapseAnimators.add(animatorReverseAlpha)
            }
        }

    }

    private fun rotateFloatingButton() {
        if (isShow) {
            menuButton.animate().rotation(-45f).setDuration(300).start()
        } else {
            menuButton.animate().rotation(90f).setDuration(300).start()
        }
    }

    private fun showButton() {
        collapseAnimatorSet.cancel()
        expandAnimatorSet.playTogether(expandAnimators)
        expandAnimatorSet.duration = 300
        expandAnimatorSet.start()
    }


    private fun hideButton() {
        expandAnimatorSet.cancel()
        collapseAnimatorSet.playTogether(collapseAnimators)
        collapseAnimatorSet.duration = 300
        collapseAnimatorSet.start()
    }


    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}