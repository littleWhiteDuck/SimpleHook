package me.simpleHook.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.bean.AppItem
import me.simpleHook.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.util.AppUtils
import me.simpleHook.util.TimeUtil

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _userApps = MutableLiveData<List<AppItem>>()
    private val _systemApps = MutableLiveData<List<AppItem>>()
    private val blackList = "me.simpleHook,bin.mt.plus.canary,com.drakeet.purewriter"
    val userApps: LiveData<List<AppItem>>
        get() = _userApps
    val systemApps: LiveData<List<AppItem>>
        get() = _systemApps

    fun fetchData(sortSelected: Int, reverseChecked: Boolean) {
        viewModelScope.launch {
            loadData(sortSelected, reverseChecked)
        }
    }

    private suspend fun loadData(sortSelected: Int, reverseChecked: Boolean) =
        withContext(Dispatchers.Default) {
            val userAppList = getSortAppList(
                getAppList(AppUtils.getInstalledUserApp(getApplication())),
                sortSelected,
                reverseChecked
            )
            val systemAppList = getSortAppList(
                getAppList(AppUtils.getInstalledSystemApp(getApplication())),
                sortSelected,
                reverseChecked
            )
            _userApps.postValue(userAppList)
            _systemApps.postValue(systemAppList)
        }

    private fun getSortAppList(
        appList: List<AppItem>,
        sortSelected: Int,
        reverseChecked: Boolean
    ): List<AppItem> {
        val tempList = appList.sortedBy { appItem ->
            when (sortSelected) {
                APP_LIST_BY_NAME -> appItem.name
                APP_LIST_BY_PACKAGE_NAME -> appItem.packageName
                APP_LIST_BY_INSTALLED_TIME -> appItem.installedTime
                else -> appItem.targetApi
            }
        }
        return if (reverseChecked) tempList.reversed() else tempList
    }

    private fun getAppList(packageInfoList: List<PackageInfo>): List<AppItem> {
        val appList = ArrayList<AppItem>()
        for (i in packageInfoList.indices) {
            if (blackList.contains(packageInfoList[i].packageName)) continue
            packageInfoList[i].apply {
                appList.add(
                    AppItem(
                        AppUtils.getAppName(getApplication(), this),
                        packageName,
                        AppUtils.getAppVersionName(getApplication(), packageName),
                        AppUtils.getAppVersionCode(getApplication(), packageName),
                        TimeUtil.getDateTime(lastUpdateTime, "yyyy-MM-dd"),
                        AppUtils.getTargetSdkVersion(getApplication(), packageName)
                    )
                )
            }
        }
        return appList
    }
}