package me.simpleHook.config

import me.simpleHook.constant.Constant
import me.simpleHook.utils.PermissionUtil

class DocumentConfigHelper : DocumentConfigBaseHelper() {

    override fun hasPermission(packageName: String): Boolean {
        return PermissionUtil.isGrantData(Constant.ANDROID_DATA_URI)
    }
}
