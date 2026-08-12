package me.simpleHook.core.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.documentfile.provider.DocumentFile

abstract class BaseOpenDocumentTreeContract<T> : ActivityResultContract<Uri, T>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val initialUri = DocumentFile.fromTreeUri(context, input)?.uri ?: input
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
    }
}

