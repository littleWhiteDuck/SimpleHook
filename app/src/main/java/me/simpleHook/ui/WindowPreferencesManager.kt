package me.simpleHook.ui


//noinspection SuspiciousImport
import android.R
import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors


class WindowPreferencesManager(private val context: Context) {

    private val isEdgeToEdgeEnabled: Boolean
        get() = true

    @Suppress("DEPRECATION")
    fun applyEdgeToEdgePreference(window: Window) {
        val edgeToEdgeEnabled = isEdgeToEdgeEnabled
        val navbarColor = getNavBarColor(isEdgeToEdgeEnabled)
        val lightBackground =
            isColorLight(MaterialColors.getColor(context, R.attr.colorBackground, Color.BLACK))
        val lightNavbar = isColorLight(navbarColor)
        val showDarkNavbarIcons = lightNavbar || navbarColor == Color.TRANSPARENT && lightBackground
        val decorView = window.decorView
        val currentStatusBar = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val currentNavBar =
            if (showDarkNavbarIcons && VERSION.SDK_INT >= VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        window.navigationBarColor = navbarColor
        val systemUiVisibility =
            ((if (edgeToEdgeEnabled) EDGE_TO_EDGE_FLAGS else View.SYSTEM_UI_FLAG_VISIBLE) or currentStatusBar or currentNavBar)
        decorView.systemUiVisibility = systemUiVisibility
        if (VERSION.SDK_INT >= VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    }

    private fun getNavBarColor(isEdgeToEdgeEnabled: Boolean): Int {
        if (isEdgeToEdgeEnabled && VERSION.SDK_INT < VERSION_CODES.O_MR1) {
            val opaqueNavBarColor =
                MaterialColors.getColor(context, R.attr.navigationBarColor, Color.BLACK)
            return ColorUtils.setAlphaComponent(opaqueNavBarColor, EDGE_TO_EDGE_BAR_ALPHA)
        }
        return if (isEdgeToEdgeEnabled) {
            Color.TRANSPARENT
        } else MaterialColors.getColor(context, R.attr.navigationBarColor, Color.BLACK)
    }

    companion object {
        private const val EDGE_TO_EDGE_BAR_ALPHA = 128

        @Suppress("DEPRECATION")
        private const val EDGE_TO_EDGE_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        fun isColorLight(
            @ColorInt
            color: Int
        ): Boolean {
            return color != Color.TRANSPARENT && ColorUtils.calculateLuminance(color) > 0.5
        }
    }
}