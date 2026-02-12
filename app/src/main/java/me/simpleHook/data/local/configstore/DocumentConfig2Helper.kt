package me.simpleHook.data.local.configstore

import me.simpleHook.core.utils.PermissionUtil

class DocumentConfig2Helper : DocumentConfigBaseHelper() {

    override fun hasPermission(packageName: String): Boolean {
        return PermissionUtil.isGrantPackage(packageName)
    }
}
