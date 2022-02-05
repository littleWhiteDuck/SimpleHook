package me.simpleHook.ui.custom

import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
    )
}

fun customDialog(
    context: Context,
    title: String = "",
    message: String = "",
    okText: String,
    okClick: (DialogInterface) -> Unit = {},
    cancelText: String,
    cancelClick: (DialogInterface) -> Unit = {},
    neutralText: String = "",
    neutralClick: (DialogInterface) -> Unit = {},
    contentView: View? = null,
    cancelAble: Boolean = true
) {
    val customDialog = MaterialAlertDialogBuilder(context)
        .setPositiveButton(okText) { dialog, _ -> okClick(dialog) }
        .setNegativeButton(cancelText) { dialog, _ -> cancelClick(dialog) }
        .setCancelable(cancelAble)
    customDialog.apply {
        if (title.isNotEmpty()){
            setTitle(title)
        }
        if (message.isNotEmpty()) {
            setMessage(message)
        } else {
            setView(contentView)
        }
        if (neutralText.isNotEmpty()) {
            setNeutralButton(neutralText) { dialog, _ -> neutralClick(dialog) }
        }
        create()
        show()
    }
}