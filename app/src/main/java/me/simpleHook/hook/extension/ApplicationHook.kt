package me.simpleHook.hook.extension

import android.app.Application
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import kotlinx.serialization.json.Json
import me.simpleHook.data.Exit
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordApplication
import me.simpleHook.data.record.RecordCrash
import me.simpleHook.data.record.RecordType
import me.simpleHook.hook.utils.RecordOutHelper

object ApplicationHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.application || extensionConfig.exit.enable) {
            findMethod(Application::class.java) {
                name == "onCreate"
            }.hookAfter {
                if (extensionConfig.application) {
                    val className = it.thisObject.javaClass.name
                    RecordOutHelper.outputRecord(
                        type = RecordType.Application,
                        RecordApplication(name = className)
                    )
                }
                if (extensionConfig.exit.enable) {
                    val exit = Json.decodeFromString<Exit>(extensionConfig.exit.info)
                    if (exit.recordCrash) recordCrash()
                }
            }
        }

    }

    private fun recordCrash() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            t ?: return@setDefaultUncaughtExceptionHandler
            RecordOutHelper.outputRecord(
                type = RecordType.CrashCaught,
                record = RecordCrash(threadName = t.name, stackDetail = e.stackTraceToString())
            )
        }
    }
}