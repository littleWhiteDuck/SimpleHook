package me.simpleHook.platform.hook.extension

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import me.simpleHook.data.ExtensionConfig

object ContactHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.contact) return
        findAllMethods(ContentResolver::class.java) {
            name == "query"
        }.hookBefore {
            val uri = it.args[0] as Uri
            if (uri == ContactsContract.CommonDataKinds.Phone.CONTENT_URI) {
                it.result = null
            }
        }
    }

}