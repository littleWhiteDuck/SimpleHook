package me.simpleHook.hook.extension

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import me.simpleHook.bean.ExtensionConfigBean

object ContactHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean) {
        findAllMethods(ContentResolver::class.java) {
            name == "query"
        }.hookBefore {
            Log.d("littleWhiteDuck", "startHook: ")
            val uri = it.args[0] as Uri
            if (uri == ContactsContract.CommonDataKinds.Phone.CONTENT_URI) {
                it.result = null
            }
        }
    }

}