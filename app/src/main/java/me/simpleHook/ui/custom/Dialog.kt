package me.simpleHook.ui.custom

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.simpleHook.R

fun requestPermissionDialog(context: Context, okClick: () -> Unit) {
    warningDialog(
        context,
        context.getString(R.string.main_request_storage_permission_title),
        context.getString(R.string.main_request_storage_permission_message),
        okText = context.getString(R.string.main_request_storage_permission_okText),
        okClick = okClick
    )
}

fun warningDialog(
    context: Context,
    title: String,
    message: String,
    okClick: () -> Unit = {},
    okText: String = "确认"
) {
    customDialog(
        context,
        title = title,
        message = message,
        okText = okText,
        okClick = { okClick() },
        cancelText = "取消"
    ).show()
}

fun customDialog(
    context: Context,
    title: String = "",
    message: String = "",
    okText: String = "",
    okClick: (DialogInterface) -> Unit = {},
    cancelText: String = "",
    cancelClick: (DialogInterface) -> Unit = {},
    neutralText: String = "",
    neutralClick: (DialogInterface) -> Unit = {},
    contentView: View? = null,
    cancelAble: Boolean = true
): AlertDialog {
    val customDialog = MaterialAlertDialogBuilder(context).setCancelable(cancelAble)
    customDialog.apply {
        if (title.isNotEmpty()) {
            setTitle(title)
        }
        if (message.isNotEmpty()) {
            setMessage(message)
        } else {
            setView(contentView)
        }
        if (okText.isNotEmpty()) {
            setPositiveButton(okText) { dialog, _ -> okClick(dialog) }
        }
        if (cancelText.isNotEmpty()) {
            setNegativeButton(cancelText) { dialog, _ -> cancelClick(dialog) }
        }
        if (neutralText.isNotEmpty()) {
            setNeutralButton(neutralText) { dialog, _ -> neutralClick(dialog) }
        }
    }
    return customDialog.create()
}