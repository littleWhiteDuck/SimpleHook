package me.simpleHook.core.extension

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import me.simpleHook.core.utils.Popup
import me.simpleHook.core.utils.ToolUtil

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}


/**
 * The calling Activity must apply the MaterialTheme
 */
fun Activity.showPopup(message: String, duration: Long = 1500) {
    Popup.show(this, message, duration)
}

/**
 * The calling Activity must apply the MaterialTheme
 */
fun Activity.showPopup(title: String, message: String, duration: Long = 1500) {
    Popup.show(this, title = title, message = message, duration = duration)
}

/**
 * The calling Activity must apply the MaterialTheme
 */
fun Activity.showPopupWithCopyMsg(title: String, message: String, duration: Long = 1500) {
    ToolUtil.toClip(this, message)
    Popup.show(this, title = title, message = message, duration = duration)
}


/**
 * The calling Activity of View must apply the MaterialTheme
 */
fun View.showPopup(message: String, duration: Long = 1500) {
    Popup.show(this, message, duration)
}

/**
 * The calling Activity of View must apply the MaterialTheme
 */
fun View.showPopup(title: String, message: String, duration: Long = 1500) {
    Popup.show(this, title = title, message = message, duration = duration)
}



