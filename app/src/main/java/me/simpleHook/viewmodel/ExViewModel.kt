package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.simpleHook.bean.ExtensionConfigBean

class ExViewModel : ViewModel() {
    val signInfoEdit = MutableLiveData("")
    val extensionConfig = MutableLiveData<ExtensionConfigBean>(null)
}