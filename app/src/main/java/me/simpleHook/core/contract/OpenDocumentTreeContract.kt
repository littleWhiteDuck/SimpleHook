package me.simpleHook.core.contract

import android.app.Activity
import android.content.Intent
import android.net.Uri

class OpenDocumentTreeContract : BaseOpenDocumentTreeContract<Uri>() {

    override fun parseResult(resultCode: Int, intent: Intent?): Uri {
        return if (intent == null || resultCode != Activity.RESULT_OK) Uri.EMPTY else intent.data
            ?: Uri.EMPTY
    }
}
