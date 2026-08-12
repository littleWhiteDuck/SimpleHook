package me.simpleHook.data.local.configstore

import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.PermissionUtil

class DocumentConfigHelper : DocumentConfigBaseHelper() {

    override fun hasPermission(packageName: String): Boolean {
        return PermissionUtil.isGrantData(Constant.ANDROID_DATA_URI)
    }
}
