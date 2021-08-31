package me.simpleHook.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageInfo
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import me.simpleHook.bean.AppItem
import me.simpleHook.util.AppUtils
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _userApps = MutableLiveData<List<AppItem>>()
    private val _systemApps = MutableLiveData<List<AppItem>>()
    val userApps: LiveData<List<AppItem>>
        get() = _userApps
    val systemApps: LiveData<List<AppItem>>
        get() = _systemApps

    fun fetchData() {
        _userApps.value = getAppList(AppUtils.getInstalledUserApp(getApplication()))
        _systemApps.value = getAppList(AppUtils.getInstalledSystemApp(getApplication()))
    }

    /* val userApps = MutableLiveData<List<AppItem>>()
     val systemApps = MutableLiveData<List<AppItem>>()
     fun fetchData() {
         userApps.value = getAppList(AppUtils.getInstalledUserApp(getApplication()))
         systemApps.value = getAppList(AppUtils.getInstalledSystemApp(getApplication()))
     }*/
    private fun getAppList(packageInfoList: List<PackageInfo>): List<AppItem> {
        val appList = ArrayList<AppItem>()
        for (i in packageInfoList.indices) {
            packageInfoList[i].apply {
                appList.add(
                    AppItem(
                        AppUtils.getAppName(getApplication(), this),
                        packageName,
                        AppUtils.getAppVersionName(getApplication(), packageName),
                        getDateTime(lastUpdateTime)
                    )
                )
            }
        }
        return appList
    }
    /**
     * 获取最后一次更新时间
     */
    @SuppressLint("SimpleDateFormat")
    private fun getDateTime(time: Long) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat("yy-MM-dd").format(time)
    } else ""
}