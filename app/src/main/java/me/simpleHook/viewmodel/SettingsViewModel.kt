package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.simpleHook.data.PermissionState

class SettingsViewModel : ViewModel() {
    val permStatus = MutableLiveData(PermissionState.GRANT)
}