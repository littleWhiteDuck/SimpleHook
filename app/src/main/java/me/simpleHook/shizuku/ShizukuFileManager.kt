package me.simpleHook.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import me.simpleHook.BuildConfig
import rikka.shizuku.Shizuku

object ShizukuFileManager {
    var service: IFileService? = null
        private set

    val isAvailable get() = service != null

    var binderAvailable = false

    var isPermissionGranted = false

    val rootMode get() = binderAvailable && Shizuku.getUid() == 0

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, FileService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shk_config")
        .debuggable(BuildConfig.DEBUG)
        .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Log.d("littleWhiteDuck", "onServiceConnected: ")
            service = IFileService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("littleWhiteDuck", "onServiceDisconnected: ")
            service = null
        }
    }


    fun init() {

        Shizuku.addBinderReceivedListener {
            binderAvailable = true
            isPermissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            Log.d("littleWhiteDuck", "init: receive")
            if (isPermissionGranted) {
                bindService()
            }
        }

        Shizuku.addBinderDeadListener {
            binderAvailable = false
            Log.d("littleWhiteDuck", "init: dead")
        }
    }

    fun bindService() {
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }


}
