package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExViewModel : ViewModel() {
    val signInfoEdit = MutableLiveData("")
    val signInfo = MutableLiveData<String>()
}