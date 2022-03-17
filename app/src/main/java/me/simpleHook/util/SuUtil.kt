package me.simpleHook.util


import android.content.Context
import android.os.Looper
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import kotlin.concurrent.thread

object SuUtil {
    private val session: RxCmdShell.Session = RxCmdShell.builder().build().open().blockingGet()
    fun init(context: Context) {
        thread {
            val root = Root.Builder().build().blockingGet()
            if (root.state == Root.State.ROOTED) {
                Cmd.builder(
                    "su",
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
        thread {
            Cmd.builder("chmod -R 777 /data/simpleHook").execute(session)
        }
    }

    fun set666() {
        thread {
            Cmd.builder("chmod -R 666 /data/simpleHook/logTemp/log.txt").execute(session)
        }
    }

    fun saveConfig(filePath: String, fileName: String, content: String) {
        thread {
            Cmd.builder(
                "mkdir -p $filePath", "cd $filePath", "echo -e '$content' > $fileName"
            ).execute(session)
        }
    }

    fun deleteConfig(filePath: String) {
        if (filePath.contains("simpleHook") && filePath.endsWith("Config.json", true)) {
            Cmd.builder(
                "rm -f $filePath"
            ).execute(session)
        }
    }
}