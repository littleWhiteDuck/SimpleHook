package me.simpleHook.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.card.MaterialCardView
import me.simpleHook.extension.dp
import me.simpleHook.extension.getColorByAttr
import me.simpleHook.ui.custom.CustomViewGroup


object Popup {
    private val handler = Handler(Looper.getMainLooper())
    private var activePopup: PopupWindow? = null

    /**
     * The calling Activity must apply the MaterialTheme
     */
    fun show(context: Context, title: String, message: String, duration: Long = 1500) {
        show(context, message = buildSpannedString {
            bold {
                append(title)
            }
            append("\n")
            scale(0.8f) {
                append(message)
            }
        }, duration)
    }

    /**
     * The calling Activity must apply the MaterialTheme
     */
    fun show(context: Context, message: CharSequence, duration: Long = 1500) {
        val contentView = ContentView(context).apply {
            titleView.title.text = message
            titleView.title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setOnClickListener {
                dismissActivePopup()
            }
        }
        dismissActivePopup()
        showNewPopup(contentView, duration)
    }

    private fun showNewPopup(contentView: View, duration: Long) {

        val popupWindow = PopupWindow(
            contentView,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        with(popupWindow) {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = false
            isFocusable = false
            activePopup = popupWindow
        }
        popupWindow.showAtLocation(contentView, Gravity.TOP, 0, 0)
        val springForce = SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }

        val statusBarHeight = WindowUtils.getStatusBarHeight(contentView.context)
        springAnimation(
            contentView,
            springForce,
            -(statusBarHeight - 8f.dp)
        ) {
            addEndListener { _, _, _, _ ->
                handler.postDelayed({
                    val animator =
                        ObjectAnimator.ofFloat(contentView, "translationY", 0f, (-300f).dp)
                    with(animator) {
                        animator.duration = 200
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) = Unit

                            override fun onAnimationEnd(animation: Animator) {
                                popupWindow.dismiss()
                            }

                            override fun onAnimationCancel(animation: Animator) = Unit

                            override fun onAnimationRepeat(animation: Animator) = Unit

                        })
                        interpolator = AccelerateInterpolator()
                        start()
                    }
                }, duration)
            }
            start()
        }
    }

    private fun dismissActivePopup() {
        activePopup?.dismiss()
        activePopup = null
    }

    private fun springAnimation(
        view: View,
        springForce: SpringForce,
        finalPosition: Float,
        init: SpringAnimation.() -> Unit
    ): SpringAnimation {
        return SpringAnimation(view, DynamicAnimation.TRANSLATION_Y).apply {
            spring = springForce.apply { this.finalPosition = finalPosition }
            init()
        }
    }
}

private class ContentView(context: Context) : CustomViewGroup(context) {
    val titleView = TitleView(context)
    private val cardView = MaterialCardView(context).apply {
        layoutParams = MarginLayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(16.dp, 32.dp, 16.dp, 8.dp)
        }
        fitsSystemWindows = true
        strokeWidth = 0
        elevation = 5f.dp
        setCardBackgroundColor(context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceVariant))
    }

    init {
        cardView.addView(titleView)
        addView(cardView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        cardView.exactWidth(measuredWidth - cardView.marginStart * 2)
        setMeasuredDimension(measuredWidth, cardView.measuredHeightWithMargins)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        cardView.autoLayout(x = cardView.marginStart, y = cardView.marginTop)
    }
}

private class TitleView(context: Context) : CustomViewGroup(context) {
    val title = TextView(context).apply {
        layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(16.dp, 4.dp, 8.dp, 4.dp)
            }
        textSize = 18f
        maxLines = 6
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
        ellipsize = TextUtils.TruncateAt.END
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        title.exactWidth(measuredWidth - title.marginStart * 2)
        setMeasuredDimension(measuredWidth, title.measuredHeightWithMargins)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        title.autoLayout(title.marginStart, title.toVerticalCenter(this))
    }
}