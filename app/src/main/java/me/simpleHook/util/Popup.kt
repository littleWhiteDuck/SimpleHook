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
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.card.MaterialCardView
import me.simpleHook.extension.dp
import me.simpleHook.extension.getColorByAttr


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
            title.text = message
            title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setOnClickListener {
                dismissActivePopup()
            }
        }
        dismissActivePopup()
        showNewPopup(contentView, duration)
    }

    /**
     * The calling Activity of View must apply the MaterialTheme
     */
    fun show(rootView: View, title: String, message: String, duration: Long = 1500) {
        show(rootView, message = buildSpannedString {
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
     * The calling Activity of View must apply the MaterialTheme
     */
    fun show(rootView: View, message: CharSequence, duration: Long = 1500) {
        val contentView = ContentView(rootView.context).apply {
            title.text = message
            title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setOnClickListener {
                dismissActivePopup()
            }
        }
        dismissActivePopup()
        showNewPopup(contentView, duration, rootView)
    }

    private fun showNewPopup(contentView: View, duration: Long, rootView: View? = null) {
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
        popupWindow.showAtLocation(rootView ?: contentView, Gravity.TOP, 0, 0)

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

private class ContentView(context: Context) : FrameLayout(context) {
    val titleView = TitleCardView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
            it.setMargins(16.dp, 32.dp, 16.dp, 8.dp)
        }
        fitsSystemWindows = true
        strokeWidth = 0
        elevation = 5f.dp
        minimumHeight = 48.dp
        setCardBackgroundColor(context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceVariant))
    }
    val title get() = titleView.title

    init {
        addView(titleView)
    }
}


private class TitleCardView(context: Context) : MaterialCardView(context) {
    val title = TextView(context).apply {
        layoutParams =
            MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(16.dp, 4.dp, 8.dp, 4.dp)
            }
        textSize = 16f
        maxLines = 6
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
        ellipsize = TextUtils.TruncateAt.END
        addView(this)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val l = (measuredWidth - title.measuredWidth) / 2
        val t = (measuredHeight - title.measuredHeight) / 2
        title.layout(l, t, l + title.measuredWidth, t + measuredHeight)
    }
}
