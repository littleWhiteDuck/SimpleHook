package me.simpleHook.util


import android.content.Context
import android.os.Looper
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import kotlin.concurrent.thread

object SuUtil {
    private val session: RxCmdShell.Session = RxCmdShell.builder().build().open().blockingGet()
    var isRoot = false
    fun init(context: Context) {
        thread {
            val root = Root.Builder().build().blockingGet()
            isRoot = root.state == Root.State.ROOTED
            if (root.state == Root.State.ROOTED) {
                Cmd.builder(
                    "su",
                    "mount -o remount /data",
                    "cd /data/local/tmp",
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


    fun saveConfig(filePath: String, fileName: String, content: String) {

        thread {
            Cmd.builder(
                "mkdir -p $filePath", "cd $filePath", "echo '$content' > $fileName"
            ).execute(session)
        }
    }

    fun deleteConfig(filePath: String) {
        if (filePath.contains("simpleHook")) {
            Cmd.builder(
                "rm -rf $filePath"
            ).execute(session)
        }
    }

    fun deleteFile(filePath: String) {
        Cmd.builder(
            "rm -rf $filePath"
        ).execute(session)
    }
}