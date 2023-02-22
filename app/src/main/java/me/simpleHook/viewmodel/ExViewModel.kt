package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.simpleHook.bean.ExtensionConfig

class ExViewModel : ViewModel() {
    val signInfoEdit = MutableLiveData("")
    val extensionConfig = MutableLiveData<ExtensionConfig>(null)
}