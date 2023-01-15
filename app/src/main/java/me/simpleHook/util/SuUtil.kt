package me.simpleHook.util


import android.content.Context
import android.os.Looper
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import me.simpleHook.constant.Constant
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

    fun copyFile(filePath: String, filePath2: String) {
        thread {
            try {
                Cmd.builder(
                    "mkdir -p $filePath2",
                    "cp $filePath $filePath2",
                    "chmod -R 777 $filePath2"
                ).execute(
                    session
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setRecordFile() {
        val filePath = Constant.ROOT_CONFIG_MAIN_DIRECTORY + "logTemp/"
        thread {
            Cmd.builder(
                "mkdir -p $filePath", "cd $filePath", "echo > log.txt", "chmod -R 666 $filePath"
            ).execute(session)
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