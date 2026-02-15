package me.simpleHook.platform.hook.extension

import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordApplication
import me.simpleHook.data.record.RecordCrash
import me.simpleHook.data.record.RecordType
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper

object ApplicationHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.application || extensionConfig.exitConfig.enable) {
            if (extensionConfig.application) {
                RecordOutHelper.outputRecord(
                    type = RecordType.Application,
                    RecordApplication(name = HookHelper.applicationName)
                )
            }
            if (extensionConfig.exitConfig.enable && extensionConfig.exitConfig.recordCrash) {
                recordCrash()
            }
        }

    }

    private fun recordCrash() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val threadName = t?.name ?: "unknown"
                RecordOutHelper.outputRecord(
                    type = RecordType.CrashCaught,
                    record = RecordCrash(threadName = threadName, stackDetail = e.stackTraceToString())
                )
            }
            previousHandler?.uncaughtException(t, e)
        }
    }
}
