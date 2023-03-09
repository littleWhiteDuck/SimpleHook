package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    val permStatus = MutableLiveData(0)
}