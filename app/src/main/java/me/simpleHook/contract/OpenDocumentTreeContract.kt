package me.simpleHook.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.documentfile.provider.DocumentFile

class OpenDocumentTreeContract : ActivityResultContract<Uri, Uri>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val documentFile = DocumentFile.fromTreeUri(context, input)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags =
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile!!.uri)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri {
        return if (intent == null || resultCode != Activity.RESULT_OK) Uri.EMPTY else intent.data!!
    }
}