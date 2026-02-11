package me.simpleHook.config

import me.simpleHook.utils.PermissionUtil

class DocumentConfig2Helper : DocumentConfigBaseHelper() {

    override fun hasPermission(packageName: String): Boolean {
        return PermissionUtil.isGrantPackage(packageName)
    }
}
