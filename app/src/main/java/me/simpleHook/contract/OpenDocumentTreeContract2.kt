package me.simpleHook.contract

import android.app.Activity
import android.content.Intent

class OpenDocumentTreeContract2 : BaseOpenDocumentTreeContract<Intent?>() {

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent
    }
}
