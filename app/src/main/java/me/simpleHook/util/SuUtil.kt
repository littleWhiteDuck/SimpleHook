package me.simpleHook.util


import android.content.Context
import android.os.Looper
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import kotlin.concurrent.thread

object SuUtil {

    fun init(context: Context) {
        thread {
            val root = Root.Builder().build().blockingGet()
            if (root.state == Root.State.ROOTED) {
                val session = RxCmdShell.builder().build().open().blockingGet()
                Cmd.builder(
                    "su",
                    "setenforce 0",
                    "mount -o remount /data",
                    "cd /data/",
                    "mkdir simpleHook",
                    "chmod -R 777 simpleHook"
                ).execute(session)
            } else {
                Looper.prepare()
                "拒绝或没有Root权限".toast(context)
                Looper.loop()
            }
        }
    }

    fun set777() {
        val session = RxCmdShell.builder().build().open().blockingGet()
        thread {
            Cmd.builder("chmod -R 777 /data/simpleHook").execute(session)
        }
    }
}