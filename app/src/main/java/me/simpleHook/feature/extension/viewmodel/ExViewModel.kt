package me.simpleHook.feature.extension.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.simpleHook.data.EXtGuiseSignItem
import me.simpleHook.data.ExClipboardConfig
import me.simpleHook.data.ExtBlockDialog
import me.simpleHook.data.ExtExitConfig
import me.simpleHook.data.ExtFileMonitorConfig
import me.simpleHook.data.ExtRecordSettings
import me.simpleHook.data.ExtensionConfig

class ExViewModel : ViewModel() {
    val signInfoEdit = MutableLiveData("")
    private val _extensionConfig = MutableLiveData(ExtensionConfig())
    val extensionConfig: LiveData<ExtensionConfig> = _extensionConfig

    private var hasInit = false

    fun initExtensionConfig(extensionConfig: ExtensionConfig) {
        if (hasInit) return
        hasInit = true
        _extensionConfig.value = extensionConfig
    }

    fun updateFilterClipboard(clipboardConfig: ExClipboardConfig) {
        _extensionConfig.value = extensionConfig.value!!.copy(filterClipboard = clipboardConfig)
    }

    fun updateExit(exitConfig: ExtExitConfig) {
        _extensionConfig.value = extensionConfig.value!!.copy(exitConfig = exitConfig)
    }

    fun updateFileMonitor(fileMonitorConfig: ExtFileMonitorConfig) {
        _extensionConfig.value = extensionConfig.value!!.copy(fileMonitor = fileMonitorConfig)
    }

    fun updateGuiseSign(signItems: List<EXtGuiseSignItem>) {
        val signConfig = extensionConfig.value!!.signConfig
        _extensionConfig.value =
            extensionConfig.value!!.copy(
                signConfig = signConfig.copy(
                    guiseSign = signConfig.guiseSign.copy(
                        signConfigs = signItems
                    )
                )
            )
    }

    fun updateRecordSettings(recordSettings: ExtRecordSettings) {
        _extensionConfig.value = extensionConfig.value!!.copy(recordSettings = recordSettings)
    }

    fun updateDialogBlock(dialogBlock: ExtBlockDialog) {
        val dialogConfig = extensionConfig.value!!.popupConfig
        _extensionConfig.value =
            extensionConfig.value!!.copy(popupConfig = dialogConfig.copy(blockDialog = dialogBlock))
    }
}