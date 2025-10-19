package me.simpleHook.hook.extension

import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordApplication
import me.simpleHook.data.record.RecordCrash
import me.simpleHook.data.record.RecordType
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.RecordOutHelper

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
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            t ?: return@setDefaultUncaughtExceptionHandler
            RecordOutHelper.outputRecord(
                type = RecordType.CrashCaught,
                record = RecordCrash(threadName = t.name, stackDetail = e.stackTraceToString())
            )
        }
    }
}