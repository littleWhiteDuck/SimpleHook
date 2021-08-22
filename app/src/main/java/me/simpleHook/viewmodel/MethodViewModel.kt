package me.simpleHook.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.simpleHook.bean.AppItem
import me.simpleHook.bean.MethodConfig
import me.simpleHook.database.entity.AppConfig

class MethodViewModel : ViewModel() {
    var configLive = MutableLiveData<AppConfig>()
    var appLive = MutableLiveData<AppItem>()
    private var methodLive: MutableLiveData<ArrayList<MethodConfig>>? =null

    fun getMethodLive(): MutableLiveData<ArrayList<MethodConfig>>? {
        if (methodLive == null){
            methodLive = MutableLiveData(ArrayList<MethodConfig>())
        }
        return methodLive
    }
}