package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    val permStatus = MutableLiveData<Int>().also {
        it.value = 0
    }
}