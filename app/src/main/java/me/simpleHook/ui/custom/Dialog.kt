package me.simpleHook.ui.custom

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.simpleHook.R


fun exitDialog(
    context: Context,
    okClick: (DialogInterface) -> Unit,
    neutralClick: (DialogInterface) -> Unit,
    cancelClick: (DialogInterface) -> Unit
) {
    customDialog(context,
        title = context.getString(R.string.save_config_warning),
        message = context.getString(R.string.save_config_warning_message),
        okText = context.getString(R.string.save_and_exit),
        okClick = {
            okClick(it)
        },
        neutralText = context.getString(R.string.exit),
        neutralClick = {
            neutralClick(it)
        },
        cancelText = context.getString(R.string.only_save),
        cancelClick = {
            cancelClick(it)
        }).show()
}


fun requestPermissionDialog(
    context: Context,
    message: String = context.getString(R.string.main_request_storage_permission_message),
    okClick: () -> Unit
) {
    warningDialog(context,
        context.getString(R.string.main_request_storage_permission_title),
        message,
        okText = context.getString(R.string.main_request_storage_permission_okText),
        okClick = okClick)
}

fun warningDialog(
    context: Context,
    title: String,
    message: String,
    okClick: () -> Unit = {},
    okText: String = context.getString(R.string.dialog_confirm)
) {
    customDialog(context,
        title = title,
        message = message,
        okText = okText,
        okClick = { okClick() },
        cancelText = context.getString(R.string.dialog_cancel)).show()
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