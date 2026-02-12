package me.simpleHook.feature.backup.domain

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import me.simpleHook.core.GlobalValue
import java.io.File

object CloudBackupHelper {
    private const val BACKUP_PATH = "SimpleHook/Backups/"
    private fun getAccount(): OkHttpSardine? = runCatching {
        val sardine = OkHttpSardine().apply {
            setCredentials(GlobalValue.sp.web_dav_account, GlobalValue.sp.web_dav_pw)
            if (!exists("${GlobalValue.sp.web_dav_host}SimpleHook")) {
                createDirectory("${GlobalValue.sp.web_dav_host}SimpleHook")
            }
            if (!exists("${GlobalValue.sp.web_dav_host}$BACKUP_PATH")) {
                createDirectory("${GlobalValue.sp.web_dav_host}$BACKUP_PATH")
            }
        }
        sardine
    }.getOrNull()

    fun getBackups(): List<DavResource> = runCatching {
        return getAccount()?.list(GlobalValue.sp.web_dav_host + BACKUP_PATH) ?: emptyList()
    }.getOrDefault(emptyList())

    fun uploadFile(file: File): Boolean = runCatching {
        val account = getAccount() ?: return@runCatching false
        account.put("${GlobalValue.sp.web_dav_host}$BACKUP_PATH${file.name}",
            file,
            "application/x-www-form-urlencoded")
        true
    }.getOrDefault(false)

    fun getUriByName(fileName: String): String {
        return "${GlobalValue.sp.web_dav_host}$BACKUP_PATH${fileName}"
    }

}